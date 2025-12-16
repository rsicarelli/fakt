# Fakt Generated API Reference

> **Status**: Production-Ready
> **Last Updated**: January 2025
> **Source**: Real examples from sample projects (jvm-single-module, android-single-module, kmp-single-module)

## Overview

For every `@Fake` annotated interface or class, Fakt generates three components:

1. **Implementation Class** - `Fake{Name}Impl`
2. **Factory Function** - `fake{name}()`
3. **Configuration DSL** - `Fake{Name}Config`

All generated code includes:
- Thread-safe call tracking via StateFlow
- Smart default behaviors
- Type-safe configuration DSL
- Internal configuration methods

## Implementation Class Pattern

### Structure

```kotlin
class Fake{InterfaceName}Impl : {InterfaceName} {
    // 1. Call tracking fields (thread-safe)
    private val _{methodName}CallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val {methodName}CallCount: StateFlow<Int> get() = _{methodName}CallCount

    // 2. Behavior storage fields
    private var {methodName}Behavior: {LambdaType} = {defaultValue}

    // 3. Interface member overrides
    override fun {methodName}({params}): {ReturnType} {
        _{methodName}CallCount.update { it + 1 }
        return {methodName}Behavior({args})
    }

    // 4. Configuration methods (internal)
    internal fun configure{MethodName}(behavior: {LambdaType}) {
        {methodName}Behavior = behavior
    }
}
```

### Real Example: UserRepository

**Input:**
```kotlin
@Fake
interface UserRepository {
    val users: List<User>
    fun findById(id: String): User?
    fun save(user: User): User
    fun delete(id: String): Boolean
}
```

**Generated Implementation:**
```kotlin
class FakeUserRepositoryImpl : UserRepository {
    // Call tracking for 'users' property
    private val _usersCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val usersCallCount: StateFlow<Int> get() = _usersCallCount

    // Call tracking for 'findById' method
    private val _findByIdCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val findByIdCallCount: StateFlow<Int> get() = _findByIdCallCount

    // Call tracking for 'save' method
    private val _saveCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val saveCallCount: StateFlow<Int> get() = _saveCallCount

    // Call tracking for 'delete' method
    private val _deleteCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val deleteCallCount: StateFlow<Int> get() = _deleteCallCount

    // Behavior storage
    private var usersBehavior: () -> List<User> = { emptyList() }
    private var findByIdBehavior: (String) -> User? = { null }
    private var saveBehavior: (User) -> User = { it }
    private var deleteBehavior: (String) -> Boolean = { false }

    // Property override
    override val users: List<User>
        get() {
            _usersCallCount.update { it + 1 }
            return usersBehavior()
        }

    // Method overrides
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
}
```

### Naming Conventions

```kotlin
// Interface/class name → Implementation class name
UserRepository → FakeUserRepositoryImpl
ApiClient → FakeApiClientImpl
DataService → FakeDataServiceImpl
```

## Factory Function Pattern

### Signature

```kotlin
fun fake{interfaceName}(
    configure: Fake{InterfaceName}Config.() -> Unit = {}
): Fake{InterfaceName}Impl
```

**Return Type**: Returns the **implementation class**, not the interface. This allows access to call tracking and advanced features.

### Real Examples

```kotlin
// Basic factory
fun fakeUserRepository(
    configure: FakeUserRepositoryConfig.() -> Unit = {}
): FakeUserRepositoryImpl {
    return FakeUserRepositoryImpl().apply {
        FakeUserRepositoryConfig(this).configure()
    }
}

// Suspend function interface factory
fun fakeAsyncDataService(
    configure: FakeAsyncDataServiceConfig.() -> Unit = {}
): FakeAsyncDataServiceImpl {
    return FakeAsyncDataServiceImpl().apply {
        FakeAsyncDataServiceConfig(this).configure()
    }
}

// Generic interface factory
fun <T> fakeRepository(
    configure: FakeRepositoryConfig<T>.() -> Unit = {}
): FakeRepositoryImpl<T> {
    return FakeRepositoryImpl<T>().apply {
        FakeRepositoryConfig(this).configure()
    }
}
```

### Usage Patterns

**No configuration (defaults):**
```kotlin
val repo = fakeUserRepository()
// Uses smart defaults for all methods
```

**With configuration:**
```kotlin
val repo = fakeUserRepository {
    findById { id -> if (id == "123") testUser else null }
    save { user -> user.copy(id = "new-id") }
    delete { id -> true }
}
```

