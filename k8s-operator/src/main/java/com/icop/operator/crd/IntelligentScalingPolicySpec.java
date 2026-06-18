package com.icop.operator.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntelligentScalingPolicySpec {

    private String targetService;
    private int minReplicas = 1;
    private int maxReplicas = 10;
    private String aiServiceUrl = "http://ai-service:8085";
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
