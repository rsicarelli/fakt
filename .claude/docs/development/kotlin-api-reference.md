# Kotlin API Reference Guide - Compiler Source Consultation

> **Purpose**: How to consult the Kotlin compiler source for technical validation
> **Usage**: Validate APIs, understand patterns, verify compatibility

## When to Consult Kotlin Source

- **API validation** - Check if methods/classes still exist in new Kotlin versions
- **Pattern verification** - How Kotlin internally resolves similar problems
- **Type system understanding** - How generics are handled internally
- **Breaking change detection** - Deprecated APIs or changes

### Kotlin Compiler Source Structure (github.com/JetBrains/kotlin)

```
compiler/
├── ir/                          # IR system - main reference
│   ├── backend.common/          # IrGenerationExtension, extensions
│   ├── ir.tree/                 # IrElement, IrClass, IrFunction hierarchy
│   └── ir.serialization.common/ # Cross-module serialization
├── fir/                         # FIR system - frontend reference
│   ├── fir2ir/                  # FIR -> IR conversion
│   ├── tree/                    # FirElement hierarchy
│   └── checkers/                # Validation and error reporting
├── backend.common/              # Extension points we use
│   └── extensions/              # IrGenerationExtension interface
└── cli/                         # Command line processing
```

## Key API Reference

### 1. IrGenerationExtension - Main Extension Point

```kotlin
interface IrGenerationExtension {
    fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)
    fun getPlatformIntrinsicExtension(loweringContext: LoweringContext): IrIntrinsicExtension?
}
```

How to validate:
1. Check method signatures haven't changed between Kotlin versions
2. Look for new methods added
3. Understand usage patterns in Kotlin's own implementations

### 2. IrPluginContext - Core Context for IR Manipulation

```kotlin
interface IrPluginContext {
    val irFactory: IrFactory                    // Creating new IR elements
    val symbolTable: SymbolTable                // Symbol resolution
    val moduleDescriptor: ModuleDescriptor      // Module information
    fun referenceClass(classId: ClassId): IrClassSymbol?
    fun referenceFunction(callableId: CallableId): IrSimpleFunctionSymbol?
}
```

### 3. IR Tree Elements - For Code Generation

```kotlin
// Interface definition structure
interface IrClass : IrDeclarationContainer, IrSymbolOwner<IrClassSymbol> {
    val name: Name
    val kind: ClassKind           // INTERFACE, CLASS, etc.
    val isInterface: Boolean
    val superTypes: List<IrType>
}

// Function definition structure
interface IrFunction : IrDeclaration, IrSymbolOwner<IrFunctionSymbol> {
    val name: Name
    val returnType: IrType
    val valueParameters: List<IrValueParameter>
    val isSuspend: Boolean
    val typeParameters: List<IrTypeParameter>
}
```

### Generic Type System Reference

```kotlin
interface IrTypeParameter : IrDeclaration {
    val name: Name
    val index: Int
    val isReified: Boolean
    val variance: Variance
    val superTypes: List<IrType>
}
```

## API Consultation Patterns

### Pattern 1: Verify Extension Registration

Check how Kotlin registers extensions in the compiler source:
```kotlin
IrGenerationExtension.registerExtension(...)
FirExtensionRegistrarAdapter.registerExtension(...)
```

### Pattern 2: Type Resolution

How Kotlin handles generic types:
```kotlin
when (val classifier = irType.classifier) {
    is IrTypeParameterSymbol -> {
        val typeParameter = classifier.owner
        val name = typeParameter.name.asString()
    }
}
```

### Pattern 3: Code Generation

IR building patterns:
```kotlin
val generatedClass = irFactory.buildClass {
    name = Name.identifier("FakeUserServiceImpl")
    kind = ClassKind.CLASS
}.apply {
    // Post-creation setup
}
```

## APIs We Depend On (Monitor These)

1. **IrGenerationExtension interface** - `generate()` method signature, extension registration mechanism
2. **IrPluginContext APIs** - `irFactory`, `symbolTable`, reference methods
3. **IR Tree Building** - `irFactory.buildClass`, `buildFun`, type parameter handling
4. **FIR Extension System** - `FirExtensionRegistrarAdapter` usage

## Quick Reference

```kotlin
// Core IR declarations
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*

// Extension framework
import org.jetbrains.kotlin.backend.common.extensions.*

// IR building utilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.util.*
```

## External References

- [Kotlin Compiler Plugin Guide](https://kotlinlang.org/docs/compiler-plugins.html)
- [Kotlin Compiler Source (GitHub)](https://github.com/JetBrains/kotlin/tree/master/compiler)
- [Kotlin Compiler Plugin Examples](https://github.com/JetBrains/kotlin/tree/master/plugins)
