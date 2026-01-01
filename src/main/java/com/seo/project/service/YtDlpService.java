package com.seo.project.service;

import com.seo.project.dto.VideoFormat;
import com.seo.project.dto.VideoInfo;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YtDlpService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    // ✅ FIXED: Switch to Android User-Agent to match the player_client=android bypass
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.102 Mobile Safari/537.36";
    
    // ✅ PRODUCTION FIX: Retry configuration for bot detection bypass
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 2000; // 2 seconds

    /**
     * ✅ FIXED: Added Android client emulation to bypass "Sign in to confirm you're not a bot"
     * ✅ PRODUCTION FIX: Added retry logic with exponential backoff for bot detection
     */
    public VideoInfo fetchVideoInfo(String videoUrl) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            VideoInfo result = fetchVideoInfoAttempt(videoUrl, attempt);
            if (result != null) {
                return result;
            }
            
            // If not the last attempt, wait before retrying
            if (attempt < MAX_RETRIES - 1) {
                long delayMs = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, attempt);
                System.out.println("Retry attempt " + (attempt + 2) + "/" + MAX_RETRIES + " after " + delayMs + "ms");
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        System.err.println("All retry attempts exhausted for: " + videoUrl);
        return null;
    }

    /**
     * ✅ PRODUCTION FIX: Single attempt to fetch video info with bot detection bypass
     */
    private VideoInfo fetchVideoInfoAttempt(String videoUrl, int attemptNumber) {
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(
                "yt-dlp", 
                "--no-warnings",             // Critical: Keep stderr clean
                "--no-playlist",             // Reduce JSON size (no playlist metadata)
                "--ignore-config",           // Production safety: Ignore any ~/.config/yt-dlp/config
                "--force-ipv4",              // ✅ FIX: Force IPv4 to prevent timeouts on Render/Cloud
                "--socket-timeout", "30",    // ✅ INCREASED: 30 seconds for slow networks
                "--retries", "2",            // Reduce internal retries (we handle retries externally)
                "--fragment-retries", "2",   // Reduce fragment retries
                "--http-chunk-size", "10485760", // 10MB chunks for large downloads
                "--extractor-args", "youtube:player_client=android&hl=en", // ✅ Android client + language
                "--user-agent", USER_AGENT,
                "--encoding", "utf-8"        // ✅ PRODUCTION FIX: Explicit encoding for stability
            );
            
            // ✅ PRODUCTION FIX: Add cookies file if it exists (for YouTube authentication)
            String cookiesFile = getCookiesPath();
            java.nio.file.Path cookiesPath = java.nio.file.Paths.get(cookiesFile);
            
            if (java.nio.file.Files.exists(cookiesPath)) {
                builder.command().add("--cookies");
                builder.command().add(cookiesFile);
                System.out.println("Using cookies from: " + cookiesFile);
            } else if (attemptNumber == 0) {
                System.out.println("⚠️  WARNING: Cookies file not found. For production, consider mounting YouTube cookies to bypass bot detection.");
            }

            // ✅ PRODUCTION FIX: Add better headers to avoid bot detection
            builder.command().add("-J");    // Dump JSON

            // Do NOT merge stderr. We need pure JSON on stdout.
            builder.redirectErrorStream(false); 
            
            process = builder.start();

            // Consume Stderr in background to prevent process hanging (Deadlock prevention)
            consumeStream(process.getErrorStream(), attemptNumber);

            // STREAM the JSON. Do not buffer it into a StringBuilder.
            try (InputStream inputStream = process.getInputStream()) {
                VideoInfo result = parseJsonToDto(inputStream);
                
                // Wait for process to complete
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    System.err.println("yt-dlp exit code: " + exitCode + " (attempt " + (attemptNumber + 1) + "/" + MAX_RETRIES + ")");
                    return null; // Return null to trigger retry
                }
                
                return result;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("yt-dlp interrupted (attempt " + (attemptNumber + 1) + "/" + MAX_RETRIES + "): " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("yt-dlp error (attempt " + (attemptNumber + 1) + "/" + MAX_RETRIES + "): " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly(); // Use forcibly to ensure cleanup
            }
        }
    }

    // Helper to drain stderr without blocking main thread
    private void consumeStream(InputStream stream, int attemptNumber) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Log bot detection errors for debugging
                    if (line.contains("Sign in to confirm")) {
                        System.err.println("🤖 BOT DETECTION (attempt " + (attemptNumber + 1) + "/" + MAX_RETRIES + "): " + line);
                    } else if (line.contains("[error]") || line.contains("ERROR")) {
                        System.err.println("yt-dlp stderr: " + line);
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * ✅ FIXED: More robust JSON parsing
     */
    private VideoInfo parseJsonToDto(InputStream inputStream) {
        String title = "Unknown Title";
        String channel = "Unknown Channel";
        String thumbnail = "";
        List<InternalFormat> rawFormats = new ArrayList<>();

        try (JsonParser parser = objectMapper.createParser(inputStream)) {
            // Must start with an Object
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return null;
            }

            // Loop through top-level fields
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = parser.currentName();
                parser.nextToken(); // Move to value

                if ("title".equals(fieldName)) {
                    title = parser.getValueAsString("Unknown Title");
                } else if ("uploader".equals(fieldName)) {
                    channel = parser.getValueAsString("Unknown Channel");
                } else if ("thumbnail".equals(fieldName)) {
                    thumbnail = parser.getValueAsString("");
                } else if ("formats".equals(fieldName)) {
                    if (parser.currentToken() == JsonToken.START_ARRAY) {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            if (parser.currentToken() == JsonToken.START_OBJECT) {
                                InternalFormat fmt = parseFormat(parser);
                                if (fmt != null) rawFormats.add(fmt);
                            } else {
                                parser.skipChildren();
                            }
                        }
                    } else {
                        parser.skipChildren();
                    }
                } else {
                    parser.skipChildren();
                }
            }

            // Smart Sorting
            List<VideoFormat> sortedFormats = new ArrayList<>();
            
            // Prefer high res video+audio
            rawFormats.stream()
                .filter(f -> f.dto.hasVideo() && f.dto.hasAudio())
                .sorted(Comparator.comparingInt((InternalFormat f) -> f.height).reversed())
                .forEach(f -> sortedFormats.add(f.dto));
            
            // Then video only
            rawFormats.stream()
                .filter(f -> f.dto.hasVideo() && !f.dto.hasAudio())
                .sorted(Comparator.comparingInt((InternalFormat f) -> f.height).reversed())
                .forEach(f -> sortedFormats.add(f.dto));
            
            // Then audio only
            rawFormats.stream()
                .filter(f -> !f.dto.hasVideo() && f.dto.hasAudio())
                .sorted(Comparator.comparingLong((InternalFormat f) -> f.size).reversed())
                .forEach(f -> sortedFormats.add(f.dto));

            return new VideoInfo(title, channel, thumbnail, sortedFormats);

        } catch (Exception e) {
            System.err.println("JSON Parse Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private InternalFormat parseFormat(JsonParser parser) throws Exception {
        String url = "";
        String ext = "";
        String protocol = "";
        String formatId = "";
        String vcodec = "none";
        String acodec = "none";
        String formatNote = "";
        long fileSize = 0;
        long fileSizeApprox = 0;
        int height = 0;
        Map<String, String> headers = new HashMap<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.currentName();
            parser.nextToken(); // Move to value

            if ("url".equals(name)) url = parser.getValueAsString("");
            else if ("ext".equals(name)) ext = parser.getValueAsString("");
            else if ("protocol".equals(name)) protocol = parser.getValueAsString("");
            else if ("format_id".equals(name)) formatId = parser.getValueAsString("");
            else if ("vcodec".equals(name)) vcodec = parser.getValueAsString("none");
            else if ("acodec".equals(name)) acodec = parser.getValueAsString("none");
            else if ("format_note".equals(name)) formatNote = parser.getValueAsString("");
            else if ("filesize".equals(name)) fileSize = parser.getValueAsLong(0);
            else if ("filesize_approx".equals(name)) fileSizeApprox = parser.getValueAsLong(0);
            else if ("height".equals(name)) height = parser.getValueAsInt(0);
            else if ("http_headers".equals(name)) {
                if (parser.currentToken() == JsonToken.START_OBJECT) {
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        String hName = parser.currentName();
                        parser.nextToken();
                        headers.put(hName, parser.getValueAsString(""));
                    }
                } else {
                    parser.skipChildren();
                }
            } else {
                parser.skipChildren();
            }
        }

        if (url.isEmpty() || protocol.isEmpty() || !protocol.startsWith("http")) return null;

        long finalSize = (fileSize > 0) ? fileSize : fileSizeApprox;
        String quality = (height > 0) ? height + "p" : (formatNote.isEmpty() ? "Unknown" : formatNote);

        boolean hasVideo = !vcodec.equals("none") && !vcodec.isEmpty();
        boolean hasAudio = !acodec.equals("none") && !acodec.isEmpty();

        if (!hasVideo && !hasAudio) return null;

        if (ext.equals("mp4") || ext.equals("m4a") || ext.equals("webm") || 
            ext.equals("mkv") || ext.equals("flv") || ext.equals("mov")) {
            
            return new InternalFormat(
                new VideoFormat(
                    formatId, 
                    quality, 
                    ext.toUpperCase(), 
                    formatBytes(finalSize), 
                    url, 
                    hasAudio, 
                    hasVideo, 
                    vcodec,
                    headers
                ),
                height,
                finalSize
            );
        }
        return null;
    }

    private record InternalFormat(VideoFormat dto, int height, long size) {}

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * ✅ Helper method to determine the correct cookies file path.
     * Checks for the Render secret path first, then falls back to a non-existent local path
     * to safely skip authentication on localhost.
     */
    private String getCookiesPath() {
        // 1. Production: Check Render Secret Path
        if (java.nio.file.Files.exists(java.nio.file.Paths.get("/etc/secrets/cookies.txt"))) {
            return "/etc/secrets/cookies.txt";
        }
        // 2. Local: Return a non-existent path so Files.exists() fails gracefully
        // This prevents the "empty file" crash on localhost
        return "/app/ignore_cookies"; 
    }
}