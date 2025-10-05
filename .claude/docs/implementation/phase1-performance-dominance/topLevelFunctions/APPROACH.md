# Top-level & Extension Function Faking: Technical Approach

> **Feature**: Call-site replacement for top-level and extension functions
> **Priority**: HIGH
> **Complexity**: High (4-6 weeks, leverage singleton infrastructure)
> **Target Performance**: **100x+ faster** than mockkStatic

## Problem Statement

### The Static Scope Challenge

Top-level functions and extension functions are powerful Kotlin features for organizing utility code outside of classes. However, under the hood, the compiler transpiles them into **static Java methods** within synthetic classes, making them difficult to intercept and replace for testing.

**Example Problem:**

```kotlin
// StringUtils.kt (top-level functions)
fun String.truncate(maxLength: Int): String {
    return if (length > maxLength) substring(0, maxLength) + "..." else this
}

fun formatPhoneNumber(number: String): String {
    // Complex formatting logic
    return number.replace(Regex("[^0-9]"), "")
}

// Usage in production
class UserPresenter {
    fun displayUser(user: User) {
        val displayName = user.name.truncate(20)
        val phone = formatPhoneNumber(user.phoneNumber)
        // ...
    }
}

// How to fake these in tests?
// Option 1: mockkStatic (❌ Slow, brittle, string-based)
mockkStatic("com.example.StringUtilsKt")
every { formatPhoneNumber(any()) } returns "+1234567890"
```

### Compiler-Generated Synthetic Classes

The Kotlin compiler transforms top-level functions into static methods:

```java
// What the compiler actually generates (simplified)
package com.example;

public final class StringUtilsKt {
    // Extension function becomes static with receiver as first param
    public static String truncate(String $receiver, int maxLength) {
        // Implementation
    }

    // Top-level function becomes static
    public static String formatPhoneNumber(String number) {
        // Implementation
    }
}
```

### Current Solutions & Pain Points

| Solution | Approach | Pain Points | Performance |
|----------|---------|-------------|-------------|
| **Don't test** | Avoid complex logic in extensions | ❌ Architecture constraint<br>❌ Limited utility functions | N/A |
| **mockkStatic** | Runtime static method interception | ❌ **Severe performance penalty**<br>❌ Brittle (string-based class name)<br>❌ Breaks on file renames | ❌ SLOW |
| **Wrapper class** | Move logic to injectable class | ✅ Testable<br>❌ Boilerplate overhead<br>❌ Defeats purpose of top-level | ✅ Fast |

### Research Evidence

