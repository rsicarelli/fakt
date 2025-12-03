# Fakes Over Mocks

Why Fakt generates fakes instead of using traditional mocking frameworks.

---

## The Problem with Mocks

Traditional mocking frameworks like MockK and Mockito have several limitations:

### **Runtime Reflection**
- Heavy runtime overhead
- Not available on all KMP targets (Native, WASM)
- Complex setup for multiplatform projects

### **No Compile-Time Safety**
- Mock setup doesn't break when interfaces change
- Refactoring can silently break tests
- Type safety is lost with `any()` matchers

### **Complex APIs**
- Steep learning curve
- Verbose setup for simple scenarios
- Magic strings and complex DSL

---

## How Fakes Are Better

Fakt generates **fakes** (not mocks) that solve these problems:

### **✅ Compile-Time Generation**
```kotlin
@Fake
interface UserService {
    fun getUser(id: String): User
}

// Generated at compile-time:
class FakeUserServiceImpl : UserService {
    private var getUserBehavior: (String) -> User = { id ->
        User(id, "Default Name")
    }

    fun configureGetUser(behavior: (String) -> User) {
        getUserBehavior = behavior
    }

    override fun getUser(id: String): User = getUserBehavior(id)
}
```

### **✅ Type Safety**
```kotlin
// This breaks at COMPILE TIME if interface changes
val fake = fakeUserService {
    getUser { id -> User(id, "Test User") }  // Typed parameters
}
```

### **✅ Universal KMP Support**
Works on **all** Kotlin targets without reflection:
- JVM, Android, iOS, Native, JS, WASM
- No runtime dependencies
- Zero overhead in production

### **✅ Thread-Safe Call Tracking**
```kotlin
val fake = fakeUserService()

fake.getUser("123")
fake.getUser("456")

// Built-in StateFlow tracking
assertEquals(2, fake.getUserCallCount.value)
```

---

## Comparison

| Feature | MockK/Mockito | Fakt |
|---------|---------------|------|
| **KMP Support** | Limited | Universal |
| **Compile-time Safety** | ❌ | ✅ |
| **Runtime Overhead** | Heavy | Zero |
| **Type Safety** | Partial | Complete |
| **Learning Curve** | Steep | Gentle |
| **Call Tracking** | Manual | Built-in |

---

## Migration Example

### **Before (MockK):**
```kotlin
@Test
fun `test user service`() = runTest {
    val mockService = mockk<UserService>()

    every { mockService.getUser(any()) } returns User("123", "Mock User")

    val result = mockService.getUser("123")

    verify { mockService.getUser("123") }
    assertEquals("Mock User", result.name)
}
```

### **After (Fakt):**
```kotlin
@Test
fun `GIVEN fake service WHEN getting user THEN returns configured user`() = runTest {
    val fake = fakeUserService {
        getUser { id -> User(id, "Test User") }
    }

    val result = fake.getUser("123")

    assertEquals(1, fake.getUserCallCount.value)
    assertEquals("Test User", result.name)
}
```

---

## Key Benefits

1. **No Magic**: Everything is generated as readable Kotlin code
2. **Predictable**: Behavior is explicit and typed
3. **Fast**: Zero runtime reflection or complex frameworks
4. **Safe**: Compiler catches interface changes immediately
5. **Simple**: Clean DSL that matches your domain

---

## Learn More

- [Basic Usage](../usage/basic-usage.md) - Getting started with fakes
- [Testing Patterns](../guides/testing-patterns.md) - Best practices
- [Migration Guide](../guides/migration.md) - Moving from MockK/Mockito