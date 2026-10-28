# Kotlin client: a string-or-number `anyOf`/`oneOf` doesn't compile with kotlinx.serialization

With the `kotlin` generator, `serializationLibrary=kotlinx_serialization` and `generateOneOfAnyOfWrappers=true`, a union with a `type: number` branch that has no `format` generates a wrapper whose `serialize()` doesn't compile. The branch becomes `BigDecimalValue(java.math.BigDecimal)`, and the serializer calls `java.math.BigDecimal.serializer()`, which doesn't exist.

## Reproduction

Requires JDK 17 or newer. The spec is [`openapi.yaml`](openapi.yaml): one `anyOf` and one `oneOf` of `string` and `number`. The build generates the client with the OpenAPI Generator Gradle plugin 7.26.0 and compiles it.

```sh
./gradlew compileKotlin
```

```text
e: build/generated/src/main/kotlin/repro/models/AnyOfValue.kt:67:103 Unresolved reference 'serializer'.
e: build/generated/src/main/kotlin/repro/models/OneOfValue.kt:66:103 Unresolved reference 'serializer'.
BUILD FAILED
```

The generated code:

```kotlin
when (value) {
    is AnyOfValue.StringValue -> jsonEncoder.encodeString(value.value)
    is AnyOfValue.BigDecimalValue -> jsonEncoder.encodeSerializableValue(java.math.BigDecimal.serializer(), value.value)
}
```

## Expected

The client compiles, and the number branch is written as a JSON number.

## Actual

`kotlin-client/anyof_class.mustache` and `oneof_class.mustache` write the number branch only under `{{#isNumber}}{{#isDouble}}` or `{{#isFloat}}`. An unformatted `number` is neither, so it falls through to the `{{^isPrimitiveType}}` branch, `encodeSerializableValue({{{dataType}}}.serializer(), …)`, and `BigDecimal` has no `serializer()`.

Mapping `number` to `kotlin.Double` (`typeMappings`) doesn't help: the branch becomes `DoubleValue`, but `serialize()` then has no case for it at all ("'when' expression must be exhaustive").

## Environment

- OpenAPI Generator 7.26.0 (the templates are unchanged on master)
- Kotlin 2.4.21, kotlinx.serialization 1.12.0, Gradle 9.8.2, JDK 25
- macOS 27
