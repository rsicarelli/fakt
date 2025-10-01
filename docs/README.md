# KtFakes - Unified IR-Native Fake Generation

> **Status**: Production-Ready MAP (Minimum Awesome Product) ✅  
> **Architecture**: Unified IR-Native Compiler Plugin  
> **Philosophy**: Build awesome tools that compete on developer experience, not just functionality

**KtFakes** is a Kotlin compiler plugin that generates type-safe fake implementations for interfaces marked with `@Fake`. Built with a unified IR-native architecture, it provides professional-quality code generation with excellent developer experience.

## 🚀 **Key Features**

### ✅ **Unified IR-Native Architecture**
- **Single compiler implementation** using pure IR APIs
- **Modular design** with clean separation of concerns
- **Type-safe generation** with zero runtime overhead
- **Professional code quality** with comprehensive testing

### ✅ **Advanced Language Support**
- **Suspend functions**: Full coroutine support with proper typing
- **Properties and methods**: Mixed interface support
- **Multi-interface**: Generate multiple fakes in one compilation
- **Type safety**: No `Any` casting, proper generic handling

### ✅ **Developer Experience**
- **Type-safe DSL**: `fakeService { getValue { "test" } }`
- **Factory functions**: Thread-safe instance creation
- **Configuration DSL**: Intuitive behavior setup
- **IDE integration**: Full IntelliJ support with debugging

## 📋 **Quick Start**

### 1. **Add Dependencies**

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("dev.rsicarelli:ktfake-runtime:1.0.0")
}

// Apply compiler plugin
plugins {
    id("com.rsicarelli.fakt") version "1.0.0"
}
```

### 2. **Annotate Interfaces**

```kotlin
import com.rsicarelli.fakt.Fake

@Fake
interface UserService {
    val currentUser: String
    fun getUser(id: String): String
    suspend fun updateUser(id: String, name: String): Boolean
}
```

### 3. **Use Generated Fakes**

```kotlin
// Generated automatically by compiler plugin
val fake = fakeUserService {
    currentUser { "John Doe" }
    getUser { id -> "User-$id" }
    updateUser { _, _ -> true }
}

// Use in tests
assertEquals("John Doe", fake.currentUser)
assertEquals("User-123", fake.getUser("123"))
assertTrue(fake.updateUser("123", "Jane"))
```

## 🏗️ **Architecture Overview**

### **Unified IR-Native Compiler Plugin**
```
KtFakes Compiler Plugin
├── FIR Phase: @Fake annotation detection
├── IR Phase: Unified code generation
│   ├── Interface Analysis: Dynamic IR-based discovery
│   ├── Code Generation: Type-safe IR node creation
│   ├── DSL Generation: Configuration class creation
│   └── Factory Generation: Thread-safe constructors
└── Generated Output: Professional Kotlin code
```

### **Generated Code Structure**
For each `@Fake` interface, KtFakes generates:

1. **Implementation Class**: `FakeUserServiceImpl`
2. **Factory Function**: `fakeUserService(configure: ...)`
3. **Configuration DSL**: `FakeUserServiceConfig`
4. **Type-Safe Behaviors**: Proper suspend/regular function handling

## 📊 **Supported Features**

| Feature | Status | Example |
|---------|--------|---------|
| Basic interfaces | ✅ | `fun getValue(): String` |
| Properties | ✅ | `val name: String` |
| Suspend functions | ✅ | `suspend fun fetch(): Data` |
| Multi-interface | ✅ | Multiple `@Fake` in one file |
| Configuration DSL | ✅ | `fake { getValue { "test" } }` |
| Factory functions | ✅ | `val fake = fakeService()` |
| Thread safety | ✅ | Instance-based generation |
| Type safety | ✅ | No `Any` casting |

## 🧪 **Testing**

KtFakes is designed for testing scenarios:

```kotlin
@Test
fun `should handle user operations`() = runTest {
    val userService = fakeUserService {
        currentUser { "TestUser" }
        updateUser { id, name -> 
            id == "valid" && name.isNotEmpty() 
        }
    }
    
    assertEquals("TestUser", userService.currentUser)
    assertTrue(userService.updateUser("valid", "NewName"))
    assertFalse(userService.updateUser("invalid", ""))
}
```

## 📚 **Documentation**

- **[Architecture Guide](ARCHITECTURE.md)**: Deep dive into unified IR-native design
- **[API Specifications](API_SPECIFICATIONS.md)**: Complete API with working examples
- **[Implementation Roadmap](IMPLEMENTATION_ROADMAP.md)**: Future enhancements and features
- **[Testing Guidelines](TESTING_GUIDELINES.md)**: Best practices for compiler plugin testing

## 🔧 **Build Commands**

```bash
# Build the compiler plugin
./gradlew :compiler:shadowJar

# Test with working example
cd test-sample && ../gradlew build

# Run all tests
./gradlew test

# Rebuild with fresh generated code
cd test-sample && rm -rf build/generated && ../gradlew compileKotlinJvm --no-build-cache
```

## 🎯 **Why Fakt**

### **Compared to MockK/Mockito**
- **Compile-time generation**: No reflection overhead
- **Type safety**: Full Kotlin type system integration
- **Suspend support**: First-class coroutine support
- **IDE integration**: Perfect debugging experience
- **Zero configuration**: Works out of the box

### **Unified Architecture Benefits**
- **Single implementation**: No confusion between approaches
- **Modular design**: Easy to extend and maintain
- **IR-native**: Future-proof with Kotlin compiler evolution
- **Professional quality**: Production-ready code generation

## 📈 **Status**

**Current**: Production-ready MAP with unified IR-native architecture
**Tested**: JDK 21, Kotlin 2.2.10+, comprehensive test suite
**Supported**: Interface analysis, code generation, suspend functions, multi-interface

## 🤝 **Contributing**

KtFakes follows MAP (Minimum Awesome Product) principles:
- Every feature must be production-quality and delightful
- Comprehensive testing with BDD approach
- Professional code generation standards
- Developer experience first

See our [unified architecture documentation](ARCHITECTURE.md) for implementation details.

---

**License**: Apache 2.0  
**Author**: Rodrigo Sicarelli  
**Architecture**: Unified IR-Native Compiler Plugin ✅