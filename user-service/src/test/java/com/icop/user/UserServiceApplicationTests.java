package com.icop.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false",
        "jwt.secret=test-secret-key-that-is-long-enough-for-hmac",
        "jwt.expiration=86400000",
        "spring.flyway.enabled=false",
        "management.server.port=0"
})
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
