# KMP Lint NG - Testing Guidelines

**Core Principle**: Every code must have comprehensive unit tests using vanilla testing with BDD naming conventions.

## 🎯 **Golden Rule**

> **"Simplicity, clarity and comprehensive coverage. Use vanilla testing with descriptive BDD names."**

Every test should follow:
1. **GIVEN**: Context of the situation being tested
2. **WHEN**: The action being executed 
3. **THEN**: The expected result

## 🏗️ **Testing Stack**

### **Framework**
- **100% Kotlin Test + JUnit5** (NO custom matchers)
- **Nested Tests** (`@Nested`) for logical grouping
- **Parameterized Tests** (`@ParameterizedTest`) for data-driven testing
- **Coroutines Test** for async code
- **Fakes instead of mocks**
- **Optimized parallel execution**

### **Project Structure**
```
module/
├── src/
│   ├── main/kotlin/...
│   └── test/kotlin/
│       ├── com/rsicarelli/kmp/lint/...Test.kt
│       └── utilities/
│           ├── TestBuilders.kt
│           ├── TestExtensions.kt
│           └── TestFakes.kt
└── build.gradle.kts (JUnit5 configuration)
```

## ⚡ **Optimized JUnit5 Configuration**

### **build.gradle.kts**
```kotlin
tasks.test {
    useJUnitPlatform()
    
    // Optimized parallel execution
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
    
    // Hardware-based thread configuration
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "dynamic")
    systemProperty("junit.jupiter.execution.parallel.config.dynamic.factor", "2.0")
    
    // Global timeout to prevent hanging tests
    systemProperty("junit.jupiter.execution.timeout.default", "30s")
    
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}
```

### **junit-platform.properties**
```properties
# /src/test/resources/junit-platform.properties

# Test Instance Lifecycle - Use PER_CLASS for better performance and immutable fields
junit.jupiter.testinstance.lifecycle.default=per_class

# Parallel Execution Configuration
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent

# Dynamic Parallel Strategy - Adapts to hardware capabilities
junit.jupiter.execution.parallel.config.strategy=dynamic
junit.jupiter.execution.parallel.config.dynamic.factor=2.0

# Alternative: Fixed parallel strategy (use if dynamic doesn't work well)
# junit.jupiter.execution.parallel.config.strategy=fixed
# junit.jupiter.execution.parallel.config.fixed.parallelism=4

# Global Test Timeouts
junit.jupiter.execution.timeout.default=30s
junit.jupiter.execution.timeout.testable.method.default=10s
junit.jupiter.execution.timeout.testtemplate.method.default=30s

# Display Names
junit.jupiter.displayname.generator.default=org.junit.jupiter.api.DisplayNameGenerator$ReplaceUnderscores

# Test Method Ordering (optional - use when test order matters)
# junit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$OrderAnnotation

# Parameterized Tests
junit.jupiter.params.displayname.default={displayName} [{index}] {arguments}

# Conditional Test Execution  
junit.jupiter.conditions.deactivate=org.junit.jupiter.api.condition.*

# Extensions auto-detection (for custom extensions)
junit.jupiter.extensions.autodetection.enabled=true

# Cleanup Mode - Clean up test resources after each test
junit.jupiter.cleanup.mode=always

# Logging Configuration (if using java.util.logging)
java.util.logging.config.file=src/test/resources/logging-test.properties

# Memory Management for Large Test Suites
# -XX:MaxRAMPercentage=80 (add to JVM args in build.gradle.kts)
```

## 📋 **Test Template**

### **Basic Structure with Isolation**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RuleEngineTest {
    
    @Test 
    fun `GIVEN source file with violations WHEN analyzing THEN should return violations list`() = runTest {
        // Given - each test creates its own instances
        val sourceFile = sourceFile { 
            name = "TestFile.kt"
            content = "class myClass" // naming violation
        }
        val ruleEngine = DefaultRuleEngine()
        
        // When
        val violations = ruleEngine.analyzeFile(sourceFile)
        
        // Then
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("naming convention"))
    }
}
```

### **Practical Example - KMP Lint**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PenaltySystemTest {
    
    @Test
    fun `GIVEN violations with different severities WHEN calculating penalties THEN should sum correctly`() = runTest {
        // Given - isolated instances for each test
        val violations = listOf(
            violation { severity = PenaltySeverity.CRITICAL },
            violation { severity = PenaltySeverity.MAJOR },
            violation { severity = PenaltySeverity.MINOR }
        )
        val penaltySystem = DefaultPenaltySystem()
        
        // When
        val result = penaltySystem.calculatePenalties(violations)
        
        // Then
        assertEquals(3, result.totalViolations)
        assertEquals(16, result.weightedTotal) // 10 + 5 + 1
    }
    
    @Nested
    inner class ThresholdValidation {
        @Test
        fun `GIVEN module over threshold WHEN validating THEN should fail validation`() = runTest {
            // Given - fresh instance for isolation
            val moduleViolations = 15
            val threshold = 10
            val validator = thresholdValidator { maxViolations = threshold }
            
            // When
            val result = validator.validate(moduleViolations)
            
            // Then
            assertFalse(result.passed)
            assertEquals("Module exceeds threshold: 15 > 10", result.message)
        }
    }
}
```

