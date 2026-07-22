package com.icop.operator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// a slimmed-down copy of the ai-service prediction — the operator only reads
// these off the wire, so it keeps its own local shape rather than sharing a
// module. ignoreUnknown means ai-service can add fields without breaking us.
@JsonIgnoreProperties(ignoreUnknown = true)
public record FailurePrediction(
        String service,
        double riskScore,
        RiskLevel riskLevel,
        List<String> signals,
        String remediationAdvice
) {}
