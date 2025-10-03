# Metro Alignment Guide - KtFakes Architectural Patterns

> **Purpose**: Como aplicar Metro architectural patterns no desenvolvimento do KtFakes
> **Audience**: KtFakes developers working on compiler plugin architecture
> **Metro Inspiration**: Dependency injection framework → Fake generation framework

## 🎯 **Metro Architecture Overview**

### **Metro's Two-Phase Compilation**
```kotlin
// Metro Pattern: FIR → IR pipeline
class MetroCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Phase 1: FIR - Analysis and validation
        FirExtensionRegistrarAdapter.registerExtension(MetroFirExtensionRegistrar(...))

        // Phase 2: IR - Code generation
        IrGenerationExtension.registerExtension(MetroIrGenerationExtension(...))
    }
}
```

### **KtFakes Alignment**
```kotlin
// KtFakes Implementation: Same pattern
class KtFakeCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Phase 1: FIR - @Fake detection and validation
        FirExtensionRegistrarAdapter.registerExtension(KtFakesFirExtensionRegistrar(...))

        // Phase 2: IR - Fake generation
        IrGenerationExtension.registerExtension(UnifiedKtFakesIrGenerationExtension(...))
    }
}
```

## 🏗️ **Key Metro Patterns Applicable to KtFakes**

### **1. CompilerPluginRegistrar Pattern**

**Metro Example:**
```kotlin
// metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/MetroCompilerPluginRegistrar.kt
public class MetroCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val options = MetroOptions.load(configuration)
        if (!options.enabled) return

        val classIds = ClassIds.fromOptions(options)
        val messageCollector = configuration.messageCollector

        // Register FIR and IR extensions
        FirExtensionRegistrarAdapter.registerExtension(...)
        IrGenerationExtension.registerExtension(...)
    }
}
```

**KtFakes Application:**
```kotlin
// ktfake/compiler/src/main/kotlin/dev/rsicarelli/ktfake/compiler/KtFakeCompilerPluginRegistrar.kt
public class KtFakeCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val options = KtFakeOptions.load(configuration)
        if (!options.enabled) return

        val messageCollector = configuration.messageCollector

        // Follow Metro pattern: FIR → IR
        FirExtensionRegistrarAdapter.registerExtension(KtFakesFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(UnifiedKtFakesIrGenerationExtension(...))
    }
}
```

### **2. IrGenerationExtension Pattern**

**Metro Example:**
```kotlin
// metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/ir/MetroIrGenerationExtension.kt
public class MetroIrGenerationExtension(...) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val symbols = Symbols(moduleFragment, pluginContext, classIds, options)
        val context = IrMetroContext(pluginContext, messageCollector, symbols, ...)

        context(context) { generateInner(moduleFragment) }
    }

    context(context: IrMetroContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        // Dependency injection generation logic
    }
}
```

**KtFakes Application:**
```kotlin
// ktfake/compiler/src/main/kotlin/.../UnifiedKtFakesIrGenerationExtension.kt
public class UnifiedKtFakesIrGenerationExtension(...) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // Follow Metro pattern: create context first
        val context = IrKtFakeContext(pluginContext, messageCollector, ...)

        context(context) { generateInner(moduleFragment) }
    }

    context(context: IrKtFakeContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        // Fake generation logic - inspired by Metro DI generation
        val fakeInterfaces = discoverFakeInterfaces(moduleFragment)
        fakeInterfaces.forEach { generateFakeImplementation(it) }
    }
}
```

### **3. Testing Strategy Pattern**

**Metro Testing Structure:**
```
metro/
├── compiler/src/test/        # Legacy tests
├── compiler-tests/           # Modern JetBrains testing infrastructure
│   ├── data/box/            # Full compilation and execution
│   ├── data/diagnostic/     # Error reporting validation
│   └── data/dump/           # FIR/IR tree inspection
└── samples/                 # Real-world integration
```

**KtFakes Alignment:**
```
ktfakes-prototype/
├── ktfake/compiler/src/test/         # Legacy tests (some)
├── ktfake/compiler-tests/ (future)   # JetBrains testing infrastructure
└── ktfake/test-sample/              # Real-world integration ✅
```

**Testing Pattern Application:**
```kotlin
// Follow KtFakes GIVEN-WHEN-THEN approach for compiler tests
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnifiedKtFakesIrGenerationExtensionTest {

    @Test
    fun `GIVEN interface declaration WHEN generating fake THEN should create implementation class with correct structure`() = runTest {
        // GIVEN-WHEN-THEN naming following KtFakes standards
    }

    @Test
    fun `GIVEN interface with suspend functions WHEN generating fake THEN should preserve suspend signatures`() = runTest {
        // Test real compilation like Metro box tests
    }
}
```

