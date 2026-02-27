# Why Fakt?

Kotlin testing has a problem that gets worse the more successful your project becomes.

Manual test fakes don't scale—each interface requires 60-80 lines of boilerplate that silently drifts from reality during refactoring. Runtime mocking frameworks (MockK, Mockito) solve the boilerplate but introduce severe performance penalties and don't work on Kotlin/Native or WebAssembly. KSP-based tools promised compile-time generation, but Kotlin 2.0 broke them all.

Fakt is a compiler plugin that generates production-quality fakes through deep integration with Kotlin's FIR and IR compilation phases—the same extension points used by [Metro](https://github.com/ZacSweers/metro), a production DI framework from Zac Sweers.

---

## What Fakt Does

Fakt reduces fake boilerplate to an annotation:

```kotlin
@Fake
interface AnalyticsService {
    fun track(event: String)
    suspend fun flush(): Result<Unit>
}
```

At compile time, Fakt generates a complete fake implementation. You use it through a type-safe factory:

```kotlin
val fake = fakeAnalyticsService {
    track { event -> println("Tracked: $event") }
    flush { Result.success(Unit) }
}

// Use in tests
fake.track("user_signup")
fake.flush()

// Verify interactions (thread-safe StateFlow)
assertEquals(1, fake.trackCalls.value.size)
assertEquals(1, fake.flushCalls.value.size)
```

---

## The Testing Problem

### Manual Fakes Don't Scale

Consider a simple interface:

```kotlin
interface AnalyticsService {
    fun track(event: String)
    suspend fun flush(): Result<Unit>
}
```

A proper, production-quality fake requires ~60-80 lines of boilerplate:

```kotlin
// Typical handwritten fake — mutable, error-prone, tedious
class FakeAnalyticsService : AnalyticsService {
    // Behavior configuration
    private var trackBehavior: ((String) -> Unit)? = null
    private var flushBehavior: (suspend () -> Result<Unit>)? = null

    // Call history (non-thread-safe!)
    private var _trackCalls = mutableListOf<Unit>()
    val trackCalls: List<Unit> get() = _trackCalls

    private var _flushCalls = mutableListOf<Unit>()
    val flushCalls: List<Unit> get() = _flushCalls

    // Interface implementation
    override fun track(event: String) {
        _trackCalls.add(Unit)
        trackBehavior?.invoke(event) ?: Unit
    }

    override suspend fun flush(): Result<Unit> {
        _flushCalls.add(Unit)
        return flushBehavior?.invoke() ?: Result.success(Unit)
    }

    // Configuration methods
    fun configureTrack(behavior: (String) -> Unit) {
        trackBehavior = behavior
    }

    fun configureFlush(behavior: suspend () -> Result<Unit>) {
        flushBehavior = behavior
    }
}
```

!!! note "Fakt does it differently"
    By default, Fakt generates **immutable** fakes where behavior is set via constructor parameters and cannot change after construction. This eliminates shared mutable state and makes fakes thread-safe by design.

    For advanced integration test scenarios, Fakt also supports [opt-in mutable fakes](user-guide/immutable-vs-mutable.md) with `@Fake(mutability = MutabilityMode.MUTABLE)`. Unlike the handwritten pattern above, mutable Fakt fakes are still compiler-generated (never drift from the interface), use a type-safe `configure {}` DSL (not raw setters), and compose cleanly with call history.

The problems: call history uses mutable variables that break under concurrent tests. N methods require ~30N lines. Interface changes don't break unused fakes—they silently drift. For 50 interfaces, this means thousands of lines of brittle boilerplate.

### The Mock Tax

Runtime mocking frameworks solve the boilerplate but pay a different cost. Kotlin classes are `final` by default, so MockK and Mockito resort to bytecode instrumentation. Independent benchmarks[^1] quantify the penalty:

| Mocking Pattern | Framework | Comparison | Verified Penalty |
|-----------------|-----------|------------|------------------|
| `mockkObject` (Singletons) | MockK | vs. Dependency Injection | **1,391x slower** |
| `mockkStatic` (Top-level functions) | MockK | vs. Interface-based DI | **146x slower** |
| `verify { ... }` (Interaction verification) | MockK | vs. State-based testing | **47x slower** |
| `relaxed` mocks (Unstubbed calls) | MockK | vs. Strict mocks | **3.7x slower** |
| `mock-maker-inline` | Mockito | vs. `all-open` plugin | **2.7-3x slower**[^2][^3] |

