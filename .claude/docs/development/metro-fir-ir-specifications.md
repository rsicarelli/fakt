# Metro FIR + IR Architecture - KtFakes Implementation Reference

> **Purpose**: Metro's two-phase FIR + IR architecture as reference for KtFakes implementation
> **Source**: Metro framework technical specifications
> **Application**: Guide KtFakes architectural decisions and patterns
> **Testing Standard**: [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)

## 🎯 **Executive Summary**

Metro is a compile-time dependency injection framework that leverages Kotlin's K2 compiler architecture through **FIR (Frontend IR)** for analysis/validation and **IR (Intermediate Representation)** for code generation. This approach provides significant performance advantages and direct compiler integration - exactly what KtFakes needs for fake generation.

## 🏗️ **Metro's Two-Phase Architecture**

### **Phase Overview**
```
┌─────────────────────────────────────────────────────────────┐
│                    Metro Compiler Plugin                    │
├─────────────────────────────────────────────────────────────┤
│  Phase 1: FIR (Frontend IR) - Analysis & Validation        │
│  ├─ Declaration Generation                                  │
│  ├─ Supertype Generation                                    │
│  ├─ Type Analysis & Validation                              │
│  ├─ Diagnostic Reporting                                    │
│  └─ Metadata Preparation                                    │
├─────────────────────────────────────────────────────────────┤
│  Phase 2: IR (Intermediate Representation) - Code Gen      │
│  ├─ Dependency Graph Construction                           │
│  ├─ Binding Resolution                                      │
│  ├─ Factory Generation                                      │
│  ├─ Implementation Synthesis                                │
│  └─ Runtime Code Emission                                   │
└─────────────────────────────────────────────────────────────┘
```

### **KtFakes Application**
```
┌─────────────────────────────────────────────────────────────┐
│                   KtFakes Compiler Plugin                   │
├─────────────────────────────────────────────────────────────┤
│  Phase 1: FIR - @Fake Detection & Validation               │
│  ├─ @Fake Annotation Detection                             │
│  ├─ Interface Validation                                    │
│  ├─ Type Parameter Analysis                                 │
│  ├─ Diagnostic Reporting                                    │
│  └─ Metadata Preparation                                    │
├─────────────────────────────────────────────────────────────┤
│  Phase 2: IR - Fake Implementation Generation              │
│  ├─ Interface Analysis                                      │
│  ├─ Fake Class Generation                                   │
│  ├─ Factory Function Generation                             │
│  ├─ Configuration DSL Generation                            │
│  └─ Type-Safe Code Emission                                 │
└─────────────────────────────────────────────────────────────┘
```

## 🔌 **Plugin Registration Pattern**

### **Metro's CompilerPluginRegistrar**
```kotlin
public class MetroCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Phase 1: FIR Extensions Registration
        FirExtensionRegistrarAdapter.registerExtension(
            MetroFirExtensionRegistrar(classIds, options)
        )

        // Phase 2: IR Generation Extension Registration
        IrGenerationExtension.registerExtension(
            MetroIrGenerationExtension(
                messageCollector, classIds, options, lookupTracker, expectActualTracker
            )
        )
    }
}
```

### **KtFakes Implementation Following Metro Pattern**
```kotlin
public class KtFakeCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val options = KtFakeOptions.load(configuration)
        if (!options.enabled) return

        val messageCollector = configuration.messageCollector

        // Phase 1: FIR - @Fake detection and validation
        FirExtensionRegistrarAdapter.registerExtension(
            KtFakesFirExtensionRegistrar(options, messageCollector)
        )

        // Phase 2: IR - Fake implementation generation
        IrGenerationExtension.registerExtension(
            UnifiedKtFakesIrGenerationExtension(messageCollector, options)
        )
    }
}
```

## 📋 **Phase 1: FIR Integration Patterns**

### **Metro's FIR Extension Registrar**
```kotlin
public class MetroFirExtensionRegistrar(
    private val classIds: ClassIds,
    private val options: MetroOptions,
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        // Built-in types and annotations support
        +MetroFirBuiltIns.getFactory(classIds, options)

        // Compile-time validation and error checking
        +::MetroFirCheckers

        // Supertype generation for factory classes
        +supertypeGenerator("Supertypes - graph factory", ::GraphFactoryFirSupertypeGenerator)

        // Declaration generation for various Metro components
        +declarationGenerator("FirGen - InjectedClass", ::InjectedClassFirGenerator)
        +declarationGenerator("FirGen - AssistedFactory", ::AssistedFactoryFirGenerator)
    }
}
```

