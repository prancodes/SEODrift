package com.seo.project.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "index";
    }

    @GetMapping({"/tags"})
    public String tags() {
        return "tags";
    }

    @GetMapping({"/thumbnail"})
    public String thumbnail() {
        return "thumbnail";
    }

    @GetMapping({"/video"})
    public String video() {
        return "video";
    }
}
