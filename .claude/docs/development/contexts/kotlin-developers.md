# Context: Kotlin Developers Using KtFakes

> **Target Audience**: Kotlin developers adopting KtFakes for test automation
> **Experience Level**: Intermediate to advanced Kotlin developers
> **Testing Standard**: [📋 Testing Guidelines](../validation/testing-guidelines.md)

## 🎯 **Developer Profile**

### **Who This Is For**
- **Kotlin developers** with 1+ years experience
- **Testing enthusiasts** who value type safety
- **Quality-focused teams** seeking better mocking solutions
- **Developers** frustrated with MockK complexity or performance issues

### **Common Scenarios**
- Migrating from MockK/Mockito to type-safe alternatives
- Building new Kotlin projects with modern testing approaches
- Seeking compile-time validation for mock behaviors
- Working with suspend functions and coroutines extensively

## 🚀 **Quick Onboarding for Kotlin Devs**

### **Familiar Patterns**
```kotlin
// If you're used to MockK...
val mockService = mockk<UserService>()
every { mockService.getUser("123") } returns User("123", "John")

// KtFakes feels natural:
val fakeService = fakeUserService {
    getUser { id -> User(id, "John") }
}
```

### **Type Safety Benefits**
```kotlin
// MockK: Runtime verification
every { service.getUser(any()) } returns User("", "")
// service.getUser("123")  // If method signature changes, tests still pass (bad!)

// KtFakes: Compile-time verification
val service = fakeUserService {
    getUser { id -> User(id, "Default") }  // If signature changes, won't compile (good!)
}
```

### **Coroutines Integration**
```kotlin
// Suspend functions work naturally
@Fake
interface ApiClient {
    suspend fun fetchUser(id: String): Result<User>
    suspend fun uploadData(data: ByteArray): Result<String>
}

val client = fakeApiClient {
    fetchUser { id ->
        delay(10)  // Can use coroutine features naturally
        Result.success(User(id, "Test User"))
    }
    uploadData { data ->
        Result.success("upload_${data.size}")
    }
}
```

## 📋 **Common Development Patterns**

### **Pattern 1: Repository Testing**
```kotlin
@Fake
interface UserRepository {
    suspend fun findById(id: String): User?
    suspend fun save(user: User): User
    suspend fun findByEmail(email: String): User?
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    @Test
    fun `GIVEN user repository WHEN finding user THEN should return correct user`() = runTest {
        // Given
        val users = mapOf("123" to User("123", "john@example.com", "John"))
        val repository = fakeUserRepository {
            findById { id -> users[id] }
            findByEmail { email -> users.values.find { it.email == email } }
        }

        // When
        val userById = repository.findById("123")
        val userByEmail = repository.findByEmail("john@example.com")

        // Then
        assertEquals("John", userById?.name)
        assertEquals("123", userByEmail?.id)
    }
}
```

### **Pattern 2: Service Layer Testing**
```kotlin
class UserService(
    private val repository: UserRepository,
    private val emailService: EmailService
) {
    suspend fun registerUser(email: String, name: String): Result<User> {
        return try {
            val existingUser = repository.findByEmail(email)
            if (existingUser != null) {
                return Result.failure(Exception("User already exists"))
            }

            val user = User(UUID.randomUUID().toString(), email, name)
            val savedUser = repository.save(user)
            emailService.sendWelcomeEmail(email, name)
            Result.success(savedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Test
fun `GIVEN user service WHEN registering new user THEN should save and send email`() = runTest {
    // Given
    val savedUsers = mutableListOf<User>()
    val sentEmails = mutableListOf<Pair<String, String>>()

    val repository = fakeUserRepository {
        findByEmail { email -> savedUsers.find { it.email == email } }
        save { user -> savedUsers.add(user); user }
    }

    val emailService = fakeEmailService {
        sendWelcomeEmail { email, name -> sentEmails.add(email to name) }
    }

    val userService = UserService(repository, emailService)

    // When
    val result = userService.registerUser("john@example.com", "John Doe")

    // Then
    assertTrue(result.isSuccess)
    assertEquals(1, savedUsers.size)
    assertEquals("john@example.com", savedUsers[0].email)
    assertEquals(listOf("john@example.com" to "John Doe"), sentEmails)
}
```

### **Pattern 3: API Client Testing**
```kotlin
@Fake
interface HttpClient {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): Result<String>
    suspend fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): Result<String>
}

class ApiService(private val httpClient: HttpClient) {
    suspend fun fetchUsers(): Result<List<User>> {
        return when (val response = httpClient.get("/users")) {
            is Result.Success -> {
                try {
                    val users = Json.decodeFromString<List<User>>(response.value)
                    Result.success(users)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            is Result.Failure -> Result.failure(response.exception)
        }
    }
}

@Test
fun `GIVEN api service WHEN fetching users THEN should parse response correctly`() = runTest {
    // Given
    val httpClient = fakeHttpClient {
        get { url ->
            when (url) {
                "/users" -> Result.success("""[{"id":"1","name":"John"}]""")
                else -> Result.failure(Exception("Not found"))
            }
        }
    }
    val apiService = ApiService(httpClient)

    // When
    val result = apiService.fetchUsers()

    // Then
    assertTrue(result.isSuccess)
    val users = result.getOrNull()!!
    assertEquals(1, users.size)
    assertEquals("John", users[0].name)
}
```

