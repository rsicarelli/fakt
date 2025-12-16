# Type Safety Validation - Generic Scoping & Type Preservation

> **Purpose**: Ensure type safety preservation in generated fake implementations
> **Challenge**: Phase 2 generic type parameter scoping architecture
> **Approach**: Metro patterns + Kotlin compiler API validation
> **Testing Standard**: [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)

## 🎯 **Type Safety Principles**

### **Core Requirements**
1. **Interface contract preservation** - Method signatures must match exactly
2. **Generic type handling** - Type parameters preserved where possible
3. **Compile-time safety** - No unsafe `Any` casting in user API
4. **Runtime safety** - Safe fallbacks with proper error handling

## 🔍 **Generic Type Scoping Challenge**

### **The Core Problem**
```kotlin
// Interface with method-level generics
interface AsyncDataService {
    suspend fun <T> processData(data: T): T
}

// Generated implementation challenge
class FakeAsyncDataServiceImpl : AsyncDataService {
    // ❌ Problem: T not in scope at class level
    private var processDataBehavior: suspend (T) -> T = { it }  // COMPILATION ERROR

    // ✅ Method level: T is in scope
    override suspend fun <T> processData(data: T): T = processDataBehavior(data)  // TYPE MISMATCH
}
```

### **Metro-Inspired Solution Approach**
```kotlin
// Phase 2 solution: Dynamic casting with identity functions
class FakeAsyncDataServiceImpl : AsyncDataService {
    // Use Any? with identity function as safe default
    private var processDataBehavior: suspend (Any?) -> Any? = { it }

    override suspend fun <T> processData(data: T): T {
        @Suppress("UNCHECKED_CAST")
        return processDataBehavior(data) as T
    }

    // Configuration remains type-safe for user
    fun configureProcessData(behavior: suspend (Any?) -> Any?) {
        processDataBehavior = behavior
    }
}
```

## ✅ **Type Safety Validation Tests**

### **1. Interface Contract Preservation**

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InterfaceContractPreservationTest {

    @Test
    fun `GIVEN interface with methods and properties WHEN generating fake THEN should preserve exact signatures`() = runTest {
        // Given
        val userService = createTestInterface("UserService") {
            method("getUser") { suspend(); parameter("id", "String"); returns("User") }
            method("updateUser") { parameter("user", "User"); returns("Boolean") }
            property("currentUser") { type("User"); nullable() }
        }
        val generator = FakeImplementationGenerator()
    
        // When
        val fakeImpl = generator.generate(userService)
    
        // Then
        assertTrue(fakeImpl.hasMethod("getUser"))
        assertTrue(fakeImpl.getMethod("getUser").isSuspend)
        assertEquals("User", fakeImpl.getMethod("getUser").returnType)
        assertTrue(fakeImpl.hasMethod("updateUser"))
        assertEquals("Boolean", fakeImpl.getMethod("updateUser").returnType)
        assertTrue(fakeImpl.hasProperty("currentUser"))
        assertTrue(fakeImpl.getProperty("currentUser").isNullable)
    }
}
```

### **2. Generic Type Parameter Handling**

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MethodLevelGenericHandlingTest {

    @Test
    fun `GIVEN interface with method-level generics WHEN generating fake THEN should handle type parameters safely`() = runTest {
        // Given
        val repository = createTestInterface("Repository") {
            method("save") { typeParameter("T"); parameter("item", "T"); returns("T") }
            method("findById") { typeParameter("T"); parameter("id", "String"); returns("T?") }
            method("processAsync") { suspend(); typeParameter("T"); parameter("data", "T"); returns("T") }
        }
        val generator = FakeImplementationGenerator()

        // When
        val fakeImpl = generator.generate(repository)
        val compilationResult = compileKotlinCode(fakeImpl)

        // Then
        assertTrue(compilationResult.isSuccess)
        assertTrue(fakeImpl.containsUncheckedCastSuppression)
        assertTrue(fakeImpl.usesIdentityFunctionDefaults)
    }
}
```

