package com.icop.ai.dto;

import java.time.Instant;
import java.util.List;

public record InsightReport(
        Instant generatedAt,
        int servicesAnalyzed,
        List<AnomalyResult> anomalies,
        List<FailurePrediction> predictions,
        String overallHealth
) {}
