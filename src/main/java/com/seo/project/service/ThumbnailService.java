package com.seo.project.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ThumbnailService provides specialized utilities for handling YouTube video assets,
 * including URL parsing, metadata extraction (via oEmbed), and binary image downloads.
 */
@Slf4j
@Service
public class ThumbnailService {

    private final WebClient webClient;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("${base.url}")
    private String baseUrl;

    /**
     * Reuses a shared WebClient.Builder for better performance and pooled connections.
     */
    public ThumbnailService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    /*
    Method to extract video ID from a given URL

    This Regex handles all major YouTube URL formats:
    1. Standard URL: https://www.youtube.com/watch?v=VIDEO_ID
    2. Shortened URL: https://youtu.be/VIDEO_ID
    3. Embed URL: https://www.youtube.com/embed/VIDEO_ID
    4. Shorts URL: https://www.youtube.com/shorts/VIDEO_ID
    5. Mobile URL: https://m.youtube.com/watch?v=VIDEO_ID

    COMPILED PATTERN: We use 'static final' to compile this once for better performance.
    This is much faster than compiling it every time the function runs.
    It handles: http, https, www, m., youtu.be, /watch?v=, /embed/, /v/, /shorts/
     */
    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.|m\\.)?(?:youtu\\.be/|youtube\\.com/(?:embed/|v/|watch\\?v=|watch\\?.+&v=|shorts/))([\\w-]{11})(?:.+)?$");

    /**
     * Extracts the 11-character Video ID from a YouTube URL.
     * 
     * @param url The full YouTube URL.
     * @return The Video ID or null if parsing fails.
     */
    public String extractVideoId(String url) {
        // 1. Basic validation to avoid NullPointerException
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        // 2. Match the URL against our robust Pattern
        Matcher matcher = YOUTUBE_URL_PATTERN.matcher(url);

        // 3. If a match is found, return the first group (the 11-char ID)
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        log.debug("Failed to extract Video ID from input (likely a keyword, not a URL): [{}]", url);
        return null;
    }

    /**
     * Fetches public video metadata using the YouTube Data API or falling back to the lightweight oEmbed API.
     */
    @SuppressWarnings("unchecked")
    public Map<String,String> fetchVideoMetadata(String videoId) {
        Map<String,String> metadata = new HashMap<>();

        // 1. Try YouTube Data API first for full details (works even if embedding is disabled)
        try {
            if (apiKey != null && !apiKey.isEmpty() && baseUrl != null && !baseUrl.isEmpty()) {
                log.debug("Attempting to fetch video metadata via YouTube Data API for video: [{}]", videoId);
                String url = baseUrl + "/videos?part=snippet&id=" + videoId + "&key=" + apiKey;
                JsonNode root = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
                if (root != null && root.has("items") && !root.get("items").isEmpty()) {
                    JsonNode item = root.get("items").get(0);
                    JsonNode snippet = item.get("snippet");
                    metadata.put("title", snippet.get("title").asString());
                    metadata.put("author_name", snippet.get("channelTitle").asString());
                    log.info("YouTube Data API metadata successfully retrieved for: {}", metadata.get("title"));
                    return metadata;
                }
            }
        } catch (Exception e) {
            log.warn("YouTube Data API metadata fetch failed for video [{}] (possibly quota limit/network): {}", videoId, e.getMessage());
        }

        // 2. Fall back to oEmbed API if YouTube Data API fails
        log.debug("Falling back to oEmbed metadata for video ID: [{}]", videoId);
        try {
            String oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=" + videoId + "&format=json";
            Map<String,Object> response = webClient.get()
                    .uri(oembedUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                if (response.containsKey("title")) metadata.put("title", (String) response.get("title"));
                if (response.containsKey("author_name")) metadata.put("author_name", (String) response.get("author_name"));
                log.info("oEmbed metadata successfully retrieved for: {}", metadata.get("title"));
            }
        } catch (Exception e) {
            log.error("Failed to retrieve oEmbed metadata for [{}]: {}", videoId, e.getMessage());
        }
        return metadata;
    }

    /**
     * Downloads an image from a URL and returns it as a downloadable ResponseEntity.
     * Handles content-type mapping and filename sanitization.
     */
    public ResponseEntity<byte[]> downloadImage(String imageUrl, String fileName) {
        log.info("Initiating image download for asset: {}", fileName);
        try {
            // Use the injected webClient (efficient reuse)
            byte[] imageBytes = webClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (imageBytes == null) {
                log.warn("Download failed: Received empty body for image URL: {}", imageUrl);
                return ResponseEntity.notFound().build();
            }

            // Map content type based on URL extension
            MediaType contentType = MediaType.IMAGE_JPEG;
            String fileExtension = ".jpg";

            if (imageUrl.contains(".webp")) {
                contentType = MediaType.parseMediaType("image/webp");
                fileExtension = ".webp";
            } else if (imageUrl.contains(".png")) {
                contentType = MediaType.IMAGE_PNG;
                fileExtension = ".png";
            }

            // Sanitize filename for operating system compatibility
            String safeFilename = fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            String fullFileName = safeFilename + fileExtension;

            // 5. Build Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setContentDispositionFormData("attachment", fullFileName);

            log.debug("Successfully prepared download response for: {}", fullFileName);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageBytes);

        } catch (Exception e) {
            log.error("Exception during image download for [{}]: {}", imageUrl, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}