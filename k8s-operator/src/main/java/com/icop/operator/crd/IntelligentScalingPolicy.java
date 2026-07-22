package com.icop.operator.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;

/**
 * The custom resource itself: `kind: IntelligentScalingPolicy`, api group
 * icop.io/v1alpha1. Extending Fabric8's CustomResource with typed spec and
 * status parameters is what lets us treat CRs as real Java objects instead
 * of raw JSON. Namespaced because scaling policies belong to a namespace,
 * same as the deployments they target.
 */
@Group("icop.io")
@Version("v1alpha1")
@Kind("IntelligentScalingPolicy")
@Plural("intelligentscalingpolicies")
public class IntelligentScalingPolicy
        extends CustomResource<IntelligentScalingPolicySpec, IntelligentScalingPolicyStatus>
        implements Namespaced {
}
