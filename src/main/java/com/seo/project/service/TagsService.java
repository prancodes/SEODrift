package com.seo.project.service;

import tools.jackson.databind.JsonNode;
import com.seo.project.dto.VideoTagsInfo;
import com.seo.project.dto.TagsGeneratorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagsService {

    private final WebClient webClient;
    private final ThumbnailService thumbnailService;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("${base.url}")
    private String baseUrl;

    // Constructor Injection
    public TagsService(WebClient.Builder builder, ThumbnailService thumbnailService) {
        this.webClient = builder.build();
        this.thumbnailService = thumbnailService;
    }

    @Cacheable(value = "tagsGenerator", key = "#input", unless = "#result == null")
    public TagsGeneratorResponse generateTags(String input) {
        String videoId = thumbnailService.extractVideoId(input);

        if (videoId != null) {
            // Case 1: Input is a URL
            // We fetch the specific video (even if it's a Shorts, because the user asked for it)
            List<VideoTagsInfo> videos = fetchVideoDetails(List.of(videoId)); 
            
            if (videos.isEmpty()) return null; // Video likely has no tags or invalid ID
            
            VideoTagsInfo primary = videos.getFirst();
            // Fetch related videos based on primary video's title/tags
            List<VideoTagsInfo> related = fetchRelatedVideos(primary);
            
            return new TagsGeneratorResponse(primary, related);
        } else {
            // Case 2: Input is a Keyword
            // We search for videos, strictly enforcing -> MUST HAVE TAGS
            List<String> searchIds = searchVideoIds(input, 20); // Fetch 20 candidates
            if (searchIds.isEmpty()) return null;

            List<VideoTagsInfo> candidates = fetchVideoDetails(searchIds);
            
            if (candidates.isEmpty()) return null;

            VideoTagsInfo primary = candidates.removeFirst(); // Top result
            
            // Get next 3 related from the remaining candidates
            List<VideoTagsInfo> related = candidates.stream()
                                                   .limit(3)
                                                   .collect(Collectors.toList());

            return new TagsGeneratorResponse(primary, related);
        }
    }

    // Fetch details (Title, Tags)
    private List<VideoTagsInfo> fetchVideoDetails(List<String> videoIds) {
        if (videoIds.isEmpty()) return Collections.emptyList();

        // Request snippet only
        String url = baseUrl + "/videos?part=snippet&id=" 
                     + String.join(",", videoIds) + "&key=" + apiKey;

        JsonNode root = webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(JsonNode.class).block();
        List<VideoTagsInfo> results = new ArrayList<>();

        if (root != null && root.has("items")) {
            for (JsonNode item : root.get("items")) {
                JsonNode snippet = item.get("snippet");
                
                // 1. Check Tags
                List<String> tags = new ArrayList<>();
                if (snippet.has("tags") && !snippet.get("tags").isEmpty()) {
                    snippet.get("tags").forEach(t -> tags.add(t.asString()));
                } else {
                    // Skip if no tags found (User wants only videos with tags)
                    continue; 
                }

                results.add(new VideoTagsInfo(
                    item.get("id").asString(),
                    snippet.get("title").asString(),
                    snippet.get("channelTitle").asString(),
                    snippet.get("thumbnails").has("medium") ? snippet.get("thumbnails").get("medium").get("url").asString() : "",
                    tags
                ));
            }
        }
        return results;
    }

    private List<VideoTagsInfo> fetchRelatedVideos(VideoTagsInfo primary) {
        // Search using the primary video's best tags or title
        String query = primary.title();
        if (primary.tags() != null && !primary.tags().isEmpty()) {
            query = primary.tags().getFirst() + " " + primary.title(); 
        }
        
        // Fetch 20 candidates
        List<String> relatedIds = searchVideoIds(query, 20);
        
        // Remove primary ID if it appears in results
        relatedIds.remove(primary.videoId());
        
        // Fetch details
        List<VideoTagsInfo> validVideos = fetchVideoDetails(relatedIds);
        
        // Return top 3
        return validVideos.stream().limit(3).collect(Collectors.toList());
    }

    private List<String> searchVideoIds(String query, int maxResults) {
        // FIX: Properly encode the query to handle pipes '|', emojis, and symbols
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        
        String url = baseUrl + "/search?part=id&type=video&q=" 
                     + encodedQuery + "&maxResults=" + maxResults + "&key=" + apiKey;

        JsonNode root = webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .block();
        List<String> ids = new ArrayList<>();

        if (root != null && root.has("items")) {
            for (JsonNode item : root.get("items")) {
                if (item.has("id") && item.get("id").has("videoId")) {
                    ids.add(item.get("id").get("videoId").asString());
                }
            }
        }
        return ids;
    }
}