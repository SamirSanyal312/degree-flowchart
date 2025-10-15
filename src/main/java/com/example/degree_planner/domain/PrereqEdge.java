package com.example.degree_planner.domain;

/** Lightweight row-mapping for prereq edges. */
public record PrereqEdge(String courseCode, String requiresCode) {}