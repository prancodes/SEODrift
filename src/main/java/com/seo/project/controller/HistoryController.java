package com.seo.project.controller;

import com.seo.project.dto.YouTubeChannelDto;
import com.seo.project.model.VideoAnalysis;
import com.seo.project.repository.UserRepository;
import com.seo.project.repository.VideoAnalysisRepository;
import com.seo.project.service.ChannelHealthEvaluator;
import com.seo.project.service.YouTubeChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
public class HistoryController {

    private final VideoAnalysisRepository videoAnalysisRepository;
    private final UserRepository userRepository;
    private final YouTubeChannelService youtubeChannelService;
    private final ChannelHealthEvaluator channelHealthEvaluator;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public HistoryController(VideoAnalysisRepository videoAnalysisRepository,
                             UserRepository userRepository,
                             YouTubeChannelService youtubeChannelService,
                             ChannelHealthEvaluator channelHealthEvaluator,
                             OAuth2AuthorizedClientService authorizedClientService) {
        this.videoAnalysisRepository = videoAnalysisRepository;
        this.userRepository = userRepository;
        this.youtubeChannelService = youtubeChannelService;
        this.channelHealthEvaluator = channelHealthEvaluator;
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping("/history")
    public String showHistory(
            Authentication authentication,
            Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return "redirect:/";
        }

        // Safe lookup — returns null if user authenticated via One Tap (no OAuth2 code flow)
        OAuth2AuthorizedClient authorizedClient = authorizedClientService
                .loadAuthorizedClient("google", authentication.getName());

        String email = oauth2User.getAttribute("email");
        userRepository.findByEmail(email).ifPresent(user -> {
            List<VideoAnalysis> history = videoAnalysisRepository.findByUserOrderByAnalyzedAtDesc(user);
            long auditCount = history.size();
            double avgSeo = history.stream()
                    .mapToInt(a -> a.getSeoScore() != null ? a.getSeoScore() : 0)
                    .average()
                    .orElse(0.0);
            double avgEngagement = history.stream()
                    .mapToDouble(a -> a.getEngagementRate() != null ? a.getEngagementRate() : 0.0)
                    .average()
                    .orElse(0.0);

            // Channel Health
            String channelHealth = "Pending";
            // authorizedClient is null when user logged in via One Tap (no OAuth2 code flow)
            YouTubeChannelDto channelData = (authorizedClient != null)
                    ? youtubeChannelService.getChannelIntelligence(authorizedClient, email)
                    : null;
            if (channelData != null) {
                int score = channelHealthEvaluator.calculateHealthScore(channelData.recentUploads(), channelData.subscriberCount());
                channelHealth = channelHealthEvaluator.getHealthStatus(score);
            }

            model.addAttribute("channel", channelData);
            model.addAttribute("history", history != null ? history : new ArrayList<>());
            model.addAttribute("auditCount", auditCount);
            model.addAttribute("avgSeoScore", Math.round(avgSeo));
            model.addAttribute("avgEngagement", Math.round(avgEngagement * 100.0) / 100.0);
            model.addAttribute("channelHealth", channelHealth);
            model.addAttribute("user", user);
        });

        return "history";
    }
}

