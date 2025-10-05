# Meta-Annotation Sample: Custom Fake Generation

This sample demonstrates how companies can define their own annotations for fake generation using the `@GeneratesFake` meta-annotation pattern, inspired by Kotlin's `@HidesFromObjC`.

## Quick Start

### 1. Define Your Custom Annotation

```kotlin
@GeneratesFake  // Meta-annotation that tells Fakt compiler to detect this
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class TestDouble
```

### 2. Use Your Annotation

```kotlin
@TestDouble  // Automatically detected by compiler!
interface PaymentService {
    fun processPayment(amount: Double, currency: String = "USD"): String?
    fun validatePayment(cardNumber: String, cvv: String): Boolean
    suspend fun refundTransaction(transactionId: String): Boolean
}
```

### 3. No Gradle Configuration Required!

The compiler automatically detects any annotation marked with `@GeneratesFake`. You don't need to configure anything in your `build.gradle.kts`:

```kotlin
// ❌ NOT NEEDED
fakt {
    customAnnotations.set(listOf("com.company.TestDouble"))
}

// ✅ Just apply the plugin
plugins {
    id("com.rsicarelli.fakt")
}
```

### 4. Use Generated Fakes in Tests

```kotlin
@Test
fun testPayment() {
    // Generated automatically: fakePaymentService(), FakePaymentServiceConfig
    val service = fakePaymentService {
        processPayment { amount, currency ->
            "TXN-${amount.toInt()}-$currency"
        }
    }

    assertEquals("TXN-100-USD", service.processPayment(100.0, "USD"))
}
```

## Why Use Custom Annotations?

### 1. **Ownership & Control**
- Your annotation, your naming convention
- Not dependent on Fakt's `@Fake` annotation
- Migration safety: Breaking changes in Fakt won't affect your codebase

### 2. **Company Standards**
- Follow your company's naming conventions (`@TestDouble`, `@MockService`, etc.)
- Better alignment with internal tooling and standards
- Clear indication this is company-specific test infrastructure

### 3. **Zero Configuration**
- No Gradle setup required
- No buildscript modifications
- Meta-annotation does all the work

## How It Works

The pattern is inspired by Kotlin's `@HidesFromObjC` meta-annotation:

1. **Meta-Annotation Detection**: The compiler looks for annotations marked with `@GeneratesFake`
2. **Automatic Discovery**: Any interface/class with such an annotation is detected
3. **Fake Generation**: Fakes are generated just like with `@Fake`

```
@GeneratesFake              <- Meta-annotation (like @HidesFromObjC)
    ↓
@TestDouble                 <- Your custom annotation
    ↓
interface PaymentService    <- Automatically detected!
    ↓
Generated:
- FakePaymentServiceImpl    <- Fake implementation
- fakePaymentService()      <- Factory function
- FakePaymentServiceConfig  <- Configuration DSL
```

## Migration from @Fake

You can migrate incrementally:

```kotlin
// Old: Using Fakt's built-in annotation
@Fake
interface ServiceA { ... }

// New: Using your custom annotation
@TestDouble
interface ServiceB { ... }

// Both work! @Fake is also marked with @GeneratesFake
```

## Examples in This Sample

- **TestDouble.kt**: Custom annotation definition
- **PaymentService.kt**: Interface using custom annotation
- **PaymentServiceTest.kt**: Tests using generated fake

## Pattern Reference

This pattern follows Kotlin's meta-annotation approach:

**Kotlin Example:**
```kotlin
@HidesFromObjC  // Meta-annotation
annotation class MyCustomAnnotation

@MyCustomAnnotation  // Automatically hidden from ObjC
fun someFunction() { }
```

**Fakt Example:**
```kotlin
@GeneratesFake  // Meta-annotation
annotation class TestDouble

@TestDouble  // Automatically generates fake
interface ServiceX { }
```

Zero configuration. Pure compiler magic. 🎉
