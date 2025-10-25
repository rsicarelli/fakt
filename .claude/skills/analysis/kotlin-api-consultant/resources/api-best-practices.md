# Kotlin Compiler API Best Practices

Professional patterns for using Kotlin compiler APIs effectively and safely.

## Core Principles

### 1. Follow Metro Patterns
**Principle**: If Metro does it, it's battle-tested

✅ **Do**:
- Check Metro source first before designing
- Follow Metro's architecture patterns
- Adapt (don't blindly copy) to your use case

❌ **Don't**:
- Invent new patterns without checking Metro
- Assume your approach is better without evidence
- Skip Metro consultation for complex features

---

### 2. Context-Driven Development
**Principle**: Bundle dependencies in context objects

✅ **Do**:
```kotlin
// Create context object
data class IrFaktContext(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
    val options: FaktOptions
) {
    val irFactory = pluginContext.irFactory
    val irBuiltIns = pluginContext.irBuiltIns
}

// Use context() for scoping
context(IrFaktContext)
fun generateFake(interfaceClass: IrClass) {
    // Direct access to context properties
    val factory = irFactory
}
```

❌ **Don't**:
```kotlin
// Pass everything as parameters
fun generateFake(
    interfaceClass: IrClass,
    pluginContext: IrPluginContext,
    messageCollector: MessageCollector,
    irFactory: IrFactory,
    irBuiltIns: IrBuiltIns,
    symbolTable: SymbolTable,
    ...  // Parameter explosion
)
```

**Benefit**: Clean code, organized dependencies, testable

---

### 3. Fail Fast, Fail Gracefully
**Principle**: Validate early, report clearly, don't crash

✅ **Do**:
```kotlin
context(IrFaktContext)
fun processFakeInterface(interfaceClass: IrClass) {
    // Validate
    if (!interfaceClass.isInterface) {
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            "@Fake can only be applied to interfaces",
            CompilerMessageLocationWithRange.create(interfaceClass)
        )
        return  // Graceful exit
    }

    // Proceed
    generateFake(interfaceClass)
}
```

❌ **Don't**:
```kotlin
fun processFakeInterface(interfaceClass: IrClass) {
    // Assume it's valid
    generateFake(interfaceClass)  // Crash if not interface

    // Or generic error
    throw Exception("Error generating fake")  // Unhelpful
}
```

**Benefit**: Clear errors, no compiler crashes, better DX

---

## API-Specific Best Practices

### IrGenerationExtension

**Pattern**: Two-phase with context

```kotlin
class MyIrExtension(
    private val messageCollector: MessageCollector,
    private val options: MyOptions
) : IrGenerationExtension {

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        // Phase 1: Create context
        val context = IrMyContext(pluginContext, messageCollector, options)

        // Phase 2: Generate with context
        context(context) {
            generateInner(moduleFragment)
        }
    }

    context(IrMyContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        // All generation logic here
    }
}
```

**Why**:
- Separates setup from generation
- Clean dependency access
- Testable generation logic

---

### IrPluginContext

**Pattern**: Use helpers, don't access internals

✅ **Do**:
```kotlin
// Use provided helper methods
val stringSymbol = pluginContext.referenceClass(
    ClassId.topLevel(FqName("kotlin.String"))
)

// Use irFactory for creation
val irClass = pluginContext.irFactory.buildClass {
    name = Name.identifier("MyClass")
}
```

❌ **Don't**:
```kotlin
// Access internal symbol table directly
val symbol = pluginContext.symbolTable.referenceClass(...)  // Fragile

// Manipulate low-level structures
pluginContext.bindingContext.get(...)  // K2 incompatible
```

**Why**: Public API is stable, internals may change

---

### IrFactory

**Pattern**: Use builder DSL

✅ **Do**:
```kotlin
val irClass = irFactory.buildClass {
    name = Name.identifier("FakeUserServiceImpl")
    kind = ClassKind.CLASS
    modality = Modality.FINAL
    visibility = DescriptorVisibilities.PUBLIC
}.apply {
    parent = interfaceClass.parent
    createImplicitParameterDeclarationWithWrappedDescriptor()
}
```

❌ **Don't**:
```kotlin
// Old style (deprecated)
val irClass = irFactory.createClass(descriptor, symbol)

// Manual IR construction without builders
val irClass = IrClassImpl(...)  // Fragile, error-prone
```

**Why**: Builders handle details, are future-proof

---

### MessageCollector

**Pattern**: Structured error reporting

✅ **Do**:
```kotlin
// Severity appropriate to issue
messageCollector.report(
    CompilerMessageSeverity.ERROR,     // Error = blocking
    "Clear message: what's wrong and why",
    CompilerMessageLocationWithRange.create(irElement)  // Location
)

// Warning for non-blocking issues
messageCollector.report(
    CompilerMessageSeverity.WARNING,
    "Complex generics detected. Type safety may be reduced.",
    CompilerMessageLocationWithRange.create(irElement)
)
```

❌ **Don't**:
```kotlin
// Generic error without location
messageCollector.report(
    CompilerMessageSeverity.ERROR,
    "Error"  // Useless message
)

// Wrong severity
messageCollector.report(
    CompilerMessageSeverity.ERROR,
    "FYI: This might not work"  // Should be WARNING
)
```

**Why**: Developers need precise, actionable feedback

---

## Safety Patterns

### 1. Null-Safe Symbol Resolution

✅ **Do**:
```kotlin
context(IrFaktContext)
fun resolveType(classId: ClassId): IrClassSymbol? {
    val symbol = pluginContext.referenceClass(classId)

    if (symbol == null) {
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            "Cannot resolve class: ${classId.asFqNameString()}"
        )
        return null
    }

    return symbol
}
```

❌ **Don't**:
```kotlin
fun resolveType(classId: ClassId): IrClassSymbol {
    return pluginContext.referenceClass(classId)!!  // Crash on null
}
```

---

### 2. Defensive Copying

✅ **Do**:
```kotlin
// Create new IR elements, don't mutate existing
fun createFakeMethod(originalMethod: IrSimpleFunction): IrSimpleFunction {
    return irFactory.buildFunction {
        name = Name.identifier("fake${originalMethod.name}")
        returnType = originalMethod.returnType  // Copy properties
    }
}
```

❌ **Don't**:
```kotlin
// Mutate original IR
fun createFakeMethod(originalMethod: IrSimpleFunction): IrSimpleFunction {
    originalMethod.name = Name.identifier("fakeMethod")  // Bad!
    return originalMethod
}
```

---

### 3. Visitor Pattern Safety

✅ **Do**:
```kotlin
class FaktIrVisitor : IrElementVisitorVoid() {
    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)  // ✅ Always call super

        if (has FakeAnnotation(declaration)) {
            processFakeInterface(declaration)
        }

        // ✅ Continue traversal
    }
}
```

❌ **Don't**:
```kotlin
class FaktIrVisitor : IrElementVisitorVoid() {
    override fun visitClass(declaration: IrClass) {
        // ❌ Forgot super.visitClass()
        processFakeInterface(declaration)
        // Children not visited!
    }
}
```

---

## Performance Best Practices

### 1. Lazy Initialization

✅ **Do**:
```kotlin
context(IrFaktContext)
class Symbols(private val pluginContext: IrPluginContext) {
    val string: IrClassSymbol by lazy {
        pluginContext.referenceClass(ClassId.topLevel(FqName("kotlin.String")))!!
    }

    val unit: IrClassSymbol by lazy {
        pluginContext.referenceClass(ClassId.topLevel(FqName("kotlin.Unit")))!!
    }
}
```

❌ **Don't**:
```kotlin
class Symbols(pluginContext: IrPluginContext) {
    // Resolve everything upfront, even if not used
    val string = pluginContext.referenceClass(...)!!
    val unit = pluginContext.referenceClass(...)!!
    val int = pluginContext.referenceClass(...)!!
    // ... 50 more symbols
}
```

---

### 2. Cache Symbol Lookups

✅ **Do**:
```kotlin
context(IrFaktContext)
object SymbolCache {
    private val cache = mutableMapOf<ClassId, IrClassSymbol?>()

    fun getOrResolve(classId: ClassId): IrClassSymbol? {
        return cache.getOrPut(classId) {
            pluginContext.referenceClass(classId)
        }
    }
}
```

❌ **Don't**:
```kotlin
// Resolve same symbol repeatedly
repeat(100) {
    val stringSymbol = pluginContext.referenceClass(stringClassId)  // Wasteful
}
```

---

### 3. Batch Operations

✅ **Do**:
```kotlin
// Process all @Fake interfaces in one pass
context(IrFaktContext)
fun processModule(moduleFragment: IrModuleFragment) {
    val fakeInterfaces = mutableListOf<IrClass>()

    // Collect
    moduleFragment.accept(object : IrElementVisitorVoid() {
        override fun visitClass(declaration: IrClass) {
            super.visitClass(declaration)
            if (hasFakeAnnotation(declaration)) {
                fakeInterfaces.add(declaration)
            }
        }
    }, null)

    // Process batch
    fakeInterfaces.forEach { generateFake(it) }
}
```

❌ **Don't**:
```kotlin
// Multiple passes over IR tree
for (interface in findFakeInterfaces()) {
    moduleFragment.accept(ProcessorForInterface(interface), null)  // O(n²)
}
```

---

## Testing Best Practices

### 1. Compilation Tests

✅ **Do**:
```kotlin
@Test
fun `GIVEN interface with @Fake WHEN compiling THEN should generate fake`() {
    val result = compile("""
        @Fake
        interface UserService {
            fun getUser(): String
        }
    """)

    assertThat(result).isSuccess()
    assertThat(result.generatedFiles).contains("FakeUserServiceImpl.kt")

    // ✅ Verify generated code compiles
    val compileGenerated = compileGeneratedCode(result)
    assertThat(compileGenerated).isSuccess()
}
```

❌ **Don't**:
```kotlin
@Test
fun testGeneration() {
    // Compile but don't verify output
    compile("...")
    // Assume it worked
}
```

---

### 2. Error Message Tests

✅ **Do**:
```kotlin
@Test
fun `GIVEN @Fake on class WHEN compiling THEN should report error`() {
    val result = compile("""
        @Fake
        class UserService  // Not an interface!
    """)

    assertThat(result).isFailed()
    assertThat(result.errors).contains(
        "@Fake can only be applied to interfaces"
    )
}
```

---

## Documentation Best Practices

### 1. Document Kotlin Version Dependencies

```kotlin
/**
 * Creates a fake implementation class.
 *
 * @requires Kotlin 2.0+ (uses K2 compiler APIs)
 * @see Metro's MetroIrGenerationExtension for pattern
 */
context(IrFaktContext)
fun generateFakeClass(interfaceClass: IrClass): IrClass
```

---

### 2. Document Metro Alignment

```kotlin
/**
 * Follows Metro's context pattern for dependency organization.
 *
 * @see dev.zacsweers.metro.compiler.context.IrMetroContext
 */
data class IrFaktContext(...)
```

---

### 3. Document API Risks

```kotlin
/**
 * ⚠️ Uses @UnsafeApi IrIntrinsicExtension
 * This API may change without notice in future Kotlin versions.
 * Isolated behind FaktIntrinsics interface for protection.
 */
@UnsafeApi
private fun setupIntrinsics() { ... }
```

---

## Migration Best Practices

### From Descriptors to IR

✅ **Do**:
```kotlin
// Use IR-native APIs
val className = irClass.name.asString()
val isInterface = irClass.kind == ClassKind.INTERFACE
```

❌ **Don't**:
```kotlin
// Use deprecated descriptors
val descriptor = irClass.descriptor  // K2 incompatible
val className = descriptor.name.asString()
```

---

## Summary Checklist

**Before using any Kotlin compiler API:**

- [ ] Check if Metro uses this API (pattern reference)
- [ ] Verify API stability (no @UnsafeApi / @ExperimentalCompilerApi)
- [ ] Check K2 compatibility (no @FirIncompatiblePluginAPI)
- [ ] Use context pattern for dependencies
- [ ] Add null checks for symbol resolution
- [ ] Use MessageCollector for errors
- [ ] Test with compilation tests
- [ ] Document version requirements

**Code Review Checklist:**

- [ ] Follows Metro patterns
- [ ] Uses context() scoping
- [ ] Graceful error handling
- [ ] No descriptor usage (K2 ready)
- [ ] Builder pattern for IR creation
- [ ] Comprehensive tests
- [ ] Clear error messages

---

## Anti-Patterns to Avoid

### ❌ God Classes
```kotlin
// One class does everything
class FaktGenerator {
    fun analyze() { ... }
    fun validate() { ... }
    fun generate() { ... }
    fun write() { ... }
}
```

**Instead**: Separate concerns (InterfaceAnalyzer, IrCodeGenerator, etc.)

---

### ❌ Magic Strings
```kotlin
if (annotation.name == "Fake") { ... }  // Fragile
```

**Instead**: Use constants or ClassId
```kotlin
val FAKE_ANNOTATION = ClassId.topLevel(FqName("com.rsicarelli.fakt.Fake"))
```

---

### ❌ Silent Failures
```kotlin
try {
    generateFake(interface)
} catch (e: Exception) {
    // Swallow error
}
```

**Instead**: Report errors
```kotlin
try {
    generateFake(interface)
} catch (e: Exception) {
    messageCollector.report(ERROR, "Generation failed: ${e.message}")
}
```

---

## Resources

- **Metro Source**: Battle-tested patterns
- **Kotlin Compiler Docs**: Official API reference
- **Fakt Alignment Docs**: `.claude/docs/development/metro-alignment.md`
- **This Guide**: Living document, update with new learnings
