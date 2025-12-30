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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class YtDlpService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    // ✅ Use a Desktop User-Agent to ensure we get all formats (1080p, etc.)
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36";

    public VideoInfo fetchVideoInfo(String videoUrl) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                "yt-dlp", 
                "--no-warnings",
                "--user-agent", USER_AGENT,
                "-J",               // Dump JSON
                "--no-playlist",
                "--flat-playlist",
                videoUrl
            );

            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            
            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("{")) continue; 
                jsonOutput.append(line);
            }

            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroy();
                throw new RuntimeException("yt-dlp timed out");
            }

            if (jsonOutput.length() > 0) {
                return parseJsonToDto(jsonOutput.toString());
            }
        } catch (Exception e) {
            System.err.println("yt-dlp error: " + e.getMessage());
        }
        return null;
    }

    private VideoInfo parseJsonToDto(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

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
                        // Jackson 3.0 uses properties()
                        for (Map.Entry<String, JsonNode> entry : headersNode.properties()) {
                            headers.put(entry.getKey(), getSafeText(entry.getValue()));
                        }
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
            rawFormats.stream().filter(f -> f.dto.hasVideo() && f.dto.hasAudio())
                .sorted(Comparator.comparingInt((InternalFormat f) -> f.height).reversed())
                .forEach(f -> sortedFormats.add(f.dto));
            rawFormats.stream().filter(f -> f.dto.hasVideo() && !f.dto.hasAudio())
                .sorted(Comparator.comparingInt((InternalFormat f) -> f.height).reversed())
                .forEach(f -> sortedFormats.add(f.dto));
            rawFormats.stream().filter(f -> !f.dto.hasVideo() && f.dto.hasAudio())
                .sorted(Comparator.comparingLong((InternalFormat f) -> f.size).reversed())
                .forEach(f -> sortedFormats.add(f.dto));

            return new VideoInfo(title, channel, thumbnail, sortedFormats);

        } catch (Exception e) {
            System.err.println("JSON Parse Error: " + e.getMessage());
            return null;
        }
    }

    private String getSafeText(JsonNode node) {
        if (node == null || node.isMissingNode()) return "";
        // Try asText() if available (Jackson 2.x/3.x standard), fallback to toString() cleanup
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