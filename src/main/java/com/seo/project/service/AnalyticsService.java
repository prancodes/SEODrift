package com.seo.project.service;

import com.seo.project.dto.VideoAnalytics;
import com.seo.project.dto.VideoAnalytics.AuditResult;
import com.seo.project.model.VideoAnalysis;
import com.seo.project.repository.UserRepository;
import com.seo.project.repository.VideoAnalysisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * AnalyticsService provides the core logic for harvesting video intelligence data
 * from multiple sources (YouTube Data API, Return YouTube Dislike API) and performing
 * a calculated SEO audit.
 */
@Slf4j
@Service
public class AnalyticsService {

    private final WebClient webClient;
    private final ThumbnailService thumbnailService;
    private final VideoAnalysisRepository videoAnalysisRepository;
    private final UserRepository userRepository;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("${base.url}")
    private String baseUrl;

    /**
     * Constructor injection for required dependencies.
     */
    public AnalyticsService(WebClient.Builder builder, 
                            ThumbnailService thumbnailService,
                            VideoAnalysisRepository videoAnalysisRepository,
                            UserRepository userRepository) {
        this.webClient = builder.build();
        this.thumbnailService = thumbnailService;
        this.videoAnalysisRepository = videoAnalysisRepository;
        this.userRepository = userRepository;
    }

    /**
     * Performs a full audit of a YouTube video URL.
     * Fetches metadata, calculates engagement, and persists results to the database if a user context is provided.
     * 
     * @param url The full YouTube video URL.
     * @param userEmail The email of the authenticated user (optional).
     * @return A VideoAnalytics DTO containing audit results, or null if the URL is invalid.
     */
    @Transactional
    public VideoAnalytics analyzeVideo(String url, String userEmail) {
        String videoId = thumbnailService.extractVideoId(url);
        if (videoId == null) {
            log.warn("Invalid URL provided for analysis: {}", url);
            return null;
        }

        log.debug("Starting analysis pipeline for video ID: [{}]", videoId);

        // Parallel Fetch: YouTube Data & RYD Data
        Mono<JsonNode> youtubeData = fetchYoutubeData(videoId);
        Mono<JsonNode> rydData = fetchRydData(videoId);

        // Zip results together to process as a single unit
        return Mono.zip(youtubeData, rydData)
            .filter(tuple -> tuple.getT1().has("items") && !tuple.getT1().get("items").isEmpty())
            .map(tuple -> {
                JsonNode yt = tuple.getT1();
                JsonNode ryd = tuple.getT2();

                JsonNode item = yt.get("items").get(0);
                JsonNode snippet = item.get("snippet");
                JsonNode stats = item.get("statistics");

                // Data Extraction
                long views = parseLong(stats, "viewCount");
                long likes = parseLong(stats, "likeCount");
                long comments = parseLong(stats, "commentCount");
                long dislikes = ryd.has("dislikes") ? ryd.get("dislikes").asLong() : 0;

                String title = snippet.get("title").asString();
                String desc = snippet.get("description").asString();
                List<String> tags = new ArrayList<>();
                if (snippet.has("tags")) {
                    snippet.get("tags").forEach(t -> tags.add(t.asString()));
                }

                // Metric Calculations
                double engagementRate = views > 0 ? ((double) (likes + comments) / views) * 100 : 0;
                double sentiment = (likes + dislikes) > 0 ? ((double) likes / (likes + dislikes)) * 100 : 0;

                // Perform SEO Audit
                List<AuditResult> audits = performAudit(title, desc, tags);
                int seoScore = (int) audits.stream().filter(AuditResult::passed).count() * (100 / Math.max(audits.size(), 1));

                VideoAnalytics analytics = new VideoAnalytics(
                    videoId, title, snippet.get("channelTitle").asString(),
                    snippet.get("thumbnails").get("high").get("url").asString(),
                    snippet.get("publishedAt").asString().substring(0, 10),
                    views, likes, dislikes, comments,
                    engagementRate, sentiment, seoScore, audits
                );

                // Persist to user history if authenticated
                if (userEmail != null) {
                    persistAnalysisToUserHistory(userEmail, videoId, title, snippet, url, seoScore, engagementRate, sentiment);
                }

                return analytics;
            }).block();
    }

