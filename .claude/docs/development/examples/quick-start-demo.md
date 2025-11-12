# Quick Start Demo - KtFakes in 5 Minutes

> **Purpose**: Get started with KtFakes in minimal time with working examples
> **Status**: All Examples Tested and Working
> **Testing Standard**: [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)

## 🚀 **5-Minute Quick Start**

### **Step 1: Add KtFakes to Your Project (30 seconds)**

```kotlin
// build.gradle.kts (test module)
dependencies {
    implementation("dev.rsicarelli.ktfake:annotations:1.0.0")
    testImplementation("dev.rsicarelli.ktfake:compiler:1.0.0")
}
```

### **Step 2: Create Your First Fake (1 minute)**

```kotlin
// Define your interface
@Fake
interface UserService {
    fun getUser(id: String): String
    fun isActive(id: String): Boolean
}

// That's it! KtFakes generates everything automatically.
```

### **Step 3: Use It in Tests (2 minutes)**

```kotlin
@Test
fun `user service should work`() = runTest {
    // Default behavior (smart defaults)
    val service = fakeUserService()
    assertEquals("", service.getUser("123"))  // Returns empty string
    assertEquals(false, service.isActive("123"))  // Returns false

    // Custom behavior
    val customService = fakeUserService {
        getUser { id -> "User-$id" }
        isActive { id -> id.isNotEmpty() }
    }

    assertEquals("User-123", customService.getUser("123"))
    assertTrue(customService.isActive("123"))
}
```

### **Step 4: Advanced Usage (1.5 minutes)**

```kotlin
@Fake
interface ApiClient {
    suspend fun fetchData(url: String): Result<String>
    val baseUrl: String
}

@Test
fun `api client should handle suspend functions`() = runTest {
    val client = fakeApiClient {
        fetchData { url ->
            when {
                url.contains("error") -> Result.failure(Exception("API Error"))
                else -> Result.success("""{"data": "success"}""")
            }
        }
        baseUrl { "https://api.example.com" }
    }

    val success = client.fetchData("https://api.example.com/users")
    assertTrue(success.isSuccess)
    assertEquals("""{"data": "success"}""", success.getOrNull())

    val error = client.fetchData("https://api.example.com/error")
    assertTrue(error.isFailure)
}
```

🎉 **You're done!** KtFakes automatically generated type-safe fakes with smart defaults and configuration DSL.

## 📋 **What You Get Automatically**

### **Generated Code Structure**
For `@Fake interface UserService`, KtFakes generates:

1. **Implementation**: `FakeUserServiceImpl`
2. **Factory Function**: `fakeUserService { ... }`
3. **Configuration DSL**: `FakeUserServiceConfig`

### **Type-Safe Configuration**
```kotlin
val service = fakeUserService {
    getUser { id -> "User-$id" }  // Type: (String) -> String
    isActive { id -> true }       // Type: (String) -> Boolean
    // setValue { 123 }           // ❌ Won't compile - type mismatch!
}
```

### **Smart Defaults**
- `String` → `""`
- `Boolean` → `false`
- `Int` → `0`
- `List<T>` → `emptyList()`
- `Result<T>` → `Result.success("")`
- `suspend fun` → Works automatically!

## 🎯 **Common Patterns**

### **Pattern 1: Service Testing**
```kotlin
@Fake 
interface PaymentService {
    suspend fun processPayment(amount: Double): Result<String>
}

val paymentService = fakePaymentService {
    processPayment { amount ->
        if (amount > 0) Result.success("tx_123")
        else Result.failure(Exception("Invalid amount"))
    }
}
```

### **Pattern 2: Repository Testing**
```kotlin
@Fake 
interface UserRepository {
    suspend fun findById(id: String): User?
    suspend fun save(user: User): User
}

val users = mutableMapOf<String, User>()
val repository = fakeUserRepository {
    findById { id -> users[id] }
    save { user -> users[user.id] = user; user }
}
```

### **Pattern 3: Event Tracking**
```kotlin
@Fake 
interface Analytics {
    fun track(event: String)
    fun identify(userId: String)
}

val events = mutableListOf<String>()
val analytics = fakeAnalytics {
    track { event -> events.add(event) }
    identify { userId -> events.add("identify:$userId") }
}
```

