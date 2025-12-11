# API Reference

Generated code API and patterns.

---

## Generated Classes

For each `@Fake` annotated interface, Fakt generates three components:

### Implementation Class

```kotlin
class Fake{Interface}Impl : {Interface} {
    // StateFlow call counters
    val {method}CallCount: StateFlow<Int>

    // Override interface members
    override fun {method}({params}): {return} = {method}Behavior({params})

    // Internal configuration methods
    internal fun configure{Method}(behavior: ({params}) -> {return})
}
```

### Factory Function

```kotlin
fun fake{Interface}(
    configure: Fake{Interface}Config.() -> Unit = {}
): Fake{Interface}Impl
```

### Configuration DSL

```kotlin
class Fake{Interface}Config(private val fake: Fake{Interface}Impl) {
    fun {method}(behavior: ({params}) -> {return})
}
```

---

## Naming Conventions

| Element                | Pattern                      | Example                  |
|------------------------|------------------------------|--------------------------|
| Implementation class   | `Fake{Interface}Impl`        | `FakeAnalyticsImpl`      |
| Factory function       | `fake{Interface}`            | `fakeAnalytics`          |
| Configuration DSL      | `Fake{Interface}Config`      | `FakeAnalyticsConfig`    |
| Call counter           | `{method}CallCount`          | `trackCallCount`         |
| Configuration method   | `{method}`                   | `track { }`              |

---

## Package Structure

Generated fakes are in the same package as the annotated interface:

```
com.example.services.Analytics (@Fake)
→ com.example.services.FakeAnalyticsImpl
→ com.example.services.fakeAnalytics()
→ com.example.services.FakeAnalyticsConfig
```

---

## Generated Code Location

| Source Set          | Generated Output                                |
|---------------------|-------------------------------------------------|
| `commonTest/`       | `build/generated/fakt/commonTest/kotlin/`       |
| `jvmTest/`          | `build/generated/fakt/jvmTest/kotlin/`          |
| `iosTest/`          | `build/generated/fakt/iosTest/kotlin/`          |

---

## Next Steps

- [Configuration](plugin-configuration.md) - Plugin options
- [Compatibility](platform-support.md) - Kotlin versions
- [Limitations](known-issues.md) - Known issues
