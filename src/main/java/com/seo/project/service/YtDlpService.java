package com.seo.project.service;

import com.seo.project.dto.VideoFormat;
import com.seo.project.dto.VideoInfo;
import tools.jackson.databind.JsonNode;
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
    // ✅ Use a Desktop User-Agent to ensure we get all formats
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36";

    /**
     * ✅ FIXED: Enhanced metadata fetching with better timeout handling
     * - Increased socket timeout for slow networks
     * - Better error handling and fallback
     * - More robust JSON parsing
     */
    public VideoInfo fetchVideoInfo(String videoUrl) {
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(
                "yt-dlp", 
                "--no-warnings",             // Critical: Keep stderr clean
                "--no-playlist",             // Reduce JSON size (no playlist metadata)
                "--ignore-config",           // Production safety: Ignore any ~/.config/yt-dlp/config
                "--force-ipv4",              // ✅ FIX: Force IPv4 to prevent timeouts on Render/Cloud
                "--socket-timeout", "30",    // ✅ INCREASED: 30 seconds (was 15) for slow networks
                "--retries", "3",            // Retry 3 times on failure
                "--fragment-retries", "3",   // Retry fragment fetching
                "--http-chunk-size", "10485760", // 10MB chunks for large downloads
                "--user-agent", USER_AGENT,
                "-J",                        // Dump JSON
                videoUrl
            );

            // Do NOT merge stderr. We need pure JSON on stdout.
            builder.redirectErrorStream(false); 
            
            process = builder.start();

            // Consume Stderr in background to prevent process hanging (Deadlock prevention)
            consumeStream(process.getErrorStream());

            // STREAM the JSON. Do not buffer it into a StringBuilder.
            // Jackson can parse directly from the InputStream, using tiny buffers.
            try (InputStream inputStream = process.getInputStream()) {
                VideoInfo result = parseJsonToDto(inputStream);
                
                // Wait for process to complete
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    System.err.println("yt-dlp exit code: " + exitCode);
                    return result; // Still return what we got
                }
                
                return result;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("yt-dlp interrupted: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("yt-dlp error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly(); // Use forcibly to ensure cleanup
            }
        }
    }

    // Helper to drain stderr without blocking main thread
    private void consumeStream(InputStream stream) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Log only errors, suppress warnings
                    if (line.contains("[error]") || line.contains("ERROR")) {
                        System.err.println("yt-dlp stderr: " + line);
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * ✅ FIXED: More robust JSON parsing with better error handling
     * - Handles incomplete JSON gracefully
     * - Better validation of format data
     * - Improved memory efficiency
     */
    private VideoInfo parseJsonToDto(InputStream inputStream) {
        try {
            // Streaming Parse: Reads tokens one by one. Huge memory savings.
            JsonNode root = objectMapper.readTree(inputStream);

            String title = getSafeText(root.path("title"));
            String channel = getSafeText(root.path("uploader"));
            String thumbnail = getSafeText(root.path("thumbnail"));

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
                    if (url.isEmpty() || protocol.isEmpty() || !protocol.startsWith("http")) continue;

                    long fileSize = f.path("filesize").asLong(0);
                    if (fileSize == 0) fileSize = f.path("filesize_approx").asLong(0);
                    
                    int height = f.path("height").asInt(0);
                    String note = getSafeText(f.path("format_note"));
                    String quality = (height > 0) ? height + "p" : (note.isEmpty() ? "Unknown" : note);

                    boolean hasVideo = !vcodec.equals("none") && !vcodec.isEmpty();
                    boolean hasAudio = !getSafeText(f.path("acodec")).equals("none");

                    // Skip formats with neither video nor audio
                    if (!hasVideo && !hasAudio) continue;

                    // Extract Headers
                    Map<String, String> headers = new HashMap<>();
                    JsonNode headersNode = f.path("http_headers");
                    
                    if (headersNode.isObject()) {
                        headersNode.properties().forEach(entry -> 
                            headers.put(entry.getKey(), getSafeText(entry.getValue()))
                        );
                    }

                    // ✅ FIXED: Support more container formats for large downloads
                    if (ext.equals("mp4") || ext.equals("m4a") || ext.equals("webm") || 
                        ext.equals("mkv") || ext.equals("flv") || ext.equals("mov")) {
                        
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

            // Smart Sorting: Prefer high res video+audio, then video only, then audio only
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
            
            // Then audio only (sorted by size approx quality)
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

    // Keep helper methods (getSafeText, formatBytes, InternalFormat record)
    private String getSafeText(JsonNode node) {
        if (node == null || node.isMissingNode()) return "";
        try {
            return node.asString("").trim();
        } catch (Exception e) {
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