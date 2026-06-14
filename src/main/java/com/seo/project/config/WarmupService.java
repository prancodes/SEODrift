package com.seo.project.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * WarmupService — eliminates the "cold connection" penalty on the very first
 * request after a Cloud Run cold start.
 *
 * WHY THIS EXISTS:
 * With spring.main.lazy-initialization=true, Spring defers bean creation until
 * first use. This means HikariCP (DB pool) and Lettuce (Redis client) don't
 * open their connections until the first real user request — adding 1-3s of
 * wait time for that user.
 *
 * WHAT THIS DOES:
 * Listens for ApplicationReadyEvent (fires once, right after Spring context
 * is fully started) and asynchronously opens the DB + Redis connections in
 * the background. By the time the first user arrives, the connections are
 * already pooled and warm.
 *
 * COST: Zero ongoing cost. This runs ONCE per container startup, not on a
 * schedule. No Neon compute hours are wasted between user requests.
 *
 * NOTE ON KEEPALIVES:
 * We intentionally do NOT schedule a periodic DB ping here. Running SELECT 1
 * every few minutes would keep Neon DB awake 24/7, exhausting free-tier
 * compute hours. The tradeoff: Neon may cold-start (~1-2s) if no user has
 * hit a DB-backed page in 5+ minutes. This is acceptable vs. burning free
 * compute hours for zero users.
 */
@Slf4j
@Component
@EnableAsync
@Lazy(false)  // MUST be eager: with spring.main.lazy-initialization=true, a lazy @Component
              // is never instantiated at startup → @EventListener never registers → warmup silently dies.
              // @Lazy(false) opts this single bean out of global lazy-init.
@RequiredArgsConstructor
public class WarmupService {

    // IMPORTANT: We MUST use ObjectProvider here. If we inject DataSource directly,
    // Spring will eagerly initialize Hibernate on the main thread, adding 20-30s to startup!
    private final ObjectProvider<DataSource> dataSourceProvider;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    /**
     * Fires once immediately after Spring context is fully initialized.
     * Runs @Async so it doesn't delay the HTTP server from accepting requests
     * — warmup happens in parallel with the server becoming ready.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void warmupConnections() {
        log.info("Warmup: Starting async connection pre-warm...");
        warmupDatabase();
        warmupRedis();
        log.info("Warmup: Connection pre-warm complete.");
    }

    /**
     * Opens the first HikariCP connection to Neon DB.
     * Cost: ~1-3s once at startup. Saves this cost on the first user request.
     */
    private void warmupDatabase() {
        try {
            DataSource dataSource = dataSourceProvider.getIfAvailable();
            if (dataSource != null) {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("SELECT 1");
                    log.info("Warmup: Database connection established and pooled.");
                }
            }
        } catch (Exception e) {
            // Non-fatal: Neon DB may still be waking from auto-suspend.
            // HikariCP will retry automatically on the first real request.
            log.warn("Warmup: DB pre-warm skipped (Neon waking up): {}", e.getMessage());
        }
    }

    /**
     * Opens the Lettuce connection to Redis.
     * Cost: ~200-500ms once at startup.
     */
    private void warmupRedis() {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                String pong = redisTemplate.getConnectionFactory()
                        .getConnection()
                        .ping();
                log.info("Warmup: Redis connection established. PING -> {}", pong);
            }
        } catch (Exception e) {
            log.warn("Warmup: Redis pre-warm skipped: {}", e.getMessage());
        }
    }
}
