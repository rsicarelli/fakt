# Fakt Annotations Reference

> **Status**: Production-Ready
> **Last Updated**: January 2025
> **Package**: `com.rsicarelli.fakt`

## @Fake Annotation

The `@Fake` annotation is the single, simple annotation used to mark interfaces or classes for fake generation.

### Definition

```kotlin
package com.rsicarelli.fakt

/**
 * Primary annotation for marking interfaces/classes for fake generation.
 *
 * This annotation enables compile-time generation of thread-safe fake implementations
 * that can be used in tests. The generated fakes follow the factory function pattern
 * to ensure instance isolation and eliminate race conditions.
 *
 * ## Basic Usage
 * ```kotlin
 * @Fake
 * interface UserService {
 *     suspend fun getUser(id: String): User
 * }
 *
 * // Usage in tests
 * val userService = fakeUserService {
 *     getUser { id -> User(id, "Test User") }
 * }
 * ```
 *
 * ## Thread Safety
 * Generated fakes are thread-safe by default through instance-based design.
 * Each call to the factory function creates a new isolated instance.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Fake
```

### Characteristics

- **No Parameters**: The annotation is intentionally simple with no configuration parameters
- **Target**: Can only be applied to classes (interfaces and classes)
- **Retention**: BINARY - available during compilation and in compiled code
- **Thread-Safe**: Generated implementations use StateFlow for call tracking
- **Test-Only**: Generated code is placed in test source sets (commonTest, jvmTest, etc.)

### Usage Examples

#### Basic Interface

```kotlin
@Fake
interface AnalyticsService {
    fun track(event: String)
    fun identify(userId: String)
}
```

#### Interface with Properties

```kotlin
@Fake
interface UserRepository {
    val users: List<User>
    fun findById(id: String): User?
    fun save(user: User): User
}
```

#### Suspend Functions

```kotlin
@Fake
interface AsyncDataService {
    suspend fun fetchData(): String
    suspend fun processData(data: String): Boolean
}
```

#### Generic Methods

```kotlin
@Fake
interface DataProcessor {
    suspend fun <T : Any?> processData(data: T): T
    fun <T> transform(input: T): T
}
```

#### SAM Interfaces (Single Abstract Method)

```kotlin
@Fake
fun interface StringFormatter {
    fun format(input: String): String
}
```

## Generated Features

For each `@Fake` annotated interface, the compiler plugin generates:

### 1. Implementation Class

```kotlin
class Fake{InterfaceName}Impl : {InterfaceName} {
    // Call tracking via StateFlow (thread-safe)
    private val _{methodName}CallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val {methodName}CallCount: StateFlow<Int> get() = _{methodName}CallCount

    // Behavior storage
    private var {methodName}Behavior: {LambdaType} = {defaultValue}

    // Interface implementation with call counting
    override fun {methodName}({params}): {ReturnType} {
        _{methodName}CallCount.update { it + 1 }
        return {methodName}Behavior({args})
    }

    // Configuration methods (internal)
    internal fun configure{MethodName}(behavior: {LambdaType}) {
        {methodName}Behavior = behavior
    }
}
```

### 2. Factory Function

```kotlin
fun fake{interfaceName}(
    configure: Fake{InterfaceName}Config.() -> Unit = {}
): Fake{InterfaceName}Impl {
    return Fake{InterfaceName}Impl().apply {
        Fake{InterfaceName}Config(this).configure()
    }
}
```

### 3. Configuration DSL

```kotlin
class Fake{InterfaceName}Config(private val fake: Fake{InterfaceName}Impl) {
    fun {methodName}(behavior: {LambdaType}) {
        fake.configure{MethodName}(behavior)
    }
}
```

### 4. Call Tracking API

All generated fakes include automatic call tracking:

```kotlin
val service = fakeUserRepository()

// Each method has a corresponding call count
service.findById("123")
service.findById("456")

// Access call counts via StateFlow
assertEquals(2, service.findByIdCallCount.value)
assertEquals(0, service.saveCallCount.value)
```

## Supported Constructs

### ✅ Fully Supported

