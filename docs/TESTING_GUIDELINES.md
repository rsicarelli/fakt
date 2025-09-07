# KtFakes Testing Guidelines

**Core Principle**: Every compiler plugin component must have comprehensive unit tests using vanilla testing with BDD naming conventions.

## 🎯 **Golden Rule**

> **"Simplicity, clarity and comprehensive coverage. Use vanilla testing with descriptive BDD names for compiler plugin components."**

Every test should follow:
1. **GIVEN**: Context of the compiler plugin situation being tested
2. **WHEN**: The compilation/transformation action being executed 
3. **THEN**: The expected generated code or behavior

## 🏗️ **Testing Stack for Compiler Plugins**

### **Framework**
- **100% Kotlin Test + JUnit5** (NO custom matchers)
- **JetBrains Compiler Testing Infrastructure** (following Metro patterns)
- **Nested Tests** (`@Nested`) for FIR/IR phase grouping
- **Parameterized Tests** (`@ParameterizedTest`) for code generation scenarios
- **Box Tests** for end-to-end compilation + execution
- **Diagnostic Tests** for error reporting validation
- **Fakes for compiler context** instead of mocks
- **Optimized parallel execution**

### **Project Structure**
```
ktfake/compiler/
├── src/
│   ├── main/kotlin/dev/rsicarelli/ktfake/compiler/...
│   └── test/kotlin/
│       ├── dev/rsicarelli/ktfake/compiler/...Test.kt
│       └── utilities/
│           ├── CompilerTestBuilders.kt
│           ├── FirTestExtensions.kt
│           ├── IrTestExtensions.kt
│           └── TestFakes.kt

ktfake/compiler-tests/
├── src/test/data/
│   ├── box/           # Full compilation + execution tests
│   │   ├── basic/
│   │   ├── advanced/
│   │   └── performance/
│   ├── diagnostic/    # Error reporting tests
│   └── dump/         # FIR/IR inspection tests
└── src/test/kotlin/
    └── KtFakesCompilerTest.kt
```

## ⚡ **Optimized JUnit5 Configuration**

### **build.gradle.kts for Compiler Tests**
```kotlin
// ktfake/compiler/build.gradle.kts
tasks.test {
    useJUnitPlatform()
    
    // Optimized parallel execution for compiler tests
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
    
    // Compiler tests can be resource intensive
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "2")
    
    // Longer timeout for compiler tests
    systemProperty("junit.jupiter.execution.timeout.default", "60s")
    systemProperty("junit.jupiter.execution.timeout.testable.method.default", "30s")
    
    maxParallelForks = minOf(Runtime.getRuntime().availableProcessors(), 4)
    
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
    
    // Compiler tests require more memory
    jvmArgs("-Xmx2g", "-XX:MaxMetaspaceSize=512m")
}

// ktfake/compiler-tests/build.gradle.kts  
tasks.test {
    useJUnitPlatform()
    
    // Box tests execute generated code - sequential execution safer
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    
    // Box tests can take longer
    systemProperty("junit.jupiter.execution.timeout.default", "120s")
    
    maxParallelForks = 1 // Box tests should run sequentially
    
    jvmArgs("-Xmx4g", "-XX:MaxMetaspaceSize=1g") // More memory for compilation
}
```

### **junit-platform.properties for Compiler Tests**
```properties
# /ktfake/compiler/src/test/resources/junit-platform.properties

# Test Instance Lifecycle - PER_CLASS for better performance with compiler context
junit.jupiter.testinstance.lifecycle.default=per_class

# Parallel Configuration - Limited for compiler tests
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent  
junit.jupiter.execution.parallel.mode.classes.default=concurrent
junit.jupiter.execution.parallel.config.strategy=fixed
junit.jupiter.execution.parallel.config.fixed.parallelism=2

# Extended timeouts for compiler operations
junit.jupiter.execution.timeout.default=60s
junit.jupiter.execution.timeout.testable.method.default=30s

# Display Names for compiler test readability
junit.jupiter.displayname.generator.default=org.junit.jupiter.api.DisplayNameGenerator$ReplaceUnderscores

# Compiler test specific extensions
junit.jupiter.extensions.autodetection.enabled=true

# Memory management for compiler tests
junit.jupiter.cleanup.mode=always

# /ktfake/compiler-tests/src/test/resources/junit-platform.properties

# Box tests - sequential execution
junit.jupiter.testinstance.lifecycle.default=per_class
junit.jupiter.execution.parallel.enabled=false

# Extended timeouts for box tests (compilation + execution)
junit.jupiter.execution.timeout.default=120s
junit.jupiter.execution.timeout.testable.method.default=60s
```