## 🏗️ **Advanced JUnit5 Features**

### **Nested Tests for Logical Grouping**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PenaltySystemTest {
    
    @Nested
    inner class ThresholdValidation {
        
        @Test
        fun `GIVEN module within threshold WHEN validating THEN should pass`() = runTest {
            // Given
            val violations = listOf(violation(severity = PenaltySeverity.MINOR))
            val configuration = penaltyConfiguration { 
                moduleThreshold("test-module") { maxMinor = 10 }
            }
            val penaltySystem = DefaultPenaltySystem()
            
            // When
            val result = penaltySystem.calculatePenalties(violations, configuration)
            val validation = penaltySystem.validateThresholds(result, configuration)
            
            // Then
            assertTrue(validation.passed)
        }
        
        @Test 
        fun `GIVEN module over threshold WHEN validating THEN should fail`() = runTest {
            // Given
            val violations = listOf(violation(severity = PenaltySeverity.CRITICAL))
            val configuration = penaltyConfiguration { 
                moduleThreshold("test-module") { maxCritical = 0 }
            }
            val penaltySystem = DefaultPenaltySystem()
            
            // When
            val result = penaltySystem.calculatePenalties(violations, configuration)
            val validation = penaltySystem.validateThresholds(result, configuration)
            
            // Then
            assertFalse(validation.passed)
            assertEquals(listOf("test-module"), validation.failedModules)
        }
    }
    
    @Nested
    inner class SuggestionGeneration {
        
        @Test
        fun `GIVEN violations with quick fixes WHEN generating suggestions THEN should include quick fix options`() = runTest {
            // Test implementation
        }
    }
}
```

### **Parameterized Tests for Data-Driven Testing**
```kotlin
class SeverityWeightTest {
    
    @ParameterizedTest
    @CsvSource(
        "CRITICAL, 10",
        "MAJOR, 3", 
        "MINOR, 1"
    )
    fun `GIVEN penalty severity WHEN calculating weight THEN should return correct value`(
        severity: PenaltySeverity,
        expectedWeight: Int
    ) = runTest {
        // Given
        val configuration = penaltyConfiguration()
        
        // When
        val actualWeight = configuration.penaltyWeights[severity]
        
        // Then
        assertEquals(expectedWeight, actualWeight)
    }
    
    @ParameterizedTest
    @ValueSource(strings = ["TestClass", "AnotherClass", "YetAnotherClass"])
    fun `GIVEN different class names WHEN analyzing THEN should detect naming violations`(
        className: String
    ) = runTest {
        // Given
        val rule = NamingConventionRule()
        val element = ktClass { name = className.lowercase() } // Force violation
        
        // When
        val violations = rule.analyze(element)
        
        // Then
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("PascalCase"))
    }
    
    @ParameterizedTest
    @MethodSource("provideViolationScenarios")
    fun `GIVEN various violation scenarios WHEN processing THEN should handle correctly`(
        scenario: ViolationScenario
    ) = runTest {
        // Given
        val penaltySystem = DefaultPenaltySystem()
        
        // When
        val result = penaltySystem.calculatePenalties(scenario.violations, scenario.configuration)
        
        // Then
        assertEquals(scenario.expectedTotalViolations, result.totalViolations)
        assertEquals(scenario.expectedWeightedTotal, result.weightedTotal)
    }
    
    companion object {
        @JvmStatic
        fun provideViolationScenarios() = listOf(
            ViolationScenario(
                violations = listOf(violation(severity = PenaltySeverity.CRITICAL)),
                configuration = penaltyConfiguration(),
                expectedTotalViolations = 1,
                expectedWeightedTotal = 10
            ),
            ViolationScenario(
                violations = listOf(
                    violation(severity = PenaltySeverity.MAJOR),
                    violation(severity = PenaltySeverity.MINOR)
                ),
                configuration = penaltyConfiguration(),
                expectedTotalViolations = 2,
                expectedWeightedTotal = 4
            )
        )
    }
    
    data class ViolationScenario(
        val violations: List<RuleViolation>,
        val configuration: PenaltyConfiguration,
        val expectedTotalViolations: Int,
        val expectedWeightedTotal: Int
    )
}
```

## 🧪 **Testing Principles**

### **🎯 Comprehensive Tests**
Implement unit tests (logic, behavior, integration) comprehensively:

- **High coverage**: Focus primarily on core (analysis-api, rule-system) and rules layers
- **Edge-cases**: Test edge cases and failure scenarios exhaustively
- **Behavior**: Follow Gherkin Given/When/Then format

### **🔌 Dependency Minimization**
Reduce external dependencies through:
- **Inversion of control**
- **Higher-order functions**
- **Loose coupling**

```kotlin
// ✅ Good practice - Injected dependency
class RuleProcessor(private val analyzer: CodeAnalyzer) {
    fun processFile(file: SourceFile): List<RuleViolation> {
        val analysisResult = analyzer.analyze(file)
        return analysisResult.violations
    }
}

