package com.seo.project.Service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class ThumbnailService {

    // Method to extract video ID from a given URL

    // This Regex handles all major YouTube URL formats:
    // 1. Standard URL: https://www.youtube.com/watch?v=VIDEO_ID
    // 2. Shortened URL: https://youtu.be/VIDEO_ID
    // 3. Embed URL: https://www.youtube.com/embed/VIDEO_ID
    // 4. Shorts URL: https://www.youtube.com/shorts/VIDEO_ID
    // 5. Mobile URL: https://m.youtube.com/watch?v=VIDEO_ID

    // COMPILED PATTERN: We use 'static final' to compile this once for better performance.
    // This is much faster than compiling it every time the function runs.
    // It handles: http, https, www, m., youtu.be, /watch?v=, /embed/, /v/, /shorts/
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
}