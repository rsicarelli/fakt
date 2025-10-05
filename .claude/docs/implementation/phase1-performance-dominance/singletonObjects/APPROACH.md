# Singleton Object Faking: Technical Approach

> **Feature**: Call-site replacement for singleton objects and companion objects
> **Priority**: CRITICAL (highest performance impact)
> **Complexity**: High (4-6 weeks)
> **Target Performance**: **1000x+ faster** than mockkObject

## Problem Statement

### The Singleton Object Challenge

Kotlin's `object` declarations are first-class language features for implementing singletons. They compile to a class with a static `INSTANCE` field, making them fundamentally difficult to replace or modify at test runtime.

**Example Problem:**

```kotlin
// Production code
object AnalyticsService {
    fun trackEvent(name: String, properties: Map<String, Any>) {
        // Send to analytics backend
    }

    fun getUserId(): String {
        return currentUser.id
    }
}

// Usage in production
class FeaturePresenter {
    fun onButtonClick() {
        AnalyticsService.trackEvent("button_clicked", mapOf("screen" to "home"))
    }
}

// Test code - how to fake this?
// Option 1: mockkObject (❌ 1300x+ slower!)
mockkObject(AnalyticsService)
every { AnalyticsService.trackEvent(any(), any()) } just Runs
```

### Current Solutions & Severe Pain

| Solution | Approach | Pain Points | Performance |
|----------|---------|-------------|-------------|
| **Dependency Injection** | Pass singleton as constructor param | ✅ Best practice<br>❌ Not always practical<br>❌ Legacy/third-party code | ✅ Fast |
| **mockkObject** | Runtime bytecode manipulation | ❌ **1300x+ slower**<br>❌ Community: "Never use"<br>❌ Major test suite bottleneck | ❌ CRITICAL |
| **Manual abstraction** | Wrap singleton in interface | ✅ Testable<br>❌ Boilerplate overhead<br>❌ Not always feasible | ✅ Fast |

### Research Evidence

