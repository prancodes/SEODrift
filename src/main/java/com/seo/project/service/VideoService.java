package com.seo.project.service;

import com.seo.project.dto.VideoInfo;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class VideoService {

    private final WebClient webClient;
    private final YtDlpService ytDlpService;

    // Inject Dependencies
    public VideoService(WebClient.Builder builder, YtDlpService ytDlpService) {
        // Increase buffer limits for large downloads if necessary
        this.webClient = builder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.ytDlpService = ytDlpService;
    }

    /**
     * Step 1: Get Metadata & Links using yt-dlp
     */
    public VideoInfo getVideoDetails(String videoUrl) {
        return ytDlpService.fetchVideoInfo(videoUrl);
    }

    /**
     * Step 2: Stream the content from Google to User
     * Returns a Reactive Flux (Non-blocking stream)
     */
    public ResponseEntity<Flux<DataBuffer>> streamVideo(String remoteUrl, String filename) {
        try {
            // We set a User-Agent to mimick a browser, sometimes helps with YouTube throttling
            Flux<DataBuffer> videoStream = webClient.get()
                    .uri(remoteUrl)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .retrieve()
                    .bodyToFlux(DataBuffer.class);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(videoStream);

        } catch (Exception e) {
            System.err.println("Streaming Error: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}