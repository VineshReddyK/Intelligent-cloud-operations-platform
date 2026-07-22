package com.icop.operator.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Kubernetes client. The no-arg builder auto-configures itself — in-cluster
 * it picks up the pod's service-account token and CA; locally it reads your
 * ~/.kube/config. Same code runs in both places, which is exactly what you
 * want for an operator.
 */
@Configuration
public class FabricClientConfig {

    private KubernetesClient client;

    @Bean
    public KubernetesClient kubernetesClient() {
        client = new KubernetesClientBuilder().build();
        return client;
    }

    // the client holds an HTTP connection pool — close it on shutdown so we
    // don't leak sockets on redeploys
    @PreDestroy
    public void close() {
        if (client != null) client.close();
    }
}
