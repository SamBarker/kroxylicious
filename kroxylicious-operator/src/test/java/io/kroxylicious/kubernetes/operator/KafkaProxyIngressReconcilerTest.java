/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kubernetes.operator;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.javaoperatorsdk.operator.api.reconciler.Context;

import io.kroxylicious.kubernetes.api.common.Condition;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxy;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyBuilder;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyIngress;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyIngressBuilder;
import io.kroxylicious.kubernetes.operator.assertj.KafkaProxyIngressStatusAssert;
import io.kroxylicious.kubernetes.operator.assertj.OperatorAssertions;

import static io.kroxylicious.kubernetes.operator.assertj.KafkaProxyIngressStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaProxyIngressReconcilerTest {

    private static final Clock TEST_CLOCK = Clock.fixed(Instant.EPOCH, ZoneId.of("Z"));

    // @formatter:off
    private static final KafkaProxyIngress INGRESS = new KafkaProxyIngressBuilder()
            .withNewMetadata()
                .withName("foo")
                .withUid(UUID.randomUUID().toString())
                .withGeneration(42L)
            .endMetadata()
            .withNewSpec()
                .withNewProxyRef()
                    .withName("my-proxy")
                .endProxyRef()
            .endSpec()
            .build();

    private static final String PROXY_UUID = "proxy-uuid";
    private static final long PROXY_GENERATION = 101L;

    private static final KafkaProxy PROXY = new KafkaProxyBuilder()
            .withNewMetadata()
                .withName("my-proxy")
                .withUid(PROXY_UUID)
                .withGeneration(PROXY_GENERATION)
            .endMetadata()
            .withNewSpec()
            .endSpec()
            .build();
    // @formatter:on

    private Context<KafkaProxyIngress> context;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        context = mock(Context.class);
    }

    @Test
    void shouldSetResolvedRefsToFalseWhenProxyNotFound() throws Exception {
        // given
        var reconciler = new KafkaProxyIngressReconciler(TEST_CLOCK);

        when(context.getSecondaryResource(KafkaProxy.class, KafkaProxyIngressReconciler.PROXY_EVENT_SOURCE_NAME)).thenReturn(Optional.empty());

        // when
        var update = reconciler.reconcile(INGRESS, context);

        // then
        assertThat(update).isNotNull();
        assertThat(update.isPatchStatus()).isTrue();
        assertThat(update.getResource()).isPresent();
        assertThat(update.getResource().get().getStatus())
                .hasObservedGenerationInSyncWithMetadataOf(INGRESS)
                .singleCondition()
                .hasObservedGenerationInSyncWithMetadataOf(INGRESS)
                .isResolvedRefsFalse(Condition.REASON_REFS_NOT_FOUND, "KafkaProxy spec.proxyRef.name not found")
                .hasLastTransitionTime(TEST_CLOCK.instant());

    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldSetResolvedRefsToTrueWhenProxyFound() throws Exception {
        // given
        Clock z = Clock.fixed(Instant.EPOCH, ZoneId.of("Z"));
        var reconciler = new KafkaProxyIngressReconciler(z);

        Context<KafkaProxyIngress> context = mock(Context.class);
        when(context.getSecondaryResource(KafkaProxy.class, KafkaProxyIngressReconciler.PROXY_EVENT_SOURCE_NAME)).thenReturn(Optional.of(PROXY));

        // when
        var update = reconciler.reconcile(INGRESS, context);

        // then
        assertThat(update).isNotNull();
        assertThat(update.isPatchStatus()).isTrue();
        assertThat(update.getResource()).isPresent();
        KafkaProxyIngressStatusAssert.assertThat(update.getResource().get().getStatus())
                .hasObservedGenerationInSyncWithMetadataOf(INGRESS)
                .conditionList()
                .singleElement()
                .isResolvedRefsTrue(INGRESS);

    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void shouldAddChecksumForValidProxyRef() throws Exception {
        // given
        Clock z = Clock.fixed(Instant.EPOCH, ZoneId.of("Z"));
        var reconciler = new KafkaProxyIngressReconciler(z);

        when(context.getSecondaryResource(KafkaProxy.class, KafkaProxyIngressReconciler.PROXY_EVENT_SOURCE_NAME)).thenReturn(Optional.of(PROXY));

        // when
        var update = reconciler.reconcile(INGRESS, context);

        // then
        assertThat(update)
                .isNotNull()
                .satisfies(uc -> Assertions.assertThat(update.getResource())
                        .isPresent()
                        .satisfies(kpi -> OperatorAssertions.assertThat(kpi.get())
                                .hasAnnotationSatisfying(ResourcesUtil.DEPENDANT_CHECKSUM_ANNOTATION,
                                        actualValue -> Assertions.assertThat(actualValue)
                                                .isNotBlank()
                                                .satisfies(str -> Assertions.assertThat(Long.parseLong(str)).isNotZero()))));

    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void shouldAddExpectedChecksum() throws Exception {
        // given
        Clock z = Clock.fixed(Instant.EPOCH, ZoneId.of("Z"));
        var reconciler = new KafkaProxyIngressReconciler(z);

        when(context.getSecondaryResource(KafkaProxy.class, KafkaProxyIngressReconciler.PROXY_EVENT_SOURCE_NAME)).thenReturn(Optional.of(PROXY));

        // when
        var update = reconciler.reconcile(INGRESS, context);

        // then
        assertThat(update)
                .isNotNull()
                .satisfies(uc -> Assertions.assertThat(update.getResource())
                        .isPresent()
                        .satisfies(kpi -> OperatorAssertions.assertThat(kpi.get())
                                .hasAnnotationSatisfying(ResourcesUtil.DEPENDANT_CHECKSUM_ANNOTATION,
                                        actualValue -> Assertions.assertThat(actualValue)
                                                .isNotBlank()
                                                .satisfies(str -> Assertions.assertThat(Long.parseLong(str)).isEqualTo(4074241081L)))));
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    void shouldIncludeProxyGenerationInChecksum() throws Exception {
        // given
        Clock z = Clock.fixed(Instant.EPOCH, ZoneId.of("Z"));
        var reconciler = new KafkaProxyIngressReconciler(z);

        KafkaProxy updatedProxy = new KafkaProxyBuilder(PROXY).withNewMetadataLike(PROXY.getMetadata()).withGeneration(PROXY_GENERATION + 1).endMetadata().build();

        when(context.getSecondaryResource(KafkaProxy.class, KafkaProxyIngressReconciler.PROXY_EVENT_SOURCE_NAME))
                .thenReturn(Optional.of(PROXY), Optional.of(updatedProxy));
        var initial = reconciler.reconcile(INGRESS, context);

        Assertions.assertThat(initial.getResource()).isPresent();
        String initialChecksum = initial.getResource().get().getMetadata().getAnnotations().get(ResourcesUtil.DEPENDANT_CHECKSUM_ANNOTATION);

        // when
        var update = reconciler.reconcile(INGRESS, context);

        // then
        assertThat(update)
                .isNotNull()
                .satisfies(uc -> {
                    Assertions.assertThat(update.getResource()).isPresent();
                    Assertions.assertThat(update.getResource().get()).satisfies(kpi -> OperatorAssertions.assertThat(kpi)
                            .hasAnnotationSatisfying(ResourcesUtil.DEPENDANT_CHECKSUM_ANNOTATION,
                                    actualValue -> Assertions.assertThat(actualValue)
                                            .isNotBlank()
                                            .isNotEqualTo(initialChecksum)));
                });
    }

    @Test
    void shouldSetResolvedRefsToUnknown() {
        // given
        Clock z = Clock.fixed(Instant.EPOCH, ZoneId.of("Z"));
        var reconciler = new KafkaProxyIngressReconciler(z);

        // when
        var update = reconciler.updateErrorStatus(INGRESS, context, new RuntimeException("Boom!"));

        // then
        assertThat(update).isNotNull();
        assertThat(update.getResource()).isPresent();
        assertThat(update.getResource().get().getStatus())
                .hasObservedGenerationInSyncWithMetadataOf(INGRESS)
                .singleCondition()
                .hasObservedGenerationInSyncWithMetadataOf(INGRESS)
                .isResolvedRefsUnknown("java.lang.RuntimeException", "Boom!")
                .hasLastTransitionTime(TEST_CLOCK.instant());

    }
}
