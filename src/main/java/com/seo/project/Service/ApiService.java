package com.seo.project.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class ApiService {

    @Value("${youtube.api.key}")
    private String apiKey;

    @PostConstruct
    public void apiKeyStatus() {
        if (apiKey != null && !apiKey.isEmpty()) {
            System.out.println("✅ SUCCESS: API Key loaded successfully!");
        } else {
            System.err.println("❌ ERROR: API Key is missing!");
        }
    }
}
