# Why Fakt?

The story behind building a compile-time fake generator for Kotlin testing.

---

## Introduction

Kotlin testing has a problem that gets worse the more successful your project becomes.

Writing test fakes manually doesn't scale. Each interface requires 60 to 80 lines of boilerplate—tracking state, configuring behavior, implementing methods. When you have five interfaces, this is tedious. When you have fifty, it's unsustainable. When those interfaces change during refactoring, your fakes silently drift from reality until tests fail in production.

The industry's answer has been runtime mocking frameworks. MockK and Mockito promise to eliminate boilerplate through reflection and bytecode manipulation. For Java, this works. For Kotlin, it fails in two fundamental ways.

First, Kotlin's design philosophy conflicts with how mocking frameworks operate. Java classes are open by default—perfect for runtime subclassing. Kotlin classes are final by default—a deliberate choice for safety and performance. MockK works around this through bytecode instrumentation, paying a severe cost. Independent benchmarks show MockK is 1,391 times slower when mocking Kotlin singletons compared to dependency injection. For large test suites, this "mock tax" accumulates to 40% slower execution.

Second, Kotlin Multiplatform has no viable mocking solution. MockK and Mockito rely on JVM-specific features—reflection, bytecode manipulation, dynamic proxies. Kotlin/Native compiles to machine code. There is no JVM. Reflection barely exists. The entire foundation of runtime mocking disappears on iOS, Desktop, and WebAssembly targets.

The Kotlin community attempted to solve this through KSP-based code generation tools. Mockative, MocKMP, and early versions of Mokkery used Kotlin Symbol Processing to generate mocks at compile time. Kotlin 2.0 broke them all. The K2 compiler failed to handle metadata tasks properly in commonTest source sets, forcing real-world migrations mid-project. The StreetComplete Android app—10,000 tests—had to abandon Mockative when K2 landed.

This is the landscape that motivated Fakt. Manual fakes don't scale. Runtime mocking is slow and KMP-incompatible. KSP-based solutions proved fragile. The Kotlin ecosystem needed a different approach.

Fakt is a compiler plugin that generates production-quality fakes through deep integration with Kotlin's compilation pipeline. It operates at the FIR and IR levels—the same extension points used by [Metro](https://github.com/ZacSweers/metro), a production dependency injection compiler plugin used by Google, Netflix, and Cash App. This isn't a code generator that reads symbols after compilation. It's part of the compiler itself, with full access to type information and the ability to survive Kotlin version updates.

This document explains the technical and philosophical reasons why compile-time fake generation is the only sustainable path forward for Kotlin testing.

## The Problem: Manual Fakes Are Tedious, Mocks Are Costly

Modern Kotlin development faces a testing dilemma. Writing test doubles (fakes, mocks, stubs) manually is time-consuming and error-prone. Runtime mocking frameworks solve the boilerplate problem but introduce severe performance penalties and architectural limitations. Fakt was created to address both challenges.

### The Manual Fake Burden

Consider a simple interface requiring a test double:

```kotlin
interface AnalyticsService {
    fun track(event: String)
    suspend fun flush(): Result<Unit>
}
```

A proper, production-quality fake requires ~60-80 lines of boilerplate:

```kotlin
class FakeAnalyticsService : AnalyticsService {
    // Behavior configuration
    private var trackBehavior: ((String) -> Unit)? = null
    private var flushBehavior: (suspend () -> Result<Unit>)? = null

    // Call tracking (non-thread-safe!)
    private var _trackCallCount = 0
    val trackCallCount: Int get() = _trackCallCount

    private var _flushCallCount = 0
    val flushCallCount: Int get() = _flushCallCount

    // Interface implementation
    override fun track(event: String) {
        _trackCallCount++
        trackBehavior?.invoke(event) ?: Unit
    }

    override suspend fun flush(): Result<Unit> {
        _flushCallCount++
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

The problems compound quickly. Call tracking uses mutable variables that break under concurrent tests. Maintenance burden scales linearly—N methods require roughly 30N lines of boilerplate. When interface signatures change during refactoring, unused fakes don't trigger compile errors. They silently drift from reality. Copy-paste errors accumulate across dozens of fakes.

For a codebase with 50 interfaces requiring fakes, this represents thousands of lines of brittle, repetitive boilerplate.

---

## The Runtime Mocking Crisis: Two Independent Failures

Runtime mocking frameworks (MockK, Mockito) solve the boilerplate problem through reflection and bytecode instrumentation. However, this "magic" approach faces two critical, independent failures that make it unsustainable for modern Kotlin development.

### Failure 1: The JVM "Mock Tax" (Performance Crisis)

Kotlin's design—**final classes by default**—creates a fundamental conflict with Java-based mocking tools. Mockito was built for a world where Java classes are `open` by default, allowing runtime subclassing. Kotlin's `final` classes block this mechanism.

#### The Four Bad Workarounds

Kotlin developers are forced to choose between four sub-optimal solutions:

1. **Manual `open` keyword** - Pollutes production code with test-only concerns
2. **`all-open` compiler plugin** - Compromises Kotlin's safety guarantees
3. **Mockito's `mock-maker-inline`** - Uses bytecode instrumentation (slow)
4. **MockK's default behavior** - Bundles instrumentation by default (hidden cost)

#### Verified Performance Penalties

Independent benchmarks[^1] quantify the "Mock Tax"—severe performance degradation when mocking idiomatic Kotlin patterns:

| Mocking Pattern | Framework | Comparison | Verified Penalty |
|-----------------|-----------|------------|------------------|
| `mockkObject` (Singletons) | MockK | vs. Dependency Injection | **1,391x slower** |
| `mockkStatic` (Top-level functions) | MockK | vs. Interface-based DI | **146x slower** |
| `verify { ... }` (Interaction verification) | MockK | vs. State-based testing | **47x slower** |
| `relaxed` mocks (Unstubbed calls) | MockK | vs. Strict mocks | **3.7x slower** |
| `mock-maker-inline` | Mockito | vs. `all-open` plugin | **2.7-3x slower**[^2][^3] |

**Real-world impact**: A production test suite with 2,668 tests experienced a **2.7x slowdown** (7.3s → 20.0s) when using `mock-maker-inline` instead of the `all-open` plugin[^3]. For large projects, this "Mock Tax" accumulates to 40% slower test suites[^1].

The testing framework **actively punishes developers for using Kotlin's most idiomatic features** (objects, top-level functions, extension functions).

### Failure 2: The KMP Dead End (Architectural Impossibility)

Kotlin Multiplatform (KMP) has exploded in adoption—Google, Netflix, Cash App, Stone, and JetBrains all use it in production. But runtime mocking is **fundamentally incompatible** with non-JVM targets.

#### Why Runtime Mocking Cannot Work in KMP

Runtime mocking relies on JVM-specific features—reflection to inspect and modify code at runtime, bytecode instrumentation to change class definitions after loading, and dynamic proxies to generate "magic" classes on the fly. Kotlin/Native and Kotlin/Wasm compile to machine code. There is no JVM. Reflection is severely limited, bytecode doesn't exist, and dynamic proxies are impossible.

**Conclusion**: MockK and Mockito **cannot run in `commonTest`** source sets targeting Native or Wasm[^6][^7]. Runtime mocking is a dead end for the entire KMP ecosystem.

The KMP community attempted compile-time solutions via KSP (Kotlin Symbol Processing), but Kotlin 2.0's K2 compiler broke these tools, forcing major migrations. The architectural limitations of KSP-based approaches are explored in detail in "The Solution" section below.

---

## The Solution: Compile-Time Fake Generation

The problems are clear: manual fakes are tedious, runtime mocking is slow and KMP-incompatible, and KSP-based solutions broke with K2. Fakt addresses all three through a fundamentally different approach.

Fakt solves both the JVM performance crisis and the KMP dead end through **deep compiler integration**—a FIR → IR two-phase architecture that succeeds where KSP-based solutions fundamentally fail.

### Why KSP-Based Solutions Failed

The KMP testing ecosystem attempted to solve the mocking crisis through **KSP (Kotlin Symbol Processing)**—a code generation tool that operates at the symbol level. This approach has proven architecturally inadequate.

**The K2 Compiler Breakage (Verified)**:

Kotlin 2.0's release broke KSP-based mocking libraries. The K2 compiler "fails to handle common metadata tasks properly" in `commonTest` source sets[^8], creating a real-world migration crisis:

- **StreetComplete app** (10,000+ tests) forced to migrate from Mockative to Mokkery
- Mockative maintainer provided no clear K2 migration path
- Mokkery itself was **forced to abandon KSP** and build a full compiler plugin just to survive[^8]

**Architectural Limitations**:

| Issue | KSP (Symbol-Level) | Compiler Plugin (IR-Level) |
|-------|-------------------|---------------------------|
| **Access Level** | After type resolution | During compilation (FIR/IR) |
| **Type System** | Read-only symbol view | Full type manipulation |
| **Generic Support** | Limited (no type substitution) | Complete (IrTypeSubstitutor) |
| **K2 Stability** | BROKEN (forced migrations) | Stable (official extension points) |
| **Cross-Module** | Fragile (metadata issues) | Robust (IR graph traversal) |

**The Verdict**: KSP is the wrong tool for production-quality fake generation. A compiler-level solution is the only viable path.

### Mokkery: A Complementary Solution

For teams committed to interaction-based testing, **Mokkery** is an excellent compiler plugin that survived the K2 migration alongside Fakt. Like Fakt, Mokkery abandoned KSP in favor of deep compiler integration—the only architecture that proved stable during Kotlin 2.0's release.

**Key Differences**:
- **Mokkery**: Mock library for interaction-based testing (MockK-like API, verifies method calls)
- **Fakt**: Fake generator for state-based testing with optional interaction tracking (StateFlow call counting)

**When to choose Mokkery**:
- Your team has MockK expertise and prefers interaction verification
- Strict call ordering verification is critical (`verify(exhaustiveOrder)`)
- Committed to London School testing exclusively

**When to choose Fakt**:
- State-based testing is your primary approach (following Google's NiA patterns)
- Need both paradigms in one tool (state verification + interaction tracking)
- Want debuggable generated code (readable `.kt` files vs IR-only)
- Need to fake Kotlin's full type system (`object`, `sealed`, etc.)

Both eliminate the JVM "Mock Tax" and work across all KMP targets. The choice depends on your testing philosophy, not technical superiority.

**Detailed comparison**: See [Fakt vs Mokkery](../../_drafts/fakt-vs-mokkery-comparison.md) for architectural analysis, decision matrices, and migration examples.

---

### Fakt's FIR → IR Two-Phase Architecture

Fakt uses a **Metro-inspired** production compiler plugin architecture with deep integration into Kotlin's compilation pipeline:

```
┌──────────────────────────────────────────────────────┐
│  PHASE 1: FIR (Frontend IR)                         │
│  • FaktFirExtensionRegistrar                         │
│  • Detects @Fake annotations on interfaces          │
│  • Validates structure, thread-safety requirements   │
│  • Full access to type system during resolution      │
└──────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────┐
│  PHASE 2: IR (Intermediate Representation)          │
│  • UnifiedFaktIrGenerationExtension                  │
│  • InterfaceAnalyzer: Dynamic interface discovery    │
│  • IrCodeGenerator: Type-safe code generation        │
│  • Generates readable .kt files (not IR nodes)      │
└──────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────┐
│  OUTPUT: Generated Kotlin Source Code               │
│  • FakeXxxImpl.kt (implementation class)             │
│  • fakeXxx() factory (type-safe DSL)                 │
│  • Thread-safe StateFlow call tracking               │
└──────────────────────────────────────────────────────┘
```

**Metro Alignment**: Fakt follows the production-validated pattern from [Metro](https://github.com/ZacSweers/metro), Zac Sweers' dependency injection compiler plugin used by Google, Netflix, and Cash App. Metro's two-phase FIR → IR architecture has proven stable across Kotlin 1.9, 2.0, and 2.1.

**Why Not IR-Native Generation?**

Fakt generates **readable `.kt` source files**, not IR nodes directly. For test fakes—code developers constantly debug—transparency outweighs marginal performance gains. Generated fakes are real Kotlin files you can step through with breakpoints, not decompiled bytecode.

---

## Why This Matters

Understanding Fakt's technical implementation is one thing—understanding **why** it matters requires exploring the philosophy, industry validation, and ecosystem synergy behind the "fakes over mocks" movement.

### The Philosophical Foundation: Why Fakes Reduce Brittleness

Beyond performance, the "fakes over mocks" movement represents a fundamental shift in testing philosophy, rooted in decades of TDD (Test-Driven Development) debate.

### State-Based vs. Interaction-Based Testing

Martin Fowler's seminal "Mocks Aren't Stubs"[^10] describes two testing schools:

**State-Based Testing (Classic/Detroit School)**:
- Uses **fakes** and **stubs** to provide inputs
- Verifies the **resulting state** of the system
- **Test assertion**: "After calling `saveUser`, is the user in the fake repository?"

**Interaction-Based Testing (London School)**:
- Uses **mocks** to verify method calls
- Verifies **how** a unit communicates with collaborators
- **Test assertion**: "Was `repository.saveUser()` called exactly once with the correct user?"

### The Refactoring Problem

Mock-based tests couple to **implementation details**, not **outcomes**[^11]. Consider this refactoring:

**Before**:
```kotlin
// SUT implementation
fun checkout() {
    repository.saveOrder(order)
}