### **3. Class-Level vs Method-Level Generics**



```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericTypeDifferentiationTest {
    @Test
    fun `GIVEN interface with method-level generics WHEN generating fake THEN should use dynamic casting strategy`() = runTest {
        // Given
        val methodLevelGeneric = createTestInterface("Processor") {
            method("process") { typeParameter("T"); parameter("item", "T"); returns("T") }
            method("transform") { typeParameter("R"); parameter("input", "String"); returns("R") }
        }
        val generator = FakeImplementationGenerator()

        // When
        val methodFake = generator.generate(methodLevelGeneric)

        // Then
        assertTrue(methodFake.usesDynamicCasting)
        // Future: Class-level generics will use different strategy
    }
}  
```
### **4. Type Constraint Handling**

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericConstraintHandlingTest {

    @Test
    fun `GIVEN interface with generic constraints WHEN generating fake THEN should handle constraints safely`() = runTest {
        // Given
        val constrainedService = createTestInterface("ConstrainedService") {
            method("sort") {
                typeParameter("T") { constraint("Comparable<T>") }
                parameter("items", "List<T>")
                returns("List<T>")
            }
            method("transform") {
                typeParameter("R") { constraint("Number"); constraint("Comparable<R>") }
                typeParameter("T")
                parameter("input", "T")
                returns("R")
            }
        }
        val generator = FakeImplementationGenerator()

        // When
        val fakeImpl = generator.generate(constrainedService)
        val compilationResult = compileKotlinCode(fakeImpl)

        // Then
        assertTrue(compilationResult.isSuccess)
        assertTrue(fakeImpl.preservesTypeConstraints)
    }
}
```

## 🔧 **Smart Default Value Strategy**

### **Type-Aware Default Generation**



```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypeSafeDefaultValuesTest {
		@Test
    fun `GIVEN interface with various return types WHEN generating fake THEN should create appropriate defaults`() = runTest {
        // Given
        val service = createTestInterface("DefaultValueService") {
            method("getString") { returns("String") }
            method("getInt") { returns("Int") }
            method("getList") { returns("List<User>") }
            method("getMap") { returns("Map<String, Int>") }
            method("getResult") { returns("Result<String>") }
            method("getNullableUser") { returns("User?") }
        }
        val generator = FakeImplementationGenerator()

        // When
        val fakeImpl = generator.generate(service)

        // Then
        assertEquals("\"\"", fakeImpl.getDefaultFor("getString"))
        assertEquals("0", fakeImpl.getDefaultFor("getInt"))
        assertEquals("emptyList()", fakeImpl.getDefaultFor("getList"))
        assertEquals("emptyMap()", fakeImpl.getDefaultFor("getMap"))
        assertEquals("Result.success(\"\")", fakeImpl.getDefaultFor("getResult"))
        assertEquals("null", fakeImpl.getDefaultFor("getNullableUser"))
        assertFalse(fakeImpl.containsTODOStatements)
    }
}
```


### **Identity Function Defaults for Generics**

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdentityFunctionDefaultsTest {
    @Test
    fun `GIVEN interface with generic methods WHEN generating fake THEN should use identity function defaults`() = runTest {
        // Given
        val processor = createTestInterface("GenericProcessor") {
            method("identity") { typeParameter("T"); parameter("value", "T"); returns("T") }
            method("asyncIdentity") { suspend(); typeParameter("T"); parameter("value", "T"); returns("T") }
            method("process") { typeParameter("T"); parameter("input", "T"); returns("T") }
        }
        val generator = FakeImplementationGenerator()

        // When
        val fakeImpl = generator.generate(processor)
        val compilationResult = compileKotlinCode(fakeImpl)

        // Then
        assertEquals("{ it }", fakeImpl.getDefaultBehaviorFor("identity"))
        assertEquals("{ it }", fakeImpl.getDefaultBehaviorFor("asyncIdentity"))
        assertEquals("{ it }", fakeImpl.getDefaultBehaviorFor("process"))
        assertTrue(compilationResult.isSuccess)
    }
}
```