- **Interfaces** - Primary target for @Fake annotation
- **Abstract classes** - Full support for abstract class faking
- **Open classes** - Generates fakes for open classes
- **Final classes** - Generates fakes for final classes
- **Properties** (val, var) - Both mutable and immutable properties
- **Methods** (fun) - Regular functions with any signature
- **Suspend functions** - Full coroutine support
- **Generic methods** - Method-level type parameters (e.g., `<T> process(data: T): T`)
- **Generic classes** - Class-level type parameters (e.g., `interface Repository<T>`)
- **Default parameters** - Methods with default parameter values
- **Nullable types** - Full null safety support
- **Collections** - List, Set, Map, etc.
- **Higher-order functions** - Functions that accept or return functions
- **SAM interfaces** - Single Abstract Method (fun interface)
- **Companion objects** - Interfaces with companion objects
- **Sealed classes/interfaces** - Support for sealed hierarchies
- **Enum properties** - Properties with enum types
- **Variance** (in, out) - Covariant and contravariant type parameters
- **Type constraints** - Generic constraints (e.g., `T : Comparable<T>`)

### ⚠️ Limitations

None identified. Fakt has comprehensive support for all Kotlin language constructs.

## Validation Rules

The compiler plugin performs several validations:

### Compile-Time Checks

1. **Target Validation**: `@Fake` can only be applied to classes (interfaces, abstract classes, open/final classes)
2. **Source Set Validation**: Generated fakes are only placed in test source sets
3. **Member Validation**: All interface/abstract members must be fakeable

### What Happens During Compilation

```
@Fake Annotation Found
        ↓
FIR Phase: Detect and Validate
        ↓
IR Phase: Generate Implementation
        ↓
Output: {InterfaceName}Fakes.kt in test source set
        ↓
Contains:
  - FakeXxxImpl (implementation)
  - fakeXxx() (factory)
  - FakeXxxConfig (DSL)
  - Call tracking API (StateFlow)
```

## Usage Patterns

### Test Setup

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    @Test
    fun `GIVEN user service WHEN fetching user THEN should return configured value`() = runTest {
        // Given
        val service = fakeUserRepository {
            findById { id -> User(id, "Test User") }
        }

        // When
        val user = service.findById("123")

        // Then
        assertEquals("Test User", user?.name)
        assertEquals(1, service.findByIdCallCount.value)
    }
}
```

### Call Count Verification

```kotlin
@Test
fun `GIVEN analytics service WHEN tracking multiple events THEN should count all calls`() = runTest {
    // Given
    val events = mutableListOf<String>()
    val analytics = fakeAnalyticsService {
        track { event -> events.add(event) }
    }

    // When
    analytics.track("login")
    analytics.track("page_view")
    analytics.track("logout")

    // Then
    assertEquals(3, analytics.trackCallCount.value)
    assertEquals(listOf("login", "page_view", "logout"), events)
}
```

### Flow-Based Testing

```kotlin
@Test
fun `GIVEN service WHEN observing call counts THEN should react to changes`() = runTest {
    // Given
    val service = fakeUserRepository()
    val callCounts = mutableListOf<Int>()

    // Observe call count changes
    launch {
        service.findByIdCallCount.collect { count ->
            callCounts.add(count)
        }
    }

    // When
    service.findById("1")
    service.findById("2")

    // Then
    delay(100) // Allow collection
    assertEquals(listOf(0, 1, 2), callCounts)
}
```

## Migration from Other Frameworks

### From MockK

```kotlin
// Before (MockK)
val service = mockk<UserRepository>()
every { service.findById("123") } returns User("123", "John")
verify(exactly = 1) { service.findById("123") }

// After (Fakt)
val service = fakeUserRepository {
    findById { id -> User(id, "John") }
}
service.findById("123")
assertEquals(1, service.findByIdCallCount.value)
```

### From Mockito

```kotlin
// Before (Mockito)
val service = mock(UserRepository::class.java)
`when`(service.findById("123")).thenReturn(User("123", "John"))
verify(service, times(1)).findById("123")

// After (Fakt)
val service = fakeUserRepository {
    findById { id -> User(id, "John") }
}
service.findById("123")
assertEquals(1, service.findByIdCallCount.value)
```

## Related Documentation

- `.claude/docs/api/specifications.md` - Complete API specifications with examples
- `.claude/docs/api/generated-api.md` - Detailed generated code reference
- `docs/introduction/why-fakt.md` - Why Fakt exists and design philosophy
- `.claude/docs/validation/testing-guidelines.md` - Testing patterns with Fakt
