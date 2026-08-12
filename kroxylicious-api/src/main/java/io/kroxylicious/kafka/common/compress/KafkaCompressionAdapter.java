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
 * Adapts {@code org.apache.kafka.common.compress.Compression} to the {@link Compression} interface.
 * {@link ByteBufferOutputStream} and {@link BufferSupplier} extend their Kafka originals so instances
 * can be passed directly to the delegate's {@code wrapForOutput}/{@code wrapForInput} methods.
 */
class KafkaCompressionAdapter implements Compression {

    private final org.apache.kafka.common.compress.Compression delegate;

    KafkaCompressionAdapter(org.apache.kafka.common.compress.Compression delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompressionType type() {
        return CompressionType.forId(delegate.type().id);
    }

    @Override
    public OutputStream wrapForOutput(ByteBufferOutputStream bufferStream, byte messageVersion) {
        // ByteBufferOutputStream extends org.apache.kafka.common.utils.ByteBufferOutputStream
        return delegate.wrapForOutput(bufferStream, messageVersion);
    }

    @Override
    public InputStream wrapForInput(ByteBuffer buffer, byte messageVersion, BufferSupplier decompressionBufferSupplier) {
        // BufferSupplier extends org.apache.kafka.common.utils.BufferSupplier
        return delegate.wrapForInput(buffer, messageVersion, decompressionBufferSupplier);
    }

    @Override
    public int decompressionOutputSize() {
        return delegate.decompressionOutputSize();
    }
}