### **KtFakes FIR Extension (Following Metro Pattern)**
```kotlin
public class KtFakesFirExtensionRegistrar(
    private val options: KtFakeOptions,
    private val messageCollector: MessageCollector
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        // Built-in types for @Fake annotation
        +KtFakesFirBuiltIns.getFactory(options)

        // Compile-time validation of @Fake interfaces
        +::KtFakesFirCheckers

        // Declaration generation for fake implementations
        +declarationGenerator("FirGen - FakeImplementation", ::FakeImplementationFirGenerator)
        +declarationGenerator("FirGen - FactoryFunction", ::FactoryFunctionFirGenerator)
        +declarationGenerator("FirGen - ConfigurationDSL", ::ConfigurationDslFirGenerator)
    }
}
```

### **FIR Validation Pattern**
```kotlin
// Metro pattern for compile-time validation
class MetroFirCheckers : FirCheckers() {
    override val declarationCheckers = setOf(
        MetroComponentChecker,
        MetroScopeChecker,
        MetroCircularDependencyChecker
    )
}

// KtFakes implementation following Metro pattern
class KtFakesFirCheckers : FirCheckers() {
    override val declarationCheckers = setOf(
        FakeInterfaceChecker,        // Validate @Fake interfaces
        GenericConstraintChecker,    // Validate generic type constraints
        SuspendFunctionChecker       // Validate suspend function usage
    )
}
```

## 🎨 **Phase 2: IR Generation Patterns**

### **Metro's IR Generation Extension**
```kotlin
public class MetroIrGenerationExtension(...) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val symbols = Symbols(moduleFragment, pluginContext, classIds, options)
        val context = IrMetroContext(pluginContext, messageCollector, symbols, ...)

        context(context) { generateInner(moduleFragment) }
    }

    context(context: IrMetroContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        // Metro-specific dependency injection generation logic
        val components = discoverComponents(moduleFragment)
        components.forEach { generateComponentImplementation(it) }
    }
}
```

### **KtFakes IR Generation (Following Metro Pattern)**
```kotlin
public class UnifiedKtFakesIrGenerationExtension(...) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // Follow Metro pattern: create context first
        val context = IrKtFakeContext(pluginContext, messageCollector, options)

        context(context) { generateInner(moduleFragment) }
    }

    context(context: IrKtFakeContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        // KtFakes-specific fake generation logic
        val fakeInterfaces = discoverFakeInterfaces(moduleFragment)
        fakeInterfaces.forEach { generateFakeImplementation(it) }
    }
}
```

### **Metro Context Pattern**
```kotlin
data class IrMetroContext(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
    val symbols: Symbols,
    val options: MetroOptions,
    // ... other Metro-specific context data
) {
    // Context-specific functionality for dependency injection
    fun resolveBinding(type: IrType): Binding? = ...
    fun createFactory(binding: Binding): IrClass = ...
}
```

### **KtFakes Context (Following Metro Pattern)**
```kotlin
data class IrKtFakeContext(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
    val options: KtFakeOptions,
    // KtFakes-specific context data
) {
    // Context-specific functionality for fake generation
    val irFactory: IrFactory get() = pluginContext.irFactory
    fun resolveType(irType: IrType): String = irTypeToKotlinString(irType)
    fun generateDefaultValue(irType: IrType): String = ...
}
```

## 🔧 **Metro Code Generation Patterns**

### **Factory Generation Pattern**
```kotlin
context(context: IrMetroContext)
private fun generateFactory(binding: IrBinding): IrClass {
    return context.irFactory.buildClass {
        name = Name.identifier("${binding.typeKey.type}Factory")
        kind = ClassKind.CLASS
        visibility = DescriptorVisibilities.PUBLIC
    }.apply {
        // Add factory implementation
        addConstructor()
        addCreateMethod(binding)
    }
}
```

