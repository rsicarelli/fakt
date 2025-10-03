# KtFakes API Specifications - Unified IR-Native Implementation

> **Status**: Production-Ready with Working Examples ✅
> **Architecture**: Unified IR-Native Compiler Plugin
> **Testing Standard**: [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)
> **Last Updated**: September 2025

## 🎯 **Overview**

This document provides complete API specifications for KtFakes with **working examples** from our unified IR-native implementation. All examples are validated against the current production code generation.

## 🚀 **Core API**

### **@Fake Annotation**

```kotlin
package dev.rsicarelli.ktfake

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Fake(
    val trackCalls: Boolean = false,
    val builder: Boolean = false,
    val concurrent: Boolean = false,
    val scope: String = "test",
    val dependencies: Array<KClass<*>> = []
)
```

**Purpose**: Mark interfaces for fake implementation generation
**Scope**: Compile-time only, no runtime overhead
**Safety**: Only processed in test source sets

## 📋 **Working Examples**

All examples below are from **real generated code** in our `test-sample` project:

### **Example 1: Basic Interface with Properties and Methods**

**Input Interface**:
```kotlin
@Fake
interface TestService {
    val someValue: String
    fun getValue(): String
    fun setValue(value: String)
}
```

**Generated Implementation**:
```kotlin
class FakeTestServiceImpl : TestService {
    private var someValueeBehavior: () -> String = { "" }
    private var getValueBehavior: () -> String = { "" }
    private var setValueBehavior: (String) -> Unit = { _ -> Unit }

    override val someValue: String get() = someValueBehavior()
    override fun getValue(): String = getValueBehavior()
    override fun setValue(value: String): Unit = setValueBehavior(value)

    internal fun configuresomeValue(behavior: () -> String) { someValueBehavior = behavior }
    internal fun configureGetValue(behavior: () -> String) { getValueBehavior = behavior }
    internal fun configureSetValue(behavior: (String) -> Unit) { setValueBehavior = behavior }
}
```

**Generated Factory Function**:
```kotlin
fun fakeTestService(configure: FakeTestServiceConfig.() -> Unit = {}): TestService {
    return FakeTestServiceImpl().apply { FakeTestServiceConfig(this).configure() }
}
```

**Generated Configuration DSL**:
```kotlin
class FakeTestServiceConfig(private val fake: FakeTestServiceImpl) {
    fun someValue(behavior: () -> String) { fake.configuresomeValue(behavior) }
    fun getValue(behavior: () -> String) { fake.configureGetValue(behavior) }
    fun setValue(behavior: (String) -> Unit) { fake.configureSetValue(behavior) }
}
```

### **Example 2: Suspend Functions Interface**

**Input Interface**:
```kotlin
@Fake
interface AsyncUserService {
    suspend fun getUser(id: String): String
    suspend fun updateUser(id: String, name: String): Boolean
}
```

**Generated Implementation**:
```kotlin
class FakeAsyncUserServiceImpl : AsyncUserService {
    private var getUserBehavior: suspend (String) -> String = { _ -> "" }
    private var updateUserBehavior: suspend (String, String) -> Boolean = { _, _ -> false }

    override suspend fun getUser(id: String): String = getUserBehavior(id)
    override suspend fun updateUser(id: String, name: String): Boolean = updateUserBehavior(id, name)

    internal fun configureGetUser(behavior: suspend (String) -> String) { getUserBehavior = behavior }
    internal fun configureUpdateUser(behavior: suspend (String, String) -> Boolean) { updateUserBehavior = behavior }
}
```

**Generated Configuration DSL**:
```kotlin
class FakeAsyncUserServiceConfig(private val fake: FakeAsyncUserServiceImpl) {
    fun getUser(behavior: suspend (String) -> String) { fake.configureGetUser(behavior) }
    fun updateUser(behavior: suspend (String, String) -> Boolean) { fake.configureUpdateUser(behavior) }
}
```

### **Example 3: Method-Only Interface**

**Input Interface**:
```kotlin
@Fake
interface AnalyticsService {
    fun track(event: String)
    fun identify(userId: String)
    fun flush()
}
```

**Generated Implementation**:
```kotlin
class FakeAnalyticsServiceImpl : AnalyticsService {
    private var trackBehavior: (String) -> Unit = { _ -> Unit }
    private var identifyBehavior: (String) -> Unit = { _ -> Unit }
    private var flushBehavior: () -> Unit = { Unit }

    override fun track(event: String): Unit = trackBehavior(event)
    override fun identify(userId: String): Unit = identifyBehavior(userId)
    override fun flush(): Unit = flushBehavior()

    internal fun configureTrack(behavior: (String) -> Unit) { trackBehavior = behavior }
    internal fun configureIdentify(behavior: (String) -> Unit) { identifyBehavior = behavior }
    internal fun configureFlush(behavior: () -> Unit) { flushBehavior = behavior }
}
```

## 🎯 **Generated API Patterns**

