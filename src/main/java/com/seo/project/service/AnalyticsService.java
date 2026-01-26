package com.seo.project.service;

import com.seo.project.dto.VideoAnalytics;
import com.seo.project.dto.VideoAnalytics.AuditResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsService {

    private final WebClient webClient;
    private final ThumbnailService thumbnailService;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("${base.url}")
    private String baseUrl;

    public AnalyticsService(WebClient.Builder builder, ThumbnailService thumbnailService) {
        this.webClient = builder.build();
        this.thumbnailService = thumbnailService;
    }

    public VideoAnalytics analyzeVideo(String url) {
        String videoId = thumbnailService.extractVideoId(url);
        if (videoId == null) return null;

        // 1. Parallel Fetch: YouTube Data & RYD Data
        Mono<JsonNode> youtubeData = fetchYoutubeData(videoId);
        Mono<JsonNode> rydData = fetchRydData(videoId);

        // 2. Zip results together
        return Mono.zip(youtubeData, rydData)
            // ✅ Filter: If YouTube data is missing/error, stop the stream (block() returns null)
            .filter(tuple -> tuple.getT1().has("items") && !tuple.getT1().get("items").isEmpty())
            .map(tuple -> {
                JsonNode yt = tuple.getT1();
                JsonNode ryd = tuple.getT2();

                JsonNode item = yt.get("items").get(0);
                JsonNode snippet = item.get("snippet");
                JsonNode stats = item.get("statistics");

                // Extract Stats
                long views = parseLong(stats, "viewCount");
                long likes = parseLong(stats, "likeCount");
                long comments = parseLong(stats, "commentCount");
                long dislikes = ryd.has("dislikes") ? ryd.get("dislikes").asLong() : 0;

                // Extract Meta
                String title = snippet.get("title").asString();
                String desc = snippet.get("description").asString();
                List<String> tags = new ArrayList<>();
                if (snippet.has("tags")) {
                    snippet.get("tags").forEach(t -> tags.add(t.asString()));
                }

                // Calculations
                double engagementRate = views > 0 ? ((double) (likes + comments) / views) * 100 : 0;
                double sentiment = (likes + dislikes) > 0 ? ((double) likes / (likes + dislikes)) * 100 : 0;

                // SEO Audit
                List<AuditResult> audits = performAudit(title, desc, tags);
                int seoScore = (int) audits.stream().filter(AuditResult::passed).count() * (100 / Math.max(audits.size(), 1));

                return new VideoAnalytics(
                    videoId, title, snippet.get("channelTitle").asString(),
                    snippet.get("thumbnails").get("high").get("url").asString(),
                    snippet.get("publishedAt").asString().substring(0, 10),
                    views, likes, dislikes, comments,
                    engagementRate, sentiment, seoScore, audits
                );
            }).block(); // Returns null if filter fails
    }

    private Mono<JsonNode> fetchYoutubeData(String videoId) {
        return webClient.get()
                .uri(baseUrl + "/videos?part=snippet,statistics&id=" + videoId + "&key=" + apiKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                // ✅ FIX: Return empty object instead of null
                .onErrorReturn(JsonNodeFactory.instance.objectNode());
    }

    private Mono<JsonNode> fetchRydData(String videoId) {
        return webClient.get()
                .uri("https://returnyoutubedislikeapi.com/votes?videoId=" + videoId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                // ✅ FIX: Return empty object instead of null
                .onErrorReturn(JsonNodeFactory.instance.objectNode());
    }

    private List<AuditResult> performAudit(String title, String desc, List<String> tags) {
        List<AuditResult> results = new ArrayList<>();

        // 1. Title Length
        boolean titleLength = title.length() >= 20 && title.length() <= 70;
        results.add(new AuditResult(titleLength, titleLength ? "Title length is optimized." : "Title should be between 20-70 characters."));

        // 2. Tags Present
        boolean hasTags = !tags.isEmpty();
        results.add(new AuditResult(hasTags, hasTags ? "Video uses tags." : "No tags found. Add tags to improve reach."));

        // 3. Keyword Synergy (Title words in Tags)
        long matchCount = 0;
        if (hasTags) {
            String lowerTitle = title.toLowerCase();
            matchCount = tags.stream().filter(t -> lowerTitle.contains(t.toLowerCase())).count();
        }
        boolean synergy = matchCount >= 1;
        results.add(new AuditResult(synergy, synergy ? "Title keywords found in tags." : "Include your main title keywords in your tags."));

        // 4. Description Links
        boolean hasLinks = desc.contains("http://") || desc.contains("https://");
        results.add(new AuditResult(hasLinks, hasLinks ? "Description contains links (CTAs)." : "Add links to your description (Socials/Products)."));

        return results;
    }

    private long parseLong(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asLong() : 0;
    }
}