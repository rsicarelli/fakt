# KtFakes Implementation Roadmap - Post-Unified Architecture

> **Status**: Unified IR-Native Architecture Complete ✅  
> **Current Version**: 1.0.0-UNIFIED  
> **Next Phase**: Advanced Feature Development  
> **Philosophy**: MAP (Minimum Awesome Product) - Always Production Quality

## 🎉 **Phase 1: COMPLETED ✅**

### **✅ Unified Architecture Migration (September 2025)**
- **Single compiler implementation**: Eliminated dual string-based/IR-native confusion
- **Modular IR-native design**: Clean separation of concerns with 6+ modules
- **End-to-end validation**: Working `test-sample` with real code generation
- **Suspend function support**: Full coroutine integration with proper typing
- **Multi-interface support**: Generate multiple fakes in single compilation

### **✅ Core Feature Set**
- **Basic interfaces**: Methods and properties ✅
- **Suspend functions**: `suspend fun getData(): String` ✅  
- **Properties**: `val name: String` ✅
- **Factory functions**: `fakeService {}` ✅
- **Configuration DSL**: Type-safe behavior setup ✅
- **Test-only generation**: Security by design ✅

### **✅ Production Quality Standards**
- **Zero compilation errors**: All generated code compiles cleanly
- **Type safety**: No `Any` casting, proper generics
- **Thread safety**: Instance-based generation  
- **Performance**: IR-native, no string parsing overhead
- **Documentation**: Complete API specs with working examples

## 🚨 **Phase 2: CRITICAL FIXES (IMMEDIATE PRIORITY)**

> **ALERT**: Sample analysis revealed critical compiler limitations blocking real-world usage.
> **Status**: Multiple compilation failures in test scenarios
> **Required**: Immediate fixes before any feature enhancements

### **🚨 Priority 1: Cross-Module Import Resolution** 
**Status**: CRITICAL BUG - Multi-module scenarios fail to compile

**Current Problem**:
```kotlin
// Generated code in core/build/generated/ktfake/commonTest/kotlin/
class FakeUserServiceImpl : UserService {
    // ERROR: NetworkService not imported!
    private var networkServiceBehavior: () -> NetworkService = { ... }
    private var storageServiceBehavior: () -> StorageService = { ... }
    //                                      ^^^^^^^^^^^^^^
    //                          Unresolved reference errors
}
```

**Required Fix**:
```kotlin
// Must generate proper imports
package core.business

import api.shared.NetworkService    // <-- MISSING
import api.shared.StorageService    // <-- MISSING  
import api.shared.LoggingService    // <-- MISSING

class FakeUserServiceImpl : UserService { ... }
```

**Implementation Plan**:
- **Import analysis**: Detect cross-module type references in IR
- **Module resolution**: Map types to their source modules
- **Import generation**: Generate proper import statements
- **Validation**: Ensure all referenced types are imported

### **🚨 Priority 2: Generic Type Parameter Handling**
**Status**: CRITICAL BUG - All generic types lose type safety

**Current Problem**:
```kotlin
// Input interface
interface UserRepository {
    fun findByAge(min: Int, max: Int): List<User>
    fun processNumbers(items: Set<Int>): Map<String, Int>
}

// BROKEN Generated code
class FakeUserRepositoryImpl : UserRepository {
    // Type parameters stripped! Compilation errors!
    private var findByAgeBehavior: (Int, Int) -> List = { _, _ -> TODO("Implement default for List") }
    private var processNumbersBehavior: (Set) -> Map = { _ -> TODO("Implement default for Map") }
    //                                    ^^^     ^^^
    //                              Missing type parameters
}
```

**Required Fix**:
```kotlin
// CORRECT Generated code
class FakeUserRepositoryImpl : UserRepository {
    private var findByAgeBehavior: (Int, Int) -> List<User> = { _, _ -> emptyList() }
    private var processNumbersBehavior: (Set<Int>) -> Map<String, Int> = { _ -> emptyMap() }
}
```

**Implementation Plan**:
- **Type parameter extraction**: Preserve generic type information in IR analysis
- **Default value mapping**: Smart defaults for common generic types
- **Type rendering**: Proper generic type string generation

### **🚨 Priority 3: Function Type Resolution**
**Status**: CRITICAL BUG - Higher-order functions generate uncompilable code

