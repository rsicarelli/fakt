# Overview

Fakt is a Kotlin compiler plugin that eliminates the boilerplate of writing test fakes by generating them at compile-time.

---

## The Problem

Writing test fakes manually is tedious and error-prone. A simple interface requires ~50 lines of boilerplate:

```kotlin
interface AnalyticsService {
    fun track(event: String)
}

// Manual fake: repetitive boilerplate
class FakeAnalyticsService : AnalyticsService {
    private var trackBehavior: ((String) -> Unit)? = null
    private var _trackCallCount = 0
    val trackCallCount: Int get() = _trackCallCount

    override fun track(event: String) {
        _trackCallCount++
        trackBehavior?.invoke(event) ?: Unit
    }

    fun configureTrack(behavior: (String) -> Unit) {
        trackBehavior = behavior
    }
}

// Manual configuration (no DSL)
val fake = FakeAnalyticsService().apply {
    configureTrack { event -> println("Tracked: $event") }
}
```

**Issues with manual fakes:**

- **Repetitive boilerplate** for every interface
- **Non-thread-safe call tracking** (`var count = 0`)
- **Silent refactoring failures** (interface changes don't break tests)
- **Maintenance burden** scales with codebase

---

## The Solution

Add `@Fake` annotation. Fakt generates everything automatically:

```kotlin
@Fake
interface AnalyticsService {
    fun track(event: String)
}

// Generated factory + DSL (zero boilerplate)
val fake = fakeAnalyticsService {
    track { event -> println("Tracked: $event") }
}

fake.track("user_signup")
assertEquals(1, fake.trackCallCount.value)
```

**What Fakt generates:**

1. **Implementation class** with thread-safe StateFlow call tracking
2. **Factory function** with clean DSL (`fakeXxx {}`)
3. **Configuration DSL** for behavior setup

---

## Architecture: Two-Phase FIR → IR Compilation

Fakt uses a **two-phase compilation architecture** inspired by production compiler plugins like [Metro](https://github.com/ZacSweers/metro):

```
┌─────────────────────────────────────────────────────────────────┐
│  PHASE 1: FIR (Frontend Intermediate Representation)           │
│  ════════════════════════════════════════════════════════       │
│  • Detects @Fake annotations on interfaces                     │
│  • Validates interface structure                               │
│  • Passes validated interfaces to IR phase                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  PHASE 2: IR (Intermediate Representation)                     │
│  ════════════════════════════════════════════════════════       │
│  • Analyzes interface metadata (methods, properties, types)    │
│  • Generates IR nodes (implementation + factory + DSL)         │
│  • Outputs Kotlin code in test source sets                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  OUTPUT: Generated Kotlin Code (test source sets only)         │
│  ════════════════════════════════════════════════════════       │
│  • FakeXxxImpl.kt         - Fake implementation class          │
│  • fakeXxx() factory      - Type-safe factory function         │
│  • FakeXxxConfig          - Configuration DSL                  │
└─────────────────────────────────────────────────────────────────┘
```

### Why IR-Level Generation?

Fakt generates code at the **Intermediate Representation (IR)** level, not text-based generation:

**✅ Full type system access**
- Proper generic handling (constraints, variance, bounds)
- Correct type resolution across modules
- Type-safe code generation

**✅ Performance**
- Faster than KSP or annotation processors
- Works with Kotlin's incremental compilation
- Intelligent caching across KMP targets

**✅ Future-proof**
- Aligned with Kotlin compiler evolution
- Compatible with K2 compiler
- Industry-standard approach (Metro, Room, etc.)

---

## Generated Code Example

For this interface:

```kotlin
@Fake
interface UserRepository {
    suspend fun getUser(id: String): Result<User>
    val currentUser: User?
}
```

Fakt generates:

```kotlin
class FakeUserRepositoryImpl : UserRepository {
    // Thread-safe StateFlow call tracking
    private val _getUserCallCount = MutableStateFlow(0)
    val getUserCallCount: StateFlow<Int> get() = _getUserCallCount

    private val _currentUserCallCount = MutableStateFlow(0)
    val currentUserCallCount: StateFlow<Int> get() = _currentUserCallCount

    // Smart default behaviors
    private var getUserBehavior: suspend (String) -> Result<User> = {
        Result.failure(NotImplementedError("getUser not configured"))
    }
    private var currentUserBehavior: () -> User? = { null }

    override suspend fun getUser(id: String): Result<User> {
        _getUserCallCount.update { it + 1 }
        return getUserBehavior(id)
    }

    override val currentUser: User?
        get() {
            _currentUserCallCount.update { it + 1 }
            return currentUserBehavior()
        }

    // Internal configuration methods
    internal fun configureGetUser(behavior: suspend (String) -> Result<User>) {
        getUserBehavior = behavior
    }

    internal fun configureCurrentUser(behavior: () -> User?) {
        currentUserBehavior = behavior
    }
}

// Factory function
fun fakeUserRepository(
    configure: FakeUserRepositoryConfig.() -> Unit = {}
): FakeUserRepositoryImpl = FakeUserRepositoryImpl().apply {
    FakeUserRepositoryConfig(this).configure()
}

// Configuration DSL
class FakeUserRepositoryConfig(private val fake: FakeUserRepositoryImpl) {
    fun getUser(behavior: suspend (String) -> Result<User>) {
        fake.configureGetUser(behavior)
    }

    fun currentUser(behavior: () -> User?) {
        fake.configureCurrentUser(behavior)
    }
}
```

---

## Key Technical Achievements

### ✅ Universal KMP Support

Fakt works on **ALL Kotlin Multiplatform targets** without reflection:

- JVM, Android, iOS, Native, JavaScript, WebAssembly
- No platform-specific dependencies
- Same API across all targets

### ✅ Zero Production Overhead

- **Zero runtime dependencies** (annotation is BINARY retention only)
- **Test-only generation** (never appears in production builds)
- **No reflection** (works on Native/WASM)

### ✅ Built-In StateFlow Call Tracking

Thread-safe, reactive call counting out of the box:

```kotlin
val fake = fakeRepository()
fake.getUser("123")

// Thread-safe
assertEquals(1, fake.getUserCallCount.value)

// Reactive (works with Flow test utilities)
fake.getUserCallCount.test {
    fake.getUser("456")
    assertEquals(2, awaitItem())
}
```

### ✅ Smart Defaults

Fakt generates sensible default behaviors:

| Type                | Default Behavior                          |
|---------------------|-------------------------------------------|
| `Unit`              | `{ }`                                     |
| `Boolean`           | `{ false }`                               |
| `Int`, `Long`, etc. | `{ 0 }`                                   |
| `String`            | `{ "" }`                                  |
| `List<T>`           | `{ emptyList() }`                         |
| `Result<T>`         | `{ Result.failure(NotImplementedError)}` |
| Generic `T -> T`    | `{ it }` (identity function)              |
| Nullable `T?`       | `{ null }`                                |

---

## What Fakt Supports

**Class Types:**
- ✅ Interfaces
- ✅ Abstract classes
- ✅ Open classes (overridable members only)

**Type System:**
- ✅ Full generics (class-level, method-level, constraints, variance)
- ✅ Nullable types
- ✅ Complex stdlib types (`Result<T>`, `List<T>`, etc.)

**Kotlin Features:**
- ✅ Suspend functions
- ✅ Properties (`val`, `var`)
- ✅ Methods with parameters
- ✅ Inheritance

---

## Current Limitations

Fakt is honest about what it doesn't support (yet):

- ❌ Data classes as `@Fake` targets (work fine as parameter/return types)
- ❌ Sealed hierarchies as `@Fake` targets
- ❌ Default parameters in interface methods

See [Limitations](../reference/limitations.md) for details and workarounds.

---

## Next Steps

- [Installation](installation.md) - Add Fakt to your project
- [Quick Start](quick-start.md) - Your first fake in 5 minutes
- [Features](features.md) - Detailed feature breakdown
