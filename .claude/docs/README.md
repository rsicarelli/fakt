# Fakt - Type-Safe Fake Generation

> **Status**: Production-Ready
> **Architecture**: FIR→IR Two-Phase Compiler Plugin
> **Testing Standard**: [Testing Guidelines](development/validation/testing-guidelines.md)

Fakt is a Kotlin compiler plugin that generates type-safe fake implementations for interfaces marked with `@Fake`.

## Features

- **Type-safe DSL**: `fakeService { getValue { "test" } }`
- **Suspend functions**: Full coroutine support
- **Smart defaults**: Sensible defaults for all types
- **Zero runtime overhead**: All work done at compile time
- **KMP support**: Works with all Kotlin platforms

## Quick Start

```kotlin
// 1. Annotate interface
@Fake
interface UserService {
    val currentUser: String
    suspend fun getUser(id: String): User
    fun updateUser(user: User): Boolean
}

// 2. Use in tests
@Test
fun `GIVEN service WHEN getting user THEN returns expected`() = runTest {
    val service = fakeUserService {
        getUser { id -> User(id, "John") }
        currentUser { "current" }
    }

    assertEquals("John", service.getUser("123").name)
}
```

## Generated Code

For each `@Fake` interface, Fakt generates:

1. **Implementation class**: `FakeUserServiceImpl`
2. **Factory function**: `fakeUserService { }`
3. **Configuration DSL**: `FakeUserServiceConfig`

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture](implementation/architecture/ARCHITECTURE.md) | FIR→IR two-phase design |
| [KMP Optimization](implementation/architecture/kmp-optimization-strategy.md) | Multi-platform strategy |
| [Testing Guidelines](development/validation/testing-guidelines.md) | GIVEN-WHEN-THEN standard |
| [Kotlin API Reference](development/kotlin-api-reference.md) | Compiler source lookup |
| [Kotlin IR API](development/kotlin-compiler-ir-api.md) | IR API reference |
| [Troubleshooting](troubleshooting/common-issues.md) | Common issues |

## Development

```bash
# Build and publish locally
make publish-local

# Test sample
make test-sample

# Debug output
make debug
```

## Testing Standard

All tests must follow GIVEN-WHEN-THEN pattern:

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceTest {
    @Test
    fun `GIVEN condition WHEN action THEN expected result`() = runTest {
        // Given
        val service = fakeService()

        // When
        val result = service.doSomething()

        // Then
        assertEquals(expected, result)
    }
}
```

See [Testing Guidelines](development/validation/testing-guidelines.md) for complete standards.