**Access implementation features:**
```kotlin
val repo = fakeUserRepository()
repo.findById("123")

// Access call tracking (only available on implementation type)
assertEquals(1, repo.findByIdCallCount.value)
```

**Polymorphic usage:**
```kotlin
// Can be assigned to interface type
val repo: UserRepository = fakeUserRepository()

// But call tracking requires implementation type
val repoImpl = fakeUserRepository()
assertEquals(0, repoImpl.findByIdCallCount.value)
```

## Configuration DSL Pattern

### Structure

```kotlin
class Fake{InterfaceName}Config(private val fake: Fake{InterfaceName}Impl) {
    // One DSL method per interface member
    fun {methodName}(behavior: {LambdaType}) {
        fake.configure{MethodName}(behavior)
    }
}
```

### Real Example

```kotlin
class FakeUserRepositoryConfig(private val fake: FakeUserRepositoryImpl) {
    fun findById(behavior: (String) -> User?) {
        fake.configureFindById(behavior)
    }

    fun save(behavior: (User) -> User) {
        fake.configureSave(behavior)
    }

    fun delete(behavior: (String) -> Boolean) {
        fake.configureDelete(behavior)
    }

    fun users(behavior: () -> List<User>) {
        fake.configureUsers(behavior)
    }
}
```

### Usage in Tests

```kotlin
@Test
fun `GIVEN configured fake WHEN calling methods THEN should use custom behavior`() = runTest {
    // Given
    val repo = fakeUserRepository {
        // Each method in the DSL corresponds to an interface member
        findById { id ->
            when (id) {
                "123" -> User("123", "Alice", 30)
                "456" -> User("456", "Bob", 25)
                else -> null
            }
        }

        save { user ->
            // Transform saved user
            user.copy(name = user.name.uppercase())
        }

        delete { id ->
            // Return true if ID is not empty
            id.isNotEmpty()
        }

        users {
            // Return test data
            listOf(
                User("123", "Alice", 30),
                User("456", "Bob", 25)
            )
        }
    }

    // When & Then
    assertEquals("Alice", repo.findById("123")?.name)
    assertEquals("CHARLIE", repo.save(User("789", "Charlie", 35)).name)
    assertTrue(repo.delete("123"))
    assertEquals(2, repo.users.size)
}
```

## Call Tracking API

Every generated fake includes automatic call tracking for all members.

### Structure

```kotlin
// For each interface member, generate:

// Private mutable state
private val _{memberName}CallCount: MutableStateFlow<Int> = MutableStateFlow(0)

// Public read-only accessor
val {memberName}CallCount: StateFlow<Int>
    get() = _{memberName}CallCount

// Increment on each call
override fun {memberName}(...) {
    _{memberName}CallCount.update { it + 1 }
    return {memberName}Behavior(...)
}
```

### Real Example

```kotlin
class FakeUserRepositoryImpl : UserRepository {
    private val _findByIdCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val findByIdCallCount: StateFlow<Int> get() = _findByIdCallCount

    override fun findById(id: String): User? {
        _findByIdCallCount.update { it + 1 }
        return findByIdBehavior(id)
    }
}
```

### Usage Patterns

**Simple call count verification:**
```kotlin
val repo = fakeUserRepository()
repo.findById("123")
repo.findById("456")

assertEquals(2, repo.findByIdCallCount.value)
assertEquals(0, repo.saveCallCount.value)
```

**Reactive testing with Flow:**
```kotlin
@Test
fun `GIVEN fake WHEN observing calls THEN should track changes`() = runTest {
    // Given
    val repo = fakeUserRepository()
    val counts = mutableListOf<Int>()

    // Observe call count changes
    val job = launch {
        repo.findByIdCallCount.collect { count ->
            counts.add(count)
        }
    }

    // When
    delay(50)
    repo.findById("1")
    delay(50)
    repo.findById("2")
    delay(50)

    // Then
    assertEquals(listOf(0, 1, 2), counts)
    job.cancel()
}
```

**Concurrent call tracking:**
```kotlin
@Test
fun `GIVEN fake WHEN called concurrently THEN should count all calls`() = runTest {
    // Given
    val repo = fakeUserRepository()

    // When - 1000 concurrent calls
    (1..1000).map {
        launch { repo.findById("$it") }
    }.forEach { it.join() }

    // Then - All calls tracked correctly
    assertEquals(1000, repo.findByIdCallCount.value)
}
```