### **KtFakes Implementation Generation (Following Metro)**
```kotlin
context(context: IrKtFakeContext)
private fun generateFakeImplementation(fakeInterface: IrClass): IrClass {
    return context.irFactory.buildClass {
        name = Name.identifier("Fake${fakeInterface.name}Impl")
        kind = ClassKind.CLASS
        visibility = DescriptorVisibilities.INTERNAL
        superTypes = listOf(fakeInterface.defaultType)
    }.apply {
        // Add behavior fields
        generateBehaviorFields(fakeInterface)
        // Add method implementations
        generateMethodImplementations(fakeInterface)
        // Add configuration methods
        generateConfigurationMethods(fakeInterface)
    }
}
```

## 🚨 **Metro Error Handling Patterns**

### **Diagnostic Reporting**
```kotlin
// Metro diagnostic pattern
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

### **KtFakes Error Handling (Following Metro)**
```kotlin
context(context: IrKtFakeContext)
internal fun reportFakeGenerationError(
    fakeInterface: IrClass,
    issue: String
) {
    context.messageCollector.report(
        CompilerMessageSeverity.ERROR,
        "KtFakes: Failed to generate fake for interface ${fakeInterface.name}: $issue",
        CompilerMessageSourceLocation.create(
            fakeInterface.file,
            fakeInterface.startOffset,
            fakeInterface.endOffset
        )
    )
}
```

## 📊 **Metro vs KtFakes Comparison**

| Aspect | Metro (DI Framework) | KtFakes (Fake Generation) |
|--------|---------------------|----------------------------|
| **Purpose** | Dependency injection at runtime | Test fake generation at compile time |
| **Input** | `@Inject`, `@Provides` annotations | `@Fake` interface annotations |
| **Output** | Dependency graph + injection code | Fake implementations + factory functions |
| **FIR Phase** | Component validation + factory declarations | Interface validation + fake declarations |
| **IR Phase** | Runtime injection code generation | Test-time fake implementation generation |
| **Context** | `IrMetroContext` with DI-specific data | `IrKtFakeContext` with fake-specific data |

### **Shared Patterns KtFakes Can Use**
- ✅ Two-phase FIR → IR compilation approach
- ✅ Context-based IR generation with `context()` functions
- ✅ CompilerPluginRegistrar structure with K2 support
- ✅ Error reporting and diagnostic patterns
- ✅ Factory generation patterns (adapted for fake factories)
- ✅ Configuration and options handling

### **KtFakes-Specific Adaptations**
- 🎯 Test-only generation validation (not production runtime)
- 🎯 Interface-to-implementation mapping (not dependency graphs)
- 🎯 Factory function + configuration DSL generation
- 🎯 Generic type scoping resolution (Metro doesn't face this challenge)

## 🔗 **KtFakes Implementation Benefits from Metro Patterns**

### **Immediate Applications**
1. **Context Pattern**: `IrKtFakeContext` following `IrMetroContext` design
2. **Error Handling**: Metro's diagnostic reporting patterns
3. **Two-Phase Architecture**: FIR validation + IR generation
4. **Extension Registration**: Proper K2 compiler integration

### **Phase 2 Applications**
1. **Generic Type Resolution**: Apply Metro's type resolution patterns
2. **Configuration System**: Metro's options handling approach
3. **Incremental Compilation**: Metro's lookup tracker integration
4. **Performance Optimization**: Metro's efficient IR generation

## 🔗 **Related Documentation**

- **Metro Alignment Guide**: [📋 Architecture Patterns](.claude/docs/development/metro-alignment.md)
- **Kotlin IR API Reference**: [📋 IR APIs](.claude/docs/development/kotlin-compiler-ir-api.md)
- **Generic Scoping Analysis**: [📋 Core Challenge](.claude/docs/analysis/generic-scoping-analysis.md)

## 📚 **Metro Source References**

### **Key Metro Files**
- `MetroCompilerPluginRegistrar.kt` - Plugin registration
- `MetroFirExtensionRegistrar.kt` - FIR phase configuration
- `MetroIrGenerationExtension.kt` - IR generation implementation
- `IrMetroContext.kt` - Context pattern implementation

### **External Resources**
- Metro GitHub: [Metro Framework](https://github.com/ZacSweers/MetroEvangelism)
- Kotlin Compiler: [K2 Architecture](https://kotlinlang.org/docs/k2-compiler.html)

---

**Metro's FIR + IR architecture provides the proven foundation for KtFakes' two-phase approach to fake generation, ensuring K2 compatibility and optimal performance.**