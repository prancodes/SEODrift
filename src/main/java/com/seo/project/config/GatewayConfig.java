package com.seo.project.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.function.Function;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

/**
 * GatewayConfig defines Spring Cloud Gateway WebMVC edge routing rules.
 * Intercepts incoming requests, proxies them to downstream APIs,
 * and integrates Redis rate limiting to prevent API abuse.
 */
@Slf4j
@Configuration
public class GatewayConfig {

    @Value("${youtube.api.key}")
    private String apiKey;

    private final StringRedisTemplate redisTemplate;

    /**
     * Constructor injection for Redis template.
     */
    public GatewayConfig(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Edge router definition routing internal UI/API traffic and proxying YouTube Search API calls.
     * Registers caching proxy filters and a custom sliding-window Redis rate limiter.
     */
    @Bean
    public RouterFunction<ServerResponse> gatewayRouter() {
        return route("youtube_proxy")
                .route(req -> req.path().startsWith("/api/gateway/youtube/"), http())
                .before(uri("https://www.googleapis.com"))
                .before(rewritePath("/api/gateway/youtube/(?<segment>.*)", "/youtube/v3/${segment}"))
                .before(addApiKey(apiKey))
                .filter(rateLimiterFilter(redisTemplate))
                .build();
    }

    /**
     * Custom filter function to append the system-wide YouTube API Key to proxied requests.
     */
    private static Function<ServerRequest, ServerRequest> addApiKey(String apiKey) {
        return request -> {
            URI newUri = UriComponentsBuilder.fromUri(request.uri())
                    .queryParam("key", apiKey)
                    .build(true)
                    .toUri();
            return ServerRequest.from(request)
                    .uri(newUri)
                    .build();
        };
    }

    /**
     * Custom sliding window rate limiter filter using StringRedisTemplate.
     * Evaluates requests based on client host IP address, enforcing a 10 requests per minute limit.
     */
    private static HandlerFilterFunction<ServerResponse, ServerResponse> rateLimiterFilter(StringRedisTemplate redisTemplate) {
        return (request, next) -> {
            String clientIp = request.remoteAddress()
                    .map(addr -> addr.getAddress().getHostAddress())
                    .orElse("anonymous");
            String rateLimitKey = "rate_limit:" + clientIp;

            Long currentCount = redisTemplate.opsForValue().increment(rateLimitKey);

            if (currentCount == null) {
                log.error("Failed to increment rate limit key inside Redis.");
                return ServerResponse.status(500).body("Internal Rate Limiter Error");
            }

            if (currentCount == 1) {
                // Initialize sliding window of 60 seconds on first request
                redisTemplate.expire(rateLimitKey, Duration.ofSeconds(60));
            }

            int limit = 10; // Enforce 10 requests per minute limit
            if (currentCount > limit) {
                log.warn("Rate limit exceeded for client [{}]. Volume: {}/{}", clientIp, currentCount, limit);
                return ServerResponse.status(429)
                        .header("X-RateLimit-Limit", String.valueOf(limit))
                        .header("X-RateLimit-Remaining", "0")
                        .header("X-RateLimit-Reset", String.valueOf(redisTemplate.getExpire(rateLimitKey)))
                        .body("Too Many Requests. Rate limit of " + limit + " requests per minute exceeded.");
            }

            ServerResponse response = next.handle(request);
            return ServerResponse.from(response)
                    .header("X-RateLimit-Limit", String.valueOf(limit))
                    .header("X-RateLimit-Remaining", String.valueOf(limit - currentCount))
                    .build();
        };
    }
}