From [Benchmarking MockK article](https://medium.com/@_kevinb/benchmarking-mockk-avoid-these-patterns-for-fast-unit-tests-220fc225da55):

> "Using `mockkObject` is **over 1,300 times slower** than injecting a faked dependency."

**Real Impact:**
- Single `mockkObject` call can add **seconds** to a test
- Test suites with 100+ object mocks: **minutes** of overhead
- Developer frustration: "Avoid at all costs"
- Architecture constraints: Design decisions driven by testing limitations

**This is the #1 performance pain point in Kotlin testing.**

---

## Fakt's Solution: Compile-time Call-site Replacement

### Revolutionary Approach

Instead of intercepting singleton calls at runtime, **rewrite the calls at compile time** in test code:

**Before (original test code):**
```kotlin
// Test source set - before Fakt transformation
@Test
fun `test analytics tracking`() {
    val presenter = FeaturePresenter()
    presenter.onButtonClick()

    // Original call to real singleton
    AnalyticsService.trackEvent("button_clicked", mapOf("screen" to "home"))
}
```

**After (Fakt IR transformation):**
```kotlin
// Test source set - after Fakt transformation (conceptual)
@Test
fun `test analytics tracking`() {
    val presenter = FeaturePresenter()
    presenter.onButtonClick()

    // Rewritten to call fake singleton
    FakeAnalyticsService.trackEvent("button_clicked", mapOf("screen" to "home"))
}
```

### How It Works

1. **Analysis Phase (FIR)**: Detect `@Fake` annotation on `object` declarations
2. **Generation Phase (IR)**: Generate a fake implementation class
3. **Transformation Phase (IR)**: Rewrite all calls in **test source sets** to use the fake
4. **Runtime**: Zero overhead, just regular method calls

---

## Implementation Plan

### Phase 1: Fake Object Generation (Week 1-2)

#### 1.1 Detect Singleton Objects

```kotlin
// In FaktFirExtensionRegistrar.kt
fun IrClass.isFakableObject(): Boolean {
    return when {
        !isObject -> false           // Not an object
        isCompanion -> true          // Companion objects (Phase 1.2b)
        kind == ClassKind.OBJECT -> true  // Singleton objects
        else -> false
    }
}

fun analyzeSingletonObject(irClass: IrClass): AnalyzedType.SingletonObject {
    val functions = irClass.functions
        .filter { it.isFakeCandidate }
        .toList()

    val properties = irClass.properties
        .filter { it.isFakeCandidate }
        .toList()

    return AnalyzedType.SingletonObject(
        declaration = irClass,
        functions = functions,
        properties = properties,
        isCompanion = irClass.isCompanion
    )
}
```

#### 1.2 Generate Fake Singleton Class

**Key Insight**: The fake is NOT a singleton—it's a regular class that can be instantiated in tests.

```kotlin
fun generateSingletonFake(objectType: AnalyzedType.SingletonObject): IrClass {
    val originalObject = objectType.declaration

    // Generate a regular class (not object!)
    val fakeClass = irFactory.buildClass {
        name = Name.identifier("Fake${originalObject.name}")
        kind = ClassKind.CLASS // Not OBJECT!
        modality = Modality.OPEN
    }

    // Implement the same interface/contract as the original
    // (if the object implements interfaces)
    fakeClass.superTypes = originalObject.superTypes.toList()

    // Generate behavior properties
    objectType.functions.forEach { function ->
        generateBehaviorProperty(fakeClass, function)
    }

    // Generate methods
    objectType.functions.forEach { function ->
        generateMethod(fakeClass, function)
    }

    // Generate configuration methods
    objectType.functions.forEach { function ->
        generateConfigMethod(fakeClass, function)
    }

    return fakeClass
}
```

**Generated Output Example:**

```kotlin
// Original
object AnalyticsService {
    fun trackEvent(name: String, properties: Map<String, Any>) {
        // Real implementation
    }
}

// Generated fake
class FakeAnalyticsService {
    private var trackEventBehavior: (String, Map<String, Any>) -> Unit = { _, _ -> }

    fun trackEvent(name: String, properties: Map<String, Any>) {
        trackEventBehavior(name, properties)
    }

    internal fun configureTrackEvent(behavior: (String, Map<String, Any>) -> Unit) {
        trackEventBehavior = behavior
    }
}

// Global instance for call-site replacement
@PublishedApi
internal val fakeAnalyticsServiceInstance = FakeAnalyticsService()

// Configuration DSL
class FakeAnalyticsServiceConfig(private val fake: FakeAnalyticsService) {
    fun trackEvent(behavior: (String, Map<String, Any>) -> Unit) {
        fake.configureTrackEvent(behavior)
    }
}

// Setup function (used in tests)
fun configureFakeAnalyticsService(configure: FakeAnalyticsServiceConfig.() -> Unit) {
    FakeAnalyticsServiceConfig(fakeAnalyticsServiceInstance).configure()
}
```

### Phase 2: Call-site Detection (Week 3-4)

#### 2.1 Identify Singleton Calls in Test Code

**Challenge**: Find all references to the singleton object in test source sets.

```kotlin
class SingletonCallSiteDetector(
    private val pluginContext: IrPluginContext
) {

    fun detectCallSites(module: IrModuleFragment): List<SingletonCallSite> {
        val callSites = mutableListOf<SingletonCallSite>()

        module.accept(object : IrElementVisitorVoid {
            override fun visitFile(declaration: IrFile) {
                // Only process test source sets
                if (declaration.isInTestSourceSet()) {
                    super.visitFile(declaration)
                }
            }

            override fun visitCall(expression: IrCall) {
                super.visitCall(expression)

                val callee = expression.symbol.owner
                val parent = callee.parent

                // Check if this call is to a singleton object
                if (parent is IrClass && parent.isObject && parent.hasFakeAnnotation()) {
                    callSites.add(
                        SingletonCallSite(
                            call = expression,
                            singletonObject = parent,
                            function = callee
                        )
                    )
                }
            }

            override fun visitGetObjectValue(expression: IrGetObjectValue) {
                super.visitGetObjectValue(expression)

                // Direct reference to singleton (e.g., AnalyticsService as a value)
                val objectClass = expression.symbol.owner
                if (objectClass.hasFakeAnnotation()) {
                    callSites.add(
                        SingletonCallSite.DirectReference(
                            expression = expression,
                            singletonObject = objectClass
                        )
                    )
                }
            }
        }, null)

        return callSites
    }

    private fun IrFile.isInTestSourceSet(): Boolean {
        // Check if file path contains "test" or "commonTest"
        return fileEntry.name.contains("/test/") ||
               fileEntry.name.contains("/commonTest/") ||
               fileEntry.name.contains("/jvmTest/") ||
               fileEntry.name.contains("/iosTest/")
    }
}

sealed class SingletonCallSite {
    data class FunctionCall(
        val call: IrCall,
        val singletonObject: IrClass,
        val function: IrSimpleFunction
    ) : SingletonCallSite()

    data class DirectReference(
        val expression: IrGetObjectValue,
        val singletonObject: IrClass
    ) : SingletonCallSite()
}
```

### Phase 3: Call-site Replacement (Week 5-6)

#### 3.1 IR Transformation

**Critical Phase**: Rewrite singleton calls to use the fake instance.

```kotlin
class SingletonCallSiteReplacer(
    private val pluginContext: IrPluginContext,
    private val irBuiltIns: IrBuiltIns
) : IrElementTransformerVoid() {

    private val singletonToFakeMap = mutableMapOf<IrClass, IrClass>()

    override fun visitFile(declaration: IrFile): IrFile {
        // Only transform test source sets
        return if (declaration.isInTestSourceSet()) {
            super.visitFile(declaration)
        } else {
            declaration // Leave production code unchanged
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val transformed = super.visitCall(expression) as IrCall

        val callee = transformed.symbol.owner
        val parent = callee.parent

        // Check if this is a call to a singleton object with @Fake
        if (parent is IrClass && parent.isObject && parent.hasFakeAnnotation()) {
            return rewriteSingletonCall(transformed, parent, callee)
        }

        return transformed
    }

    private fun rewriteSingletonCall(
        originalCall: IrCall,
        singletonObject: IrClass,
        originalFunction: IrSimpleFunction
    ): IrExpression {
        // Get or create the fake class
        val fakeClass = singletonToFakeMap.getOrPut(singletonObject) {
            findGeneratedFake(singletonObject)
        }

        // Find the corresponding function in the fake class
        val fakeFunction = fakeClass.functions
            .first { it.name == originalFunction.name }

        // Get the global fake instance
        val fakeInstance = fakeClass.properties
            .first { it.name.asString() == "fakeInstance" }

        // Rewrite: SingletonObject.method() → fakeInstance.method()
        return IrCallImpl(
            startOffset = originalCall.startOffset,
            endOffset = originalCall.endOffset,
            type = originalCall.type,
            symbol = fakeFunction.symbol,
            typeArgumentsCount = originalCall.typeArgumentsCount,
            valueArgumentsCount = originalCall.valueArgumentsCount
        ).apply {
            // Set dispatch receiver to the fake instance
            dispatchReceiver = IrGetFieldImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                symbol = fakeInstance.backingField!!.symbol,
                type = fakeClass.defaultType
            )

            // Copy all arguments
            for (i in 0 until originalCall.valueArgumentsCount) {
                putValueArgument(i, originalCall.getValueArgument(i))
            }
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        val objectClass = expression.symbol.owner

        if (objectClass.hasFakeAnnotation()) {
            // Replace: AnalyticsService → fakeAnalyticsServiceInstance
            val fakeClass = singletonToFakeMap.getOrPut(objectClass) {
                findGeneratedFake(objectClass)
            }

            val fakeInstance = fakeClass.properties
                .first { it.name.asString() == "fakeInstance" }

            return IrGetFieldImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                symbol = fakeInstance.backingField!!.symbol,
                type = fakeClass.defaultType
            )
        }

        return super.visitGetObjectValue(expression)
    }
}
```

#### 3.2 Integration with IR Generation

```kotlin
// In UnifiedFaktIrGenerationExtension.kt
override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    // Phase 1: Generate fakes (existing)
    val generatedFakes = generateAllFakes(moduleFragment, pluginContext)

    // Phase 2: NEW - Replace singleton calls in test code
    val singletonReplacer = SingletonCallSiteReplacer(pluginContext, irBuiltIns)
    moduleFragment.transform(singletonReplacer, null)

    // Phase 3: Write generated code
    writeGeneratedCode(generatedFakes)
}
```

---

## Edge Cases & Challenges

### Challenge 1: Companion Objects

Companion objects have similar issues but are scoped to a class:

```kotlin
class UserRepository {
    companion object {
        fun create(apiUrl: String): UserRepository {
            return UserRepository(apiUrl)
        }
    }
}

// Call site: UserRepository.create("https://api.example.com")
// Need to rewrite to: UserRepository.Companion using fake
```

**Solution**: Similar call-site replacement, handle qualified names:

```kotlin
// Detect: ClassName.CompanionObjectMember
if (parent is IrClass && parent.isCompanion) {
    val ownerClass = parent.parentAsClass
    // Generate FakeCompanion and rewrite calls
}
```

### Challenge 2: Import Statements

Rewriting calls might require adding imports to test files:

```kotlin
// Original test
import com.example.AnalyticsService

AnalyticsService.trackEvent(...)

// After transformation, might need:
import com.example.fakeAnalyticsServiceInstance
```

**Solution**: Use `ImportResolver` (existing infrastructure) to add necessary imports.

### Challenge 3: Property Access

Singletons can have properties:

```kotlin
object Config {
    val apiUrl: String = "https://api.example.com"
}

// Usage: Config.apiUrl
```

**Solution**: Generate fake properties with default values, allow configuration:

```kotlin
class FakeConfig {
    var apiUrl: String = "https://test.api.example.com"
}
```

### Challenge 4: Nested Objects

Objects can be nested:

```kotlin
class Analytics {
    object Events {
        fun track(name: String) { }
    }
}

// Usage: Analytics.Events.track("test")
```

**Solution**: Generate fakes for nested objects, handle qualified names in call-site replacement.

---

## Testing Strategy

### GIVEN-WHEN-THEN Coverage

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SingletonObjectFakeGenerationTest {

    @Test
    fun `GIVEN singleton object with methods WHEN generating fake THEN should create regular class`() = runTest {
        // Given
        val sourceCode = """
            @Fake
            object AnalyticsService {
                fun trackEvent(name: String) { }
            }
        """

        // When
        val result = compilationTestHelper.compile(sourceCode)

        // Then
        assertTrue(result.hasClass("FakeAnalyticsService"))
        assertFalse(result.getClass("FakeAnalyticsService").isObject)
        assertTrue(result.compiles())
    }

    @Test
    fun `GIVEN test code calling singleton WHEN transforming THEN should rewrite to fake instance`() = runTest {
        // Given
        val productionCode = """
            @Fake
            object AnalyticsService {
                fun trackEvent(name: String) { println(name) }
            }
        """

        val testCode = """
            fun testAnalytics() {
                AnalyticsService.trackEvent("test")
            }
        """

        // When
        val result = compilationTestHelper.compileWithTest(productionCode, testCode)

        // Then
        assertTrue(result.testCodeContainsCallTo("fakeAnalyticsServiceInstance"))
        assertFalse(result.testCodeContainsCallTo("AnalyticsService.INSTANCE"))
        assertTrue(result.compiles())
    }

    @Test
    fun `GIVEN companion object WHEN generating fake THEN should handle qualified names`() = runTest {
        // Given
        val sourceCode = """
            class UserRepository {
                @Fake
                companion object {
                    fun create(url: String): UserRepository = TODO()
                }
            }
        """

        // When
        val result = compilationTestHelper.compile(sourceCode)

        // Then
        assertTrue(result.hasClass("FakeUserRepositoryCompanion"))
        assertTrue(result.compiles())
    }
}
```

### Performance Benchmark Tests

**Critical**: Validate the 1000x+ performance claim.

```kotlin
@Test
fun `GIVEN singleton object fake WHEN compared to mockkObject THEN should be 1000x faster`() {
    val iterations = 1000

    // Baseline: mockkObject (runtime bytecode manipulation)
    val mockkTime = measureTime {
        repeat(iterations) {
            mockkObject(AnalyticsService)
            every { AnalyticsService.trackEvent(any()) } just Runs
            AnalyticsService.trackEvent("test")
            unmockkObject(AnalyticsService)
        }
    }

    // Fakt: compile-time fake (just method calls)
    val faktTime = measureTime {
        repeat(iterations) {
            configureFakeAnalyticsService {
                trackEvent { _ -> /* no-op */ }
            }
            fakeAnalyticsServiceInstance.trackEvent("test")
        }
    }

    val speedup = mockkTime.inWholeMilliseconds / faktTime.inWholeMilliseconds
    assertTrue(
        speedup >= 1000,
        "Expected 1000x+ speedup, got ${speedup}x. mockkTime=${mockkTime}, faktTime=${faktTime}"
    )
}
```

---

## Metro Alignment

### Relevant Metro Patterns

1. **IR Transformation**: Metro rewrites calls for dependency injection
   - Review: `metro/compiler/.../ir/MetroIrTransformer.kt`
   - Pattern: Call-site analysis and rewriting
   - Apply: Similar technique for singleton replacement

2. **Symbol Resolution**: Metro resolves injected dependencies
   - Review: `metro/compiler/.../ir/SymbolResolver.kt`
   - Pattern: Map original symbols to generated implementations
   - Apply: Map singleton objects to fake instances

3. **Source Set Detection**: Metro distinguishes prod vs. generated code
   - Pattern: File path analysis to determine context
   - Apply: Only transform test source sets

### Consultation Checklist

Before implementation:
- [ ] `/consult-kotlin-api IrCall` - Verify call rewriting APIs
- [ ] `/consult-kotlin-api IrGetObjectValue` - Object reference handling
- [ ] `/validate-metro-alignment` - Check Metro's IR transformation patterns
- [ ] Review Metro's approach to call-site replacement

---

## Success Criteria

### Must Have (P0)
- ✅ **1000x+ performance improvement** over mockkObject (benchmarked)
- ✅ Generate fake classes for singleton objects
- ✅ Rewrite singleton calls in test source sets only
- ✅ Zero impact on production code compilation
- ✅ Handle method calls and property access
- ✅ Comprehensive GIVEN-WHEN-THEN test coverage

### Should Have (P1)
- ✅ Companion object support
- ✅ Nested object handling
- ✅ Clear error messages for unsupported cases
- ✅ Import statement management

### Nice to Have (P2)
- ⏳ IDE integration (go-to-definition works for fakes)
- ⏳ Debugging support (stack traces show fake calls)
- ⏳ Configuration DSL for global fake setup

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Performance claims don't materialize | Low | CRITICAL | Continuous benchmarking, realistic tests |
| Call-site replacement too complex | Medium | High | Prototype early, consult Metro patterns |
| Edge cases break production code | Low | CRITICAL | Source set isolation, comprehensive tests |
| Developers don't understand setup | Medium | Medium | Excellent docs, migration examples |

---

## Migration Guide (Preview)

### From mockkObject

**Before (MockK):**
```kotlin
@Test
fun `test analytics`() {
    mockkObject(AnalyticsService)
    every { AnalyticsService.trackEvent(any()) } just Runs

    presenter.onButtonClick()

    verify { AnalyticsService.trackEvent("button_clicked", any()) }
    unmockkObject(AnalyticsService)
}
```

**After (Fakt):**
```kotlin
@Test
fun `test analytics`() {
    configureFakeAnalyticsService {
        trackEvent { name, properties ->
            // Optional: capture calls for assertion
            capturedCalls.add(name)
        }
    }

    presenter.onButtonClick()

    // State-based verification (fakes over mocks!)
    assertTrue(capturedCalls.contains("button_clicked"))
}
```

**Benefits**:
- **1000x+ faster** execution
- State-based testing (modern best practice)
- No setup/teardown (mockkObject/unmockkObject)

---

## Next Steps

1. ✅ Review this approach document
2. 🎯 Prototype call-site detection (prove feasibility)
3. 🎯 Implement fake generation for singleton objects
4. 🎯 Implement call-site replacement transformation
5. 🎯 Create performance benchmark suite
6. 🎯 Document migration from mockkObject
7. 🎯 Publish benchmark results (marketing material)

---

## References

- **Critical Research**: [Benchmarking MockK - 1300x slowdown](https://medium.com/@_kevinb/benchmarking-mockk-avoid-these-patterns-for-fast-unit-tests-220fc225da55)
- **MockK Object Mocking**: [MockK Guidebook](https://notwoods.github.io/mockk-guidebook/docs/mocking/static/)
- **Phase 1 README**: [../README.md](../README.md)
- **Main Roadmap**: [../../roadmap.md](../../roadmap.md)

---

**Singleton Objects = The 1000x performance revolution.** 🚀