From [MockK Advanced Features article](https://blog.kotlin-academy.com/mocking-is-not-rocket-science-mockk-advanced-features-42277e5983b5):

> "Static mocking requires extensive bytecode manipulation and is **significantly slower** than regular mocking. Developers are advised to avoid complex logic in extension functions that would necessitate mocking."

**Real Impact:**
- Test suites slow down noticeably with `mockkStatic`
- String-based class names (`"com.example.FileKt"`) are fragile
- File renames break tests in non-obvious ways
- Developers avoid useful patterns due to testing constraints

---

## Fakt's Solution: Type-safe Call-site Replacement

### Core Approach

Similar to singleton objects, but for static scope:

1. **Analysis Phase**: Detect `@Fake` annotation on files or specific functions
2. **Generation Phase**: Generate fake implementations
3. **Transformation Phase**: Rewrite calls in test code to use fakes
4. **Result**: Type-safe, fast, no string-based references

### Example Transformation

**Production Code (unchanged):**
```kotlin
// StringUtils.kt
@file:Fake // Annotate entire file

fun String.truncate(maxLength: Int): String {
    return if (length > maxLength) substring(0, maxLength) + "..." else this
}

fun formatPhoneNumber(number: String): String {
    return number.replace(Regex("[^0-9]"), "")
}
```

**Generated Fake:**
```kotlin
// FakeStringUtils.kt (in test source set)
object FakeStringUtils {
    var truncateBehavior: (String, Int) -> String = { str, max ->
        if (str.length > max) str.substring(0, max) + "..." else str
    }

    var formatPhoneNumberBehavior: (String) -> String = { it }

    fun truncate(receiver: String, maxLength: Int): String {
        return truncateBehavior(receiver, maxLength)
    }

    fun formatPhoneNumber(number: String): String {
        return formatPhoneNumberBehavior(number)
    }
}

// Configuration DSL
class FakeStringUtilsConfig {
    fun truncate(behavior: (String, Int) -> String) {
        FakeStringUtils.truncateBehavior = behavior
    }

    fun formatPhoneNumber(behavior: (String) -> String) {
        FakeStringUtils.formatPhoneNumberBehavior = behavior
    }
}

fun configureFakeStringUtils(configure: FakeStringUtilsConfig.() -> Unit) {
    FakeStringUtilsConfig().configure()
}
```

**Test Code Transformation:**
```kotlin
// Before transformation (original test code)
@Test
fun `test user display`() {
    val user = User("Very Long Name", "123-456-7890")
    presenter.displayUser(user)
    // Calls formatPhoneNumber() internally
}

// After Fakt transformation
@Test
fun `test user display`() {
    configureFakeStringUtils {
        formatPhoneNumber { "FAKE_PHONE" }
    }

    val user = User("Very Long Name", "123-456-7890")
    presenter.displayUser(user)
    // Now calls FakeStringUtils.formatPhoneNumber()
}
```

---

## Implementation Plan

### Phase 1: File-level Annotation & Analysis (Week 1)

#### 1.1 File-level @Fake Annotation

```kotlin
// Support @file:Fake annotation
// In runtime module
@Target(AnnotationTarget.FILE)
annotation class Fake

// Detection in FIR phase
class FaktFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        // Detect @file:Fake annotations
        +firAnnotationChecker { session ->
            FileAnnotationChecker(session)
        }
    }
}

class FileAnnotationChecker(session: FirSession) : FirAnnotationChecker() {
    override fun check(declaration: FirAnnotatedDeclaration, context: CheckerContext) {
        if (declaration is FirFile) {
            val hasFakeAnnotation = declaration.annotations.any { it.isFakeAnnotation() }
            if (hasFakeAnnotation) {
                // Mark this file for fake generation
                context.reportFakeFileDetected(declaration)
            }
        }
    }
}
```

#### 1.2 Analyze Top-level Functions

```kotlin
fun analyzeFakeFile(firFile: FirFile): AnalyzedType.TopLevelFunctions {
    val functions = firFile.declarations
        .filterIsInstance<FirSimpleFunction>()
        .map { analyzeFunction(it) }

    return AnalyzedType.TopLevelFunctions(
        sourceFile = firFile,
        functions = functions,
        syntheticClassName = determineSyntheticClassName(firFile)
    )
}

// Kotlin compiler generates synthetic class name from file name
fun determineSyntheticClassName(firFile: FirFile): String {
    val fileName = firFile.name.removeSuffix(".kt")
    return "${fileName}Kt"
}

// Example: StringUtils.kt → StringUtilsKt
```

### Phase 2: Extension Function Handling (Week 2)

#### 2.1 Detect Extension Functions

```kotlin
fun FirSimpleFunction.isExtensionFunction(): Boolean {
    return receiverParameter != null
}

data class ExtensionFunctionInfo(
    val function: FirSimpleFunction,
    val receiverType: FirTypeRef,
    val functionName: String,
    val parameters: List<FirValueParameter>
)

fun analyzeExtensionFunction(function: FirSimpleFunction): ExtensionFunctionInfo {
    return ExtensionFunctionInfo(
        function = function,
        receiverType = function.receiverParameter!!.typeRef,
        functionName = function.name.asString(),
        parameters = function.valueParameters
    )
}
```

#### 2.2 Generate Extension Function Fakes

Extension functions need special handling—they become static methods with receiver as first parameter:

```kotlin
// Original extension function
fun String.truncate(maxLength: Int): String { ... }

// Generated fake
object FakeStringUtils {
    var truncateBehavior: (String, Int) -> String = { str, max -> str }

    // Static method with receiver as first param
    fun truncate(receiver: String, maxLength: Int): String {
        return truncateBehavior(receiver, maxLength)
    }
}
```

### Phase 3: Call-site Replacement (Week 3-4)

#### 3.1 Detect Top-level Function Calls

**Challenge**: Identify calls to top-level functions vs. member functions.

```kotlin
class TopLevelFunctionCallDetector : IrElementVisitorVoid {

    private val topLevelCalls = mutableListOf<TopLevelCallSite>()

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)

        val callee = expression.symbol.owner
        val parent = callee.parent

        // Check if parent is a file facade (synthetic class for top-level functions)
        if (parent is IrClass && parent.isFileFacade()) {
            topLevelCalls.add(
                TopLevelCallSite.FunctionCall(
                    call = expression,
                    function = callee,
                    syntheticClass = parent
                )
            )
        }
    }

    override fun visitGetValue(expression: IrGetValue) {
        super.visitGetValue(expression)

        // Extension function calls might look different
        val valueSymbol = expression.symbol
        if (valueSymbol.owner.origin == IrDeclarationOrigin.EXTENSION_RECEIVER) {
            // This might be an extension function call
            // Need to trace usage
        }
    }
}

fun IrClass.isFileFacade(): Boolean {
    return origin == IrDeclarationOrigin.FILE_CLASS
}
```

#### 3.2 Rewrite Calls to Fakes

```kotlin
class TopLevelCallSiteReplacer : IrElementTransformerVoid() {

    override fun visitCall(expression: IrCall): IrExpression {
        val transformed = super.visitCall(expression) as IrCall

        val callee = transformed.symbol.owner
        val parent = callee.parent

        if (parent is IrClass && parent.isFileFacade() && parent.hasFakeAnnotation()) {
            return rewriteTopLevelCall(transformed, parent, callee)
        }

        return transformed
    }

    private fun rewriteTopLevelCall(
        originalCall: IrCall,
        syntheticClass: IrClass,
        originalFunction: IrSimpleFunction
    ): IrExpression {
        // Find the generated fake object
        val fakeObject = findGeneratedFake(syntheticClass)

        // Find corresponding function in fake
        val fakeFunction = fakeObject.functions
            .first { it.name == originalFunction.name }

        // Rewrite: formatPhoneNumber(x) → FakeStringUtils.formatPhoneNumber(x)
        return IrCallImpl(
            startOffset = originalCall.startOffset,
            endOffset = originalCall.endOffset,
            type = originalCall.type,
            symbol = fakeFunction.symbol,
            typeArgumentsCount = originalCall.typeArgumentsCount,
            valueArgumentsCount = originalCall.valueArgumentsCount
        ).apply {
            // For extension functions, receiver becomes first argument
            if (originalFunction.isExtensionFunction()) {
                putValueArgument(0, originalCall.extensionReceiver)
                for (i in 0 until originalCall.valueArgumentsCount) {
                    putValueArgument(i + 1, originalCall.getValueArgument(i))
                }
            } else {
                // Regular top-level function
                for (i in 0 until originalCall.valueArgumentsCount) {
                    putValueArgument(i, originalCall.getValueArgument(i))
                }
            }
        }
    }
}
```

### Phase 4: Integration (Week 5-6)

#### 4.1 Source Set Filtering

**Critical**: Only transform test source sets, leave production code unchanged.

```kotlin
override fun visitFile(declaration: IrFile): IrFile {
    return if (declaration.isInTestSourceSet()) {
        super.visitFile(declaration) // Apply transformation
    } else {
        declaration // Leave production code as-is
    }
}
```

#### 4.2 Import Management

Generated fakes need proper imports in test files:

```kotlin
// Original test
import com.example.formatPhoneNumber

// After transformation, need:
import com.example.FakeStringUtils
import com.example.configureFakeStringUtils
```

Use existing `ImportResolver` infrastructure.

---

## Edge Cases & Challenges

### Challenge 1: Operator Extensions

```kotlin
operator fun String.plus(other: String): String {
    return this + other + "!!!"
}

// Usage: "hello" + "world"
```

**Solution**: Generate fake for operator, handle operator call sites specially.

### Challenge 2: Infix Extensions

```kotlin
infix fun Int.add(other: Int): Int {
    return this + other
}

// Usage: 1 add 2
```

**Solution**: Detect infix calls, rewrite appropriately.

### Challenge 3: Generic Extension Functions

```kotlin
fun <T> List<T>.secondOrNull(): T? {
    return getOrNull(1)
}
```

**Solution**: Preserve type parameters in generated fake (leverage existing generic support).

### Challenge 4: Scoping

Multiple files can have the same top-level function name:

```kotlin
// StringUtils.kt
fun format(s: String): String = ...

// NumberUtils.kt
fun format(n: Int): String = ...
```

**Solution**: Namespace fakes by file name (`FakeStringUtils`, `FakeNumberUtils`).

---

## Testing Strategy

### GIVEN-WHEN-THEN Coverage

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TopLevelFunctionFakeGenerationTest {

    @Test
    fun `GIVEN file with top-level functions WHEN annotated with Fake THEN should generate fake object`() = runTest {
        // Given
        val sourceCode = """
            @file:Fake
            package com.example

            fun formatPhoneNumber(number: String): String {
                return number.replace(Regex("[^0-9]"), "")
            }
        """

        // When
        val result = compilationTestHelper.compile(sourceCode)

        // Then
        assertTrue(result.hasClass("FakeStringUtils"))
        assertTrue(result.getClass("FakeStringUtils").isObject)
        assertTrue(result.compiles())
    }

    @Test
    fun `GIVEN extension function WHEN generating fake THEN should preserve receiver type`() = runTest {
        // Given
        val sourceCode = """
            @file:Fake
            package com.example

            fun String.truncate(maxLength: Int): String {
                return if (length > maxLength) substring(0, maxLength) else this
            }
        """

        // When
        val result = compilationTestHelper.compile(sourceCode)

        // Then
        val fakeMethod = result.getClass("FakeStringUtils").getMethod("truncate")
        assertEquals("String", fakeMethod.parameters[0].type.toString()) // Receiver
        assertEquals("Int", fakeMethod.parameters[1].type.toString())    // maxLength
        assertTrue(result.compiles())
    }

    @Test
    fun `GIVEN test calling top-level function WHEN transforming THEN should rewrite to fake`() = runTest {
        // Given
        val productionCode = """
            @file:Fake
            package com.example

            fun formatPhoneNumber(number: String): String = number
        """

        val testCode = """
            package com.example.test

            fun testFormatting() {
                val result = formatPhoneNumber("123-456-7890")
            }
        """

        // When
        val result = compilationTestHelper.compileWithTest(productionCode, testCode)

        // Then
        assertTrue(result.testCodeContainsCallTo("FakeStringUtils.formatPhoneNumber"))
        assertFalse(result.testCodeContainsCallTo("StringUtilsKt.formatPhoneNumber"))
        assertTrue(result.compiles())
    }
}
```

### Performance Benchmarks

```kotlin
@Test
fun `GIVEN top-level function fake WHEN compared to mockkStatic THEN should be 100x faster`() {
    val iterations = 1000

    // Baseline: mockkStatic
    val mockkTime = measureTime {
        repeat(iterations) {
            mockkStatic("com.example.StringUtilsKt")
            every { formatPhoneNumber(any()) } returns "FAKE"
            formatPhoneNumber("123-456-7890")
            unmockkStatic("com.example.StringUtilsKt")
        }
    }

    // Fakt: compile-time fake
    val faktTime = measureTime {
        repeat(iterations) {
            configureFakeStringUtils {
                formatPhoneNumber { "FAKE" }
            }
            FakeStringUtils.formatPhoneNumber("123-456-7890")
        }
    }

    val speedup = mockkTime.inWholeMilliseconds / faktTime.inWholeMilliseconds
    assertTrue(
        speedup >= 100,
        "Expected 100x+ speedup, got ${speedup}x"
    )
}
```

---

## Metro Alignment

### Relevant Metro Patterns

1. **Static Method Handling**: Metro might have similar patterns for static factories
   - Review: Check if Metro handles static scope differently
   - Pattern: Call-site replacement for dependency injection
   - Apply: Similar approach for top-level functions

2. **File-level Analysis**: Metro processes entire modules
   - Review: How Metro analyzes file structure
   - Pattern: File facade detection
   - Apply: Detect synthetic classes for top-level functions

### Consultation Checklist

- [ ] `/consult-kotlin-api IrDeclarationOrigin.FILE_CLASS` - File facade detection
- [ ] `/validate-metro-alignment` - Check Metro's static handling
- [ ] Review how Metro handles extension receiver transformations

---

## Success Criteria

### Must Have (P0)
- ✅ Generate fakes for files with @file:Fake annotation
- ✅ Support regular top-level functions
- ✅ Support extension functions (preserve receiver)
- ✅ Rewrite calls in test code only
- ✅ **100x+ faster than mockkStatic**
- ✅ Type-safe (no string-based class names)

### Should Have (P1)
- ✅ Handle operator extensions
- ✅ Handle infix extensions
- ✅ Generic extension function support
- ✅ Clear error messages

### Nice to Have (P2)
- ⏳ Per-function @Fake annotation (instead of file-level)
- ⏳ IDE integration (autocomplete for fake config)
- ⏳ Selective fake generation (only annotated functions)

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Extension receiver transformation complex | Medium | High | Prototype early, test extensively |
| Performance claims don't materialize | Low | High | Continuous benchmarking |
| Edge cases (operator, infix) break | Medium | Medium | Comprehensive test matrix |
| String-based mockkStatic already avoided | Low | Medium | Survey shows it's still used (pain point validated) |

---

## Next Steps

1. ✅ Review this approach document
2. 🎯 Implement @file:Fake annotation support
3. 🎯 Extend analyzer for top-level functions
4. 🎯 Generate fake objects for file facades
5. 🎯 Implement call-site replacement (leverage singleton infrastructure)
6. 🎯 Handle extension functions specially
7. 🎯 Create performance benchmark suite
8. 🎯 Document migration from mockkStatic

---

## References

- **MockK Static Mocking**: [MockK Advanced Features](https://blog.kotlin-academy.com/mocking-is-not-rocket-science-mockk-advanced-features-42277e5983b5)
- **Extension Functions**: [MockK Extension Mocking](https://notwoods.github.io/mockk-guidebook/docs/mocking/extension/)
- **Phase 1 README**: [../README.md](../README.md)
- **Main Roadmap**: [../../roadmap.md](../../roadmap.md)

---

**Top-level Functions = Type-safe, 100x faster than mockkStatic.** 🚀
