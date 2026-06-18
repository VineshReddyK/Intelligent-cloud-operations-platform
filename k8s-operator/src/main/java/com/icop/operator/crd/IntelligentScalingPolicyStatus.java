package com.icop.operator.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntelligentScalingPolicyStatus {

    private int currentReplicas;
    private int desiredReplicas;
    private String riskLevel;
    private String lastScaledAt;
    private String reason;
    private long observedGeneration;

    public int getCurrentReplicas() { return currentReplicas; }
    public void setCurrentReplicas(int v) { this.currentReplicas = v; }

    public int getDesiredReplicas() { return desiredReplicas; }
    public void setDesiredReplicas(int v) { this.desiredReplicas = v; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String v) { this.riskLevel = v; }

    public String getLastScaledAt() { return lastScaledAt; }
    public void setLastScaledAt(String v) { this.lastScaledAt = v; }

    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }

    public long getObservedGeneration() { return observedGeneration; }
    public void setObservedGeneration(long v) { this.observedGeneration = v; }
}
