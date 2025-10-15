package com.example.degree_planner.data;

import com.example.degree_planner.domain.Course;
import org.springframework.data.repository.CrudRepository;

public interface CourseRepository extends CrudRepository<Course, String> {}