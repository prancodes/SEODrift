package com.seo.project.controller;

import com.seo.project.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.seo.project.dto.YouTubeChannelDto;
import com.seo.project.service.ChannelHealthEvaluator;
import com.seo.project.service.YouTubeChannelService;
import com.seo.project.model.UserChannelSnapshot;
import com.seo.project.repository.UserChannelSnapshotRepository;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import tools.jackson.databind.ObjectMapper;

/**
 * DashboardController handles the retrieval and display of user-specific data,
 * primarily the creator console dashboard and personal account information.
 */
@Slf4j
@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final YouTubeChannelService youtubeChannelService;
    private final ChannelHealthEvaluator channelHealthEvaluator;
    private final UserChannelSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public DashboardController(UserRepository userRepository,
            YouTubeChannelService youtubeChannelService,
            ChannelHealthEvaluator channelHealthEvaluator,
            UserChannelSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.userRepository = userRepository;
        this.youtubeChannelService = youtubeChannelService;
        this.channelHealthEvaluator = channelHealthEvaluator;
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
        this.authorizedClientService = authorizedClientService;
    }

    /**
     * Renders the user dashboard.
     * Orchestrates data retrieval from PostgreSQL based on the authenticated Google
     * ID.
     */
    @GetMapping("/dashboard")
    public String showDashboard(
            Authentication authentication,
            Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            log.warn("Unauthorized access attempt to /dashboard. Redirecting to home.");
            return "redirect:/";
        }

        // Safe lookup — returns null if user authenticated via One Tap (no OAuth2 code flow)
        OAuth2AuthorizedClient authorizedClient = authorizedClientService
                .loadAuthorizedClient("google", authentication.getName());

        String email = oauth2User.getAttribute("email");
        log.info("Accessing Dashboard for user: [{}]", email);

        userRepository.findWithCompetitorsByEmail(email).ifPresentOrElse(user -> {
            // 1. YouTube Channel Live Data
            // authorizedClient is null when user logged in via One Tap (no OAuth2 code flow)
            YouTubeChannelDto channelData = (authorizedClient != null)
                    ? youtubeChannelService.getChannelIntelligence(authorizedClient, email)
                    : null;
            boolean hasChannel = (channelData != null);
            model.addAttribute("hasChannel", hasChannel);

            if (channelData != null) {
                model.addAttribute("channel", channelData);

                // Calculate accurate health score from real uploads
                int score = channelHealthEvaluator.calculateHealthScore(channelData.recentUploads(),
                        channelData.subscriberCount());
                String health = channelHealthEvaluator.getHealthStatus(score);

                model.addAttribute("healthScore", score);
                model.addAttribute("channelHealth", health);

                try {
                    model.addAttribute("recentUploadsJson",
                            objectMapper.writeValueAsString(channelData.recentUploads()));
                    model.addAttribute("geoDistributionJson",
                            objectMapper.writeValueAsString(channelData.geoDistribution()));
                } catch (Exception e) {
                    model.addAttribute("recentUploadsJson", "[]");
                    model.addAttribute("geoDistributionJson", "{}");
                }
            } else {
                model.addAttribute("channelHealth", "Pending");
                model.addAttribute("healthScore", 0);
                model.addAttribute("recentUploadsJson", "[]");
                model.addAttribute("geoDistributionJson", "{}");
            }

            // 2. User Snapshots (Historical Growth)
            List<UserChannelSnapshot> snapshots = snapshotRepository.findByUserOrderByRecordedAtAsc(user);

            model.addAttribute("user", user);
            model.addAttribute("snapshots", snapshots);
            try {
                List<Map<String, Object>> snapshotList = snapshots.stream().map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("subscriberCount", s.getSubscriberCount());
                    map.put("viewCount", s.getViewCount());
                    map.put("videoCount", s.getVideoCount());
                    map.put("recordedAt", s.getRecordedAt() != null ? s.getRecordedAt().toString() : null);
                    return map;
                }).collect(Collectors.toList());
                model.addAttribute("snapshotsJson", objectMapper.writeValueAsString(snapshotList));
            } catch (Exception e) {
                model.addAttribute("snapshotsJson", "[]");
            }
        }, () -> {
            model.addAttribute("hasChannel", false);
            model.addAttribute("channelHealth", "Pending");
            model.addAttribute("recentUploadsJson", "[]");
            model.addAttribute("geoDistributionJson", "{}");
            model.addAttribute("snapshotsJson", "[]");
            model.addAttribute("user", com.seo.project.model.User.builder()
                    .name(oauth2User.getAttribute("name"))
                    .email(email)
                    .build());
        });

        return "dashboard";
    }
}
