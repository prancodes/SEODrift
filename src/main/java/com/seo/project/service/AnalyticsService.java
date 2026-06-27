package com.seo.project.service;

import com.seo.project.dto.VideoAnalytics;
import com.seo.project.dto.VideoAnalytics.AuditResult;
import com.seo.project.model.VideoAnalysis;
import com.seo.project.repository.UserRepository;
import com.seo.project.repository.VideoAnalysisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private final AiWorkspaceService aiWorkspaceService;

    private final AnalyticsService self;

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
                            UserRepository userRepository,
                            AiWorkspaceService aiWorkspaceService,
                            @Lazy AnalyticsService self) {
        this.webClient = builder.build();
        this.thumbnailService = thumbnailService;
        this.videoAnalysisRepository = videoAnalysisRepository;
        this.userRepository = userRepository;
        this.aiWorkspaceService = aiWorkspaceService;
        this.self = self;
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

        // Rate Limiting Logic for Free Users
        if (userEmail != null) {
            userRepository.findByEmail(userEmail).ifPresent(user -> {
                if (!"ROLE_PRO".equals(user.getRole())) {
                    LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
                    long count = videoAnalysisRepository.countByUserAndAnalyzedAtAfterAndVideoUrlNotLike(user, startOfDay, "ai-draft:%");
                    if (count >= 3) {
                        throw new RuntimeException("FREE_LIMIT_EXCEEDED");
                    }
                }
            });
        }

        // Invoke through proxy self-reference to trigger Spring AOP caching interception
        VideoAnalytics analytics = self.getCachedVideoAnalytics(videoId, url);

        // Persist to user history if authenticated
        if (analytics != null && userEmail != null) {
            persistAnalysisToUserHistory(userEmail, analytics, url);
        }

        return analytics;
    }

    /**
     * Harvests and analyzes video metadata. Wrapped in @Cacheable to hit Aiven Redis
     * cache for subsequent identical video requests across different platform users.
     */
    @Cacheable(value = "videoAnalytics", key = "#videoId", unless = "#result == null")
    public VideoAnalytics getCachedVideoAnalytics(String videoId, String url) {
        log.info("Cache miss for video ID: [{}]. Fetching fresh metrics from YouTube & RYD APIs.", videoId);

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
                boolean likesHidden = !stats.has("likeCount");
                Long likes = likesHidden ? null : parseLong(stats, "likeCount");
                long comments = parseLong(stats, "commentCount");
                long dislikes = ryd.has("dislikes") ? ryd.get("dislikes").asLong() : 0;

                String title = snippet.get("title").asString();
                String desc = snippet.get("description").asString();
                List<String> tags = new ArrayList<>();
                if (snippet.has("tags")) {
                    snippet.get("tags").forEach(t -> tags.add(t.asString()));
                }

                // Metric Calculations
                Double engagementRate = null;
                Double sentiment = null;
                if (likes != null) {
                    engagementRate = views > 0 ? ((double) (likes + comments) / views) * 100 : 0.0;
                    sentiment = (likes + dislikes) > 0 ? ((double) likes / (likes + dislikes)) * 100 : 0.0;
                }

                // Perform Dynamic AI SEO Audit using Gemini
                com.seo.project.dto.VideoAiAuditDto aiAudit = aiWorkspaceService.generateVideoAudit(title, desc, tags);
                
                // Map from Ai Audit Item to VideoAnalytics.AuditResult
                List<AuditResult> audits = aiAudit.audits().stream()
                        .map(auditItem -> new AuditResult(auditItem.passed(), auditItem.message()))
                        .collect(Collectors.toList());

                return new VideoAnalytics(
                    videoId, title, snippet.get("channelTitle").asString(),
                    snippet.get("thumbnails").get("high").get("url").asString(),
                    snippet.get("publishedAt").asString().substring(0, 10),
                    views, likes, dislikes, comments,
                    engagementRate, sentiment, likesHidden, aiAudit.seoScore(), audits
                );
            }).block();
    }

    /**
     * Safely persists an audit record to the database under the specified user.
     */
    private void persistAnalysisToUserHistory(String userEmail, VideoAnalytics analytics, String url) {
        log.debug("Persisting analysis history for user: {}", userEmail);
        userRepository.findByEmail(userEmail).ifPresentOrElse(user -> {
            VideoAnalysis entity = videoAnalysisRepository.findByUserAndVideoIdAndIsDeletedFalse(user, analytics.videoId())
                    .map(existing -> {
                        existing.setTitle(analytics.title());
                        existing.setChannelTitle(analytics.channelName());
                        existing.setThumbnailUrl(analytics.thumbnailUrl());
                        existing.setVideoUrl(url);
                        existing.setSeoScore(analytics.seoScore());
                        existing.setEngagementRate(analytics.engagementRate());
                        existing.setSentimentScore(analytics.sentimentScore());
                        existing.setAnalyzedAt(LocalDateTime.now());
                        return existing;
                    })
                    .orElseGet(() -> VideoAnalysis.builder()
                            .videoId(analytics.videoId())
                            .title(analytics.title())
                            .channelTitle(analytics.channelName())
                            .thumbnailUrl(analytics.thumbnailUrl())
                            .videoUrl(url)
                            .seoScore(analytics.seoScore())
                            .engagementRate(analytics.engagementRate())
                            .sentimentScore(analytics.sentimentScore())
                            .user(user)
                            .build());
            videoAnalysisRepository.save(entity);
            log.info("Analysis history entry saved (upsert): Video=[{}] User=[{}]", analytics.videoId(), userEmail);
        }, () -> log.error("Integrity Error: Authentication context exists for [{}] but user record missing from database.", userEmail));
    }

    private Mono<JsonNode> fetchYoutubeData(String videoId) {
        log.debug("Fetching YouTube data for ID: {}", videoId);
        return webClient.get()
                .uri(baseUrl + "/videos?part=snippet,statistics&id=" + videoId + "&key=" + apiKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("Error fetching YouTube data", e))
                .onErrorReturn(JsonNodeFactory.instance.objectNode());
    }

    private Mono<JsonNode> fetchRydData(String videoId) {
        log.debug("Fetching RYD (Dislikes) data for ID: {}", videoId);
        return webClient.get()
                .uri("https://returnyoutubedislikeapi.com/votes?videoId=" + videoId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("Error fetching RYD data", e))
                .onErrorReturn(JsonNodeFactory.instance.objectNode());
    }



    private long parseLong(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asLong() : 0;
    }
}