**Current Problem**:
```kotlin
// Input interface  
interface EventProcessor {
    fun process(item: Any, processor: (Any) -> String): String
    suspend fun processAsync(item: String, processor: suspend (String) -> String): String
}

// BROKEN Generated code
class FakeEventProcessorImpl : EventProcessor {
    // Function types become unresolvable symbols!
    private var processBehavior: (Any, Function1) -> String = { _, _ -> "" }
    private var processAsyncBehavior: suspend (String, SuspendFunction1) -> String = { _, _ -> "" }
    //                                                     ^^^^^^^^^^^^^^^^
    //                                             Unresolved reference
}
```

**Required Fix**:
```kotlin
// CORRECT Generated code
class FakeEventProcessorImpl : EventProcessor {
    private var processBehavior: (Any, (Any) -> String) -> String = { _, _ -> "" }
    private var processAsyncBehavior: suspend (String, suspend (String) -> String) -> String = { _, _ -> "" }
}
```

**Implementation Plan**:
- **Function type analysis**: Properly handle function type IR nodes
- **Lambda type generation**: Convert function types to proper Kotlin syntax
- **Suspend function support**: Handle suspend function types correctly

### **🚨 Priority 4: Intelligent Default Value Generation**
**Status**: HIGH PRIORITY - Generated code contains compilation-blocking TODOs

**Current Problem**:
```kotlin
// Generated code with blocking TODOs
private var findByAgeBehavior: (Int, Int) -> List<User> = { _, _ -> TODO("Implement default for List") }
private var getResultBehavior: () -> Result<String> = { TODO("Implement default for Result") }
private var getUserBehavior: () -> User = { TODO("Implement default for User") }
//                                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                                         Causes compilation failure!
```

**Required Fix**:
```kotlin
// Smart defaults that compile
private var findByAgeBehavior: (Int, Int) -> List<User> = { _, _ -> emptyList() }
private var getResultBehavior: () -> Result<String> = { Result.success("") }
private var getUserBehavior: () -> User = { User("", "", "") } // or null if nullable
```

**Implementation Plan**:
- **Type-aware defaults**: Map common types to sensible defaults
- **Constructor analysis**: For data classes, use primary constructor with defaults
- **Nullable handling**: Use null for nullable types, constructor calls for non-nullable

## 🚀 **Phase 3: Enhanced Features (After Critical Fixes)**

### **🎯 Priority 1: Parameter-Aware Behavior Configuration**
*Moved from Phase 2 - requires functional type system first*

### **🎯 Priority 2: Call Tracking Implementation** 
*Enhanced priority due to testing framework importance*

### **🎯 Priority 3: Call Tracking Implementation** 

**Current State**: Annotation exists but not implemented

**Target Implementation**:
```kotlin
@Fake(trackCalls = true)
interface AnalyticsService {
    fun track(event: String, properties: Map<String, Any>)
}

// Generated with tracking
val analytics = fakeAnalyticsService()
analytics.track("login", mapOf("method" to "email"))

// Verification methods
analytics.verify().track(calledWith("login", any()), times(1))
analytics.verify().track(never())
analytics.getCalls().size shouldBe 1
```

**Implementation Plan**:
- **Call storage**: Generate call data classes
- **Verification DSL**: Fluent verification API
- **Memory management**: Configurable call history limits
- **Performance**: Minimal overhead when tracking disabled

## 🚀 **Phase 3: Advanced Features (3-9 Months)**

### **🎯 Priority 1: Cross-Module Dependencies**

**Target Feature**:
```kotlin
@Fake(dependencies = [UserService::class, AnalyticsService::class])
interface OrderService {
    suspend fun createOrder(userId: String, items: List<Item>): Order
}

// Auto-injected dependencies
val orderService = fakeOrderService {
    createOrder { userId, items -> Order(userId, items) }
    
    // Configure dependencies
    userService {
        getUser { User(it, "Test User") }
    }
    
    analytics {
        track { event, props -> println("Tracked: $event") }
    }
}
```

**Implementation Challenges**:
- **Dependency resolution**: Locate and validate dependency fakes
- **Circular dependency detection**: Prevent infinite loops
- **Module coordination**: Cross-module fake generation
- **Configuration complexity**: Nested configuration DSL

### **🎯 Priority 2: Builder Pattern Support**

**Target Feature**:
```kotlin
@Fake(builder = true)
data class User(
    val id: String,
    val name: String,
    val email: String,
    val preferences: UserPreferences
)

// Generated builder
val user = fakeUser {
    id("user-123")
    name("John Doe")
    email("john@example.com")
    preferences {  // Nested fake generation
        theme("dark")
        notifications(true)
    }
}
```

