package com.example.degree_planner.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("course")
public class Course {
  @Id private String code;     // PK
  private String title;
  private Integer credits;
  private Boolean core;

  public Course() {}

  public Course(String code, String title, Integer credits, Boolean core) {
    this.code = code; this.title = title; this.credits = credits; this.core = core;
  }

  public String getCode() { return code; }
  public String getTitle() { return title; }
  public Integer getCredits() { return credits; }
  public Boolean getCore() { return core; }

  public void setCode(String code) { this.code = code; }
  public void setTitle(String title) { this.title = title; }
  public void setCredits(Integer credits) { this.credits = credits; }
  public void setCore(Boolean core) { this.core = core; }
}