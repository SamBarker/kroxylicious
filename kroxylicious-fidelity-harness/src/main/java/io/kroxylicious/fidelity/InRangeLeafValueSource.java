/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.fidelity;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Produces deterministic, well-formed, in-bounds values - the default {@link LeafValueSource}, used
 * whenever a fidelity test just needs realistic, non-default field values rather than deliberately
 * malformed ones.
 */
public final class InRangeLeafValueSource implements LeafValueSource {

    private static final String UUID_RELATIVE_NAME = "common.Uuid";
    private static final String BASE_RECORDS_RELATIVE_NAME = "common.record.internal.BaseRecords";

    @Override
    public Optional<Object> valueFor(Class<?> javaType, FieldPath fieldPath, String schemaTypeName, RandomGenerator random) {
        return scalarValueFor(javaType, schemaTypeName, random)
                .or(() -> uuidValueFor(javaType, random))
                .or(() -> baseRecordsValueFor(javaType));
    }

    /**
     * Flat, non-recursive leaf values: primitives, their boxed equivalents, and the handful of built-in
     * reference types ({@code String}, {@code byte[]}, {@code ByteBuffer}) treated as opaque blobs rather
     * than structures to recurse into. Returns {@link Optional#empty()} for any other type.
     */
    private Optional<Object> scalarValueFor(Class<?> type, String schemaTypeName, RandomGenerator random) {
        // A UINT16 field is wire-typed as an unsigned short but represented as a Java int (a signed short
        // can't hold the full 0..65535 range); the generated setter rejects anything outside that range,
        // unlike a genuine INT32 field, so it needs its own bounded value ahead of the plain int check.
        if ("UINT16".equals(schemaTypeName)) {
            return Optional.of(1 + random.nextInt(0xFFFF));
        }
        if (type == short.class || type == Short.class) {
            return Optional.of((short) (1 + random.nextInt(Short.MAX_VALUE)));
        }
        if (type == int.class || type == Integer.class) {
            return Optional.of(1 + random.nextInt(Integer.MAX_VALUE - 1));
        }
        if (type == long.class || type == Long.class) {
            return Optional.of(1L + random.nextInt(Integer.MAX_VALUE - 1));
        }
        if (type == byte.class || type == Byte.class) {
            return Optional.of((byte) (1 + random.nextInt(Byte.MAX_VALUE)));
        }
        if (type == boolean.class || type == Boolean.class) {
            return Optional.of(true);
        }
        if (type == double.class || type == Double.class) {
            return Optional.of(1.0 + random.nextInt(1_000_000));
        }
        if (type == String.class) {
            return Optional.of("value-" + random.nextInt(1_000_000));
        }
        if (type == byte[].class) {
            return Optional.of(randomBytes(random));
        }
        if (type == ByteBuffer.class) {
            return Optional.of(ByteBuffer.wrap(randomBytes(random)));
        }
        return Optional.empty();
    }

    private byte[] randomBytes(RandomGenerator random) {
        byte[] bytes = new byte[4 + random.nextInt(8)];
        random.nextBytes(bytes);
        return bytes;
    }

    private Optional<Object> uuidValueFor(Class<?> type, RandomGenerator random) {
        if (!isUuid(type)) {
            return Optional.empty();
        }
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(long.class, long.class);
            return Optional.of(constructor.newInstance(random.nextLong(), random.nextLong()));
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to construct " + type + " via its (long, long) constructor", e);
        }
    }

    private static boolean isUuid(Class<?> type) {
        return KroxyliciousSerdes.isApiType(type, UUID_RELATIVE_NAME) || KafkaSerdes.isApiType(type, UUID_RELATIVE_NAME);
    }

    /**
     * {@code BaseRecords} is an interface with no general-purpose implementation to populate reflectively;
     * the canonical empty records value is a leaf-value substitution, the same kind of move as using
     * {@code Uuid.ZERO_UUID}-shaped construction or an empty {@code ByteBuffer} - not an attempt to
     * fabricate a real record batch. Doesn't consume {@code random} at all, same as the {@code boolean}
     * branch above always returning {@code true}.
     */
    private Optional<Object> baseRecordsValueFor(Class<?> type) {
        if (!isBaseRecords(type)) {
            return Optional.empty();
        }
        try {
            Class<?> memoryRecordsClass = Class.forName(type.getPackageName() + ".MemoryRecords");
            return Optional.of(memoryRecordsClass.getField("EMPTY").get(null));
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to look up MemoryRecords.EMPTY alongside " + type, e);
        }
    }

    private static boolean isBaseRecords(Class<?> type) {
        return KroxyliciousSerdes.isApiType(type, BASE_RECORDS_RELATIVE_NAME) || KafkaSerdes.isApiType(type, BASE_RECORDS_RELATIVE_NAME);
    }
}
