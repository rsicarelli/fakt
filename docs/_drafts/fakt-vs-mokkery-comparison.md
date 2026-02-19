# Fakt vs Mokkery: Detailed Comparison

> **Status**: Draft - Not indexed in documentation
> **Purpose**: Deep comparison between Fakt (fake generator) and Mokkery (mock library) for KMP testing
> **Last Updated**: 2026-01-05

---

## Executive Summary

Both Fakt and Mokkery are compiler plugins that survived the K2 migration crisis where KSP-based solutions failed. They represent two complementary approaches to KMP testing:

- **Mokkery**: Mock library for interaction-based testing (MockK analogue)
- **Fakt**: Fake generator for state-based testing with optional interaction tracking

---

## The Mokkery Success Story: Compiler Plugins as the Survivor Architecture

While the KSP ecosystem collapsed during Kotlin 2.0's K2 migration, one library thrived: **Mokkery**[^1].

Mokkery is a Kotlin/IR compiler plugin—not a KSP processor. This architectural choice proved decisive during the K2 migration. While Mockative and MocKMP struggled with broken `commonTest` generation, Mokkery's developer was proactively testing K2 betas in February 2024 and shipped stable K2 support by May 2024[^2].

### The Real-World Impact

The K2 migration created a forced ecosystem migration. StreetComplete, a popular open-source Android app with 10,000+ tests, had to abandon Mockative for Mokkery when K2 broke their test suite[^3]. The Mockative maintainer himself publicly suggested users investigate Mokkery as a viable alternative[^4].

This ecosystem schism validated a critical insight: **compiler plugins, not KSP, are the only stable architecture for KMP test tooling**.

### Why Mokkery Succeeded

Mokkery operates *inside* the Kotlin compiler as an IR transformer. When you write `mock<UserRepository>()`, Mokkery's plugin replaces that call with a fully-generated implementation class at the IR level[^5]. This eliminates KSP's fundamental problem: fragile source-set code generation.

Mokkery's API is intentionally designed as a MockK analogue[^6], providing zero-friction migration for KMP developers:

```kotlin
// Mokkery: Familiar MockK-like DSL
val repository = mock<BookRepository>()
every { repository.findAll() } returns flowOf(Book("..."))
verify(exhaustiveOrder) { repository.findById("1") }
```

Mokkery is production-ready (v2.10.2), actively maintained, and the current incumbent for KMP mocking[^7]. For teams committed to interaction-based testing, Mokkery is a credible, stable solution.

---

## Architectural Comparison

### Compilation Level

| Aspect | Mokkery | Fakt |
|--------|---------|------|
| **Plugin Type** | IR-only (single-phase) | FIR → IR (two-phase) |
| **Code Generation** | Anonymous IR classes (invisible) | Readable `.kt` files |
| **Debuggability** | Decompiled bytecode only | Source files with breakpoints |
| **Build Output** | No physical files | `build/generated/fakt/` |

### Technical Architecture

**Mokkery**:
- Single-phase IR transformation
- Runtime `mock<T>()` call replaced with generated class
- No physical source files (IR-native)
- Fast compilation (no file I/O)

**Fakt**:
- Two-phase FIR → IR pipeline
- Compile-time `@Fake` annotation detection
- Generates readable `.kt` files
- Full type resolution at FIR phase

**The Compiler Plugin Validation**:

Mokkery's success proves the architectural validity of compiler plugins for KMP testing. Both Mokkery and Fakt share this fundamental design decision. This isn't coincidental—it's the *only* path that survives K2's stricter compilation model.

---

## Testing Paradigm Differences

### Where Mokkery and Fakt Diverge

The critical difference isn't architecture—it's **testing paradigm coverage**.

**Mokkery**: A **mocking library** designed exclusively for *interaction-based testing* (verifying that methods were called).

**Fakt**: A **fake generator** that supports *both paradigms*: state-based testing (primary) with built-in interaction tracking through StateFlow call counting.

### Mokkery's Documented Limitations

As a mocking library, Mokkery is architecturally *unable* to mock[^8]:

- `object` singletons
- `sealed class` and `sealed interface` hierarchies
- Top-level functions and extension functions
- Final classes from third-party dependencies

These aren't bugs—they're the glass ceiling of the mocking paradigm. Mocking requires "fully overridable" types (interfaces, abstract classes). Sealed types, objects, and final classes cannot be "mocked" at runtime.

### Fakt's Dual Paradigm Advantage

Fakt doesn't mock—it generates *real implementations* with support for both testing approaches. Every generated fake includes:

- **Behavior configuration** (state-based testing)
- **StateFlow call history** (interaction-based testing)
- **Thread-safety** (no `var count = 0` footguns)

The same Fakt fake can verify state *and* interactions:

```kotlin
val fake = fakeUserRepository {
    save { user -> user.copy(id = "generated-id") }
}

// State-based verification
val result = fake.save(User("test"))
assertEquals("generated-id", result.id) // What happened?

// Interaction-based verification (same fake)
assertEquals(1, fake.saveCalls.value.size) // How many times?
```

