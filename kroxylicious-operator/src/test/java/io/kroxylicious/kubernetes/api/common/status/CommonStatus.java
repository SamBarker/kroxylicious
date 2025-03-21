/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kubernetes.api.common.status;

import io.kroxylicious.kubernetes.api.v1alpha1.KafkaServiceStatusBuilder;

public class CommonStatus implements io.fabric8.kubernetes.api.builder.Editable<KafkaServiceStatusBuilder>, io.fabric8.kubernetes.api.model.KubernetesResource {
    @com.fasterxml.jackson.annotation.JsonProperty("conditions")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private java.util.List<io.kroxylicious.kubernetes.api.common.Condition> conditions;
    /**
     * The metadata.generation that was observed during the last reconciliation by the operator.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("observedGeneration")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("The metadata.generation that was observed during the last reconciliation by the operator.\n")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Long observedGeneration;

    @Override
    public KafkaServiceStatusBuilder edit() {
        return new KafkaServiceStatusBuilder(this);
    }

    public java.util.List<io.kroxylicious.kubernetes.api.common.Condition> getConditions() {
        return conditions;
    }

    public void setConditions(java.util.List<io.kroxylicious.kubernetes.api.common.Condition> conditions) {
        this.conditions = conditions;
    }

    public Long getObservedGeneration() {
        return observedGeneration;
    }

    public void setObservedGeneration(Long observedGeneration) {
        this.observedGeneration = observedGeneration;
    }

    protected boolean canEqual(Object other) {
        return other instanceof KafkaServiceStatus;
    }
}
