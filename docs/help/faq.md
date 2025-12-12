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

**SNAPSHOT** signals real-world validation in progress, not "broken." The API is functionally complete and production-ready. We prioritize honesty over marketing—1.0.0 will follow community feedback and battle-testing.

---

## Comparison with Other Tools

### Why not use MockK or Mockito?

MockK and Mockito are **runtime mocking frameworks** using reflection (JVM/Android only). Fakt generates fakes at **compile-time** using Kotlin IR:

- ✅ Works on ALL KMP targets (iOS, Native, JS, WASM) without reflection
- ✅ Zero runtime cost, compile-time type safety
- ✅ Generated code you can read and debug

**Use MockK/Mockito when:** You need dynamic mocking or are on JVM-only projects.

**Use Fakt when:** Building Kotlin Multiplatform projects or want zero-runtime-cost test doubles.

See [Why Fakt](../get-started/why-fakt.md) for detailed comparison.

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

**Yes**. Class-level, method-level, generic constraints, and variance are all supported. See [Usage Guide: Generics](../user-guide/usage.md#generics) for detailed examples.

### Does Fakt support suspend functions?

**Yes**. Suspend functions preserve coroutine semantics. See [Usage Guide: Suspend Functions](../user-guide/usage.md#suspend-functions) for details.

### Does Fakt support properties (val/var)?

**Yes**. Both read-only (`val`) and mutable (`var`) properties with call tracking. See [Usage Guide: Properties](../user-guide/usage.md#properties) for examples.

### Can I fake data classes or sealed classes?

**No**. Fakt only generates fakes for interfaces, abstract classes, and open classes. Data/sealed classes work fine as parameter/return types.

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

See [Performance Guide](../user-guide/performance.md) for detailed benchmarks and telemetry configuration.

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
- [Multi-Module](../user-guide/multi-module.md) - Architecture, setup, and implementation details
- [kmp-multi-module sample](https://github.com/rsicarelli/fakt/tree/main/samples/kmp-multi-module) - Working example

!!! warning "Experimental Feature"
    Multi-module support is marked `@ExperimentalFaktMultiModule`. It works but the API may change before 1.0.

---

## Troubleshooting

For common issues and solutions, see the [Troubleshooting Guide](troubleshooting.md):

- [Generated fakes not appearing in IDE](troubleshooting.md#generated-fakes-not-appearing)
- [Unresolved reference: fakeXxx](troubleshooting.md#unresolved-reference-fakexxx)
- [Compilation errors](troubleshooting.md#compilation-fails-with-irtypealiassymbol-not-found)
- [Multi-module issues](troubleshooting.md#multi-module-issues)

---

## Contributing & Reporting Issues

### How can I contribute to Fakt?

Contributions are welcome! Please:

1. Follow **GIVEN-WHEN-THEN** testing standard
2. Ensure all generated code compiles without errors
3. Test both single-platform and KMP scenarios
4. Run `make format` before committing

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