// ✅ Test with fake
class RuleProcessorTest {
    @Test
    fun `GIVEN fake analyzer WHEN processing file THEN should return violations`() {
        // Given - isolated instance
        val fakeAnalyzer = fakeCodeAnalyzer {
            onAnalyze { violations(violation("test violation")) }
        }
        val ruleProcessor = RuleProcessor(fakeAnalyzer)
        val testFile = sourceFile("Test.kt", "class Test")
        
        // When
        val result = ruleProcessor.processFile(testFile)
        
        // Then
        assertFalse(result.isEmpty())
    }
}
```

### **🏗️ Modular and Testable Design**
Organize code in small independent parts:

```kotlin
interface LintRule {
    fun analyze(element: KtElement): List<RuleViolation>
}

class NamingConventionRule : LintRule {
    override fun analyze(element: KtElement): List<RuleViolation> {
        if (element is KtClass && !element.name!!.isPascalCase()) {
            return listOf(violation("Class should use PascalCase"))
        }
        return emptyList()
    }
}

class NamingConventionRuleTest {
    @Test
    fun `GIVEN class with camelCase name WHEN analyzing THEN should return violation`() {
        // Given - new instance for each test
        val rule = NamingConventionRule()
        val element = ktClass { name = "myClass" }
        
        // When
        val violations = rule.analyze(element)
        
        // Then
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("PascalCase"))
    }
}
```

## 🎭 **Fakes Instead of Mocks**

### **Fluent Fake Builders with Higher-Order Functions**
```kotlin
// ✅ Idiomatic Kotlin - Fluent builders with DSL
fun fakeCodeAnalyzer(configure: FakeCodeAnalyzerScope.() -> Unit = {}): CodeAnalyzer =
    FakeCodeAnalyzer().apply { FakeCodeAnalyzerScope(this).configure() }

class FakeCodeAnalyzerScope(private val fake: FakeCodeAnalyzer) {
    fun onAnalyze(block: (SourceFile) -> AnalysisResult) {
        fake.analyzeHandler = block
    }
    
    fun simulateFailure(message: String = "Simulated failure") {
        fake.shouldFail = true
        fake.failureMessage = message
    }
}

class FakeCodeAnalyzer : CodeAnalyzer {
    var lastAnalyzedFile: SourceFile? = null
    var shouldFail: Boolean = false
    var failureMessage: String = "Analysis failed"
    var analyzeHandler: (SourceFile) -> AnalysisResult = { AnalysisResult(it, emptyList()) }
    
    override suspend fun analyze(file: SourceFile): AnalysisResult {
        lastAnalyzedFile = file
        return if (shouldFail) {
            throw AnalysisException(failureMessage)
        } else {
            analyzeHandler(file)
        }
    }
}
```

### **Test Data Builders with Default Arguments**
```kotlin
// ✅ Idiomatic Kotlin - Functions with default arguments
fun sourceFile(
    name: String = "Test.kt",
    content: String = "class Test",
    configure: SourceFileScope.() -> Unit = {}
): SourceFile = SourceFile(name, content).apply { SourceFileScope(this).configure() }

fun violation(
    message: String = "Test violation",
    severity: PenaltySeverity = PenaltySeverity.MAJOR,
    configure: ViolationScope.() -> Unit = {}
): RuleViolation = RuleViolation(
    rule = ruleMetadata(),
    element = null,
    message = message,
    severity = severity,
    quickFixes = emptyList(),
    context = emptyMap()
).apply { ViolationScope(this).configure() }

