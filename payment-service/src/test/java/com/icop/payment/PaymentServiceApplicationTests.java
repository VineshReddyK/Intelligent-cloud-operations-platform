package com.icop.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Context-loads smoke test, same recipe as the other services: H2 standing in
 * for Postgres, Kafka listeners parked, Flyway off. If the wiring is broken,
 * this fails the build before anything ships.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false",
        "spring.flyway.enabled=false",
        "management.server.port=0"
})
class PaymentServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
