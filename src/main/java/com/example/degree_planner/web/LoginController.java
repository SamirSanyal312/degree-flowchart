package com.example.degree_planner.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

  // Renders the login page template at /templates/login.html
  @GetMapping("/login")
  public String login() {
    return "login";
  }
}