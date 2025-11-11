# Why Fakt?

The story behind building a compile-time fake generator for Kotlin testing.

---

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

**Critical issues:**
- **Non-thread-safe call tracking** (`var count = 0`) breaks under concurrent tests
- **Maintenance burden** scales with interface complexity (N methods = ~30N lines)
- **Silent refactoring failures** (interface signature changes don't break unused fakes)
- **Copy-paste errors** accumulate across dozens of fakes

For a codebase with 50+ interfaces requiring fakes, this represents thousands of lines of brittle, repetitive boilerplate.

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

Kotlin Multiplatform (KMP) has exploded in adoption—Google, Netflix, Cash App, and JetBrains all use it in production. But runtime mocking is **fundamentally incompatible** with non-JVM targets.

#### Why Runtime Mocking Cannot Work in KMP

Runtime mocking relies on JVM-specific features:

- **Reflection** - Inspect and modify code at runtime
- **Bytecode instrumentation** - Change class definitions after loading
- **Dynamic proxies** - Generate "magic" classes on the fly

Kotlin/Native (iOS, Desktop) and Kotlin/Wasm compile to native machine code. **They have no JVM**. Reflection is severely limited[^4][^5], bytecode doesn't exist, and dynamic proxies are impossible.

**Conclusion**: MockK and Mockito **cannot run in `commonTest`** source sets targeting Native or Wasm[^6][^7]. Runtime mocking is a dead end for the entire KMP ecosystem.

#### The KSP "Workaround" Crisis

The KMP community attempted compile-time code generation via KSP (Kotlin Symbol Processing), creating tools like Mockative, MocKMP, and Mokkery. **This ecosystem is in crisis:**

**Kotlin 2.0 Breakage (Verified)**:
- The K2 compiler "fails to handle common metadata tasks properly" in `commonTest` source sets[^8]
- Real-world example: StreetComplete app (10,000+ tests) forced to migrate from Mockative to Mokkery due to K2 incompatibility[^8]
- Mockative maintainer not providing clear K2 migration path[^8]

**Feature Limitations**:
- **Mokkery** cannot mock: `object`, `sealed`, top-level functions, final classes (without `all-open`)[^8]
- **MocKMP** can only mock interfaces, no relaxed mocks[^9]

**Strategic failure**: The KSP "solutions" cannot even solve the original Kotlin problems (objects, top-level functions) that create JVM performance penalties.

---

## The Philosophical Foundation: Why Fakes Reduce Brittleness

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

## Industry Validation: Google's "Now in Android" Playbook

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

## The Asynchronous Testing Synergy

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

## The Solution: Compile-Time Fake Generation

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

Fakt generates **readable `.kt` source files**, not IR nodes directly. For test fakes—code developers constantly debug—**transparency outweighs marginal performance gains**. Generated fakes are real Kotlin files you can step through with breakpoints, not decompiled bytecode.

### Supporting Both Testing Paradigms

Fakt doesn't force you to choose between state-based or interaction-based testing. **Every generated fake supports both philosophies simultaneously**.

**State-Based Testing (Classic/Detroit School)**:

```kotlin
@Fake
interface UserRepository {
    suspend fun save(user: User): User
}

// Generated: Behavior configuration DSL
val fake = fakeUserRepository {
    save { user -> user.copy(id = "generated-id") }
}

// Test: Verify STATE
@Test
fun `GIVEN user WHEN saving THEN returns persisted user`() = runTest {
    val result = fake.save(User("test"))
    assertEquals("generated-id", result.id) // State verification
}
```

**Interaction-Based Testing (London School)**:

```kotlin
// Same generated fake includes automatic call tracking
@Test
fun `GIVEN multiple saves WHEN called THEN tracks interaction count`() = runTest {
    fake.save(User("user1"))
    fake.save(User("user2"))

    // Verify INTERACTIONS (thread-safe StateFlow)
    assertEquals(2, fake.saveCallCount.value)
}
```

**The Technical Achievement**:

Every generated method/property includes:
- **Behavior configuration** (state-based testing)
- **StateFlow call tracking** (interaction-based testing)
- **Thread-safety** (no `var count = 0` footguns)
- **Zero runtime overhead** (compile-time generation)

One tool. Both paradigms. Zero dogma.

### How Fakt Works

```kotlin
@Fake
interface UserRepository {
    suspend fun getUser(id: String): Result<User>
}

// Generated at compile-time (zero runtime overhead)
val fake = fakeUserRepository {
    getUser { id -> Result.success(User(id, "Alice")) }
}

// State-based: Configure behavior
fake.getUser("123") // Returns User("123", "Alice")

// Interaction-based: Verify calls
assertEquals(1, fake.getUserCallCount.value) // Thread-safe StateFlow
```

### Technical Advantages

**Universal Platform Support**:
- JVM, Android, Native (iOS/Desktop), JavaScript, WebAssembly
- No reflection required (works on Native/Wasm)
- Zero runtime dependencies

**Zero Performance Overhead**:
- Eliminates 1,391x MockK penalty[^1]
- No bytecode instrumentation
- Generated code as fast as handwritten fakes

**100% Debuggable**:
- Readable `.kt` files in `build/generated/fakt/`
- Set breakpoints, inspect variables
- Step through generated implementations line-by-line

**Compiler-Level Stability**:
- Uses official Kotlin compiler extension points
- Survives K2 compiler updates (unlike KSP tools)
- Aligned with Metro's production-tested patterns

**Built-In Thread Safety**:
- StateFlow call tracking (not `var count = 0`)
- No concurrent test flakiness
- Reactive (works with Turbine for Flow testing)

---

## When NOT to Use Fakes

Fakt isn't a silver bullet. Some scenarios favor other tools:

**Third-Party APIs** → Use **WireMock**[^18] or **Pact**[^19]
- Hand-written fakes for external APIs are "dangerous illusions of fidelity"
- WireMock tests full HTTP client stack
- Pact validates contracts with provider teams

**Legacy Code Without Interfaces** → Use **Pragmatic Mocks**
- Mocking frameworks can mock concrete classes via reflection
- Allows testing before major refactoring
- "Fakes-only" demands all-or-nothing interface refactoring

**Side Effects Without Observable State** → Use **Mocks**
- Fire-and-forget analytics, logging
- Interaction timing/ordering matters
- Fakes provide no value without state to assert

---

## The Path Forward

Fakt represents the convergence of:
- **Industry best practices** (Google's NiA pattern)
- **Verified performance data** (1,391x MockK penalty eliminated)
- **Architectural necessity** (KMP requires compile-time solutions)
- **Dual paradigm support** (state-based AND interaction-based testing)
- **Compiler-level stability** (FIR → IR architecture survives K2 updates)

For JVM-only teams, Fakt delivers 40% faster test suites[^1] and more resilient tests. For KMP teams, Fakt is the **only stable, feature-complete testing solution** for `commonTest`.

---

## Next Steps

- [Installation](installation.md) - Add Fakt to your project in 5 minutes
- [Quick Start](quick-start.md) - Generate your first fake
- [Testing Patterns](../guides/testing-patterns.md) - Hybrid strategies and best practices
- [Migration Guide](../guides/migration.md) - Moving from MockK/Mockito

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
