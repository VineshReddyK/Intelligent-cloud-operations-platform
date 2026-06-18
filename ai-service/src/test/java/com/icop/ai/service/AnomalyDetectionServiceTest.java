package com.icop.ai.service;

import ai.djl.ndarray.NDManager;
import com.icop.ai.dto.AnomalyResult;
import com.icop.ai.dto.MetricSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    NDManager manager;

    private AnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        // manager.create() returns null by default; the service catches the resulting
        // NullPointerException and falls back to pure-Java z-score computation.
        service = new AnomalyDetectionService(manager, 60, 2.5);
    }

    @Test
    void noAnomalyWhenWindowTooSmall() {
        MetricSnapshot snap = new MetricSnapshot("user-service", 10.0, 0.5, 100.0, 0.0, "CLOSED", Instant.now());
        List<AnomalyResult> results = service.detect(snap);
        assertThat(results).noneMatch(AnomalyResult::anomaly);
    }

    @Test
    void detectsAnomalyAfterWindowFills() {
        // Feed 15 normal values (error_rate ~0)
        for (int i = 0; i < 15; i++) {
            service.detect(new MetricSnapshot("svc", 10.0, 0.1, 50.0, 0.0, "CLOSED", Instant.now()));
        }
        // Feed a spike (error_rate=80%) — should trigger anomaly
        List<AnomalyResult> results = service.detect(
                new MetricSnapshot("svc", 10.0, 80.0, 50.0, 0.0, "CLOSED", Instant.now()));

        assertThat(results)
                .filteredOn(r -> "error_rate_pct".equals(r.metric()))
                .anyMatch(AnomalyResult::anomaly);
    }

    @Test
    void noFalsePositiveForStableMetrics() {
        // Feed 20 identical values
        for (int i = 0; i < 20; i++) {
            service.detect(new MetricSnapshot("svc", 5.0, 1.0, 200.0, 10.0, "CLOSED", Instant.now()));
        }
        // Same value again — no anomaly
        List<AnomalyResult> results = service.detect(
                new MetricSnapshot("svc", 5.0, 1.0, 200.0, 10.0, "CLOSED", Instant.now()));

        assertThat(results).noneMatch(AnomalyResult::anomaly);
    }
}
