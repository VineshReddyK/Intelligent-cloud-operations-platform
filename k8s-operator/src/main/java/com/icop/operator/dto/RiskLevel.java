package com.icop.operator.dto;

// mirrors ai-service's RiskLevel, plus an UNKNOWN the AI side doesn't have —
// the operator needs a "couldn't reach the AI service" value, and it maps to
// the safe low-risk baseline downstream
public enum RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL, UNKNOWN
}
