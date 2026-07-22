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
 * The reconcile loop — the heart of the operator.
 *
 * This is the standard Kubernetes controller pattern: on a timer, look at
 * every IntelligentScalingPolicy in the cluster and nudge reality toward
 * what each one asks for. What makes it "intelligent" is where the desired
 * state comes from — not a static CPU threshold like a plain HPA, but the
 * AI service's risk assessment for that service.
 *
 * Per policy, each pass:
 *   1. ask the AI service for the target's current risk level
 *   2. turn that risk into a desired replica count via the policy's rules
 *   3. read the Deployment's actual replica count
 *   4. scale only if they differ
 *   5. write what happened back to the CR's status subresource
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
        // expose the loop count as a gauge so prometheus can confirm the
        // operator is actually alive and reconciling, not just running
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
            // 1. current risk from the AI service (falls back to UNKNOWN if it's down)
            RiskLevel risk = aiClient.getRiskLevel(aiUrl, targetService);

            // 2. risk → replica count, clamped to the policy's min/max
            int desired = decisionService.computeDesiredReplicas(policy.getSpec(), risk);

            // 3. what the deployment actually has right now
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

            // 4. only touch the cluster when there's an actual delta — reconcile
            // runs constantly, so a no-op scale every 30s would be pure noise
            if (current != desired) {
                log.info("Scaling {}/{}: {} → {} replicas (risk={})", namespace, targetService, current, desired, risk);

                k8sClient.apps().deployments()
                        .inNamespace(namespace)
                        .withName(targetService)
                        .scale(desired);
            } else {
                log.debug("No scaling needed for {}/{}: {} replicas (risk={})", namespace, targetService, current, risk);
            }

            // 5. record the outcome on status — this is what `kubectl get isp`
            // shows and how you audit why a service got scaled
            IntelligentScalingPolicyStatus status = new IntelligentScalingPolicyStatus();
            status.setCurrentReplicas(desired);
            status.setDesiredReplicas(desired);
            status.setRiskLevel(risk.name());
            status.setLastScaledAt(Instant.now().toString());
            status.setReason(current != desired
                    ? "Scaled from " + current + " to " + desired + " due to " + risk + " risk"
                    : "No change — " + desired + " replicas at " + risk + " risk");
            // observedGeneration lets users tell "the operator has seen my
            // latest edit" from "it's still acting on the old spec"
            status.setObservedGeneration(policy.getMetadata().getGeneration() != null
                    ? policy.getMetadata().getGeneration() : 0L);

            policy.setStatus(status);
            // updateStatus hits the /status subresource specifically — spec and
            // status have separate RBAC and shouldn't be written together
            k8sClient.resources(IntelligentScalingPolicy.class, IntelligentScalingPolicyList.class)
                    .inNamespace(namespace)
                    .withName(name)
                    .updateStatus(policy);

        } catch (Exception e) {
            // one bad policy must not stop the others — log it and carry on,
            // the next pass will retry anyway
            log.error("Reconciliation failed for policy {}/{}: {}", namespace, name, e.getMessage(), e);
        }
    }
}
