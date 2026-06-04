package com.seo.project.controller.api;

import com.seo.project.model.CompetitorChannel;
import com.seo.project.model.User;
import com.seo.project.repository.CompetitorChannelRepository;
import com.seo.project.repository.UserRepository;
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
    private final CompetitorChannelRepository competitorChannelRepository;

    public CompetitorApiController(UserRepository userRepository, 
                                   CompetitorChannelRepository competitorChannelRepository) {
        this.userRepository = userRepository;
        this.competitorChannelRepository = competitorChannelRepository;
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, String>> addCompetitor(
            Authentication authentication, 
            @RequestBody Map<String, String> request) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        String email = oauth2User.getAttribute("email");
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();
        String channelId = request.get("channelId");
        
        if (channelId == null || channelId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Channel ID is required"));
        }

        // Check if already tracking
        boolean alreadyTracking = user.getCompetitorChannels().stream()
                .anyMatch(c -> c.getChannelId().equals(channelId));
                
        if (alreadyTracking) {
            return ResponseEntity.badRequest().body(Map.of("error", "Already tracking this competitor"));
        }

        // Find existing or create placeholder
        CompetitorChannel competitor = competitorChannelRepository.findByChannelId(channelId)
                .orElseGet(() -> {
                    CompetitorChannel newChannel = new CompetitorChannel();
                    newChannel.setChannelId(channelId);
                    newChannel.setTitle("Loading..."); // Placeholder until scraper updates it
                    return competitorChannelRepository.save(newChannel);
                });

        user.getCompetitorChannels().add(competitor);
        userRepository.save(user);

        log.info("User {} added competitor channel {}", email, channelId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Competitor added successfully");
        return ResponseEntity.ok(response);
    }
}
