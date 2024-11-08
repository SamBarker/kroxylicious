/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.kubernetes.operator;

import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxy;
import io.kroxylicious.kubernetes.api.v1alpha1.kafkaproxyspec.Clusters;
import io.kroxylicious.proxy.config.ClusterNetworkAddressConfigProviderDefinition;
import io.kroxylicious.proxy.config.ConfigParser;
import io.kroxylicious.proxy.config.Configuration;
import io.kroxylicious.proxy.config.TargetCluster;
import io.kroxylicious.proxy.config.VirtualCluster;
import io.kroxylicious.proxy.config.admin.AdminHttpConfiguration;
import io.kroxylicious.proxy.config.admin.EndpointsConfiguration;
import io.kroxylicious.proxy.config.admin.PrometheusMetricsConfig;
import io.kroxylicious.proxy.internal.clusternetworkaddressconfigprovider.PortPerBrokerClusterNetworkAddressConfigProvider;
import io.kroxylicious.proxy.service.HostPort;

/**
 * A Kube {@code Secret} containing the proxy config YAML.
 * We use a {@code Secret} (rather than a {@code ConfigMap})
 * because the config might contain sensitive settings like passwords
 */
@KubernetesDependent
public class ProxyConfigSecret
        extends CRUDKubernetesDependentResource<Secret, KafkaProxy> {

    /**
     * The key of the {@code config.yaml} entry in the desired {@code Secret}.
     */
    public static final String CONFIG_YAML_KEY = "config.yaml";

    public ProxyConfigSecret() {
        super(Secret.class);
    }

    /**
     * @return The {@code metadata.name} of the desired Secret {@code Secret}.
     */
    static String secretName(KafkaProxy primary) {
        return primary.getMetadata().getName();
    }

    @Override
    protected Secret desired(KafkaProxy primary,
                             Context<KafkaProxy> context) {
        // @formatter:off
        return new SecretBuilder()
                .editOrNewMetadata()
                    .withName(secretName(primary))
                    .withNamespace(primary.getMetadata().getNamespace())
                    .addNewOwnerReferenceLike(ResourcesUtil.ownerReferenceTo(primary)).endOwnerReference()
                .endMetadata()
                .withStringData(Map.of(CONFIG_YAML_KEY, generateProxyConfig(primary)))
                .build();
        // @formatter:on
    }

    String generateProxyConfig(KafkaProxy primary) {

        List<Clusters> clusters = ResourcesUtil.distinctClusters(primary);
        var virtualClusters = clusters.stream()
                .collect(Collectors.toMap(
                        Clusters::getName,
                        cluster -> getVirtualCluster(primary, cluster),
                        (v1, v2) -> v1, // Dupes handled below
                        LinkedHashMap::new));

        Configuration configuration = new Configuration(
                new AdminHttpConfiguration(null, null, new EndpointsConfiguration(new PrometheusMetricsConfig())),
                virtualClusters,
                List.of(),
                List.of(),
                false);

        ObjectMapper mapper = ConfigParser.createObjectMapper();
        try {
            return mapper.writeValueAsString(configuration);
        }
        catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static VirtualCluster getVirtualCluster(KafkaProxy primary, Clusters cluster) {
        var o = ResourcesUtil.distinctClusters(primary);
        var clusterNum = o.indexOf(cluster);
        return new VirtualCluster(
                new TargetCluster(cluster.getUpstream().getBootstrapServers(), Optional.empty()),
                new ClusterNetworkAddressConfigProviderDefinition(
                        "PortPerBrokerClusterNetworkAddressConfigProvider",
                        new PortPerBrokerClusterNetworkAddressConfigProvider.PortPerBrokerClusterNetworkAddressConfigProviderConfig(
                                new HostPort("localhost", 9292 + (100 * clusterNum)),
                                ClusterService.serviceName(cluster),
                                null,
                                null,
                                null)),
                Optional.empty(),
                false, false);
    }
}
