/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.fidelity;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.kroxylicious.kafka.common.message.AddPartitionsToTxnRequestData;
import io.kroxylicious.kafka.common.message.AddRaftVoterRequestData;
import io.kroxylicious.kafka.common.message.AlterClientQuotasRequestData;
import io.kroxylicious.kafka.common.message.DescribeClusterRequestData;
import io.kroxylicious.kafka.common.message.FetchSnapshotRequestData;
import io.kroxylicious.kafka.common.message.LeaveGroupRequestData;
import io.kroxylicious.kafka.common.message.ProduceRequestData;
import io.kroxylicious.kafka.common.message.RequestHeaderData;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectiveMessagePopulatorTest {

    private static final long SEED = 42L;

    @Test
    void shouldPopulateFlatPrimitiveAndStringFields() {
        // Given
        RequestHeaderData message = new RequestHeaderData();

        // When
        ReflectiveMessagePopulator.populate(message, message.highestSupportedVersion(), SEED);

        // Then
        assertThat(message.requestApiKey()).isNotZero();
        assertThat(message.requestApiVersion()).isNotZero();
        assertThat(message.correlationId()).isNotZero();
        assertThat(message.clientId()).isNotNull().isNotEmpty();
    }

    @Test
    void shouldPopulateKafkaNamespaceFieldsTheSameWay() {
        // Given
        org.apache.kafka.common.message.RequestHeaderData message = new org.apache.kafka.common.message.RequestHeaderData();

        // When
        ReflectiveMessagePopulator.populate(message, message.highestSupportedVersion(), SEED);

        // Then
        assertThat(message.requestApiKey()).isNotZero();
        assertThat(message.requestApiVersion()).isNotZero();
        assertThat(message.correlationId()).isNotZero();
        assertThat(message.clientId()).isNotNull().isNotEmpty();
    }

    @Test
    void shouldPopulateNestedStructListFields() {
        // Given
        LeaveGroupRequestData message = new LeaveGroupRequestData();

        // When
        ReflectiveMessagePopulator.populate(message, message.highestSupportedVersion(), SEED);

        // Then
        assertThat(message.groupId()).isNotNull().isNotEmpty();
        List<LeaveGroupRequestData.MemberIdentity> members = message.members();
        assertThat(members).isNotEmpty()
                .allSatisfy(member -> assertThat(member.memberId()).isNotNull().isNotEmpty());
    }

    @Test
    void shouldNotFabricateUnknownTaggedFields() {
        // Given
        RequestHeaderData message = new RequestHeaderData();

        // When
        ReflectiveMessagePopulator.populate(message, message.highestSupportedVersion(), SEED);

        // Then
        assertThat(message.unknownTaggedFields()).isEmpty();
    }

    @Test
    void shouldPopulateDoubleAndBooleanFields() {
        // Given
        AlterClientQuotasRequestData.OpData message = new AlterClientQuotasRequestData.OpData();

        // When
        ReflectiveMessagePopulator.populate(message, message.highestSupportedVersion(), SEED);

        // Then
        assertThat(message.key()).isNotNull().isNotEmpty();
        assertThat(message.value()).isNotZero();
        assertThat(message.remove()).isTrue();
    }

    @Test
    void shouldPopulateLongUuidAndNestedSingularStructFields() {
        // Given
        FetchSnapshotRequestData.PartitionSnapshot message = new FetchSnapshotRequestData.PartitionSnapshot();

        // When
        ReflectiveMessagePopulator.populate(message, message.highestSupportedVersion(), SEED);

        // Then
        assertThat(message.position()).isNotZero();
        assertThat(message.replicaDirectoryId()).isNotEqualTo(io.kroxylicious.kafka.common.Uuid.ZERO_UUID);
        assertThat(message.snapshotId().endOffset()).isNotZero();
    }

    @Test
    void shouldNotOverwriteAConstructorAssignedNonZeroDefault() {
        // Given
        DescribeClusterRequestData message = new DescribeClusterRequestData();

        // When - endpointType is only part of the schema from version 1 onward; at version 0 it's outside
        // the schema, so populate() must leave it at its constructor-assigned default rather than fabricate
        // a value write() would then refuse to serialise below version 1.
        ReflectiveMessagePopulator.populate(message, (short) 0, SEED);

        // Then
        assertThat(message.endpointType()).isEqualTo((byte) 1);
    }

    @Test
    void shouldPopulateBaseRecordsFieldsWithEmptyRecords() {
        // Given
        ProduceRequestData.PartitionProduceData message = new ProduceRequestData.PartitionProduceData();

        // When
        ReflectiveMessagePopulator.populate(message, message.highestSupportedVersion(), SEED);

        // Then - BaseRecords is an interface with no general-purpose implementation to populate
        // reflectively; substituting the canonical empty records is the same kind of leaf-value
        // substitution as ByteBuffer/Uuid, not an attempt to fabricate a real record batch.
        assertThat(message.records()).isNotNull();
    }

    @Test
    void shouldNotPopulateFieldsExcludedFromTheTargetVersionsSchema() {
        // Given
        AddPartitionsToTxnRequestData message = new AddPartitionsToTxnRequestData();

        // When - v3AndBelowTransactionalId and its siblings were dropped from the schema by version 4,
        // replaced by the transactions field; both are still declared on the Java class (for backward
        // compatible reads) but only one set is actually part of the highest version's wire schema.
        ReflectiveMessagePopulator.populate(message, message.highestSupportedVersion(), SEED);

        // Then
        assertThat(message.v3AndBelowTransactionalId()).isEmpty();
        assertThat((Iterable<?>) message.transactions()).isNotEmpty();
    }

    @Test
    void shouldPopulateUint16FieldsWithinRange() {
        // Given
        AddRaftVoterRequestData.Listener message = new AddRaftVoterRequestData.Listener();

        // When
        ReflectiveMessagePopulator.populate(message, message.highestSupportedVersion(), SEED);

        // Then - port is wire-typed UINT16 (an unsigned short); the generated setter rejects values
        // outside 0..65535, unlike a plain INT32 field where any int is valid.
        assertThat(message.port()).isBetween(0, 65535);
    }

    @Test
    void shouldBeReproducibleForTheSameSeed() {
        // Given
        RequestHeaderData first = new RequestHeaderData();
        RequestHeaderData second = new RequestHeaderData();

        // When
        ReflectiveMessagePopulator.populate(first, first.highestSupportedVersion(), SEED);
        ReflectiveMessagePopulator.populate(second, second.highestSupportedVersion(), SEED);

        // Then
        assertThat(second).usingRecursiveComparison().isEqualTo(first);
    }
}
