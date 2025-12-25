package com.seo.project.service;

import com.seo.project.dto.VideoFormat;
import com.seo.project.dto.VideoInfo;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class YtDlpService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public VideoInfo fetchVideoInfo(String videoUrl) {
        try {
            // Run yt-dlp to dump JSON metadata
            ProcessBuilder builder = new ProcessBuilder(
                "yt-dlp", 
                "--no-warnings",    // Silence warnings
                "--extractor-args", "youtube:player_client=android", // ✅ CRITICAL FIX: Use Android API
                "-J",               // Dump JSON
                "--no-playlist",    // Single video only
                "--flat-playlist",  // Don't expand playlists if url is mixed
                videoUrl
            );

            // Merge error output into standard output
            builder.redirectErrorStream(true);
            
            Process process = builder.start();

            // Read the Output
            // Use UTF-8 to handle emojis in titles correctly
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            
            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // 🛡️ CRITICAL FIX: Ignore any line that isn't the JSON start
                if (!line.trim().startsWith("{")) {
                    // Optional: Print ignored lines to console for debugging
                    // System.out.println("Ignored log: " + line);
                    continue; 
                }
                jsonOutput.append(line);
            }

            // Wait max 15 seconds to prevent hanging threads
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroy();
                throw new RuntimeException("yt-dlp timed out");
            }

            if (jsonOutput.length() > 0) {
                return parseJsonToDto(jsonOutput.toString());
            } else {
                // Log this to see what happened in the console
                System.err.println("❌ yt-dlp returned NO JSON. Try running the command manually to debug.");
            }

        } catch (Exception e) {
            System.err.println("yt-dlp execution failed: " + e.getMessage());
        }
        return null;
    }

    private VideoInfo parseJsonToDto(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String title = root.path("title").asString("Unknown Title");
            String channel = root.path("uploader").asString("Unknown Channel");
            // Get the best thumbnail (last in the list usually, or just 'thumbnail' field)
            String thumbnail = root.path("thumbnail").asString("");

            List<VideoFormat> formats = new ArrayList<>();
            JsonNode formatsNode = root.path("formats");

            if (formatsNode.isArray()) {
                for (JsonNode f : formatsNode) {
                    // Extract technical details
                    String url = f.path("url").asString("");
                    String ext = f.path("ext").asString("");
                    String protocol = f.path("protocol").asString(""); // http, https, m3u8...
                    
                    // Filter: We want direct HTTP/HTTPS links (not m3u8 manifests)
                    if (!protocol.startsWith("http")) continue;

                    // Extract Metadata
                    long fileSize = f.path("filesize").asLong(0);
                    if (fileSize == 0) fileSize = f.path("filesize_approx").asLong(0);
                    
                    String note = f.path("format_note").asString("");
                    int height = f.path("height").asInt(0);
                    String quality = (note != null && !note.isEmpty()) ? note : height + "p";

                    // Check Streams
                    boolean hasVideo = !f.path("vcodec").asString("").equals("none");
                    boolean hasAudio = !f.path("acodec").asString("").equals("none");

                    // LOGIC: What do we display?
                    // 1. MP4 Video with Audio (Standard 360p, 720p)
                    // 2. High Res Video Only (1080p, 4K) -> User sees "Muted" icon
                    // 3. Audio Only (m4a)
                    // 4. WebM and other container formats as fallbacks
                    
                    // Accept MP4, M4A, WebM, and other common containers
                    if (ext.equals("mp4") || ext.equals("m4a") || ext.equals("webm") || 
                        ext.equals("mkv") || ext.equals("flv")) {
                        formats.add(new VideoFormat(
                            quality,
                            ext.toUpperCase(),
                            formatBytes(fileSize),
                            url,
                            hasAudio,
                            hasVideo
                        ));
                    }
                }
            }

            // Sort: Best quality first (Simplified sorting)
            Collections.reverse(formats);

            return new VideoInfo(title, channel, thumbnail, formats);

        } catch (Exception e) {
            System.err.println("JSON Parsing error: " + e.getMessage());
            return null;
        }
    }

    // Helper: 1048576 -> "1.0 MB"
    private String formatBytes(long bytes) {
        if (bytes <= 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}