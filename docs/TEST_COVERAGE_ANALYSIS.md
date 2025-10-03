# Test Coverage Analysis - Unified Architecture

> **Purpose**: Analyze disabled legacy tests to identify missing coverage for our unified IR-native architecture  
> **Approach**: Follow proper BDD naming (`should [action]`) per TESTING_GUIDELINES.md  
> **Goal**: Create comprehensive test plan without "cheating" - validate real functionality

## 📊 **Current Test Status**

### **✅ COVERED - Integration Tests (4/4 passing)**
```kotlin
// test-sample/src/jvmTest/kotlin/FakeGenerationTest.kt
@Test fun `generated fake factory functions work`()          // Basic factory function creation
@Test fun `generated fakes implement interfaces correctly`() // Interface implementation validation  
@Test fun `generated fake methods are callable`()           // Method execution validation
@Test fun `generated configuration DSL is available`()      // Configuration DSL structure
```

### **✅ COVERED - Unit Tests (9/9 passing)**
```kotlin
// compiler/src/test/kotlin/.../UnifiedFaktIrGenerationExtensionTest.kt
@Test fun `should create extension instance successfully`()              // Basic instantiation
@Test fun `should have required public methods for IR generation`()     // Method availability
@Test fun `should handle interface name processing correctly`()          // String utilities
@Test fun `should collect messages during processing`()                 // Message handling
@Test fun `should handle error scenarios gracefully`()                  // Error handling
@Test fun `should handle nullable type annotations correctly`()          // Type handling
// ... (9 total tests)
```

## 🔍 **Gap Analysis from Disabled Tests**

Based on analysis of 19 disabled test files, here are the missing coverage areas:

### **🏗️ CORE GENERATION - Missing Coverage**

#### **Implementation Class Generation**
```kotlin
// From: ImplementationClassGeneratorTest.kt.disabled
// MISSING TESTS for generateImplementationClass():

@Test fun `should create fake implementation class with correct name`()
@Test fun `should override all interface methods with proper signatures`() 
@Test fun `should create behavior fields for each method`()
@Test fun `should create configuration methods for each behavior`()
@Test fun `should generate thread-safe instance-based implementation`()
@Test fun `should include proper imports and package declarations`()
```

#### **Factory Function Generation**
```kotlin
// From: FactoryFunctionGeneratorTest.kt.disabled  
// MISSING TESTS for generateFactoryFunction():

@Test fun `should create factory function with camelCase naming`()
@Test fun `should include configuration parameter with correct type`()
@Test fun `should return implementation instance wrapped in configuration`()
@Test fun `should handle interfaces with no methods`()
@Test fun `should support custom factory naming through annotations`()
```

#### **Configuration DSL Generation**
```kotlin
// From: ConfigurationDslGeneratorTest.kt.disabled
// MISSING TESTS for generateConfigurationDsl():

@Test fun `should create configuration class with correct name`()
@Test fun `should generate configuration methods for each interface method`()
@Test fun `should preserve method parameter types in configuration`()
@Test fun `should handle suspend functions in configuration DSL`()
@Test fun `should create type-safe configuration without Any casting`()
```

### **🎯 ADVANCED FEATURES - Missing Coverage**

#### **Call Tracking (@Fake(trackCalls = true))**
```kotlin
// From: CallTrackingGeneratorTest.kt.disabled
// MISSING TESTS for call tracking functionality:

@Test fun `should generate call data classes for tracked methods`()
@Test fun `should create call collection lists for each tracked method`()
@Test fun `should generate verification methods for call tracking`()
@Test fun `should support call clearing and history management`()
@Test fun `should handle multiple parameters in call tracking`()
@Test fun `should provide call count verification methods`()
```

#### **Builder Pattern Support (@Fake(builder = true))**
```kotlin  
// From: BuilderPatternGeneratorTest.kt.disabled
// MISSING TESTS for builder pattern functionality:

@Test fun `should generate builder class for data classes`()
@Test fun `should create fluent builder methods`()
@Test fun `should support nested fake dependencies in builders`()
@Test fun `should generate builder factory functions`()
@Test fun `should handle default values in builder pattern`()
```

#### **Cross-Module Dependencies (@Fake(dependencies = [...]))**
```kotlin
// From: CrossModuleDependencyGeneratorTest.kt.disabled  
// MISSING TESTS for dependency injection:

@Test fun `should detect dependency interfaces from annotation`()
@Test fun `should generate dependency injection in constructor`()
@Test fun `should provide configuration access for dependencies`()
@Test fun `should handle circular dependency detection`()
@Test fun `should support cross-module dependency resolution`()
```

### **⚡ TYPE SYSTEM - Missing Coverage**

#### **Default Value Generation**
```kotlin
// From: DefaultValueGeneratorTest.kt.disabled
// MISSING TESTS for getDefaultValue():

@Test fun `should generate correct defaults for primitive types`()
@Test fun `should generate null for nullable types`()
@Test fun `should handle collection types with empty collections`()
@Test fun `should support custom types with constructor defaults`()
@Test fun `should handle generic types with bounds`()
@Test fun `should generate proper defaults for coroutine types`()
```

#### **Type Mapping Enhancement**
```kotlin
// MISSING TESTS for irTypeToKotlinString():

@Test fun `should convert complex generic types correctly`()
@Test fun `should handle variance annotations in generics`()
@Test fun `should support sealed class type mapping`()
@Test fun `should handle function types with receivers`()
@Test fun `should convert suspend function types correctly`()
```

