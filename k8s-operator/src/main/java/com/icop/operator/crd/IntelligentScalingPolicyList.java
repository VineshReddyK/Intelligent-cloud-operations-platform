package com.icop.operator.crd;

import io.fabric8.kubernetes.client.CustomResourceList;

// Fabric8 requires an explicit list type for every CR — this is what a
// `kubectl get intelligentscalingpolicies` deserializes into. Nothing to
// add beyond the generic binding.
public class IntelligentScalingPolicyList extends CustomResourceList<IntelligentScalingPolicy> {
}