## 🚨 **Critical Type Safety Scenarios**

### **1. Suspend Function Type Preservation**



```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SuspendFunctionPreservationTest {
		@Test
    fun `GIVEN interface with suspend functions WHEN generating fake THEN should preserve suspend signatures`() = runTest {
        // Given
        val asyncService = createTestInterface("AsyncService") {
            method("getUser") { suspend(); parameter("id", "String"); returns("User") }
            method("processData") { suspend(); typeParameter("T"); parameter("data", "T"); returns("T") }
            method("normalFunction") { returns("String") }
        }
        val generator = FakeImplementationGenerator()

        // When
        val fakeImpl = generator.generate(asyncService)

        // Then
        assertTrue(fakeImpl.getMethod("getUser").isSuspend)
        assertTrue(fakeImpl.getMethod("processData").isSuspend)
        assertFalse(fakeImpl.getMethod("normalFunction").isSuspend)
        assertTrue(fakeImpl.getBehaviorProperty("getUser").isSuspend)
        assertTrue(fakeImpl.getBehaviorProperty("processData").isSuspend)
    }
}
```
### **2. Function Type Safety**

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FunctionTypeSafetyTest {

    @Test
    fun `GIVEN interface with function types WHEN generating fake THEN should handle function types safely`() = runTest {
        // Given
        val eventProcessor = createTestInterface("EventProcessor") {
            method("process") {
                parameter("item", "Any")
                parameter("processor", "(Any) -> String")
                returns("String")
            }
            method("processAsync") {
                suspend()
                parameter("item", "String")
                parameter("processor", "suspend (String) -> String")
                returns("String")
            }
            method("transform") {
                parameter("mapper", "(Int) -> String")
                parameter("reducer", "(String, String) -> String")
                returns("String")
            }
        }
        val generator = FakeImplementationGenerator()

        // When
        val fakeImpl = generator.generate(eventProcessor)
        val compilationResult = compileKotlinCode(fakeImpl)

        // Then
        assertTrue(fakeImpl.usesProperLambdaSyntax)
        assertFalse(fakeImpl.containsText("Function1"))
        assertFalse(fakeImpl.containsText("Function2"))
        assertTrue(compilationResult.isSuccess)
    }
}
```

### **3. Nullable Type Handling**



```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NullableTypeHandlingTest {

    @Test
    fun `GIVEN interface with nullable types WHEN generating fake THEN should handle nullability correctly`() = runTest {
        // Given
        val nullableService = createTestInterface("NullableService") {
            method("findUser") { parameter("id", "String"); returns("User?") }
            method("findGeneric") { typeParameter("T"); parameter("id", "String"); returns("T?") }
            method("getOptionalData") { suspend(); returns("String?") }
        }
        val generator = FakeImplementationGenerator()

        // When
        val fakeImpl = generator.generate(nullableService)
        val compilationResult = compileKotlinCode(fakeImpl)

        // Then
        assertEquals("null", fakeImpl.getDefaultFor("findUser"))
        assertEquals("null", fakeImpl.getDefaultFor("getOptionalData"))
        assertEquals("null", fakeImpl.getDefaultFor("findGeneric"))
        assertTrue(compilationResult.isSuccess)
    }
}    
```
## 📊 **Type Safety Metrics**

### **Success Criteria**
- **100% interface contract preservation** - All method signatures match
- **Zero unsafe casting** in user-facing APIs
- **Safe fallback defaults** for all type scenarios
- **Generic type handling** with appropriate suppressions

### **Phase 2 Generic Scoping Metrics**

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericScopingSuccessRateTest {

    @Test
    fun `GIVEN multiple generic interfaces WHEN measuring compilation success THEN should meet Phase 2 targets`() = runTest {
        // Given
        val genericInterfaces = listOf(
            createTestInterface("AsyncDataService") {
                method("processData") { suspend(); typeParameter("T"); parameter("data", "T"); returns("T") }
            },
            createTestInterface("GenericRepository") {
                method("save") { typeParameter("T"); parameter("item", "T"); returns("T") }
            },
            createTestInterface("EventProcessor") {
                method("process") { typeParameter("T"); parameter("event", "T"); returns("T") }
            }
        )
        val generator = FakeImplementationGenerator()

        // When
        val results = genericInterfaces.map { interfaceClass ->
            val fakeImpl = generator.generate(interfaceClass)
            val compilationResult = compileKotlinCode(fakeImpl)

            GenericHandlingResult(
                interfaceName = interfaceClass.name,
                compiles = compilationResult.isSuccess,
                usesIdentityDefaults = fakeImpl.usesIdentityFunctionDefaults,
                usesDynamicCasting = fakeImpl.usesDynamicCasting,
                typeParametersPreserved = fakeImpl.preservesTypeParameters
            )
        }
        val compilationSuccessRate = results.count { it.compiles } / results.size.toDouble()
        val identityDefaultsUsage = results.count { it.usesIdentityDefaults } / results.size.toDouble()

        // Then
        println("Generic compilation success: ${compilationSuccessRate * 100}%")
        println("Identity defaults usage: ${identityDefaultsUsage * 100}%")
        assertTrue(compilationSuccessRate > 0.90) // 90%+ compilation success
        assertTrue(identityDefaultsUsage > 0.80)  // 80%+ use safe defaults
    }
}
```