fun ruleMetadata(
    id: String = "test.rule",
    category: RuleCategory = RuleCategory.CORRECTNESS
): RuleMetadata = RuleMetadata(
    id = id,
    name = "Test Rule",
    description = "Rule for testing",
    category = category,
    severity = PenaltySeverity.MAJOR
)

// Usage examples
fun violations(vararg violations: RuleViolation): List<RuleViolation> = violations.toList()

fun thresholdValidator(configure: ThresholdValidatorScope.() -> Unit): ThresholdValidator =
    ThresholdValidator().apply { ThresholdValidatorScope(this).configure() }
```

### **Test Data with Data Classes**
```kotlin
// ✅ Use data classes for expected objects in assertions
data class ExpectedAnalysisResult(
    val totalViolations: Int,
    val criticalCount: Int,
    val majorCount: Int,
    val minorCount: Int,
    val moduleNames: List<String> = emptyList()
)

// ✅ Use data classes for parameterized test scenarios
data class PenaltyScenario(
    val description: String,
    val violations: List<RuleViolation>,
    val expectedResult: ExpectedAnalysisResult
)

// ✅ Helper functions with comprehensive default arguments
fun analysisResult(
    totalViolations: Int = 0,
    criticalCount: Int = 0,
    majorCount: Int = 0,
    minorCount: Int = 0,
    moduleNames: List<String> = emptyList(),
    configure: AnalysisResultScope.() -> Unit = {}
): AnalysisResult = AnalysisResult(
    violations = generateViolations(criticalCount, majorCount, minorCount),
    moduleResults = moduleNames.associateWith { ModuleResult(it) },
    totalCount = totalViolations
).apply { AnalysisResultScope(this).configure() }

// ✅ Builder pattern with data classes for complex scenarios
fun penaltyScenario(
    description: String,
    configure: PenaltyScenarioBuilder.() -> Unit
): PenaltyScenario = PenaltyScenarioBuilder(description).apply(configure).build()

class PenaltyScenarioBuilder(private val description: String) {
    private val violations = mutableListOf<RuleViolation>()
    private var expectedResult: ExpectedAnalysisResult? = null
    
    fun withViolations(vararg violations: RuleViolation) {
        this.violations.addAll(violations)
    }
    
    fun expectResult(configure: ExpectedAnalysisResult.Builder.() -> Unit) {
        expectedResult = ExpectedAnalysisResult.Builder().apply(configure).build()
    }
    
    fun build() = PenaltyScenario(description, violations.toList(), expectedResult!!)
}
```

### **Test Extension Functions**
```kotlin
// ✅ Idiomatic Kotlin - Extension functions for frequently used values
fun Int.toInstant(): Instant = Instant.ofEpochSecond(this.toLong())
fun Int.toUUID(): UUID = UUID.fromString("00000000-0000-0000-a000-${this.toString().padStart(11, '0')}")
fun String.toPath(): Path = Paths.get(this)

// ✅ Test-specific utility functions (NO custom matchers)
fun assertWithinTolerance(expected: Float, actual: Float, tolerance: Float = 0.001f) {
    assertTrue(kotlin.math.abs(expected - actual) <= tolerance, 
               "Expected $actual to be within $tolerance of $expected")
}

// ✅ Collection helper functions
fun <T> Collection<T>.assertContainsExactly(vararg expected: T) {
    assertEquals(expected.size, this.size, "Collection size mismatch")
    expected.forEach { expectedItem ->
        assertTrue(this.contains(expectedItem), "Expected collection to contain $expectedItem")
    }
}

fun <T> Collection<T>.assertContainsInOrder(vararg expected: T) {
    val actualList = this.toList()
    assertEquals(expected.size, actualList.size, "Collection size mismatch")
    expected.forEachIndexed { index, expectedItem ->
        assertEquals(expectedItem, actualList[index], "Item at index $index doesn't match")
    }
}

// ✅ String content verification helpers
fun String.assertContainsAll(vararg substrings: String) {
    substrings.forEach { substring ->
        assertTrue(this.contains(substring), "Expected '$this' to contain '$substring'")
    }
}

fun String.assertMatchesPattern(pattern: Regex, message: String = "String doesn't match expected pattern") {
    assertTrue(pattern.matches(this), "$message. Pattern: $pattern, Actual: $this")
}

