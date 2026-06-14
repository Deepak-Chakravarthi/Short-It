package com.urlshortener.shortener.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Apache Curator (Zookeeper) client.
 *
 * ExponentialBackoffRetry: retries with increasing delays on transient failures.
// * Session timeout: how long Zookeeper waits before declaring this node dead
 *   and deleting its ephemeral nodes. ~30s is a safe default.
 */
@Configuration
@Slf4j
public class ZookeeperConfig {

    @Value("${zookeeper.connection-string:localhost:2181}")
    private String connectionString;

    @Value("${zookeeper.session-timeout-ms:30000}")
    private int sessionTimeoutMs;

    @Value("${zookeeper.connection-timeout-ms:10000}")
    private int connectionTimeoutMs;

    @Bean(destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(connectionString)
                .sessionTimeoutMs(sessionTimeoutMs)
                .connectionTimeoutMs(connectionTimeoutMs)
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .namespace("url-shortener") // All znodes scoped under /url-shortener
                .build();

        client.start();
        log.info("CuratorFramework started, connecting to Zookeeper at {}", connectionString);
        return client;
    }
}
