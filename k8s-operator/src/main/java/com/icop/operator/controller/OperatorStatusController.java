package com.icop.operator.controller;

import com.icop.operator.crd.IntelligentScalingPolicy;
import com.icop.operator.crd.IntelligentScalingPolicyList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * A small read-only window into the operator — handy for demos and dashboards
 * so you don't have to shell into the cluster with kubectl just to see what
 * the operator is managing.
 */
@RestController
@RequestMapping("/api/operator")
@Tag(name = "Operator Status", description = "Kubernetes Operator management and CRD status")
public class OperatorStatusController {

    private final KubernetesClient k8sClient;

    public OperatorStatusController(KubernetesClient k8sClient) {
        this.k8sClient = k8sClient;
    }

    @GetMapping("/policies")
    @Operation(summary = "List all IntelligentScalingPolicy CRs across all namespaces")
    public ResponseEntity<List<IntelligentScalingPolicy>> listPolicies() {
        List<IntelligentScalingPolicy> policies = k8sClient
                .resources(IntelligentScalingPolicy.class, IntelligentScalingPolicyList.class)
                .inAnyNamespace()
                .list()
                .getItems();
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/status")
    @Operation(summary = "Operator health: server version + active policy count")
    public ResponseEntity<Map<String, Object>> operatorStatus() {
        int policyCount = k8sClient
                .resources(IntelligentScalingPolicy.class, IntelligentScalingPolicyList.class)
                .inAnyNamespace()
                .list()
                .getItems()
                .size();

        // reaching the API server for the version doubles as a liveness check —
        // if this call works, our cluster connection is healthy
        String k8sVersion = k8sClient.getKubernetesVersion().getGitVersion();

        return ResponseEntity.ok(Map.of(
                "status", "RUNNING",
                "kubernetesVersion", k8sVersion,
                "activePolicies", policyCount
        ));
    }
}