## 📋 **Compiler Plugin Test Templates**

### **FIR Extension Test Template**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeDeclarationGeneratorTest {
    
    @Test 
    fun `GIVEN interface with @Fake annotation WHEN processing FIR declarations THEN should generate factory function declaration`() = runTest {
        // Given - each test creates its own FIR context
        val firSession = createTestFirSession()
        val sourceInterface = buildInterface(firSession) {
            name = "UserService"
            addAnnotation(ClassIds.FAKE_ANNOTATION)
            addFunction("getUser") {
                returnType = userTypeRef(firSession)
                addValueParameter("id", stringTypeRef(firSession))
            }
        }
        
        val generator = FakeFactoryFirGenerator(firSession)
        
        // When
        val generatedDeclarations = generator.generateTopLevelClassifiersAndNestedClassifiers(sourceInterface)
        
        // Then
        assertEquals(3, generatedDeclarations.size) // factory, impl, config
        
        val factoryFunction = generatedDeclarations.find { it.symbol.name.asString() == "fakeUserService" }
        assertNotNull(factoryFunction)
        assertTrue(factoryFunction is FirSimpleFunction)
        assertEquals(1, (factoryFunction as FirSimpleFunction).valueParameters.size) // configure lambda
    }
}
```

### **IR Transformation Test Template**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)  
class FakeImplementationTransformerTest {
    
    @Test
    fun `GIVEN @Fake interface WHEN transforming IR THEN should generate thread-safe implementation class`() = runTest {
        // Given - isolated IR context for each test
        val irContext = createTestIrContext()
        val sourceInterface = createTestInterface(irContext) {
            name = "UserService"
            addAnnotation(ClassIds.FAKE_ANNOTATION)
            addFunction("getUser") {
                returnType = userIrType(irContext)
                addValueParameter("id", stringIrType(irContext))
            }
        }
        
        val transformer = FakeImplementationTransformer(irContext)
        
        // When
        val transformedModule = sourceInterface.parent
        transformer.visitClass(sourceInterface)
        
        // Then
        val implementationClass = transformedModule.findClass("FakeUserServiceImpl")
        assertNotNull(implementationClass)
        
        // Verify thread-safe behavior field
        val behaviorField = implementationClass!!.fields.find { it.name.asString() == "getUserBehavior" }
        assertNotNull(behaviorField)
        assertTrue(behaviorField!!.type.isFunction()) // Should be function type
        
        // Verify method implementation
        val getUserMethod = implementationClass.functions.find { it.name.asString() == "getUser" }
        assertNotNull(getUserMethod)
        assertTrue(getUserMethod!!.body != null) // Should have implementation
    }
}
```

### **Box Test Template (Full Compilation)**
```kotlin
// ktfake/compiler-tests/src/test/data/box/basic/simple-interface.kt

@Fake
interface UserService {
    suspend fun getUser(id: String): User
}

data class User(val id: String, val name: String)

fun box(): String {
    val userService = fakeUserService {
        getUser { id -> User(id, "Test User") }  
    }
    
    val result = runBlocking { userService.getUser("123") }
    
    return if (result.name == "Test User" && result.id == "123") "OK" else "FAIL: ${result}"
}
```