## 🧪 **Testing Best Practices for Kotlin Devs**

### **GIVEN-WHEN-THEN Structure**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceTest {

    @Test
    fun `GIVEN service with behavior WHEN calling method THEN should return expected result`() = runTest {
        // Given - Setup state and dependencies
        val service = fakeService {
            getValue { "expected-value" }
        }

        // When - Execute the action being tested
        val result = service.getValue()

        // Then - Verify the outcome
        assertEquals("expected-value", result)
    }
}
```

### **State Management in Tests**
```kotlin
@Test
fun `GIVEN stateful service WHEN multiple operations THEN should maintain state correctly`() = runTest {
    // Given - Create mutable state container
    val cache = mutableMapOf<String, String>()
    val cacheService = fakeCacheService {
        put { key, value -> cache[key] = value; true }
        get { key -> cache[key] }
        size { cache.size }
    }

    // When & Then - Progressive state verification
    assertEquals(0, cacheService.size)

    assertTrue(cacheService.put("key1", "value1"))
    assertEquals(1, cacheService.size)
    assertEquals("value1", cacheService.get("key1"))

    assertTrue(cacheService.put("key2", "value2"))
    assertEquals(2, cacheService.size)
}
```

### **Error Scenario Testing**
```kotlin
@Test
fun `GIVEN service with error conditions WHEN errors occur THEN should handle gracefully`() = runTest {
    // Given
    val service = fakeApiClient {
        fetchData { url ->
            when {
                url.contains("timeout") -> Result.failure(TimeoutException("Request timeout"))
                url.contains("forbidden") -> Result.failure(HttpException(403, "Forbidden"))
                url.contains("success") -> Result.success("Valid data")
                else -> Result.failure(Exception("Unknown error"))
            }
        }
    }

    // When & Then
    assertTrue(service.fetchData("timeout-endpoint").isFailure)
    assertTrue(service.fetchData("forbidden-endpoint").isFailure)
    assertTrue(service.fetchData("success-endpoint").isSuccess)
}
```

## 🔧 **IDE Integration Tips**

### **IntelliJ IDEA Setup**
```kotlin
// Generated fakes show up in IDE with full autocomplete
val service = fakeUserService {
    getUser { id ->  // ← Full type inference and autocomplete
        User(id, "test@example.com", "Test User")
    }
    // IDE suggests all available methods from UserService interface
}
```

### **Debugging Generated Code**
```kotlin
// Generated code is in build/generated/fakt/test/kotlin/
// You can navigate to generated implementation to understand behavior
// Set breakpoints in your test configuration lambdas for debugging
```

### **Build Integration**
```kotlin
// build.gradle.kts
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Ensure fakes generate before tests run
tasks.named("compileTestKotlin") {
    dependsOn("compileKotlin")  // Plugin runs during test compilation
}
```

## 🚨 **Common Kotlin Developer Gotchas**

### **❌ Captured Variable Issue**
```kotlin
// Don't do this - will capture initial value
var counter = 0
val service = fakeCounterService {
    getCount { counter }  // Always returns 0!
}
counter = 5  // This doesn't affect the fake
```

### **✅ Use Mutable References**
```kotlin
// Do this instead
val counter = AtomicInteger(0)
val service = fakeCounterService {
    getCount { counter.get() }
    increment { counter.incrementAndGet() }
}
```

### **❌ Forgetting Suspend Context**
```kotlin
// Wrong - blocking call in suspend context
val service = fakeApiService {
    fetchData { url ->
        Thread.sleep(1000)  // ❌ Don't block coroutine
        Result.success("data")
    }
}
```

### **✅ Use Coroutine Features**
```kotlin
// Right - use coroutine features
val service = fakeApiService {
    fetchData { url ->
        delay(10)  // ✅ Use delay for timing
        Result.success("data")
    }
}
```

## 🔗 **Migration Guides**

### **From MockK**
```kotlin
// MockK
val mockService = mockk<UserService>()
every { mockService.getUser(any()) } returns User("123", "John")
verify { mockService.getUser("123") }

// KtFakes equivalent
val fakeService = fakeUserService {
    getUser { id -> User(id, "John") }
}
// Call tracking available in future versions
```

### **From Mockito-Kotlin**
```kotlin
// Mockito-Kotlin
val mockService = mock<UserService>()
whenever(mockService.getUser(any())).thenReturn(User("123", "John"))

// KtFakes equivalent
val fakeService = fakeUserService {
    getUser { id -> User(id, "John") }
}
```

## 🔗 **Related Documentation**

- **[📋 Quick Start Demo](../examples/quick-start-demo.md)** - Get started in 5 minutes
- **[📋 Working Examples](../examples/working-examples.md)** - Real-world patterns
- **[📋 Testing Guidelines](../validation/testing-guidelines.md)** - THE ABSOLUTE STANDARD
- **[📋 Common Issues](../troubleshooting/common-issues.md)** - Problem solving

---

**KtFakes provides the type safety and developer experience that Kotlin developers expect, with seamless integration into modern Kotlin development workflows.**