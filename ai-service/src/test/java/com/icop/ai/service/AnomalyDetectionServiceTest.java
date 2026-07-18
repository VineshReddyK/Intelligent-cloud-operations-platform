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

/**
 * Exercises the z-score logic through the pure-Java fallback path — mocking
 * NDManager means the DJL call NPEs and the service drops to plain math,
 * which conveniently also proves the fallback works.
 */
@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    NDManager manager;

    private AnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        // window 60, threshold 2.5 — same defaults as production config
        service = new AnomalyDetectionService(manager, 60, 2.5);
    }

    @Test
    void noAnomalyWhenWindowTooSmall() {
        // a single sample can't be anomalous — there's nothing to compare against
        MetricSnapshot snap = new MetricSnapshot("user-service", 10.0, 0.5, 100.0, 0.0, "CLOSED", Instant.now());
        List<AnomalyResult> results = service.detect(snap);
        assertThat(results).noneMatch(AnomalyResult::anomaly);
    }

    @Test
    void detectsAnomalyAfterWindowFills() {
        // 15 quiet cycles to warm the window...
        for (int i = 0; i < 15; i++) {
            service.detect(new MetricSnapshot("svc", 10.0, 0.1, 50.0, 0.0, "CLOSED", Instant.now()));
        }
        // ...then an 80% error rate walks in
        List<AnomalyResult> results = service.detect(
                new MetricSnapshot("svc", 10.0, 80.0, 50.0, 0.0, "CLOSED", Instant.now()));

        assertThat(results)
                .filteredOn(r -> "error_rate_pct".equals(r.metric()))
                .anyMatch(AnomalyResult::anomaly);
    }

    @Test
    void noFalsePositiveForStableMetrics() {
        // perfectly flat metrics must never alarm — this is the epsilon-in-
        // the-variance case, where σ would otherwise be zero
        for (int i = 0; i < 20; i++) {
            service.detect(new MetricSnapshot("svc", 5.0, 1.0, 200.0, 10.0, "CLOSED", Instant.now()));
        }
        List<AnomalyResult> results = service.detect(
                new MetricSnapshot("svc", 5.0, 1.0, 200.0, 10.0, "CLOSED", Instant.now()));

        assertThat(results).noneMatch(AnomalyResult::anomaly);
    }
}
