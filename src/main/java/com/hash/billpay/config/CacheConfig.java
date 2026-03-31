package com.hash.billpay.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures Caffeine-based caching for the application.
 * Caffeine is high performance cache In memory local cache
 * Ehcache has more features like distributed caching & disk based caching
 * Exchange rate lookups are cached to avoid redundant external API calls.
 * TTL of 1 hour, max 500 entries
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("exchangeRates");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(500)
                .recordStats());
        return cacheManager;
    }
}

