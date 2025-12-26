package com.seo.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // Set timeout to -1 (Infinite) or e.g., 3600000 (1 hour in ms)
        // This prevents "AsyncRequestTimeoutException" when merging large videos
        configurer.setDefaultTimeout(-1); 
    }
}