## Type-Specific Generation

### Suspend Functions

```kotlin
@Fake
interface AsyncService {
    suspend fun fetchData(): String
    suspend fun processData(data: String): Boolean
}

// Generated:
class FakeAsyncServiceImpl : AsyncService {
    private var fetchDataBehavior: suspend () -> String = { "" }
    private var processDataBehavior: suspend (String) -> Boolean = { false }

    override suspend fun fetchData(): String {
        _fetchDataCallCount.update { it + 1 }
        return fetchDataBehavior()
    }

    override suspend fun processData(data: String): Boolean {
        _processDataCallCount.update { it + 1 }
        return processDataBehavior(data)
    }
}
```

### Generic Methods

```kotlin
@Fake
interface DataProcessor {
    fun <T> process(data: T): T
    suspend fun <R> transform(input: R): R
}

// Generated with type erasure handling:
class FakeDataProcessorImpl : DataProcessor {
    private var processBehavior: (Any?) -> Any? = { it }
    private var transformBehavior: suspend (Any?) -> Any? = { it }

    override fun <T : Any?> process(data: T): T {
        _processCallCount.update { it + 1 }
        @Suppress("UNCHECKED_CAST")
        return processBehavior(data as Any?) as T
    }

    override suspend fun <R : Any?> transform(input: R): R {
        _transformCallCount.update { it + 1 }
        @Suppress("UNCHECKED_CAST")
        return transformBehavior(input as Any?) as R
    }

    internal fun <T : Any?> configureProcess(behavior: (T) -> T) {
        @Suppress("UNCHECKED_CAST")
        processBehavior = behavior as (Any?) -> Any?
    }

    internal fun <R : Any?> configureTransform(behavior: suspend (R) -> R) {
        @Suppress("UNCHECKED_CAST")
        transformBehavior = behavior as suspend (Any?) -> Any?
    }
}
```

### Properties

```kotlin
@Fake
interface Configuration {
    val appName: String
    val version: Int
    var debugMode: Boolean
}

// Generated:
class FakeConfigurationImpl : Configuration {
    private val _appNameCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val appNameCallCount: StateFlow<Int> get() = _appNameCallCount

    private var appNameBehavior: () -> String = { "" }

    override val appName: String
        get() {
            _appNameCallCount.update { it + 1 }
            return appNameBehavior()
        }

    // Similar for version and debugMode...
}
```

### Higher-Order Functions

```kotlin
@Fake
interface EventHandler {
    fun onEvent(callback: (String) -> Unit)
    fun transform(mapper: (Int) -> String): String
}

// Generated preserving function types:
class FakeEventHandlerImpl : EventHandler {
    private var onEventBehavior: ((String) -> Unit) -> Unit = { }
    private var transformBehavior: ((Int) -> String) -> String = { _ -> "" }

    override fun onEvent(callback: (String) -> Unit) {
        _onEventCallCount.update { it + 1 }
        onEventBehavior(callback)
    }

    override fun transform(mapper: (Int) -> String): String {
        _transformCallCount.update { it + 1 }
        return transformBehavior(mapper)
    }
}
```

## Smart Default Values

Fakt generates intelligent default behaviors based on return types:

```kotlin
// Primitives
String          → { "" }
Int             → { 0 }
Boolean         → { false }
Unit            → { Unit }

// Nullable types
T?              → { null }

// Collections
List<T>         → { emptyList() }
Set<T>          → { emptySet() }
Map<K,V>        → { emptyMap() }

// Identity functions (for custom types)
User            → { it }
CustomClass     → { it }

// Suspend functions
suspend () -> T → { defaultValue<T>() }
```

## File Structure

Generated code is organized into single files per interface:

```
build/generated/fakt/
└── commonTest/kotlin/
    └── com/rsicarelli/fakt/samples/
        └── basic/
            ├── FakeUserRepositoryImpl.kt    # All generated code for UserRepository
            ├── FakeApiClientImpl.kt          # All generated code for ApiClient
            └── FakeDataServiceImpl.kt        # All generated code for DataService
```

Each file contains:
```kotlin
// File: FakeUserRepositoryImpl.kt
package com.rsicarelli.fakt.samples.basic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// 1. Implementation class
class FakeUserRepositoryImpl : UserRepository { ... }

// 2. Factory function
fun fakeUserRepository(configure: FakeUserRepositoryConfig.() -> Unit = {}): FakeUserRepositoryImpl { ... }

// 3. Configuration DSL
class FakeUserRepositoryConfig(private val fake: FakeUserRepositoryImpl) { ... }
```

