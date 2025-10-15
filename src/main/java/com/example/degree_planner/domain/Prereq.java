package com.example.degree_planner.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("prereq")
public class Prereq {
    @Id
    private String id;

    @Column("course_code")
    private String courseCode;

    @Column("requires_code")
    private String requiresCode;

    public Prereq(String id, String courseCode, String requiresCode) {
        this.id = id;
        this.courseCode = courseCode;
        this.requiresCode = requiresCode;
    }

    public String getId() { return id; }
    public String getCourseCode() { return courseCode; }
    public String getRequiresCode() { return requiresCode; }
}