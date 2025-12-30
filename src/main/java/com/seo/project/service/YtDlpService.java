package com.seo.project.service;

import com.seo.project.dto.VideoFormat;
import com.seo.project.dto.VideoInfo;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
// import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import java.util.concurrent.TimeUnit;

@Service
public class YtDlpService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    // ✅ Use a Desktop User-Agent to ensure we get all formats
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36";

    public VideoInfo fetchVideoInfo(String videoUrl) {
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(
                "yt-dlp", 
                "--no-warnings",             // Critical: Keep stderr clean
                "--no-playlist",             // Reduce JSON size (no playlist metadata)
                "--ignore-config",           // Production safety: Ignore any ~/.config/yt-dlp/config
                "--user-agent", USER_AGENT,
                "-J",                        // Dump JSON
                videoUrl
            );

            // ✅ FIX 1: Do NOT merge stderr. We need pure JSON on stdout.
            // Merging them risks corrupting the JSON if a warning slips through.
            builder.redirectErrorStream(false); 
            
            process = builder.start();

            // ✅ FIX 2: Consume Stderr in background to prevent process hanging (Deadlock prevention)
            consumeStream(process.getErrorStream());

            // ✅ FIX 3: STREAM the JSON. Do not buffer it into a StringBuilder.
            // Jackson can parse directly from the InputStream, using tiny buffers.
            try (InputStream inputStream = process.getInputStream()) {
                return parseJsonToDto(inputStream);
            }

        } catch (Exception e) {
            System.err.println("yt-dlp error: " + e.getMessage());
            return null;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }

    // Helper to drain stderr without blocking main thread
    private void consumeStream(InputStream stream) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                while (reader.readLine() != null) { /* Ignore stderr or log if needed */ }
            } catch (Exception ignored) {}
        }).start();
    }

    // ✅ Updated Signature: Accepts InputStream instead of String
    private VideoInfo parseJsonToDto(InputStream inputStream) {
        try {
            // ✅ Streaming Parse: Reads tokens one by one. huge memory savings.
            JsonNode root = objectMapper.readTree(inputStream);

            String title = getSafeText(root.path("title"));
            String channel = getSafeText(root.path("uploader"));
            String thumbnail = getSafeText(root.path("thumbnail")); // yt-dlp -J usually has high-res thumbnails

            List<InternalFormat> rawFormats = new ArrayList<>();
            JsonNode formatsNode = root.path("formats");

            if (formatsNode.isArray()) {
                for (JsonNode f : formatsNode) {
                    String url = getSafeText(f.path("url"));
                    String ext = getSafeText(f.path("ext"));
                    String protocol = getSafeText(f.path("protocol"));
                    String formatId = getSafeText(f.path("format_id"));
                    String vcodec = getSafeText(f.path("vcodec"));

                    // Filter: We want direct HTTP/HTTPS links
                    if (!protocol.startsWith("http")) continue;

                    long fileSize = f.path("filesize").asLong(0);
                    if (fileSize == 0) fileSize = f.path("filesize_approx").asLong(0);
                    
                    int height = f.path("height").asInt(0);
                    String note = getSafeText(f.path("format_note"));
                    String quality = (height > 0) ? height + "p" : (note.isEmpty() ? "Unknown" : note);

                    boolean hasVideo = !vcodec.equals("none") && !vcodec.isEmpty();
                    boolean hasAudio = !getSafeText(f.path("acodec")).equals("none");

                    // Extract Headers
                    Map<String, String> headers = new HashMap<>();
                    JsonNode headersNode = f.path("http_headers");
                    
                    if (headersNode.isObject()) {
                         // Jackson 3.0 compatible iteration
                         headersNode.properties().forEach(entry -> 
                            headers.put(entry.getKey(), getSafeText(entry.getValue()))
                         );
                    }

                    if (ext.equals("mp4") || ext.equals("m4a") || ext.equals("webm") || 
                        ext.equals("mkv") || ext.equals("flv")) {
                        
                        rawFormats.add(new InternalFormat(
                            new VideoFormat(
                                formatId, 
                                quality, 
                                ext.toUpperCase(), 
                                formatBytes(fileSize), 
                                url, 
                                hasAudio, 
                                hasVideo, 
                                vcodec,
                                headers
                            ),
                            height,
                            fileSize
                        ));
                    }
                }
            }

            // Smart Sorting
            List<VideoFormat> sortedFormats = new ArrayList<>();
            // Prefer high res video+audio
            rawFormats.stream().filter(f -> f.dto.hasVideo() && f.dto.hasAudio())
                .sorted(Comparator.comparingInt((InternalFormat f) -> f.height).reversed())
                .forEach(f -> sortedFormats.add(f.dto));
            // Then video only
            rawFormats.stream().filter(f -> f.dto.hasVideo() && !f.dto.hasAudio())
                .sorted(Comparator.comparingInt((InternalFormat f) -> f.height).reversed())
                .forEach(f -> sortedFormats.add(f.dto));
            // Then audio only (sorted by size approx quality)
            rawFormats.stream().filter(f -> !f.dto.hasVideo() && f.dto.hasAudio())
                .sorted(Comparator.comparingLong((InternalFormat f) -> f.size).reversed())
                .forEach(f -> sortedFormats.add(f.dto));

            return new VideoInfo(title, channel, thumbnail, sortedFormats);

        } catch (Exception e) {
            System.err.println("JSON Parse Error: " + e.getMessage());
            return null;
        }
    }

    // Keep helper methods (getSafeText, formatBytes, InternalFormat record) as they were
    private String getSafeText(JsonNode node) {
        if (node == null || node.isMissingNode()) return "";
        try {
            return node.asString(""); // Jackson standard
        } catch (Exception e) {
            // Fallback for weird versions
            String s = node.toString(); 
            if (s.startsWith("\"") && s.endsWith("\"")) {
                return s.substring(1, s.length() - 1);
            }
            return s;
        }
    }

    private record InternalFormat(VideoFormat dto, int height, long size) {}

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}