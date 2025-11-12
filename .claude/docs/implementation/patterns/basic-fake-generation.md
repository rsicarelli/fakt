# Pattern: Basic Fake Generation

> **Purpose**: Fundamental patterns for generating fakes from simple interfaces
> **Complexity**: Beginner to Intermediate
> **Testing Standard**: [📋 Testing Guidelines](../validation/testing-guidelines.md)

## 🎯 **Pattern Overview**

Basic fake generation covers the most common use cases for KtFakes:
- Simple interfaces with methods and properties
- Basic type handling (primitives, strings, collections)
- Default behavior setup and custom configuration
- Foundation patterns for more complex scenarios

## 📋 **Pattern Categories**

### **Category 1: Method-Only Interfaces**

**When to Use**: Interfaces with only methods, no properties

```kotlin
@Fake
interface EmailService {
    fun sendEmail(to: String, subject: String, body: String): Boolean
    fun sendBulkEmail(recipients: List<String>, subject: String, body: String): Int
    fun validateEmail(email: String): Boolean
}
```

**Generated API**:
```kotlin
// Factory function
fun fakeEmailService(configure: FakeEmailServiceConfig.() -> Unit = {}): EmailService

// Configuration DSL
class FakeEmailServiceConfig(private val fake: FakeEmailServiceImpl) {
    fun sendEmail(behavior: (String, String, String) -> Boolean)
    fun sendBulkEmail(behavior: (List<String>, String, String) -> Int)
    fun validateEmail(behavior: (String) -> Boolean)
}
```

**Usage Patterns**:
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailServiceTest {

    @Test
    fun `GIVEN email service WHEN using defaults THEN should provide sensible behavior`() = runTest {
        // Given - Default behavior
        val emailService = fakeEmailService()

        // When & Then - Smart defaults
        assertEquals(false, emailService.sendEmail("test@example.com", "Subject", "Body"))
        assertEquals(0, emailService.sendBulkEmail(listOf("a@test.com"), "Subject", "Body"))
        assertEquals(false, emailService.validateEmail("invalid"))
    }

    @Test
    fun `GIVEN email service WHEN configured THEN should use custom behavior`() = runTest {
        // Given - Custom behavior
        val sentEmails = mutableListOf<Triple<String, String, String>>()
        val emailService = fakeEmailService {
            sendEmail { to, subject, body ->
                sentEmails.add(Triple(to, subject, body))
                true
            }
            validateEmail { email ->
                email.contains("@") && email.contains(".")
            }
        }

        // When
        val result = emailService.sendEmail("john@example.com", "Hello", "Hi John!")
        val isValid = emailService.validateEmail("john@example.com")

        // Then
        assertTrue(result)
        assertTrue(isValid)
        assertEquals(1, sentEmails.size)
        assertEquals("john@example.com", sentEmails[0].first)
    }
}
```

### **Category 2: Property-Only Interfaces**

**When to Use**: Configuration interfaces or data providers

```kotlin
@Fake
interface AppConfig {
    val apiUrl: String
    val timeout: Int
    val retryCount: Int
    val isDebugMode: Boolean
}
```

**Generated API**:
```kotlin
class FakeAppConfigImpl : AppConfig {
    private var apiUrlBehavior: () -> String = { "" }
    private var timeoutBehavior: () -> Int = { 0 }
    private var retryCountBehavior: () -> Int = { 0 }
    private var isDebugModeBehavior: () -> Boolean = { false }

