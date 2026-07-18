package com.icop.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// the brain of the platform: watches everyone's metrics, spots anomalies,
// predicts failures, and feeds the k8s operator its scaling signals.
// @EnableScheduling powers the periodic analysis loop in InsightScheduler
@SpringBootApplication
@EnableScheduling
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
