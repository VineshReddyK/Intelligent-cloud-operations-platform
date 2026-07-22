package com.icop.operator.service;

import com.icop.operator.crd.IntelligentScalingPolicySpec;
import com.icop.operator.crd.ScalingRules;
import com.icop.operator.dto.RiskLevel;
import org.springframework.stereotype.Service;

/**
 * Turns a risk level into a replica count. Pulled out of the reconciler on
 * purpose — it's the one piece of real logic here, and keeping it pure (no
 * k8s client, no I/O) makes it trivial to unit test.
 */
@Service
public class ScalingDecisionService {

    public int computeDesiredReplicas(IntelligentScalingPolicySpec spec, RiskLevel risk) {
        ScalingRules rules = spec.getScalingRules();

        // UNKNOWN maps to the low-risk count deliberately: if the AI service is
        // unreachable we hold at a safe baseline rather than scaling on a guess
        int desired = switch (risk) {
            case LOW     -> rules.getLowRiskReplicas();
            case MEDIUM  -> rules.getMediumRiskReplicas();
            case HIGH    -> rules.getHighRiskReplicas();
            case CRITICAL -> rules.getCriticalRiskReplicas();
            case UNKNOWN -> rules.getLowRiskReplicas();
        };

        // min/max are the hard guardrails — the rules can suggest anything, but
        // a misconfigured policy still can't scale to zero or to infinity
        return Math.max(spec.getMinReplicas(), Math.min(spec.getMaxReplicas(), desired));
    }
}
