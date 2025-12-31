package com.seo.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

// import javax.net.ssl.SSLException;
import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // Set timeout to -1 (Infinite) or e.g., 3600000 (1 hour in ms)
        // This prevents "AsyncRequestTimeoutException" when merging large videos
        configurer.setDefaultTimeout(-1); 
    }

    /**
     * ✅ FIXED: Configure WebClient with optimized settings for large file downloads
     * - Increased buffer sizes for large video metadata
     * - Longer timeouts for slow connections
     * - Connection pooling for better resource management
     * - Support for large response bodies
     */
    @Bean
    public WebClient webClient() {
        // Connection pool configuration
        ConnectionProvider connectionProvider = ConnectionProvider.builder("video-pool")
            .maxConnections(100)                           // Max concurrent connections
            .maxIdleTime(Duration.ofSeconds(60))          // Keep-alive timeout
            .maxLifeTime(Duration.ofMinutes(30))          // Max connection lifetime
            .pendingAcquireTimeout(Duration.ofSeconds(60)) // Wait time for connection
            .pendingAcquireMaxCount(1000)                  // Queue size
            .evictInBackground(Duration.ofSeconds(120))   // Cleanup interval
            .build();

        // Build SSL context for untrusted certificates
        HttpClient httpClient;
        try {
            // Build SSL context first to catch exceptions early
            var sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
            
            httpClient = HttpClient.create(connectionProvider)
                .secure(sslSpec -> sslSpec.sslContext(sslContext))
                .responseTimeout(Duration.ofSeconds(60))       // Response timeout: 60 seconds
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // Connection timeout: 30 seconds
                .option(ChannelOption.SO_KEEPALIVE, true)      // Enable TCP keep-alive
                .option(ChannelOption.SO_RCVBUF, 1024 * 1024)  // 1MB socket receive buffer
                .option(ChannelOption.SO_SNDBUF, 1024 * 1024); // 1MB socket send buffer
        } catch (Exception e) {
            // Fallback: Create HttpClient without custom SSL context
            System.err.println("SSL context creation failed, using default: " + e.getMessage());
            httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(Duration.ofSeconds(60))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_RCVBUF, 1024 * 1024)
                .option(ChannelOption.SO_SNDBUF, 1024 * 1024);
        }

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> {
                // ✅ FIX: Remove memory limit to allow large responses
                configurer.defaultCodecs().maxInMemorySize(-1); // Unlimited
                configurer.defaultCodecs().enableLoggingRequestDetails(false);
            })
            .build();
    }
}