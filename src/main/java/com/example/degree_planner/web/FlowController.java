package com.example.degree_planner.web;

import com.example.degree_planner.data.CourseRepository;
import com.example.degree_planner.data.PrereqRepository;
import com.example.degree_planner.domain.Course;
import com.example.degree_planner.domain.Status;
import com.example.degree_planner.domain.PrereqEdge;
import com.example.degree_planner.service.EligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
@RequiredArgsConstructor
public class FlowController {

    private final CourseRepository courses;
    private final PrereqRepository prereqs;
    private final EligibilityService eligibility;

    // Small DTO the template will consume
    public record Node(
            String code, String title, int credits, boolean core,
            String status, int col, int row
    ) {}

    @GetMapping("/flow")
    public String flow(Model model) {
        // 1) Load courses
        List<Course> all = StreamSupport.stream(courses.findAll().spliterator(), false)
                .sorted(Comparator.comparing(Course::getCode))
                .toList();

        // 2) For Project 1, assume we already know completed.
        // Replace this with the actual student transcript later.
        Set<String> completed = new LinkedHashSet<>();
        completed.add("CSCI-P100"); // sample

        // 3) Compute status of every course
        Set<String> allCodes = all.stream().map(Course::getCode).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Status> status = eligibility.evaluate(completed, allCodes);

        // 4) Place nodes into simple columns by course "hundreds":
        // 100-level -> col=1, 200-level -> col=2, etc.
        Map<Integer, Integer> nextRow = new HashMap<>();
        List<Node> nodes = new ArrayList<>();

        for (Course c : all) {
            String code = c.getCode();
            int num = 0;
            try {
                num = Integer.parseInt(code.replaceAll("\\D", ""));
            } catch (Exception ignored) {}
            int col = Math.max(1, num / 100); // 100->1, 150->1, 200->2, 300->3, ...
            int row = nextRow.merge(col, 1, Integer::sum) - 1;

            nodes.add(new Node(
                    code,
                    c.getTitle(),
                    c.getCredits() == null ? 0 : c.getCredits(),
                    Boolean.TRUE.equals(c.getCore()),
                    status.get(code).name(), // COMPLETED|ELIGIBLE|LOCKED
                    col,
                    row
            ));
        }

        // 5) Load edges (course_code, requires_code)
        List<PrereqEdge> edges = prereqs.findAllEdges();

        model.addAttribute("nodes", nodes);
        model.addAttribute("edges", edges);
        return "flow";
    }
}