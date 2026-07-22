package com.icop.operator.service;

import com.icop.operator.crd.IntelligentScalingPolicySpec;
import com.icop.operator.crd.ScalingRules;
import com.icop.operator.dto.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The decision logic is pure, so these tests need no Spring, no k8s, no
 * mocks — just plug in a risk level and check the replica count. The clamp
 * test is the one that matters most: it's the guardrail against a
 * misconfigured policy scaling somewhere dangerous.
 */
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
        // rule says 7, but the policy caps at 5 — the cap has to win
        spec.setMaxReplicas(5);
        assertThat(service.computeDesiredReplicas(spec, RiskLevel.HIGH)).isEqualTo(5);
    }

    @Test
    void unknownRiskFallsBackToLow() {
        // AI unreachable → hold at the safe baseline, don't guess
        assertThat(service.computeDesiredReplicas(spec, RiskLevel.UNKNOWN)).isEqualTo(2);
    }
}
