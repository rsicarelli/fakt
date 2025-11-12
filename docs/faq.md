# Frequently Asked Questions

Common questions about Fakt, answered with honesty and technical context.

---

## Stability & Safety

### Is Fakt safe to use in production testing?

**Short answer**: Yes. Fakt is functionally stable and ready for production use in test suites.

**Longer answer**: Fakt follows a two-phase FIR → IR compilation architecture inspired by production compiler plugins like [Metro](https://github.com/ZacSweers/metro). While the Kotlin compiler plugin API is not a stable API (marked `@UnsafeApi`), Fakt:

- Generates code at compile-time with **zero runtime dependencies**
- Has been tested across Kotlin 2.2.20+ and all KMP targets
- Uses forward compatibility patterns (N+.2 version support)
- Is versioned as **1.0.0-SNAPSHOT** to signal pre-1.0 status

Generated fakes are production-quality code that compiles to native binaries without reflection.

!!! warning "Compiler Plugin API Stability"
    The Kotlin compiler plugin API can change between Kotlin versions. Fakt is tested against each Kotlin release and updated as needed. Pin your Kotlin version in production CI/CD.

---

### Why "1.0.0-SNAPSHOT" instead of "1.0.0"?

We're following semantic versioning strictly:

- **SNAPSHOT** signals that we're still validating real-world usage patterns
- **1.0.0** will be released after community feedback and battle-testing
- The compiler plugin API is functionally complete and stable for production testing

We prioritize **honesty over marketing**. SNAPSHOT doesn't mean "broken"—it means "we're listening to feedback before declaring 1.0."

---

## Comparison with Other Tools

### Why not use MockK or Mockito?

MockK and Mockito are **runtime mocking frameworks** that use reflection. This limits them to JVM/Android targets and adds runtime overhead.

Fakt generates fakes at **compile-time** using Kotlin IR, which means:

- ✅ Works on **ALL KMP targets** (iOS, Native, JS, WASM) without reflection
- ✅ **Zero runtime cost** (no reflection proxy overhead)
- ✅ **Compile-time type safety** (refactoring breaks tests immediately)
- ✅ **Generated code you can read and debug**

**Use MockK/Mockito when:**
- You need dynamic mocking (testing framework internals)
- You're mocking concrete classes with complex inheritance
- You're on JVM-only projects and don't need KMP

**Use Fakt when:**
- You're building Kotlin Multiplatform projects
- You want type-safe, cross-platform test doubles
- You prefer explicit, readable fake implementations
- You want zero runtime cost and no reflection

---

### How does Fakt compare to hand-written fakes?

Fakt generates the **same code you'd write manually**, but faster and without mistakes:

| Aspect                  | Hand-Written Fakes      | Fakt Fakes             |
|-------------------------|-------------------------|------------------------|
| **Boilerplate**         | ~50 lines per interface | Auto-generated         |
| **Call tracking**       | Manual (`var count = 0`)| StateFlow (thread-safe)|
| **Refactoring safety**  | Breaks silently         | Breaks at compile-time |
| **Maintenance**         | Scales with codebase    | Zero maintenance       |
| **Customization**       | Full control            | DSL configuration      |

Fakt doesn't replace hand-written fakes for complex scenarios (stateful mocks, partial implementations). It **eliminates boilerplate** for the 80% case.

---

## Feature Support

### Does Fakt support generics?

**Yes**. Fakt fully supports:

- ✅ Class-level generics (`interface Repository<T>`)
- ✅ Method-level generics (`fun <T> transform(value: T): T`)
- ✅ Generic constraints (`<T : Comparable<T>>`)
- ✅ Variance (`out T`, `in T`)

Generated fakes preserve type parameters and use smart defaults:

```kotlin
@Fake
interface Repository<T> {
    fun save(item: T): Result<Unit>
    fun <R> transform(item: T, mapper: (T) -> R): R
}

// Generated fake works as expected
val fake = fakeRepository<User> {
    save { item -> Result.success(Unit) }
    transform { item, mapper -> mapper(item) }
}
```

**Current limitation**: Some complex nested generics with multiple constraints may require manual implementation. We track edge cases in [GitHub issues](https://github.com/rsicarelli/fakt/issues).

---

### Does Fakt support suspend functions?

**Yes**. Suspend functions are fully supported:

```kotlin
@Fake
interface ApiClient {
    suspend fun fetchData(id: String): Result<Data>
}

val fake = fakeApiClient {
    fetchData { id ->
        delay(100) // Suspends correctly
        Result.success(Data(id))
    }
}
```

Fakt preserves coroutine semantics—no weird `runBlocking` wrappers needed.

---

### Does Fakt support properties (val/var)?

**Yes**. Both read-only (`val`) and mutable (`var`) properties are supported:

```kotlin
@Fake
interface Settings {
    val theme: String
    var fontSize: Int
}

val fake = fakeSettings {
    theme { "dark" }
    fontSize { 14 }
}

assertEquals("dark", fake.theme)
assertEquals(1, fake.themeCallCount.value)

fake.fontSize = 16
assertEquals(1, fake.setFontSizeCallCount.value)
```

Mutable properties generate **both** getter and setter call counters.

---

### Can I fake data classes or sealed classes?

**No**. Fakt only generates fakes for:

- ✅ Interfaces
- ✅ Abstract classes
- ✅ Open classes (overridable members only)

Data classes and sealed classes work fine as **parameter/return types**, but you can't put `@Fake` on them directly.

**Why**: Data classes have fixed implementations (compiler-generated). Faking them would be misleading—use builders or copy() instead.

---

## Performance

### What about performance impact on build times?

Fakt uses **intelligent caching** across KMP targets. First target compilation typically adds ~40ms for 100+ interfaces. Subsequent targets (JVM, iOS, Android) hit cache and add ~1ms each.

**For large projects** (1000+ interfaces), expect:

- **First compilation**: ~200-400ms
- **Cached targets**: near-zero overhead (~1-2ms each)

Example from a real KMP project:

```
DISCOVERY: 1ms (100 interfaces, 21 classes)
GENERATION: 39ms (121 new fakes, avg 333µs/fake)
TOTAL: 40ms (iosArm64 first compilation)

compileKotlinJvm:     1ms (121 from cache)
compileKotlinAndroid: 1ms (121 from cache)
```

See [Performance Guide](guides/performance.md) for detailed benchmarks and telemetry configuration.

---

## Multi-Module Projects

### Does Fakt work with multi-module projects?

**Yes, with experimental multi-module support**:

```kotlin
// Producer module: :core:analytics/build.gradle.kts
@Fake
interface Analytics

// Collector module: :core:analytics-fakes/build.gradle.kts
plugins {
    id("com.rsicarelli.fakt")
}

fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}

// Consumer module: :app/build.gradle.kts
dependencies {
    commonTest {
        implementation(projects.core.analyticsFakes)
    }
}
```

**For comprehensive documentation**, see:
- [Multi-Module Overview](multi-module/index.md) - Architecture and when to use
- [Getting Started](multi-module/getting-started.md) - Step-by-step setup guide
- [kmp-multi-module sample](https://github.com/rsicarelli/fakt/tree/main/samples/kmp-multi-module) - Working example

!!! warning "Experimental Feature"
    Multi-module support is marked `@ExperimentalFaktMultiModule`. It works but the API may change before 1.0.

---

## Troubleshooting

### Generated fakes aren't appearing in my IDE

**Solutions:**

1. **Rebuild the project**: `./gradlew clean build`
2. **Invalidate IDE caches**: File → Invalidate Caches → Invalidate and Restart
3. **Check build directory**: Fakes are in `build/generated/fakt/commonTest/kotlin/`
4. **Verify Gradle sync**: Ensure Gradle sync completed successfully

---

### I'm getting "Unresolved reference: fakeXxx"

**Common causes:**

1. **Missing build step**: Run `./gradlew build` first
2. **Wrong source set**: Import from test code (`src/commonTest/`), not main
3. **Package mismatch**: Generated fakes are in the same package as the interface
4. **Gradle sync issue**: Re-sync Gradle in your IDE

---

### Compilation fails with "IrTypeAliasSymbol not found"

This usually means:

1. **Kotlin version mismatch**: Ensure you're on Kotlin 2.2.20+
2. **Fakt version incompatibility**: Update Fakt to match your Kotlin version

See [Compatibility](reference/compatibility.md) for version requirements.

---

## Contributing & Reporting Issues

### How can I contribute to Fakt?

Contributions are welcome! Please:

1. Follow **GIVEN-WHEN-THEN** testing standard
2. Ensure all generated code compiles without errors
3. Test both single-platform and KMP scenarios
4. Run `make format` before committing

See [Contributing Guide](contributing.md) for development workflows.

---

### Where do I report bugs?

Report issues on [GitHub Issues](https://github.com/rsicarelli/fakt/issues). Please include:

- Kotlin version
- Fakt version
- KMP targets (if applicable)
- Minimal reproduction (interface + error message)
- Full compilation logs (`./gradlew build --info`)

---

## Still Have Questions?

- [Troubleshooting](troubleshooting.md) - Common issues and solutions
- [GitHub Discussions](https://github.com/rsicarelli/fakt/discussions) - Ask the community
- [GitHub Issues](https://github.com/rsicarelli/fakt/issues) - Report bugs