## 🎯 **Metro Pattern Application to Type Safety**

### **Context-Based Type Resolution**
```kotlin
// Apply Metro context pattern to type resolution
context(context: IrKtFakeContext)
private fun resolveTypeWithSafety(irType: IrType): TypeResolution {
    return when (irType) {
        is IrSimpleType -> {
            when (val classifier = irType.classifier) {
                is IrTypeParameterSymbol -> {
                    // Metro-inspired: safe type parameter handling
                    TypeResolution.GenericParameter(
                        name = classifier.owner.name.asString(),
                        useIdentityDefault = true,
                        requiresCasting = true
                    )
                }
                is IrClassSymbol -> {
                    // Known type - generate appropriate default
                    TypeResolution.KnownType(
                        kotlinType = classifier.owner.name.asString(),
                        defaultValue = generateDefaultValue(classifier.owner)
                    )
                }
            }
        }
        else -> TypeResolution.Unknown
    }
}
```

### **Error Handling with Metro Diagnostics**
```kotlin
// Apply Metro error handling to type safety issues
context(context: IrKtFakeContext)
private fun reportTypeSafetyIssue(element: IrElement, issue: TypeSafetyIssue) {
    when (issue) {
        is TypeSafetyIssue.GenericScoping -> {
            context.messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "KtFakes: Generic type parameter ${issue.parameterName} requires dynamic casting. " +
                "This is a known Phase 2 limitation.",
                element.sourceLocation()
            )
        }
        is TypeSafetyIssue.UnsafeDefault -> {
            context.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "KtFakes: Cannot generate safe default for type ${issue.typeName}. " +
                "Consider providing explicit default behavior.",
                element.sourceLocation()
            )
        }
    }
}
```

## 🔮 **Future Type Safety Enhancements**

### **Phase 3: Advanced Generic Handling**
- **Generic class generation** for class-level generics
- **Type substitution system** using Kotlin compiler internals
- **Advanced constraint handling** for complex type bounds
- **Cross-module generic resolution** for dependency injection

### **Performance Optimization**
- **Type caching** for repeated type resolution
- **Optimized casting** with compile-time validation
- **Smart import generation** for type dependencies

## 📚 **Related Documentation**

- **Compilation Validation**: `.claude/docs/validation/compilation-validation.md`
- **Metro Alignment**: `.claude/docs/development/metro-alignment.md`
- **Kotlin API Reference**: `.claude/docs/development/kotlin-api-reference.md`
- **Generic Scoping Analysis**: `fakt/docs/GENERIC_TYPE_SCOPING_ANALYSIS.md`

---

**Type safety is the foundation of reliable generated code. Metro patterns + Kotlin API validation ensure robust type handling.**