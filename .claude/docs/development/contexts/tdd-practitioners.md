# Context: TDD Practitioners Using KtFakes

> **Target Audience**: Test-Driven Development practitioners and testing enthusiasts
> **Approach**: Test-first development with type-safe fakes
> **Testing Standard**: [📋 Testing Guidelines](../validation/testing-guidelines.md)

## 🎯 **TDD Profile**

### **Who This Is For**
- **TDD practitioners** who write tests before implementation
- **Quality engineers** focused on comprehensive test coverage
- **Teams** following rigorous testing practices
- **Developers** who value fast feedback loops and refactoring safety

### **TDD Values Alignment**
- **Red-Green-Refactor**: Fast feedback with compile-time safety
- **Test isolation**: Each test is independent and focused
- **Refactoring confidence**: Type-safe fakes survive interface changes
- **Specification by example**: Tests document expected behavior clearly

## 🔴 **Red Phase: Writing Failing Tests**

### **Interface-First Design**
```kotlin
// Step 1: Define interface based on what you need
@Fake
interface PaymentProcessor {
    suspend fun processPayment(amount: Money, card: CreditCard): Result<PaymentResult>
    suspend fun refund(transactionId: String, amount: Money): Result<RefundResult>
    fun validateCard(card: CreditCard): ValidationResult
}

// Step 2: Write failing test with fake
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentServiceTest {

    @Test
    fun `GIVEN payment service WHEN processing valid payment THEN should return success result`() = runTest {
        // Given - Setup fake with expected behavior
        val paymentProcessor = fakePaymentProcessor {
            processPayment { amount, card ->
                Result.success(PaymentResult("tx_123", amount, PaymentStatus.APPROVED))
            }
            validateCard { card ->
                ValidationResult.VALID
            }
        }

        val paymentService = PaymentService(paymentProcessor)  // ❌ Doesn't exist yet

        // When
        val result = paymentService.processPayment(Money(100.0, "USD"), validCard)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("tx_123", result.getOrNull()?.transactionId)
    }
}
```

### **Test Structure for TDD**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRegistrationServiceTest {

    @Test
    fun `GIVEN new user registration WHEN user already exists THEN should return error`() = runTest {
        // Given - Setup known state
        val existingUsers = setOf("john@example.com")
        val userRepository = fakeUserRepository {
            existsByEmail { email -> existingUsers.contains(email) }
        }

        val registrationService = UserRegistrationService(userRepository)  // To be implemented

        // When
        val result = registrationService.register("john@example.com", "John Doe")

        // Then
        assertTrue(result.isFailure)
        assertEquals("User already exists", result.exceptionOrNull()?.message)
    }

    @Test
    fun `GIVEN new user registration WHEN user is new THEN should save user successfully`() = runTest {
        // Given
        val savedUsers = mutableListOf<User>()
        val userRepository = fakeUserRepository {
            existsByEmail { email -> false }  // No existing users
            save { user -> savedUsers.add(user); user }
        }

        val registrationService = UserRegistrationService(userRepository)

        // When
        val result = registrationService.register("jane@example.com", "Jane Doe")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, savedUsers.size)
        assertEquals("jane@example.com", savedUsers[0].email)
    }
}
```

## 🟢 **Green Phase: Making Tests Pass**

### **Minimal Implementation**
```kotlin
// Implement just enough to make tests pass
class PaymentService(private val processor: PaymentProcessor) {
    suspend fun processPayment(amount: Money, card: CreditCard): Result<PaymentResult> {
        val validation = processor.validateCard(card)
        return if (validation == ValidationResult.VALID) {
            processor.processPayment(amount, card)
        } else {
            Result.failure(Exception("Invalid card"))
        }
    }
}

