package com.seo.project.controller;

import com.seo.project.dto.VideoAnalytics;
import com.seo.project.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * AnalyticsController manages the core "Video Intelligence" features of the
 * application.
 * It handles video URL submissions, triggers API-based analysis, and manages
 * the result display.
 */
@Slf4j
@Controller
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Renders the initial analytics submission page or processes GET URL search.
     */
    @GetMapping("/analytics")
    public String showAnalyticsPage(@RequestParam(value = "url", required = false) String url,
                                   Authentication authentication,
                                   Model model) {
        if (url != null && !url.isBlank()) {
            return analyzeVideo(url, authentication, model);
        }
        return "analytics";
    }

    /**
     * Processes a YouTube URL submission and returns a comprehensive SEO audit.
     * Integrates with Google YouTube Data API and other metrics via
     * AnalyticsService.
     */
    @PostMapping("/analytics")
    public String analyzeVideo(@RequestParam("url") String url,
            Authentication authentication,
            Model model) {
        try {
            String userEmail = null;
            log.info("Processing intelligence request for URL: [{}]", url);

            // Extract user context for personalized history tracking
            if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                userEmail = oauth2User.getAttribute("email");
                log.debug("User context detected: {}. Analytics will be saved to history.", userEmail);
            } else {
                log.info("Anonymous analytics request. History will not be recorded.");
            }

            VideoAnalytics analytics = analyticsService.analyzeVideo(url, userEmail);

            if (analytics != null) {
                model.addAttribute("data", analytics);
                model.addAttribute("url", url);
                log.debug("Analysis completed for: {}", analytics.title());
            } else {
                log.warn("Analysis failed for URL: {}. Returned null analytics.", url);
                model.addAttribute("error", "Invalid YouTube URL or unable to fetch data.");
            }
        } catch (Exception e) {
            log.error("Unexpected error during video analysis process: {}", e.getMessage(), e);
            model.addAttribute("error", "An internal error occurred. Please try again later.");
        }
        return "analytics";
    }
}