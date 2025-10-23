package com.example.degree_planner.web;

import com.example.degree_planner.data.CourseRepository;
import com.example.degree_planner.data.PrereqRepository;
import com.example.degree_planner.domain.Course;
import com.example.degree_planner.domain.Prereq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PlanController {

  private final JdbcTemplate jdbc;
  private final CourseRepository courses;
  private final PrereqRepository prereqs;

  record CourseDTO(String code, String title, int credits, boolean core) {}
  record PlanRequest(int semesterNo, List<String> courseCodes) {}
  record PlanResponse(String status, String message, int plannedCredits) {}

  private Map<String, Object> getStudent(String username) {
    return jdbc.queryForMap("select id, start_semester, degree from students where username=?", username);
  }

  private Map<String, Set<String>> prereqMap() {
    Map<String, Set<String>> m = new HashMap<>();
    for (Prereq e : StreamSupport.stream(prereqs.findAll().spliterator(), false).toList()) {
      m.computeIfAbsent(e.getCourseCode(), k -> new LinkedHashSet<>()).add(e.getRequiresCode());
    }
    return m;
  }

  private Map<String,Integer> creditsMap(List<Course> all) {
    return all.stream().collect(Collectors.toMap(Course::getCode, c -> c.getCredits()==null?0:c.getCredits()));
  }

  private Map<String,Integer> fallMap() {
    Map<String,Integer> m = new HashMap<>();
    jdbc.query("select code, ifnull(offered_fall,1) as f from course",
        (RowCallbackHandler) rs -> m.put(rs.getString("code"), rs.getInt("f")));
    return m;
  }

  private Map<String,Integer> springMap() {
    Map<String,Integer> m = new HashMap<>();
    jdbc.query("select code, ifnull(offered_spring,1) as s from course",
        (RowCallbackHandler) rs -> m.put(rs.getString("code"), rs.getInt("s")));
    return m;
  }

  private Set<String> completed(long studentId) {
    return new LinkedHashSet<>(jdbc.query(
        "select course_code from completion where student_id=?",
        (rs, n) -> rs.getString(1), studentId));
  }

  private Map<Integer, Set<String>> plans(long studentId) {
    Map<Integer, Set<String>> map = new HashMap<>();
    jdbc.query("select semester_no, course_code from semester_plan where student_id=?",
        (RowCallbackHandler) rs -> map
            .computeIfAbsent(rs.getInt(1), k -> new LinkedHashSet<>())
            .add(rs.getString(2)),
        studentId);
    return map;
  }

  // GET available courses for a semester
  @GetMapping("/semesters/{semesterNo}/available")
  @Transactional(readOnly = true)
  public ResponseEntity<List<CourseDTO>> available(@PathVariable int semesterNo, Principal principal) {
    var student = getStudent(principal.getName());
    long sid = ((Number)student.get("id")).longValue();

    boolean isFall = (semesterNo % 2 == 1);

    List<Course> all = StreamSupport.stream(courses.findAll().spliterator(), false).toList();
    Map<String, Set<String>> prereqsM = prereqMap();
    Map<String,Integer> fall = fallMap(), spring = springMap();

    Set<String> done = completed(sid);
    Map<Integer,Set<String>> planMap = plans(sid);
    Set<String> alreadyChosen = new LinkedHashSet<>(done);
    planMap.values().forEach(alreadyChosen::addAll);

    Set<String> prereqBase = new LinkedHashSet<>(done);
    for (int p=1;p<semesterNo;p++) prereqBase.addAll(planMap.getOrDefault(p, Set.of()));

    List<CourseDTO> out = new ArrayList<>();
    for (Course c : all) {
      String code = c.getCode();
      if (alreadyChosen.contains(code)) continue;

      boolean offered = isFall ? fall.getOrDefault(code,1)==1 : spring.getOrDefault(code,1)==1;
      if (!offered) continue;

      if (!prereqBase.containsAll(prereqsM.getOrDefault(code, Set.of()))) continue;

      out.add(new CourseDTO(code, c.getTitle(), c.getCredits()==null?0:c.getCredits(), Boolean.TRUE.equals(c.getCore())));
    }
    out.sort(Comparator.comparing((CourseDTO d) -> d.code));
    return ResponseEntity.ok(out);
  }

  // POST save semester plan (validates 3..9 credits, term & prereqs)
  @PostMapping("/plan")
  @Transactional
  public ResponseEntity<PlanResponse> savePlan(@RequestBody PlanRequest req, Principal principal) {
    if (req.semesterNo < 1 || req.semesterNo > 8) {
      return bad("Invalid semester number");
    }
    var student = getStudent(principal.getName());
    long sid = ((Number)student.get("id")).longValue();
    int startSem = ((Number)student.get("start_semester")).intValue();

    if (req.semesterNo < startSem) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new PlanResponse("error","Past semesters are read-only",0));
    }

    List<Course> all = StreamSupport.stream(courses.findAll().spliterator(), false).toList();
    Map<String, Course> byCode = all.stream().collect(Collectors.toMap(Course::getCode, c->c));
    Map<String,Integer> credits = creditsMap(all);
    Map<String, Set<String>> prereqsM = prereqMap();
    Map<String,Integer> fall = fallMap(), spring = springMap();

    Set<String> done = completed(sid);
    Map<Integer,Set<String>> planMap = plans(sid);
    Set<String> earlier = new LinkedHashSet<>(done);
    for (int p=1;p<req.semesterNo;p++) earlier.addAll(planMap.getOrDefault(p, Set.of()));

    boolean isFall = (req.semesterNo % 2 == 1);

    int totalCr = 0;
    Set<String> seen = new LinkedHashSet<>();
    for (String code : (req.courseCodes==null? List.<String>of(): req.courseCodes)) {
      if (!byCode.containsKey(code))  return bad("Unknown course: "+code);
      if (seen.contains(code))        return bad("Duplicate: "+code);
      if (done.contains(code))        return bad("Already completed: "+code);
      if (planMap.values().stream().anyMatch(s -> s.contains(code))) return bad("Already planned elsewhere: "+code);

      boolean offered = isFall ? fall.getOrDefault(code,1)==1 : spring.getOrDefault(code,1)==1;
      if (!offered)                return bad(code+" is not offered this term");

      if (!earlier.containsAll(prereqsM.getOrDefault(code, Set.of())))
        return bad(code+" prereqs not satisfied");

      totalCr += credits.getOrDefault(code,0);
      seen.add(code);
    }

    if (totalCr < 3)  return bad("Minimum 3 credits required");
    if (totalCr > 9)  return bad("Maximum 9 credits per semester");

    jdbc.update("delete from semester_plan where student_id=? and semester_no=?", sid, req.semesterNo);
    for (String code : seen) {
      jdbc.update("insert into semester_plan(student_id, semester_no, course_code) values (?,?,?)",
          sid, req.semesterNo, code);
    }
    return ResponseEntity.ok(new PlanResponse("ok","Saved", totalCr));
  }

  private ResponseEntity<PlanResponse> bad(String msg) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new PlanResponse("error", msg, 0));
  }
}
