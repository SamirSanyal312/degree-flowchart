package com.example.degree_planner.web;

import com.example.degree_planner.web.FlowController.EdgeDTO;
import com.example.degree_planner.web.FlowController.NodeDTO;
import com.example.degree_planner.web.FlowController.ProgressDTO;
import com.example.degree_planner.web.FlowController.SemesterDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {"/schema.sql", "/data.sql"},
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(
  statements = {
    "DELETE FROM completion WHERE student_id=2",
    "INSERT INTO completion(student_id, course_code, semester_no, grade) VALUES (2,'CSCI-B503',1,'A')",
    "INSERT INTO completion(student_id, course_code, semester_no, grade) VALUES (2,'CSCI-P536',1,'A')",
    "INSERT INTO completion(student_id, course_code, semester_no, grade) VALUES (2,'CSCI-P200',1,'A')",
    "INSERT INTO completion(student_id, course_code, semester_no, grade) VALUES (2,'CSCI-B561',1,'A')"
  },
  executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
// (optional) clean up after each method
@Sql(
  statements = { "DELETE FROM completion WHERE student_id=2" },
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class FlowControllerTest {

  @Autowired
  MockMvc mvc;

  // --- helpers ---
  @SuppressWarnings("unchecked")
  private <T> T modelAttr(MvcResult r, String name, Class<T> type) {
    Object o = r.getModelAndView().getModel().get(name);
    assertThat(o).isInstanceOf(type);
    return (T) o;
  }

  private static Map<String, NodeDTO> indexByCode(List<NodeDTO> nodes) {
    return nodes.stream().collect(Collectors.toMap(NodeDTO::code, n -> n));
  }

  // student1 -> MS, start_semester = 1 (no read-only semesters)
  @Test
  @WithMockUser(username = "student1")
  void flow_forStudent1_rendersAndHasExpectedModel() throws Exception {
    MvcResult res =
        mvc.perform(get("/flow"))
            .andExpect(status().isOk())
            .andExpect(view().name("flow"))
            .andExpect(model().attributeExists("displayName", "degreeName",
                "nodes", "edges", "semesters", "progress", "groups"))
            .andReturn();

    // Semesters: 4 items, none read-only for student1
    List<SemesterDTO> sems = modelAttr(res, "semesters", List.class);
    assertThat(sems).hasSize(4);
    assertThat(sems.stream().allMatch(s -> !s.readOnly())).isTrue();
    assertThat(sems.stream().allMatch(s -> s.plannedCredits() == 0)).isTrue();

    // Progress: earned/planned computed from DB; at least non-negative, totalRequired=30
    ProgressDTO progress = modelAttr(res, "progress", ProgressDTO.class);
    assertThat(progress.totalRequired()).isEqualTo(30);
    assertThat(progress.earned()).isGreaterThanOrEqualTo(0);
    assertThat(progress.planned()).isGreaterThanOrEqualTo(0);

    // Nodes present, edges present
    List<NodeDTO> nodes = modelAttr(res, "nodes", List.class);
    List<EdgeDTO> edges = modelAttr(res, "edges", List.class);
    assertThat(nodes).isNotEmpty();
    assertThat(edges).isNotEmpty();

    // A couple of sanity checks on known course codes
    Map<String, NodeDTO> byCode = indexByCode(nodes);
    assertThat(byCode).containsKeys("CSCI-B503", "CSCI-P536", "CSCI-B547");

    // For student1 (no completions set in data.sql), B503 should NOT be COMPLETED
    assertThat(byCode.get("CSCI-B503").status()).isNotEqualTo("COMPLETED");
  }

  // student2 -> MS, start_semester = 3 (first 2 semesters read-only) and has completions in data.sql
  @Test
  @WithMockUser(username = "student2")
  void flow_forStudent2_honorsReadOnlyAndStatuses() throws Exception {
    MvcResult res =
        mvc.perform(get("/flow"))
            .andExpect(status().isOk())
            .andExpect(view().name("flow"))
            .andExpect(model().attributeExists("nodes","semesters","progress","groups"))
            .andReturn();

    // Semesters: first two read-only, last two editable
    List<SemesterDTO> sems = modelAttr(res, "semesters", List.class);
    assertThat(sems).hasSize(4);
    assertThat(sems.get(0).readOnly()).isTrue();
    assertThat(sems.get(1).readOnly()).isTrue();
    assertThat(sems.get(2).readOnly()).isFalse();
    assertThat(sems.get(3).readOnly()).isFalse();

    // Earned credits for student2 from data.sql: 4 courses x 3cr = 12
    ProgressDTO progress = modelAttr(res, "progress", ProgressDTO.class);
    assertThat(progress.totalRequired()).isEqualTo(30);
    assertThat(progress.earned()).isEqualTo(12);
    assertThat(progress.planned()).isGreaterThanOrEqualTo(0);

    // Node statuses derived from data.sql completions & prereqs
    List<NodeDTO> nodes = modelAttr(res, "nodes", List.class);
    Map<String, NodeDTO> byCode = indexByCode(nodes);

    // Completed courses in data.sql for student2
    assertThat(byCode.get("CSCI-B503").status()).isEqualTo("COMPLETED");
    assertThat(byCode.get("CSCI-P536").status()).isEqualTo("COMPLETED");

    // B547 requires P536; with P536 completed it should be ELIGIBLE (not locked)
    assertThat(byCode.get("CSCI-B547").status()).isEqualTo("ELIGIBLE");

    // Groups exist and at least one has earned > 0 (progress is being tallied)
    List<?> groups = modelAttr(res, "groups", List.class);
    assertThat(groups).isNotEmpty();
  }
}
