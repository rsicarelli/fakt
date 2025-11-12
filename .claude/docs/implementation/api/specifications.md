# Fakt API Specifications

> **Status**: Production-Ready
> **Last Updated**: January 2025
> **Architecture**: FIR → IR Two-Phase Compiler Plugin

## Overview

Fakt is a Kotlin compiler plugin that generates type-safe test fakes at compile time. This document provides complete API specifications with **real examples from working code**.

## Core API

### @Fake Annotation

```kotlin
package com.rsicarelli.fakt

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Fake
```

**Purpose**: Mark interfaces/classes for fake generation
**Scope**: Compile-time processing, zero runtime overhead
**Output**: Generated test fakes in test source sets

## Generated Code Structure

For each `@Fake` annotated interface, three components are generated:

1. **Implementation Class** - `Fake{InterfaceName}Impl`
2. **Factory Function** - `fake{interfaceName}()`
3. **Configuration DSL** - `Fake{InterfaceName}Config`

## Working Examples

All examples below are from **real generated code** in the kmp-single-module sample project.

### Example 1: Basic Repository Pattern

**Input Interface:**
```kotlin
@Fake
interface UserRepository {
    val users: List<User>
    fun findById(id: String): User?
    fun save(user: User): User
    fun delete(id: String): Boolean
    fun findByAge(minAge: Int, maxAge: Int = 100): List<User>
}
```

**Generated Implementation:**
```kotlin
class FakeUserRepositoryImpl : UserRepository {
    // Call tracking (thread-safe with StateFlow)
    private val _usersCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val usersCallCount: StateFlow<Int> get() = _usersCallCount

    private val _findByIdCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val findByIdCallCount: StateFlow<Int> get() = _findByIdCallCount

    private val _saveCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val saveCallCount: StateFlow<Int> get() = _saveCallCount

    private val _deleteCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val deleteCallCount: StateFlow<Int> get() = _deleteCallCount

    private val _findByAgeCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val findByAgeCallCount: StateFlow<Int> get() = _findByAgeCallCount

    // Behavior storage
    private var usersBehavior: () -> List<User> = { emptyList() }
    private var findByIdBehavior: (String) -> User? = { null }
    private var saveBehavior: (User) -> User = { it }
    private var deleteBehavior: (String) -> Boolean = { false }
    private var findByAgeBehavior: (Int, Int) -> List<User> = { _, _ -> emptyList() }

    // Property implementation
    override val users: List<User>
        get() {
            _usersCallCount.update { it + 1 }
            return usersBehavior()
        }

    // Method implementations
    override fun findById(id: String): User? {
        _findByIdCallCount.update { it + 1 }
        return findByIdBehavior(id)
    }

    override fun save(user: User): User {
        _saveCallCount.update { it + 1 }
        return saveBehavior(user)
    }

    override fun delete(id: String): Boolean {
        _deleteCallCount.update { it + 1 }
        return deleteBehavior(id)
    }

    override fun findByAge(minAge: Int, maxAge: Int): List<User> {
        _findByAgeCallCount.update { it + 1 }
        return findByAgeBehavior(minAge, maxAge)
    }

    // Configuration methods (internal)
    internal fun configureUsers(behavior: () -> List<User>) {
        usersBehavior = behavior
    }

    internal fun configureFindById(behavior: (String) -> User?) {
        findByIdBehavior = behavior
    }

    internal fun configureSave(behavior: (User) -> User) {
        saveBehavior = behavior
    }

    internal fun configureDelete(behavior: (String) -> Boolean) {
        deleteBehavior = behavior
    }

    internal fun configureFindByAge(behavior: (Int, Int) -> List<User>) {
        findByAgeBehavior = behavior
    }
}
```

**Generated Factory:**
```kotlin
fun fakeUserRepository(
    configure: FakeUserRepositoryConfig.() -> Unit = {}
): FakeUserRepositoryImpl {
    return FakeUserRepositoryImpl().apply {
        FakeUserRepositoryConfig(this).configure()
    }
}
```

