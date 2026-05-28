package com.seo.project.controller;

import com.seo.project.model.VideoAnalysis;
import com.seo.project.repository.VideoAnalysisRepository;
import com.seo.project.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * DashboardController handles the retrieval and display of user-specific data,
 * primarily the SEO audit history and personal account information.
 */
@Slf4j
@Controller
public class DashboardController {

    private final VideoAnalysisRepository videoAnalysisRepository;
    private final UserRepository userRepository;

    public DashboardController(VideoAnalysisRepository videoAnalysisRepository, UserRepository userRepository) {
        this.videoAnalysisRepository = videoAnalysisRepository;
        this.userRepository = userRepository;
    }

    /**
     * Renders the user dashboard. 
     * Orchestrates data retrieval from PostgreSQL based on the authenticated Google ID.
     */
    @GetMapping("/dashboard")
    public String showDashboard(Authentication authentication, Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            log.warn("Unauthorized access attempt to /dashboard. Redirecting to home.");
            return "redirect:/";
        }

        String email = oauth2User.getAttribute("email");
        log.info("Accessing Dashboard for user: [{}]", email);

        // Fetch user context and compute comprehensive statistics
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            List<VideoAnalysis> analyses = videoAnalysisRepository.findByUserOrderByAnalyzedAtDesc(user);
            long auditCount = analyses.size();
            double avgSeo = analyses.stream()
                    .mapToInt(a -> a.getSeoScore() != null ? a.getSeoScore() : 0)
                    .average()
                    .orElse(0.0);
            double avgEngagement = analyses.stream()
                    .mapToDouble(a -> a.getEngagementRate() != null ? a.getEngagementRate() : 0.0)
                    .average()
                    .orElse(0.0);

            String health = "Pending";
            if (auditCount > 0) {
                if (avgSeo >= 80) health = "Excellent";
                else if (avgSeo >= 50) health = "Needs Work";
                else health = "Critical";
            }

            model.addAttribute("auditCount", auditCount);
            model.addAttribute("avgSeoScore", Math.round(avgSeo));
            model.addAttribute("avgEngagement", Math.round(avgEngagement * 100.0) / 100.0);
            model.addAttribute("channelHealth", health);
            model.addAttribute("user", user);
        }, () -> {
            model.addAttribute("auditCount", 0);
            model.addAttribute("avgSeoScore", 0);
            model.addAttribute("avgEngagement", 0.0);
            model.addAttribute("channelHealth", "Pending");
            model.addAttribute("user", com.seo.project.model.User.builder()
                    .name(oauth2User.getAttribute("name"))
                    .email(email)
                    .build());
        });
        
        return "dashboard";
    }

    /**
     * Fragment endpoint for lazy-loading the audit history.
     */
    @GetMapping("/dashboard/history")
    public String getDashboardHistory(Authentication authentication, Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return "error";
        }

        String email = oauth2User.getAttribute("email");
        userRepository.findByEmail(email).ifPresent(user -> {
            List<VideoAnalysis> history = videoAnalysisRepository.findByUserOrderByAnalyzedAtDesc(user);
            model.addAttribute("history", history != null ? history : new ArrayList<>());
        });

        return "dashboard :: history-fragment";
    }
}
