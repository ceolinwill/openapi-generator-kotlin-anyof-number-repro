# Kotlin client: a string-or-number `anyOf`/`oneOf` doesn't compile with kotlinx.serialization

Reported in https://github.com/OpenAPITools/openapi-generator/issues/25452.

With the `kotlin` generator, `serializationLibrary=kotlinx_serialization` and `generateOneOfAnyOfWrappers=true`, a union with a number branch generates a wrapper whose `serialize()` doesn't compile. [#25472](https://github.com/OpenAPITools/openapi-generator/pull/25472) fixed an unformatted `type: number` (`BigDecimal`), but not the same union with `number` mapped to `kotlin.Double` or `integer` mapped to `kotlin.Long` (`--type-mappings=integer=kotlin.Long,number=kotlin.Double`).

## Reproduction

Requires JDK 17 or newer. The spec is [`openapi.yaml`](openapi.yaml): an `anyOf` and a `oneOf` of `string` and `number`, and the same two of `string` and `integer`. The build runs the `7.27.0-20261030.091743-151` CLI snapshot (published after #25472 merged; SHA256 `aa5f6609f715edff1a8701ff8ad1eb608fb6eed9973efe339e3b15fb7659591d`) with those type mappings, then compiles the client.

```sh
./gradlew compileKotlin
```

```text
e: build/generated/src/main/kotlin/repro/models/AnyOfInteger.kt:67:64 Argument type mismatch: actual type is 'Long', but 'Int' was expected.
e: build/generated/src/main/kotlin/repro/models/AnyOfValue.kt:65:9 'when' expression must be exhaustive. Add the 'is DoubleValue' branch or an 'else' branch.
e: build/generated/src/main/kotlin/repro/models/OneOfInteger.kt:66:64 Argument type mismatch: actual type is 'Long', but 'Int' was expected.
e: build/generated/src/main/kotlin/repro/models/OneOfValue.kt:64:9 'when' expression must be exhaustive. Add the 'is DoubleValue' branch or an 'else' branch.
BUILD FAILED
```

The generated code:

```kotlin
when (value) {
    is AnyOfValue.StringValue -> jsonEncoder.encodeString(value.value)
}

when (value) {
    is AnyOfInteger.StringValue -> jsonEncoder.encodeString(value.value)
    is AnyOfInteger.LongValue -> jsonEncoder.encodeInt(value.value)
}
```

On 7.26.0, the unmapped `AnyOfValue` and `OneOfValue` failed with `Unresolved reference 'serializer'` (`java.math.BigDecimal.serializer()`); #25472 fixed that.

## Expected

The client compiles, and every number branch is written as a JSON number.

## Actual

In `kotlin-client/anyof_class.mustache` and `oneof_class.mustache`, a primitive number branch is encoded only under `{{#isNumber}}{{#isDouble}}` or `{{#isFloat}}`. A `kotlin.Double` mapped from an unformatted `number` has `isNumber` but neither flag, so `serialize()` has no case for `DoubleValue`. An unformatted `integer` has `isInteger`, so it's written with `encodeInt`, which doesn't accept the mapped `kotlin.Long`. Without any mapping, `format: double` and `format: float` miss their case the same way: they set `isDouble`/`isFloat` but not `isNumber`, as the #25472 description notes.

Writing every numeric primitive branch as `jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))` (one `{{#isNumeric}}` case in place of the `isInteger`/`isLong`/`isNumber` cases) compiles and round-trips `2` for `integer`, `int64`, `number`, `double` and `float` branches, with and without these mappings.

## Environment

- OpenAPI Generator `7.27.0-20261030.091743-151` (snapshot CLI, run from Gradle)
- Kotlin 2.4.21, kotlinx.serialization 1.12.0, Gradle 9.8.2, JDK 25
- macOS 27