    /**
     * Safely persists an audit record to the database under the specified user.
     */
    private void persistAnalysisToUserHistory(String userEmail, String videoId, String title, JsonNode snippet, 
                                            String url, int seoScore, double engagementRate, double sentiment) {
        log.debug("Persisting analysis history for user: {}", userEmail);
        userRepository.findByEmail(userEmail).ifPresentOrElse(user -> {
            VideoAnalysis entity = videoAnalysisRepository.findByUserAndVideoId(user, videoId)
                    .map(existing -> {
                        existing.setTitle(title);
                        existing.setChannelTitle(snippet.get("channelTitle").asString());
                        existing.setThumbnailUrl(snippet.get("thumbnails").get("high").get("url").asString());
                        existing.setVideoUrl(url);
                        existing.setSeoScore(seoScore);
                        existing.setEngagementRate(engagementRate);
                        existing.setSentimentScore(sentiment);
                        existing.setAnalyzedAt(java.time.LocalDateTime.now());
                        return existing;
                    })
                    .orElseGet(() -> VideoAnalysis.builder()
                            .videoId(videoId)
                            .title(title)
                            .channelTitle(snippet.get("channelTitle").asString())
                            .thumbnailUrl(snippet.get("thumbnails").get("high").get("url").asString())
                            .videoUrl(url)
                            .seoScore(seoScore)
                            .engagementRate(engagementRate)
                            .sentimentScore(sentiment)
                            .user(user)
                            .build());
            videoAnalysisRepository.save(entity);
            log.info("Analysis history entry saved (upsert): Video=[{}] User=[{}]", videoId, userEmail);
        }, () -> log.error("Integrity Error: Authentication context exists for [{}] but user record missing from database.", userEmail));
    }

    private Mono<JsonNode> fetchYoutubeData(String videoId) {
        log.debug("Fetching YouTube data for ID: {}", videoId);
        return webClient.get()
                .uri(baseUrl + "/videos?part=snippet,statistics&id=" + videoId + "&key=" + apiKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorReturn(JsonNodeFactory.instance.objectNode());
    }

    private Mono<JsonNode> fetchRydData(String videoId) {
        log.debug("Fetching RYD (Dislikes) data for ID: {}", videoId);
        return webClient.get()
                .uri("https://returnyoutubedislikeapi.com/votes?videoId=" + videoId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorReturn(JsonNodeFactory.instance.objectNode());
    }

    /**
     * Core SEO heuristic audit logic.
     */
    private List<AuditResult> performAudit(String title, String desc, List<String> tags) {
        List<AuditResult> results = new ArrayList<>();

        // 1. Title Length Heuristic
        boolean titleLength = title.length() >= 20 && title.length() <= 70;
        results.add(new AuditResult(titleLength, titleLength ? "Title length is optimized." : "Title should be between 20-70 characters."));

        // 2. Tag Meta-data Check
        boolean hasTags = !tags.isEmpty();
        results.add(new AuditResult(hasTags, hasTags ? "Video uses tags." : "No tags found. Add tags to improve reach."));

        // 3. Keyword Synergy Check
        long matchCount = 0;
        if (hasTags) {
            String lowerTitle = title.toLowerCase();
            matchCount = tags.stream().filter(t -> lowerTitle.contains(t.toLowerCase())).count();
        }
        boolean synergy = matchCount >= 1;
        results.add(new AuditResult(synergy, synergy ? "Title keywords found in tags." : "Include your main title keywords in your tags."));

        // 4. CTA (Call To Action) Check
        boolean hasLinks = desc.contains("http://") || desc.contains("https://");
        results.add(new AuditResult(hasLinks, hasLinks ? "Description contains links (CTAs)." : "Add links to your description (Socials/Products)."));

        return results;
    }

    private long parseLong(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asLong() : 0;
    }
}