A production test suite with 2,668 tests experienced a 2.7x slowdown (7.3s → 20.0s) when using `mock-maker-inline`[^3]. For large projects, the mock tax accumulates to 40% slower test suites[^1].

### The KMP Dead End

Runtime mocking relies on JVM-specific features: reflection, bytecode instrumentation, dynamic proxies. Kotlin/Native and Kotlin/Wasm compile to machine code. There is no JVM. MockK and Mockito cannot run in `commonTest` source sets targeting Native or Wasm[^6][^7].

The community attempted KSP-based solutions, but Kotlin 2.0's K2 compiler broke them. The StreetComplete app (10,000+ tests) was forced to migrate mid-project[^8].

---

## Why Compiler Plugins Work

KSP-based tools (Mockative, MocKMP) operated at the symbol level—after type resolution, with limited access to the type system. When K2 landed, they broke. Compiler plugins operate during compilation, with full access to FIR and IR. They survive Kotlin version updates.

| Aspect | KSP | Compiler Plugin |
|--------|-----|-----------------|
| Access | After type resolution | During compilation |
| Type System | Read-only symbols | Full manipulation |
| K2 Stability | Broken | Stable |

Fakt uses a two-phase FIR → IR architecture:

```
┌──────────────────────────────────────────────────────┐
│  PHASE 1: FIR (Frontend IR)                         │
│  • Detects @Fake annotations                        │
│  • Validates interface structure                    │
│  • Full type system access                          │
└──────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────┐
│  PHASE 2: IR (Intermediate Representation)          │
│  • Analyzes interface methods and properties        │
│  • Generates readable .kt source files              │
│  • Thread-safe StateFlow call history               │
└──────────────────────────────────────────────────────┘
```