### **Diagnostic Test Template (Error Reporting)**
```kotlin
// ktfake/compiler-tests/src/test/data/diagnostic/fake-object-error.kt

<!KTFAKES_FAKE_OBJECT_NOT_ALLOWED!>
@Fake
object UserService<!> {  // Should report error: objects not allowed
    fun getUser(): User = TODO()
}
```

## 🏗️ **Advanced JUnit5 Features for Compiler Testing**

### **Nested Tests for Compiler Phases**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtFakesCompilerPluginTest {
    
    @Nested
    inner class FirPhaseTests {
        
        @Test
        fun `GIVEN @Fake annotation WHEN FIR processes declarations THEN should generate type signatures`() = runTest {
            // Test FIR declaration generation
        }
        
        @Test 
        fun `GIVEN invalid @Fake usage WHEN FIR validates THEN should report diagnostic error`() = runTest {
            // Test FIR validation and error reporting
        }
        
        @Nested
        inner class ThreadSafetyValidation {
            
            @Test
            fun `GIVEN object declaration with @Fake WHEN validating THEN should report error`() = runTest {
                // Test thread-safety validation
            }
        }
    }
    
    @Nested
    inner class IrPhaseTests {
        
        @Test
        fun `GIVEN FIR declarations WHEN IR transforms THEN should generate implementation code`() = runTest {
            // Test IR code generation
        }
        
        @Nested
        inner class CodeGeneration {
            
            @Test
            fun `GIVEN interface methods WHEN generating implementation THEN should create behavior fields`() = runTest {
                // Test specific code generation patterns
            }
        }
    }
}
```

### **Parameterized Tests for Code Generation Scenarios**
```kotlin
class FakeGenerationScenariosTest {
    
    @ParameterizedTest
    @CsvSource(
        "trackCalls=false, 3",  // factory + impl + config
        "trackCalls=true, 4",   // + call tracking classes
        "builder=true, 4"       // + builder classes
    )
    fun `GIVEN different @Fake configurations WHEN generating code THEN should create correct number of classes`(
        configString: String,
        expectedClassCount: Int
    ) = runTest {
        // Given
        val config = parseFakeConfig(configString)
        val generator = FakeCodeGenerator()
        val sourceInterface = createTestInterface { 
            addAnnotation(ClassIds.FAKE_ANNOTATION, config)
        }
        
        // When
        val generatedClasses = generator.generateForInterface(sourceInterface)
        
        // Then
        assertEquals(expectedClassCount, generatedClasses.size)
    }
    
    @ParameterizedTest
    @MethodSource("provideFakeInterfaceScenarios")
    fun `GIVEN various interface signatures WHEN generating fakes THEN should handle all method types`(
        scenario: InterfaceScenario
    ) = runTest {
        // Given
        val generator = FakeCodeGenerator()
        
        // When
        val result = generator.generateForInterface(scenario.sourceInterface)
        
        // Then
        assertEquals(scenario.expectedFactoryMethods, result.factoryMethodCount)
        assertEquals(scenario.expectedConfigMethods, result.configMethodCount)
    }
    
