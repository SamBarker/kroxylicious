/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.fidelity;

import java.nio.ByteBuffer;

import org.apache.kafka.common.protocol.ByteBufferAccessor;
import org.apache.kafka.common.protocol.Message;
import org.apache.kafka.common.protocol.MessageSizeAccumulator;
import org.apache.kafka.common.protocol.ObjectSerializationCache;

/**
 * Everything scoped to Kafka's generated protocol classes ({@code org.apache.kafka.*}) - the counterpart
 * to {@link KroxyliciousSerdes}: serializing/deserializing {@code Message} instances to and from raw bytes
 * (the standard two-pass size-then-write protocol), and recognising a specific type under this namespace
 * by name, since Kroxylicious's and Kafka's generated classes are structurally identical but otherwise
 * unrelated, with no common supertype to {@code instanceof} against.
 */
public final class KafkaSerdes {

    private static final String ROOT = "org.apache.kafka.";

    private KafkaSerdes() {
    }

    /**
     * Serializes a message to bytes at the given version.
     *
     * @param message the message to serialize
     * @param version the protocol version to serialize at
     * @return the serialized bytes
     */
    public static byte[] write(Message message, short version) {
        ObjectSerializationCache cache = new ObjectSerializationCache();
        MessageSizeAccumulator accumulator = new MessageSizeAccumulator();
        message.addSize(accumulator, cache, version);
        ByteBuffer buffer = ByteBuffer.allocate(accumulator.totalSize());
        message.write(new ByteBufferAccessor(buffer), cache, version);
        return MessageSerdesUtil.asByteArray(buffer);
    }

    /**
     * Deserializes bytes into the given message instance at the given version.
     *
     * @param message the instance to populate
     * @param bytes the bytes to deserialize
     * @param version the protocol version to deserialize at
     * @param <T> the message type
     * @return {@code message}, populated, plus how many bytes of {@code bytes} were left unconsumed
     */
    public static <T extends Message> ReadResult<T> read(T message, byte[] bytes, short version) {
        ByteBufferAccessor accessor = new ByteBufferAccessor(ByteBuffer.wrap(bytes));
        try {
            message.read(accessor, version);
            return new ReadResult<>(message, accessor.remaining(), null);
        }
        catch (RuntimeException e) {
            return new ReadResult<>(message, accessor.remaining(), e);
        }
    }

    /**
     * Builds the fully-qualified class name under this namespace for a name relative to it.
     *
     * @param relativeName the name relative to {@code org.apache.kafka}, e.g. {@code "common.Uuid"}
     * @return the fully-qualified class name under this namespace
     */
    public static String apiType(String relativeName) {
        return ROOT + relativeName;
    }

    /**
     * Tests whether a class is exactly the type named by a name relative to this namespace.
     *
     * @param clazz the class to test
     * @param relativeName the name relative to {@code org.apache.kafka}, e.g. {@code "common.Uuid"}
     * @return true if {@code clazz} is exactly the type named by {@code relativeName} under this namespace
     */
    @SuppressWarnings("java:S1872") // No common supertype exists to instanceof against: Kroxylicious's and
    // Kafka's generated protocol classes are structurally identical but unrelated types, matched here by
    // fully-qualified name on purpose.
    public static boolean isApiType(Class<?> clazz, String relativeName) {
        return clazz.getName().equals(apiType(relativeName));
    }
}
