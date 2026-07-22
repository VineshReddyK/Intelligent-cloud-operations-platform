package com.icop.operator.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The risk-to-replicas mapping, nested under spec. Defaults sketch a sensible
 * curve — steady at 2 when calm, jumping to 10 when things are on fire — but
 * every service can override these in its own policy YAML.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScalingRules {

    private int lowRiskReplicas = 2;
    private int mediumRiskReplicas = 3;
    private int highRiskReplicas = 6;
    private int criticalRiskReplicas = 10;

    public int getLowRiskReplicas() { return lowRiskReplicas; }
    public void setLowRiskReplicas(int v) { this.lowRiskReplicas = v; }

    public int getMediumRiskReplicas() { return mediumRiskReplicas; }
    public void setMediumRiskReplicas(int v) { this.mediumRiskReplicas = v; }

    public int getHighRiskReplicas() { return highRiskReplicas; }
    public void setHighRiskReplicas(int v) { this.highRiskReplicas = v; }

    public int getCriticalRiskReplicas() { return criticalRiskReplicas; }
    public void setCriticalRiskReplicas(int v) { this.criticalRiskReplicas = v; }
}
