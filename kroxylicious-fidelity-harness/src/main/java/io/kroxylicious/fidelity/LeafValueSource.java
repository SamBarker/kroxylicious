/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.fidelity;

import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Supplies a value for a single leaf field of a generated {@code *Data}/nested-struct instance - a
 * primitive, a boxed primitive, {@code String}, {@code byte[]}, {@code ByteBuffer}, {@code Uuid}, or
 * {@code BaseRecords}. Leaf fields are opaque and non-recursive: unlike a nested struct, a {@code List},
 * or a collection, nothing about their contents is walked further by {@link ReflectiveMessagePopulator}.
 * <p>
 * {@code javaType} is always a plain {@link Class}, never a {@link java.lang.reflect.ParameterizedType} -
 * only {@code List<T>}/struct/collection fields are parameterized, and those never reach this interface.
 *
 * @see InRangeLeafValueSource the default implementation, producing well-formed, in-bounds values
 */
public interface LeafValueSource {

    /**
     * Supplies a value for one leaf field.
     *
     * @param javaType the field's Java type
     * @param fieldPath identifies the field being populated; {@code null} when generating a {@code List}/
     *            collection element, where there's no single field the value individually belongs to
     * @param schemaTypeName the schema's wire type name (e.g. {@code "UINT16"}, {@code "INT32"},
     *            {@code "STRING"}); {@code null} under the same circumstances as {@code fieldPath}
     * @param random the shared randomness source for the whole object graph being populated
     * @return the value to set, or {@link Optional#empty()} if this source doesn't handle {@code javaType}
     */
    Optional<Object> valueFor(Class<?> javaType, FieldPath fieldPath, String schemaTypeName, RandomGenerator random);
}
