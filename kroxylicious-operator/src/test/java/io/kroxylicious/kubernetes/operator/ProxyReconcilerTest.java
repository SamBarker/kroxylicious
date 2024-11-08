/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kubernetes.operator;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.javaoperatorsdk.operator.api.reconciler.Context;

import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxy;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyBuilder;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyStatus;
import io.kroxylicious.kubernetes.api.v1alpha1.kafkaproxystatus.Conditions;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyReconcilerTest {

    @Mock
    Context<KafkaProxy> context;

    private AutoCloseable closeable;

    @BeforeEach
    public void openMocks() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void releaseMocks() throws Exception {
        closeable.close();
    }

    @Test
    void successfulInitialReconciliationShouldResultInReadyTrueCondition() {
        // Given
        // @formatter:off
        long generation = 42L;
        var primary = new KafkaProxyBuilder()
                .withNewMetadata()
                    .withGeneration(generation)
                    .withName("my-proxy")
                .endMetadata()
                .build();
        // @formatter:on

        // When
        var updateControl = new ProxyReconciler().reconcile(primary, context);

        // Then
        assertThat(updateControl.isPatchStatus()).isTrue();
        var statusAssert = assertThat(updateControl.getResource()).isNotNull()
                .extracting(KafkaProxy::getStatus);
        statusAssert.extracting(KafkaProxyStatus::getObservedGeneration).isEqualTo(generation);
        ObjectAssert<Conditions> first = statusAssert.extracting(KafkaProxyStatus::getConditions, InstanceOfAssertFactories.list(Conditions.class))
                .first();
        first.extracting(Conditions::getObservedGeneration).isEqualTo(generation);
        first.extracting(Conditions::getLastTransitionTime).isNotNull();
        first.extracting(Conditions::getType).isEqualTo("Ready");
        first.extracting(Conditions::getStatus).isEqualTo(Conditions.Status.TRUE);
        first.extracting(Conditions::getMessage).isEqualTo("");
        first.extracting(Conditions::getReason).isEqualTo("");
    }

    @Test
    void failedInitialReconciliationShouldResultInReadyTrueCondition() {
        // Given
        // @formatter:off
        long generation = 42L;
        var primary = new KafkaProxyBuilder()
                .withNewMetadata()
                .withGeneration(generation)
                .withName("my-proxy")
                .endMetadata()
                .build();
        // @formatter:on

        // When
        var updateControl = new ProxyReconciler().updateErrorStatus(primary, context, new InvalidResourceException("Resource was terrible"));

        // Then
        assertThat(updateControl.isPatch()).isTrue();
        var statusAssert = assertThat(updateControl.getResource()).isNotNull()
                .isPresent().get()
                .extracting(KafkaProxy::getStatus);
        statusAssert.extracting(KafkaProxyStatus::getObservedGeneration).isEqualTo(generation);
        ObjectAssert<Conditions> first = statusAssert.extracting(KafkaProxyStatus::getConditions, InstanceOfAssertFactories.list(Conditions.class))
                .first();
        first.extracting(Conditions::getObservedGeneration).isEqualTo(generation);
        first.extracting(Conditions::getLastTransitionTime).isNotNull();
        first.extracting(Conditions::getType).isEqualTo("Ready");
        first.extracting(Conditions::getStatus).isEqualTo(Conditions.Status.FALSE);
        first.extracting(Conditions::getMessage).isEqualTo("Resource was terrible");
        first.extracting(Conditions::getReason).isEqualTo("InvalidResourceException");
    }

    @Test
    void remainInReadyTrueShouldRetainTransitionTime() {
        // Given
        long generation = 42L;
        var time = ZonedDateTime.now(ZoneId.of("Z"));
        // @formatter:off
        var primary = new KafkaProxyBuilder()
                .withNewMetadata()
                    .withGeneration(generation)
                    .withName("my-proxy")
                .endMetadata()
                .withNewStatus()
                    .addNewCondition()
                        .withType("Ready")
                        .withStatus(Conditions.Status.TRUE)
                        .withMessage("")
                        .withReason("")
                        .withLastTransitionTime(time)
                    .endCondition()
                .endStatus()
                .build();
        // @formatter:on

        // When
        var updateControl = new ProxyReconciler().reconcile(primary, context);

        // Then
        assertThat(updateControl.isPatchStatus()).isTrue();
        var statusAssert = assertThat(updateControl.getResource()).isNotNull()
                .extracting(KafkaProxy::getStatus);
        statusAssert.extracting(KafkaProxyStatus::getObservedGeneration).isEqualTo(generation);
        ObjectAssert<Conditions> first = statusAssert.extracting(KafkaProxyStatus::getConditions, InstanceOfAssertFactories.list(Conditions.class))
                .first();
        first.extracting(Conditions::getObservedGeneration).isEqualTo(generation);
        first.extracting(Conditions::getLastTransitionTime).isEqualTo(time);
        first.extracting(Conditions::getType).isEqualTo("Ready");
        first.extracting(Conditions::getStatus).isEqualTo(Conditions.Status.TRUE);
        first.extracting(Conditions::getMessage).isEqualTo("");
        first.extracting(Conditions::getReason).isEqualTo("");
    }

    @Test
    void transitionToReadyFalseShouldChangeTransitionTime() {
        // Given
        long generation = 42L;
        var time = ZonedDateTime.now(ZoneId.of("Z"));
        // @formatter:off
        var primary = new KafkaProxyBuilder()
                .withNewMetadata()
                    .withGeneration(generation)
                    .withName("my-proxy")
                .endMetadata()
                .withNewStatus()
                    .addNewCondition()
                        .withType("Ready")
                        .withStatus(Conditions.Status.TRUE)
                        .withMessage("")
                        .withReason("")
                        .withLastTransitionTime(time)
                    .endCondition()
                .endStatus()
                .build();
        // @formatter:on

        // When
        var updateControl = new ProxyReconciler().updateErrorStatus(primary, context, new InvalidResourceException("Resource was terrible"));

        // Then
        assertThat(updateControl.isPatch()).isTrue();
        var statusAssert = assertThat(updateControl.getResource()).isNotNull().isPresent().get()
                .extracting(KafkaProxy::getStatus);
        statusAssert.extracting(KafkaProxyStatus::getObservedGeneration).isEqualTo(generation);
        ObjectAssert<Conditions> first = statusAssert.extracting(KafkaProxyStatus::getConditions, InstanceOfAssertFactories.list(Conditions.class))
                .first();
        first.extracting(Conditions::getObservedGeneration).isEqualTo(generation);
        first.extracting(Conditions::getLastTransitionTime).isNotEqualTo(time);
        first.extracting(Conditions::getType).isEqualTo("Ready");
        first.extracting(Conditions::getStatus).isEqualTo(Conditions.Status.FALSE);
        first.extracting(Conditions::getMessage).isEqualTo("Resource was terrible");
        first.extracting(Conditions::getReason).isEqualTo(InvalidResourceException.class.getSimpleName());
    }

    @Test
    void remainInReadyFalseShouldRetainTransitionTime() {
        // Given
        long generation = 42L;
        var time = ZonedDateTime.now(ZoneId.of("Z"));
        // @formatter:off
        var primary = new KafkaProxyBuilder()
                 .withNewMetadata()
                    .withGeneration(generation)
                    .withName("my-proxy")
                .endMetadata()
                .withNewStatus()
                    .addNewCondition()
                        .withType("Ready")
                        .withStatus(Conditions.Status.FALSE)
                        .withMessage("Resource was terrible")
                        .withReason(InvalidResourceException.class.getSimpleName())
                        .withLastTransitionTime(time)
                    .endCondition()
                .endStatus()
                .build();
        // @formatter:on

        // When
        var updateControl = new ProxyReconciler().updateErrorStatus(primary, context, new InvalidResourceException("Resource was terrible"));

        // Then
        assertThat(updateControl.isPatch()).isTrue();
        var statusAssert = assertThat(updateControl.getResource()).isNotNull().isPresent().get()
                .extracting(KafkaProxy::getStatus);
        statusAssert.extracting(KafkaProxyStatus::getObservedGeneration).isEqualTo(generation);
        ObjectAssert<Conditions> first = statusAssert.extracting(KafkaProxyStatus::getConditions, InstanceOfAssertFactories.list(Conditions.class))
                .first();
        first.extracting(Conditions::getObservedGeneration).isEqualTo(generation);
        first.extracting(Conditions::getLastTransitionTime).isEqualTo(time);
        first.extracting(Conditions::getType).isEqualTo("Ready");
        first.extracting(Conditions::getStatus).isEqualTo(Conditions.Status.FALSE);
        first.extracting(Conditions::getMessage).isEqualTo("Resource was terrible");
        first.extracting(Conditions::getReason).isEqualTo(InvalidResourceException.class.getSimpleName());
    }

    @Test
    void transitionToReadyTrueShouldChangeTransitionTime() {
        // Given
        long generation = 42L;
        var time = ZonedDateTime.now(ZoneId.of("Z"));
        // @formatter:off
        var primary = new KafkaProxyBuilder()
                .withNewMetadata()
                    .withGeneration(generation)
                    .withName("my-proxy")
                .endMetadata()
                .withNewStatus()
                    .addNewCondition()
                        .withType("Ready")
                        .withStatus(Conditions.Status.FALSE)
                        .withMessage("Resource was terrible")
                        .withReason(InvalidResourceException.class.getSimpleName())
                        .withLastTransitionTime(time)
                    .endCondition()
                .endStatus()
                .build();
        // @formatter:on

        // When
        var updateControl = new ProxyReconciler().reconcile(primary, context);

        // Then
        assertThat(updateControl.isPatchStatus()).isTrue();
        var statusAssert = assertThat(updateControl.getResource()).isNotNull()
                .extracting(KafkaProxy::getStatus);
        statusAssert.extracting(KafkaProxyStatus::getObservedGeneration).isEqualTo(generation);
        ObjectAssert<Conditions> first = statusAssert.extracting(KafkaProxyStatus::getConditions, InstanceOfAssertFactories.list(Conditions.class))
                .first();
        first.extracting(Conditions::getObservedGeneration).isEqualTo(generation);
        first.extracting(Conditions::getLastTransitionTime).isNotEqualTo(time);
        first.extracting(Conditions::getType).isEqualTo("Ready");
        first.extracting(Conditions::getStatus).isEqualTo(Conditions.Status.TRUE);
        first.extracting(Conditions::getMessage).isEqualTo("");
        first.extracting(Conditions::getReason).isEqualTo("");
    }

}
