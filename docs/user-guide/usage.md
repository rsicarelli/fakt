# Usage Guide

Comprehensive reference for using Fakt-generated fakes in your tests. This guide covers everything from basic patterns to advanced features like coroutines, generics, and call history.

---

## Quick Start

The most common use case—a simple interface with methods:

```kotlin
// src/commonMain/kotlin/com/example/Analytics.kt
import com.rsicarelli.fakt.Fake

@Fake
interface Analytics {
    fun track(event: String)
    fun identify(userId: String)
}
```

**Using in tests:**

```kotlin
// src/commonTest/kotlin/com/example/AnalyticsTest.kt
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsTest {
    @Test
    fun `GIVEN fake analytics WHEN tracking events THEN captures calls`() {
        val events = mutableListOf<String>()

        val fake = fakeAnalytics {
            track { event -> events.add(event) }
            identify { userId -> println("User: $userId") }
        }

        fake.track("user_signup")
        fake.track("user_login")
        fake.identify("user-123")

        assertEquals(listOf("user_signup", "user_login"), events)
        assertEquals(2, fake.trackCalls.value.size)
        assertEquals(1, fake.identifyCalls.value.size)
    }
}
```

---

## Return Types & Default Behaviors

### Return Values

Configure return values for methods:

```kotlin
@Fake
interface UserRepository {
    fun getUser(id: String): User?
    fun getAllUsers(): List<User>
    fun count(): Int
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN configured repository WHEN fetching users THEN returns configured values`() {
    val testUser = User("123", "Alice")

    val fake = fakeUserRepository {
        getUser { id ->
            if (id == "123") testUser else null
        }
        getAllUsers { listOf(testUser) }
        count { 1 }
    }

    assertEquals(testUser, fake.getUser("123"))
    assertNull(fake.getUser("456"))
    assertEquals(1, fake.getAllUsers().size)
    assertEquals(1, fake.count())
}
```

---

### Smart Defaults

Fakt generates smart defaults. You only configure what you need:

```kotlin
@Fake
interface Settings {
    fun getTheme(): String
    fun getFontSize(): Int
    fun isEnabled(): Boolean
}
```

**Using defaults:**

```kotlin
@Test
fun `GIVEN unconfigured fake WHEN calling methods THEN uses defaults`() {
    val fake = fakeSettings()  // No configuration

    assertEquals("", fake.getTheme())      // String default: ""
    assertEquals(0, fake.getFontSize())    // Int default: 0
    assertEquals(false, fake.isEnabled())  // Boolean default: false
}
```

**Override defaults:**

```kotlin
@Test
fun `GIVEN configured fake WHEN calling methods THEN uses custom behavior`() {
    val fake = fakeSettings {
        getTheme { "dark" }
        isEnabled { true }
        // getFontSize not configured, uses default: 0
    }

    assertEquals("dark", fake.getTheme())
    assertEquals(true, fake.isEnabled())
    assertEquals(0, fake.getFontSize())  // Default
}
```

---

### Nullable Types

Handle nullable types naturally with `null` defaults:

```kotlin
@Fake
interface UserService {
    fun findUser(id: String): User?
    fun findByEmail(email: String): User?
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN user service fake WHEN finding users THEN handles nulls correctly`() {
    val testUser = User("123", "Alice")

    val fake = fakeUserService {
        findUser { id ->
            if (id == "123") testUser else null
        }
        // findByEmail not configured, default: null
    }

    assertNotNull(fake.findUser("123"))
    assertNull(fake.findUser("456"))
    assertNull(fake.findByEmail("alice@example.com"))  // Default: null
}
```

---

### Result Types

Fakt handles `Result<T>` with sensible defaults:

```kotlin
@Fake
interface ApiClient {
    fun fetchData(id: String): Result<Data>
    fun upload(data: Data): Result<Unit>
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN API client fake WHEN fetching data THEN returns configured Result`() {
    val testData = Data("123", "content")

    val fake = fakeApiClient {
        fetchData { id ->
            if (id == "123") Result.success(testData)
            else Result.failure(NotFoundException())
        }
        upload { data ->
            Result.success(Unit)
        }
    }

    val result1 = fake.fetchData("123")
    assertTrue(result1.isSuccess)
    assertEquals(testData, result1.getOrNull())

    val result2 = fake.fetchData("456")
    assertTrue(result2.isFailure)

    val result3 = fake.upload(testData)
    assertTrue(result3.isSuccess)
}
```

---

### Collection Types

Smart defaults for collections:

```kotlin
@Fake
interface Repository {
    fun getAll(): List<Item>
    fun getTags(): Set<String>
    fun getMetadata(): Map<String, String>
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN repository fake WHEN fetching collections THEN uses defaults or configured values`() {
    val fake = fakeRepository {
        getAll { listOf(Item("1"), Item("2")) }
        // getTags not configured, default: emptySet()
        // getMetadata not configured, default: emptyMap()
    }

    assertEquals(2, fake.getAll().size)
    assertTrue(fake.getTags().isEmpty())      // Default
    assertTrue(fake.getMetadata().isEmpty())  // Default
}
```

---

### Unit Return Type

Methods returning `Unit` get no-op defaults:

```kotlin
@Fake
interface EventBus {
    fun publish(event: Event)
    fun subscribe(handler: EventHandler)
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN event bus fake WHEN publishing events THEN executes configured behavior`() {
    val receivedEvents = mutableListOf<Event>()

    val fake = fakeEventBus {
        publish { event -> receivedEvents.add(event) }
        // subscribe not configured, default: { } (no-op)
    }

    fake.publish(Event("test-event"))
    fake.subscribe(mockHandler)  // No-op default

    assertEquals(1, receivedEvents.size)
    assertEquals(1, fake.publishCalls.value.size)
    assertEquals(1, fake.subscribeCalls.value.size)
}
```

---

## Method Parameters

Methods with multiple parameters work as expected:

```kotlin
@Fake
interface Calculator {
    fun add(a: Int, b: Int): Int
    fun divide(numerator: Double, denominator: Double): Double
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN calculator fake WHEN calling methods THEN uses provided parameters`() {
    val fake = fakeCalculator {
        add { a, b -> a + b }
        divide { numerator, denominator ->
            if (denominator == 0.0) Double.NaN
            else numerator / denominator
        }
    }

    assertEquals(5, fake.add(2, 3))
    assertEquals(2.5, fake.divide(5.0, 2.0))
    assertTrue(fake.divide(10.0, 0.0).isNaN())
}
```

---

## Suspend Functions

Fakt fully supports Kotlin coroutines and suspend functions without any special configuration.

### Basic Suspend Functions

Suspend functions work naturally in generated fakes:

```kotlin
@Fake
interface ApiClient {
    suspend fun fetchData(id: String): Result<Data>
    suspend fun upload(data: Data): Result<Unit>
}
```

**Usage in tests:**

```kotlin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ApiClientTest {
    @Test
    fun `GIVEN API client fake WHEN fetching data THEN suspends correctly`() = runTest {
        val testData = Data("123", "content")

        val fake = fakeApiClient {
            fetchData { id ->
                delay(100)  // Suspends correctly
                Result.success(testData)
            }
            upload { data ->
                delay(50)
                Result.success(Unit)
            }
        }

        val result = fake.fetchData("123")

        assertTrue(result.isSuccess)
        assertEquals(testData, result.getOrNull())
        assertEquals(1, fake.fetchDataCalls.value.size)
    }
}
```

---

### Suspend + Non-Suspend Mix

Interfaces can mix suspend and regular functions:

```kotlin
@Fake
interface UserRepository {
    fun getLocalUser(id: String): User?
    suspend fun fetchRemoteUser(id: String): Result<User>
    suspend fun syncUsers(): Result<Unit>
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN repository WHEN calling mixed functions THEN works correctly`() = runTest {
    val fake = fakeUserRepository {
        getLocalUser { id -> User(id, "Local") }
        fetchRemoteUser { id ->
            delay(100)
            Result.success(User(id, "Remote"))
        }
        syncUsers {
            delay(200)
            Result.success(Unit)
        }
    }

    // Regular function (no suspend)
    val local = fake.getLocalUser("123")
    assertEquals("Local", local?.name)

    // Suspend functions
    val remote = fake.fetchRemoteUser("456")
    val syncResult = fake.syncUsers()

    assertTrue(remote.isSuccess)
    assertTrue(syncResult.isSuccess)
}
```

---

### Suspend Properties

Properties with suspend getters are supported:

```kotlin
@Fake
interface AsyncConfig {
    suspend fun loadConfig(): Map<String, String>
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN async config WHEN loading THEN suspends correctly`() = runTest {
    val fake = fakeAsyncConfig {
        loadConfig {
            delay(50)
            mapOf("key" to "value")
        }
    }

    val config = fake.loadConfig()

    assertEquals("value", config["key"])
}
```

---

### Exception Handling

Suspend functions can throw exceptions naturally:

```kotlin
@Test
fun `GIVEN API client WHEN fetch fails THEN throws exception`() = runTest {
    val fake = fakeApiClient {
        fetchData { id ->
            delay(100)
            throw NetworkException("Connection failed")
        }
    }

    assertFailsWith<NetworkException> {
        fake.fetchData("123")
    }
}
```

---

### Coroutine Context

Fakt-generated fakes work with all coroutine contexts:

```kotlin
@Test
fun `GIVEN repository WHEN using different dispatchers THEN works correctly`() = runTest {
    val fake = fakeRepository {
        fetchData { id ->
            withContext(Dispatchers.Default) {
                // Computation
                Data(id)
            }
        }
    }

    val data = fake.fetchData("123")
    assertEquals("123", data.id)
}
```

---

### Best Practices for Suspend Functions

#### Use runTest for Suspend Tests

Always wrap suspend function tests in `runTest`:

```kotlin
@Test
fun `test suspend function`() = runTest {  // ✅ Required
    val fake = fakeApiClient()
    fake.fetchData("123")
}
```

#### Use delay() for Testing Timing

Test timing-sensitive code with `delay()`:

```kotlin
@Test
fun `GIVEN slow API WHEN fetching THEN handles timeout`() = runTest {
    val fake = fakeApiClient {
        fetchData { id ->
            delay(5000)  // Simulate slow response
            Result.success(Data(id))
        }
    }

    withTimeout(1000) {
        assertFailsWith<TimeoutCancellationException> {
            fake.fetchData("123")
        }
    }
}
```

---

## Properties

Fakt generates fakes for both read-only (`val`) and mutable (`var`) properties with automatic call history.

### Read-Only Properties (val)

```kotlin
@Fake
interface Config {
    val apiUrl: String
    val timeout: Int
}

val fake = fakeConfig {
    apiUrl { "https://api.example.com" }
    timeout { 30 }
}

assertEquals("https://api.example.com", fake.apiUrl)
assertEquals(1, fake.apiUrlCalls.value.size)
```

---

### Mutable Properties (var)

Mutable properties track both getter and setter calls separately:

```kotlin
@Fake
interface Settings {
    var theme: String
    var fontSize: Int
}

val fake = fakeSettings {
    theme { "dark" }
    fontSize { 14 }
}

// Getter tracking
assertEquals("dark", fake.theme)
assertEquals(1, fake.getThemeCalls.value.size)

// Setter tracking
fake.theme = "light"
assertEquals(1, fake.setThemeCalls.value.size)
```

---

## Generics

Fakt fully supports generic type parameters at both class and method levels.

### Class-Level Generics

```kotlin
@Fake
interface Repository<T> {
    fun save(item: T): Result<Unit>
    fun getAll(): List<T>
}

val fake = fakeRepository<User> {
    save { item -> Result.success(Unit) }
    getAll { emptyList() }
}
```

---

### Method-Level Generics

```kotlin
@Fake
interface Transformer {
    fun <T, R> transform(input: T, mapper: (T) -> R): R
}

val fake = fakeTransformer {
    transform { input, mapper -> mapper(input) }
}
```

---

### Generic Constraints

```kotlin
@Fake
interface ComparableRepository<T : Comparable<T>> {
    fun findMax(items: List<T>): T?
}

val fake = fakeComparableRepository<Int> {
    findMax { items -> items.maxOrNull() }
}
```

---

### Variance

Fakt supports variance modifiers (`out`, `in`):

```kotlin
@Fake
interface Producer<out T> {
    fun produce(): T
}

@Fake
interface Consumer<in T> {
    fun consume(item: T)
}
```

---

## Call History & Verification

Every Fakt-generated fake includes automatic, thread-safe call history with a powerful verification DSL.

### Basic Call History

Every method automatically tracks calls:

```kotlin
@Fake
interface Logger {
    fun log(message: String)
    fun error(message: String)
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN fake logger WHEN logging messages THEN tracks calls`() {
    val fake = fakeLogger {
        log { message -> println(message) }
        error { message -> System.err.println(message) }
    }

    fake.log("Info message")
    fake.log("Another info")
    fake.error("Error occurred")

    assertEquals(2, fake.logCalls.value.size)
    assertEquals(1, fake.errorCalls.value.size)
}
```

---

### Verification DSL

Fakt generates a scoped verification DSL for each method, providing expressive assertions:

```kotlin
@Test
fun `GIVEN repository WHEN saving users THEN verifies call history`() {
    val fake = fakeUserRepository {
        save { user -> user }
    }

    fake.save(User("1", "Alice"))
    fake.save(User("2", "Bob"))

    fake.verifySave {
        assertTrue(wasCalledTimes(2))
        assertTrue(wasCalledWith(User("1", "Alice")))
        assertEquals("1", first.user.id)
        assertEquals(2, all.size)
    }
}
```

**Verifier Methods:**

| Method | Description |
|--------|-------------|
| `wasCalledTimes(n)` | Returns `true` if method was called exactly `n` times |
| `wasCalledWith(...)` | Returns `true` if method was called with specified arguments |
| `wasNeverCalled()` | Returns `true` if method was never called |
| `wasCalledInOrder(...)` | Returns `true` if single-param method was called in specified order |
| `neverCalledWith(...)` | Returns `true` if single-param method was never called with value |
| `first` | First call's data (throws if no calls) |
| `lastOrNull` | Last call's data, or `null` if no calls |
| `all` | List of all call data objects |

---

### Call History Data Classes

For each method, Fakt generates a data class capturing all parameters:

```kotlin
// For: fun save(user: User, validate: Boolean): User
data class FakeUserRepositorySaveCall(
    val user: User,
    val validate: Boolean
)

// Access in verification:
fake.verifySave {
    assertEquals("Alice", first.user.name)
    assertTrue(first.validate)
    assertEquals(2, all.size)
}
```

**Zero-Parameter Methods:**

Methods without parameters still track calls:

```kotlin
assertEquals(2, fake.clearCalls.value.size)
fake.verifyClear {
    assertTrue(wasCalledTimes(2))
}
```

---

### Call Order Verification

For methods with a single parameter, verify call order:

```kotlin
@Fake
interface Analytics {
    fun track(event: String)
}

@Test
fun `GIVEN analytics WHEN tracking events THEN verifies order`() {
    val fake = fakeAnalytics()

    fake.track("page_view")
    fake.track("button_click")
    fake.track("purchase")

    fake.verifyTrack {
        assertTrue(wasCalledInOrder("page_view", "button_click", "purchase"))
        assertTrue(neverCalledWith("error"))
    }
}
```

---

### Property Call History

Properties track both getter and setter calls:

```kotlin
@Fake
interface Settings {
    var theme: String
}

val fake = fakeSettings {
    theme { "dark" }
}

val _ = fake.theme  // Getter
assertEquals(1, fake.getThemeCalls.value.size)

fake.theme = "light"  // Setter
assertEquals(1, fake.setThemeCalls.value.size)
```

---

### Thread Safety

Call history is backed by thread-safe internal state. All tracking operations are safe for concurrent access:

```kotlin
@Test
fun `GIVEN fake WHEN calling from multiple threads THEN tracks correctly`() = runTest {
    val fake = fakeAnalytics()

    withContext(Dispatchers.Default) {
        repeat(1000) {
            launch {
                fake.track("event")
            }
        }
    }

    assertEquals(1000, fake.trackCalls.value.size)
}
```

---

### Configuring Call History

By default, Fakt generates full call history for every fake. You can disable this for lightweight fakes that don't need verification.

#### Project-Wide Default

Configure the default for all fakes in your project:

```kotlin
// build.gradle.kts
fakt {
    enableCallHistory.set(false)  // Disable for all fakes by default
}
```

#### Per-Interface Override

Override the project default for specific interfaces:

```kotlin
import com.rsicarelli.fakt.CallHistoryMode

// Disable call history for this interface (lightweight fake)
@Fake(callHistory = CallHistoryMode.DISABLED)
interface Logger {
    fun log(message: String)
}

// Enable call history (even if plugin default is disabled)
@Fake(callHistory = CallHistoryMode.ENABLED)
interface PaymentService {
    fun processPayment(amount: Double): Result<Receipt>
}

// Follow plugin default
@Fake  // Same as @Fake(callHistory = CallHistoryMode.DEFAULT)
interface UserService { ... }
```

#### When to Disable Call History

Disable call history when:

- Fakes are only used for stubbing, not verification
- You want smaller generated code
- Migrating legacy tests that don't need call history

Enable call history when:

- You need to verify method calls (`wasCalledTimes`, `wasCalledWith`)
- Migrating from mocking frameworks (MockK, Mockito)
- Testing interaction patterns

#### What Changes

**With call history enabled (default):**

```kotlin
val fake = fakeLogger()
fake.log("message")

assertEquals(1, fake.logCalls.value.size)  // Available
fake.verifyLog {                     // Available
    assertTrue(wasCalledWith("message"))
}
```

**With call history disabled:**

```kotlin
val fake = fakeLogger()
fake.log("message")

// fake.logCalls             // Not generated
// fake.verifyLog { ... }    // Not generated
```

---

## Advanced Patterns

### Inheritance

Fakt handles inherited methods correctly:

```kotlin
interface BaseService {
    fun start(): Boolean
    fun stop(): Boolean
}

@Fake
interface UserService : BaseService {
    fun getUser(id: String): User
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN service fake WHEN calling inherited methods THEN works correctly`() {
    val fake = fakeUserService {
        start { true }
        stop { true }
        getUser { id -> User(id, "Alice") }
    }

    assertTrue(fake.start())
    assertEquals("Alice", fake.getUser("123").name)
    assertTrue(fake.stop())

    assertEquals(1, fake.startCalls.value.size)
    assertEquals(1, fake.getUserCalls.value.size)
    assertEquals(1, fake.stopCalls.value.size)
}
```

---

### Immutable Fakes (Default)

Fakt-generated fakes are **immutable after construction** by default. To use different behavior, create a new fake:

```kotlin
@Test
fun `GIVEN different scenarios WHEN needing different behavior THEN create new fakes`() {
    // Scenario 1: User found
    val fakeFound = fakeUserRepository {
        getUser { id -> User(id, "Alice") }
    }
    assertEquals("Alice", fakeFound.getUser("123").name)

    // Scenario 2: User not found
    val fakeNotFound = fakeUserRepository {
        getUser { id -> null }
    }
    assertNull(fakeNotFound.getUser("123"))
}
```

!!! tip "Why Immutable by Default?"
    Immutable fakes are safer: no shared mutable state between tests, no accidental mid-test reconfiguration, and predictable behavior from construction to completion. This is the recommended approach for unit tests.

---

### Mutable Fakes (Opt-In)

For integration tests where behavior needs to change mid-test, Fakt supports **mutable fakes**:

```kotlin
import com.rsicarelli.fakt.MutabilityMode

@Fake(mutability = MutabilityMode.MUTABLE)
interface UserRepository {
    fun findById(id: String): User?
    suspend fun save(user: User): User
    val status: String
}
```

Mutable fakes include a `modify {}` method for selective reconfiguration:

```kotlin
@Test
fun `GIVEN repository WHEN database fails mid-test THEN handles failure gracefully`() {
    // GIVEN: A mutable repository that initially succeeds
    val fake = fakeUserRepository {
        findById { id -> User(id, "Alice") }
        status { "active" }
    }

    // Initial behavior works
    assertEquals("Alice", fake.findById("1")?.name)

    // WHEN: Simulating database failure mid-test
    fake.modify {
        findById { null }  // Now returns null
    }

    // THEN: New behavior applies, non-reconfigured behaviors unchanged
    assertNull(fake.findById("1"))
    assertEquals("active", fake.status)  // Unchanged
}
```

!!! note "Selective Reconfiguration"
    The `modify {}` method only updates the behaviors you specify. All other behaviors remain unchanged from their previous configuration.

!!! warning "Use Judiciously"
    Mutable fakes are designed for integration tests where the same injected fake needs to simulate state changes. For unit tests, prefer immutable fakes with separate instances per scenario.

**Learn more:** [Immutable vs Mutable](immutable-vs-mutable.md) for an in-depth comparison, and [Plugin Configuration](plugin-configuration.md#mutable-fakes-configuration) for enabling mutable fakes project-wide.

---

## Next Steps

**Learn More:**
- **[Testing Patterns](testing-patterns.md)** - GIVEN-WHEN-THEN, isolated fakes, verification strategies
- **[Multi-Module](multi-module.md)** - Cross-module fake sharing with collector modules
- **[Migration from Mocks](migration-from-mocks.md)** - Migrating from MockK or Mockito
- **[Performance](performance.md)** - Build performance and optimization

**Advanced Configuration:**
- **[Immutable vs Mutable](immutable-vs-mutable.md)** - In-depth comparison of immutable and mutable fakes
- **[Plugin Configuration](plugin-configuration.md)** - Compiler plugin configuration and log levels
- **[Generated Code Reference](generated-code-reference.md)** - Understanding generated fake implementations
- **[Platform Support](platform-support.md)** - KMP target support and platform-specific patterns
