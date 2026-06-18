package com.icop.ai.config;

import ai.djl.ndarray.NDManager;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