**Generated Configuration DSL:**
```kotlin
class FakeUserRepositoryConfig(private val fake: FakeUserRepositoryImpl) {
    fun findById(behavior: (String) -> User?) { fake.configureFindById(behavior) }
    fun save(behavior: (User) -> User) { fake.configureSave(behavior) }
    fun delete(behavior: (String) -> Boolean) { fake.configureDelete(behavior) }
    fun findByAge(behavior: (Int, Int) -> List<User>) { fake.configureFindByAge(behavior) }
    fun users(behavior: () -> List<User>) { fake.configureUsers(behavior) }
}
```

### Example 2: Suspend Functions with Generics

**Input Interface:**
```kotlin
@Fake
interface AsyncDataService {
    suspend fun fetchData(): String
    suspend fun <T> processData(data: T): T
    suspend fun batchProcess(items: List<String>): List<String>
}
```

**Generated Implementation:**
```kotlin
class FakeAsyncDataServiceImpl : AsyncDataService {
    // Call tracking
    private val _fetchDataCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val fetchDataCallCount: StateFlow<Int> get() = _fetchDataCallCount

    private val _processDataCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val processDataCallCount: StateFlow<Int> get() = _processDataCallCount

    private val _batchProcessCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val batchProcessCallCount: StateFlow<Int> get() = _batchProcessCallCount

    // Behavior storage
    private var fetchDataBehavior: suspend () -> String = { "" }
    private var processDataBehavior: suspend (Any?) -> Any? = { it }
    private var batchProcessBehavior: suspend (List<String>) -> List<String> = { it }

    // Suspend function implementations
    override suspend fun fetchData(): String {
        _fetchDataCallCount.update { it + 1 }
        return fetchDataBehavior()
    }

    override suspend fun <T : Any?> processData(data: T): T {
        _processDataCallCount.update { it + 1 }
        @Suppress("UNCHECKED_CAST")
        return processDataBehavior(data as Any?) as T
    }

    override suspend fun batchProcess(items: List<String>): List<String> {
        _batchProcessCallCount.update { it + 1 }
        return batchProcessBehavior(items)
    }

    // Configuration methods
    internal fun configureFetchData(behavior: suspend () -> String) {
        fetchDataBehavior = behavior
    }

    internal fun <T : Any?> configureProcessData(behavior: suspend (T) -> T) {
        @Suppress("UNCHECKED_CAST")
        processDataBehavior = behavior as suspend (Any?) -> Any?
    }

    internal fun configureBatchProcess(behavior: suspend (List<String>) -> List<String>) {
        batchProcessBehavior = behavior
    }
}
```

**Generated Configuration DSL:**
```kotlin
class FakeAsyncDataServiceConfig(private val fake: FakeAsyncDataServiceImpl) {
    fun fetchData(behavior: suspend () -> String) {
        fake.configureFetchData(behavior)
    }

    fun <T : Any?> processData(behavior: suspend (T) -> T) {
        fake.configureProcessData(behavior)
    }

    fun batchProcess(behavior: suspend (List<String>) -> List<String>) {
        fake.configureBatchProcess(behavior)
    }
}
```

## Usage API

### Basic Usage with Default Behavior

```kotlin
@Test
fun `GIVEN fake repository WHEN using defaults THEN should return smart defaults`() = runTest {
    // Given
    val repo = fakeUserRepository()

    // When & Then
    assertEquals(emptyList(), repo.users)
    assertNull(repo.findById("123"))
    assertEquals(false, repo.delete("123"))
}
```

### Custom Behavior Configuration

```kotlin
@Test
fun `GIVEN configured fake WHEN calling methods THEN should use custom behavior`() = runTest {
    // Given
    val testUser = User("123", "John Doe", 30)
    val repo = fakeUserRepository {
        findById { id ->
            if (id == "123") testUser else null
        }
        save { user ->
            user.copy(name = user.name.uppercase())
        }
        delete { id -> id.isNotEmpty() }
    }

    // When & Then
    assertEquals(testUser, repo.findById("123"))
    assertNull(repo.findById("456"))
    assertEquals("JANE DOE", repo.save(User("456", "Jane Doe", 25)).name)
    assertTrue(repo.delete("123"))
}
```

### Call Tracking Verification

```kotlin
@Test
fun `GIVEN fake service WHEN calling methods THEN should track call counts`() = runTest {
    // Given
    val repo = fakeUserRepository()

    // When
    repo.findById("1")
    repo.findById("2")
    repo.findById("3")
    repo.save(User("4", "Test", 25))

    // Then
    assertEquals(3, repo.findByIdCallCount.value)
    assertEquals(1, repo.saveCallCount.value)
    assertEquals(0, repo.deleteCallCount.value)
}
```

