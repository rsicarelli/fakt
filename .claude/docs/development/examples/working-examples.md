# Working Examples - KtFakes in Action

> **Purpose**: Complete practical examples demonstrating KtFakes usage in real scenarios
> **Status**: All Examples Validated Against Current Implementation
> **Testing Standard**: [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)

## 🎯 **Overview**

This document provides real, working examples of KtFakes usage from basic scenarios to advanced patterns. All examples are tested and validated against the current unified IR-native implementation.

## Quick Reference

**Single-Platform Samples:**
- `samples/jvm-single-module`: JVM-only, standard kotlin-jvm plugin
- `samples/android-single-module`: Android Library, AGP 8.12.3+

**Multi-Platform Samples:**
- `samples/kmp-single-module`: Basic KMP with all targets
- `samples/kmp-multi-module`: Advanced multi-module KMP architecture

All samples include the same core scenarios for consistency:
- UserRepository (CRUD + call tracking)
- AuthenticationService (suspend + Result types)
- PropertyAndMethodInterface (properties + methods)

## 🚀 **Quick Start Examples**

### **Example 1: Basic Service Testing**

**Interface Definition**:
```kotlin
@Fake
interface UserService {
    val currentUser: String
    fun getUser(id: String): String
    fun updateUser(id: String, name: String): Boolean
}
```

**Test Implementation**:
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    @Test
    fun `GIVEN user service WHEN getting user THEN should return user data`() = runTest {
        // Given
        val userService = fakeUserService {
            getUser { id -> "User-$id" }
            currentUser { "current-user" }
        }

        // When
        val user = userService.getUser("123")
        val current = userService.currentUser

        // Then
        assertEquals("User-123", user)
        assertEquals("current-user", current)
    }

    @Test
    fun `GIVEN user service WHEN updating user THEN should execute update logic`() = runTest {
        // Given
        var lastUpdate: Pair<String, String>? = null
        val userService = fakeUserService {
            updateUser { id, name ->
                lastUpdate = id to name
                true
            }
        }

        // When
        val result = userService.updateUser("123", "John Doe")

        // Then
        assertTrue(result)
        assertEquals("123" to "John Doe", lastUpdate)
    }
}
```

### **Example 2: Suspend Functions with Coroutines**

**Interface Definition**:
```kotlin
@Fake
interface ApiClient {
    suspend fun fetchData(url: String): Result<String>
    suspend fun uploadData(data: String): Result<Unit>
    fun getHeaders(): Map<String, String>
}
```

**Test Implementation**:
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiClientTest {

    @Test
    fun `GIVEN api client WHEN fetching data THEN should handle success response`() = runTest {
        // Given
        val apiClient = fakeApiClient {
            fetchData { url ->
                when {
                    url.contains("users") -> Result.success("""{"users": []}""")
                    url.contains("error") -> Result.failure(Exception("API Error"))
                    else -> Result.success("{}")
                }
            }
            getHeaders { mapOf("Authorization" to "Bearer token") }
        }

        // When
        val usersResponse = apiClient.fetchData("https://api.example.com/users")
        val errorResponse = apiClient.fetchData("https://api.example.com/error")
        val headers = apiClient.getHeaders()

        // Then
        assertTrue(usersResponse.isSuccess)
        assertEquals("""{"users": []}""", usersResponse.getOrNull())
        assertTrue(errorResponse.isFailure)
        assertEquals("Bearer token", headers["Authorization"])
    }

    @Test
    fun `GIVEN api client WHEN uploading data THEN should handle upload flow`() = runTest {
        // Given
        var uploadedData: String? = null
        val apiClient = fakeApiClient {
            uploadData { data ->
                uploadedData = data
                if (data.length > 100) Result.failure(Exception("Data too large"))
                else Result.success(Unit)
            }
        }

        // When
        val smallDataResult = apiClient.uploadData("small data")
        val largeDataResult = apiClient.uploadData("x".repeat(101))

        // Then
        assertTrue(smallDataResult.isSuccess)
        assertTrue(largeDataResult.isFailure)
        assertEquals("x".repeat(101), uploadedData) // Last call
    }
}
```

## 🏗️ **Advanced Patterns**

### **Example 3: State Management and Side Effects**

**Interface Definition**:
```kotlin
@Fake
interface CacheService {
    fun put(key: String, value: Any): Boolean
    fun get(key: String): Any?
    fun clear(): Unit
    val size: Int
}
```

