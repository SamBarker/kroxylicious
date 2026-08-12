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
package io.kroxylicious.kafka.common.utils;

import java.nio.ByteBuffer;

/**
 * Thin subclass of the Kafka {@code ByteBufferOutputStream} so that instances of this class can be passed
 * wherever {@code org.apache.kafka.common.utils.ByteBufferOutputStream} is expected while also being
 * usable as {@code io.kroxylicious.kafka.common.utils.ByteBufferOutputStream}.
 *
 * TODO #4578: evaluate whether this bridge is still needed once Compression is properly copied.
 */
public class ByteBufferOutputStream extends org.apache.kafka.common.utils.ByteBufferOutputStream {

    public ByteBufferOutputStream(ByteBuffer buffer) {
        super(buffer);
    }

    public ByteBufferOutputStream(int initialCapacity) {
        super(initialCapacity);
    }

    public ByteBufferOutputStream(int initialCapacity, boolean directBuffer) {
        super(initialCapacity, directBuffer);
    }
}
