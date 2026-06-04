package com.seo.project.service;

import com.seo.project.dto.YouTubeChannelDto;
import com.seo.project.dto.YouTubeVideoDto;
import com.seo.project.model.User;
import com.seo.project.model.UserChannelSnapshot;
import com.seo.project.repository.UserChannelSnapshotRepository;
import com.seo.project.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class YouTubeChannelService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final UserChannelSnapshotRepository snapshotRepository;

    @Value("${base.url}")
    private String youtubeApiBaseUrl;

    public YouTubeChannelService(WebClient.Builder builder, 
                                 UserRepository userRepository, 
                                 UserChannelSnapshotRepository snapshotRepository) {
        this.webClient = builder.build();
        this.userRepository = userRepository;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Orchestrates the retrieval of user channel intelligence.
     * Uses OAuth2 tokens to fetch live data. Uses DB fallback if the API fails or quota is exceeded.
     */
    public YouTubeChannelDto getChannelIntelligence(OAuth2AuthorizedClient authorizedClient, String userEmail) {
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            log.warn("User {} not found in database.", userEmail);
            return null;
        }
        User user = userOpt.get();
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        try {
            // 1. Fetch channel details
            YouTubeChannelDto channelDto = fetchChannelData(accessToken);
            
            if (channelDto != null) {
                // 2. Fetch recent uploads
                List<YouTubeVideoDto> recentUploads = fetchRecentUploads(accessToken, channelDto.uploadsPlaylistId());
                
                // 3. Try to fetch Analytics (Geo distribution)
                Map<String, Double> geoData = fetchGeoDemographics(accessToken, channelDto.country());
                
                // Update the DTO with uploads and geo data
                channelDto = new YouTubeChannelDto(
                        channelDto.channelId(),
                        channelDto.title(),
                        channelDto.customUrl(),
                        channelDto.avatarUrl(),
                        channelDto.uploadsPlaylistId(),
                        channelDto.subscriberCount(),
                        channelDto.viewCount(),
                        channelDto.videoCount(),
                        channelDto.country(),
                        geoData,
                        recentUploads
                );

                // 4. Save/Update cache and snapshots
                updateUserCacheAndSnapshots(user, channelDto);
                
                return channelDto;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch live YouTube data for {}: {}. Falling back to database cache.", userEmail, e.getMessage());
        }

        // Fallback to database cache if API fails
        return buildDtoFromCache(user);
    }

    private YouTubeChannelDto fetchChannelData(String accessToken) {
        String url = youtubeApiBaseUrl + "/channels?mine=true&part=snippet,contentDetails,statistics";
        
        JsonNode root = webClient.get()
                .uri(url)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root != null && root.has("items") && !root.get("items").isEmpty()) {
            JsonNode item = root.get("items").get(0);
            JsonNode snippet = item.get("snippet");
            JsonNode stats = item.get("statistics");
            JsonNode contentDetails = item.get("contentDetails");

            String channelId = item.get("id").asString();
            String title = snippet.get("title").asString();
            String customUrl = snippet.has("customUrl") ? snippet.get("customUrl").asString() : "";
            String avatarUrl = "";
            if (snippet.has("thumbnails")) {
                JsonNode thumbs = snippet.get("thumbnails");
                if (thumbs.has("high")) {
                    avatarUrl = thumbs.get("high").get("url").asString();
                } else if (thumbs.has("medium")) {
                    avatarUrl = thumbs.get("medium").get("url").asString();
                } else if (thumbs.has("default")) {
                    avatarUrl = thumbs.get("default").get("url").asString();
                }
            }
                    
            String uploadsPlaylistId = contentDetails.get("relatedPlaylists").has("uploads")
                    ? contentDetails.get("relatedPlaylists").get("uploads").asString() : "";

            long subscriberCount = parseLong(stats, "subscriberCount");
            long viewCount = parseLong(stats, "viewCount");
            long videoCount = parseLong(stats, "videoCount");
            String country = snippet.has("country") ? snippet.get("country").asString() : "";

            return new YouTubeChannelDto(channelId, title, customUrl, avatarUrl, uploadsPlaylistId, 
                    subscriberCount, viewCount, videoCount, country, new HashMap<>(), new ArrayList<>());
        }
        return null;
    }

    private List<YouTubeVideoDto> fetchRecentUploads(String accessToken, String uploadsPlaylistId) {
        if (uploadsPlaylistId == null || uploadsPlaylistId.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 1: Get Video IDs from PlaylistItems
        String playlistUrl = youtubeApiBaseUrl + "/playlistItems?playlistId=" + uploadsPlaylistId + "&part=snippet,contentDetails&maxResults=10";
        JsonNode playlistRoot = webClient.get()
                .uri(playlistUrl)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<String> videoIds = new ArrayList<>();
        if (playlistRoot != null && playlistRoot.has("items")) {
            for (JsonNode item : playlistRoot.get("items")) {
                videoIds.add(item.get("contentDetails").get("videoId").asString());
            }
        }

        if (videoIds.isEmpty()) return new ArrayList<>();

        // Step 2: Get Video Stats for those IDs
        String videosUrl = youtubeApiBaseUrl + "/videos?id=" + String.join(",", videoIds) + "&part=snippet,statistics";
        JsonNode videosRoot = webClient.get()
                .uri(videosUrl)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<YouTubeVideoDto> recentUploads = new ArrayList<>();
        if (videosRoot != null && videosRoot.has("items")) {
            for (JsonNode item : videosRoot.get("items")) {
                JsonNode snippet = item.get("snippet");
                JsonNode stats = item.get("statistics");

                String videoId = item.get("id").asString();
                String title = snippet.get("title").asString();
                String publishedAt = snippet.get("publishedAt").asString().substring(0, 10);
                String thumbnailUrl = snippet.get("thumbnails").has("medium") 
                        ? snippet.get("thumbnails").get("medium").get("url").asString() : "";

                long views = parseLong(stats, "viewCount");
                long likes = parseLong(stats, "likeCount");
                long comments = parseLong(stats, "commentCount");
                
                double engagementRate = views > 0 ? ((double)(likes + comments) / views) * 100 : 0.0;
                
                // We use AnalyticsService's audit indirectly by checking tags
                List<String> tags = new ArrayList<>();
                if (snippet.has("tags")) {
                    snippet.get("tags").forEach(t -> tags.add(t.asString()));
                }
                
                // Simple SEO proxy logic for the dashboard view
                int seoScore = 0;
                if (title.length() >= 20 && title.length() <= 70) seoScore += 40;
                if (!tags.isEmpty()) seoScore += 30;
                if (snippet.has("description") && snippet.get("description").asString().contains("http")) seoScore += 30;

                recentUploads.add(new YouTubeVideoDto(videoId, title, publishedAt, thumbnailUrl, views, likes, comments, engagementRate, seoScore));
            }
        }
        return recentUploads;
    }

    private Map<String, Double> fetchGeoDemographics(String accessToken, String channelCountry) {
        Map<String, Double> geoData = new HashMap<>();
        long totalGeoViews = 0;
        try {
            // YouTube Analytics API Call
            // Note: Requires https://www.googleapis.com/auth/yt-analytics.readonly scope
            String today = java.time.LocalDate.now().toString();
            String lastMonth = java.time.LocalDate.now().minusDays(30).toString();
            
            String analyticsUrl = "https://youtubeanalytics.googleapis.com/v2/reports?ids=channel==MINE" +
                    "&startDate=" + lastMonth + "&endDate=" + today +
                    "&metrics=views&dimensions=country&sort=-views&maxResults=10";

            JsonNode analyticsRoot = webClient.get()
                    .uri(analyticsUrl)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (analyticsRoot != null && analyticsRoot.has("rows")) {
                for (JsonNode row : analyticsRoot.get("rows")) {
                    totalGeoViews += row.get(1).asLong();
                }
                
                if (totalGeoViews > 0) {
                    for (JsonNode row : analyticsRoot.get("rows")) {
                        String countryCode = row.get(0).asString(); // e.g. "US"
                        long views = row.get(1).asLong();
                        double percentage = ((double) views / totalGeoViews) * 100.0;
                        geoData.put(countryCode, percentage);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("YouTube Analytics API fetch failed. Scope may be missing or channel has no data. Error: {}", e.getMessage());
        }

        if (geoData.isEmpty() || totalGeoViews < 10) {
            geoData.clear();
            String fallbackCountry = (channelCountry != null && !channelCountry.trim().isEmpty()) ? channelCountry.trim().toUpperCase() : "IN";
            geoData.put(fallbackCountry, 100.0);
            log.info("No demographics data found. Highlighting channel country: {}", fallbackCountry);
        }
        return geoData;
    }

    private void updateUserCacheAndSnapshots(User user, YouTubeChannelDto dto) {
        // Update user cache
        user.setYoutubeChannelId(dto.channelId());
        user.setYoutubeChannelTitle(dto.title());
        user.setYoutubeCustomUrl(dto.customUrl());
        user.setYoutubeAvatarUrl(dto.avatarUrl());
        user.setYoutubeUploadsPlaylistId(dto.uploadsPlaylistId());
        user.setYoutubeSubscriberCount(dto.subscriberCount());
        user.setYoutubeViewCount(dto.viewCount());
        user.setYoutubeVideoCount(dto.videoCount());
        user.setYoutubeLastUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);

        // Check if we need to record a snapshot (rate-limit snapshots to max 1 per 12 hours)
        boolean hasRecentSnapshot = snapshotRepository.existsByUserAndRecordedAtAfter(user, LocalDateTime.now().minus(12, ChronoUnit.HOURS));
        
        if (!hasRecentSnapshot) {
            UserChannelSnapshot snapshot = UserChannelSnapshot.builder()
                    .user(user)
                    .subscriberCount(dto.subscriberCount())
                    .viewCount(dto.viewCount())
                    .videoCount(dto.videoCount())
                    .build();
            snapshotRepository.save(snapshot);
            log.info("Recorded new channel metrics snapshot for user {}", user.getEmail());
        }
    }

    private YouTubeChannelDto buildDtoFromCache(User user) {
        if (user.getYoutubeChannelId() == null) {
            return null; // No cache available
        }
        
        log.info("Serving channel metrics from database cache for user {}", user.getEmail());
        return new YouTubeChannelDto(
                user.getYoutubeChannelId(),
                user.getYoutubeChannelTitle(),
                user.getYoutubeCustomUrl(),
                user.getYoutubeAvatarUrl(),
                user.getYoutubeUploadsPlaylistId(),
                user.getYoutubeSubscriberCount() != null ? user.getYoutubeSubscriberCount() : 0,
                user.getYoutubeViewCount() != null ? user.getYoutubeViewCount() : 0,
                user.getYoutubeVideoCount() != null ? user.getYoutubeVideoCount() : 0,
                "", // No country in user cache entity
                new HashMap<>(), // Can't cache geo data easily in the user table, return empty
                new ArrayList<>() // Can't cache recent uploads easily in the user table, return empty
        );
    }

    private long parseLong(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asLong() : 0;
    }
}
