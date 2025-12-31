package com.seo.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Profile("prod") // Only loads in Production
public class KeepAliveService {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveService.class);
    private final RestClient restClient;
    private final String appUrl;

    // 1. Add a flag to track if we've already logged the warning
    private boolean hasLoggedMissingUrl = false;

    // Constructor Injection (Best Practice for Testability)
    public KeepAliveService(@Value("${app.public.url:}") String appUrl) {
        this.appUrl = appUrl;
        // Build RestClient internally to avoid dependency conflicts
        this.restClient = RestClient.builder().build();
    }

    // Runs every 45 seconds (45000 milliseconds)
    @Scheduled(fixedRate = 45000)
    public void pingSelf() {
        // Assign to local variable for thread safety
        final String targetUrl = this.appUrl;

        // 3. Logic: Skip if URL is invalid, empty, or localhost
        if (targetUrl == null || targetUrl.isEmpty() || targetUrl.contains("localhost")) {
            if (!hasLoggedMissingUrl) {
                logger.warn("⚠️ KeepAlive: Skipping ping (URL is localhost or empty). Set 'APP_PUBLIC_URL' in environment variables.");
                hasLoggedMissingUrl = true; // Mark as logged so it doesn't repeat
            }
            return;
        }

        try {
            logger.debug("Pinging self at {}", targetUrl);
            
            restClient.get()
                .uri(targetUrl)
                .retrieve()
                .toBodilessEntity(); // Discard body, we just want the 200 OK

        } catch (Exception e) {
            // Log error clearly
            logger.error("❌ KeepAlive Ping Failed: {}", e.getMessage());
        }
    }
}