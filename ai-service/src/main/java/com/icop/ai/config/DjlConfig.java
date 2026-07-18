package com.icop.ai.config;

import ai.djl.ndarray.NDManager;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One root NDManager for the whole app. DJL managers own native (off-heap)
 * memory, so the lifecycle matters: child arrays are created per-operation
 * in try-with-resources, and this root gets closed on shutdown to release
 * the native engine cleanly.
 */
@Configuration
public class DjlConfig {

    private NDManager rootManager;

    @Bean
    public NDManager ndManager() {
        rootManager = NDManager.newBaseManager();
        return rootManager;
    }

    @PreDestroy
    public void close() {
        if (rootManager != null) {
            rootManager.close();
        }
    }
}
