package com.icop.ai.scheduler;

import com.icop.ai.service.InsightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InsightScheduler {

    private static final Logger log = LoggerFactory.getLogger(InsightScheduler.class);

    private final InsightService insightService;

    public InsightScheduler(InsightService insightService) {
        this.insightService = insightService;
    }

    @Scheduled(fixedDelayString = "${ai.anomaly.poll-interval-ms:30000}")
    public void runInsightCycle() {
        try {
            insightService.runAnalysis();
        } catch (Exception e) {
            log.error("Insight analysis cycle failed: {}", e.getMessage(), e);
        }
    }
}
