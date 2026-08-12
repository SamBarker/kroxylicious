/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kroxylicious.kafka.common.compress;

import io.kroxylicious.kafka.common.record.internal.CompressionType;
import io.kroxylicious.kafka.common.utils.BufferSupplier;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Bridge interface for {@code org.apache.kafka.common.compress.Compression}, using types from the
 * {@code io.kroxylicious.kafka} namespace.
 *
 * TODO #4578: replace this bridge with a proper copy of Compression once CompressionType is moved to io.kroxylicious namespace
 */
public interface Compression {

    CompressionType type();

    OutputStream wrapForOutput(ByteBufferOutputStream bufferStream, byte messageVersion);

    InputStream wrapForInput(ByteBuffer buffer, byte messageVersion, BufferSupplier decompressionBufferSupplier);

    default int decompressionOutputSize() {
        throw new UnsupportedOperationException("Size of decompression buffer is not defined for this compression type=" + type().name);
    }

    interface Builder<T extends Compression> {
        T build();
    }

    Compression NONE = none().build();

    static Builder<Compression> none() {
        return () -> new KafkaCompressionAdapter(org.apache.kafka.common.compress.Compression.NONE);
    }

    static Builder<? extends Compression> of(CompressionType compressionType) {
        org.apache.kafka.common.record.CompressionType kafkaType =
                org.apache.kafka.common.record.CompressionType.forId(compressionType.id);
        return () -> new KafkaCompressionAdapter(
                org.apache.kafka.common.compress.Compression.of(kafkaType).build());
    }
}
