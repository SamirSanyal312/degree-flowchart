package com.example.degree_planner.web;

import com.example.degree_planner.data.PrereqRepository;
import com.example.degree_planner.domain.Prereq;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PrereqController {

    private final PrereqRepository repo;

    public PrereqController(PrereqRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/prereqs")
    public String showPrereqs(Model model) {
        List<Prereq> rows = repo.findAllOrdered();
        model.addAttribute("rows", rows);   // <— name used by the template
        return "prereqs";
    }
}