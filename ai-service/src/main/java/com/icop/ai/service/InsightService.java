package com.icop.ai.service;

import com.icop.ai.dto.AnomalyResult;
import com.icop.ai.dto.FailurePrediction;
import com.icop.ai.dto.InsightReport;
import com.icop.ai.dto.MetricSnapshot;
import com.icop.ai.dto.RiskLevel;
import com.icop.ai.entity.AnomalyEvent;
import com.icop.ai.repository.AnomalyEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The conductor: collect metrics → run anomaly detection → run failure
 * prediction → roll everything up into one InsightReport. The scheduler
 * calls this every cycle; the REST API serves whatever the last cycle
 * produced.
 */
@Service
public class InsightService {

    private static final Logger log = LoggerFactory.getLogger(InsightService.class);

    private final MetricsCollectorService metricsCollector;
    private final AnomalyDetectionService anomalyDetection;
    private final FailurePredictionService failurePrediction;
    private final AnomalyEventRepository anomalyRepo;

    // AtomicReference instead of a lock — readers always get a complete
    // report, never a half-written one
    private final AtomicReference<InsightReport> latestReport = new AtomicReference<>();

    public InsightService(MetricsCollectorService metricsCollector,
                          AnomalyDetectionService anomalyDetection,
                          FailurePredictionService failurePrediction,
                          AnomalyEventRepository anomalyRepo) {
        this.metricsCollector = metricsCollector;
        this.anomalyDetection = anomalyDetection;
        this.failurePrediction = failurePrediction;
        this.anomalyRepo = anomalyRepo;
    }

    public InsightReport runAnalysis() {
        log.debug("Running AI insight analysis cycle");

        List<MetricSnapshot> snapshots = metricsCollector.collectAll();
        List<AnomalyResult> allAnomalies = new ArrayList<>();
        List<FailurePrediction> allPredictions = new ArrayList<>();

        for (MetricSnapshot snapshot : snapshots) {
            List<AnomalyResult> anomalies = anomalyDetection.detect(snapshot);
            FailurePrediction prediction = failurePrediction.predict(snapshot);

            allAnomalies.addAll(anomalies);
            allPredictions.add(prediction);

            // only confirmed anomalies hit the DB — the normal readings would
            // just be noise with an insert cost
            anomalies.stream()
                    .filter(AnomalyResult::anomaly)
                    .forEach(a -> anomalyRepo.save(
                            new AnomalyEvent(a.service(), a.metric(), a.currentValue(), a.zScore())));

            if (prediction.riskLevel() == RiskLevel.CRITICAL || prediction.riskLevel() == RiskLevel.HIGH) {
                log.warn("HIGH RISK: service={} score={:.1f} level={} advice={}",
                        prediction.service(), prediction.riskScore(),
                        prediction.riskLevel(), prediction.remediationAdvice());
            }
        }

        String overallHealth = computeOverallHealth(allPredictions);

        InsightReport report = new InsightReport(
                Instant.now(),
                snapshots.size(),
                allAnomalies.stream().filter(AnomalyResult::anomaly).toList(),
                allPredictions,
                overallHealth
        );

        latestReport.set(report);
        log.info("Analysis complete: services={} anomalies={} health={}",
                snapshots.size(),
                report.anomalies().size(),
                overallHealth);

        return report;
    }

    public InsightReport getLatestReport() {
        // lazily run a cycle if asked before the scheduler's first pass
        InsightReport report = latestReport.get();
        return report != null ? report : runAnalysis();
    }

    // worst service wins — platform health is only as good as its sickest member
    private String computeOverallHealth(List<FailurePrediction> predictions) {
        if (predictions.isEmpty()) return "UNKNOWN";
        boolean hasCritical = predictions.stream().anyMatch(p -> p.riskLevel() == RiskLevel.CRITICAL);
        boolean hasHigh = predictions.stream().anyMatch(p -> p.riskLevel() == RiskLevel.HIGH);
        boolean hasMedium = predictions.stream().anyMatch(p -> p.riskLevel() == RiskLevel.MEDIUM);
        if (hasCritical) return "CRITICAL";
        if (hasHigh) return "DEGRADED";
        if (hasMedium) return "WARNING";
        return "HEALTHY";
    }
}
