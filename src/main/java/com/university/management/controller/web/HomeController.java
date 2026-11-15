package com.university.management.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    // The POST /login part is handled by Spring Security (SecurityConfig.formLogin)
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
