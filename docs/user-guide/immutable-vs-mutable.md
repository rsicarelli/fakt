# Immutable vs Mutable Fakes

Understanding when to use each mode and the trade-offs involved.

---

## Immutable Fakes (Default)

By default, Fakt generates **immutable fakes** where all behavior is set at construction time via the factory function and cannot change afterwards:

```kotlin
@Fake
interface UserRepository {
    fun findById(id: String): User?
    suspend fun save(user: User): User
}
```

```kotlin
// Behavior is fixed at construction
val fake = fakeUserRepository {
    findById { id -> User(id, "Alice") }
}

// To test different behavior, create a new fake
val fakeNotFound = fakeUserRepository {
    findById { null }
}
```

### Why Immutable by Default?

Immutable fakes provide several guarantees that make tests safer and easier to reason about:

**Thread Safety**

Immutable fakes are inherently thread-safe. Since behavior properties are `private val`, they cannot be modified after construction. Multiple coroutines or threads can safely call the same fake concurrently with no risk of data races on the behavior configuration:

```kotlin
@Test
fun `GIVEN immutable fake WHEN called from 100 coroutines THEN all calls are safe`() = runTest {
    val fake = fakeUserRepository {
        findById { id -> User(id, "Alice") }
    }

    // Safe: behavior is immutable, no data races possible
    List(100) { i ->
        async { fake.findById("$i") }
    }.awaitAll()

    assertEquals(100, fake.findByIdCalls.value.size)
}
```

**Predictability**

Each fake instance has exactly one behavior for its entire lifetime. When reading a test, you can trace any fake's behavior back to its construction site without scanning for later mutations:

```kotlin
@Test
fun `GIVEN user found WHEN loading profile THEN displays name`() {
    // Everything about this fake is visible right here
    val fake = fakeUserRepository {
        findById { id -> User(id, "Alice") }
    }

    val result = profileLoader.load(fake, "123")

    assertEquals("Alice", result.name)
}
```

**No Test Pollution**

Immutable fakes cannot accumulate state changes across test phases. Each scenario uses its own fake instance, preventing one test's configuration from leaking into another:

```kotlin
@Test
fun `GIVEN user exists WHEN deleting THEN succeeds`() {
    val fake = fakeUserRepository { findById { User("1", "Alice") } }
    assertTrue(userService.delete(fake, "1"))
}

@Test
fun `GIVEN user missing WHEN deleting THEN fails gracefully`() {
    val fake = fakeUserRepository { findById { null } }  // Fresh instance
    assertFalse(userService.delete(fake, "1"))
}
```

**Debuggability**

When a test fails, the fake's behavior is fully determined by its construction. There is no need to trace through mid-test mutations to understand what went wrong.

---

## Mutable Fakes (Opt-In)

For scenarios where behavior needs to change during a single test, Fakt supports **mutable fakes** as an opt-in feature:

```kotlin
import com.rsicarelli.fakt.MutabilityMode

@Fake(mutability = MutabilityMode.MUTABLE)
interface UserRepository {
    fun findById(id: String): User?
    suspend fun save(user: User): User
    val status: String
}
```

Mutable fakes include a `configure {}` method that allows selective reconfiguration:

```kotlin
@Test
fun `GIVEN repository WHEN database fails mid-operation THEN service recovers`() {
    val fake = fakeUserRepository {
        findById { id -> User(id, "Alice") }
        save { user -> user }
        status { "connected" }
    }

    // Phase 1: Normal operation
    val user = fake.findById("1")
    assertEquals("Alice", user?.name)

    // Phase 2: Simulate database failure
    fake.configure {
        findById { null }
        status { "disconnected" }
    }

    // Phase 3: Verify degraded behavior
    assertNull(fake.findById("1"))
    assertEquals("disconnected", fake.status)
    // save was NOT reconfigured — it still works as before
}
```

### When Mutable Fakes Make Sense

Mutable fakes are designed for **integration tests** where the same injected fake instance must simulate different states during a single test:

**Failure simulation** — Test how your system handles a dependency that starts working and then fails:

```kotlin
@Test
fun `GIVEN cache working WHEN cache becomes unavailable THEN falls back to database`() {
    val fakeCache = fakeCacheService {
        get { key -> "cached_value" }
    }
    val service = DataService(fakeCache, realDatabase)

    // Cache works initially
    assertEquals("cached_value", service.fetch("key"))

    // Cache goes down
    fakeCache.configure {
        get { throw ConnectionException("Cache unavailable") }
    }

    // Service falls back to database
    val result = service.fetch("key")
    assertEquals("db_value", result)
}
```

**State machine transitions** — Test components that depend on state changes from a single dependency:

```kotlin
@Test
fun `GIVEN auth service WHEN token expires THEN refreshes automatically`() {
    val fakeAuth = fakeAuthService {
        isAuthenticated { true }
        getToken { "valid-token-123" }
    }

    val client = ApiClient(fakeAuth)
    assertTrue(client.makeRequest("/api/data").isSuccess)

    // Token expires
    fakeAuth.configure {
        isAuthenticated { false }
        getToken { throw TokenExpiredException() }
    }

    // Client should trigger refresh flow
    val result = client.makeRequest("/api/data")
    assertTrue(result.isRefreshTriggered)
}
```

**Feature flag toggling** — Test runtime behavior changes:

```kotlin
@Test
fun `GIVEN feature disabled WHEN feature enabled mid-session THEN new UI renders`() {
    val fakeFlags = fakeFeatureFlagService {
        isEnabled { false }
    }
    val viewModel = SettingsViewModel(fakeFlags)

    assertFalse(viewModel.showNewFeature.value)

    fakeFlags.configure {
        isEnabled { true }
    }

    assertTrue(viewModel.showNewFeature.value)
}
```

### Selective Reconfiguration

The `configure {}` method only updates the behaviors you explicitly set. All other behaviors remain unchanged:

```kotlin
fake.configure {
    findById { null }  // Only this changes
    // save, status — remain as previously configured
}
```

This means you can reconfigure one method without worrying about accidentally resetting others.

---

## Concurrency Considerations

### Immutable Fakes: Thread-Safe by Design

Behavior properties are `private val` — they cannot change after construction. This means:

- Multiple coroutines can call the same fake simultaneously
- No synchronization is needed
- Call history uses thread-safe internal state (backed by `AtomicReference`)
- Safe for `runTest` with concurrent `async` / `launch` blocks

### Mutable Fakes: Caller Responsibility

Behavior properties are `internal var` — they **can** be reassigned via `configure {}`. This means:

- **Do not call `configure {}` from multiple threads/coroutines simultaneously**
- **Do not call `configure {}` while other coroutines are actively calling the fake**
- Use `configure {}` in sequential test phases, not during concurrent execution

```kotlin
// SAFE: Sequential phases
fake.findById("1")           // Phase 1
fake.configure { ... }        // Reconfigure between phases
fake.findById("2")           // Phase 2

// UNSAFE: Concurrent mutation
launch { fake.configure { findById { null } } }  // Don't do this
launch { fake.findById("1") }                      // while this runs
```

!!! tip "Rule of thumb"
    If your test uses concurrent coroutines to call a fake, use an **immutable** fake. Reserve mutable fakes for sequential, phase-based integration tests.

---

## Comparison

| Aspect | Immutable (Default) | Mutable (Opt-In) |
|--------|-------------------|---------|
| **Behavior properties** | `private val` | `internal var` |
| **Reconfiguration** | Not possible — create a new fake | Via `configure {}` method |
| **Thread safety** | Guaranteed | Caller responsibility |
| **Recommended for** | Unit tests, concurrent tests | Integration tests, state simulation |
| **`configure {}` method** | Not generated | Generated |
| **Annotation** | `@Fake` (default) | `@Fake(mutability = MutabilityMode.MUTABLE)` |
| **Plugin setting** | `enableMutableFakes.set(false)` | `enableMutableFakes.set(true)` |

---

## Call History Composition

Mutable fakes and call history are independent features that compose without conflicts. When both are enabled:

- Call history **accumulates across reconfigurations** — it is never reset by `configure {}`
- This means you can verify the total number of calls across all phases of a test

```kotlin
@Test
fun `GIVEN mutable tracked fake WHEN reconfigured THEN call history persists`() {
    val fake = fakeMutableTrackedRepository {
        find { listOf(User("1", "Test")) }
    }

    // Phase 1: Two calls
    fake.find("query1")
    fake.find("query2")
    assertEquals(2, fake.findCalls.value.size)

    // Reconfigure
    fake.configure {
        find { emptyList() }
    }

    // Phase 2: One more call with new behavior
    assertEquals(emptyList(), fake.find("query3"))

    // Call history accumulated: 2 + 1 = 3
    assertEquals(3, fake.findCalls.value.size)
}
```

---

## Configuration

Mutable fakes can be enabled at two levels:

**Project-wide default** in `build.gradle.kts`:

```kotlin
fakt {
    enableMutableFakes.set(true)  // All fakes mutable by default
}
```

**Per-interface override** via annotation:

```kotlin
// Always mutable, regardless of plugin default
@Fake(mutability = MutabilityMode.MUTABLE)
interface UserRepository { ... }

// Always immutable, regardless of plugin default
@Fake(mutability = MutabilityMode.IMMUTABLE)
interface Logger { ... }

// Follow plugin default
@Fake  // or @Fake(mutability = MutabilityMode.DEFAULT)
interface UserService { ... }
```

**Resolution order:** Annotation setting takes precedence over plugin default.

See [Plugin Configuration](plugin-configuration.md#mutable-fakes-configuration) for complete configuration reference.

---

## Recommendations

1. **Start with immutable fakes** — they are the safe default for the vast majority of tests
2. **Use mutable fakes sparingly** — only when a single test truly needs mid-test behavior changes
3. **Prefer separate immutable fakes** over mutable reconfiguration when testing multiple scenarios
4. **Avoid concurrent `configure {}` calls** — use mutable fakes in sequential test phases only
5. **Combine with call history** when you need to verify interactions across reconfiguration boundaries

---

## Next Steps

- **[Plugin Configuration](plugin-configuration.md#mutable-fakes-configuration)** - Enable mutable fakes project-wide or per-interface
- **[Testing Patterns](testing-patterns.md#mid-test-reconfiguration-mutable-fakes)** - Practical testing patterns for mutable fakes
- **[Generated Code Reference](generated-code-reference.md#generated-code-with-mutable-fakes)** - Understanding the generated `configure {}` method
- **[Usage Guide](usage.md#mutable-fakes-opt-in)** - Quick start with mutable fakes
