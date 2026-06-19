package com.urlshortener.redirect.config;

/**
 * No Redis config needed for local dev (cache.type=simple).
 *
 * For production with Redis, declare RedisCacheManager bean here
 * and change cache.type=redis in application.yml.
 */
public class RedisConfig {
}
