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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.Optional;

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
        var userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return "redirect:/";
        }
        
        var user = userOpt.get();
        List<VideoAnalysis> history = videoAnalysisRepository.findByUserOrderByAnalyzedAtDesc(user);
        long auditCount = history != null ? history.size() : 0;
        double avgSeo = history != null ? history.stream()
                .mapToInt(a -> a.getSeoScore() != null ? a.getSeoScore() : 0)
                .average()
                .orElse(0.0) : 0.0;
        double avgEngagement = history != null ? history.stream()
                .mapToDouble(a -> a.getEngagementRate() != null ? a.getEngagementRate() : 0.0)
                .average()
                .orElse(0.0) : 0.0;

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

        return "history";
    }

    @DeleteMapping("/api/history/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteHistoryItem(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = oauth2User.getAttribute("email");
        Optional<com.seo.project.model.User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        Optional<VideoAnalysis> analysisOpt = videoAnalysisRepository.findById(id);
        if (analysisOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Item not found"));
        }

        VideoAnalysis analysis = analysisOpt.get();
        // Security check: Verify owner
        if (!analysis.getUser().getId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        videoAnalysisRepository.delete(analysis);
        log.info("User [{}] deleted history item [{}]", email, id);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Item deleted successfully"));
    }
}