// ✅ File and path helpers
fun Path.createTempFileWithContent(content: String, suffix: String = ".tmp"): Path {
    val tempFile = Files.createTempFile(this, "test", suffix)
    Files.write(tempFile, content.toByteArray())
    return tempFile
}

fun Path.assertFileExists(message: String = "File should exist") {
    assertTrue(Files.exists(this), "$message: $this")
}

fun Path.assertFileNotExists(message: String = "File should not exist") {
    assertFalse(Files.exists(this), "$message: $this")
}

// ✅ Time and duration helpers
fun Duration.assertLessThan(maxDuration: Duration) {
    assertTrue(this < maxDuration, "Duration $this should be less than $maxDuration")
}

fun measureTestTime(block: () -> Unit): Duration {
    val start = System.nanoTime()
    block()
    val end = System.nanoTime()
    return Duration.ofNanos(end - start)
}

// ✅ Exception helpers
inline fun <reified T : Throwable> assertThrowsWithMessage(
    expectedMessage: String,
    noinline executable: () -> Any?
): T {
    val exception = assertFailsWith<T> { executable() }
    assertTrue(exception.message?.contains(expectedMessage) == true,
              "Expected exception message to contain '$expectedMessage', but was: '${exception.message}'")
    return exception
}

// ✅ Resource cleanup helpers
fun <T : AutoCloseable, R> T.useInTest(block: (T) -> R): R {
    return this.use(block)
}

// ✅ Data validation helpers
fun <T> T?.assertNotNullAndGet(message: String = "Value should not be null"): T {
    assertNotNull(this, message)
    return this!!
}

fun <T> List<T>.assertSingleElement(message: String = "List should contain exactly one element"): T {
    assertEquals(1, this.size, message)
    return this.first()
}

// ✅ Fluent assertions for complex objects
fun <T> T.assertThat(assertion: (T) -> Unit): T {
    assertion(this)
    return this
}

// Usage example:
// result.assertThat { 
//     assertEquals(expectedCount, it.totalViolations)
//     assertTrue(it.hasWarnings)
// }
```

### **Comprehensive Failure Testing**
```kotlin
class LintRuleFailureTest {
    
    @Test
    fun `GIVEN malformed source code WHEN analyzing THEN should handle gracefully`() {
        // Given - isolated instance
        val rule = NamingConventionRule()
        val malformedElement = ktClass { 
            name = null // Malformed
            isValid = false 
        }
        
        // When & Then
        assertDoesNotThrow {
            val violations = rule.analyze(malformedElement)
            assertTrue(violations.isEmpty()) // Should handle gracefully
        }
    }
    
    @Test
    fun `GIVEN very long class name WHEN analyzing THEN should detect violation`() {
        // Given
        val rule = NamingConventionRule()
        val veryLongName = "A".repeat(100)
        val element = ktClass { name = veryLongName }
        
        // When
        val violations = rule.analyze(element)
        
        // Then
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("too long"))
    }
    
    @Test
    fun `GIVEN network failure during analysis WHEN processing THEN should retry gracefully`() {
        // Given
        val analyzer = fakeCodeAnalyzer {
            simulateFailure("Network timeout")
        }
        val file = sourceFile("Remote.kt", "class Remote")
        
        // When & Then
        val result = assertFailsWith<AnalysisException> {
            analyzer.analyze(file)
        }
        assertTrue(result.message!!.contains("Network timeout"))
    }
}
```

## 🧹 **Resource Management and Cleanup**

### **Immutable Test Data (Preferred)**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RuleEngineTest {
    
    // ✅ Immutable, non-nullable fields using val
    private val testConfiguration = penaltyConfiguration {
        moduleThreshold("test-module") { maxCritical = 0; maxMajor = 5 }
    }
    
    private val sampleViolations = listOf(
        violation(severity = PenaltySeverity.CRITICAL),
        violation(severity = PenaltySeverity.MAJOR)
    )
    
    @Test
    fun `GIVEN configuration WHEN processing violations THEN should validate correctly`() = runTest {
        // Given - reuse immutable data
        val ruleEngine = DefaultRuleEngine()
        
        // When
        val result = ruleEngine.processViolations(sampleViolations, testConfiguration)
        
        // Then
        assertNotNull(result)
    }
}
```

