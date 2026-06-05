package com.seo.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Exposes global application properties to all Thymeleaf templates.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Makes the Google Client ID available as ${googleClientId} in all templates.
     */
    @ModelAttribute("googleClientId")
    public String getGoogleClientId() {
        return googleClientId;
    }

    /**
     * Makes the Base URL available as ${baseUrl} in all templates.
     */
    @ModelAttribute("baseUrl")
    public String getBaseUrl() {
        if (baseUrl == null) {
            return "";
        }
        // Ensure we don't have a trailing slash so that path joining is consistent
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * Makes the active request URI available as ${currentUri} in all templates.
     */
    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }
}