    override val apiUrl: String get() = apiUrlBehavior()
    override val timeout: Int get() = timeoutBehavior()
    override val retryCount: Int get() = retryCountBehavior()
    override val isDebugMode: Boolean get() = isDebugModeBehavior()
}
```

**Usage Patterns**:
```kotlin
@Test
fun `GIVEN app config WHEN configuring properties THEN should provide configured values`() = runTest {
    // Given
    val config = fakeAppConfig {
        apiUrl { "https://api.test.com" }
        timeout { 5000 }
        retryCount { 3 }
        isDebugMode { true }
    }

    // When & Then
    assertEquals("https://api.test.com", config.apiUrl)
    assertEquals(5000, config.timeout)
    assertEquals(3, config.retryCount)
    assertTrue(config.isDebugMode)
}
```

### **Category 3: Mixed Interfaces (Properties + Methods)**

**When to Use**: Most common pattern - services with state and behavior

```kotlin
@Fake
interface UserService {
    val currentUser: String?
    val isLoggedIn: Boolean
    fun login(username: String, password: String): Boolean
    fun logout(): Unit
    fun getProfile(userId: String): User?
}
```

**Usage Pattern**:
```kotlin
@Test
fun `GIVEN user service WHEN handling login flow THEN should manage state correctly`() = runTest {
    // Given - Stateful fake
    var loggedInUser: String? = null
    val userService = fakeUserService {
        currentUser { loggedInUser }
        isLoggedIn { loggedInUser != null }
        login { username, password ->
            if (username == "admin" && password == "secret") {
                loggedInUser = username
                true
            } else false
        }
        logout { loggedInUser = null }
        getProfile { userId ->
            if (userId == loggedInUser) User(userId, "$userId@example.com", userId)
            else null
        }
    }

    // When & Then - Progressive state changes
    assertFalse(userService.isLoggedIn)
    assertNull(userService.currentUser)

    assertTrue(userService.login("admin", "secret"))
    assertTrue(userService.isLoggedIn)
    assertEquals("admin", userService.currentUser)

    val profile = userService.getProfile("admin")
    assertNotNull(profile)
    assertEquals("admin@example.com", profile?.email)

    userService.logout()
    assertFalse(userService.isLoggedIn)
    assertNull(userService.currentUser)
}
```

## 🔧 **Type Handling Patterns**

### **Primitive Types**
```kotlin
@Fake
interface PrimitiveService {
    fun getString(): String
    fun getInt(): Int
    fun getBoolean(): Boolean
    fun getDouble(): Double
    fun getLong(): Long
}

// Default behaviors:
val service = fakePrimitiveService()
assertEquals("", service.getString())      // String → ""
assertEquals(0, service.getInt())          // Int → 0
assertEquals(false, service.getBoolean())  // Boolean → false
assertEquals(0.0, service.getDouble())     // Double → 0.0
assertEquals(0L, service.getLong())        // Long → 0L
```

### **Collection Types**
```kotlin
@Fake
interface CollectionService {
    fun getList(): List<String>
    fun getMap(): Map<String, Int>
    fun getSet(): Set<String>
    fun getMutableList(): MutableList<String>
}

// Default behaviors:
val service = fakeCollectionService()
assertTrue(service.getList().isEmpty())        // List → emptyList()
assertTrue(service.getMap().isEmpty())         // Map → emptyMap()
assertTrue(service.getSet().isEmpty())         // Set → emptySet()
assertTrue(service.getMutableList().isEmpty()) // MutableList → mutableListOf()
```

### **Nullable Types**
```kotlin
@Fake
interface NullableService {
    fun getOptionalString(): String?
    fun getOptionalUser(): User?
    fun processOptional(data: String?): Boolean
}

// Default behaviors:
val service = fakeNullableService()
assertNull(service.getOptionalString())  // String? → null
assertNull(service.getOptionalUser())    // User? → null

// Custom behavior:
val customService = fakeNullableService {
    getOptionalString { "not-null" }
    processOptional { data -> data != null }
}
assertEquals("not-null", customService.getOptionalString())
```

### **Result Types**
```kotlin
@Fake
interface ResultService {
    fun getResult(): Result<String>
    fun processResult(data: String): Result<Boolean>
}

// Default behaviors:
val service = fakeResultService()
assertTrue(service.getResult().isSuccess)           // Result<T> → Result.success("")
assertEquals("", service.getResult().getOrNull())   // Success with empty string