### **Temporary File Management**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileAnalysisTest {
    
    private lateinit var tempDirectory: Path
    
    @BeforeAll
    fun setupTempDirectory() {
        tempDirectory = Files.createTempDirectory("kmp-lint-test")
    }
    
    @AfterAll
    fun cleanupTempDirectory() {
        Files.walk(tempDirectory)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }
    
    @Test
    fun `GIVEN source file WHEN analyzing THEN should detect violations`() = runTest {
        // Given - create temp file
        val sourceFile = tempDirectory.createTempFileWithContent(
            content = "class myClass", // naming violation
            suffix = ".kt"
        )
        
        // When
        val analyzer = KotlinFileAnalyzer()
        val violations = analyzer.analyze(sourceFile)
        
        // Then
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("naming convention"))
        
        // File cleanup happens automatically in @AfterAll
    }
}
```

### **Database/Container Cleanup Patterns**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest {
    
    private lateinit var testDatabase: TestDatabase
    
    @BeforeAll
    fun setupDatabase() {
        testDatabase = TestDatabase.start()
    }
    
    @AfterAll
    fun tearDownDatabase() {
        testDatabase.stop()
    }
    
    @BeforeEach
    fun cleanDatabase() {
        // Clean database before each test for isolation
        testDatabase.clean()
    }
    
    @Test
    fun `GIVEN empty database WHEN inserting violations THEN should persist correctly`() = runTest {
        // Test implementation with clean database state
    }
}
```

### **Resource Cleanup with use() Extension**
```kotlin
class FileProcessingTest {
    
    @Test
    fun `GIVEN large file WHEN processing THEN should handle resources properly`() = runTest {
        // Given
        val largeFile = createLargeTestFile()
        
        // When & Then - automatic resource cleanup
        largeFile.inputStream().useInTest { stream ->
            val processor = FileProcessor()
            val result = processor.process(stream)
            
            assertNotNull(result)
            assertTrue(result.isProcessed)
        }
        
        // Stream is automatically closed
    }
}
```

### **Exception Safety in Cleanup**
```kotlin
class RobustCleanupTest {
    
    @Test 
    fun `GIVEN resources WHEN exception occurs THEN should cleanup safely`() = runTest {
        var resource1: TestResource? = null
        var resource2: TestResource? = null
        
        try {
            resource1 = TestResource.create()
            resource2 = TestResource.create()
            
            // Test logic that might throw
            performRiskyOperation(resource1, resource2)
            
        } finally {
            // Safe cleanup - handle null and exceptions
            resource1?.let { res ->
                kotlin.runCatching { res.close() }.onFailure { 
                    // Log cleanup failure but don't rethrow
                    println("Warning: Failed to cleanup resource1: ${it.message}")
                }
            }
            resource2?.let { res ->
                kotlin.runCatching { res.close() }.onFailure { 
                    println("Warning: Failed to cleanup resource2: ${it.message}")
                }
            }
        }
    }
}
```

### **Memory Management for Large Test Suites**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemoryAwareTest {
    
    // ✅ Use val for immutable shared data
    private val sharedConfiguration = heavyConfiguration()
    
    @AfterEach
    fun garbageCollectAfterTest() {
        // Force garbage collection after memory-intensive tests
        if (isMemoryIntensiveTest()) {
            System.gc()
        }
    }
    
    @Test
    fun `GIVEN large dataset WHEN processing THEN should not leak memory`() = runTest {
        // Given
        val largeDataset = generateLargeDataset()
        
        // When
        val result = processLargeDataset(largeDataset)
        
        // Then
        assertNotNull(result)
        
        // Clear large objects explicitly if needed
        @Suppress("UNUSED_VALUE")
        largeDataset = null // Help GC
    }
}
```

## 🔒 **Test Isolation**

### **❌ Bad Practice - Shared State**
```kotlin
// ❌ Avoid - shared state between tests
class RuleEngineTest {
    private lateinit var ruleEngine: RuleEngine
    
