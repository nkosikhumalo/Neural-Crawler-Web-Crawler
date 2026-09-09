package com.neuralcrawler.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
  FILE: UIController.java
  =========================
  Serves the placeholder page at GET / — points browser users toward the Angular app.
*/
@Controller
public class UIController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
