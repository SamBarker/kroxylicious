/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.fidelity;

/**
 * Identifies a single schema field by name.
 * <p>
 * A bare name, not a path: it can't distinguish two same-named fields at different nesting depths (e.g.
 * two different structs each with their own {@code name} field). Wrapped in its own type now (rather than
 * a plain {@code String}) so that {@link LeafValueSource}, already public API, can grow this into a real
 * multi-segment path later without a breaking signature change.
 *
 * @param name the schema field's camelCase name
 */
public record FieldPath(String name) {

    @Override
    public String toString() {
        return name;
    }
}
