# KtFakes Annotations Reference

> **Purpose**: Complete reference for all KtFakes annotations and their usage
> **Status**: Production-Ready Core + Future Features Documented
> **Testing Standard**: [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)

## 🎯 **Core Annotation: @Fake**

### **Basic Definition**
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

### **Parameters Reference**

#### **trackCalls: Boolean = false**
**Status**: 🔮 Future Feature (Phase 2B)
**Purpose**: Enable method call tracking and verification

```kotlin
@Fake(trackCalls = true)
interface UserService {
    fun getUser(id: String): User
    fun updateUser(user: User): Boolean
}

// Future generated API:
val service = fakeUserService()
service.getUser("123")
service.getUser("456")

// Verification API (future):
verify(service).getUser("123") // Called once
verify(service).getUser(any()) // Called twice
verify(service).updateUser(never()) // Never called
```

#### **builder: Boolean = false**
**Status**: 🔮 Future Feature (Phase 2B)
**Purpose**: Generate builder pattern for data class creation

```kotlin
@Fake(builder = true)
interface UserRepository {
    fun save(user: User): User
}

// Future generated API:
val user = buildUser {
    id = "123"
    name = "John Doe"
    email = "john@example.com"
}
```

#### **concurrent: Boolean = false**
**Status**: 🔮 Future Feature (Phase 2B)
**Purpose**: Thread-safe fake implementation with synchronization

```kotlin
@Fake(concurrent = true)
interface CacheService {
    fun put(key: String, value: Any)
    fun get(key: String): Any?
}

// Generated with thread-safe behavior storage
// Uses ConcurrentHashMap and atomic operations
```

#### **scope: String = "test"**
**Status**: ✅ Current Implementation
**Purpose**: Control where fakes are generated

```kotlin
@Fake(scope = "test")        // Default: test source sets only
@Fake(scope = "debug")       // Debug builds only
@Fake(scope = "all")         // All source sets (NOT recommended)
```

**Security Validation**:
```kotlin
@Test
fun `GIVEN fake annotation WHEN in main source set THEN should reject generation`() = runTest {
    // Security constraint: fakes only in test source sets
    // Compiler plugin validates source set type before generation
}
```

#### **dependencies: Array<KClass<*>> = []**
**Status**: 🔮 Future Feature (Phase 2C)
**Purpose**: Cross-module fake dependency injection

```kotlin
@Fake(dependencies = [UserRepository::class, EmailService::class])
interface UserManager {
    fun createUser(data: UserData): User
}

// Future generated API with dependency injection:
val userManager = fakeUserManager {
    // Automatic injection of fake dependencies
    dependencies {
        userRepository = fakeUserRepository { ... }
        emailService = fakeEmailService { ... }
    }
}
```

## 🔧 **Usage Patterns**

### **Basic Usage (Current)**
```kotlin
@Fake
interface ApiService {
    suspend fun fetchData(): Result<String>
    fun processData(data: String): Boolean
}

// Generated factory function
val service = fakeApiService {
    fetchData { Result.success("test-data") }
    processData { data -> data.isNotEmpty() }
}
```

### **Advanced Configuration (Current)**
```kotlin
@Test
fun `GIVEN complex service WHEN configuring behaviors THEN should work correctly`() = runTest {
    // Given
    val apiService = fakeApiService {
        fetchData {
            if (Math.random() > 0.5) Result.success("data")
            else Result.failure(Exception("Network error"))
        }
        processData { data ->
            println("Processing: $data")
            true
        }
    }

    // When & Then
    val result = apiService.fetchData()
    assertTrue(result.isSuccess || result.isFailure)
    assertTrue(apiService.processData("test"))
}
```

## 📋 **Annotation Validation**

### **Compile-Time Validation**
```kotlin
@Test
fun `GIVEN invalid annotation parameters WHEN compiling THEN should report errors`() = runTest {
    // Invalid target - should fail compilation
    // @Fake
    // class MyClass { ... } // Error: @Fake only for interfaces

    // Invalid scope - should warn
    // @Fake(scope = "invalid")
    // interface Service { ... } // Warning: Unknown scope
}
```

### **Source Set Validation**
```kotlin
@Test
fun `GIVEN fake annotation WHEN in production code THEN should be ignored`() = runTest {
    // Security test: production code should not process @Fake
    // Only test source sets: src/test/kotlin, src/jvmTest/kotlin, etc.
}
```

## 🔮 **Future Annotations (Design)**

### **@FakeConfig**
```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class FakeConfig(
    val defaultBehavior: DefaultBehavior = DefaultBehavior.RETURN_DEFAULTS,
    val strictMode: Boolean = false,
    val generateDocs: Boolean = true
)

enum class DefaultBehavior {
    RETURN_DEFAULTS,  // Current behavior
    THROW_EXCEPTION,  // Throw for unconfigured methods
    NO_OP            // Do nothing for Unit methods
}
```

### **@CallTracking**
```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class CallTracking(
    val enabled: Boolean = true,
    val captureArgs: Boolean = true,
    val captureReturn: Boolean = false
)

// Usage:
@Fake(trackCalls = true)
interface UserService {
    @CallTracking(captureArgs = true, captureReturn = true)
    fun getUser(id: String): User

    @CallTracking(enabled = false)
    fun internalMethod(): Unit  // Not tracked
}
```

### **@MockBehavior**
```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class MockBehavior(
    val defaultReturn: String = "",
    val throwsException: KClass<out Exception> = Nothing::class,
    val delay: Long = 0L  // For suspend functions
)

// Usage:
@Fake
interface ApiService {
    @MockBehavior(defaultReturn = """{"status": "ok"}""")
    suspend fun fetchData(): String

    @MockBehavior(throwsException = NetworkException::class)
    suspend fun uploadData(data: String): Unit
}
```

## 🧪 **Testing Annotation Behavior**

### **Annotation Processing Tests**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnnotationProcessingTest {

    @Test
    fun `GIVEN interface with Fake annotation WHEN processing THEN should generate fakes`() = runTest {
        // Test annotation detection and processing
    }

    @Test
    fun `GIVEN interface without Fake annotation WHEN processing THEN should skip generation`() = runTest {
        // Test annotation requirement validation
    }

    @Test
    fun `GIVEN Fake annotation with invalid parameters WHEN processing THEN should report error`() = runTest {
        // Test parameter validation
    }
}
```

### **Generated Code Validation**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GeneratedCodeValidationTest {

    @Test
    fun `GIVEN annotated interface WHEN generated THEN should preserve annotation intent`() = runTest {
        // Validate that generated code reflects annotation parameters
    }

    @Test
    fun `GIVEN scope parameter WHEN generating THEN should respect source set constraints`() = runTest {
        // Test scope validation and security constraints
    }
}
```

## 🔗 **Related Documentation**

- **[📋 API Specifications](.claude/docs/api/specifications.md)** - Complete API reference
- **[📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)** - GIVEN-WHEN-THEN patterns
- **[📋 Type Safety Validation](.claude/docs/validation/type-safety-validation.md)** - Type system behavior
- **[📋 Working Examples](.claude/docs/examples/working-examples.md)** - Practical usage patterns

---

**This annotation reference covers both current production features and documented future enhancements for KtFakes development planning.**