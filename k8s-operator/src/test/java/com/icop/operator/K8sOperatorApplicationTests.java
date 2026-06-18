package com.icop.operator;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "operator.reconcile-interval-ms=999999999",
        "management.server.port=0"
})
class K8sOperatorApplicationTests {

    @MockBean
    KubernetesClient kubernetesClient;

    @Test
    void contextLoads() {}
}