**Implementation Requirements**:
- **Data class analysis**: Extract constructor parameters
- **Builder generation**: Fluent configuration API
- **Nested object support**: Recursive fake generation
- **Default value handling**: Smart defaults for builders

### **🎯 Priority 3: Exception and Edge Case Handling**

**Target Feature**:
```kotlin
val service = fakeUserService {
    getUser { id ->
        when {
            id.isBlank() -> throw IllegalArgumentException("ID cannot be blank")
            id == "404" -> throw UserNotFoundException(id)
            id.startsWith("banned") -> throw SecurityException("User banned")
            else -> User(id, "User-$id")
        }
    }
    
    // Shorthand for common exceptions
    deleteUser(throws = SecurityException("Not authorized"))
    updateUser(throwsIf = { id, _ -> id == "readonly" })
}
```

## 🔮 **Phase 4: Performance & Polish (6-12 Months)**

### **🎯 Compilation Performance Optimization**

**Target Improvements**:
- **Incremental generation**: Only regenerate changed fakes
- **Parallel processing**: Multi-threaded fake generation
- **Caching**: Smart caching of analysis results
- **Build integration**: Optimal Gradle build cache support

### **🎯 IDE Integration Enhancement**

**Target Features**:
- **Code navigation**: Jump to generated implementations
- **Real-time validation**: Live error highlighting
- **Auto-completion**: Smart completion for configuration DSL
- **Refactoring support**: Rename interface → update fakes

### **🎯 Debugging Support**

**Target Features**:
- **Debug info**: Source maps for generated code
- **Breakpoint support**: Debug inside fake behaviors
- **Call stack clarity**: Clear fake method traces
- **Error messages**: Precise error location mapping

## 📊 **Feature Priority Matrix - UPDATED After Sample Analysis**

### **🚨 CRITICAL FIXES (Must Complete Before Any Features)**
| Critical Issue | Impact | Effort | Priority | Timeline | Definition of Done |
|---------------|--------|--------|----------|----------|-------------------|
| Generic type parameters | CRITICAL | High | P0 | 2-3 weeks | ✅ `suspend fun <T> processData(data: T): T` generates correct types<br/>✅ All sample interfaces compile without errors<br/>✅ Type safety preserved in generated code<br/>✅ No `Any` fallbacks for known generic types |
| Smart default values | CRITICAL | Medium | P0 | 1 week | ✅ Zero TODO statements in generated code<br/>✅ All basic types have sensible defaults<br/>✅ Result<T> generates `Result.success(defaultValue)`<br/>✅ Complex types use null or constructor calls |
| Function type resolution | HIGH | Medium | P0 | 1-2 weeks | ✅ `(T) -> R` function types generate correctly<br/>✅ `suspend (T) -> R` generates correctly<br/>✅ No `Function1`, `SuspendFunction1` artifacts<br/>✅ Higher-order functions compile successfully |
| Cross-module imports | MEDIUM | Medium | P1 | 1-2 weeks | ✅ Generated files include proper imports<br/>✅ Multi-module scenarios compile<br/>✅ Cross-module type references work<br/>✅ Package resolution handles all dependencies |

### **📈 FEATURE DEVELOPMENT (After Critical Fixes)**
| Feature | Impact | Effort | Priority | Timeline | Definition of Done |
|---------|--------|--------|----------|----------|-------------------|
| Parameter-aware behavior | High | Medium | P1 | 1-2 months | ✅ `getUser { id -> "User-$id" }` syntax works<br/>✅ Multiple parameter support<br/>✅ Type-safe parameter passing<br/>✅ Full API documentation with examples |
| Call tracking | Medium | High | P1 | 2-3 months | ✅ `@Fake(trackCalls = true)` implemented<br/>✅ Verification DSL: `verify().method(times(1))`<br/>✅ Call history access and clearing<br/>✅ Performance: <5% overhead when enabled |
| Advanced collection support | High | Low | P2 | 2-3 weeks | ✅ `List<User>`, `Map<String, Int>` generate correctly<br/>✅ Nested collections work: `Map<String, List<Set<Int>>>`<br/>✅ Smart defaults for all collection types<br/>✅ Generic constraints preserved |
| Cross-module dependencies | High | High | P2 | 3-4 months | ✅ `@Fake(dependencies = [UserService::class])` works<br/>✅ Nested configuration DSL<br/>✅ Circular dependency detection<br/>✅ Integration tests across modules |
| Builder patterns | Medium | Medium | P3 | 4-5 months | ✅ `@Fake(builder = true)` for data classes<br/>✅ Fluent builder API generation<br/>✅ Nested object builder support<br/>✅ Type-safe builder validation |
| Exception handling | Medium | Low | P3 | 1 month | ✅ `throws` parameter support<br/>✅ Conditional exception throwing<br/>✅ Common exception shortcuts<br/>✅ Exception verification in tests |
| MVP completeness | High | Low | P1 | 2 weeks | ✅ Replace all "For MVP" comments<br/>✅ Remove all "TODO" placeholders<br/>✅ Upgrade FIR detection from placeholder<br/>✅ Complete ThreadSafetyChecker implementation |

