package com.icop.ai.dto;

import java.time.Instant;

// one service's vitals at one moment — the four numbers plus breaker state
// that every downstream model works from
public record MetricSnapshot(
        String service,
        double requestRatePerSecond,
        double errorRatePercent,
        double p99LatencyMs,
        double kafkaLag,
        String circuitBreakerState,
        Instant collectedAt
) {}
