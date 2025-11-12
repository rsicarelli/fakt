# Features

Comprehensive overview of what Fakt supports and how it works.

---

## Supported Class Types

Fakt can generate fakes for these Kotlin types:

### ✅ Interfaces

The primary use case—clean contracts without implementation:

```kotlin
@Fake
interface UserRepository {
    suspend fun getUser(id: String): User
    fun deleteUser(id: String): Boolean
}
```

### ✅ Abstract Classes

Abstract classes with abstract members:

```kotlin
@Fake
abstract class BaseService {
    abstract fun start(): Boolean
    abstract val isRunning: Boolean
}
```

### ✅ Open Classes

Open classes with overridable members (only overridable members are faked):

```kotlin
@Fake
open class NetworkClient {
    open suspend fun fetch(url: String): Result<String> =
        Result.failure(NotImplementedError())
}
```

### ❌ Data Classes

Data classes have compiler-generated implementations and can't be faked. Use builders or `copy()` instead.

**Works as parameter/return types:**

```kotlin
data class User(val id: String, val name: String)

@Fake  // ✅ This works
interface UserRepository {
    fun getUser(id: String): User  // ✅ Data class as return type
}
```

### ❌ Sealed Classes/Interfaces

Sealed hierarchies can't be faked directly. Use exhaustive when-expressions or visitor patterns.

**Works as parameter/return types:**

```kotlin
sealed interface Result<out T>
data class Success<T>(val value: T) : Result<T>
data class Failure(val error: Throwable) : Result<Nothing>

@Fake  // ✅ This works
interface Repository {
    fun save(data: String): Result<Unit>  // ✅ Sealed class as return type
}
```

---

## Type System Support

### ✅ Generics

Full generic support with smart defaults:

**Class-level generics:**

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

**Method-level generics:**

```kotlin
@Fake
interface Transformer {
    fun <T, R> transform(input: T, mapper: (T) -> R): R
}

val fake = fakeTransformer {
    transform { input, mapper -> mapper(input) }  // Identity function default
}
```

**Generic constraints:**

```kotlin
@Fake
interface ComparableRepository<T : Comparable<T>> {
    fun findMax(items: List<T>): T?
}

val fake = fakeComparableRepository<Int> {
    findMax { items -> items.maxOrNull() }
}
```

**Variance:**

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

### ✅ Nullable Types

Full support for nullable types with `null` defaults:

```kotlin
@Fake
interface UserService {
    fun findUser(id: String): User?
    suspend fun getCurrentUser(): User?
}

val fake = fakeUserService {
    findUser { id -> null }  // Default: null
    getCurrentUser { User("123", "Alice") }
}
```

---

### ✅ Complex Standard Library Types

Fakt handles common stdlib types with smart defaults:

| Type                     | Default Behavior                         |
|--------------------------|------------------------------------------|
| `Result<T>`              | `Result.failure(NotImplementedError())`  |
| `List<T>`                | `emptyList()`                            |
| `Set<T>`                 | `emptySet()`                             |
| `Map<K, V>`              | `emptyMap()`                             |
| `Sequence<T>`            | `emptySequence()`                        |
| `Array<T>`               | `emptyArray()`                           |
| `Pair<A, B>`             | `Pair(defaultA, defaultB)`               |
| `Triple<A, B, C>`        | `Triple(defaultA, defaultB, defaultC)`   |

**Example:**

```kotlin
@Fake
interface DataRepository {
    suspend fun fetchItems(): Result<List<Item>>
    fun getCache(): Map<String, Item>
}

val fake = fakeDataRepository {
    fetchItems { Result.success(listOf(Item("test"))) }
    getCache { emptyMap() }  // Default
}
```

---

## Kotlin Language Features

### ✅ Suspend Functions

Full coroutine support—no weird `runBlocking` wrappers:

```kotlin
@Fake
interface ApiClient {
    suspend fun login(username: String, password: String): Result<Token>
    suspend fun fetchData(): List<Data>
}

val fake = fakeApiClient {
    login { username, password ->
        delay(100)  // Suspends correctly
        Result.success(Token("fake-token"))
    }
    fetchData {
        delay(50)
        emptyList()
    }
}

// Use in tests
runTest {
    val result = fake.login("alice", "pass123")
    assertTrue(result.isSuccess)
}
```

---

### ✅ Properties

Both read-only (`val`) and mutable (`var`) properties:

**Read-only properties:**

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
assertEquals(1, fake.apiUrlCallCount.value)  // Call tracking
```

**Mutable properties:**

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
assertEquals(1, fake.getThemeCallCount.value)

// Setter tracking
fake.theme = "light"
assertEquals(1, fake.setThemeCallCount.value)
```

---

### ✅ Default Parameters

Methods with default parameters are supported (defaults are ignored):

```kotlin
@Fake
interface Logger {
    fun log(message: String, level: LogLevel = LogLevel.INFO)
}

val fake = fakeLogger {
    log { message, level ->
        println("[$level] $message")
    }
}

fake.log("Error occurred", LogLevel.ERROR)
fake.log("Info message")  // Uses interface's default
```