// Custom behavior:
val customService = fakeResultService {
    getResult { Result.success("custom-data") }
    processResult { data ->
        if (data.isNotEmpty()) Result.success(true)
        else Result.failure(Exception("Empty data"))
    }
}
```

## 🧪 **Testing Patterns**

### **State Verification Pattern**
```kotlin
@Test
fun `GIVEN service with state WHEN performing operations THEN should maintain correct state`() = runTest {
    // Given - Setup stateful fake
    val items = mutableListOf<String>()
    val service = fakeStorageService {
        addItem { item -> items.add(item); true }
        getItems { items.toList() }
        getCount { items.size }
        clear { items.clear() }
    }

    // When & Then - Progressive verification
    assertEquals(0, service.getCount())
    assertTrue(service.addItem("item1"))
    assertEquals(1, service.getCount())
    assertEquals(listOf("item1"), service.getItems())

    assertTrue(service.addItem("item2"))
    assertEquals(2, service.getCount())

    service.clear()
    assertEquals(0, service.getCount())
    assertTrue(service.getItems().isEmpty())
}
```

### **Behavior Verification Pattern**
```kotlin
@Test
fun `GIVEN service WHEN calling methods THEN should execute configured behaviors`() = runTest {
    // Given - Track method calls
    val methodCalls = mutableListOf<String>()
    val service = fakeTrackingService {
        method1 { param ->
            methodCalls.add("method1($param)")
            "result1"
        }
        method2 { param1, param2 ->
            methodCalls.add("method2($param1, $param2)")
            true
        }
    }

    // When
    service.method1("test")
    service.method2("a", "b")

    // Then
    assertEquals(2, methodCalls.size)
    assertEquals("method1(test)", methodCalls[0])
    assertEquals("method2(a, b)", methodCalls[1])
}
```

### **Error Handling Pattern**
```kotlin
@Test
fun `GIVEN service WHEN errors occur THEN should handle appropriately`() = runTest {
    // Given - Service with error conditions
    val service = fakeFileService {
        readFile { filename ->
            when {
                filename.isEmpty() -> Result.failure(IllegalArgumentException("Empty filename"))
                filename == "missing.txt" -> Result.failure(FileNotFoundException("File not found"))
                filename == "locked.txt" -> Result.failure(IOException("File locked"))
                else -> Result.success("File content")
            }
        }
    }

    // When & Then - Test various error conditions
    assertTrue(service.readFile("").isFailure)
    assertTrue(service.readFile("missing.txt").isFailure)
    assertTrue(service.readFile("locked.txt").isFailure)
    assertTrue(service.readFile("valid.txt").isSuccess)
}
```

## 📊 **Common Anti-Patterns**

### **❌ Overly Complex Fakes**
```kotlin
// Don't do this - too much logic in fake
val service = fakeComplexService {
    processData { data ->
        // 50 lines of complex business logic
        // This should be in the real implementation, not the fake
    }
}
```

### **✅ Simple, Focused Fakes**
```kotlin
// Do this - simple, predictable behavior
val service = fakeSimpleService {
    processData { data ->
        if (data.isValid) ProcessedData(data.id, "processed")
        else ProcessedData(data.id, "invalid")
    }
}
```

### **❌ Shared Mutable State**
```kotlin
// Don't do this - shared state between tests
companion object {
    val sharedState = mutableListOf<String>()  // ❌ Tests affect each other
}
```

### **✅ Test-Isolated State**
```kotlin
// Do this - each test has its own state
@Test
fun `test with isolated state`() = runTest {
    val localState = mutableListOf<String>()  // ✅ Isolated per test
    val service = fakeService {
        addItem { item -> localState.add(item); true }
    }
}
```

## 🔗 **Related Patterns**

- **[📋 Suspend Function Handling](suspend-function-handling.md)** - Async/coroutine patterns
- **[📋 Complex Generics Strategy](complex-generics-strategy.md)** - Generic type handling
- **[📋 Multi-Interface Projects](multi-interface-projects.md)** - Enterprise scenarios
- **[📋 Working Examples](../examples/working-examples.md)** - Real-world usage

---

**Basic fake generation patterns provide the foundation for all KtFakes usage. Master these patterns before moving to more advanced scenarios.**