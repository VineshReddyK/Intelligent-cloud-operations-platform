package com.icop.operator.service;

import com.icop.operator.crd.IntelligentScalingPolicySpec;
import com.icop.operator.crd.ScalingRules;
import com.icop.operator.dto.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScalingDecisionServiceTest {

    private ScalingDecisionService service;
    private IntelligentScalingPolicySpec spec;

    @BeforeEach
    void setUp() {
        service = new ScalingDecisionService();
        spec = new IntelligentScalingPolicySpec();
        spec.setMinReplicas(2);
        spec.setMaxReplicas(10);

        ScalingRules rules = new ScalingRules();
        rules.setLowRiskReplicas(2);
        rules.setMediumRiskReplicas(4);
        rules.setHighRiskReplicas(7);
        rules.setCriticalRiskReplicas(10);
        spec.setScalingRules(rules);
    }

    @Test
    void lowRiskReturnsMinReplicas() {
        assertThat(service.computeDesiredReplicas(spec, RiskLevel.LOW)).isEqualTo(2);
    }

    @Test
    void criticalRiskReturnsMaxReplicas() {
        assertThat(service.computeDesiredReplicas(spec, RiskLevel.CRITICAL)).isEqualTo(10);
    }

    @Test
    void highRiskClampsToMax() {
        spec.setMaxReplicas(5);
        // highRiskReplicas=7, max=5 → should clamp to 5
        assertThat(service.computeDesiredReplicas(spec, RiskLevel.HIGH)).isEqualTo(5);
    }

    @Test
    void unknownRiskFallsBackToLow() {
        assertThat(service.computeDesiredReplicas(spec, RiskLevel.UNKNOWN)).isEqualTo(2);
    }
}