**Stateful Test Implementation**:
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheServiceTest {

    @Test
    fun `GIVEN cache service WHEN managing state THEN should maintain consistency`() = runTest {
        // Given - Stateful fake with internal storage
        val storage = mutableMapOf<String, Any>()
        val cacheService = fakeCacheService {
            put { key, value ->
                storage[key] = value
                true
            }
            get { key -> storage[key] }
            clear { storage.clear() }
            size { storage.size }
        }

        // When & Then - Progressive state changes
        assertEquals(0, cacheService.size)

        assertTrue(cacheService.put("user:123", "John Doe"))
        assertEquals(1, cacheService.size)
        assertEquals("John Doe", cacheService.get("user:123"))

        assertTrue(cacheService.put("user:456", "Jane Smith"))
        assertEquals(2, cacheService.size)

        cacheService.clear()
        assertEquals(0, cacheService.size)
        assertNull(cacheService.get("user:123"))
    }
}
```

### **Example 4: Complex Business Logic Testing**

**Interface Definition**:
```kotlin
@Fake
interface PaymentProcessor {
    suspend fun processPayment(amount: Double, currency: String, method: String): Result<String>
    suspend fun refundPayment(transactionId: String): Result<Boolean>
    fun validateCard(cardNumber: String): Boolean
    val supportedCurrencies: List<String>
}
```

**Business Logic Test**:
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentProcessorTest {

    @Test
    fun `GIVEN payment processor WHEN processing payments THEN should handle business rules`() = runTest {
        // Given - Complex business logic in fake
        val processedTransactions = mutableListOf<String>()
        val paymentProcessor = fakePaymentProcessor {
            processPayment { amount, currency, method ->
                when {
                    amount <= 0 -> Result.failure(Exception("Invalid amount"))
                    currency !in listOf("USD", "EUR", "BRL") -> Result.failure(Exception("Unsupported currency"))
                    method == "INVALID_CARD" -> Result.failure(Exception("Payment failed"))
                    else -> {
                        val txId = "tx_${System.currentTimeMillis()}"
                        processedTransactions.add(txId)
                        Result.success(txId)
                    }
                }
            }
            refundPayment { txId ->
                val wasProcessed = processedTransactions.contains(txId)
                if (wasProcessed) {
                    processedTransactions.remove(txId)
                    Result.success(true)
                } else {
                    Result.failure(Exception("Transaction not found"))
                }
            }
            validateCard { cardNumber ->
                cardNumber.length == 16 && cardNumber.all { it.isDigit() }
            }
            supportedCurrencies { listOf("USD", "EUR", "BRL") }
        }

        // When & Then - Test business scenarios

        // Valid payment
        val validPayment = paymentProcessor.processPayment(100.0, "USD", "CREDIT_CARD")
        assertTrue(validPayment.isSuccess)
        val txId = validPayment.getOrNull()!!
        assertTrue(processedTransactions.contains(txId))

        // Invalid amount
        val invalidAmount = paymentProcessor.processPayment(-10.0, "USD", "CREDIT_CARD")
        assertTrue(invalidAmount.isFailure)

        // Unsupported currency
        val invalidCurrency = paymentProcessor.processPayment(100.0, "JPY", "CREDIT_CARD")
        assertTrue(invalidCurrency.isFailure)

        // Card validation
        assertTrue(paymentProcessor.validateCard("1234567890123456"))
        assertFalse(paymentProcessor.validateCard("invalid-card"))

        // Refund flow
        val refundResult = paymentProcessor.refundPayment(txId)
        assertTrue(refundResult.isSuccess)
        assertTrue(refundResult.getOrNull() == true)
        assertFalse(processedTransactions.contains(txId)) // Removed after refund
    }
}
```

## 🔄 **Integration Testing Patterns**

### **Example 5: Multi-Service Integration**

**Service Definitions**:
```kotlin
@Fake
interface UserRepository {
    suspend fun findById(id: String): User?
    suspend fun save(user: User): User
}

@Fake
interface EmailService {
    suspend fun sendEmail(to: String, subject: String, body: String): Boolean
}

@Fake
interface AuditLogger {
    fun logUserAction(userId: String, action: String)
}
```

**Integration Test**:
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRegistrationIntegrationTest {

    @Test
    fun `GIVEN user registration flow WHEN registering new user THEN should coordinate all services`() = runTest {
        // Given - Setup coordinated fakes
        val savedUsers = mutableMapOf<String, User>()
        val sentEmails = mutableListOf<Triple<String, String, String>>()
        val auditLogs = mutableListOf<Pair<String, String>>()

        val userRepository = fakeUserRepository {
            findById { id -> savedUsers[id] }
            save { user ->
                savedUsers[user.id] = user
                user
            }
        }

        val emailService = fakeEmailService {
            sendEmail { to, subject, body ->
                sentEmails.add(Triple(to, subject, body))
                true // Simulate successful email
            }
        }

        val auditLogger = fakeAuditLogger {
            logUserAction { userId, action ->
                auditLogs.add(userId to action)
            }
        }

        // When - Simulate user registration flow
        val newUser = User(id = "123", email = "john@example.com", name = "John Doe")

        // Check user doesn't exist
        val existingUser = userRepository.findById("123")
        assertNull(existingUser)

        // Save new user
        val savedUser = userRepository.save(newUser)
        assertEquals(newUser, savedUser)

        // Send welcome email
        val emailSent = emailService.sendEmail(
            to = newUser.email,
            subject = "Welcome!",
            body = "Welcome to our platform, ${newUser.name}!"
        )
        assertTrue(emailSent)

        // Log registration
        auditLogger.logUserAction(newUser.id, "USER_REGISTERED")

        // Then - Verify coordination
        assertEquals(newUser, savedUsers["123"])
        assertEquals(1, sentEmails.size)
        assertEquals("john@example.com", sentEmails[0].first)
        assertEquals("Welcome!", sentEmails[0].second)
        assertEquals(1, auditLogs.size)
        assertEquals("123" to "USER_REGISTERED", auditLogs[0])
    }
}
```

## 🧪 **Testing Patterns and Best Practices**

### **Example 6: Error Scenario Testing**

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ErrorScenarioTest {

    @Test
    fun `GIVEN flaky service WHEN handling errors THEN should test resilience`() = runTest {
        // Given - Simulate flaky behavior
        var attemptCount = 0
        val flakyService = fakeApiClient {
            fetchData { url ->
                attemptCount++
                when (attemptCount) {
                    1 -> Result.failure(Exception("Network timeout"))
                    2 -> Result.failure(Exception("Server error"))
                    3 -> Result.success("Success on third try")
                    else -> Result.success("Stable response")
                }
            }
        }

        // When & Then - Test retry logic
        assertTrue(flakyService.fetchData("test").isFailure) // First attempt
        assertTrue(flakyService.fetchData("test").isFailure) // Second attempt
        assertTrue(flakyService.fetchData("test").isSuccess) // Third attempt
        assertTrue(flakyService.fetchData("test").isSuccess) // Stable
    }
}
```

