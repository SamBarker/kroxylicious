/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import io.kroxylicious.proxy.model.VirtualClusterModel;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Owns the virtual cluster configuration tree and lifecycle state.
 * <p>
 * This is the single source of truth for which virtual clusters exist and their
 * current lifecycle state. It does not manage networking, endpoint registration,
 * metrics, or any Netty infrastructure — those remain with {@link io.kroxylicious.proxy.KafkaProxy}.
 * </p>
 * <p>
 * The {@code onVirtualClusterStopped} callback notifies the owner (typically KafkaProxy)
 * when a virtual cluster reaches the terminal {@link VirtualClusterLifecycleState.Stopped}
 * state, allowing the owner to apply proxy-level policy (e.g. {@code serve: none}).
 * During reload, the Draining → Initializing → Serving cycle is managed internally
 * without involving the callback — reload never reaches Stopped.
 * </p>
 */
public class VirtualClusterManager {

    private final List<VirtualClusterModel> virtualClusterModels;
    private final Map<String, VirtualClusterLifecycleManager> lifecycleManagers;
    private final BiConsumer<String, Optional<Throwable>> onVirtualClusterStopped;

    /**
     * Creates a new VirtualClusterManager for the given set of virtual clusters.
     *
     * @param virtualClusterModels the complete set of virtual cluster configurations
     * @param onVirtualClusterStopped callback invoked with {@code (clusterName, priorFailureCause)}
     *        whenever a virtual cluster reaches the terminal Stopped state. The cause is empty
     *        for clean stops (e.g. drain completed during shutdown) and present for failure-driven stops.
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if the list contains duplicate cluster names
     */
    public VirtualClusterManager(List<VirtualClusterModel> virtualClusterModels,
                                 BiConsumer<String, Optional<Throwable>> onVirtualClusterStopped) {
        Objects.requireNonNull(virtualClusterModels, "virtualClusterModels must not be null");
        this.onVirtualClusterStopped = Objects.requireNonNull(onVirtualClusterStopped, "onVirtualClusterStopped must not be null");
        this.virtualClusterModels = List.copyOf(virtualClusterModels);
        this.lifecycleManagers = new LinkedHashMap<>();
        for (var vcm : this.virtualClusterModels) {
            var name = vcm.getClusterName();
            if (lifecycleManagers.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate cluster name: " + name);
            }
            lifecycleManagers.put(name, new VirtualClusterLifecycleManager(name));
        }
    }

    /**
     * Returns the virtual cluster models this manager was constructed with.
     * @return unmodifiable list of virtual cluster models
     */
    public List<VirtualClusterModel> getVirtualClusterModels() {
        return virtualClusterModels;
    }

    /**
     * Returns the lifecycle manager for the given virtual cluster name.
     * @param clusterName the virtual cluster name
     * @return the lifecycle manager, or null if no cluster with that name exists
     */
    @Nullable
    public VirtualClusterLifecycleManager lifecycleManagerFor(String clusterName) {
        return lifecycleManagers.get(clusterName);
    }
}