This is the same pattern used by [Metro](https://github.com/ZacSweers/metro), Zac Sweers' DI compiler plugin. Metro's architecture has proven stable across Kotlin 1.9, 2.0, and 2.1.

---

## Why Fakes Over Mocks

Beyond performance, fakes represent a different testing philosophy. Martin Fowler's "Mocks Aren't Stubs"[^10] describes two schools: state-based testing (verify outcomes) and interaction-based testing (verify method calls).

The problem with interaction-based tests: they couple to implementation details[^11]. Refactor a method signature without changing behavior, and mock-based tests break. Google's Testing Blog defines resilience as a critical test quality—"a test shouldn't fail if the code under test isn't defective"[^12]. Mock-based tests often violate this.

Google's "Now in Android" app makes this explicit[^14]:

> **"Don't use mocking frameworks. Instead, use fakes."**

The goal: "less brittle tests that may exercise more production code, instead of just verifying specific calls against mocks"[^15].

Kotlin's async testing stack—`runTest`, `TestDispatcher`, Turbine[^17]—is inherently state-based. Turbine's `awaitItem()` verifies emitted values, not method calls. The natural data source for this stack is a fake with `MutableStateFlow` backing. Fakt automates this pattern.

---

## Practical Guidance

### Fakes vs. Mocks: Quick Comparison

| Feature | MockK/Mockito | Fakt |
|---------|---------------|------|
| **KMP Support** | Limited (JVM only) | Universal (all targets) |
| **Compile-time Safety** | ❌ | ✅ |
| **Runtime Overhead** | Heavy (reflection) | Zero |
| **Type Safety** | Partial (`any()` matchers) | Complete |
| **Learning Curve** | Steep (complex DSL) | Gentle (typed functions) |
| **Call History** | Manual (`verify { }`) | Built-in (StateFlow) |
| **Thread Safety** | Not guaranteed | StateFlow-based |
| **Debuggability** | Reflection (opaque) | Generated `.kt` files |

---

### Choosing the Right Tool

Fakt and mocking libraries solve overlapping but distinct problems. Choosing between them depends on your constraints and testing needs.

**Fakt works best when:**

- You've already chosen fakes over mocks. If you understand the state-based testing philosophy and prefer testing outcomes over verifying interactions, Fakt automates what you'd otherwise write by hand.

- You only use mocks for convenience. Many developers reach for mocking frameworks not for `verify { }` features, but simply because writing manual fakes is tedious. Fakt gives you the factory convenience without the mock overhead—generated fakes are plain Kotlin classes.

- You're building for Kotlin Multiplatform. Fakt generates plain Kotlin that compiles on JVM, Native, and WebAssembly—no reflection required. This applies to any source set, not just `commonTest`.

- You value exercising production code in tests. Fakt-generated fakes are real implementations your tests compile against, catching interface drift at build time rather than runtime.

- Tests run concurrently. Fakt tracks call history with StateFlow, which is thread-safe by design. Manual fakes with `var count = 0` break under parallel execution.

**Mocking libraries (Mokkery, MockK) work best when:**

- You need strict call ordering verification. Verifying that `validate()` was called before `charge()` before `ship()` in exact sequence requires `verify(exhaustiveOrder)`. Fakt tracks call history, not call order.

- You need spy behavior. Partial mocking of real implementations—calling real methods while intercepting others—is something only mocking frameworks can do. Fakt generates new implementations, it doesn't wrap existing ones.

- You're mocking third-party classes without interfaces. If a library exposes final classes with no interface to program against, mocking frameworks can instrument the bytecode. Fakt requires an interface to annotate.

**Neither tool replaces contract testing.** For third-party HTTP APIs, use WireMock or Pact. Hand-written fakes for external services drift from reality without contract validation—they create dangerous illusions of fidelity that break in production.

---

## Get Started

- [Getting Started](get-started/index.md) - Install Fakt and create your first fake
- [Features](get-started/features.md) - Complete feature reference
- [Usage Guide](user-guide/usage.md) - Common patterns and examples
- [Migration from Mocks](user-guide/migration-from-mocks.md) - Moving from MockK/Mockito

---

## Works Cited

[^1]: Benchmarking Mockk — Avoid these patterns for fast unit tests. Kevin Block. [https://medium.com/@_kevinb/benchmarking-mockk-avoid-these-patterns-for-fast-unit-tests-220fc225da55](https://medium.com/@_kevinb/benchmarking-mockk-avoid-these-patterns-for-fast-unit-tests-220fc225da55)

[^2]: Effective migration to Kotlin on Android. Aris Papadopoulos. [https://medium.com/android-news/effective-migration-to-kotlin-on-android-cfb92bfaa49b](https://medium.com/android-news/effective-migration-to-kotlin-on-android-cfb92bfaa49b)

[^3]: Mocking Kotlin classes with Mockito — the fast way. Brais Gabín Moreira. [https://medium.com/21buttons-tech/mocking-kotlin-classes-with-mockito-the-fast-way-631824edd5ba](https://medium.com/21buttons-tech/mocking-kotlin-classes-with-mockito-the-fast-way-631824edd5ba)

[^4]: Reflection | Kotlin Documentation. [https://kotlinlang.org/docs/reflection.html](https://kotlinlang.org/docs/reflection.html)

[^5]: Reflection? - Native - Kotlin Discussions. [https://discuss.kotlinlang.org/t/reflection/4054](https://discuss.kotlinlang.org/t/reflection/4054)

[^6]: Did someone try to use Mockk on KMM project. Kotlin Slack. [https://slack-chats.kotlinlang.org/t/10131532/did-someone-try-to-use-mockk-on-kmm-project](https://slack-chats.kotlinlang.org/t/10131532/did-someone-try-to-use-mockk-on-kmm-project)

[^7]: Mock common tests in kotlin using multiplatform. Stack Overflow. [https://stackoverflow.com/questions/65491916/mock-common-tests-in-kotlin-using-multiplatform](https://stackoverflow.com/questions/65491916/mock-common-tests-in-kotlin-using-multiplatform)

[^8]: Mocking in Kotlin Multiplatform: KSP vs Compiler Plugins. Martin Hristev. [https://medium.com/@mhristev/mocking-in-kotlin-multiplatform-ksp-vs-compiler-plugins-4424751b83d7](https://medium.com/@mhristev/mocking-in-kotlin-multiplatform-ksp-vs-compiler-plugins-4424751b83d7)

[^9]: MocKMP: a Mocking processor for Kotlin/Multiplatform. Salomon BRYS. [https://medium.com/kodein-koders/mockmp-a-mocking-processor-for-kotlin-multiplatform-51957c484fe5](https://medium.com/kodein-koders/mockmp-a-mocking-processor-for-kotlin-multiplatform-51957c484fe5)

[^10]: Mocks Aren't Stubs. Martin Fowler. [https://martinfowler.com/articles/mocksArentStubs.html](https://martinfowler.com/articles/mocksArentStubs.html)

[^11]: Unit Testing — Why must you mock me? Craig Walker. [https://medium.com/@walkercp/unit-testing-why-must-you-mock-me-69293508dd13](https://medium.com/@walkercp/unit-testing-why-must-you-mock-me-69293508dd13)

[^12]: Testing on the Toilet: Effective Testing. Google Testing Blog. [https://testing.googleblog.com/2014/05/testing-on-toilet-effective-testing.html](https://testing.googleblog.com/2014/05/testing-on-toilet-effective-testing.html)

[^13]: Trade-offs to consider when choosing to use Mocks vs Fakes. HackMD. [https://hackmd.io/@pierodibello/Trade-offs-to-consider-when-choosing-to-use-Mocks-vs-Fakes](https://hackmd.io/@pierodibello/Trade-offs-to-consider-when-choosing-to-use-Mocks-vs-Fakes)

[^14]: Testing strategy and how to test. Now in Android Wiki. [https://github.com/android/nowinandroid/wiki/Testing-strategy-and-how-to-test](https://github.com/android/nowinandroid/wiki/Testing-strategy-and-how-to-test)

[^15]: android/nowinandroid: A fully functional Android app built entirely with Kotlin and Jetpack Compose. GitHub. [https://github.com/android/nowinandroid](https://github.com/android/nowinandroid)

[^16]: Testing Kotlin coroutines on Android. Android Developers. [https://developer.android.com/kotlin/coroutines/test](https://developer.android.com/kotlin/coroutines/test)

[^17]: Flow testing with Turbine. Cash App Code Blog. [https://code.cash.app/flow-testing-with-turbine](https://code.cash.app/flow-testing-with-turbine)

[^18]: Why we should use wiremock instead of Mockito. Stack Overflow. [https://stackoverflow.com/questions/50726017/why-we-should-use-wiremock-instead-of-mockito](https://stackoverflow.com/questions/50726017/why-we-should-use-wiremock-instead-of-mockito)

[^19]: Stop Breaking My API: A Practical Guide to Contract Testing with Pact. Medium. [https://medium.com/@mohsenny/stop-breaking-my-api-a-practical-guide-to-contract-testing-with-pact-33858d113386](https://medium.com/@mohsenny/stop-breaking-my-api-a-practical-guide-to-contract-testing-with-pact-33858d113386)

[^20]: lupuuss/Mokkery: The mocking library for Kotlin Multiplatform. GitHub. [https://github.com/lupuuss/Mokkery](https://github.com/lupuuss/Mokkery)

[^21]: Kotlin 2.0.0 support · Issue #1 · lupuuss/Mokkery. GitHub. [https://github.com/lupuuss/Mokkery/issues/1](https://github.com/lupuuss/Mokkery/issues/1)

[^22]: Use multiplatform mocking library for tests · Issue #5420 · streetcomplete/StreetComplete. GitHub. [https://github.com/streetcomplete/StreetComplete/issues/5420](https://github.com/streetcomplete/StreetComplete/issues/5420)

[^23]: Kotlin 2.2.0 support · Issue #83 · lupuuss/Mokkery. GitHub. [https://github.com/lupuuss/Mokkery/issues/83](https://github.com/lupuuss/Mokkery/issues/83)

[^24]: Mocking | Mokkery. [https://mokkery.dev/docs/Guides/Mocking/](https://mokkery.dev/docs/Guides/Mocking/)

[^25]: A to Z of Testing in Kotlin Multiplatform. Kinto Technologies. [https://blog.kinto-technologies.com/posts/2024-12-24-tests-in-kmp/](https://blog.kinto-technologies.com/posts/2024-12-24-tests-in-kmp/)

[^26]: Limitations | Mokkery. [https://mokkery.dev/docs/Limitations/](https://mokkery.dev/docs/Limitations/)