    @BeforeEach
    fun setUp() {
        ruleEngine = DefaultRuleEngine() // Dangerous reuse
    }
}
```

### **✅ Good Practice - Isolated Instances**
```kotlin
// ✅ Each test creates its own instances
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RuleEngineTest {
    
    @Test
    fun `GIVEN valid source file WHEN analyzing THEN should return analysis result`() {
        // Given - new instance ensuring isolation
        val ruleEngine = DefaultRuleEngine()
        val sourceFile = sourceFile("Test.kt", "class Test")
        
        // When & Then
        assertNotNull(ruleEngine.analyzeFile(sourceFile))
    }
    
    @Test
    fun `GIVEN invalid syntax file WHEN analyzing THEN should handle error`() {
        // Given - another isolated instance
        val ruleEngine = DefaultRuleEngine()
        val invalidFile = sourceFile("Invalid.kt", "class {")
        
        // When & Then
        assertDoesNotThrow { 
            ruleEngine.analyzeFile(invalidFile) 
        }
    }
}
```

## 📏 **Test Method Organization**

### **Logical Grouping with @Nested**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComprehensiveRuleEngineTest {
    
    @Nested
    inner class RuleRegistration {
        
        @Test
        fun `GIVEN new rule WHEN registering THEN should be available in registry`() = runTest {
            // Test implementation
        }
        
        @Test
        fun `GIVEN duplicate rule ID WHEN registering THEN should replace existing`() = runTest {
            // Test implementation
        }
        
        @ParameterizedTest
        @EnumSource(RuleCategory::class)
        fun `GIVEN rules from different categories WHEN registering THEN should organize by category`(
            category: RuleCategory
        ) = runTest {
            // Test implementation with category parameter
        }
    }
    
    @Nested
    inner class RuleExecution {
        
        @Test
        fun `GIVEN registered rules WHEN executing analysis THEN should process all applicable rules`() = runTest {
            // Test implementation
        }
        
        @Nested
        inner class ErrorHandling {
            
            @Test
            fun `GIVEN rule throws exception WHEN executing THEN should handle gracefully`() = runTest {
                // Test implementation
            }
            
            @Test
            fun `GIVEN malformed input WHEN processing THEN should return meaningful errors`() = runTest {
                // Test implementation
            }
        }
    }
    
    @Nested
    inner class Performance {
        
        @Test
        fun `GIVEN large codebase WHEN analyzing THEN should complete within timeout`() = runTest {
            // Performance test implementation
        }
        
        @Test
        fun `GIVEN parallel execution WHEN processing multiple files THEN should scale linearly`() = runTest {
            // Concurrency test implementation
        }
    }
}
```

### **Test Method Naming Conventions**
```kotlin
class RuleEngineNamingExamplesTest {
    
    // ✅ GOOD: Clear, descriptive BDD-style names
    @Test
    fun `GIVEN empty rule registry WHEN adding first rule THEN should initialize successfully`() = runTest { }
    
    @Test
    fun `GIVEN source file with 10 violations WHEN applying severity filter THEN should return only critical violations`() = runTest { }
    
    @Test
    fun `GIVEN rule with quick fixes WHEN violation detected THEN should include applicable fixes in result`() = runTest { }
    
    // ✅ GOOD: Edge cases clearly identified
    @Test
    fun `GIVEN null input file WHEN processing THEN should throw IllegalArgumentException`() = runTest { }
    
    @Test
    fun `GIVEN empty string as rule ID WHEN registering THEN should reject with validation error`() = runTest { }
    
    // ✅ GOOD: Integration scenarios
    @Test
    fun `GIVEN multiple modules with cross-dependencies WHEN analyzing project THEN should detect circular references`() = runTest { }
}
```

### **Test Data Organization**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WellOrganizedTest {
    
    // ✅ Immutable test data organized by purpose
    private val validationScenarios = listOf(
        ValidationScenario("empty input", "", ValidationResult.INVALID),
        ValidationScenario("valid class", "class MyClass", ValidationResult.VALID),
        ValidationScenario("invalid naming", "class myClass", ValidationResult.INVALID)
    )
    
    private val performanceTestData = PerformanceTestData(
        smallCodebase = generateCodebase(fileCount = 10),
        mediumCodebase = generateCodebase(fileCount = 100),
        largeCodebase = generateCodebase(fileCount = 1000)
    )
    
    // ✅ Group related test data
    companion object {
        
        @JvmStatic
        fun severityTestCases(): List<Arguments> = listOf(
            Arguments.of(PenaltySeverity.CRITICAL, 10, "should fail build"),
            Arguments.of(PenaltySeverity.MAJOR, 3, "should warn but continue"),
            Arguments.of(PenaltySeverity.MINOR, 1, "should pass with notification")
        )
        
        @JvmStatic
        fun ruleConfigurationCases(): List<RuleConfiguration> = listOf(
            ruleConfiguration { strictMode = true },
            ruleConfiguration { strictMode = false },
            ruleConfiguration { customThresholds = mapOf("test" to 5) }
        )
    }
    
    @Nested
    inner class ValidationTests {
        
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.rsicarelli.kmp.lint.WellOrganizedTest#severityTestCases")
        fun `GIVEN different penalty severities WHEN processing THEN should handle according to configuration`(
            severity: PenaltySeverity,
            expectedWeight: Int,
            expectedBehavior: String
        ) = runTest {
            // Test implementation using parameters
        }
    }
}
```

### **Test Lifecycle Management**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LifecycleAwareTest {
    
    private lateinit var expensiveResource: ExpensiveResource
    private val testMetrics = TestMetrics()
    
    @BeforeAll
    fun initializeOnce() {
        // ✅ Initialize expensive resources once per class
        expensiveResource = ExpensiveResource.initialize()
        testMetrics.startSuite("LifecycleAwareTest")
    }
    
    @AfterAll
    fun cleanupOnce() {
        // ✅ Cleanup expensive resources
        expensiveResource.cleanup()
        testMetrics.finalizeSuite()
        testMetrics.printReport()
    }
    
    @BeforeEach
    fun prepareTest() {
        // ✅ Reset state before each test
        expensiveResource.reset()
        testMetrics.startTest()
    }
    
    @AfterEach
    fun verifyTest() {
        // ✅ Verify test state and collect metrics
        testMetrics.endTest()
        assertTrue(expensiveResource.isInValidState(), "Resource should be in valid state after test")
    }
    
    @Test
    fun `GIVEN initialized resource WHEN performing operation THEN should maintain consistency`() = runTest {
        // Test implementation that uses expensiveResource
    }
}
```

