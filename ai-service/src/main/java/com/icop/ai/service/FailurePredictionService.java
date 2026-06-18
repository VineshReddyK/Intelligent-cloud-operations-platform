package com.icop.ai.service;

import com.icop.ai.dto.FailurePrediction;
import com.icop.ai.dto.MetricSnapshot;
import com.icop.ai.dto.RiskLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-signal weighted scoring model for service failure prediction.
 *
 * Score breakdown (total 100 pts):
 *   - Circuit breaker state : 40 pts  (OPEN=40, HALF_OPEN=20)
 *   - HTTP error rate       : 30 pts  (>10%=30, >5%=15)
 *   - P99 latency           : 20 pts  (>2000ms=20, >1000ms=10)
 *   - Kafka consumer lag    : 10 pts  (>1000 msgs=10)
 */
@Service
public class FailurePredictionService {

    private final double highRiskThreshold;
    private final double criticalThreshold;

    public FailurePredictionService(
            @Value("${ai.prediction.high-risk-threshold:70}") double highRiskThreshold,
            @Value("${ai.prediction.critical-threshold:90}") double criticalThreshold) {
        this.highRiskThreshold = highRiskThreshold;
        this.criticalThreshold = criticalThreshold;
    }

    public FailurePrediction predict(MetricSnapshot snapshot) {
        double score = 0.0;
        List<String> signals = new ArrayList<>();

        // Circuit breaker (40 pts)
        switch (snapshot.circuitBreakerState()) {
            case "OPEN" -> { score += 40; signals.add("Circuit breaker OPEN — calls are short-circuiting"); }
            case "HALF_OPEN" -> { score += 20; signals.add("Circuit breaker HALF_OPEN — recovery attempt in progress"); }
            default -> {}
        }

        // Error rate (30 pts)
        double errRate = snapshot.errorRatePercent();
        if (errRate > 10) {
            score += 30;
            signals.add(String.format("Critical error rate: %.1f%%", errRate));
        } else if (errRate > 5) {
            score += 15;
            signals.add(String.format("Elevated error rate: %.1f%%", errRate));
        }

        // P99 latency (20 pts)
        double latency = snapshot.p99LatencyMs();
        if (latency > 2000) {
            score += 20;
            signals.add(String.format("P99 latency critical: %.0f ms", latency));
        } else if (latency > 1000) {
            score += 10;
            signals.add(String.format("P99 latency elevated: %.0f ms", latency));
        }

        // Kafka consumer lag (10 pts)
        double lag = snapshot.kafkaLag();
        if (lag > 1000) {
            score += 10;
            signals.add(String.format("High Kafka consumer lag: %.0f messages", lag));
        }

        RiskLevel risk = classify(score);
        return new FailurePrediction(snapshot.service(), score, risk, signals, remediate(risk, signals));
    }

    private RiskLevel classify(double score) {
        if (score >= criticalThreshold) return RiskLevel.CRITICAL;
        if (score >= highRiskThreshold) return RiskLevel.HIGH;
        if (score >= 40) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private String remediate(RiskLevel risk, List<String> signals) {
        return switch (risk) {
            case CRITICAL -> "IMMEDIATE: scale up replicas via HPA override, drain traffic, check error logs and DB connections";
            case HIGH -> "URGENT: investigate error logs; if CB is OPEN wait for auto-reset or force reset via /actuator/circuitbreakers";
            case MEDIUM -> "MONITOR: watch downstream dependencies, consider pre-emptive cache warm-up or replica increase";
            case LOW -> "HEALTHY: service operating within normal parameters";
        };
    }
}
