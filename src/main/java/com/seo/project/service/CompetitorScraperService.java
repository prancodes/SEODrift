package com.seo.project.service;

import com.seo.project.model.CompetitorChannel;
import com.seo.project.model.CompetitorSnapshot;
import com.seo.project.model.CompetitorVideo;
import com.seo.project.repository.CompetitorChannelRepository;
import com.seo.project.repository.CompetitorSnapshotRepository;
import com.seo.project.repository.CompetitorVideoRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CompetitorScraperService {

    private final WebClient webClient;
    private final CompetitorChannelRepository competitorChannelRepository;
    private final CompetitorSnapshotRepository competitorSnapshotRepository;
    private final CompetitorVideoRepository competitorVideoRepository;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("${base.url}")
    private String youtubeApiBaseUrl;

    public CompetitorScraperService(WebClient.Builder webClientBuilder,
                                     CompetitorChannelRepository competitorChannelRepository,
                                     CompetitorSnapshotRepository competitorSnapshotRepository,
                                     CompetitorVideoRepository competitorVideoRepository) {
        this.webClient = webClientBuilder.build();
        this.competitorChannelRepository = competitorChannelRepository;
        this.competitorSnapshotRepository = competitorSnapshotRepository;
        this.competitorVideoRepository = competitorVideoRepository;
    }

    /**
     * Daily scheduled competitor scraping task.
     * Starts at 2:00 AM daily. Synchronized across instances via ShedLock.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "competitorScraperLock", lockAtMostFor = "15m", lockAtLeastFor = "2m")
    @Transactional
    public void scrapeAllCompetitorsScheduled() {
        log.info("Starting scheduled competitor scraping...");
        List<CompetitorChannel> competitors = competitorChannelRepository.findAll();
        for (CompetitorChannel competitor : competitors) {
            try {
                scrapeChannel(competitor.getChannelId());
                // Simple gap to avoid excessive API load/rate limits
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("Error scraping competitor {}: {}", competitor.getChannelId(), e.getMessage());
            }
        }
        log.info("Finished scheduled competitor scraping.");
    }

    /**
     * Scrapes a single YouTube channel by ID or Handle, updates its details, records a snapshot, and indexes its top recent videos.
     */
    @Transactional
    public CompetitorChannel getOrCreateCompetitor(String input) {
        String queryId = input.trim();
        log.info("Resolving and scraping competitor channel: {}", queryId);

        try {
            // 1. Fetch channel details (Subscriber, View, and Video counts + uploads playlist ID)
            String channelUrl;
            if (queryId.startsWith("UC") && queryId.length() == 24) {
                channelUrl = youtubeApiBaseUrl + "/channels?id=" + queryId + "&key=" + apiKey + "&part=snippet,contentDetails,statistics";
            } else {
                String handle = queryId;
                if (!handle.startsWith("@")) {
                    handle = "@" + handle;
                }
                channelUrl = youtubeApiBaseUrl + "/channels?forHandle=" + handle + "&key=" + apiKey + "&part=snippet,contentDetails,statistics";
            }

            JsonNode root = webClient.get()
                    .uri(channelUrl)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null || !root.has("items") || root.get("items").isEmpty()) {
                log.warn("No channel details found for competitor input: {}", queryId);
                throw new IllegalArgumentException("No YouTube channel found for input: " + queryId);
            }

            JsonNode item = root.get("items").get(0);
            String realChannelId = item.get("id").asString();

            // 2. Find or create competitor channel using the real resolved channel ID
            CompetitorChannel competitor = competitorChannelRepository.findByChannelId(realChannelId)
                    .orElseGet(() -> {
                        CompetitorChannel newChannel = new CompetitorChannel();
                        newChannel.setChannelId(realChannelId);
                        return newChannel;
                    });

            JsonNode snippet = item.get("snippet");
            JsonNode stats = item.get("statistics");
            JsonNode contentDetails = item.get("contentDetails");

            String title = snippet.get("title").asString();
            String description = snippet.has("description") ? snippet.get("description").asString() : "";
            String customUrl = snippet.has("customUrl") ? snippet.get("customUrl").asString() : "";
            String thumbnailUrl = "";
            if (snippet.has("thumbnails")) {
                JsonNode thumbs = snippet.get("thumbnails");
                if (thumbs.has("high")) {
                    thumbnailUrl = thumbs.get("high").get("url").asString();
                } else if (thumbs.has("medium")) {
                    thumbnailUrl = thumbs.get("medium").get("url").asString();
                } else if (thumbs.has("default")) {
                    thumbnailUrl = thumbs.get("default").get("url").asString();
                }
            }

            long subscriberCount = parseLong(stats, "subscriberCount");
            long viewCount = parseLong(stats, "viewCount");
            long videoCount = parseLong(stats, "videoCount");
            String uploadsPlaylistId = contentDetails.get("relatedPlaylists").has("uploads")
                    ? contentDetails.get("relatedPlaylists").get("uploads").asString() : "";

            competitor.setTitle(title);
            competitor.setDescription(description);
            competitor.setCustomUrl(customUrl);
            competitor.setThumbnailUrl(thumbnailUrl);
            competitor.setSubscriberCount(subscriberCount);
            competitor.setViewCount(viewCount);
            competitor.setVideoCount(videoCount);
            competitor.setLastScrapedAt(LocalDateTime.now());
            competitor = competitorChannelRepository.save(competitor);

            // 3. Save snapshot for historical tracking
            CompetitorSnapshot snapshot = CompetitorSnapshot.builder()
                    .competitorChannel(competitor)
                    .subscriberCount(subscriberCount)
                    .viewCount(viewCount)
                    .videoCount(videoCount)
                    .build();
            competitorSnapshotRepository.save(snapshot);

            // 4. Fetch latest 5 uploads
            if (uploadsPlaylistId != null && !uploadsPlaylistId.isEmpty()) {
                scrapeRecentUploads(competitor, uploadsPlaylistId);
            }

            log.info("Successfully scraped and saved competitor {}", title);
            return competitor;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to scrape competitor {}: {}", queryId, e.getMessage(), e);
            throw new RuntimeException("Scraping failed", e);
        }
    }

    /**
     * Scrapes a single YouTube channel, updates its details, records a snapshot, and indexes its top recent videos.
     */
    @Transactional
    public void scrapeChannel(String channelId) {
        getOrCreateCompetitor(channelId);
    }

    private void scrapeRecentUploads(CompetitorChannel competitor, String uploadsPlaylistId) {
        try {
            // Get latest 5 video IDs from uploads playlist
            String playlistUrl = youtubeApiBaseUrl + "/playlistItems?playlistId=" + uploadsPlaylistId + "&part=contentDetails&maxResults=5&key=" + apiKey;
            JsonNode playlistRoot = webClient.get()
                    .uri(playlistUrl)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (playlistRoot == null || !playlistRoot.has("items")) {
                return;
            }

            List<String> videoIds = new ArrayList<>();
            for (JsonNode item : playlistRoot.get("items")) {
                if (item.has("contentDetails") && item.get("contentDetails").has("videoId")) {
                    videoIds.add(item.get("contentDetails").get("videoId").asString());
                }
            }

            if (videoIds.isEmpty()) {
                return;
            }

            // Get stats and details for those videos
            String videosUrl = youtubeApiBaseUrl + "/videos?id=" + String.join(",", videoIds) + "&part=snippet,statistics&key=" + apiKey;
            JsonNode videosRoot = webClient.get()
                    .uri(videosUrl)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (videosRoot == null || !videosRoot.has("items")) {
                return;
            }

            for (JsonNode item : videosRoot.get("items")) {
                String videoId = item.get("id").asString();
                JsonNode snippet = item.get("snippet");
                JsonNode stats = item.get("statistics");

                String title = snippet.get("title").asString();
                String publishedAtStr = snippet.get("publishedAt").asString();
                LocalDateTime publishedAt = ZonedDateTime.parse(publishedAtStr).toLocalDateTime();

                long views = parseLong(stats, "viewCount");
                long likes = parseLong(stats, "likeCount");

                CompetitorVideo video = competitorVideoRepository.findByVideoId(videoId)
                        .orElseGet(() -> {
                            CompetitorVideo newVideo = new CompetitorVideo();
                            newVideo.setVideoId(videoId);
                            newVideo.setCompetitorChannel(competitor);
                            return newVideo;
                        });

                video.setTitle(title);
                video.setPublishedAt(publishedAt);
                video.setViewCount(views);
                video.setLikeCount(likes);
                competitorVideoRepository.save(video);
            }

        } catch (Exception e) {
            log.warn("Failed to scrape uploads for competitor {}: {}", competitor.getTitle(), e.getMessage());
        }
    }

    private long parseLong(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asLong() : 0;
    }
}
