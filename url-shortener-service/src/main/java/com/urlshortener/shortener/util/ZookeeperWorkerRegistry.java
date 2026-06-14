package com.urlshortener.shortener.util;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class ZookeeperWorkerRegistry {

    private static final String WORKER_BASE_PATH = "/url-shortener/workers";
    private static final String COUNTER_PATH     = "/url-shortener/worker-counter";
    private static final int    MAX_WORKERS      = 1024;

    private final CuratorFramework curator;

    @Getter
    private long workerId;
    private String registeredNodePath;

    public ZookeeperWorkerRegistry(CuratorFramework curator) {
        this.curator = curator;
        this.workerId = registerAndAcquireWorkerId();
    }

    private long registerAndAcquireWorkerId() {
        try {
            ensureBasePath();

            // Use a distributed atomic integer as a monotonic counter for worker IDs
            DistributedAtomicInteger counter = new DistributedAtomicInteger(
                curator, COUNTER_PATH, new ExponentialBackoffRetry(100, 5)
            );

            AtomicValue<Integer> result = counter.increment();
            if (!result.succeeded()) {
                throw new RuntimeException("Failed to acquire worker ID from Zookeeper counter");
            }

            long id = (result.postValue() - 1) % MAX_WORKERS;

            // Register an ephemeral sequential node so cluster members are discoverable
            String nodePath = WORKER_BASE_PATH + "/worker-" + id;
            registeredNodePath = curator.create()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(nodePath, getInstanceMetadata().getBytes());

            log.info("Registered with Zookeeper: nodeId={}, workerId={}", registeredNodePath, id);
            return id;

        } catch (Exception e) {
            log.error("Zookeeper registration failed. Falling back to random worker ID.", e);
            // Fallback: random ID (acceptable for dev/testing; not for prod multi-node)
            long fallback = (long)(Math.random() * MAX_WORKERS);
            log.warn("Using fallback workerId={}", fallback);
            return fallback;
        }
    }

    private void ensureBasePath() throws Exception {
        if (curator.checkExists().forPath(WORKER_BASE_PATH) == null) {
            curator.create().creatingParentsIfNeeded().forPath(WORKER_BASE_PATH);
        }
    }

    private String getInstanceMetadata() {
        // Store host/pid metadata in the ephemeral node for observability
        String host = System.getenv().getOrDefault("HOSTNAME", "unknown-host");
        String pid  = ProcessHandle.current().pid() + "";
        return String.format("{\"host\":\"%s\",\"pid\":\"%s\"}", host, pid);
    }

    @PreDestroy
    public void deregister() {
        try {
            if (registeredNodePath != null) {
                curator.delete().forPath(registeredNodePath);
                log.info("Deregistered from Zookeeper: {}", registeredNodePath);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanly deregister from Zookeeper", e);
            // Non-fatal: ephemeral node will expire on session timeout anyway
        }
    }
}
