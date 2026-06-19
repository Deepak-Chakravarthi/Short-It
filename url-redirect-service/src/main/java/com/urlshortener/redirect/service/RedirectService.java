package com.urlshortener.redirect.service;

import com.urlshortener.redirect.exception.ShortUrlNotFoundException;
import com.urlshortener.redirect.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the hot path: short key → long URL resolution.
 * <p>
 * Cache strategy:
 * L1: Redis (shared across all redirect-service instances)
 * TTL mirrors the URL expiry or defaults to 24h.
 * L2: PostgreSQL (source of truth)
 * <p>
 * Click count tracking is fire-and-forget async so it does NOT
 * block the HTTP redirect response. 302 goes out immediately.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectService {

    private final UrlRepository urlRepository;

    /**
     * Resolve shortKey → longUrl.
     * <p>
     * Cache key = shortKey; cached for 24h by default.
     * On cache miss, falls through to DB, then populates cache.
     */
    @Cacheable(value = "url-mappings", key = "#shortKey")
    @Transactional(readOnly = true)
    public String resolveLongUrl(String shortKey) {
        log.debug("Cache miss for shortKey={}, querying DB", shortKey);

        return urlRepository.findActiveByShortKey(shortKey)
                .map(mapping -> {
                    log.info("Resolved shortKey={} → {}", shortKey, mapping.getLongUrl());
                    return mapping.getLongUrl();
                })
                .orElseThrow(() -> {
                    log.warn("Short key not found or expired: {}", shortKey);
                    return new ShortUrlNotFoundException(shortKey);
                });
    }

}
