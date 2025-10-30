package com.example.degree_planner;

import com.example.degree_planner.data.CourseRepository;
import com.example.degree_planner.data.PrereqRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DegreePlannerApplication {

  public static void main(String[] args) {
    SpringApplication.run(DegreePlannerApplication.class, args);
  }

  @Bean
  CommandLineRunner checkDb(CourseRepository courses, PrereqRepository prereqs) {
    return args -> {
      System.out.println("[DB] courses=" + courses.count() + ", prereqs=" + prereqs.count());
    };
  }
}