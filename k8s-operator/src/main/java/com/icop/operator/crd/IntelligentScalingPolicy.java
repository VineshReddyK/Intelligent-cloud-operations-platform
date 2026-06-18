package com.icop.operator.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("icop.io")
@Version("v1alpha1")
@Kind("IntelligentScalingPolicy")
@Plural("intelligentscalingpolicies")
public class IntelligentScalingPolicy
        extends CustomResource<IntelligentScalingPolicySpec, IntelligentScalingPolicyStatus>
        implements Namespaced {
}
