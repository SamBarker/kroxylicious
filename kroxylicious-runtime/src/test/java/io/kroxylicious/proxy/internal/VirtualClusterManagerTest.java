/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.internal;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import io.kroxylicious.proxy.model.VirtualClusterModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VirtualClusterManagerTest {

    @SuppressWarnings("unchecked")
    private final BiConsumer<String, Optional<Throwable>> noOpCallback = mock(BiConsumer.class);

    private static VirtualClusterModel mockModel(String name) {
        var model = mock(VirtualClusterModel.class);
        when(model.getClusterName()).thenReturn(name);
        return model;
    }

    @Test
    void shouldCreateLifecycleManagerInInitializingState() {
        // given
        var vcm = new VirtualClusterManager(List.of(mockModel("cluster-a")), noOpCallback);

        // when
        var manager = vcm.lifecycleManagerFor("cluster-a");

        // then
        assertThat(manager).isNotNull()
                .extracting(VirtualClusterLifecycleManager::getState)
                .isInstanceOf(VirtualClusterLifecycleState.Initializing.class);
    }

    @Test
    void shouldCreateLifecycleManagerForEachModel() {
        // given
        var vcm = new VirtualClusterManager(
                List.of(mockModel("cluster-a"), mockModel("cluster-b")),
                noOpCallback);

        // when/then
        assertThat(vcm.lifecycleManagerFor("cluster-a")).isNotNull();
        assertThat(vcm.lifecycleManagerFor("cluster-b")).isNotNull();
    }

    @Test
    void shouldReturnNullForUnknownCluster() {
        // given
        var vcm = new VirtualClusterManager(List.of(mockModel("cluster-a")), noOpCallback);

        // when
        var result = vcm.lifecycleManagerFor("nonexistent");

        // then
        assertThat(result).isNull();
    }

    @Test
    void shouldExposeVirtualClusterModels() {
        // given
        var modelA = mockModel("cluster-a");
        var modelB = mockModel("cluster-b");
        var vcm = new VirtualClusterManager(List.of(modelA, modelB), noOpCallback);

        // when
        var models = vcm.getVirtualClusterModels();

        // then
        assertThat(models).containsExactly(modelA, modelB);
    }

    @Test
    void shouldRejectNullModels() {
        assertThatThrownBy(() -> new VirtualClusterManager(null, noOpCallback))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullCallback() {
        List<VirtualClusterModel> virtualClusterModels = List.of(mockModel("cluster-a"));
        assertThatThrownBy(() -> new VirtualClusterManager(virtualClusterModels, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectDuplicateClusterNames() {
        List<VirtualClusterModel> virtualClusterModels = List.of(mockModel("cluster-a"), mockModel("cluster-a"));
        assertThatThrownBy(() -> new VirtualClusterManager(
                virtualClusterModels,
                noOpCallback))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cluster-a");
    }
}