    companion object {
        @JvmStatic
        fun provideFakeInterfaceScenarios() = listOf(
            InterfaceScenario(
                name = "Simple interface",
                sourceInterface = createInterface {
                    addFunction("getValue") { returnType = stringType }
                },
                expectedFactoryMethods = 1,
                expectedConfigMethods = 3 // behavior, value, throws
            ),
            InterfaceScenario(
                name = "Suspend interface", 
                sourceInterface = createInterface {
                    addSuspendFunction("fetchData") { returnType = dataType }
                },
                expectedFactoryMethods = 1,
                expectedConfigMethods = 3
            )
        )
    }
}
```

## 🧪 **Compiler Testing Principles**

### **🎯 Comprehensive Compiler Tests**
Implement tests for all compiler plugin phases:

- **FIR Extension Tests**: Declaration generation, validation, error reporting
- **IR Transformation Tests**: Code generation, optimization, correctness
- **End-to-End Tests**: Full compilation pipeline with execution verification
- **Error Handling**: Malformed input, edge cases, failure scenarios

### **🔌 Compiler Context Isolation**
Reduce coupling through proper test context management:
- **Isolated FIR sessions** for each test
- **Independent IR contexts** to prevent interference
- **Fresh plugin instances** for each test scenario

```kotlin
// ✅ Good practice - Isolated compiler context per test
class FirExtensionTest {
    @Test
    fun `GIVEN test interface WHEN processing FIR THEN should generate declarations`() = runTest {
        // Given - fresh context ensuring isolation
        val firSession = createTestFirSession()
        val extension = KtFakesFirExtension(firSession)
        val testInterface = buildTestInterface(firSession)
        
        // When
        val declarations = extension.generateDeclarations(testInterface)
        
        // Then  
        assertNotNull(declarations)
    }
}
```

### **🏗️ Modular Compiler Component Design**
Organize compiler plugin components for testability:

```kotlin
interface FakeCodeGenerator {
    fun generateFactoryFunction(sourceInterface: FirClass): FirSimpleFunction
    fun generateImplementationClass(sourceInterface: FirClass): FirClass  
    fun generateConfigurationClass(sourceInterface: FirClass): FirClass
}

class DefaultFakeCodeGenerator : FakeCodeGenerator {
    override fun generateFactoryFunction(sourceInterface: FirClass): FirSimpleFunction {
        return buildSimpleFunction {
            name = Name.identifier("fake${sourceInterface.name.asString().decapitalize()}")
            // Implementation details
        }
    }
}

class FakeCodeGeneratorTest {
    @Test
    fun `GIVEN interface with suspend methods WHEN generating factory THEN should preserve suspend modifier`() = runTest {
        // Given - new instance for each test
        val generator = DefaultFakeCodeGenerator()
        val suspendInterface = createTestInterface {
            addSuspendFunction("fetchData") { returnType = dataType }
        }
        
        // When
        val factory = generator.generateFactoryFunction(suspendInterface)
        
        // Then
        assertNotNull(factory)
        // Verify factory correctly handles suspend methods
    }
}
```

## 🎭 **Compiler Test Fakes Instead of Mocks**

### **FIR Session and Context Fakes**
```kotlin
// ✅ Idiomatic Kotlin - Fake FIR components
fun fakeFireSession(configure: FakeFirSessionScope.() -> Unit = {}): FirSession =
    FakeFirSession().apply { FakeFirSessionScope(this).configure() }

class FakeFirSessionScope(private val session: FakeFirSession) {
    fun withBuiltins(builtins: FirBuiltinTypes) {
        session.builtinTypes = builtins
    }
    
    fun withModuleData(moduleData: FirModuleData) {
        session.moduleData = moduleData
    }
}

class FakeFirSession : FirSession {
    var builtinTypes: FirBuiltinTypes = createTestBuiltinTypes()
    var moduleData: FirModuleData = createTestModuleData()
    
    override fun <T> service(service: Service<T>): T {
        // Provide fake services for testing
        return when (service) {
            is FirBuiltinTypes.Service -> builtinTypes as T
            else -> throw UnsupportedOperationException("Service ${service} not supported in fake")
        }
    }
}
```

### **IR Context Fakes**  
```kotlin
fun fakeIrContext(configure: FakeIrContextScope.() -> Unit = {}): IrPluginContext =
    FakeIrPluginContext().apply { FakeIrContextScope(this).configure() }

class FakeIrContextScope(private val context: FakeIrPluginContext) {
    fun withSymbols(symbols: IrBuiltIns) {
        context.irBuiltIns = symbols
    }
    
    fun withTypeTranslator(translator: TypeTranslator) {
        context.typeTranslator = translator
    }
}

class FakeIrPluginContext : IrPluginContext {
    var irBuiltIns: IrBuiltIns = createTestBuiltIns()
    var typeTranslator: TypeTranslator = createTestTypeTranslator()
    
