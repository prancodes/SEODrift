package com.seo.project.service;

import com.seo.project.dto.VideoInfo;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoService {

    private final WebClient webClient;
    private final YtDlpService ytDlpService;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    // Task Store
    private final Map<String, DownloadTask> tasks = new ConcurrentHashMap<>();

    // Regex to find percentage (e.g., " 45.5%")
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+\\.\\d+)%");

    public VideoService(WebClient.Builder builder, YtDlpService ytDlpService) {
        this.webClient = builder.codecs(c -> c.defaultCodecs().maxInMemorySize(-1)).build();
        this.ytDlpService = ytDlpService;
    }

    public VideoInfo getVideoDetails(String videoUrl) { return ytDlpService.fetchVideoInfo(videoUrl); }

    // --- ASYNC TASK MANAGEMENT ---

    public static class DownloadTask {
        public String status = "Initializing...";
        public Path filePath;
        public boolean isAudio; 
        public long timestamp = System.currentTimeMillis();
    }

    public String getTaskStatus(String token) {
        DownloadTask task = tasks.get(token);
        return (task != null) ? task.status : "ERROR: Task not found";
    }

    public DownloadTask getTask(String token) {
        return tasks.get(token);
    }

    public void removeTask(String token) {
        tasks.remove(token);
    }

    /**
     * ✅ Unified Background Downloader (Video & Audio)
     */
    public void startMergeTask(String token, String originalUrl, String formatId, String outputFormat, boolean isAudio) {
        DownloadTask task = new DownloadTask();
        task.isAudio = isAudio; // Store type
        tasks.put(token, task);

        new Thread(() -> {
            Path tempFile = null;
            Process process = null;
            try {
                // Determine extension
                String ext = outputFormat; // e.g. "mp4" or "m4a"
                tempFile = Files.createTempFile("seodrift_" + token, "." + ext);
                task.filePath = tempFile;

                List<String> command = new ArrayList<>();
                command.add("yt-dlp");
                command.add("--no-warnings");
                command.add("--newline"); // ✅ CRITICAL FIX: Forces real-time output for Java to read
                command.add("--user-agent"); command.add(USER_AGENT);
                
                if (isAudio) {
                    // ✅ AUDIO MODE: Download & Extract Audio
                    command.add("-f"); command.add(formatId);
                    command.add("-x"); // Extract audio
                    command.add("--audio-format"); command.add("m4a"); // Force M4A for compatibility
                } else {
                    // ✅ VIDEO MODE: Merge Video+Audio
                    command.add("-f"); command.add(formatId + "+bestaudio");
                    command.add("--merge-output-format"); command.add(ext);
                }

                command.add("-o"); command.add(tempFile.toAbsolutePath().toString());
                command.add("--force-overwrites");
                command.add(originalUrl);

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                
                process = pb.start();

                // Real-time Status Updates
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Parse [download] 45.5% ...
                        if (line.contains("[download]") && line.contains("%")) {
                            Matcher m = PERCENT_PATTERN.matcher(line);
                            if (m.find()) {
                                task.status = "Downloading: " + m.group(1) + "%";
                            }
                        } 
                        // Handle phases
                        else if (line.contains("[Merger]")) {
                            task.status = "Merging Video & Audio...";
                        } 
                        // ✅ FIX: Show 100% + Extracting status
                        else if (line.contains("[ExtractAudio]") || line.contains("[Fixup")) {
                            task.status = "Downloading: 100% (Extracting Audio...)";
                        }
                    }
                }

                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    if(Files.exists(tempFile) && Files.size(tempFile) > 0) {
                        task.status = "COMPLETED";
                    } else {
                        task.status = "ERROR: File missing";
                    }
                } else {
                    task.status = "ERROR: Process failed";
                }

            } catch (Exception e) {
                task.status = "ERROR: " + e.getMessage();
                e.printStackTrace();
            }
        }).start();
    }

    // --- STANDARD STREAMING (Backup) ---
    public ResponseEntity<Flux<DataBuffer>> streamVideo(String remoteUrl, String filename, Map<String, String> customHeaders) {
        try {
            var requestSpec = webClient.get().uri(URI.create(remoteUrl));
            boolean uaSet = false;
            if (customHeaders != null) {
                for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("Host") || entry.getKey().equalsIgnoreCase("Content-Length")) continue;
                    if (entry.getKey().equalsIgnoreCase(HttpHeaders.USER_AGENT)) uaSet = true;
                    requestSpec.header(entry.getKey(), entry.getValue());
                }
            }
            if (!uaSet) requestSpec.header(HttpHeaders.USER_AGENT, USER_AGENT);
            Flux<DataBuffer> videoStream = requestSpec.retrieve().bodyToFlux(DataBuffer.class);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(videoStream);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}