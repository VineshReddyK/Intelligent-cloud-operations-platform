package com.icop.ai;

import ai.djl.ndarray.NDManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "prometheus.url=http://localhost:9090",
        "ai.anomaly.poll-interval-ms=999999999"
})
class AiServiceApplicationTests {

    @MockBean
    NDManager ndManager;

    @Test
    void contextLoads() {}
}
