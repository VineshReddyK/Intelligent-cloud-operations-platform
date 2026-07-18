package com.icop.ai.scheduler;

import com.icop.ai.service.InsightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Heartbeat of the AI service — one analysis cycle every 30s by default.
 * fixedDelay (not fixedRate) on purpose: the next cycle starts counting
 * after the previous one finishes, so slow prometheus queries can never
 * stack cycles on top of each other.
 */
@Component
public class InsightScheduler {

    private static final Logger log = LoggerFactory.getLogger(InsightScheduler.class);

    private final InsightService insightService;

    public InsightScheduler(InsightService insightService) {
        this.insightService = insightService;
    }

    @Scheduled(fixedDelayString = "${ai.anomaly.poll-interval-ms:30000}")
    public void runInsightCycle() {
        // catch everything — an unhandled exception here would silently kill
        // the scheduled task for good, and nobody would notice until much later
        try {
            insightService.runAnalysis();
        } catch (Exception e) {
            log.error("Insight analysis cycle failed: {}", e.getMessage(), e);
        }
    }
}
