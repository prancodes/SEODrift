package com.seo.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * CacheConfig configures the caching infrastructure for the platform.
 * Binds Spring Cache to Redis using Lettuce connection pooling and JSON serializers.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.ssl.enabled}")
    private boolean sslEnabled;


    /**
     * Configures the LettuceConnectionFactory with SSL and peer verification configurations.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(host);
        serverConfig.setPort(port);
        if (password != null && !password.isEmpty()) {
            serverConfig.setPassword(password);
        }

        io.lettuce.core.SocketOptions socketOptions = io.lettuce.core.SocketOptions.builder()
                .keepAlive(true)
                .build();
        io.lettuce.core.ClientOptions clientOptions = io.lettuce.core.ClientOptions.builder()
                .socketOptions(socketOptions)
                .build();

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
        builder.clientOptions(clientOptions);
        if (sslEnabled) {
            builder.useSsl();
        }

        return new LettuceConnectionFactory(serverConfig, builder.build());
    }

    /**
     * Configures the RedisCacheManager bean with custom JSON-based serialization and default TTL.
     */
    @Bean
    public RedisCacheManager cacheManager(LettuceConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // Default time-to-live is 1 hour
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()))
                .disableCachingNullValues(); // Prevent caching null values

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
