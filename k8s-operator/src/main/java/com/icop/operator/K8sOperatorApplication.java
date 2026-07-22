package com.icop.operator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// the custom operator — watches IntelligentScalingPolicy CRs and scales the
// deployments they point at based on the AI service's risk assessment.
// @EnableScheduling drives the reconcile loop in ScalingReconciler
@SpringBootApplication
@EnableScheduling
public class K8sOperatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(K8sOperatorApplication.class, args);
    }
}
