/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.fidelity;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.kroxylicious.kafka.common.Uuid;
import io.kroxylicious.kafka.common.record.internal.BaseRecords;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;

import static org.assertj.core.api.Assertions.assertThat;

class InRangeLeafValueSourceTest {

    private static final long SEED = 42L;

    private final InRangeLeafValueSource source = new InRangeLeafValueSource();

    @Test
    void shouldPopulateShortWithNonZeroValue() {
        // Given
        Random random = new Random(SEED);

        // When
        Optional<Object> value = source.valueFor(short.class, null, null, random);

        // Then
        assertThat(value).isPresent();
        assertThat((Short) value.orElseThrow()).isNotZero();
    }

    @Test
    void shouldPopulateUint16FieldWithinRange() {
        // Given - port-style fields are Java int but wire-typed UINT16 (an unsigned short), so the
        // generated setter rejects anything outside 0..65535, unlike a genuine INT32 field.
        Random random = new Random(SEED);

        // When
        Optional<Object> value = source.valueFor(int.class, null, "UINT16", random);

        // Then
        assertThat(value).isPresent();
        assertThat((Integer) value.orElseThrow()).isBetween(0, 65535);
    }

    @Test
    void shouldPopulatePlainIntWithoutUint16Bounding() {
        // Given
        Random random = new Random(SEED);

        // When
        Optional<Object> value = source.valueFor(int.class, null, "INT32", random);

        // Then
        assertThat(value).isPresent();
        assertThat((Integer) value.orElseThrow()).isNotZero();
    }

    @Test
    void shouldPopulateNonEmptyString() {
        // Given
        Random random = new Random(SEED);

        // When
        Optional<Object> value = source.valueFor(String.class, null, null, random);

        // Then
        assertThat(value).isPresent();
        assertThat((String) value.orElseThrow()).isNotEmpty();
    }

    @Test
    void shouldPopulateNonEmptyByteArray() {
        // Given
        Random random = new Random(SEED);

        // When
        Optional<Object> value = source.valueFor(byte[].class, null, null, random);

        // Then
        assertThat(value).isPresent();
        assertThat((byte[]) value.orElseThrow()).isNotEmpty();
    }

    @Test
    void shouldPopulateNonEmptyByteBuffer() {
        // Given
        Random random = new Random(SEED);

        // When
        Optional<Object> value = source.valueFor(ByteBuffer.class, null, null, random);

        // Then
        assertThat(value).isPresent();
        assertThat(((ByteBuffer) value.orElseThrow()).remaining()).isPositive();
    }

    @Test
    void shouldConstructNonZeroUuid() {
        // Given
        Random random = new Random(SEED);

        // When
        Optional<Object> value = source.valueFor(Uuid.class, null, null, random);

        // Then
        assertThat(value).isPresent();
        assertThat(value.orElseThrow()).isInstanceOf(Uuid.class).isNotEqualTo(Uuid.ZERO_UUID);
    }

    @Test
    void shouldSubstituteEmptyRecordsForBaseRecords() {
        // Given
        Random random = new Random(SEED);

        // When
        Optional<Object> value = source.valueFor(BaseRecords.class, null, null, random);

        // Then
        assertThat(value).contains(MemoryRecords.EMPTY);
    }

    @Test
    void shouldReturnEmptyForAnUnhandledType() {
        // Given
        Random random = new Random(SEED);

        // When
        Optional<Object> value = source.valueFor(Object.class, null, null, random);

        // Then
        assertThat(value).isEmpty();
    }
}
