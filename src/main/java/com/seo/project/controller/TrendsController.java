package com.seo.project.controller;

import com.seo.project.model.User;
import com.seo.project.repository.UserRepository;
import com.seo.project.service.CompetitorAnalyticsService;
import com.seo.project.service.YouTubeChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Slf4j
@Controller
public class TrendsController {

    private final UserRepository userRepository;
    private final CompetitorAnalyticsService competitorAnalyticsService;
    private final ObjectMapper objectMapper;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final YouTubeChannelService youtubeChannelService;

    public TrendsController(UserRepository userRepository,
                            CompetitorAnalyticsService competitorAnalyticsService,
                            ObjectMapper objectMapper,
                            OAuth2AuthorizedClientService authorizedClientService,
                            YouTubeChannelService youtubeChannelService) {
        this.userRepository = userRepository;
        this.competitorAnalyticsService = competitorAnalyticsService;
        this.objectMapper = objectMapper;
        this.authorizedClientService = authorizedClientService;
        this.youtubeChannelService = youtubeChannelService;
    }

    /**
     * Renders the Trends & Competitor Intelligence dashboard view.
     */
    @GetMapping("/trends")
    public String showTrends(Authentication authentication, Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            log.warn("Unauthorized access to /trends. Redirecting to home.");
            return "redirect:/";
        }

        String email = oauth2User.getAttribute("email");
        Optional<User> userOpt = userRepository.findWithCompetitorsByEmail(email);

        if (userOpt.isEmpty()) {
            return "redirect:/";
        }

        User user = userOpt.get();
        model.addAttribute("user", user);

        // Check if user has an active YouTube channel
        OAuth2AuthorizedClient authorizedClient = authorizedClientService
                .loadAuthorizedClient("google", authentication.getName());
        boolean hasChannel = false;
        if (authorizedClient != null) {
            try {
                hasChannel = (youtubeChannelService.getChannelIntelligence(authorizedClient, email) != null);
            } catch (Exception e) {
                log.warn("Failed to retrieve YouTube channel data for trends validation: {}", e.getMessage());
            }
        }
        model.addAttribute("hasChannel", hasChannel);

        if (!hasChannel) {
            // Bypass API fetches for guest users
            model.addAttribute("hasCompetitors", false);
            model.addAttribute("benchmarkJson", "{\"datasets\":[]}");
            model.addAttribute("rhythmJson", "{\"days\":{},\"hours\":{}}");
            model.addAttribute("momentum", List.of());
            model.addAttribute("keywordTrends", List.of());
            return "trends";
        }

        boolean hasCompetitors = user.getCompetitorChannels() != null && !user.getCompetitorChannels().isEmpty();
        model.addAttribute("hasCompetitors", hasCompetitors);

        // 1. Subscriber Benchmarking Line Chart JSON
        Map<String, Object> benchmarkData = competitorAnalyticsService.getSubscriberBenchmarkingData(user, user.getCompetitorChannels());
        try {
            model.addAttribute("benchmarkJson", objectMapper.writeValueAsString(benchmarkData));
        } catch (Exception e) {
            model.addAttribute("benchmarkJson", "{\"datasets\":[]}");
        }

        // 2. Posting Rhythm Chart JSON
        Map<String, Object> rhythmData = competitorAnalyticsService.calculatePostingRhythm(user.getCompetitorChannels());
        try {
            model.addAttribute("rhythmJson", objectMapper.writeValueAsString(rhythmData));
        } catch (Exception e) {
            model.addAttribute("rhythmJson", "{\"days\":{},\"hours\":{}}");
        }

        // 3. Topic Momentum
        List<Map<String, Object>> momentum = competitorAnalyticsService.calculateTopicMomentum(user.getCompetitorChannels());
        model.addAttribute("momentum", momentum);

        boolean isPro = "ROLE_PRO".equals(user.getRole());
        model.addAttribute("isPro", isPro);

        // 7. Viral Outliers Engine (Task 1.1)
        if (isPro) {
            List<Map<String, Object>> outliers = competitorAnalyticsService.findViralOutliers(user.getCompetitorChannels());
            model.addAttribute("viralOutliers", outliers);
        } else {
            model.addAttribute("viralOutliers", Collections.emptyList());
        }

        return "trends";
    }
}
