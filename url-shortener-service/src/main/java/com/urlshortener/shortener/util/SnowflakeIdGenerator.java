package com.urlshortener.shortener.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Snowflake ID Generator
 *
 * This guarantees uniqueness across servers without coordination per request.
 * Zookeeper handles worker ID assignment at startup only.
 */
@Component
@Slf4j
public class SnowflakeIdGenerator {

    // Custom epoch: 2024-01-01T00:00:00Z in milliseconds
    private static final long CUSTOM_EPOCH = 1704067200000L;

    private static final int WORKER_ID_BITS   = 10;
    private static final int SEQUENCE_BITS    = 12;

    private static final long MAX_WORKER_ID   = ~(-1L << WORKER_ID_BITS);   // 1023
    private static final long MAX_SEQUENCE    = ~(-1L << SEQUENCE_BITS);    // 4095

    private static final int WORKER_ID_SHIFT  = SEQUENCE_BITS;              // 12
    private static final int TIMESTAMP_SHIFT  = SEQUENCE_BITS + WORKER_ID_BITS; // 22

    private final long workerId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(ZookeeperWorkerRegistry workerRegistry) {
        this.workerId = workerRegistry.getWorkerId();
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                "Worker ID must be between 0 and " + MAX_WORKER_ID + ", got: " + workerId
            );
        }
        log.info("SnowflakeIdGenerator initialized with workerId={}", workerId);
    }

    /**
     * Generate the next unique Snowflake ID.
     * Thread-safe via synchronization.
     */
    public synchronized long nextId() {
        long currentTimestamp = currentTimeMs();

        if (currentTimestamp < lastTimestamp) {
            long drift = lastTimestamp - currentTimestamp;
            log.warn("Clock moved backwards by {}ms. Waiting to recover...", drift);
            currentTimestamp = waitForNextMillis(lastTimestamp);
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence exhausted within same millisecond → wait for next ms
                currentTimestamp = waitForNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        long id = ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;

        log.debug("Generated Snowflake ID: {} (ts={}, worker={}, seq={})",
                id, currentTimestamp, workerId, sequence);
        return id;
    }

    private long waitForNextMillis(long lastTs) {
        long ts = currentTimeMs();
        while (ts <= lastTs) {
            ts = currentTimeMs();
        }
        return ts;
    }

    private long currentTimeMs() {
        return System.currentTimeMillis();
    }
}