class UserRegistrationService(private val repository: UserRepository) {
    suspend fun register(email: String, name: String): Result<User> {
        return if (repository.existsByEmail(email)) {
            Result.failure(Exception("User already exists"))
        } else {
            val user = User(UUID.randomUUID().toString(), email, name)
            repository.save(user)
            Result.success(user)
        }
    }
}
```

### **Verify Green State**
```kotlin
// All tests should now pass
@Test
fun `all tests should pass with minimal implementation`() = runTest {
    // Verify that your fake behaviors match the actual implementation needs
    // This is where type safety really shines - mismatches caught at compile time
}
```

## 🔵 **Refactor Phase: Improving Design**

### **Refactoring with Confidence**
```kotlin
// Refactor interface - tests will fail to compile if incompatible
@Fake
interface PaymentProcessor {
    // Add new parameter - tests must be updated (compile-time safety)
    suspend fun processPayment(
        amount: Money,
        card: CreditCard,
        options: PaymentOptions = PaymentOptions.DEFAULT
    ): Result<PaymentResult>

    // Rename method - all usages must be updated
    suspend fun refundPayment(transactionId: String, amount: Money): Result<RefundResult>  // was: refund

    // Change return type - fakes must be updated
    suspend fun validateCard(card: CreditCard): Result<ValidationResult>  // was: ValidationResult
}
```

### **Test Updates for Refactoring**
```kotlin
@Test
fun `GIVEN payment processor WHEN refactoring interface THEN tests should update safely`() = runTest {
    // Given - Update fake to match new interface
    val paymentProcessor = fakePaymentProcessor {
        processPayment { amount, card, options ->  // New parameter
            Result.success(PaymentResult("tx_123", amount, PaymentStatus.APPROVED))
        }
        refundPayment { transactionId, amount ->  // Renamed method
            Result.success(RefundResult(transactionId, amount))
        }
        validateCard { card ->  // New return type
            Result.success(ValidationResult.VALID)
        }
    }

    // When & Then - Implementation automatically guided by type system
    val service = PaymentService(paymentProcessor)
    val result = service.processPayment(Money(100.0, "USD"), validCard)
    assertTrue(result.isSuccess)
}
```

## 🧪 **TDD Best Practices with KtFakes**

### **Triangulation Pattern**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CalculatorServiceTest {

    @Test
    fun `GIVEN calculator WHEN adding two numbers THEN should return sum`() = runTest {
        // Test 1: Specific case
        val calculator = fakeCalculator {
            add { a, b -> a + b }
        }
        val service = CalculatorService(calculator)

        assertEquals(5, service.add(2, 3))
    }

    @Test
    fun `GIVEN calculator WHEN adding different numbers THEN should return correct sum`() = runTest {
        // Test 2: Different values (triangulation)
        val calculator = fakeCalculator {
            add { a, b -> a + b }
        }
        val service = CalculatorService(calculator)

        assertEquals(10, service.add(4, 6))
        assertEquals(-1, service.add(3, -4))
        assertEquals(0, service.add(0, 0))
    }
}
```

### **Test Data Builders**
```kotlin
// Create test data builders for complex objects
class UserBuilder {
    private var id = "default-id"
    private var email = "default@example.com"
    private var name = "Default Name"

    fun withId(id: String) = apply { this.id = id }
    fun withEmail(email: String) = apply { this.email = email }
    fun withName(name: String) = apply { this.name = name }
    fun build() = User(id, email, name)
}

fun userBuilder() = UserBuilder()

@Test
fun `GIVEN user service WHEN saving user THEN should handle various user types`() = runTest {
    // Given
    val savedUsers = mutableListOf<User>()
    val userRepository = fakeUserRepository {
        save { user -> savedUsers.add(user); user }
    }

    val service = UserService(userRepository)

    // When - Using test data builders
    val adminUser = userBuilder().withEmail("admin@company.com").withName("Admin User").build()
    val regularUser = userBuilder().withEmail("user@company.com").withName("Regular User").build()

    service.save(adminUser)
    service.save(regularUser)

    // Then
    assertEquals(2, savedUsers.size)
    assertTrue(savedUsers.any { it.email == "admin@company.com" })
    assertTrue(savedUsers.any { it.email == "user@company.com" })
}
```