// Mock-based test
verify { repository.saveOrder(order) }
```

**After** (valid refactoring):
```kotlin
// SUT implementation
fun checkout() {
    repository.saveOrderWithAudit(order, auditLog = true) // New overload
}

// Mock-based test BREAKS (false negative)
verify { repository.saveOrder(order) } // ❌ Fails!
```

The **outcome is identical** (order is saved), but the **process changed**. The mock-based test reports a failure even though no bug was introduced. This creates a vicious cycle: brittle tests discourage refactoring, leading to code rot.

Google's "Testing on the Toilet" papers define **resilience** as a critical test quality: "A test shouldn't fail if the code under test isn't defective"[^12]. Mock-based tests violate this principle.

### The Virtuous Cycle of Fakes

Fake-based testing with state verification creates resilient tests:

```kotlin
// Fake-based test (survives refactoring)
val fake = fakeRepository()
viewModel.checkout()

// Assert the OUTCOME, not the process
assertEquals(1, fake.orders.size)
assertTrue(fake.orders.contains(order))
```

This test continues passing after the refactoring because it verifies **what happened** (order was saved), not **how it happened** (which method was called). Kent Beck observed that mocks "slow down refactoring... because of the higher coupling between your tests and the actual implementation"[^13].

---

### Industry Validation: Google's "Now in Android" Playbook

The "fakes over mocks" philosophy isn't theoretical—it's the **explicit, documented strategy** of Google's flagship Android reference app.

### The Official Directive

Google's "Now in Android" (NiA) testing strategy wiki states unambiguously:

> **"Don't use mocking frameworks. Instead, use fakes."**[^14]

This decision is deliberate. The documentation explains the goal is to create "**less brittle tests that may exercise more production code, instead of just verifying specific calls against mocks**"[^15].

### Pattern 1: Test-Only Hooks

NiA uses "test-only hooks" to control fake behavior[^14]:

```kotlin
// Production interface
interface NewsRepository {
    fun getNews(): Flow<List<Article>>
}

