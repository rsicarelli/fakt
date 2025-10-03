# Testing Guidelines - KtFakes Compiler Plugin

> **THE BIBLE**: This is our **absolute testing standard**
> **Source**: Based on `/ktfake/docs/TESTING_GUIDELINES.md`
> **Applies to**: ALL test code in documentation, examples, and implementations

## 🎯 **Golden Rule**

> **"Simplicity, clarity and comprehensive coverage. Use vanilla testing with descriptive BDD names."**

Every test MUST follow:
1. **GIVEN**: Context of the situation being tested
2. **WHEN**: The action being executed
3. **THEN**: The expected result

## 📋 **Required Test Structure**

### **✅ CORRECT Pattern - ALWAYS Use This**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnifiedKtFakesIrGenerationExtensionTest {

    @Test
    fun `GIVEN interface with suspend functions WHEN generating fake THEN should preserve suspend signatures`() = runTest {
        // Given - create isolated instances
        val asyncInterface = createTestInterface("AsyncService") {
            method("getUser") { suspend(); returns("User") }
            method("updateUser") { suspend(); returns("Boolean") }
        }
        val generator = UnifiedKtFakesIrGenerationExtension()

        // When
        val result = generator.generateFakeImplementation(asyncInterface)

        // Then
        assertTrue(result.hasMethod("getUser"))
        assertTrue(result.getMethod("getUser").isSuspend)
        assertTrue(result.compiles())
    }
}
```

### **❌ WRONG Patterns - NEVER Use These**
```kotlin
// ❌ Wrong: "should" pattern (not our standard)
@Test
fun `should generate fake implementation correctly`() { ... }

// ❌ Wrong: Non-BDD naming
@Test
fun testFakeGeneration() { ... }

// ❌ Wrong: Too verbose Given-When-Then
@Test
fun `GIVEN interface with methods WHEN we generate implementation THEN it should work correctly`() { ... }

// ❌ Wrong: Missing lifecycle annotation
class SomeTest { ... } // Missing @TestInstance

// ❌ Wrong: Custom matchers
assertThat(result).hasSize(1).contains("expected") // Use vanilla assertions
```

## 🏗️ **Required Testing Framework**

### **Framework Stack**
- **100% Kotlin Test + JUnit5** (NO custom matchers)
- **@TestInstance(TestInstance.Lifecycle.PER_CLASS)** (always required)
- **runTest** for coroutines code
- **Vanilla assertions** - assertEquals, assertTrue, assertNotNull, etc.
- **Fakes instead of mocks**

### **Basic Test Template**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompilerPluginTest {

    @Test
    fun `GIVEN interface with properties WHEN analyzing THEN should extract property metadata`() = runTest {
        // Given - each test creates its own instances
        val testInterface = createTestInterface("UserService") {
            property("currentUser") { type("User"); nullable() }
            property("isLoggedIn") { type("Boolean") }
        }
        val analyzer = InterfaceAnalyzer()

        // When
        val metadata = analyzer.analyze(testInterface)

        // Then
        assertEquals(2, metadata.properties.size)
        assertTrue(metadata.hasProperty("currentUser"))
        assertTrue(metadata.getProperty("currentUser").isNullable)
    }
}
```

## 🎯 **Compiler Plugin Specific Patterns**

### **Testing IR Generation**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrGenerationTest {

    @Test
    fun `GIVEN interface with generic methods WHEN generating IR THEN should preserve type parameters`() = runTest {
        // Given - isolated instances for each test
        val genericInterface = createTestInterface("Repository") {
            method("save") {
                typeParameter("T")
                parameter("item", "T")
                returns("T")
            }
        }
        val irGenerator = IrCodeGenerator()

        // When
        val generatedIr = irGenerator.generate(genericInterface)

        // Then
        assertNotNull(generatedIr)
        assertTrue(generatedIr.hasTypeParameter("T"))
        assertEquals("T", generatedIr.getMethod("save").returnType)
    }
}
```

### **Testing Compilation Validation**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompilationValidationTest {

    @Test
    fun `GIVEN generated fake implementation WHEN compiling THEN should compile without errors`() = runTest {
        // Given
        val fakeImplementation = generateFakeFor("UserService")
        val compiler = KotlinCompiler()

        // When
        val compilationResult = compiler.compile(fakeImplementation)

        // Then
        assertTrue(compilationResult.isSuccess)
        assertTrue(compilationResult.errors.isEmpty())
    }
}
```

