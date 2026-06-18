package com.icop.operator.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FabricClientConfig {

    private KubernetesClient client;

    @Bean
    public KubernetesClient kubernetesClient() {
        client = new KubernetesClientBuilder().build();
        return client;
    }

    @PreDestroy
    public void close() {
        if (client != null) client.close();
    }
}