This dual paradigm support means you're not forced to choose philosophies—Fakt adapts to your testing needs.

---

## The Brittleness Trade-off

Consider this refactoring scenario that highlights the paradigm difference:

```kotlin
// Original implementation
fun checkout() {
    repository.saveOrder(order)
}

// Mokkery test (interaction-based ONLY)
verify(exactly = 1) { repository.saveOrder(order) }

// Refactored implementation (same outcome, different method signature)
fun checkout() {
    repository.saveOrderWithAudit(order, auditLog = true)
}

// Result: Mokkery test BREAKS (false negative)
// The outcome is identical, but the process changed
```

The outcome is identical (order saved), but the *process* changed. The mock-based test fails even though no bug exists. This is the brittleness Martin Fowler warned about—tests coupled to *how* code works, not *what* it achieves[^9].

### The same refactoring with Fakt

```kotlin
// Fakt test (state-based verification)
val fake = fakeRepository()
viewModel.checkout()
assertEquals(1, fake.orders.size) // Assert OUTCOME, not process
// Test survives refactoring
```

This test survives because it verifies **state** (was the order saved?), not **interactions** (which method was called?). And when you *do* need interaction verification, Fakt provides it through StateFlow: `assertEquals(1, fake.saveOrderCalls.value.size)`.

---

## Decision Matrix

### Choose Mokkery When:

- ✅ Interaction-based testing is your standard
- ✅ MockK muscle memory drives your team
- ✅ Side-effect verification without observable state
- ✅ Committed to London School exclusively
- ✅ Need to verify call ordering (`verify(exhaustiveOrder)`)

### Choose Fakt When:

- ✅ State-based testing is your primary approach
- ✅ Need both paradigms in one tool
- ✅ Building test fixtures with controllable behavior
- ✅ Following Google's NiA pattern (state-based)
- ✅ Need to fake `object`, `sealed`, or final types
- ✅ Want debuggable generated code (readable `.kt` files)

---

## Complementary Solutions

Mokkery and Fakt serve different philosophical commitments:

| Aspect | Mokkery | Fakt |
|--------|---------|------|
| **Primary Paradigm** | Interaction-based only | State-based + optional interactions |
| **API Style** | MockK-like DSL | Builder pattern DSL |
| **Test Resilience** | Coupled to process | Coupled to outcome |
| **Type Coverage** | Interfaces, abstract classes | Interfaces, abstract, open, sealed |
| **Migration Path** | From MockK → Mokkery | From handwritten fakes → Fakt |
| **Team Background** | Teams with mocking expertise | Teams adopting state-based testing |

For teams with MockK expertise, Mokkery is the natural KMP migration path. For teams adopting state-based testing or needing flexibility between both paradigms, Fakt completes the Kotlin async testing stack (`runTest` + Turbine + Fakes).

---

## Performance Comparison

Both eliminate the JVM "Mock Tax" from runtime reflection-based frameworks (MockK/Mockito):

| Metric | MockK (Runtime) | Mokkery (Compile-time) | Fakt (Compile-time) |
|--------|----------------|----------------------|-------------------|
| `mockkObject` penalty | 1,391x slower[^10] | ✅ Eliminated | ✅ Eliminated |
| `mockkStatic` penalty | 146x slower[^10] | ✅ Eliminated | ✅ Eliminated |
| Reflection overhead | ❌ Heavy | ✅ Zero | ✅ Zero |
| Compilation time | N/A | Fast (IR-only) | Moderate (FIR + IR) |

---

## Feature Matrix

| Feature | Mokkery | Fakt |
|---------|---------|------|
| **KMP Support** | ✅ All targets | ✅ All targets |
| **State-based testing** | ❌ | ✅ Primary |
| **Interaction verification** | ✅ Primary | ✅ Via StateFlow |
| **Call ordering verification** | ✅ `verify(exhaustiveOrder)` | ❌ |
| **Mock `object` singletons** | ❌[^8] | ✅ |
| **Mock `sealed` types** | ❌[^8] | ✅ |
| **Mock final classes** | ❌[^8] | ❌ (same limitation) |
| **Readable generated code** | ❌ (IR-only) | ✅ (`.kt` files) |
| **Debuggability** | Decompiled bytecode | Source-level breakpoints |
| **Thread-safe call tracking** | ✅ | ✅ (StateFlow) |
| **Learning curve** | Steep (MockK DSL) | Gentle (builder pattern) |

---

## Real-World Example Comparison

### Scenario: Testing a ViewModel with Repository Dependency

#### Mokkery Approach (Interaction-Based)

```kotlin
@Test
fun `GIVEN repository WHEN loading user THEN calls getUser once`() = runTest {
    val repository = mock<UserRepository>()
    every { repository.getUser(any()) } returns User("123", "Alice")

    val viewModel = UserViewModel(repository)
    viewModel.loadUser("123")

    verify(exactly = 1) { repository.getUser("123") }
    assertEquals("Alice", viewModel.uiState.value.userName)
}
```

**Pros**:
- Explicit call verification
- Familiar to MockK users
- Verifies interaction happened