### **🔧 ERROR HANDLING - Missing Coverage**

#### **Diagnostics and Validation**
```kotlin
// From: FaktErrorsTest.kt.disabled
// MISSING TESTS for error scenarios:

@Test fun `should report clear error for invalid interface types`()
@Test fun `should detect unsupported return types`()
@Test fun `should validate annotation parameter combinations`()
@Test fun `should handle compilation errors gracefully`()
@Test fun `should provide helpful error messages with source locations`()
```

#### **Test-Only Generation Security**
```kotlin
// From: TestOnlyGenerationTest.kt.disabled
// MISSING TESTS for security validation:

@Test fun `should only generate fakes in test source sets`()
@Test fun `should reject generation in main source sets`()
@Test fun `should detect source set types correctly`()
@Test fun `should handle mixed source set scenarios`()
```

### **🔌 PLUGIN INFRASTRUCTURE - Missing Coverage**

#### **Compiler Plugin Registration**
```kotlin
// From: KtFakeCompilerPluginRegistrarTest.kt.disabled
// MISSING TESTS for plugin setup:

@Test fun `should register FIR extension correctly`()
@Test fun `should register IR extension correctly`()
@Test fun `should handle plugin configuration parameters`()
@Test fun `should support debug mode activation`()
```

#### **FIR Phase Processing**  
```kotlin
// From: FakeAnnotationDetectorTest.kt.disabled, ThreadSafetyCheckerTest.kt.disabled
// MISSING TESTS for FIR phase:

@Test fun `should detect @Fake annotations during FIR phase`()
@Test fun `should validate thread safety requirements`()
@Test fun `should suppress appropriate compiler warnings`()
@Test fun `should handle annotation parameter validation`()
```

## 📋 **Test Implementation Priority**

### **🚨 HIGH PRIORITY (Core Functionality)**
1. **Implementation Class Generation Tests** - These validate our main `generateImplementationClass()` method
2. **Factory Function Generation Tests** - Critical for the factory pattern we use
3. **Configuration DSL Tests** - Validates type-safe configuration without Any casting
4. **Default Value Generation Tests** - Ensures proper type defaults for all scenarios

### **⚡ MEDIUM PRIORITY (Type System)**
5. **Enhanced Type Mapping Tests** - Complex generics, nullable types, suspend functions
6. **Error Handling Tests** - Proper diagnostics and helpful error messages
7. **Test-Only Generation Security** - Ensures security constraints are enforced

### **🔮 LOW PRIORITY (Advanced Features)**
8. **Call Tracking Tests** - For `@Fake(trackCalls = true)` functionality
9. **Builder Pattern Tests** - For `@Fake(builder = true)` functionality  
10. **Cross-Module Dependency Tests** - For `@Fake(dependencies = [...])` functionality

## 🎯 **Recommended Test Implementation Strategy**

### **Phase 1: Core Generation Validation**
Focus on testing the three main generation methods in `UnifiedFaktIrGenerationExtension`:

```kotlin
class UnifiedFaktIrGenerationExtensionTest {
    // Current tests (9/9 passing) ✅
    
    // ADD - Implementation Class Generation Tests
    @Test fun `should generate implementation class with correct structure`()
    @Test fun `should create behavior fields for all interface methods`() 
    @Test fun `should generate configuration methods with proper signatures`()
    @Test fun `should handle suspend functions in implementation class`()
    @Test fun `should create thread-safe instance-based implementation`()
    
    // ADD - Factory Function Generation Tests  
    @Test fun `should generate factory function with correct naming`()
    @Test fun `should include configuration parameter in factory signature`()
    @Test fun `should return configured implementation instance`()
    
    // ADD - Configuration DSL Generation Tests
    @Test fun `should create configuration class with proper structure`()
    @Test fun `should preserve method signatures in configuration DSL`()
    @Test fun `should support suspend function configuration`()
}
```

### **Phase 2: Type System Enhancement**
Expand type handling validation:

```kotlin
class UnifiedTypeSystemTest {
    @Test fun `should convert all primitive types correctly`()
    @Test fun `should handle nullable type annotations`()
    @Test fun `should generate appropriate defaults for collection types`()
    @Test fun `should support generic types with bounds`()
    @Test fun `should handle complex nested generic types`()
}
```

### **Phase 3: Advanced Features** 
Implement tests for future features as we add them:

```kotlin
class CallTrackingTest {
    @Test fun `should generate call data classes for tracked methods`()
    // ... (implement as features are added)
}
```

## 🚀 **Implementation Notes**

### **BDD Compliance**
All new tests must follow the `should [action]` naming pattern from TESTING_GUIDELINES.md:
- ✅ CORRECT: `@Test fun 'should generate implementation class with correct structure'()`
- ❌ INCORRECT: `@Test fun 'GIVEN interface WHEN generating THEN should create class'()`

### **No Cheating Policy**
All tests must validate real functionality:
- ✅ Test actual generated code content
- ✅ Verify real compilation and type safety
- ❌ Don't mock core generation methods
- ❌ Don't use placeholder assertions like `assertTrue(true)`

### **Integration with Current Architecture**
All new tests should work with the existing `UnifiedFaktIrGenerationExtension` class:
- Use the existing `internal` methods we exposed for testing
- Follow the established pattern from current working tests
- Maintain the same test utilities and mock helpers

This comprehensive test plan ensures we cover all the functionality that was in the disabled legacy tests while following proper BDD conventions and maintaining our "no cheating" quality standards.