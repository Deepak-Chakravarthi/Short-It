package com.urlshortener.shortener.scheduler;

import com.urlshortener.shortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredUrlCleanupJob {

    private static final int BATCH_SIZE = 1000;

    private final UrlRepository urlRepository;
    private final CacheManager  cacheManager;


    @Scheduled(cron = "${app.cleanup.cron:0 0 2 * * *}")
    @Transactional
    public void purgeExpiredUrls() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Starting expired URL cleanup job at {}", now);

        int totalDeleted = 0;
        int batchDeleted;

        do {
            batchDeleted = urlRepository.deleteExpiredBefore(now);
            totalDeleted += batchDeleted;
            log.debug("Cleanup batch: deleted {} expired records", batchDeleted);

            if (batchDeleted > 0) {
                // Flush between batches so each chunk is its own smaller transaction window
                // (Spring's @Transactional wraps the whole method, but we log progress here)
                log.debug("Cumulative deleted so far: {}", totalDeleted);
            }
        } while (batchDeleted >= BATCH_SIZE);

        // Redis: expired entries have their own TTL set at write time (24h default),
        // but the cache key space may diverge from DB. Clear the relevant cache namespace.
        // In production with a large cache, prefer targeted eviction per deleted shortKey.
        if (totalDeleted > 0) {
            clearExpiredFromRedisCache();
        }

        log.info("Expired URL cleanup complete. Total deleted: {} records", totalDeleted);
    }


    private void clearExpiredFromRedisCache() {
        try {
            var cache = cacheManager.getCache("url-mappings");
            if (cache != null) {
                cache.clear();
                log.info("Redis url-mappings cache cleared after cleanup");
            }
        } catch (Exception e) {
            log.warn("Failed to clear Redis cache after cleanup — entries will self-expire", e);
        }
    }
}
