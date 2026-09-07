/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.fidelity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Reflectively walks a generated {@code *Data}/nested-struct instance's own fields - filtering to the
 * ones present in its schema at a given version, recursing into nested structs, lists and collections -
 * and invokes each field's setter, so fidelity tests can exercise more than default-constructed
 * (all-zero) instances without hand-authoring a fixture per spec. What value each leaf field gets is
 * delegated to a {@link LeafValueSource}, so this class owns only traversal: which fields exist, in what
 * shape, not what's a well-formed or malformed value for any of them.
 * <p>
 * Only package-private fields are populated: the generated classes declare every real schema field with
 * default (package) access, and use {@code private} exclusively for the generator's own bookkeeping
 * (the unknown-tagged-fields list) - so that visibility split is a reliable signal of "real payload
 * field" versus "generator internals", without depending on field naming.
 * <p>
 * Each call gets its own instance: {@link #populate} recurses into nested structs, lists and
 * collections, and every level of that recursion needs the same {@link Random}, target {@code version},
 * and {@link LeafValueSource} - carrying them as instance state avoids threading all three through every
 * private method.
 */
public final class ReflectiveMessagePopulator {

    private static final String MULTI_COLLECTION_RELATIVE_NAME = "common.utils.ImplicitLinkedHashMultiCollection";
    private static final String TAGGED_FIELDS_RELATIVE_NAME = "common.protocol.types.TaggedFields";

    private final Random random;
    private final short messageVersion;
    private final LeafValueSource leafValueSource;

    private ReflectiveMessagePopulator(Random random, short messageVersion, LeafValueSource leafValueSource) {
        this.random = random;
        this.messageVersion = messageVersion;
        this.leafValueSource = leafValueSource;
    }

    /**
     * Populates {@code message}'s fields with deterministic, well-formed, non-default values, restricted
     * to the fields actually present in {@code message}'s schema at {@code version} - so the result never
     * trips the generated {@code write()}'s own version guards for fields introduced later, or excluded
     * again earlier, than {@code version}. Equivalent to
     * {@code populate(message, version, seed, new InRangeLeafValueSource())}.
     *
     * @param message the instance to populate
     * @param version the wire version {@code message} will be serialised at
     * @param seed the seed controlling the generated values
     */
    public static void populate(Object message, short version, long seed) {
        populate(message, version, seed, new InRangeLeafValueSource());
    }

    /**
     * As {@link #populate(Object, short, long)}, but with an explicit {@link LeafValueSource} - e.g. to
     * deliberately construct malformed values for error-parity testing, rather than always well-formed
     * ones.
     *
     * @param message the instance to populate
     * @param version the wire version {@code message} will be serialised at
     * @param seed the seed controlling the generated values
     * @param leafValueSource supplies the value for each leaf field
     */
    @SuppressFBWarnings("PREDICTABLE_RANDOM") // Deterministic pseudorandomness is the point: reproducible test fixtures, not security relevant
    public static void populate(Object message, short version, long seed, LeafValueSource leafValueSource) {
        new ReflectiveMessagePopulator(new Random(seed), version, leafValueSource).populate(message);
    }

    private void populate(Object message) {
        SchemaFields schemaFields = schemaFieldsAt(message.getClass(), messageVersion);
        // Fields outside the target version's schema (not yet introduced, or dropped again before
        // this version - specs aren't purely additive) must keep their constructor default: it's the
        // only value the generated write() accepts for them at this version.
        Arrays.stream(message.getClass().getDeclaredFields()).filter(field -> {
            int modifiers = field.getModifiers();
            boolean generatorInternal = Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers);
            boolean outsideSchema = !schemaFields.fieldTypeNames().containsKey(field.getName());
            return !generatorInternal && !outsideSchema;
        }).forEach(field -> invokeSetterForField(message, field, schemaFields.fieldTypeNames().get(field.getName())));
    }

    private void invokeSetterForField(Object message, Field field, String schemaTypeName) {
        String name = field.getName();
        Class<?> type = field.getType();
        try {
            Method setter = message.getClass().getMethod("set" + capitalize(name), type);
            setter.invoke(message, valueFor(field.getGenericType(), new FieldPath(name), schemaTypeName));
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to populate field " + field, e);
        }
    }

    private record SchemaFields(Map<String, String> fieldTypeNames) {}

    /**
     * Reads the {@code SCHEMAS} array every generated message/struct class declares and returns the
     * camelCase names of the fields present in the schema at {@code version}, mapped to their schema
     * wire type name - the same set write() consults internally, so filtering against it keeps
     * populate() from ever setting a field write() would then refuse to serialise at that version.
     */
    private static SchemaFields schemaFieldsAt(Class<?> clazz, short version) {
        try {
            Object[] schemas = (Object[]) clazz.getField("SCHEMAS").get(null);
            if (version < 0 || version >= schemas.length || schemas[version] == null) {
                return new SchemaFields(Map.of());
            }
            Object schema = schemas[version];
            Object[] boundFields = (Object[]) schema.getClass().getMethod("fields").invoke(schema);
            Map<String, String> fieldTypeNames = new HashMap<>();
            for (Object boundField : boundFields) {
                Object fieldDef = boundField.getClass().getField("def").get(boundField);
                addFieldNames(fieldDef, fieldTypeNames);
            }
            return new SchemaFields(fieldTypeNames);
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read schema fields for " + clazz + " at version " + version, e);
        }
    }

    /**
     * A top-level {@code Field} contributes its own name, except a {@code TaggedFieldsSection}, whose
     * name is the synthetic {@code "_tagged_fields"} marker rather than a real Java field - its actual
     * schema-declared fields (e.g. {@code replicaDirectoryId}) live one level down, keyed by tag, inside
     * its {@code TaggedFields} type.
     */
    private static void addFieldNames(Object fieldDef, Map<String, String> fieldTypeNames) throws ReflectiveOperationException {
        Object type = fieldDef.getClass().getField("type").get(fieldDef);
        if (isTaggedFieldsType(type.getClass())) {
            Map<?, ?> taggedFields = (Map<?, ?>) type.getClass().getMethod("fields").invoke(type);
            for (Object taggedFieldDef : taggedFields.values()) {
                addFieldNames(taggedFieldDef, fieldTypeNames);
            }
            return;
        }
        String snakeCaseName = (String) fieldDef.getClass().getField("name").get(fieldDef);
        String camelCaseName = toCamelCase(snakeCaseName);
        // Most Type constants (UINT16 among them) are anonymous DocumentedType subclasses, which aren't
        // themselves public - typeName() must be looked up via the nearest public class in the hierarchy
        // (DocumentedType, which declares it), or reflection's accessibility check on the anonymous
        // subclass rejects the call.
        String typeName = (String) nearestPublicClass(type.getClass()).getMethod("typeName").invoke(type);
        fieldTypeNames.put(camelCaseName, typeName);
    }

    private static boolean isTaggedFieldsType(Class<?> clazz) {
        return KroxyliciousSerdes.isApiType(clazz, TAGGED_FIELDS_RELATIVE_NAME) || KafkaSerdes.isApiType(clazz, TAGGED_FIELDS_RELATIVE_NAME);
    }

    private static Class<?> nearestPublicClass(Class<?> clazz) {
        Class<?> candidate = clazz;
        while (!Modifier.isPublic(candidate.getModifiers())) {
            candidate = candidate.getSuperclass();
        }
        return candidate;
    }

    private static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String toCamelCase(String snakeCaseName) {
        String[] words = snakeCaseName.split("_");
        StringBuilder camelCase = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            String word = words[i];
            camelCase.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return camelCase.toString();
    }

    private Object valueFor(Type type, FieldPath fieldPath, String schemaTypeName) {
        Optional<Object> leafValue = (type instanceof Class<?> clazz)
                ? leafValueSource.valueFor(clazz, fieldPath, schemaTypeName, random)
                : Optional.empty();
        return leafValue
                .or(() -> containerValueFor(type))
                .or(() -> structValueFor(type))
                .orElseThrow(() -> new UnsupportedOperationException("Don't know how to populate a field of type " + type));
    }

    /**
     * Types that hold a variable number of repeated elements: a generated {@code List<T>} field, or an
     * {@code ImplicitLinkedHashMultiCollection}-based collection. Returns {@link Optional#empty()} for any
     * other type, deferring to {@link #valueFor}'s remaining checks.
     */
    private Optional<Object> containerValueFor(Type type) {
        if (type instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() == List.class) {
            return Optional.of(listValueFor(parameterizedType.getActualTypeArguments()[0]));
        }
        if (type instanceof Class<?> clazz && isMultiCollectionType(clazz)) {
            return Optional.of(collectionValueFor(clazz));
        }
        return Optional.empty();
    }

    private Optional<Object> structValueFor(Type type) {
        if (type instanceof Class<?> structClass) {
            return Optional.of(newStruct(structClass));
        }
        return Optional.empty();
    }

    private List<Object> listValueFor(Type elementType) {
        List<Object> elements = new ArrayList<>();
        int size = 1 + random.nextInt(2);
        for (int i = 0; i < size; i++) {
            elements.add(valueFor(elementType, null, null));
        }
        return elements;
    }

    private static boolean isMultiCollectionType(Class<?> clazz) {
        for (Class<?> ancestor = clazz.getSuperclass(); ancestor != null; ancestor = ancestor.getSuperclass()) {
            if (KroxyliciousSerdes.isApiType(ancestor, MULTI_COLLECTION_RELATIVE_NAME) || KafkaSerdes.isApiType(ancestor, MULTI_COLLECTION_RELATIVE_NAME)) {
                return true;
            }
        }
        return false;
    }

    private Object collectionValueFor(Class<?> collectionClass) {
        Type elementType = ((ParameterizedType) collectionClass.getGenericSuperclass()).getActualTypeArguments()[0];
        try {
            Object collection = collectionClass.getDeclaredConstructor().newInstance();
            Method add = findSingleArgAddMethod(collectionClass);
            int size = 1 + random.nextInt(2);
            for (int i = 0; i < size; i++) {
                add.invoke(collection, valueFor(elementType, null, null));
            }
            return collection;
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to populate collection " + collectionClass, e);
        }
    }

    private static Method findSingleArgAddMethod(Class<?> collectionClass) {
        for (Method method : collectionClass.getMethods()) {
            if (method.getName().equals("add") && method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new IllegalStateException("No single-argument add(...) method found on " + collectionClass);
    }

    private Object newStruct(Class<?> structClass) {
        try {
            Object instance = structClass.getDeclaredConstructor().newInstance();
            populate(instance);
            return instance;
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate nested struct " + structClass, e);
        }
    }
}