## 🧪 **Development Methodology**

### **MAP Standards for All Features**
- **Production quality first**: No "good enough" features
- **Comprehensive testing**: BDD tests for every feature
- **Real-world validation**: Working examples before release
- **Developer experience**: Intuitive, delightful APIs

### **Feature Development Process**
1. **Spike & Research**: Understand requirements deeply
2. **Architecture Design**: Plan modular, extensible implementation
3. **TDD Implementation**: Test-first development with BDD naming
4. **Integration Testing**: End-to-end validation with `test-sample`
5. **Documentation**: Complete API specs with working examples
6. **Performance Testing**: Ensure scalability and efficiency

### **Quality Gates**
- **Zero Regressions**: All existing functionality must continue working
- **Type Safety**: No `Any` casting or unsafe operations
- **Performance**: No significant compilation time impact
- **API Consistency**: All new APIs follow established patterns

## 🎯 **Success Metrics**

### **Developer Experience**
- **Setup time**: < 5 minutes from zero to working fakes
- **Learning curve**: < 30 minutes to understand core concepts
- **Error messages**: Clear, actionable error descriptions
- **IDE support**: Full IntelliJ/Android Studio integration

### **Performance Benchmarks**
- **Compilation time**: < 10% overhead vs. regular compilation
- **Runtime performance**: Zero overhead vs. manual implementations
- **Memory usage**: Minimal fake instance memory footprint
- **Build cache**: High cache hit rates for incremental builds

### **Feature Coverage**
- **Interface types**: 95%+ of real-world interfaces supported
- **Kotlin features**: Full language feature compatibility
- **Testing frameworks**: JUnit 5, Kotest, TestNG integration
- **Multiplatform**: JVM, Android, JS, Native support

## 🚀 **Community & Ecosystem**

### **Plugin Ecosystem**
- **Testing frameworks**: Native integrations with popular test libraries
- **Build tools**: Gradle, Maven, Bazel support
- **IDEs**: IntelliJ, Android Studio, VS Code plugins
- **CI/CD**: GitHub Actions, Jenkins, TeamCity integrations

### **Community Contributions**
- **Feature requests**: GitHub issues with clear specifications
- **Implementation contributions**: Well-documented pull requests
- **Documentation**: Community examples and best practices
- **Testing**: Real-world usage feedback and bug reports

## 📈 **Version Planning**

### **v1.0.0 - Unified Architecture** ✅
- Single IR-native compiler implementation
- Basic interface support with suspend functions
- Factory functions and configuration DSL
- Test-only generation with security

### **v1.0.1 - Critical Fixes** (IMMEDIATE NEXT RELEASE)
- Cross-module import resolution (fixes multi-module compilation)
- Generic type parameter preservation (restores type safety)
- Function type resolution (fixes higher-order functions)
- Smart default value generation (removes compilation-blocking TODOs)

### **v1.1.0 - Enhanced Behaviors** (After Critical Fixes)
- Parameter-aware behavior configuration
- Advanced collection type support
- Improved error messages and diagnostics

### **v1.2.0 - Call Tracking**
- Full call tracking implementation
- Verification DSL with fluent assertions
- Performance optimizations

### **v2.0.0 - Advanced Features**
- Cross-module dependencies
- Builder pattern support  
- Exception handling utilities
- Breaking API improvements

### **v2.1.0+ - Ecosystem Integration**
- IDE plugins and tooling
- Performance optimizations
- Community-requested features

---

**Roadmap Status**: ✅ Unified Architecture Complete - Ready for Advanced Features  
**Next Milestone**: v1.1.0 with Parameter-Aware Behaviors  
**Long-term Vision**: Industry-standard Kotlin fake generation tool