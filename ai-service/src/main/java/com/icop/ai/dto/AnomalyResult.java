package com.icop.ai.dto;

import java.time.Instant;

// one metric's verdict for one cycle. the compact constructor stamps the
// time so callers don't have to remember to
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
