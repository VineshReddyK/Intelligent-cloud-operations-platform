package com.icop.operator.reconciler;

import com.icop.operator.crd.IntelligentScalingPolicy;
import com.icop.operator.crd.IntelligentScalingPolicyList;
import com.icop.operator.crd.IntelligentScalingPolicyStatus;
import com.icop.operator.dto.RiskLevel;
import com.icop.operator.service.AiInsightClient;
import com.icop.operator.service.ScalingDecisionService;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core reconciliation loop for the ICOP Kubernetes Operator.
 *
 * Every reconcile interval:
 *  1. Lists all IntelligentScalingPolicy CRs in the cluster
 *  2. For each policy, queries the AI service for the target service's risk level
 *  3. Computes the desired replica count from the policy's ScalingRules
 *  4. Patches the target Deployment's replica count if it differs
 *  5. Updates the CR's status subresource (risk level, replicas, timestamp)
 */
@Component
public class ScalingReconciler {

    private static final Logger log = LoggerFactory.getLogger(ScalingReconciler.class);

    private final KubernetesClient k8sClient;
    private final AiInsightClient aiClient;
    private final ScalingDecisionService decisionService;
    private final AtomicInteger reconcileCount = new AtomicInteger();

    public ScalingReconciler(KubernetesClient k8sClient,
                             AiInsightClient aiClient,
                             ScalingDecisionService decisionService,
                             MeterRegistry meterRegistry) {
        this.k8sClient = k8sClient;
        this.aiClient = aiClient;
        this.decisionService = decisionService;
        meterRegistry.gauge("icop.operator.reconcile.total", reconcileCount);
    }

    @Scheduled(fixedDelayString = "${operator.reconcile-interval-ms:30000}")
    public void reconcile() {
        reconcileCount.incrementAndGet();

        List<IntelligentScalingPolicy> policies = k8sClient
                .resources(IntelligentScalingPolicy.class, IntelligentScalingPolicyList.class)
                .inAnyNamespace()
                .list()
                .getItems();

        if (policies.isEmpty()) {
            log.debug("No IntelligentScalingPolicy resources found — nothing to reconcile");
            return;
        }

        log.info("Reconciling {} IntelligentScalingPolicy resources", policies.size());
        policies.forEach(this::reconcilePolicy);
    }

    private void reconcilePolicy(IntelligentScalingPolicy policy) {
        String name = policy.getMetadata().getName();
        String namespace = policy.getMetadata().getNamespace();
        String targetService = policy.getSpec().getTargetService();
        String aiUrl = policy.getSpec().getAiServiceUrl();

        try {
            // Step 1: ask AI service for current risk level
            RiskLevel risk = aiClient.getRiskLevel(aiUrl, targetService);

            // Step 2: compute desired replicas
            int desired = decisionService.computeDesiredReplicas(policy.getSpec(), risk);

            // Step 3: read current replica count from Deployment
            var deployment = k8sClient.apps().deployments()
                    .inNamespace(namespace)
                    .withName(targetService)
                    .get();

            if (deployment == null) {
                log.warn("Deployment {} not found in namespace {} — skipping", targetService, namespace);
                return;
            }

            int current = deployment.getSpec().getReplicas() != null
                    ? deployment.getSpec().getReplicas() : 1;

            // Step 4: scale only when replica count differs
            if (current != desired) {
                log.info("Scaling {}/{}: {} → {} replicas (risk={})", namespace, targetService, current, desired, risk);

                k8sClient.apps().deployments()
                        .inNamespace(namespace)
                        .withName(targetService)
                        .scale(desired);
            } else {
                log.debug("No scaling needed for {}/{}: {} replicas (risk={})", namespace, targetService, current, risk);
            }

            // Step 5: update CRD status subresource
            IntelligentScalingPolicyStatus status = new IntelligentScalingPolicyStatus();
            status.setCurrentReplicas(desired);
            status.setDesiredReplicas(desired);
            status.setRiskLevel(risk.name());
            status.setLastScaledAt(Instant.now().toString());
            status.setReason(current != desired
                    ? "Scaled from " + current + " to " + desired + " due to " + risk + " risk"
                    : "No change — " + desired + " replicas at " + risk + " risk");
            status.setObservedGeneration(policy.getMetadata().getGeneration() != null
                    ? policy.getMetadata().getGeneration() : 0L);

            policy.setStatus(status);
            k8sClient.resources(IntelligentScalingPolicy.class, IntelligentScalingPolicyList.class)
                    .inNamespace(namespace)
                    .withName(name)
                    .updateStatus(policy);

        } catch (Exception e) {
            log.error("Reconciliation failed for policy {}/{}: {}", namespace, name, e.getMessage(), e);
        }
    }
}
