package com.seo.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.resource.ClientResources;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
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
    public LettuceConnectionFactory redisConnectionFactory(ClientResources clientResources) {
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

        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        builder.clientOptions(clientOptions);
        builder.clientResources(clientResources);
        if (sslEnabled) {
            builder.useSsl();
        }

        // Configure GenericObjectPool for Lettuce connection pooling to reuse connections
        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(2);
        poolConfig.setMaxWait(Duration.ofMillis(2000));
        builder.poolConfig(poolConfig);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, builder.build());
        return factory;
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
                .withCacheConfiguration("channelIntelligence", config.entryTtl(Duration.ofMinutes(15)))
                .build();
    }
}
