package com.seo.project.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;

@Service
@Profile("prod") // Only loads in Production
public class KeepAliveService {

    private final WebClient webClient;

    // 1. Add a flag to track if we've already logged the warning
    private boolean hasLoggedMissingUrl = false;

    // Inject your Render URL from application.properties or Environment Variables
    @Value("${app.public.url}") 
    private String appUrl;

    public KeepAliveService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // Runs every 45 seconds (45000 milliseconds)
    @Scheduled(fixedRate = 45000)
    public void pingSelf() {
        if (appUrl == null || appUrl.contains("localhost") || appUrl.isEmpty()) {
            // 2. Only print once
            if (!hasLoggedMissingUrl) {
                System.out.println("⚠️ KeepAlive: Skipping ping (URL is localhost or empty). Set APP_PUBLIC_URL.");
                hasLoggedMissingUrl = true; // Mark as logged so it doesn't repeat
            }
            return;
        }

        try {
            webClient.get()
                .uri(appUrl) // Pings the home page ("/")
                .retrieve()
                .toBodilessEntity() // We don't care about the body, just the connection
                .subscribe(
                    response -> {},
                    error -> System.err.println("❌ KeepAlive Ping Failed: " + error.getMessage())
                );
        } catch (Exception e) {
            System.err.println("❌ KeepAlive Error: " + e.getMessage());
        }
    }
}