> **Testing Standard**: [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)
```

### **4. Options and Configuration Pattern**

**Metro Options:**
```kotlin
// metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/MetroOptions.kt
data class MetroOptions(
    val enabled: Boolean = true,
    val debug: Boolean = false,
    val enableValidation: Boolean = true,
    // ... other options
) {
    companion object {
        fun load(configuration: CompilerConfiguration): MetroOptions = ...
    }
}
```

**KtFakes Options Application:**
```kotlin
// ktfake/compiler/src/main/kotlin/.../KtFakeOptions.kt
data class KtFakeOptions(
    val enabled: Boolean = true,
    val debug: Boolean = false,
    val enableCallTracking: Boolean = false,
    val testOnly: Boolean = true,  // KtFakes-specific: only in test sources
) {
    companion object {
        fun load(configuration: CompilerConfiguration): KtFakeOptions = ...
    }
}
```

## 🔧 **Metro Patterns for Specific KtFakes Challenges**

### **Generic Type Scoping (Phase 2 Challenge)**

**How Metro Handles Complex Types:**
```kotlin
// Metro approach to complex type handling
context(context: IrMetroContext)
private fun resolveTypeKey(irType: IrType): TypeKey {
    // Metro uses sophisticated type resolution
    return when (irType) {
        is IrSimpleType -> {
            val classifier = irType.classifier
            when (classifier) {
                is IrTypeParameterSymbol -> {
                    // Metro handles generics through type substitution
                    TypeKey(classifier.owner.name.asString(), irType.qualifier)
                }
                // ... other cases
            }
        }
    }
}
```

**KtFakes Application to Generic Scoping:**
```kotlin
// Apply Metro type resolution pattern to fake generation
context(context: IrKtFakeContext)
private fun resolveGenericType(irType: IrType): String {
    return when (irType) {
        is IrSimpleType -> {
            val classifier = irType.classifier
            when (classifier) {
                is IrTypeParameterSymbol -> {
                    // Use Metro-inspired type resolution for generics
                    // This could solve our class-level vs method-level scoping issue
                    classifier.owner.name.asString()
                }
                // ... follow Metro patterns for other cases
            }
        }
    }
}
```

### **Error Handling and Diagnostics**

**Metro Diagnostic Pattern:**
```kotlin
// metro/compiler/src/main/kotlin/.../diagnostics.kt
internal fun reportError(
    element: IrElement,
    message: String,
    messageCollector: MessageCollector
) {
    messageCollector.report(
        CompilerMessageSeverity.ERROR,
        message,
        CompilerMessageSourceLocation.create(element.file, element.startOffset, element.endOffset)
    )
}
```

**KtFakes Application:**
```kotlin
// Apply Metro diagnostic patterns for better error messages
internal fun reportFakeGenerationError(
    fakeInterface: IrClass,
    issue: String,
    messageCollector: MessageCollector
) {
    messageCollector.report(
        CompilerMessageSeverity.ERROR,
        "KtFakes: Failed to generate fake for interface ${fakeInterface.name}: $issue",
        CompilerMessageSourceLocation.create(fakeInterface.file, fakeInterface.startOffset, fakeInterface.endOffset)
    )
}
```

## 📚 **Metro Code Generation Patterns**

### **Code Generation Context Pattern**

**Metro Context Usage:**
```kotlin
context(context: IrMetroContext)
private fun generateFactory(binding: IrBinding): IrClass {
    // Metro uses context for all generation
    val factoryClass = context.irFactory.buildClass {
        name = Name.identifier("${binding.typeKey.type}Factory")
        // ... factory generation
    }
    return factoryClass
}
```

**KtFakes Context Application:**
```kotlin
context(context: IrKtFakeContext)
private fun generateFakeImplementation(fakeInterface: IrClass): IrClass {
    // Follow Metro context pattern for fake generation
    val fakeClass = context.irFactory.buildClass {
        name = Name.identifier("Fake${fakeInterface.name}Impl")
        kind = ClassKind.CLASS
        // ... fake implementation generation following Metro patterns
    }
    return fakeClass
}
```

## 🎯 **Metro vs KtFakes Architectural Differences**

| Aspect | Metro (DI Framework) | KtFakes (Fake Generation) |
|--------|---------------------|----------------------------|
| **Purpose** | Dependency injection at runtime | Test fake generation at compile time |
| **Input** | `@Inject`, `@Provides` annotations | `@Fake` interface annotations |
| **Output** | Dependency graph + injection code | Fake implementations + factory functions |
| **Complexity** | Graph analysis + cycle detection | Interface analysis + code generation |
| **Runtime** | Production dependency resolution | Test-only fake instances |

### **Shared Patterns We Can Use:**
- ✅ Two-phase FIR → IR compilation
- ✅ CompilerPluginRegistrar structure
- ✅ IrGenerationExtension patterns
- ✅ Testing infrastructure approach
- ✅ Options and configuration handling
- ✅ Error reporting and diagnostics

### **KtFakes-Specific Patterns:**
- 🎯 Test-only generation validation
- 🎯 Interface-to-implementation mapping
- 🎯 Factory function + configuration DSL generation
- 🎯 Generic type scoping resolution

## 🚀 **Action Items for Metro Alignment**

### **Immediate Alignment (Phase 2):**
1. **Adopt Metro testing patterns** - Use compiler-tests/ structure
2. **Follow Metro context patterns** - IrKtFakeContext similar to IrMetroContext
3. **Apply Metro error handling** - Better diagnostic messages
4. **Use Metro type resolution** - For generic scoping challenge

### **Future Alignment (Phase 3+):**
1. **Metro-like graph analysis** - For complex fake dependencies
2. **Metro configuration patterns** - Advanced KtFakeOptions
3. **Metro incremental compilation** - Performance optimization
4. **Metro multiplatform patterns** - Cross-platform fake generation

## 📖 **References**

- **Metro Compiler Plugin:** `/metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/`
- **Metro Testing:** `/metro/compiler-tests/`
- **Metro Samples:** `/metro/samples/`
- **Kotlin Compiler APIs:** `/kotlin/compiler/`

---

**Always consult Metro patterns first before designing new KtFakes architecture. Metro is our proven, production-tested inspiration.**