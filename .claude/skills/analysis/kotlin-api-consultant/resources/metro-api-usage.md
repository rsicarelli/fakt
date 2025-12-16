# Metro API Usage Patterns

How Metro (Slack's DI framework) uses Kotlin compiler APIs - battle-tested patterns for production compiler plugins.

## Core Philosophy

**Metro's approach**:
- Two-phase FIR → IR compilation
- Context-driven generation (IrMetroContext)
- K2 compiler support (supportsK2 = true)
- Professional error handling
- Clean separation of concerns

**Fakt should follow these patterns**

## CompilerPluginRegistrar Pattern

### Metro Implementation

```kotlin
// metro/compiler/src/.../MetroCompilerPluginRegistrar.kt

class MetroCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // 1. Load options
        val options = MetroOptions.load(configuration)
        if (!options.enabled) return

        // 2. Create dependencies
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            ?: MessageCollector.NONE

        // 3. Register FIR extension (K2 detection phase)
        FirExtensionRegistrarAdapter.registerExtension(
            MetroFirExtensionRegistrar(messageCollector, options)
        )

        // 4. Register IR extension (K1 and K2 generation phase)
        IrGenerationExtension.registerExtension(
            MetroIrGenerationExtension(messageCollector, options)
        )
    }
}
```

### Key Patterns

**1. Options Loading**
```kotlin
// Create options data class
data class MetroOptions(val enabled: Boolean, ...)

// Load from CompilerConfiguration
companion object {
    fun load(configuration: CompilerConfiguration): MetroOptions {
        return MetroOptions(
            enabled = configuration.get(KEY_ENABLED, true),
            ...
        )
    }
}
```

**2. Enabled Check**
```kotlin
if (!options.enabled) return  // Early return if plugin disabled
```

**3. MessageCollector Access**
```kotlin
val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    ?: MessageCollector.NONE  // Fallback to silent collector
```

**4. K2 Support**
```kotlin
override val supportsK2: Boolean = true  // Required for Kotlin 2.x
```

### Fakt Adaptation

```kotlin
// FaktCompilerPluginRegistrar.kt
class FaktCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true  // ✅ Follow Metro

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val options = FaktOptions.load(configuration)  // ✅ Metro pattern
        if (!options.enabled) return  // ✅ Early exit

        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            ?: MessageCollector.NONE  // ✅ Safe fallback

        // FIR phase (K2 detection)
        FirExtensionRegistrarAdapter.registerExtension(
            FaktFirExtensionRegistrar(messageCollector, options)
        )

        // IR phase (K1 and K2 generation)
        IrGenerationExtension.registerExtension(
            UnifiedFaktIrGenerationExtension(messageCollector, options)
        )
    }
}
```

## IrGenerationExtension Pattern

### Metro Implementation

```kotlin
// metro/compiler/src/.../ir/MetroIrGenerationExtension.kt

class MetroIrGenerationExtension(
    private val messageCollector: MessageCollector,
    private val options: MetroOptions,
) : IrGenerationExtension {

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        // 1. Create context object
        val context = IrMetroContext(
            pluginContext = pluginContext,
            messageCollector = messageCollector,
            symbols = Symbols(pluginContext, messageCollector),
            options = options
        )

        // 2. Use context() for scoping
        context(context) {
            generateInner(moduleFragment)
        }
    }

    // 3. Separate generation logic
    context(IrMetroContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        // All generation logic here has access to context
        moduleFragment.accept(MetroIrVisitor(), null)
    }
}
```

### Key Patterns

**1. Context Object Pattern**
```kotlin
data class IrMetroContext(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
    val symbols: Symbols,
    val options: MetroOptions
) {
    // Convenience accessors
    val irFactory: IrFactory = pluginContext.irFactory
    val irBuiltIns: IrBuiltIns = pluginContext.irBuiltIns

    // Helper methods
    fun referenceClass(classId: ClassId): IrClassSymbol? =
        pluginContext.referenceClass(classId)
}
```

**2. context() Scoping**
```kotlin
context(IrMetroContext)
private fun generateXxx(...) {
    // Can access context properties directly
    val factory = irFactory
    val builtins = irBuiltIns
}
```

**3. Visitor Pattern**
```kotlin
class MetroIrVisitor : IrElementVisitorVoid() {
    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)
        // Process @Component classes
    }
}
```

### Fakt Adaptation

```kotlin
// UnifiedFaktIrGenerationExtension.kt
class UnifiedFaktIrGenerationExtension(
    private val messageCollector: MessageCollector,
    private val options: FaktOptions,
) : IrGenerationExtension {

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        // ✅ Create context (Metro pattern)
        val context = IrFaktContext(
            pluginContext = pluginContext,
            messageCollector = messageCollector,
            options = options
        )

        // ✅ Use context scoping
        context(context) {
            generateInner(moduleFragment)
        }
    }

    context(IrFaktContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        // ✅ Visitor pattern for @Fake detection
        moduleFragment.accept(FaktIrVisitor(), null)
    }
}
```

## IrPluginContext Usage

### Metro Patterns

**Symbol Resolution:**
```kotlin
context(IrMetroContext)
fun resolveType(classId: ClassId): IrClassSymbol? {
    return pluginContext.referenceClass(classId)
        ?: run {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Cannot resolve class: ${classId.asFqNameString()}"
            )
            null
        }
}
```

**Creating IR Elements:**
```kotlin
context(IrMetroContext)
fun createClass(name: Name, parent: IrDeclarationParent): IrClass {
    return irFactory.buildClass {
        this.name = name
        kind = ClassKind.CLASS
        modality = Modality.FINAL
    }.apply {
        this.parent = parent
        createImplicitParameterDeclarationWithWrappedDescriptor()
    }
}
```

### Fakt Adaptation

```kotlin
context(IrFaktContext)
fun generateFakeClass(interfaceClass: IrClass): IrClass {
    // ✅ Use irFactory from context (Metro pattern)
    return irFactory.buildClass {
        name = Name.identifier("Fake${interfaceClass.name}Impl")
        kind = ClassKind.CLASS
        modality = Modality.FINAL
        visibility = DescriptorVisibilities.PUBLIC
    }.apply {
        parent = interfaceClass.parent
        superTypes = listOf(interfaceClass.defaultType)
        createImplicitParameterDeclarationWithWrappedDescriptor()
    }
}
```

## Error Handling Patterns

### Metro Approach

**MessageCollector Usage:**
```kotlin
context(IrMetroContext)
fun reportError(message: String, irElement: IrElement? = null) {
    messageCollector.report(
        CompilerMessageSeverity.ERROR,
        message,
        irElement?.let { CompilerMessageLocationWithRange.create(it) }
    )
}

fun reportWarning(message: String) {
    messageCollector.report(
        CompilerMessageSeverity.WARNING,
        message
    )
}
```

**Graceful Failure:**
```kotlin
context(IrMetroContext)
fun processComponent(componentClass: IrClass) {
    if (!isValidComponent(componentClass)) {
        reportError(
            "Invalid @Component structure: ${componentClass.name}",
            componentClass
        )
        return  // ✅ Don't crash, just skip
    }

    // Proceed with valid component
    generateComponent(componentClass)
}
```

### Fakt Adaptation

```kotlin
context(IrFaktContext)
fun processFakeInterface(interfaceClass: IrClass) {
    // ✅ Validation with error reporting (Metro pattern)
    if (!interfaceClass.isInterface) {
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            "@Fake can only be applied to interfaces, not ${interfaceClass.kind}",
            CompilerMessageLocationWithRange.create(interfaceClass)
        )
        return
    }

    // ✅ Graceful handling of unsupported features
    if (hasUnsupportedGenerics(interfaceClass)) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "Complex generics detected in ${interfaceClass.name}. " +
            "Type safety may be reduced. See Phase 2 roadmap.",
            CompilerMessageLocationWithRange.create(interfaceClass)
        )
        // Continue with best-effort generation
    }

    generateFake(interfaceClass)
}
```

## FIR Phase Patterns

### Metro FirExtensionRegistrar

```kotlin
class MetroFirExtensionRegistrar(
    private val messageCollector: MessageCollector,
    private val options: MetroOptions
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        // Register FIR checkers
        +::MetroFirSupertypeGenerationExtension

        // Register additional extensions
        +::MetroFirDeclarationGenerationExtension
    }
}
```

### Key Pattern: Early Detection

Metro uses FIR phase to:
1. Detect annotations (@Component, @Inject, etc.)
2. Validate structure early (before IR)
3. Prepare metadata for IR phase
4. Report errors with better context

### Fakt Adaptation

```kotlin
class FaktFirExtensionRegistrar(
    private val messageCollector: MessageCollector,
    private val options: FaktOptions
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        // ✅ Register @Fake detection in FIR phase
        +::FaktFirSupertypeGenerationExtension
    }
}
```

## Testing Patterns

### Metro Test Structure

```
metro/compiler-tests/
├── src/test/kotlin/
│   └── dev/zacsweers/metro/compiler/
│       ├── MetroCompilerPluginTest.kt      # Main test suite
│       ├── fixtures/                       # Test sources
│       └── CompilerTestHelpers.kt          # Shared utilities
```

### Metro Test Pattern

```kotlin
@Test
fun `component generation test`() {
    val result = compile(
        sourceFile("Component.kt", """
            @Component
            interface AppComponent {
                fun provideString(): String
            }
        """)
    )

    result.assertSuccess()
    result.assertGeneratedFile("AppComponentImpl.kt")
    result.assertCompiles()
}
```

### Fakt Recommendation

```kotlin
// Follow Metro's comprehensive testing
@Test
fun `GIVEN interface with @Fake WHEN generating THEN should create implementation`() = runTest {
    val result = compileFaktPlugin(
        sourceFile("UserService.kt", """
            @Fake
            interface UserService {
                fun getUser(): String
            }
        """)
    )

    // ✅ Validate generation success
    result.assertSuccess()

    // ✅ Validate generated file
    result.assertGeneratedFile("FakeUserServiceImpl.kt")

    // ✅ Validate compilation
    result.assertCompiles()
}
```

## Summary: Metro Patterns to Follow

### ✅ Always Follow
1. **supportsK2 = true** - K2 compiler ready
2. **Context object pattern** - Organization and scoping
3. **context() usage** - Clean access to dependencies
4. **MessageCollector for errors** - Professional error reporting
5. **Graceful failure** - Don't crash, report and skip
6. **Two-phase FIR → IR** - Proper compilation pipeline

### ✅ Strongly Recommended
7. **Options loading pattern** - Configuration management
8. **Visitor pattern** - IR traversal
9. **Symbol resolution helpers** - Encapsulate complexity
10. **Comprehensive testing** - Compilation + generation tests

### 🎯 Fakt-Specific Adaptations
- Metro generates production DI code → Fakt generates test fakes
- Metro has complex dependency graphs → Fakt has simpler interface analysis
- Metro needs runtime performance → Fakt prioritizes developer experience
- But: **Architecture patterns remain the same**

## References

- Metro source: `/metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/`
- Fakt implementation: `fakt/compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/`
- Metro alignment docs: `.claude/docs/development/metro-alignment.md`
