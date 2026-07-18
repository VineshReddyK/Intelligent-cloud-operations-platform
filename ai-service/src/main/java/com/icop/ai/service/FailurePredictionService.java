package com.icop.ai.service;

import com.icop.ai.dto.FailurePrediction;
import com.icop.ai.dto.MetricSnapshot;
import com.icop.ai.dto.RiskLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Weighted multi-signal risk score, 0–100. The weights encode a simple
 * belief: a breaker that's already OPEN is the loudest possible alarm (40),
 * errors matter more than latency (30 vs 20), and kafka lag is an early
 * whisper rather than a siren (10).
 *
 *   circuit breaker  40  (OPEN=40, HALF_OPEN=20)
 *   error rate       30  (>10%=30, >5%=15)
 *   p99 latency      20  (>2s=20, >1s=10)
 *   kafka lag        10  (>1000 msgs=10)
 *
 * Every point comes with a human-readable signal, so a score is never
 * just a number you have to take on faith.
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

        switch (snapshot.circuitBreakerState()) {
            case "OPEN" -> { score += 40; signals.add("Circuit breaker OPEN — calls are short-circuiting"); }
            case "HALF_OPEN" -> { score += 20; signals.add("Circuit breaker HALF_OPEN — recovery attempt in progress"); }
            default -> {}
        }

        double errRate = snapshot.errorRatePercent();
        if (errRate > 10) {
            score += 30;
            signals.add(String.format("Critical error rate: %.1f%%", errRate));
        } else if (errRate > 5) {
            score += 15;
            signals.add(String.format("Elevated error rate: %.1f%%", errRate));
        }

        double latency = snapshot.p99LatencyMs();
        if (latency > 2000) {
            score += 20;
            signals.add(String.format("P99 latency critical: %.0f ms", latency));
        } else if (latency > 1000) {
            score += 10;
            signals.add(String.format("P99 latency elevated: %.0f ms", latency));
        }

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

    // canned advice per level — the k8s operator reads CRITICAL as its cue
    // to scale, humans read the rest
    private String remediate(RiskLevel risk, List<String> signals) {
        return switch (risk) {
            case CRITICAL -> "IMMEDIATE: scale up replicas via HPA override, drain traffic, check error logs and DB connections";
            case HIGH -> "URGENT: investigate error logs; if CB is OPEN wait for auto-reset or force reset via /actuator/circuitbreakers";
            case MEDIUM -> "MONITOR: watch downstream dependencies, consider pre-emptive cache warm-up or replica increase";
            case LOW -> "HEALTHY: service operating within normal parameters";
        };
    }
}