// Test repository with hooks
class TestNewsRepository : NewsRepository {
    private val newsFlow = MutableStateFlow<List<Article>>(emptyList())

    // Production method
    override fun getNews(): Flow<List<Article>> = newsFlow

    // Test-only hook (not in interface)
    fun sendNews(articles: List<Article>) {
        newsFlow.value = articles
    }
}

// Test usage
@Test
fun `GIVEN breaking news WHEN loading THEN displays alert`() = runTest {
    val fake = TestNewsRepository()
    val viewModel = NewsViewModel(fake)

    // Use test-only hook to control fake
    fake.sendNews(listOf(Article(breaking = true)))

    // Assert resulting state
    assertTrue(viewModel.uiState.value.showAlert)
}
```

This pattern transforms testing from passive "setup-then-verify" (mocks) to active "act-and-assert" (fakes).

### Pattern 2: High-Fidelity Testing

NiA doesn't fake everything—it uses **real implementations** when controllable[^15]:

- **DataStore**: Uses real `DataStore` writing to **temporary folders** wiped after each test
- **Benefit**: Tests exercise serialization, error handling, and migration logic

This reveals a mature testing hierarchy:
1. **First choice**: Real implementation in controlled environment
2. **Second choice**: Fake for uncontrollable dependencies (network)
3. **Last resort (banned)**: Mocks from frameworks

---

### The Asynchronous Testing Synergy

Kotlin's modern async stack—`runTest`, `TestDispatcher`, and Turbine—is **inherently state-based**, creating natural synergy with fakes.

### Official JetBrains Tooling

**`runTest` + `TestDispatcher`**[^16]:
- Official `kotlinx-coroutines-test` library
- Provides `TestScope` with virtual time control
- Best practice: Inject `CoroutineDispatcher` into ViewModels/Repositories

```kotlin
@Test
fun `GIVEN delay WHEN loading THEN skips virtual time`() = runTest {
    val fake = fakeRepository()
    val viewModel = ViewModel(fake, testScheduler) // Inject TestDispatcher

    viewModel.loadData() // Internally: delay(5000)

    // Test completes instantly (virtual time)
    assertEquals(LoadingState.Success, viewModel.state.value)
}
```

### Cash App's Turbine (De Facto Standard)

Turbine[^17] is the universally adopted library for testing `Flow`:

```kotlin
@Test
fun `GIVEN repository updates WHEN observing THEN emits new state`() = runTest {
    val fake = fakeRepository {
        getUser { User(id = "123", name = "Alice") }
    }

    viewModel.userFlow.test {
        fake.sendUser(User(id = "123", name = "Bob")) // Test-only hook
        assertEquals("Bob", awaitItem().name) // State-based assertion
    }
}
```

**Key insight**: Turbine's API (`awaitItem()`) is **designed for state verification**, not interaction verification. The most natural way to produce data for Turbine is a **fake** with `MutableStateFlow` backing.

### The Golden Path

The modern Kotlin testing stack is:
- `runTest` (virtual time)
- Turbine (Flow testing)
- **Fakes** (state-based data sources)

This entire ecosystem is **state-based by design**. Compile-time fake generation completes this stack by automating the "handwritten fake" pattern.

---

## Practical Guidance

Now that you understand what Fakt does, why it matters, and how it works, let's address practical questions: when should you use it, what does it support, and how does it compare to alternatives?

### Fakes vs. Mocks: Quick Comparison

| Feature | MockK/Mockito | Fakt |
|---------|---------------|------|
| **KMP Support** | Limited (JVM only) | Universal (all targets) |
| **Compile-time Safety** | ❌ | ✅ |
| **Runtime Overhead** | Heavy (reflection) | Zero |
| **Type Safety** | Partial (`any()` matchers) | Complete |
| **Learning Curve** | Steep (complex DSL) | Gentle (typed functions) |
| **Call Tracking** | Manual (`verify { }`) | Built-in (StateFlow) |
| **Thread Safety** | Not guaranteed | StateFlow-based |
| **Debuggability** | Reflection (opaque) | Generated `.kt` files |

---

### When NOT to Use Fakes

Fakt addresses a specific problem space. Some scenarios still favor other approaches.

Third-party APIs are better tested with tools like WireMock or Pact. Hand-written fakes for external APIs create "dangerous illusions of fidelity"—they drift from reality without contract validation. WireMock tests the full HTTP client stack. Pact validates contracts with provider teams.

Legacy codebases without interfaces still benefit from pragmatic mocking. Mocking frameworks can mock concrete classes through reflection, allowing incremental refactoring. A "fakes-only" approach demands all-or-nothing interface extraction.

Side effects without observable state—fire-and-forget analytics, logging, interaction timing verification—remain the natural domain of mocks. When there's no state to assert, fakes provide no value.

---

## The Path Forward

The Kotlin testing landscape has shifted. Google's flagship Android app explicitly bans mocking frameworks in favor of fakes. Independent benchmarks quantify the performance cost of runtime mocking—up to 1,391 times slower for idiomatic Kotlin patterns. Kotlin Multiplatform adoption continues accelerating, but reflection-based tools cannot follow it to Native and WebAssembly targets. The K2 compiler broke KSP-based alternatives, forcing production codebases to migrate mid-project.

Fakt addresses this convergence. For JVM-only teams, it eliminates the performance penalties of runtime mocking while creating more resilient tests through state-based verification. For KMP teams, it's the only stable compiler-plugin-based fake generator for commonTest source sets, complementing Mokkery's mocking capabilities with broader type system coverage.

The [Metro](https://github.com/ZacSweers/metro) architecture proves the approach works at scale—a production compiler plugin used by Google, Netflix, and Cash App that relies on the same FIR-to-IR integration pattern. The philosophical foundation has decades of validation through TDD debate and the explicit endorsement of industry leaders. The technical implementation leverages official Kotlin compiler extension points that survive version updates.

Compile-time fake generation isn't an alternative to mocking. It's the sustainable path forward for Kotlin testing.

---

## Next Steps

- [Getting Started](index.md) - Install Fakt and create your first fake in 5 minutes
- [Features](features.md) - Complete feature reference
- [Usage Guide](../user-guide/usage.md) - Common patterns and examples
- [Testing Patterns](../user-guide/testing-patterns.md) - Best practices and strategies
- [Migration from Mocks](../user-guide/migration-from-mocks.md) - Moving from MockK/Mockito to Fakt

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