## ⚡ **Real-World Example: User Registration**

```kotlin
// Your business logic
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val analytics: Analytics
) {
    suspend fun registerUser(email: String, name: String): Result<User> {
        return try {
            val user = User(id = UUID.randomUUID().toString(), email, name)
            val savedUser = userRepository.save(user)
            emailService.sendWelcomeEmail(email, name)
            analytics.track("user_registered")
            Result.success(savedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Your test with KtFakes
@Test
fun `user registration should work end-to-end`() = runTest {
    // Given - Setup fakes
    val savedUsers = mutableListOf<User>()
    val sentEmails = mutableListOf<Pair<String, String>>()
    val trackedEvents = mutableListOf<String>()

    val userRepository = fakeUserRepository {
        save { user -> savedUsers.add(user); user }
    }

    val emailService = fakeEmailService {
        sendWelcomeEmail { email, name -> sentEmails.add(email to name) }
    }

    val analytics = fakeAnalytics {
        track { event -> trackedEvents.add(event) }
    }

    val registrationService = UserRegistrationService(userRepository, emailService, analytics)

    // When
    val result = registrationService.registerUser("john@example.com", "John Doe")

    // Then
    assertTrue(result.isSuccess)
    assertEquals(1, savedUsers.size)
    assertEquals("john@example.com", savedUsers[0].email)
    assertEquals(listOf("john@example.com" to "John Doe"), sentEmails)
    assertEquals(listOf("user_registered"), trackedEvents)
}
```

## 🔧 **Development Workflow**

### **1. Build the Plugin**
```bash
./gradlew :compiler:shadowJar
```

### **2. Test Generation**
```bash
cd test-sample
../gradlew compileKotlinJvm  # Generates fakes automatically
```

### **3. Check Generated Code**
```bash
ls build/generated/ktfake/test/kotlin/
# See your generated fakes!
```

### **4. Debug Issues**
```bash
# Enable verbose output
../gradlew compileKotlinJvm -i | grep -E "(KtFakes|Generated|ERROR)"
```

## 🧪 **Testing Best Practices**

### **Use GIVEN-WHEN-THEN Pattern**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceTest {
    @Test
    fun `GIVEN service with custom behavior WHEN calling method THEN should return expected result`() = runTest {
        // Given
        val service = fakeService {
            getValue { "expected" }
        }

        // When
        val result = service.getValue()

        // Then
        assertEquals("expected", result)
    }
}
```

### **Test State Changes**
```kotlin
@Test
fun `GIVEN stateful service WHEN multiple calls THEN should maintain state`() = runTest {
    // Given
    var counter = 0
    val service = fakeCounterService {
        increment { ++counter }
        getCount { counter }
    }

    // When & Then
    assertEquals(0, service.getCount())
    service.increment()
    assertEquals(1, service.getCount())
    service.increment()
    assertEquals(2, service.getCount())
}
```

## 🚨 **Common Gotchas**

### **❌ Wrong: Mutable state in lambda**
```kotlin
// Don't do this - will capture initial value
var count = 0
val service = fakeService {
    getCount { count }  // Will always return 0
}
count = 5  // This won't affect the fake
```

### **✅ Right: Mutable state via reference**
```kotlin
// Do this - capture by reference
val state = AtomicInteger(0)
val service = fakeService {
    getCount { state.get() }
    increment { state.incrementAndGet() }
}
```

### **❌ Wrong: Forget @Fake annotation**
```kotlin
interface Service {  // Missing @Fake - no generation!
    fun getValue(): String
}
```

### **✅ Right: Always use @Fake**
```kotlin
@Fake
interface Service {  // ✅ Will generate fakeService()
    fun getValue(): String
}
```

## 🔗 **Next Steps**

1. **[📋 Working Examples](.claude/docs/examples/working-examples.md)** - More complex scenarios
2. **[📋 API Specifications](.claude/docs/api/specifications.md)** - Complete API reference
3. **[📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)** - Testing best practices
4. **[📋 Common Issues](.claude/docs/troubleshooting/common-issues.md)** - Problem solving

---

**You're now ready to use KtFakes in your projects! Start with simple interfaces and gradually explore more advanced patterns.**