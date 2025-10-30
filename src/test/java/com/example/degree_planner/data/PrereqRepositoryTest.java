package com.example.degree_planner.data;

import com.example.degree_planner.domain.Prereq;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PrereqRepositoryTest {

  @Autowired PrereqRepository repo;

  @Test
  void findAll_returnsEdges() {
    List<Prereq> edges = (List<Prereq>) repo.findAll();
    assertThat(edges).isNotEmpty();
    assertThat(edges).allSatisfy(e -> {
      assertThat(e.getCourseCode()).isNotBlank();
      assertThat(e.getRequiresCode()).isNotBlank();
    });
  }
}
