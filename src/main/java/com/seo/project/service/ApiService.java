package com.seo.project.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * ApiService manages the lifecycle and validation of external API credentials.
 */
@Slf4j
@Service
public class ApiService {

    @Value("${youtube.api.key}")
    private String apiKey;

    /**
     * Diagnostic check to ensure the YouTube Data API key is correctly loaded 
     * from environment variables during application startup.
     */
    @PostConstruct
    public void validateApiKey() {
        if (apiKey != null && !apiKey.isEmpty()) {
            log.info("YouTube API credentials successfully validated and loaded.");
        } else {
            log.error("CRITICAL: YouTube API Key is missing! Video analysis features will be disabled.");
        }
    }
}