    override fun referenceClass(classId: ClassId): IrClassSymbol? {
        // Return fake class symbols for testing
        return createFakeClassSymbol(classId)
    }
}
```

### **Test Data Builders for Compiler Constructs**
```kotlin
// ✅ Idiomatic Kotlin - Builders for compiler test data
fun firInterface(
    name: String = "TestInterface",
    configure: FirInterfaceBuilder.() -> Unit = {}
): FirRegularClass = FirInterfaceBuilder(name).apply(configure).build()

class FirInterfaceBuilder(private val name: String) {
    private val functions = mutableListOf<FirSimpleFunction>()
    private val annotations = mutableListOf<FirAnnotationCall>()
    
    fun addFunction(name: String, configure: FirFunctionBuilder.() -> Unit) {
        functions.add(FirFunctionBuilder(name).apply(configure).build())
    }
    
    fun addFakeAnnotation(trackCalls: Boolean = false, builder: Boolean = false) {
        annotations.add(createFakeAnnotation(trackCalls, builder))
    }
    
    fun build(): FirRegularClass = buildRegularClass {
        // Build FIR class with configured properties
        this.name = Name.identifier(this@FirInterfaceBuilder.name)
        classKind = ClassKind.INTERFACE
        declarations.addAll(functions)
        this.annotations.addAll(this@FirInterfaceBuilder.annotations)
    }
}

// ✅ IR test builders
fun irClass(
    name: String = "TestClass",
    configure: IrClassBuilder.() -> Unit = {}
): IrClass = IrClassBuilder(name).apply(configure).build()

class IrClassBuilder(private val name: String) {
    private val functions = mutableListOf<IrSimpleFunction>()
    
    fun addFunction(name: String, configure: IrFunctionBuilder.() -> Unit) {
        functions.add(IrFunctionBuilder(name).apply(configure).build())
    }
    
    fun build(): IrClass = irFactory.createClass(
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
        origin = IrDeclarationOrigin.DEFINED,
        name = Name.identifier(name),
        kind = ClassKind.CLASS,
        visibility = DescriptorVisibilities.PUBLIC,
        modality = Modality.FINAL
    ).apply {
        declarations.addAll(functions)
    }
}
```

### **Compiler Test Assertion Extensions**
```kotlin
// ✅ Test-specific utility functions for compiler constructs
fun FirDeclaration.assertHasAnnotation(annotationClassId: ClassId) {
    assertTrue(
        annotations.any { it.annotationTypeRef.classId == annotationClassId },
        "Expected declaration to have annotation ${annotationClassId}"
    )
}

fun FirSimpleFunction.assertIsThreadSafe() {
    // Verify function generates thread-safe implementation
    assertFalse(this.symbol.name.asString().contains("object"))
    assertTrue(this.symbol.name.asString().startsWith("fake"))
}

fun IrClass.assertImplementsInterface(interfaceName: String) {
    assertTrue(
        superTypes.any { it.classFqName?.shortName()?.asString() == interfaceName },
        "Expected class to implement interface $interfaceName"
    )
}

fun IrClass.assertHasMethod(methodName: String) {
    assertTrue(
        functions.any { it.name.asString() == methodName },
        "Expected class to have method $methodName"
    )
}

fun IrClass.assertHasField(fieldName: String) {
    assertTrue(
        fields.any { it.name.asString() == fieldName },
        "Expected class to have field $fieldName"
    )
}

// ✅ Code generation verification helpers
fun IrSimpleFunction.assertGeneratesCorrectBody() {
    assertNotNull(body, "Generated function should have body")
    assertTrue(body is IrBlockBody, "Generated function should have block body")
}

fun IrClass.assertIsThreadSafeImplementation() {
    // Verify no static/object patterns
    assertFalse(kind == ClassKind.OBJECT, "Implementation should not be object")
    
    // Verify behavior fields are instance-based
    fields.filter { it.name.asString().contains("Behavior") }.forEach { field ->
        assertFalse(field.isStatic, "Behavior field should not be static")
    }
}

