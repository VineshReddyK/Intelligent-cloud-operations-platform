package com.icop.ai.dto;

import java.time.Instant;
import java.util.List;

// a risk verdict plus its receipts: the signals list says exactly which
// thresholds fired, and remediationAdvice says what to do about it
public record FailurePrediction(
        String service,
        double riskScore,
        RiskLevel riskLevel,
        List<String> signals,
        String remediationAdvice,
        Instant predictedAt
) {
    public FailurePrediction(String service, double riskScore, RiskLevel riskLevel,
                             List<String> signals, String remediationAdvice) {
        this(service, riskScore, riskLevel, signals, remediationAdvice, Instant.now());
    }
}
