package com.example.degree_planner.data;

import com.example.degree_planner.domain.Course;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CourseRepositoryTest {

  @Autowired CourseRepository repo;

  @Test
  void findAll_returnsSomeCourses() {
    List<Course> all = (List<Course>) repo.findAll();
    assertThat(all).isNotEmpty();
    assertThat(all.get(0).getCode()).isNotBlank();
  }
}
