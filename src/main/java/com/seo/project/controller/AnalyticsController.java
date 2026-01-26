package com.seo.project.controller;

import com.seo.project.dto.VideoAnalytics;
import com.seo.project.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/analytics")
    public String showAnalyticsPage() {
        return "analytics";
    }

    @PostMapping("/analytics")
    public String analyzeVideo(@RequestParam("url") String url, Model model) {
        try {
            VideoAnalytics analytics = analyticsService.analyzeVideo(url);
            if (analytics != null) {
                model.addAttribute("data", analytics);
                model.addAttribute("url", url);
            } else {
                model.addAttribute("error", "Invalid YouTube URL or unable to fetch data.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
        }
        return "analytics";
    }
}