package com.seo.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TagsController {

    @GetMapping("/tags")
    public String showTagsPage() {
        return "tags";
    }

}