### **Testing Type Safety**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypeSafetyTest {

    @Test
    fun `GIVEN interface with generic constraints WHEN generating THEN should preserve constraints`() = runTest {
        // Given
        val constrainedInterface = createTestInterface("Processor") {
            method("process") {
                typeParameter("T") { constraint("Comparable<T>") }
                parameter("items", "List<T>")
                returns("List<T>")
            }
        }
        val generator = TypeSafeGenerator()

        // When
        val result = generator.generateWithConstraints(constrainedInterface)

        // Then
        assertTrue(result.preservesConstraints)
        assertEquals("Comparable<T>", result.getConstraintFor("T"))
    }
}
```

## 🧪 **Test Organization with @Nested**

### **Logical Grouping**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeGenerationTest {

    @Nested
    inner class BasicInterfaceGeneration {

        @Test
        fun `GIVEN interface with simple methods WHEN generating THEN should create implementation class`() = runTest {
            // Test implementation
        }

        @Test
        fun `GIVEN interface with properties WHEN generating THEN should create property implementations`() = runTest {
            // Test implementation
        }
    }

    @Nested
    inner class GenericTypeHandling {

        @Test
        fun `GIVEN interface with method-level generics WHEN generating THEN should handle dynamic casting`() = runTest {
            // Test implementation
        }

        @Test
        fun `GIVEN interface with class-level generics WHEN generating THEN should use Any replacement`() = runTest {
            // Test implementation
        }
    }

    @Nested
    inner class SuspendFunctionSupport {

        @Test
        fun `GIVEN interface with suspend functions WHEN generating THEN should preserve suspend modifiers`() = runTest {
            // Test implementation
        }
    }
}
```

## 🎭 **Fakes Instead of Mocks**

### **Fake Builder Pattern**
```kotlin
// ✅ Use fakes with builder pattern
fun fakeInterfaceAnalyzer(configure: FakeInterfaceAnalyzerScope.() -> Unit = {}): InterfaceAnalyzer =
    FakeInterfaceAnalyzer().apply { FakeInterfaceAnalyzerScope(this).configure() }

class FakeInterfaceAnalyzerScope(private val fake: FakeInterfaceAnalyzer) {
    fun onAnalyze(block: (IrClass) -> InterfaceMetadata) {
        fake.analyzeHandler = block
    }

    fun simulateError(message: String = "Analysis failed") {
        fake.shouldFail = true
        fake.errorMessage = message
    }
}

class FakeInterfaceAnalyzer : InterfaceAnalyzer {
    var analyzeHandler: (IrClass) -> InterfaceMetadata = { InterfaceMetadata.empty() }
    var shouldFail: Boolean = false
    var errorMessage: String = ""

    override fun analyze(irClass: IrClass): InterfaceMetadata {
        if (shouldFail) {
            throw AnalysisException(errorMessage)
        }
        return analyzeHandler(irClass)
    }
}
```

### **Test Data Builders**
```kotlin
// ✅ Builder functions with default arguments
fun createTestInterface(
    name: String = "TestInterface",
    configure: TestInterfaceScope.() -> Unit = {}
): IrClass = TestInterfaceBuilder(name).apply {
    TestInterfaceScope(this).configure()
}.build()

fun createCompilerContext(
    messageCollector: MessageCollector = NoOpMessageCollector(),
    pluginContext: IrPluginContext = createMockPluginContext()
): CompilerContext = CompilerContext(messageCollector, pluginContext)

// Usage in tests
fun `GIVEN complex interface WHEN processing THEN should handle all features`() = runTest {
    // Given
    val complexInterface = createTestInterface("ComplexService") {
        property("status") { type("String") }
        method("process") {
            typeParameter("T")
            parameter("data", "T")
            returns("Result<T>")
            suspend()
        }
    }
    val context = createCompilerContext()

    // When & Then...
}
```

## 📊 **Assertion Patterns**

