package com.icop.operator.service;

import com.icop.operator.crd.IntelligentScalingPolicySpec;
import com.icop.operator.crd.ScalingRules;
import com.icop.operator.dto.RiskLevel;
import org.springframework.stereotype.Service;

@Service
public class ScalingDecisionService {

    public int computeDesiredReplicas(IntelligentScalingPolicySpec spec, RiskLevel risk) {
        ScalingRules rules = spec.getScalingRules();

        int desired = switch (risk) {
            case LOW     -> rules.getLowRiskReplicas();
            case MEDIUM  -> rules.getMediumRiskReplicas();
            case HIGH    -> rules.getHighRiskReplicas();
            case CRITICAL -> rules.getCriticalRiskReplicas();
            case UNKNOWN -> rules.getLowRiskReplicas();
        };

        // Clamp to configured min/max bounds
        return Math.max(spec.getMinReplicas(), Math.min(spec.getMaxReplicas(), desired));
    }
}
