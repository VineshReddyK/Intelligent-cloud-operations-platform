package com.icop.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-long-enough-for-hmac",
        "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
        "spring.cloud.gateway.routes[1].uri=http://localhost:8082",
        "spring.cloud.gateway.routes[2].uri=http://localhost:8083",
        "spring.cloud.gateway.routes[3].uri=http://localhost:8084"
})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
