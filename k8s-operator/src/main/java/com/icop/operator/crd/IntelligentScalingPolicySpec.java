package com.icop.operator.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The `spec:` block — what the user declares in their YAML. Defaults here are
 * the safety net: a policy that only sets targetService still gets sane
 * bounds, the in-cluster AI URL, and default scaling rules.
 *
 * ignoreUnknown so a newer CRD field in the YAML won't blow up an older
 * operator binary.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntelligentScalingPolicySpec {

    private String targetService;
    private int minReplicas = 1;
    private int maxReplicas = 10;
    private String aiServiceUrl = "http://ai-service:8085"; // k8s DNS default
    private ScalingRules scalingRules = new ScalingRules();

    public String getTargetService() { return targetService; }
    public void setTargetService(String v) { this.targetService = v; }

    public int getMinReplicas() { return minReplicas; }
    public void setMinReplicas(int v) { this.minReplicas = v; }

    public int getMaxReplicas() { return maxReplicas; }
    public void setMaxReplicas(int v) { this.maxReplicas = v; }

    public String getAiServiceUrl() { return aiServiceUrl; }
    public void setAiServiceUrl(String v) { this.aiServiceUrl = v; }

    public ScalingRules getScalingRules() { return scalingRules; }
    public void setScalingRules(ScalingRules v) { this.scalingRules = v; }
}
