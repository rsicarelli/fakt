# API Reference

Generated code API and patterns.

---

## Generated Classes

For each `@Fake` annotated interface, Fakt generates several components:

### Implementation Class

```kotlin
class Fake{Interface}Impl : {Interface} {
    // Call counters (derived from history)
    val {method}CallCount: Int

    // Call history
    val {method}CallHistory: List<{Interface}{Method}Call>

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

## Call History Data Classes

!!! note "Call History is Configurable"
    The classes and properties described in this section are only generated when call history is enabled. See [Plugin Configuration](plugin-configuration.md#call-history-configuration) for details.

For each method with parameters, Fakt generates a data class capturing all arguments:

```kotlin
// For interface method: fun save(user: User, validate: Boolean): User
data class FakeUserRepositorySaveCall(
    val user: User,
    val validate: Boolean
)

// For interface method: fun getUser(id: String): User?
data class FakeUserRepositoryGetUserCall(
    val id: String
)
```

**Naming Pattern:** `Fake{Interface}{Method}Call`

These data classes enable type-safe access to call history:

```kotlin
val fake = fakeUserRepository {
    save { user, _ -> user }
}

fake.save(User("1", "Alice"), true)
fake.save(User("2", "Bob"), false)

// Access call history
assertEquals(2, fake.saveCallHistory.size)
assertEquals("Alice", fake.saveCallHistory[0].user.name)
assertTrue(fake.saveCallHistory[0].validate)
```

---

## Verifier Classes

For each method, Fakt generates a verifier class with assertion helpers:

```kotlin
class Fake{Interface}{Method}Verifier(
    private val history: List<Fake{Interface}{Method}Call>
) {
    // Assertion methods
    fun wasCalledTimes(n: Int): Boolean
    fun wasCalledWith({params}): Boolean
    fun wasNeverCalled(): Boolean

    // For single-parameter methods only:
    fun wasCalledInOrder(vararg values: {ParamType}): Boolean
    fun neverCalledWith(value: {ParamType}): Boolean

    // History access
    val first: Fake{Interface}{Method}Call
    val lastOrNull: Fake{Interface}{Method}Call?
    val all: List<Fake{Interface}{Method}Call>
}
```

**Verifier API Reference:**

| Method | Description |
|--------|-------------|
| `wasCalledTimes(n)` | Returns `true` if called exactly `n` times |
| `wasCalledWith(...)` | Returns `true` if called with specified arguments |
| `wasNeverCalled()` | Returns `true` if never called |
| `wasCalledInOrder(...)` | Returns `true` if called in order (single-param only) |
| `neverCalledWith(value)` | Returns `true` if never called with value (single-param only) |
| `first` | First call data (throws `NoSuchElementException` if empty) |
| `lastOrNull` | Last call data, or `null` if no calls |
| `all` | Complete list of call data objects |

---

## Verify Extension Functions

For each method, Fakt generates a scoped verification extension:

```kotlin
inline fun Fake{Interface}Impl.verify{Method}(
    block: Fake{Interface}{Method}Verifier.() -> Unit
)
```

**Usage:**

```kotlin
fake.verifySave {
    assertTrue(wasCalledTimes(2))
    assertTrue(wasCalledWith(User("1", "Alice"), true))
    assertEquals("Alice", first.user.name)
}

fake.verifyTrack {
    assertTrue(wasCalledInOrder("page_view", "button_click"))
    assertTrue(neverCalledWith("error"))
}
```

---

## Generated Code with Call History Disabled

When call history is disabled via `enableCallHistory.set(false)` or `@Fake(callHistory = CallHistoryMode.DISABLED)`, generated fakes are simplified:

**Generated:**

- `Fake{Interface}Impl` - Implementation class
- `fake{Interface}()` - Factory function
- `Fake{Interface}Config` - Configuration DSL

**Not Generated:**

- `{method}CallCount` properties
- `{method}CallHistory` lists
- `Fake{Interface}{Method}Call` data classes
- `Fake{Interface}{Method}Verifier` classes
- `verify{Method}` extension functions

This results in smaller, simpler generated code for fakes that only need stubbing.

---

## Naming Conventions

| Element                | Pattern                           | Example                           |
|------------------------|-----------------------------------|-----------------------------------|
| Implementation class   | `Fake{Interface}Impl`             | `FakeAnalyticsImpl`               |
| Factory function       | `fake{Interface}`                 | `fakeAnalytics`                   |
| Configuration DSL      | `Fake{Interface}Config`           | `FakeAnalyticsConfig`             |
| Call counter           | `{method}CallCount`               | `trackCallCount`                  |
| Call history           | `{method}CallHistory`             | `trackCallHistory`                |
| Call data class        | `Fake{Interface}{Method}Call`     | `FakeAnalyticsTrackCall`          |
| Verifier class         | `Fake{Interface}{Method}Verifier` | `FakeAnalyticsTrackVerifier`      |
| Verify function        | `verify{Method}`                  | `verifyTrack`                     |
| Configuration method   | `{method}`                        | `track { }`                       |

---

## Package Structure

Generated fakes are in the same package as the annotated interface:

```
com.example.services.Analytics (@Fake)
→ com.example.services.FakeAnalyticsImpl
→ com.example.services.fakeAnalytics()
→ com.example.services.FakeAnalyticsConfig
→ com.example.services.FakeAnalyticsTrackCall
→ com.example.services.FakeAnalyticsTrackVerifier
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
