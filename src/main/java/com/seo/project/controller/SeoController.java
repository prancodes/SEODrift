package com.seo.project.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller to dynamically serve robots.txt and sitemap.xml
 * by reading resource templates from the classpath and resolving the active base URL.
 */
@Slf4j
@RestController
public class SeoController {

    private final ResourceLoader resourceLoader;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public SeoController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    private String getCleanBaseUrl(HttpServletRequest request) {
        String url = baseUrl;
        // Fallback to request URL scheme/host if app.base-url is default/localhost/empty
        if (url == null || url.trim().isEmpty() || url.contains("localhost") || url.contains("127.0.0.1")) {
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            url = scheme + "://" + serverName;
            if (serverPort != 80 && serverPort != 443) {
                url += ":" + serverPort;
            }
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String loadAndResolveTemplate(String resourcePath, String cleanBaseUrl) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
        String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        return template.replace("${baseUrl}", cleanBaseUrl);
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getRobotsTxt(HttpServletRequest request) {
        log.debug("Serving resolved robots.txt");
        try {
            String cleanUrl = getCleanBaseUrl(request);
            return loadAndResolveTemplate("seo/robots.txt", cleanUrl);
        } catch (IOException e) {
            log.error("Failed to load robots.txt template", e);
            return "User-agent: *\nDisallow: /";
        }
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getSitemapXml(HttpServletRequest request) {
        log.debug("Serving resolved sitemap.xml");
        try {
            String cleanUrl = getCleanBaseUrl(request);
            return loadAndResolveTemplate("seo/sitemap.xml", cleanUrl);
        } catch (IOException e) {
            log.error("Failed to load sitemap.xml template", e);
            return "";
        }
    }
}
