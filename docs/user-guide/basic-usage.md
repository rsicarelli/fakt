# Basic Usage

Learn the fundamentals of using Fakt-generated fakes in your tests.

---

## Simple Interface

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
        assertEquals(2, fake.trackCallCount.value)
        assertEquals(1, fake.identifyCallCount.value)
    }
}
```

---

## Return Values

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

## Default Behaviors

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

## Call Tracking

Every method automatically tracks calls via StateFlow:

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
fun `GIVEN fake logger WHEN logging messages THEN tracks call counts`() {
    val fake = fakeLogger {
        log { message -> println(message) }
        error { message -> System.err.println(message) }
    }

    fake.log("Info message")
    fake.log("Another info")
    fake.error("Error occurred")

    assertEquals(2, fake.logCallCount.value)
    assertEquals(1, fake.errorCallCount.value)
}
```

---

## Multiple Parameters

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

## Nullable Return Types

Handle nullable types naturally:

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

## Result Types

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

## Collection Return Types

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

## Unit Return Type

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
    assertEquals(1, fake.publishCallCount.value)
    assertEquals(1, fake.subscribeCallCount.value)
}
```

---

## Inheritance

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

    assertEquals(1, fake.startCallCount.value)
    assertEquals(1, fake.getUserCallCount.value)
    assertEquals(1, fake.stopCallCount.value)
}
```

---

## Reconfiguring Fakes

You can reconfigure behavior mid-test if needed:

```kotlin
@Test
fun `GIVEN fake WHEN reconfiguring behavior THEN uses new behavior`() {
    val fake = fakeUserRepository()

    // Initial configuration
    fake.configureGetUser { id -> User(id, "Alice") }
    assertEquals("Alice", fake.getUser("123").name)

    // Reconfigure
    fake.configureGetUser { id -> User(id, "Bob") }
    assertEquals("Bob", fake.getUser("123").name)
}
```

!!! warning "Advanced Usage"
    Reconfiguring via `configureXxx()` methods is an advanced pattern. Prefer creating new fakes for different test scenarios.

---

## Next Steps

**Learn More:**
- **[Suspend Functions](suspend-functions.md)** - Async/coroutine support
- **[Generics](generics.md)** - Generic type handling
- **[Properties](properties.md)** - val/var faking
- **[Call Tracking](call-tracking.md)** - Advanced StateFlow patterns

**Best Practices:**
- **[Testing Patterns](testing-patterns.md)** - GIVEN-WHEN-THEN, isolated fakes, verification strategies