**Cons**:
- Breaks if method signature changes
- Coupled to implementation details
- Can't reuse mock across tests easily

---

#### Fakt Approach (State-Based with Optional Interaction Tracking)

```kotlin
@Test
fun `GIVEN repository WHEN loading user THEN displays user name`() = runTest {
    val fake = fakeUserRepository {
        getUser { id -> User(id, "Alice") }
    }

    val viewModel = UserViewModel(fake)
    viewModel.loadUser("123")

    // State-based: Verify outcome
    assertEquals("Alice", viewModel.uiState.value.userName)

    // Optional: Interaction verification if needed
    assertEquals(1, fake.getUserCalls.value.size)
}
```

**Pros**:
- Survives refactoring (verifies outcome, not process)
- Reusable fake across tests
- Both paradigms available
- Test-only hooks for behavior control

**Cons**:
- Can't verify strict call ordering
- Requires explicit behavior configuration

---

## Migration Paths

### From MockK to Mokkery (KMP Teams)

```kotlin
// Before: MockK (JVM only)
val mock = mockk<UserService>()
every { mock.getUser(any()) } returns User("123", "Test")

// After: Mokkery (KMP)
val mock = mock<UserService>()
every { mock.getUser(any()) } returns User("123", "Test")
// API is nearly identical!
```

---

### From Handwritten Fakes to Fakt

```kotlin
// Before: Manual fake (60+ lines)
class FakeUserService : UserService {
    private var getUserBehavior: ((String) -> User)? = null
    private val _getUserCalls = mutableListOf<Unit>()
    val getUserCalls: List<Unit> get() = _getUserCalls

    override fun getUser(id: String): User {
        _getUserCalls.add(Unit)
        return getUserBehavior?.invoke(id) ?: User(id, "")
    }

    fun configureGetUser(behavior: (String) -> User) {
        getUserBehavior = behavior
    }
}

// After: Fakt (zero boilerplate)
@Fake
interface UserService {
    fun getUser(id: String): User
}

val fake = fakeUserService {
    getUser { id -> User(id, "Test") }
}
```

---

## Conclusion

Mokkery and Fakt are complementary solutions, not competitors:

- **Mokkery excels** at interaction-based testing for teams with mocking expertise
- **Fakt excels** at state-based testing with dual paradigm support

Both validate the compiler plugin architecture as the survivor of the K2 schism. The choice isn't "which is better"—it's about **paradigm needs** and **team philosophy**.

For teams migrating from MockK, Mokkery offers zero-friction API compatibility. For teams adopting Google's state-based testing patterns or needing to fake Kotlin's full type system (objects, sealed types), Fakt provides the flexibility and resilience benefits documented throughout this comparison.

---

## Works Cited

[^1]: lupuuss/Mokkery: The mocking library for Kotlin Multiplatform. GitHub. [https://github.com/lupuuss/Mokkery](https://github.com/lupuuss/Mokkery)

[^2]: Kotlin 2.0.0 support · Issue #1 · lupuuss/Mokkery. GitHub. [https://github.com/lupuuss/Mokkery/issues/1](https://github.com/lupuuss/Mokkery/issues/1)

[^3]: Use multiplatform mocking library for tests · Issue #5420 · streetcomplete/StreetComplete. GitHub. [https://github.com/streetcomplete/StreetComplete/issues/5420](https://github.com/streetcomplete/StreetComplete/issues/5420)

[^4]: Mocking in Kotlin Multiplatform: KSP vs Compiler Plugins. Martin Hristev. [https://medium.com/@mhristev/mocking-in-kotlin-multiplatform-ksp-vs-compiler-plugins-4424751b83d7](https://medium.com/@mhristev/mocking-in-kotlin-multiplatform-ksp-vs-compiler-plugins-4424751b83d7)

[^5]: Kotlin 2.2.0 support · Issue #83 · lupuuss/Mokkery. GitHub. [https://github.com/lupuuss/Mokkery/issues/83](https://github.com/lupuuss/Mokkery/issues/83)

[^6]: Mocking | Mokkery. [https://mokkery.dev/docs/Guides/Mocking/](https://mokkery.dev/docs/Guides/Mocking/)

[^7]: A to Z of Testing in Kotlin Multiplatform. Kinto Technologies. [https://blog.kinto-technologies.com/posts/2024-12-24-tests-in-kmp/](https://blog.kinto-technologies.com/posts/2024-12-24-tests-in-kmp/)

[^8]: Limitations | Mokkery. [https://mokkery.dev/docs/Limitations/](https://mokkery.dev/docs/Limitations/)

[^9]: Mocks Aren't Stubs. Martin Fowler. [https://martinfowler.com/articles/mocksArentStubs.html](https://martinfowler.com/articles/mocksArentStubs.html)

[^10]: Benchmarking Mockk — Avoid these patterns for fast unit tests. Kevin Block. [https://medium.com/@_kevinb/benchmarking-mockk-avoid-these-patterns-for-fast-unit-tests-220fc225da55](https://medium.com/@_kevinb/benchmarking-mockk-avoid-these-patterns-for-fast-unit-tests-220fc225da55)
