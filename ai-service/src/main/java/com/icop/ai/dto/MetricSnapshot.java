package com.icop.ai.dto;

import java.time.Instant;

public record MetricSnapshot(
        String service,
        double requestRatePerSecond,
        double errorRatePercent,
        double p99LatencyMs,
        double kafkaLag,
        String circuitBreakerState,
        Instant collectedAt
) {}