### **Factory Function Pattern**
```kotlin
// Pattern: fake{InterfaceName}(configure: Fake{InterfaceName}Config.() -> Unit = {})
fun fakeTestService(configure: FakeTestServiceConfig.() -> Unit = {}): TestService
fun fakeAsyncUserService(configure: FakeAsyncUserServiceConfig.() -> Unit = {}): AsyncUserService
fun fakeAnalyticsService(configure: FakeAnalyticsServiceConfig.() -> Unit = {}): AnalyticsService
```

### **Implementation Class Pattern**
```kotlin
// Pattern: Fake{InterfaceName}Impl : {InterfaceName}
class FakeTestServiceImpl : TestService
class FakeAsyncUserServiceImpl : AsyncUserService
class FakeAnalyticsServiceImpl : AnalyticsService
```

### **Configuration DSL Pattern**
```kotlin
// Pattern: Fake{InterfaceName}Config
class FakeTestServiceConfig(private val fake: FakeTestServiceImpl)
class FakeAsyncUserServiceConfig(private val fake: FakeAsyncUserServiceImpl)
class FakeAnalyticsServiceConfig(private val fake: FakeAnalyticsServiceImpl)
```

## 🔧 **Usage API**

### **Basic Usage**
```kotlin
// Default behavior (using smart defaults)
val service = fakeTestService()
assertEquals("", service.getValue()) // String default
assertEquals("", service.someValue) // Property default

// Custom behavior configuration
val customService = fakeTestService {
    getValue { "custom-value" }
    someValue { "awesome-someValue" }
    setValue { value -> println("Setting: $value") }
}
```

### **Suspend Function Usage**
```kotlin
@Test
fun `GIVEN async service WHEN calling suspend functions THEN should work correctly`() = runTest {
    // Default behavior
    val service = fakeAsyncUserService()
    assertEquals("", service.getUser("123"))
    assertEquals(false, service.updateUser("123", "New Name"))

    // Custom behavior
    val customService = fakeAsyncUserService {
        getUser { id -> "User-$id" }
        updateUser { id, name -> true }
    }

    assertEquals("User-123", customService.getUser("123"))
    assertEquals(true, customService.updateUser("123", "Updated"))
}
```

### **Testing Patterns**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    @Test
    fun `GIVEN fake user service WHEN getting user THEN should return configured value`() = runTest {
        // Given
        val service = fakeAsyncUserService {
            getUser { id -> "User-$id" }
        }

        // When
        val result = service.getUser("123")

        // Then
        assertEquals("User-123", result)
    }

    @Test
    fun `GIVEN analytics service WHEN tracking events THEN should execute configured behavior`() = runTest {
        // Given
        val events = mutableListOf<String>()
        val analytics = fakeAnalyticsService {
            track { event -> events.add(event) }
        }

        // When
        analytics.track("user_login")
        analytics.track("page_view")

        // Then
        assertEquals(listOf("user_login", "page_view"), events)
    }
}
```

## 📊 **Type System Support**

### **Smart Default Values**
```kotlin
// Primitive types
String → ""
Int → 0
Boolean → false
Unit → Unit

// Collections
List<T> → emptyList()
Map<K,V> → emptyMap()
Set<T> → emptySet()

// Special types
Result<T> → Result.success("")
Flow<T> → emptyFlow()

// Nullable types
String? → null
Int? → null
Any? → null

// Function types
() -> String → { "" }
(String) -> Unit → { _ -> Unit }
suspend (T) -> R → { _ -> defaultValue<R>() }
```

### **Function Type Resolution**
```kotlin
// Method signatures are preserved exactly
interface Repository {
    fun process(data: String, callback: (String) -> Unit): Boolean
}

// Generated:
private var processBehavior: (String, (String) -> Unit) -> Boolean = { _, _ -> false }
override fun process(data: String, callback: (String) -> Unit): Boolean = processBehavior(data, callback)
```

## 🚨 **Current Limitations**

### **Phase 2A: Method-Level Generics** ⚠️
```kotlin
// Currently requires workaround
interface GenericService {
    fun <T> process(data: T): T  // Method-level generic challenge
}

// Workaround: Use interface-level generics
interface GenericService<T> {
    fun process(data: T): T  // ✅ Fully supported
}
```

### **Advanced Features** 🔮
```kotlin
// Future features (not yet implemented)
@Fake(trackCalls = true)  // Call tracking
@Fake(builder = true)     // Builder pattern
@Fake(dependencies = [...]) // Cross-module dependencies
```

## 🔗 **Related Documentation**

- **[📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)** - GIVEN-WHEN-THEN patterns
- **[📋 Type Safety Validation](.claude/docs/validation/type-safety-validation.md)** - Type system testing
- **[📋 Working Examples](.claude/docs/examples/working-examples.md)** - Complete usage examples
- **[📋 Generic Scoping Analysis](.claude/docs/analysis/generic-scoping-analysis.md)** - Technical challenges

---

**This API specification reflects the current production-ready state of KtFakes with working examples validated against real generated code.**