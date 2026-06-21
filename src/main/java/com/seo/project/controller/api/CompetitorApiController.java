package com.seo.project.controller.api;

import com.seo.project.model.CompetitorChannel;
import com.seo.project.model.User;
import com.seo.project.repository.UserRepository;
import com.seo.project.service.CompetitorScraperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/competitors")
public class CompetitorApiController {

    private final UserRepository userRepository;
    private final CompetitorScraperService competitorScraperService;

    public CompetitorApiController(UserRepository userRepository, 
                                   CompetitorScraperService competitorScraperService) {
        this.userRepository = userRepository;
        this.competitorScraperService = competitorScraperService;
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, String>> addCompetitor(
            Authentication authentication, 
            @RequestBody Map<String, String> request) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        String email = oauth2User.getAttribute("email");
        Optional<User> userOpt = userRepository.findWithCompetitorsByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();
        String channelId = request.get("channelId");
        
        if (channelId == null || channelId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Channel ID or Handle is required"));
        }

        // Fetch and resolve competitor channel (supports UC... ID and @handle)
        CompetitorChannel competitor;
        try {
            competitor = competitorScraperService.getOrCreateCompetitor(channelId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to fetch competitor: ", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve competitor details from YouTube. Please verify the channel handle/ID."));
        }

        // Check if already tracking the resolved competitor channel ID
        final String resolvedChannelId = competitor.getChannelId();
        boolean alreadyTracking = user.getCompetitorChannels().stream()
                .anyMatch(c -> c.getChannelId().equals(resolvedChannelId));
                
        if (alreadyTracking) {
            return ResponseEntity.badRequest().body(Map.of("error", "Already tracking this competitor"));
        }

        user.getCompetitorChannels().add(competitor);
        userRepository.save(user);

        log.info("User {} added competitor channel {} (resolved to: {})", email, channelId, resolvedChannelId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Competitor added successfully");
        return ResponseEntity.ok(response);
    }
}