// ✅ Generated code pattern verification
fun List<IrDeclaration>.assertContainsGeneratedPattern(
    factoryName: String,
    implName: String, 
    configName: String
) {
    assertTrue(any { it.name.asString() == factoryName }, "Should contain factory function")
    assertTrue(any { it.name.asString() == implName }, "Should contain implementation class")  
    assertTrue(any { it.name.asString() == configName }, "Should contain configuration class")
}
```

### **Box Test Execution Helpers**
```kotlin
// ✅ Box test utilities
fun String.executeAsBoxTest(): BoxTestResult {
    val compilationResult = compileKotlinCode(this)
    if (!compilationResult.success) {
        return BoxTestResult.CompilationFailed(compilationResult.errors)
    }
    
    val executionResult = executeCompiledCode(compilationResult.bytecode)
    return BoxTestResult.Success(executionResult.output)
}

sealed class BoxTestResult {
    data class Success(val output: String) : BoxTestResult()
    data class CompilationFailed(val errors: List<CompilerError>) : BoxTestResult()
    data class ExecutionFailed(val exception: Throwable) : BoxTestResult()
}

fun BoxTestResult.assertSuccess(expectedOutput: String = "OK") {
    when (this) {
        is BoxTestResult.Success -> assertEquals(expectedOutput, output)
        is BoxTestResult.CompilationFailed -> fail("Compilation failed: ${errors.joinToString()}")
        is BoxTestResult.ExecutionFailed -> fail("Execution failed", exception)
    }
}

// ✅ Usage in box tests
class BoxTestExecution {
    @Test 
    fun `GIVEN simple fake interface WHEN compiling and executing THEN should return OK`() = runTest {
        val boxTestCode = """
            @Fake
            interface TestService {
                fun getValue(): String
            }
            
            fun box(): String {
                val service = fakeTestService {
                    getValue("Test Value")
                }
                return if (service.getValue() == "Test Value") "OK" else "FAIL"
            }
        """.trimIndent()
        
        // When & Then
        boxTestCode.executeAsBoxTest().assertSuccess("OK")
    }
}
```

## 🧹 **Compiler Test Resource Management**

### **Compilation Context Cleanup**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompilerResourceTest {
    
    private lateinit var compilationEnvironment: CompilationEnvironment
    
    @BeforeAll
    fun setupCompilation() {
        compilationEnvironment = CompilationEnvironment.create()
    }
    
    @AfterAll  
    fun cleanupCompilation() {
        compilationEnvironment.cleanup()
    }
    
    @BeforeEach
    fun resetCompilationState() {
        // Reset compiler state before each test
        compilationEnvironment.reset()
    }
    
    @Test
    fun `GIVEN compilation environment WHEN processing source THEN should handle resources properly`() = runTest {
        // Test implementation with managed compilation resources
    }
}
```

### **Memory Management for Large Compilation Tests**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LargeCompilationTest {
    
    @AfterEach
    fun cleanupMemoryAfterCompilation() {
        // Force cleanup after memory-intensive compilation tests
        System.gc()
        System.runFinalization()
    }
    
    @Test
    fun `GIVEN large codebase WHEN compiling with plugin THEN should not exhaust memory`() = runTest {
        // Given
        val largeCodebase = generateLargeTestCodebase(fileCount = 100)
        
        // When
        val compilationResult = compileWithPlugin(largeCodebase)
        
        // Then
        assertTrue(compilationResult.success)
        
        // Help GC
        @Suppress("UNUSED_VALUE")
        largeCodebase = null
    }
}
```

## 🔒 **Compiler Test Isolation**

### **❌ Bad Practice - Shared Compiler State**
```kotlin
// ❌ Avoid - shared compiler context between tests
class CompilerPluginTest {
    private lateinit var pluginContext: IrPluginContext
    