### **State-Based vs Interaction Testing**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderServiceTest {

    @Test
    fun `GIVEN order service WHEN placing order THEN should result in correct state`() = runTest {
        // State-based testing - verify final state
        val orders = mutableListOf<Order>()
        val orderRepository = fakeOrderRepository {
            save { order -> orders.add(order); order }
        }

        val service = OrderService(orderRepository)
        val result = service.placeOrder(customerId = "123", items = listOf(item1, item2))

        // Verify state
        assertTrue(result.isSuccess)
        assertEquals(1, orders.size)
        assertEquals("123", orders[0].customerId)
    }

    @Test
    fun `GIVEN order service WHEN placing order THEN should call dependencies correctly`() = runTest {
        // Interaction testing - verify method calls
        var orderSaved = false
        var emailSent = false

        val orderRepository = fakeOrderRepository {
            save { order -> orderSaved = true; order }
        }

        val emailService = fakeEmailService {
            sendOrderConfirmation { customerId, orderId -> emailSent = true }
        }

        val service = OrderService(orderRepository, emailService)
        service.placeOrder(customerId = "123", items = listOf(item1))

        // Verify interactions
        assertTrue(orderSaved)
        assertTrue(emailSent)
    }
}
```

## 🔄 **TDD Workflow Integration**

### **Fast Feedback Loop**
```kotlin
// 1. Write failing test (RED)
@Test
fun `GIVEN user service WHEN getting user by email THEN should return user`() = runTest {
    val userService = fakeUserService {
        findByEmail { email -> User("123", email, "John") }
    }

    val service = UserLookupService(userService)  // Doesn't exist yet
    val user = service.lookupByEmail("john@example.com")

    assertEquals("john@example.com", user.email)
}

// 2. Implement minimal code (GREEN)
class UserLookupService(private val userService: UserService) {
    suspend fun lookupByEmail(email: String): User {
        return userService.findByEmail(email)
    }
}

// 3. Refactor (BLUE)
class UserLookupService(private val userService: UserService) {
    suspend fun lookupByEmail(email: String): User? {
        return try {
            userService.findByEmail(email)
        } catch (e: Exception) {
            null  // Handle errors gracefully
        }
    }
}
```

### **Test Coverage Tracking**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComprehensiveServiceTest {

    @Test
    fun `GIVEN service WHEN handling success case THEN should work correctly`() = runTest {
        // Test happy path
    }

    @Test
    fun `GIVEN service WHEN handling error case THEN should handle gracefully`() = runTest {
        // Test error scenarios
    }

    @Test
    fun `GIVEN service WHEN handling edge case THEN should behave correctly`() = runTest {
        // Test edge cases and boundary conditions
    }
}
```

## 🎯 **TDD-Specific Benefits of KtFakes**

### **Compile-Time Contract Verification**
```kotlin
// Interface changes break tests at compile time, not runtime
@Fake
interface UserService {
    // suspend fun getUser(id: String): User  // Old signature
    suspend fun getUser(id: String, includeDeleted: Boolean = false): User  // New signature
}

// All tests using fakeUserService must be updated:
val service = fakeUserService {
    getUser { id, includeDeleted ->  // Must match new signature
        User(id, "test@example.com", "Test")
    }
}
```

### **Refactoring Safety**
```kotlin
// Rename interface method
@Fake
interface PaymentService {
    // fun process(payment: Payment): Result  // Old name
    fun processPayment(payment: Payment): Result  // New name
}

// All test fakes must be updated - IDE helps with refactoring
val service = fakePaymentService {
    processPayment { payment -> Result.success() }  // Must use new name
}
```

### **Type-Safe Test Data**
```kotlin
@Test
fun `GIVEN service WHEN processing data THEN should validate types`() = runTest {
    val service = fakeDataProcessor {
        process { data: UserData ->  // Type is enforced
            ProcessedData(data.id, data.name.uppercase())
        }
        // process { "invalid" -> ... }  // ❌ Won't compile
    }
}
```

## 🔗 **TDD Resources and References**

- **[📋 Testing Guidelines](../validation/testing-guidelines.md)** - THE ABSOLUTE STANDARD for TDD
- **[📋 Working Examples](../examples/working-examples.md)** - TDD patterns in action
- **[📋 API Specifications](../api/specifications.md)** - Complete fake API reference
- **[📋 Type Safety Validation](../validation/type-safety-validation.md)** - Compile-time safety

---

**KtFakes supports TDD practitioners with type-safe fakes that provide fast feedback, refactoring confidence, and clear specification by example.**