package com.seo.project.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Simple controller to handle public static pages.
 */
@Slf4j
@Controller
public class WebController {

    @GetMapping({"/", "/home"})
    public String homePage() {
        log.debug("Routing to Landing Page");
        return "index";
    }
}