### Suspend Function Testing

```kotlin
@Test
fun `GIVEN async service WHEN calling suspend functions THEN should work correctly`() = runTest {
    // Given
    val service = fakeAsyncDataService {
        fetchData { "test-data" }
        processData<String> { data -> data.uppercase() }
        batchProcess { items -> items.map { it.reversed() } }
    }

    // When & Then
    assertEquals("test-data", service.fetchData())
    assertEquals("HELLO", service.processData("hello"))
    assertEquals(listOf("cba", "fed"), service.batchProcess(listOf("abc", "def")))

    // Verify call tracking
    assertEquals(1, service.fetchDataCallCount.value)
    assertEquals(1, service.processDataCallCount.value)
    assertEquals(1, service.batchProcessCallCount.value)
}
```

### Reactive Testing with StateFlow

```kotlin
@Test
fun `GIVEN fake service WHEN observing call counts THEN should receive updates`() = runTest {
    // Given
    val service = fakeAsyncDataService()
    val callCounts = mutableListOf<Int>()

    // Collect call count changes
    val job = launch {
        service.fetchDataCallCount.collect { count ->
            callCounts.add(count)
        }
    }

    // When
    delay(50)
    service.fetchData()
    delay(50)
    service.fetchData()
    delay(50)
    service.fetchData()
    delay(50)

    // Then
    assertEquals(listOf(0, 1, 2, 3), callCounts)
    job.cancel()
}
```

## Generated Patterns

### Naming Conventions

```kotlin
// Interface name → Generated names
@Fake interface UserService
// → class FakeUserServiceImpl
// → fun fakeUserService()
// → class FakeUserServiceConfig

@Fake interface ApiClient
// → class FakeApiClientImpl
// → fun fakeApiClient()
// → class FakeApiClientConfig
```

### Method Name Conventions

```kotlin
// Interface method → Generated components
fun getUser(id: String): User

// Behavior field:
private var getUserBehavior: (String) -> User = { id -> User(id, "", 0) }

// Call tracking:
private val _getUserCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
val getUserCallCount: StateFlow<Int> get() = _getUserCallCount

// Override implementation:
override fun getUser(id: String): User {
    _getUserCallCount.update { it + 1 }
    return getUserBehavior(id)
}

// Configuration method:
internal fun configureGetUser(behavior: (String) -> User) {
    getUserBehavior = behavior
}

// DSL method:
fun getUser(behavior: (String) -> User) { fake.configureGetUser(behavior) }
```

## Type System

### Smart Default Values

Fakt generates intelligent defaults based on return types:

```kotlin
// Primitive types
String          → { "" }
Int             → { 0 }
Long            → { 0L }
Float           → { 0.0f }
Double          → { 0.0 }
Boolean         → { false }
Char            → { '\u0000' }

// Unit type
Unit            → { Unit }

// Nullable types
String?         → { null }
User?           → { null }
T?              → { null }

// Collections
List<T>         → { emptyList() }
Set<T>          → { emptySet() }
Map<K, V>       → { emptyMap() }
Array<T>        → { emptyArray() }

// Kotlin stdlib types
Pair<A, B>      → { Pair(defaultA, defaultB) }
Triple<A, B, C> → { Triple(defaultA, defaultB, defaultC) }
Result<T>       → { Result.success(defaultT) }
Sequence<T>     → { emptySequence() }

// Custom types
User            → Identity function { it }
CustomClass     → Identity function { it }
```

### Generic Type Handling

**Method-Level Generics:**
```kotlin
@Fake
interface DataProcessor {
    fun <T> process(data: T): T
    suspend fun <R> transform(input: R): R
}

// Generated with type erasure workarounds:
private var processBehavior: (Any?) -> Any? = { it }

internal fun <T : Any?> configureProcess(behavior: (T) -> T) {
    @Suppress("UNCHECKED_CAST")
    processBehavior = behavior as (Any?) -> Any?
}
```