### **✅ Use Vanilla Kotlin-Test Assertions**
```kotlin
// ✅ Correct assertions
assertEquals(expected, actual)
assertTrue(condition)
assertFalse(condition)
assertNotNull(value)
assertNull(value)
assertFailsWith<ExceptionType> { code() }

// ✅ Collection assertions
assertEquals(3, list.size)
assertTrue(list.contains("expected"))
assertTrue(list.isEmpty())

// ✅ String assertions
assertTrue(text.contains("expected"))
assertTrue(text.startsWith("prefix"))
assertEquals("expected", text.trim())
```

### **❌ NO Custom Matchers**
```kotlin
// ❌ Wrong - custom matchers not allowed
assertThat(result).hasSize(1).contains("expected")
result.should.have.size(1)
expect(result).to.contain("expected")

// ✅ Correct - vanilla assertions
assertEquals(1, result.size)
assertTrue(result.contains("expected"))
```

## 🔧 **Compiler Plugin Testing Specifics**

### **Testing Generated Code**
```kotlin
@Test
fun `GIVEN interface with suspend methods WHEN generating fake THEN should compile successfully`() = runTest {
    // Given
    val suspendInterface = createTestInterface("AsyncService") {
        method("getData") { suspend(); returns("String") }
        method("updateData") { suspend(); parameter("data", "String"); returns("Unit") }
    }

    // When
    val generatedCode = generateFakeImplementation(suspendInterface)
    val compilationResult = compileKotlin(generatedCode)

    // Then
    assertTrue(compilationResult.isSuccess)
    assertTrue(generatedCode.contains("suspend fun getData()"))
    assertTrue(generatedCode.contains("suspend fun updateData(data: String)"))
}
```

### **Testing Type System Integration**
```kotlin
@Test
fun `GIVEN interface with generic constraints WHEN analyzing types THEN should extract constraint information`() = runTest {
    // Given
    val constrainedInterface = createTestInterface("TypeProcessor") {
        method("sort") {
            typeParameter("T") { constraint("Comparable<T>") }
            parameter("items", "List<T>")
            returns("List<T>")
        }
    }
    val typeAnalyzer = TypeSystemAnalyzer()

    // When
    val typeInfo = typeAnalyzer.analyzeMethod(constrainedInterface.getMethod("sort"))

    // Then
    assertEquals(1, typeInfo.typeParameters.size)
    assertEquals("T", typeInfo.typeParameters.first().name)
    assertEquals("Comparable<T>", typeInfo.typeParameters.first().constraint)
}
```

## 🚫 **Prohibited Practices**

❌ **"should" naming pattern** - Use GIVEN-WHEN-THEN
❌ **Custom BDD frameworks** - Use vanilla JUnit5
❌ **Custom matchers** - Use kotlin-test assertions only
❌ **Mocks** - Use fakes
❌ **@BeforeEach/@AfterEach** - Use isolated instances per test

## ✅ **Required Practices**

✅ **GIVEN-WHEN-THEN naming** - Always uppercase
✅ **@TestInstance(TestInstance.Lifecycle.PER_CLASS)** - Always required
✅ **runTest** for coroutines code
✅ **Isolated instances** - Create new instances in each test
✅ **Vanilla assertions** - kotlin-test only
✅ **Fakes with builders** - Higher-order functions for configuration
✅ **@Nested inner classes** for logical grouping

## 🔗 **Integration with Development Workflow**

### **Test-First Development**
```kotlin
// 1. Write failing test first
@Test
fun `GIVEN interface with varargs WHEN generating THEN should handle varargs correctly`() = runTest {
    // Given
    val varargsInterface = createTestInterface("VarargsService") {
        method("process") {
            parameter("items", "vararg String")
            returns("Int")
        }
    }

    // When
    val result = generateFakeImplementation(varargsInterface)

    // Then
    assertTrue(result.handlesVarargs)
    assertTrue(result.compiles())
}

// 2. Implement feature to make test pass
// 3. Refactor while keeping tests green
```

### **Test Categories for Compiler Plugin**
- **Interface Analysis Tests** - FIR phase functionality
- **Code Generation Tests** - IR phase functionality
- **Compilation Validation Tests** - Generated code must compile
- **Type Safety Tests** - Generic handling and type preservation
- **Integration Tests** - End-to-end compiler plugin execution

---

**This document is THE ABSOLUTE STANDARD for all testing in KtFakes. Every test code snippet in documentation MUST follow these patterns.**