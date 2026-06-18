package com.icop.operator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FailurePrediction(
        String service,
        double riskScore,
        RiskLevel riskLevel,
        List<String> signals,
        String remediationAdvice
) {}
