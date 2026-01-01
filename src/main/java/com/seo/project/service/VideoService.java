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
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoService {

    private final WebClient webClient;
    private final YtDlpService ytDlpService;
    // ✅ FIXED: Updated to Android User-Agent
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.102 Mobile Safari/537.36";
    
    // ✅ PRODUCTION FIX: Retry configuration
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 3000; // 3 seconds for downloads (longer than fetch)

    // Task Store
    private final Map<String, DownloadTask> tasks = new ConcurrentHashMap<>();

    // Regex to find percentage (e.g., " 45.5%")
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+\\.\\d+)%");

    public VideoService(WebClient.Builder builder, YtDlpService ytDlpService) {
        this.webClient = builder.codecs(c -> c.defaultCodecs().maxInMemorySize(-1)).build();
        this.ytDlpService = ytDlpService;
    }

    public VideoInfo getVideoDetails(String videoUrl) { 
        return ytDlpService.fetchVideoInfo(videoUrl); 
    }

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
     * ✅ FIXED: Unified Background Downloader for large Video & Audio files
     * ✅ PRODUCTION FIX: Added retry logic with exponential backoff for bot detection
     */
    public void startMergeTask(String token, String originalUrl, String formatId, String outputFormat, boolean isAudio) {
        DownloadTask task = new DownloadTask();
        task.isAudio = isAudio;
        tasks.put(token, task);

        new Thread(() -> {
            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                if (executeMergeTaskAttempt(token, originalUrl, formatId, outputFormat, isAudio, attempt)) {
                    // Success
                    return;
                }
                
                DownloadTask currentTask = tasks.get(token);
                if (currentTask != null && attempt < MAX_RETRIES - 1) {
                    long delayMs = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, attempt);
                    currentTask.status = "Retry attempt " + (attempt + 2) + "/" + MAX_RETRIES + " in " + (delayMs / 1000) + "s...";
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        if (currentTask != null) {
                            currentTask.status = "ERROR: Download interrupted";
                        }
                        return;
                    }
                }
            }
            
            // All retries exhausted
            DownloadTask currentTask = tasks.get(token);
            if (currentTask != null) {
                currentTask.status = "ERROR: All download attempts failed. YouTube may require authentication.";
            }
        }).start();
    }

    /**
     * ✅ PRODUCTION FIX: Single attempt to download/merge video with bot detection bypass
     */
    private boolean executeMergeTaskAttempt(String token, String originalUrl, String formatId, String outputFormat, boolean isAudio, int attemptNumber) {
        Path tempFile = null;
        final Process[] processWrapper = new Process[1]; // Wrapper to allow assignment in lambda
        try {
            DownloadTask task = tasks.get(token);
            if (task == null) return false;

            // Determine extension
            String ext = outputFormat; // e.g. "mp4" or "m4a"
            tempFile = Files.createTempFile("seodrift_" + token, "." + ext);

            // Delete the empty file immediately so yt-dlp creates it fresh
            Files.deleteIfExists(tempFile);
            task.filePath = tempFile;

            List<String> command = new ArrayList<>();
            command.add("yt-dlp");
            command.add("--no-warnings");
            command.add("--force-ipv4");           // Force IPv4 for reliable downloads
            command.add("--socket-timeout");       // Timeout for slow networks
            command.add("30");
            command.add("--retries");              // Retry on failure (internal)
            command.add("2");
            command.add("--fragment-retries");     // Retry fragment fetching for HLS/DASH
            command.add("2");
            command.add("--http-chunk-size");      // ✅ FIXED: 25MB chunks for large downloads
            command.add("26214400");               // 25MB in bytes
            command.add("--buffer-size");          // Increase buffer size
            command.add("20480");                  // 20KB buffer
            command.add("--newline");              // Forces real-time output for Java to read
            command.add("--encoding");
            command.add("utf-8");                  // ✅ PRODUCTION FIX: Explicit encoding
            // ✅ CRITICAL BYPASS: Force YouTube to treat this as an Android App request
            command.add("--extractor-args");       
            command.add("youtube:player_client=android&hl=en");
            command.add("--user-agent");
            command.add(USER_AGENT);
            
            // ✅ PRODUCTION FIX: Add cookies file if it exists (for YouTube authentication)
            String cookieFile = getCookiesPath();
            Path cookiesPath = Paths.get(cookieFile);
            
            if (Files.exists(cookiesPath)) {
                command.add("--cookies");
                command.add(cookieFile);
                if (attemptNumber == 0) {
                    System.out.println("Using cookies from: " + cookieFile);
                }
            } else if (attemptNumber == 0) {
                System.out.println("⚠️  WARNING: Cookies file not found. For production, consider mounting YouTube cookies to bypass bot detection.");
            }
            
            if (isAudio) {
                // AUDIO MODE: Download & Extract Audio
                command.add("-f");
                command.add(formatId);
                command.add("-x");                 // Extract audio
                command.add("--audio-format");
                command.add("m4a");               // Force M4A for compatibility
            } else {
                // VIDEO MODE: Merge Video+Audio with best quality
                command.add("-f");
                command.add(formatId + "+bestaudio/best");
                command.add("--merge-output-format");
                command.add(ext);
            }

            command.add("-o");
            command.add(tempFile.toAbsolutePath().toString());
            command.add("--force-overwrites");
            command.add("--progress");             // Enable progress reporting
            command.add("--progress-template");    // Custom progress format
            command.add("[download] %(progress._default_hook)s");
            command.add(originalUrl);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);         // Keep stdout and stderr separate
            
            processWrapper[0] = pb.start();

            // Real-time Status Updates from stdout
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(processWrapper[0].getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Parse [download] 45.5% ...
                    if (line.contains("[download]") && line.contains("%")) {
                        Matcher m = PERCENT_PATTERN.matcher(line);
                        if (m.find()) {
                            task.status = "Downloading: " + m.group(1) + "%";
                        }
                    } 
                    // Handle merging phase
                    else if (line.contains("[Merger]")) {
                        task.status = "Merging Video & Audio...";
                    } 
                    // Handle audio extraction
                    else if (line.contains("[ExtractAudio]") || line.contains("[Fixup]")) {
                        task.status = "Downloading: 100% (Extracting Audio...)";
                    }
                    // Handle postprocessing
                    else if (line.contains("[ffmpeg]") || line.contains("postprocess")) {
                        task.status = "Processing Video...";
                    }
                }
            }

            // Also consume stderr to avoid deadlock
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(processWrapper[0].getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Log all errors with attempt info
                        if (line.contains("Sign in to confirm")) {
                            System.err.println("🤖 BOT DETECTION (attempt " + (attemptNumber + 1) + "/" + MAX_RETRIES + "): " + line);
                        } else if (line.contains("error") || line.contains("ERROR")) {
                            System.err.println("yt-dlp error (attempt " + (attemptNumber + 1) + "/" + MAX_RETRIES + "): " + line);
                        }
                    }
                } catch (Exception ignored) {}
            }).start();

            int exitCode = processWrapper[0].waitFor();
            
            if (exitCode == 0) {
                if (Files.exists(tempFile) && Files.size(tempFile) > 0) {
                    task.status = "COMPLETED";
                    return true;
                } else {
                    task.status = "ERROR: Output file is empty or missing";
                    return false;
                }
            } else {
                System.err.println("yt-dlp exit code: " + exitCode + " (attempt " + (attemptNumber + 1) + "/" + MAX_RETRIES + ")");
                task.status = "ERROR: Download failed (exit code: " + exitCode + ")";
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            DownloadTask task = tasks.get(token);
            if (task != null) {
                task.status = "ERROR: Download interrupted";
            }
            System.err.println("Download interrupted (attempt " + (attemptNumber + 1) + "/" + MAX_RETRIES + "): " + e.getMessage());
            return false;
        } catch (Exception e) {
            DownloadTask task = tasks.get(token);
            if (task != null) {
                task.status = "ERROR: " + e.getMessage();
            }
            System.err.println("Download error (attempt " + (attemptNumber + 1) + "/" + MAX_RETRIES + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (processWrapper[0] != null && processWrapper[0].isAlive()) {
                processWrapper[0].destroyForcibly();
            }
        }
    }

    // --- STANDARD STREAMING (Backup/Direct Link) ---
    
    public ResponseEntity<Flux<DataBuffer>> streamVideo(String remoteUrl, String filename, Map<String, String> customHeaders) {
        try {
            var requestSpec = webClient.get().uri(URI.create(remoteUrl));
            boolean uaSet = false;
            
            if (customHeaders != null) {
                for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("Host") || 
                        entry.getKey().equalsIgnoreCase("Content-Length")) continue;
                    if (entry.getKey().equalsIgnoreCase(HttpHeaders.USER_AGENT)) uaSet = true;
                    requestSpec.header(entry.getKey(), entry.getValue());
                }
            }
            
            // Ensure Android UA is used here too if not provided
            if (!uaSet) requestSpec.header(HttpHeaders.USER_AGENT, USER_AGENT);
            
            Flux<DataBuffer> videoStream = requestSpec.retrieve()
                .bodyToFlux(DataBuffer.class)
                .doOnError(e -> System.err.println("Stream error: " + e.getMessage()));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(videoStream);
        } catch (Exception e) {
            System.err.println("Streaming failed: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
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