package com.seo.project.controller;

import com.seo.project.model.KeywordTrend;
import com.seo.project.model.SavedKeyword;
import com.seo.project.model.User;
import com.seo.project.repository.KeywordTrendRepository;
import com.seo.project.repository.SavedKeywordRepository;
import com.seo.project.repository.UserRepository;
import com.seo.project.service.CompetitorAnalyticsService;
import com.seo.project.service.AiWorkspaceService;
import com.seo.project.dto.KeywordAiAnalysisDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class KeywordVelocityController {

    private final UserRepository userRepository;
    private final SavedKeywordRepository savedKeywordRepository;
    private final KeywordTrendRepository keywordTrendRepository;
    private final CompetitorAnalyticsService competitorAnalyticsService;
    private final AiWorkspaceService aiWorkspaceService;

    public KeywordVelocityController(UserRepository userRepository,
                                     SavedKeywordRepository savedKeywordRepository,
                                     KeywordTrendRepository keywordTrendRepository,
                                     CompetitorAnalyticsService competitorAnalyticsService,
                                     AiWorkspaceService aiWorkspaceService) {
        this.userRepository = userRepository;
        this.savedKeywordRepository = savedKeywordRepository;
        this.keywordTrendRepository = keywordTrendRepository;
        this.competitorAnalyticsService = competitorAnalyticsService;
        this.aiWorkspaceService = aiWorkspaceService;
    }

    /**
     * Renders the dedicated Keyword Search Velocity page.
     */
    @GetMapping("/keywords")
    public String showKeywordsPage(Authentication authentication, Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            log.warn("Unauthorized access to /keywords. Redirecting to home.");
            return "redirect:/";
        }

        String email = oauth2User.getAttribute("email");
        Optional<User> userOpt = userRepository.findWithCompetitorsByEmail(email);

        if (userOpt.isEmpty()) {
            return "redirect:/";
        }

        User user = userOpt.get();
        model.addAttribute("user", user);

        // Fetch saved keywords and their latest trends
        List<SavedKeyword> savedKeywords = savedKeywordRepository.findByUserOrderBySavedAtDesc(user);
        model.addAttribute("savedKeywords", savedKeywords);

        List<KeywordTrend> trendsList = new ArrayList<>();
        for (SavedKeyword sk : savedKeywords) {
            List<KeywordTrend> trends = keywordTrendRepository.findByKeywordOrderByRecordedDateDesc(sk.getKeyword());
            if (trends.isEmpty()) {
                // Scrape live if no trend snapshot exists
                KeywordTrend newTrend = competitorAnalyticsService.fetchKeywordTrend(sk.getKeyword());
                trendsList.add(newTrend);
            } else {
                trendsList.add(trends.get(0));
            }
        }
        model.addAttribute("keywordTrends", trendsList);

        return "keywords";
    }

    /**
     * Endpoint to retrieve historical trend data for Chart.js plotting on the frontend.
     */
    @GetMapping("/api/keywords/{keyword}/history")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getKeywordHistory(Authentication authentication,
                                                                       @PathVariable String keyword) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String cleanedKeyword = keyword.trim().toLowerCase();
        List<KeywordTrend> trends = keywordTrendRepository.findByKeywordOrderByRecordedDateDesc(cleanedKeyword);
        
        // Reverse to ascending order for chronological charting
        List<KeywordTrend> sortedTrends = new ArrayList<>(trends);
        Collections.reverse(sortedTrends);

        List<Map<String, Object>> result = sortedTrends.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("recordedDate", t.getRecordedDate().toString());
            map.put("videoCountThisMonth", t.getVideoCountThisMonth());
            map.put("videoCountLastMonth", t.getVideoCountLastMonth());
            map.put("growthRate", t.getGrowthRate());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint to retrieve historical trend data for ALL saved keywords of the user.
     */
    @GetMapping("/api/keywords/all-history")
    @ResponseBody
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getAllKeywordsHistory(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return ResponseEntity.status(401).build();
        }

        String email = oauth2User.getAttribute("email");
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        List<SavedKeyword> savedKeywords = savedKeywordRepository.findByUserOrderBySavedAtDesc(user);
        List<String> keywords = savedKeywords.stream()
                .map(SavedKeyword::getKeyword)
                .collect(Collectors.toList());

        if (keywords.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyMap());
        }

        List<KeywordTrend> allTrends = keywordTrendRepository.findByKeywordInOrderByRecordedDateDesc(keywords);
        
        Map<String, List<KeywordTrend>> grouped = allTrends.stream()
                .collect(Collectors.groupingBy(KeywordTrend::getKeyword));

        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        for (String kw : keywords) {
            List<KeywordTrend> trends = grouped.getOrDefault(kw, Collections.emptyList());
            List<KeywordTrend> sortedTrends = new ArrayList<>(trends);
            Collections.reverse(sortedTrends);

            List<Map<String, Object>> points = sortedTrends.stream().map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("recordedDate", t.getRecordedDate().toString());
                map.put("videoCountThisMonth", t.getVideoCountThisMonth());
                map.put("videoCountLastMonth", t.getVideoCountLastMonth());
                map.put("growthRate", t.getGrowthRate());
                return map;
            }).collect(Collectors.toList());

            result.put(kw, points);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Saves a new target keyword for the user and triggers an immediate trend scrape.
     */
    @PostMapping("/api/keywords/add")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addKeyword(Authentication authentication,
                                                          @RequestBody Map<String, String> request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = oauth2User.getAttribute("email");
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        String keyword = request.get("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Keyword cannot be empty"));
        }
        keyword = keyword.trim().toLowerCase();

        // Check if keyword is already saved
        Optional<SavedKeyword> existing = savedKeywordRepository.findByUserAndKeyword(user, keyword);
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Keyword is already saved"));
        }

        SavedKeyword savedKeyword = SavedKeyword.builder()
                .keyword(keyword)
                .user(user)
                .savedAt(LocalDateTime.now())
                .build();
        try {
            savedKeywordRepository.saveAndFlush(savedKeyword);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate keyword save attempted for user: {}, keyword: {}", user.getEmail(), keyword);
            return ResponseEntity.badRequest().body(Map.of("error", "Keyword is already saved"));
        }

        // Trigger immediate fetch of KeywordTrend metrics
        competitorAnalyticsService.fetchKeywordTrend(keyword);

        return ResponseEntity.ok(Map.of("message", "Keyword saved successfully"));
    }

    /**
     * Removes a saved keyword from the user's list.
     */
    @PostMapping("/api/keywords/delete")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteKeyword(Authentication authentication,
                                                             @RequestBody Map<String, String> request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = oauth2User.getAttribute("email");
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        String keyword = request.get("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Keyword is required"));
        }
        keyword = keyword.trim().toLowerCase();

        Optional<SavedKeyword> savedKeyword = savedKeywordRepository.findByUserAndKeyword(user, keyword);
        if (savedKeyword.isPresent()) {
            savedKeywordRepository.delete(savedKeyword.get());
            return ResponseEntity.ok(Map.of("message", "Keyword removed successfully"));
        }

        return ResponseEntity.status(404).body(Map.of("error", "Keyword not found"));
    }

    @GetMapping("/api/keywords/{keyword}/ai-analysis")
    @ResponseBody
    public ResponseEntity<KeywordAiAnalysisDto> getKeywordAiAnalysis(Authentication authentication,
                                                                     @PathVariable String keyword,
                                                                     @RequestParam int volume,
                                                                     @RequestParam double growth) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String cleanedKeyword = keyword.trim().toLowerCase();
        List<KeywordTrend> trends = keywordTrendRepository.findByKeywordOrderByRecordedDateDesc(cleanedKeyword);
        
        KeywordTrend latestTrend = null;
        if (!trends.isEmpty()) {
            latestTrend = trends.get(0);
        }

        // 1. Serve from DB cache if present
        if (latestTrend != null && latestTrend.getAiDifficulty() != null) {
            log.info("Serving cached Gemini SEO analysis from DB for keyword: '{}'", cleanedKeyword);
            return ResponseEntity.ok(new KeywordAiAnalysisDto(
                    latestTrend.getAiDifficulty(),
                    latestTrend.getAiCompetitionAdvice(),
                    latestTrend.getAiGrowthPotential(),
                    latestTrend.getAiSeoAdvice()
            ));
        }

        // 2. Fallback to Gemini API call
        KeywordAiAnalysisDto analysis = aiWorkspaceService.analyzeKeyword(
                cleanedKeyword,
                volume,
                growth
        );

        // 3. Cache results to DB
        if (latestTrend != null) {
            latestTrend.setAiDifficulty(analysis.difficulty());
            latestTrend.setAiCompetitionAdvice(analysis.competitionAdvice());
            latestTrend.setAiGrowthPotential(analysis.growthPotential());
            latestTrend.setAiSeoAdvice(analysis.seoAdvice());
            keywordTrendRepository.save(latestTrend);
            log.info("Cached fresh Gemini SEO analysis in DB for keyword: '{}'", cleanedKeyword);
        }

        return ResponseEntity.ok(analysis);
    }
}