## Complete Real-World Example

**Source Interface:**
```kotlin
@Fake
interface UserService {
    val currentUser: User?
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout()
    fun isAuthenticated(): Boolean
}
```

**Generated Code (Complete):**
```kotlin
// Generated by Fakt
package com.example.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FakeUserServiceImpl : UserService {
    // Call tracking
    private val _currentUserCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val currentUserCallCount: StateFlow<Int> get() = _currentUserCallCount

    private val _loginCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val loginCallCount: StateFlow<Int> get() = _loginCallCount

    private val _logoutCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val logoutCallCount: StateFlow<Int> get() = _logoutCallCount

    private val _isAuthenticatedCallCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val isAuthenticatedCallCount: StateFlow<Int> get() = _isAuthenticatedCallCount

    // Behavior storage
    private var currentUserBehavior: () -> User? = { null }
    private var loginBehavior: suspend (String, String) -> Result<User> = { _, _ ->
        Result.failure(Exception("Not configured"))
    }
    private var logoutBehavior: suspend () -> Unit = { }
    private var isAuthenticatedBehavior: () -> Boolean = { false }

    // Interface implementation
    override val currentUser: User?
        get() {
            _currentUserCallCount.update { it + 1 }
            return currentUserBehavior()
        }

    override suspend fun login(email: String, password: String): Result<User> {
        _loginCallCount.update { it + 1 }
        return loginBehavior(email, password)
    }

    override suspend fun logout() {
        _logoutCallCount.update { it + 1 }
        logoutBehavior()
    }

    override fun isAuthenticated(): Boolean {
        _isAuthenticatedCallCount.update { it + 1 }
        return isAuthenticatedBehavior()
    }

    // Configuration methods
    internal fun configureCurrentUser(behavior: () -> User?) {
        currentUserBehavior = behavior
    }

    internal fun configureLogin(behavior: suspend (String, String) -> Result<User>) {
        loginBehavior = behavior
    }

    internal fun configureLogout(behavior: suspend () -> Unit) {
        logoutBehavior = behavior
    }

    internal fun configureIsAuthenticated(behavior: () -> Boolean) {
        isAuthenticatedBehavior = behavior
    }
}

fun fakeUserService(
    configure: FakeUserServiceConfig.() -> Unit = {}
): FakeUserServiceImpl {
    return FakeUserServiceImpl().apply {
        FakeUserServiceConfig(this).configure()
    }
}

class FakeUserServiceConfig(private val fake: FakeUserServiceImpl) {
    fun currentUser(behavior: () -> User?) {
        fake.configureCurrentUser(behavior)
    }

    fun login(behavior: suspend (String, String) -> Result<User>) {
        fake.configureLogin(behavior)
    }

    fun logout(behavior: suspend () -> Unit) {
        fake.configureLogout(behavior)
    }

    fun isAuthenticated(behavior: () -> Boolean) {
        fake.configureIsAuthenticated(behavior)
    }
}
```

**Usage in Tests:**
```kotlin
@Test
fun `GIVEN authenticated user WHEN checking auth THEN should return true`() = runTest {
    // Given
    val testUser = User("test@example.com", "Test User")
    val service = fakeUserService {
        currentUser { testUser }
        isAuthenticated { true }
        login { email, password ->
            if (email == "test@example.com" && password == "secret") {
                Result.success(testUser)
            } else {
                Result.failure(Exception("Invalid credentials"))
            }
        }
        logout { println("User logged out") }
    }

    // When & Then
    assertTrue(service.isAuthenticated())
    assertEquals(testUser, service.currentUser)

    val loginResult = service.login("test@example.com", "secret")
    assertTrue(loginResult.isSuccess)
    assertEquals(testUser, loginResult.getOrNull())

    service.logout()

    // Verify call tracking
    assertEquals(1, service.currentUserCallCount.value)
    assertEquals(1, service.isAuthenticatedCallCount.value)
    assertEquals(1, service.loginCallCount.value)
    assertEquals(1, service.logoutCallCount.value)
}
```

## Related Documentation

- `.claude/docs/api/annotations.md` - @Fake annotation reference
- `.claude/docs/api/specifications.md` - Complete API specifications
- `.claude/docs/validation/testing-guidelines.md` - Testing patterns with Fakt
- `docs/introduction/why-fakt.md` - Design philosophy