---

### ✅ Inheritance

Fakt handles inheritance correctly:

```kotlin
interface BaseRepository {
    fun getId(): String
}

@Fake
interface UserRepository : BaseRepository {
    suspend fun getUser(id: String): User
}

val fake = fakeUserRepository {
    getId { "repo-123" }  // Inherited method
    getUser { id -> User(id, "Alice") }
}
```

---

## Call Tracking

### Built-In StateFlow Counters

Every method and property gets automatic call tracking:

```kotlin
@Fake
interface Analytics {
    fun track(event: String)
    suspend fun identify(userId: String)
    val sessionId: String
}

val fake = fakeAnalytics {
    track { event -> println(event) }
    identify { userId -> }
    sessionId { "session-123" }
}

fake.track("event1")
fake.track("event2")

// Thread-safe call counting
assertEquals(2, fake.trackCallCount.value)
assertEquals(0, fake.identifyCallCount.value)

// Property tracking
val id = fake.sessionId
assertEquals(1, fake.sessionIdCallCount.value)
```

---

### Reactive Testing

StateFlow counters work with Kotlin Flow test utilities:

```kotlin
import app.cash.turbine.test

@Test
fun `GIVEN fake WHEN calling method THEN counter updates reactively`() = runTest {
    val fake = fakeRepository()

    fake.getUserCallCount.test {
        assertEquals(0, awaitItem())

        fake.getUser("123")
        assertEquals(1, awaitItem())

        fake.getUser("456")
        assertEquals(2, awaitItem())
    }
}
```

---

## Code Generation Patterns

### Smart Default Behaviors

Fakt generates sensible defaults based on return type:

| Return Type         | Generated Default                        |
|---------------------|------------------------------------------|
| `Unit`              | `{ }`                                    |
| `Boolean`           | `{ false }`                              |
| `Int`, `Long`, etc. | `{ 0 }`                                  |
| `Double`, `Float`   | `{ 0.0 }`                                |
| `String`            | `{ "" }`                                 |
| `List<T>`           | `{ emptyList() }`                        |
| `T?` (nullable)     | `{ null }`                               |
| `T -> T` (identity) | `{ it }`                                 |
| `Result<T>`         | `{ Result.failure(NotImplementedError)}` |

---

### Configuration DSL

Every fake gets a type-safe DSL for configuration:

```kotlin
@Fake
interface UserRepository {
    suspend fun getUser(id: String): Result<User>
    suspend fun saveUser(user: User): Result<Unit>
}

// Generated DSL
val fake = fakeUserRepository {
    getUser { id ->  // Type-safe lambda
        Result.success(User(id, "Alice"))
    }
    saveUser { user ->  // Compiler knows parameter types
        Result.success(Unit)
    }
}
```

---

### Factory Functions

Generated factory functions follow Kotlin conventions:

```kotlin
// For interface UserRepository:
fun fakeUserRepository(
    configure: FakeUserRepositoryConfig.() -> Unit = {}
): FakeUserRepositoryImpl

// For interface ApiClient:
fun fakeApiClient(
    configure: FakeApiClientConfig.() -> Unit = {}
): FakeApiClientImpl
```

Naming: `fake{InterfaceName}` (camelCase from interface name)

---

## Multi-Module Support (Experimental)

Cross-module fake consumption via dedicated `-fakes` modules:

```kotlin
// Producer module: :core:analytics
@Fake
interface Analytics

// Collector module: :core:analytics-fakes
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}

// Consumer module: :app
dependencies {
    commonTest {
        implementation(projects.core.analyticsFakes)
    }
}
```

**For complete documentation**, see [Multi-Module Overview](../multi-module/index.md).

---

## Performance Features

### Intelligent Caching

Fakt caches generated code across KMP targets:

- **First target**: Full generation (~40ms for 100 interfaces)
- **Subsequent targets**: Cache hits (~1ms each)

### Telemetry Configuration

Four log levels for debugging and performance analysis:

```kotlin
import com.rsicarelli.fakt.compiler.api.LogLevel

fakt {
    logLevel.set(LogLevel.INFO)   // Default: concise summary
    // LogLevel.QUIET  - Zero output (CI/CD)
    // LogLevel.DEBUG  - Detailed breakdown with FIR + IR details
}
```

See [Performance Guide](../guides/performance.md) for benchmarks.

---

## Platform Support

Works on ALL Kotlin Multiplatform targets:

- ✅ JVM, Android
- ✅ iOS (arm64, x64, simulator)
- ✅ macOS, Linux, Windows
- ✅ JavaScript (IR), WebAssembly
- ✅ watchOS, tvOS

Single-platform projects (JVM-only, Android-only) are fully supported.

---

## Next Steps

- [Basic Usage](../usage/basic-usage.md) - Get started with fakes
- [Suspend Functions](../usage/suspend-functions.md) - Async patterns
- [Generics](../usage/generics.md) - Generic type handling
- [Call Tracking](../usage/call-tracking.md) - StateFlow counters