    @BeforeEach
    fun setUp() {
        pluginContext = createPluginContext() // Dangerous reuse
    }
}
```

### **✅ Good Practice - Isolated Compiler Contexts**  
```kotlin
// ✅ Each test creates its own compiler context
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompilerPluginTest {
    
    @Test
    fun `GIVEN @Fake interface WHEN processing with plugin THEN should generate code`() = runTest {
        // Given - fresh context ensuring isolation
        val pluginContext = createTestPluginContext()
        val firSession = createTestFirSession()
        val plugin = KtFakesCompilerPlugin(pluginContext, firSession)
        val sourceInterface = createTestInterface(firSession)
        
        // When & Then
        assertNotNull(plugin.processInterface(sourceInterface))
    }
    
    @Test
    fun `GIVEN invalid @Fake usage WHEN processing THEN should report error`() = runTest {
        // Given - another isolated context
        val pluginContext = createTestPluginContext()
        val firSession = createTestFirSession()
        val plugin = KtFakesCompilerPlugin(pluginContext, firSession)
        val invalidInterface = createInvalidTestInterface(firSession)
        
        // When & Then
        assertFailsWith<CompilerError> { 
            plugin.processInterface(invalidInterface) 
        }
    }
}
```

## 📊 **Coverage and Quality for Compiler Plugin**

### **Coverage Goals by Layer**
- **Core Compiler Plugin** (FIR/IR extensions): 95%+ coverage
- **Code Generation Logic** (transformers, generators): 90%+ coverage  
- **Error Reporting** (diagnostics, validation): 95%+ coverage
- **Gradle Plugin Integration**: 85%+ coverage

### **Quality Gates**
- Execution on all PRs with compiler plugin changes
- Box tests must pass for merge (full compilation verification)
- Diagnostic tests must cover all error scenarios
- Performance regression testing for large codebases

### **Integration Test Example**
```kotlin
class GradlePluginCompilerIntegrationTest {
    
    @Test
    fun `GIVEN project with @Fake interfaces WHEN building THEN should generate working fakes`() = runTest {
        // Given - isolated test project
        val testProject = testGradleProject {
            buildFile {
                plugins {
                    kotlin("jvm")  
                    id("dev.rsicarelli.ktfake")
                }
            }
            
            sourceFile("UserService.kt", """
                @Fake
                interface UserService {
                    suspend fun getUser(id: String): User
                }
                
                data class User(val id: String, val name: String)
            """.trimIndent())
            
            testFile("UserServiceTest.kt", """
                class UserServiceTest {
                    @Test
                    fun testFake() {
                        val service = fakeUserService {
                            getUser { id -> User(id, "Test") }
                        }
                        assertEquals("Test", runBlocking { service.getUser("123").name })
                    }
                }
            """.trimIndent())
        }
        
        // When
        val result = testProject.runTask("test")
        
        // Then
        assertTrue(result.success, "Generated fakes should compile and work correctly")
        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }
}
```

## ✅ **Recommended Practices for Compiler Plugin Testing**

✅ **Descriptive compiler-specific test names** with Given-When-Then  
✅ **Phase-specific fakes** (FIR session fakes, IR context fakes)  
✅ **Isolated compiler contexts** for each test  
✅ **Box tests for end-to-end verification**  
✅ **Diagnostic tests for all error scenarios**  
✅ **100% Standard kotlin-test assertions** (NO custom matchers)  
✅ **@Nested inner classes** for FIR/IR phase grouping  
✅ **@ParameterizedTest** for code generation scenarios  
✅ **Memory management** for compilation tests  
✅ **Resource cleanup** for compiler contexts  
✅ **Performance testing** for large codebase scenarios  

## 🚫 **Prohibited Practices for Compiler Testing**

❌ **Shared compiler contexts** between tests  
❌ **Mocking compiler internals** (use fakes)  
❌ **Custom compiler test DSLs**  
❌ **Testing compiler framework instead of plugin logic**  
❌ **Ignoring memory management** in compilation tests  
❌ **Skipping box tests** for generated code verification  
❌ **Manual compilation** instead of using test infrastructure  

This testing guideline ensures comprehensive, reliable testing of the KtFakes compiler plugin while maintaining the vanilla testing principles and adapting them specifically for compiler plugin development challenges.