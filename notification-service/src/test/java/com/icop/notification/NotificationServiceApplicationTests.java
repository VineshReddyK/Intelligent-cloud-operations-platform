package com.icop.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Context-loads smoke test. No datasource overrides here — this service has
 * no DB — just Kafka parked (auto-startup off) so the listeners don't try to
 * reach a broker that isn't running in CI.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false",
        "management.server.port=0"
})
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