**Class-Level Generics:**
```kotlin
@Fake
interface Repository<T> {
    fun save(item: T): T
    fun findAll(): List<T>
}

// Generated with concrete type parameter handling:
class FakeRepositoryImpl<T> : Repository<T> {
    private var saveBehavior: (T) -> T = { it }
    private var findAllBehavior: () -> List<T> = { emptyList() }

    override fun save(item: T): T {
        _saveCallCount.update { it + 1 }
        return saveBehavior(item)
    }

    override fun findAll(): List<T> {
        _findAllCallCount.update { it + 1 }
        return findAllBehavior()
    }
}
```

### Higher-Order Functions

```kotlin
@Fake
interface EventHandler {
    fun onEvent(callback: (String) -> Unit)
    fun process(transform: (Int) -> String): String
}

// Generated preserving function types:
private var onEventBehavior: ((String) -> Unit) -> Unit = { callback -> Unit }
private var processBehavior: ((Int) -> String) -> String = { transform -> "" }
```

## Call Tracking API

### StateFlow Integration

Every generated fake includes thread-safe call tracking via StateFlow:

```kotlin
class FakeXxxImpl : Xxx {
    // Private mutable state
    private val _{methodName}CallCount: MutableStateFlow<Int> = MutableStateFlow(0)

    // Public read-only accessor
    val {methodName}CallCount: StateFlow<Int>
        get() = _{methodName}CallCount

    // Automatic increment on method call
    override fun {methodName}(...): ReturnType {
        _{methodName}CallCount.update { it + 1 }
        return {methodName}Behavior(...)
    }
}
```

### Call Tracking Usage

**Simple Verification:**
```kotlin
val service = fakeUserRepository()
service.findById("123")
assertEquals(1, service.findByIdCallCount.value)
```

**Multiple Calls:**
```kotlin
val service = fakeUserRepository()
repeat(5) { service.findById("$it") }
assertEquals(5, service.findByIdCallCount.value)
```

**Flow Collection:**
```kotlin
val service = fakeUserRepository()
val counts = mutableListOf<Int>()

launch {
    service.findByIdCallCount.collect { counts.add(it) }
}

service.findById("1")
delay(100)
assertEquals(listOf(0, 1), counts)
```

## Thread Safety

All generated fakes are **thread-safe by design**:

1. **Call tracking** uses `MutableStateFlow` with atomic updates via `.update { }`
2. **Behavior configuration** happens before concurrent access (during DSL setup)
3. **Factory pattern** ensures each test gets an isolated instance

```kotlin
@Test
fun `GIVEN fake service WHEN called concurrently THEN should track correctly`() = runTest {
    // Given
    val service = fakeUserRepository()

    // When - 100 concurrent calls
    (1..100).map { id ->
        launch {
            service.findById("$id")
        }
    }.forEach { it.join() }

    // Then - All calls tracked
    assertEquals(100, service.findByIdCallCount.value)
}
```

## Output Location

Generated fakes are placed in test source sets:

```
build/generated/fakt/
├── commonTest/kotlin/              # KMP common test code
│   └── com/example/package/
│       └── FakeXxxImpl.kt
├── jvmTest/kotlin/                 # JVM-specific tests
│   └── com/example/package/
│       └── FakeXxxImpl.kt
└── test/kotlin/                    # Single-platform projects
    └── com/example/package/
        └── FakeXxxImpl.kt
```

## Compilation Flow

```
Source Code
     ↓
[@Fake Annotation Detected]
     ↓
┌─────────────────────────┐
│  PHASE 1: FIR Analysis  │
│  • Detect @Fake         │
│  • Validate interface   │
│  • Extract metadata     │
└─────────────────────────┘
     ↓
┌─────────────────────────┐
│  PHASE 2: IR Generation │
│  • Create IR nodes      │
│  • Generate impl class  │
│  • Generate factory     │
│  • Generate DSL         │
│  • Add call tracking    │
└─────────────────────────┘
     ↓
Generated Fakes (Test Source Set)
     ↓
Compilation Success ✓
```

## Related Documentation

- `.claude/docs/api/annotations.md` - @Fake annotation reference
- `.claude/docs/api/generated-api.md` - Generated code patterns
- `.claude/docs/validation/testing-guidelines.md` - GIVEN-WHEN-THEN testing standards
- `docs/introduction/why-fakt.md` - Design philosophy and motivation
