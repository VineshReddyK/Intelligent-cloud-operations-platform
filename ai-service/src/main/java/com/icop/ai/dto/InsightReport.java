package com.icop.ai.dto;

import java.time.Instant;
import java.util.List;

// one analysis cycle, boxed up: only the *confirmed* anomalies make it in
// here, but every service gets a prediction row regardless of health
public record InsightReport(
        Instant generatedAt,
        int servicesAnalyzed,
        List<AnomalyResult> anomalies,
        List<FailurePrediction> predictions,
        String overallHealth
) {}
