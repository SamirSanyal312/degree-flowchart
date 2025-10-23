package com.example.degree_planner.web;

import com.example.degree_planner.data.CourseRepository;
import com.example.degree_planner.data.PrereqRepository;
import com.example.degree_planner.domain.Course;
import com.example.degree_planner.domain.Prereq;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
@RequiredArgsConstructor
public class FlowController {

  private final CourseRepository courses;
  private final PrereqRepository prereqs;
  private final JdbcTemplate jdbc;

  // DTOs
  public record NodeDTO(
      String code, String title, int credits, boolean core,
      String status, int col, int row, List<String> unmet
  ) {}
  public record EdgeDTO(String requiresCode, String courseCode, boolean met) {}
  public record SemesterDTO(int num, boolean readOnly, int availableCount, int plannedCredits) {}
  public record ProgressDTO(int earned, int planned, int totalRequired) {}

  @GetMapping("/flow")
  @Transactional(readOnly = true)
  public String flow(Model model, Principal principal) {
    final String username = principal.getName();
    final int TOTAL_SEMS = 4;
    final int TOTAL_REQ_CREDITS = 30;

    // 1) All courses & prereqs
    List<Course> allCourses = StreamSupport.stream(courses.findAll().spliterator(), false)
        .sorted(Comparator.comparing(Course::getCode))
        .toList();
    List<Prereq> allEdges = StreamSupport.stream(prereqs.findAll().spliterator(), false).toList();

    Map<String, Set<String>> prereqMap = new HashMap<>();
    for (Prereq e : allEdges) {
      prereqMap.computeIfAbsent(e.getCourseCode(), k -> new LinkedHashSet<>()).add(e.getRequiresCode());
    }

    // 2) Student row
    var student = jdbc.queryForMap(
        "select id, display_name, start_semester, degree from students where username = ?",
        username);
    long studentId = ((Number) student.get("id")).longValue();
    int startSem = ((Number) student.get("start_semester")).intValue();
    String degreeCode = (String) student.get("degree");
    String degreeName = switch (degreeCode == null ? "" : degreeCode.toUpperCase()) {
      case "MS" -> "M.S. in Computer Science";
      case "UG" -> "B.S. in Computer Science";
      default   -> degreeCode;
    };

    // 3) Completed & plans
    Set<String> completed = new LinkedHashSet<>(jdbc.query(
        "select course_code from completion where student_id=?",
        (rs, n) -> rs.getString(1), studentId));

    Map<Integer, Set<String>> plans = new HashMap<>();
    jdbc.query(
        "select semester_no, course_code from semester_plan where student_id=? order by semester_no",
        (RowCallbackHandler) rs -> {
          int s = rs.getInt(1);
          String code = rs.getString(2);
          plans.computeIfAbsent(s, k -> new LinkedHashSet<>()).add(code);
        },
        studentId
    );

    // 4) Offerings (explicit RowCallbackHandler to avoid ambiguity)
    Map<String, Integer> fallMap = new HashMap<>();
    Map<String, Integer> springMap = new HashMap<>();
    jdbc.query(
        "select code, ifnull(offered_fall,1) as fall, ifnull(offered_spring,1) as spring from course",
        (RowCallbackHandler) rs -> {
          fallMap.put(rs.getString("code"), rs.getInt("fall"));
          springMap.put(rs.getString("code"), rs.getInt("spring"));
        }
    );

    // 5) Graph statuses
    Map<String, String> statusByCode = new LinkedHashMap<>();
    for (Course c : allCourses) {
      String code = c.getCode();
      if (completed.contains(code)) statusByCode.put(code, "COMPLETED");
      else statusByCode.put(code, completed.containsAll(prereqMap.getOrDefault(code, Set.of()))
          ? "ELIGIBLE" : "LOCKED");
    }

    // 6) Layout: columns via prereq depth, rows stable
    Map<String, Integer> memoLevel = new HashMap<>();
    for (Course c : allCourses) dfsLevel(c.getCode(), prereqMap, memoLevel);

    Map<Integer, List<String>> byLevel = new TreeMap<>();
    for (Course c : allCourses) {
      int lv = memoLevel.getOrDefault(c.getCode(), 0);
      byLevel.computeIfAbsent(lv, k -> new ArrayList<>()).add(c.getCode());
    }
    byLevel.values().forEach(list -> list.sort(Comparator.naturalOrder()));

    Map<String, Integer> row = new HashMap<>();
    for (var e : byLevel.entrySet()) {
      int r = 0; for (String code : e.getValue()) row.put(code, r++);
    }

    // 7) Nodes & edges
    List<NodeDTO> nodes = new ArrayList<>();
    for (Course c : allCourses) {
      String code = c.getCode();
      var reqs = prereqMap.getOrDefault(code, Set.of());
      var unmet = reqs.stream().filter(r -> !completed.contains(r)).sorted().toList();
      nodes.add(new NodeDTO(
          code,
          c.getTitle() == null ? "" : c.getTitle(),
          c.getCredits() == null ? 0 : c.getCredits(),
          Boolean.TRUE.equals(c.getCore()),
          statusByCode.get(code),
          memoLevel.getOrDefault(code, 0) + 1,
          row.getOrDefault(code, 0),
          unmet
      ));
    }

    List<EdgeDTO> edges = new ArrayList<>();
    for (Prereq e : allEdges) {
      edges.add(new EdgeDTO(e.getRequiresCode(), e.getCourseCode(),
          completed.contains(e.getRequiresCode())));
    }

    // 8) Semesters & progress
    Map<String, Integer> credits = allCourses.stream()
        .collect(Collectors.toMap(Course::getCode, c -> c.getCredits() == null ? 0 : c.getCredits()));

    int earnedCredits = completed.stream().mapToInt(code -> credits.getOrDefault(code, 0)).sum();
    int plannedCreditsTotal = plans.values().stream().flatMap(Set::stream)
        .mapToInt(code -> credits.getOrDefault(code, 0)).sum();

    List<SemesterDTO> sems = new ArrayList<>();
    Set<String> alreadyChosen = new LinkedHashSet<>(completed);
    plans.values().forEach(alreadyChosen::addAll);

    for (int s = 1; s <= TOTAL_SEMS; s++) {
      boolean isFall = (s % 2 == 1);
      boolean readOnly = s < startSem;

      Set<String> prereqBase = new LinkedHashSet<>(completed);
      for (int p = 1; p < s; p++) prereqBase.addAll(plans.getOrDefault(p, Set.of()));

      int availableCount = 0;
      for (Course c : allCourses) {
        String code = c.getCode();
        if (alreadyChosen.contains(code)) continue;

        boolean offered = isFall ? fallMap.getOrDefault(code,1)==1
                                 : springMap.getOrDefault(code,1)==1;
        if (!offered) continue;

        if (!prereqBase.containsAll(prereqMap.getOrDefault(code, Set.of()))) continue;

        availableCount++;
      }

      int plannedThisSem = plans.getOrDefault(s, Set.of()).stream()
          .mapToInt(code -> credits.getOrDefault(code, 0)).sum();

      sems.add(new SemesterDTO(s, readOnly, availableCount, plannedThisSem));
    }

    ProgressDTO progress = new ProgressDTO(earnedCredits, plannedCreditsTotal, TOTAL_REQ_CREDITS);

    // 9) Model → view
    model.addAttribute("displayName", student.get("display_name"));
    model.addAttribute("degreeName", degreeName);
    model.addAttribute("nodes", nodes);
    model.addAttribute("edges", edges);
    model.addAttribute("semesters", sems);
    model.addAttribute("progress", progress);
    return "flow";
  }

  private static int dfsLevel(String code, Map<String, Set<String>> prereqMap, Map<String, Integer> memo) {
    Integer seen = memo.get(code);
    if (seen != null) return seen;
    Set<String> reqs = prereqMap.get(code);
    if (reqs == null || reqs.isEmpty()) { memo.put(code, 0); return 0; }
    int best = 0;
    for (String r : reqs) best = Math.max(best, 1 + dfsLevel(r, prereqMap, memo));
    memo.put(code, best);
    return best;
  }
}