### **Example 7: Performance and Timing**

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerformanceTest {

    @Test
    fun `GIVEN timed service WHEN measuring performance THEN should handle timing constraints`() = runTest {
        // Given - Service with timing behavior
        val timedService = fakeApiClient {
            fetchData { url ->
                // Simulate different response times
                when {
                    url.contains("fast") -> {
                        delay(10)
                        Result.success("Fast response")
                    }
                    url.contains("slow") -> {
                        delay(100)
                        Result.success("Slow response")
                    }
                    else -> Result.success("Normal response")
                }
            }
        }

        // When & Then - Test timing behavior
        val startTime = System.currentTimeMillis()
        val fastResult = timedService.fetchData("fast-endpoint")
        val fastTime = System.currentTimeMillis() - startTime

        val slowStart = System.currentTimeMillis()
        val slowResult = timedService.fetchData("slow-endpoint")
        val slowTime = System.currentTimeMillis() - slowStart

        assertTrue(fastResult.isSuccess)
        assertTrue(slowResult.isSuccess)
        assertTrue(fastTime < slowTime) // Timing verification
    }
}
```

## 📊 **Real-World Scenarios**

### **Example 8: E-commerce Order Processing**

```kotlin
@Fake
interface OrderService {
    suspend fun createOrder(customerId: String, items: List<OrderItem>): Result<Order>
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Boolean
    fun calculateTotal(items: List<OrderItem>): Double
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderProcessingTest {

    @Test
    fun `GIVEN e-commerce order service WHEN processing orders THEN should handle complete workflow`() = runTest {
        // Given - Comprehensive order service fake
        val orders = mutableMapOf<String, Order>()
        val orderService = fakeOrderService {
            createOrder { customerId, items ->
                val total = items.sumOf { it.price * it.quantity }
                if (items.isEmpty()) {
                    Result.failure(Exception("Cannot create order with no items"))
                } else {
                    val order = Order(
                        id = "order_${System.currentTimeMillis()}",
                        customerId = customerId,
                        items = items,
                        total = total,
                        status = OrderStatus.PENDING
                    )
                    orders[order.id] = order
                    Result.success(order)
                }
            }
            updateOrderStatus { orderId, status ->
                orders[orderId]?.let { order ->
                    orders[orderId] = order.copy(status = status)
                    true
                } ?: false
            }
            calculateTotal { items ->
                items.sumOf { it.price * it.quantity }
            }
        }

        // When - Complete order workflow
        val items = listOf(
            OrderItem("item1", 10.0, 2),
            OrderItem("item2", 25.0, 1)
        )

        val orderResult = orderService.createOrder("customer123", items)
        assertTrue(orderResult.isSuccess)

        val order = orderResult.getOrNull()!!
        assertEquals(45.0, order.total)
        assertEquals(OrderStatus.PENDING, order.status)

        // Update order status
        assertTrue(orderService.updateOrderStatus(order.id, OrderStatus.CONFIRMED))
        assertEquals(OrderStatus.CONFIRMED, orders[order.id]?.status)

        // Calculate total verification
        assertEquals(45.0, orderService.calculateTotal(items))
    }
}
```

## 🔗 **Related Documentation**

- **[📋 API Specifications](.claude/docs/api/specifications.md)** - Complete API reference
- **[📋 Generated API Reference](.claude/docs/api/generated-api.md)** - Generated code details
- **[📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)** - GIVEN-WHEN-THEN patterns
- **[📋 Common Issues](.claude/docs/troubleshooting/common-issues.md)** - Problem solving

---

**These working examples demonstrate real-world usage of KtFakes with patterns validated against the current unified IR-native implementation.**