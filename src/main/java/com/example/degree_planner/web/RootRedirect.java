package com.example.degree_planner.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootRedirect {

  // hit http://localhost:8080/ and go to the login
  @GetMapping("/")
  public String root() {
    return "redirect:/login";
  }

  // after login we can also support /home -> /courses
  @GetMapping("/home")
  public String home() {
    return "redirect:/courses";
  }
}