package com.example.degree_planner.data;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.degree_planner.domain.Prereq;
import com.example.degree_planner.domain.PrereqEdge;

@Repository
public interface PrereqRepository extends CrudRepository<Prereq, String> {

    // For eligibility calculation (just the code list)
    @Query("""
           SELECT requires_code
           FROM prereq
           WHERE course_code = :code
           ORDER BY requires_code
           """)
    List<String> requiredCodes(@Param("code") String courseCode);

    // For the /prereqs table view (full rows)
    @Query("""
           SELECT id, course_code, requires_code
           FROM prereq
           ORDER BY course_code, requires_code
           """)
    List<Prereq> findAllOrdered();

    // For the flow view (lightweight edge projection)
    @Query("""
           SELECT course_code AS courseCode,
                  requires_code AS requiresCode
           FROM prereq
           ORDER BY course_code, requires_code
           """)
    List<PrereqEdge> findAllEdges();

    // (optional) if you want rows for a single course
    @Query("""
           SELECT id, course_code, requires_code
           FROM prereq
           WHERE course_code = :code
           ORDER BY requires_code
           """)
    List<Prereq> findAllByCourseCode(@Param("code") String courseCode);
}