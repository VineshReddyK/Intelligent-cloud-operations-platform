package com.icop.ai.controller;

import com.icop.ai.dto.AnomalyResult;
import com.icop.ai.dto.FailurePrediction;
import com.icop.ai.dto.InsightReport;
import com.icop.ai.entity.AnomalyEvent;
import com.icop.ai.repository.AnomalyEventRepository;
import com.icop.ai.service.AnomalyDetectionService;
import com.icop.ai.service.InsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
@Tag(name = "AI Insights", description = "Anomaly detection, failure prediction, and auto-remediation")
public class InsightController {

    private final InsightService insightService;
    private final AnomalyEventRepository anomalyRepo;
    private final AnomalyDetectionService anomalyDetection;

    public InsightController(InsightService insightService,
                             AnomalyEventRepository anomalyRepo,
                             AnomalyDetectionService anomalyDetection) {
        this.insightService = insightService;
        this.anomalyRepo = anomalyRepo;
        this.anomalyDetection = anomalyDetection;
    }

    @GetMapping("/report")
    @Operation(summary = "Get latest AI insight report",
               description = "Returns the most recent analysis cycle result with anomalies, failure predictions, and overall health")
    public ResponseEntity<InsightReport> getReport() {
        return ResponseEntity.ok(insightService.getLatestReport());
    }

    @PostMapping("/report/refresh")
    @Operation(summary = "Trigger a fresh analysis cycle immediately")
    public ResponseEntity<InsightReport> refresh() {
        return ResponseEntity.ok(insightService.runAnalysis());
    }

    @GetMapping("/anomalies")
    @Operation(summary = "Get anomaly events from the last N hours")
    public ResponseEntity<List<AnomalyEvent>> getRecentAnomalies(
            @RequestParam(defaultValue = "1") int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return ResponseEntity.ok(anomalyRepo.findByDetectedAtAfterOrderByDetectedAtDesc(since));
    }

    @GetMapping("/anomalies/{service}")
    @Operation(summary = "Get anomaly history for a specific service")
    public ResponseEntity<List<AnomalyEvent>> getAnomaliesForService(@PathVariable String service) {
        return ResponseEntity.ok(anomalyRepo.findByServiceOrderByDetectedAtDesc(service));
    }

    @GetMapping("/predictions")
    @Operation(summary = "Get current failure predictions for all services")
    public ResponseEntity<List<FailurePrediction>> getPredictions() {
        InsightReport report = insightService.getLatestReport();
        return ResponseEntity.ok(report.predictions());
    }

    @GetMapping("/health")
    @Operation(summary = "Get overall platform health status")
    public ResponseEntity<Map<String, Object>> getPlatformHealth() {
        InsightReport report = insightService.getLatestReport();
        return ResponseEntity.ok(Map.of(
                "overallHealth", report.overallHealth(),
                "servicesAnalyzed", report.servicesAnalyzed(),
                "activeAnomalies", report.anomalies().size(),
                "generatedAt", report.generatedAt(),
                "windowStats", anomalyDetection.windowStats()
        ));
    }
}
