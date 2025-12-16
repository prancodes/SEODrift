package com.seo.project.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

@Service
public class ThumbnailService {

    private final WebClient webClient;

    // Inject WebClient.Builder to allow reuse and better performance
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
     * @param url The full YouTube URL (e.g., https://youtu.be/abc12345678)
     * @return The Video ID (e.g., abc12345678) or null if not found.
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
        
        return null;
    }

    // Fetch Video Metadata (using oembed)
    @SuppressWarnings("unchecked")
    public Map<String,String> fetchVideoMetadata(String videoId) {
        Map<String,String> metadata = new HashMap<>();

        // Default values in case fetch fails
        metadata.put("title", "video-" + videoId);
        metadata.put("author_name", "yt-Video" + videoId);

        try{
            String oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=" + videoId + "&format=json";
            // .block() calls it synchronously (keeps it simple for the current Controller)
            Map<String,Object> response = webClient.get()
                    .uri(oembedUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("title")) {
                metadata.put("title", (String) response.get("title"));
            }
            if (response != null && response.containsKey("author_name")) {
                metadata.put("author_name", (String) response.get("author_name"));
            }
        } catch (Exception e) {
            System.out.println("Error fetching video title: " + e.getMessage());
        }
        return metadata;
    }

    // Downloads image bytes from the URL, with proper filename as videoTitle
    public ResponseEntity<byte[]> downloadImage(String imageUrl, String fileName) {
        try {
            // 3. Use the injected webClient (efficient reuse)
            byte[] imageBytes = webClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(); // Blocking here to return simple bytes to the browser

            if (imageBytes == null) {
                return ResponseEntity.notFound().build();
            }

            // 4. Smart Content-Type Detection
            MediaType contentType = MediaType.IMAGE_JPEG; // Default
            String fileExtension = ".jpg";

            if (imageUrl.contains(".webp")) {
                contentType = MediaType.parseMediaType("image/webp");
                fileExtension = ".webp";
            } else if (imageUrl.contains(".png")) {
                contentType = MediaType.IMAGE_PNG;
                fileExtension = ".png";
            }

            // Sanitize filename (remove illegal characters for Windows/Linux)
            String safeFilename = fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            String fullFileName = safeFilename + fileExtension;
            // System.out.println("Downloading image as: " + fullFileName);

            // 5. Build Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            // "attachment" forces the browser to show the 'Save As' dialog
            headers.setContentDispositionFormData("attachment", fullFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageBytes);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

}