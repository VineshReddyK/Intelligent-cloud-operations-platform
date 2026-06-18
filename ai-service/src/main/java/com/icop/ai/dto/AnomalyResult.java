package com.icop.ai.dto;

import java.time.Instant;

public record AnomalyResult(
        String service,
        String metric,
        double currentValue,
        double zScore,
        boolean anomaly,
        Instant detectedAt
) {
    public AnomalyResult(String service, String metric, double currentValue, double zScore, boolean anomaly) {
        this(service, metric, currentValue, zScore, anomaly, Instant.now());
    }
}
