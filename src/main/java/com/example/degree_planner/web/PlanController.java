package com.example.degree_planner.web;

import com.example.degree_planner.data.CourseRepository;
import com.example.degree_planner.domain.Course;
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

  // ---- small DTOs ----
  public record CourseDTO(String code, String title, int credits, boolean core) {}
  public record PlanRequest(int semesterNo, List<String> courseCodes) {}
  public record PlanResponse(String status, String message, int plannedCredits) {}

  // ---- helpers ----
  private Map<String, Object> getStudent(String username) {
    return jdbc.queryForMap("select id, start_semester from students where username=?", username);
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

  private Map<String,Integer> creditsMap(List<Course> all) {
    return all.stream().collect(Collectors.toMap(Course::getCode, c -> c.getCredits()==null?0:c.getCredits()));
  }

  // ==========================================================
  // GET /api/semesters/{n}/available
  //  - Show ALL courses
  //  - Exclude any already completed OR already planned in ANY semester
  //  - No term/prereq/bucket gating (per requirements)
  // ==========================================================
  @GetMapping("/semesters/{semesterNo}/available")
  @Transactional(readOnly = true)
  public ResponseEntity<List<CourseDTO>> available(@PathVariable int semesterNo, Principal principal) {
    var student = getStudent(principal.getName());
    long sid = ((Number)student.get("id")).longValue();

    List<Course> all = StreamSupport.stream(courses.findAll().spliterator(), false).toList();

    Set<String> done = completed(sid);
    Map<Integer,Set<String>> planMap = plans(sid);
    Set<String> alreadyChosen = new LinkedHashSet<>(done);
    planMap.values().forEach(alreadyChosen::addAll);

    List<CourseDTO> out = new ArrayList<>();
    for (Course c : all) {
      String code = c.getCode();
      if (alreadyChosen.contains(code)) continue; // never show once chosen anywhere
      out.add(new CourseDTO(
          code,
          c.getTitle()==null? "" : c.getTitle(),
          c.getCredits()==null? 0 : c.getCredits(),
          Boolean.TRUE.equals(c.getCore())
      ));
    }
    out.sort(Comparator.comparing((CourseDTO d) -> d.code));
    return ResponseEntity.ok(out);
  }

  // ==========================================================
  // POST /api/plan
  //  - Save semester plan
  //  - Enforce per-semester 3..9 credits
  //  - Prevent duplicates across semesters
  //  - No bucket enforcement; no term/prereq gating
  // ==========================================================
  @PostMapping("/plan")
  @Transactional
  public ResponseEntity<PlanResponse> savePlan(@RequestBody PlanRequest req, Principal principal) {
    if (req == null || req.semesterNo < 1 || req.semesterNo > 8) {
      return bad("Invalid semester number");
    }
    var student = getStudent(principal.getName());
    long sid = ((Number)student.get("id")).longValue();
    int startSem = ((Number)student.get("start_semester")).intValue();

    if (req.semesterNo < startSem) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new PlanResponse("error","Past semesters are read-only",0));
    }

    List<Course> all = StreamSupport.stream(courses.findAll().spliterator(), false).toList();
    Map<String, Course> byCode = all.stream()
        .collect(Collectors.toMap(Course::getCode, c -> c));
    Map<String,Integer> credits = creditsMap(all);

    Set<String> done = completed(sid);
    Map<Integer,Set<String>> planMap = plans(sid);

    // courses planned in other semesters (we replace only the target semester)
    Set<String> plannedOther = new LinkedHashSet<>();
    for (var e : planMap.entrySet()) if (e.getKey() != req.semesterNo) plannedOther.addAll(e.getValue());

    // ---- validate new selection ----
    int totalCr = 0;
    Set<String> seen = new LinkedHashSet<>();
    List<String> picked = (req.courseCodes == null) ? List.of() : req.courseCodes;

    for (String code : picked) {
      if (!byCode.containsKey(code))  return bad("Unknown course: " + code);
      if (seen.contains(code))        return bad("Duplicate: " + code);
      if (done.contains(code))        return bad("Already completed: " + code);
      if (plannedOther.contains(code)) return bad("Already planned in another semester: " + code);

      totalCr += credits.getOrDefault(code,0);
      seen.add(code);
    }

    if (totalCr < 3)  return bad("Minimum 3 credits required");
    if (totalCr > 9)  return bad("Maximum 9 credits per semester");

    // ---- save: replace this semester's rows ----
    jdbc.update("delete from semester_plan where student_id=? and semester_no=?", sid, req.semesterNo);
    for (String code : seen) {
      jdbc.update("insert into semester_plan(student_id, semester_no, course_code) values (?,?,?)",
          sid, req.semesterNo, code);
    }
    return ResponseEntity.ok(new PlanResponse("ok","Saved", totalCr));
  }

  // ==========================================================
  // DELETE /api/plan
  //  - Clear ALL planned courses for logged-in user
  // ==========================================================
  @DeleteMapping("/plan")
  @Transactional
  public ResponseEntity<Map<String,String>> clearAllPlanned(Principal principal) {
    var student = jdbc.queryForMap("select id from students where username=?", principal.getName());
    long sid = ((Number)student.get("id")).longValue();
    jdbc.update("delete from semester_plan where student_id=?", sid);
    return ResponseEntity.ok(Map.of("status","ok","message","All planned courses cleared"));
  }

  private ResponseEntity<PlanResponse> bad(String msg) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new PlanResponse("error", msg, 0));
  }
}