### **Complex Scenario Organization**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComplexScenarioTest {
    
    @Nested
    @DisplayName("Multi-module Project Analysis")
    inner class MultiModuleAnalysis {
        
        @Test
        @DisplayName("Should analyze dependencies correctly")
        fun `GIVEN project with multiple modules WHEN analyzing dependencies THEN should build correct dependency graph`() = runTest {
            // Given
            val project = multiModuleProject {
                module("core") {
                    sourceFile("Domain.kt", "class User")
                }
                module("feature-auth") {
                    dependency("core")
                    sourceFile("AuthService.kt", "class AuthService(user: User)")
                }
                module("app") {
                    dependency("core")
                    dependency("feature-auth")
                }
            }
            
            // When
            val analyzer = ProjectAnalyzer()
            val result = analyzer.analyze(project)
            
            // Then
            assertEquals(3, result.modules.size)
            assertTrue(result.hasDependency("feature-auth", "core"))
            assertTrue(result.hasDependency("app", "feature-auth"))
            assertFalse(result.hasCircularDependencies())
        }
        
        @Nested
        @DisplayName("Circular Dependency Detection")
        inner class CircularDependencyDetection {
            
            @Test
            fun `GIVEN modules with circular dependency WHEN analyzing THEN should detect cycle`() = runTest {
                // Nested test for specific circular dependency scenarios
            }
        }
    }
}
```

## 📊 **Coverage and Quality**

### **Coverage Goals**
- **Core Layer** (analysis-api, rule-system, penalty-system): 95%+ coverage
- **Rules Layer** (correctness-rules, performance-rules, etc): 90%+ coverage  
- **Integrations Layer**: Focus on CLI and Gradle Plugin

### **Quality Gates**
- Execution on all PRs
- Merge blocking if coverage < minimum
- Mandatory review for adjustments

### **Integration Test Example**
```kotlin
class GradlePluginIntegrationTest {
    
    @Test
    fun `GIVEN project with lint violations WHEN running gradle task THEN should fail build`() {
        // Given - isolated project for testing
        val testProject = testProject {
            sourceFile("BadCode.kt", "class myClass") // Naming violation
        }
        
        // When
        val result = testProject.runTask("kmpLint")
        
        // Then
        assertFalse(result.success)
        assertTrue(result.output.contains("naming convention"))
    }
}
```

## 🚫 **Prohibited Practices**

❌ **Custom BDD frameworks**  
❌ **Complex test DSLs**  
❌ **Custom matchers** (use kotlin-test assertions only)  
❌ **Mocks** (use fakes)  
❌ **Unnecessary builders**  
❌ **Testing framework instead of code**  

## ✅ **Recommended Practices**

✅ Descriptive test names with Given-When-Then  
✅ Simple and focused fakes  
✅ Higher-order functions for test utilities  
✅ Extension functions for common values  
✅ Default arguments for test data builders  
✅ **100% Standard kotlin-test assertions** (NO custom matchers)  
✅ **@Nested inner classes** for logical test grouping  
✅ **@ParameterizedTest** for data-driven testing  
✅ **runTest** for coroutines code  
✅ **Focused and independent tests**  
✅ **Data classes for test parameters**  
✅ **Val fields with PER_CLASS lifecycle**	
