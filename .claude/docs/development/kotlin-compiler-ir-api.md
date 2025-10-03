# Kotlin Compiler IR API Reference - KtFakes Development Guide

> **Purpose**: Comprehensive reference for Kotlin IR APIs used in KtFakes development
> **Version**: Based on Kotlin 2.2.10 source code
> **Scope**: IR-Native code generation for fake implementation generation
> **Testing Standard**: [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)

## 🎯 **Overview**

The Kotlin IR (Intermediate Representation) API provides the foundation for KtFakes to generate type-safe fake implementations at compile time. This guide documents the key APIs needed for our unified IR-native approach.

## 🏗️ **Core IR Architecture**

### **IR Hierarchy**
```
IrElement (base class)
├── IrStatement
│   ├── IrDeclaration
│   │   ├── IrClass             # Interface and implementation classes
│   │   ├── IrSimpleFunction    # Methods and factory functions
│   │   ├── IrProperty          # Interface properties
│   │   ├── IrField             # Backing fields for behavior storage
│   │   └── IrConstructor       # Implementation constructors
│   └── IrExpression
│       ├── IrCall              # Method calls and behavior invocations
│       ├── IrGetValue          # Variable access
│       └── IrReturn            # Return statements
└── IrTypeArgument              # Generic type parameters
```

### **KtFakes Usage Mapping**
- **IrClass**: Interface analysis and fake implementation generation
- **IrSimpleFunction**: Method signature preservation and behavior generation
- **IrProperty**: Property implementation with getter/setter generation
- **IrField**: Private behavior storage fields
- **IrCall**: Behavior invocation in method implementations

## 📦 **Essential IR Modules**

### **Key Source Directories**
```
kotlin/compiler/ir/
├── ir.tree/                    # Core IR data structures (IrClass, IrFunction)
├── backend.common/             # Extension points (IrGenerationExtension)
├── ir.psi2ir/                 # PSI to IR conversion utilities
├── ir.utils/                  # IR manipulation utilities
└── serialization.*/           # IR serialization (for debugging)
```

### **Critical Packages for KtFakes**
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

## 🔌 **Plugin Registration Framework**

### **CompilerPluginRegistrar (K2 Compatible)**
```kotlin
@ExperimentalCompilerApi
abstract class CompilerPluginRegistrar {
    abstract fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration)
    abstract val supportsK2: Boolean
}
```

**KtFakes Implementation:**
```kotlin
class KtFakeCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Phase 1: FIR extension for @Fake detection
        FirExtensionRegistrarAdapter.registerExtension(KtFakesFirExtensionRegistrar())

        // Phase 2: IR extension for fake implementation generation
        IrGenerationExtension.registerExtension(UnifiedKtFakesIrGenerationExtension())
    }
}
```

### **Extension Storage API**
```kotlin
class ExtensionStorage {
    fun <T : Any> ProjectExtensionDescriptor<T>.registerExtension(extension: T)
}
```

## 🎨 **IR Generation Extension**

### **IrGenerationExtension Interface**
```kotlin
interface IrGenerationExtension {
    companion object : ProjectExtensionDescriptor<IrGenerationExtension>(
        "org.jetbrains.kotlin.irGenerationExtension",
        IrGenerationExtension::class.java
    )

    fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)
}
```

**KtFakes Implementation Pattern:**
```kotlin
class UnifiedKtFakesIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // Create context following Metro patterns
        val context = IrKtFakeContext(pluginContext, messageCollector, ...)

        context(context) { generateInner(moduleFragment) }
    }

    context(context: IrKtFakeContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        val fakeInterfaces = discoverFakeInterfaces(moduleFragment)
        fakeInterfaces.forEach { generateFakeImplementation(it) }
    }
}
```

## 🔍 **Interface Analysis APIs**

### **IrClass Analysis**
```kotlin
// Interface detection
val isInterface: Boolean = irClass.isInterface
val isFakeAnnotated: Boolean = irClass.hasAnnotation(FqName("dev.rsicarelli.ktfake.Fake"))

// Member extraction
val methods: List<IrSimpleFunction> = irClass.declarations.filterIsInstance<IrSimpleFunction>()
val properties: List<IrProperty> = irClass.declarations.filterIsInstance<IrProperty>()

// Generic type parameters
val typeParameters: List<IrTypeParameter> = irClass.typeParameters
```

**KtFakes Interface Analysis Example:**
```kotlin
context(context: IrKtFakeContext)
private fun analyzeInterface(irClass: IrClass): InterfaceMetadata {
    require(irClass.isInterface) { "Expected interface, got ${irClass.kind}" }

    return InterfaceMetadata(
        name = irClass.name.asString(),
        packageName = irClass.packageFqName?.asString() ?: "",
        methods = irClass.declarations.filterIsInstance<IrSimpleFunction>()
            .map { analyzeMethod(it) },
        properties = irClass.declarations.filterIsInstance<IrProperty>()
            .map { analyzeProperty(it) },
        typeParameters = irClass.typeParameters.map { analyzeTypeParameter(it) }
    )
}
```

### **Method Signature Analysis**
```kotlin
context(context: IrKtFakeContext)
private fun analyzeMethod(irFunction: IrSimpleFunction): MethodMetadata {
    return MethodMetadata(
        name = irFunction.name.asString(),
        isSuspend = irFunction.isSuspend,
        typeParameters = irFunction.typeParameters.map { it.name.asString() },
        parameters = irFunction.valueParameters.map { param ->
            ParameterMetadata(
                name = param.name.asString(),
                type = irTypeToKotlinString(param.type),
                isVararg = param.varargElementType != null
            )
        },
        returnType = irTypeToKotlinString(irFunction.returnType)
    )
}
```

## 🏭 **Code Generation APIs**

### **IrFactory Usage**
```kotlin
context(context: IrKtFakeContext)
private fun generateFakeImplementation(interfaceClass: IrClass): IrClass {
    return context.irFactory.buildClass {
        name = Name.identifier("Fake${interfaceClass.name}Impl")
        kind = ClassKind.CLASS
        visibility = DescriptorVisibilities.INTERNAL
        superTypes = listOf(interfaceClass.defaultType)
    }.apply {
        // Add behavior fields
        generateBehaviorFields(interfaceClass)

        // Add method implementations
        generateMethodImplementations(interfaceClass)

        // Add configuration methods
        generateConfigurationMethods(interfaceClass)
    }
}
```

### **Method Generation**
```kotlin
context(context: IrKtFakeContext)
private fun generateMethodImplementation(
    fakeClass: IrClass,
    originalMethod: IrSimpleFunction
): IrSimpleFunction {
    return context.irFactory.buildFun {
        name = originalMethod.name
        returnType = originalMethod.returnType
        visibility = DescriptorVisibilities.PUBLIC
        modality = Modality.OPEN
        isSuspend = originalMethod.isSuspend

        // Preserve type parameters for method-level generics
        originalMethod.typeParameters.forEach { typeParam ->
            addTypeParameter(typeParam.name.asString(), typeParam.superTypes)
        }

        // Copy value parameters
        originalMethod.valueParameters.forEach { param ->
            addValueParameter(param.name.asString(), param.type)
        }
    }.apply {
        // Generate method body that calls behavior field
        body = generateMethodBody(this, originalMethod)
    }
}
```

## 🔧 **Type System APIs**

### **IrType Handling**
```kotlin
context(context: IrKtFakeContext)
private fun irTypeToKotlinString(irType: IrType): String {
    return when (irType) {
        is IrSimpleType -> {
            when (val classifier = irType.classifier) {
                is IrClassSymbol -> {
                    val kotlinClass = classifier.owner
                    buildString {
                        append(kotlinClass.name.asString())

                        // Handle generic type arguments
                        if (irType.arguments.isNotEmpty()) {
                            append("<")
                            append(irType.arguments.joinToString(", ") { arg ->
                                when (arg) {
                                    is IrTypeProjection -> irTypeToKotlinString(arg.type)
                                    else -> "*"
                                }
                            })
                            append(">")
                        }

                        // Handle nullability
                        if (irType.isNullable()) append("?")
                    }
                }
                is IrTypeParameterSymbol -> {
                    // Method-level generic: preserve parameter name
                    classifier.owner.name.asString() + if (irType.isNullable()) "?" else ""
                }
                else -> "Any" + if (irType.isNullable()) "?" else ""
            }
        }
        else -> "Any" + if (irType.isNullable()) "?" else ""
    }
}
```

### **Generic Type Parameter Handling (Phase 2 Challenge)**
```kotlin
context(context: IrKtFakeContext)
private fun handleGenericTypeParameter(typeParam: IrTypeParameter): String {
    // Phase 2A: Dynamic casting approach
    return when {
        typeParam.isMethodLevel() -> {
            // Method-level generics: preserve name but use Any? in behavior
            "Any?" // Will be cast to T in method implementation
        }
        typeParam.isClassLevel() -> {
            // Class-level generics: Phase 2B will generate generic classes
            "Any" // Current approach, future: generate FakeClass<T>
        }
        else -> "Any"
    }
}
```

## 🛠️ **Utility APIs**

### **Symbol Resolution**
```kotlin
context(context: IrKtFakeContext)
private fun resolveSymbol(fqName: String): IrClassSymbol? {
    return context.pluginContext.referenceClass(ClassId.fromString(fqName))
}

// Commonly used symbols
val anyType = context.pluginContext.irBuiltIns.anyType
val unitType = context.pluginContext.irBuiltIns.unitType
val stringType = context.pluginContext.irBuiltIns.stringType
```

### **Annotation Analysis**
```kotlin
context(context: IrKtFakeContext)
private fun hasAnnotation(irClass: IrClass, annotationFqName: String): Boolean {
    return irClass.annotations.any { annotation ->
        annotation.symbol.owner.parentAsClass.fqNameWhenAvailable?.asString() == annotationFqName
    }
}

// KtFakes usage
val isFakeAnnotated = hasAnnotation(irClass, "dev.rsicarelli.ktfake.Fake")
```

## 🧪 **Testing IR APIs**

### **Validation Testing Pattern**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrApiValidationTest {

    @Test
    fun `GIVEN IR class WHEN analyzing interface THEN should extract method metadata correctly`() = runTest {
        // Given
        val testInterface = createTestInterface("UserService") {
            method("getUser") { parameter("id", "String"); returns("User") }
            method("updateUser") { suspend(); parameter("user", "User"); returns("Boolean") }
        }
        val analyzer = InterfaceAnalyzer()

        // When
        val metadata = analyzer.analyze(testInterface)

        // Then
        assertEquals(2, metadata.methods.size)
        assertTrue(metadata.getMethod("updateUser").isSuspend)
        assertEquals("User", metadata.getMethod("getUser").returnType)
    }
}
```

## 🚨 **Common Pitfalls & Solutions**

### **Type Parameter Scoping (Core Challenge)**
```kotlin
// ❌ PROBLEM: Type parameters not accessible at class level
class FakeServiceImpl {
    private var behavior: (T) -> T = { it } // T not in scope!
}

// ✅ SOLUTION: Use Any? with dynamic casting
class FakeServiceImpl {
    private var behavior: (Any?) -> Any? = { it }

    override fun <T> method(data: T): T {
        @Suppress("UNCHECKED_CAST")
        return behavior(data) as T
    }
}
```

### **Function Type Generation**
```kotlin
// ❌ PROBLEM: Function1, Function2 artifacts
private var behavior: Function1<String, String>

// ✅ SOLUTION: Proper lambda syntax
private var behavior: (String) -> String
```

### **Import Resolution**
```kotlin
// ❌ PROBLEM: Missing imports for cross-module types
// Generated code fails to compile

// ✅ SOLUTION: Explicit import generation
context(context: IrKtFakeContext)
private fun generateImports(usedTypes: Set<String>): List<String> {
    return usedTypes.mapNotNull { typeName ->
        resolveTypeImport(typeName)
    }
}
```

## 🔗 **Related Documentation**

- **Generic Scoping Analysis**: [📋 Core Challenge](.claude/docs/analysis/generic-scoping-analysis.md)
- **Metro Alignment**: [📋 Context Patterns](.claude/docs/development/metro-alignment.md)
- **Type Safety Validation**: [📋 Testing Strategy](.claude/docs/validation/type-safety-validation.md)

## 📚 **Kotlin Compiler Source References**

### **Key Source Files**
- `/kotlin/compiler/ir/tree/src/org/jetbrains/kotlin/ir/declarations/IrClass.kt`
- `/kotlin/compiler/ir/tree/src/org/jetbrains/kotlin/ir/expressions/IrCall.kt`
- `/kotlin/compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/IrGenerationExtension.kt`
- `/kotlin/compiler/ir/ir.psi2ir/src/org/jetbrains/kotlin/psi2ir/generators/DeclarationGenerator.kt`

### **API Documentation**
- Kotlin Compiler Plugin Guide: [Official Docs](https://kotlinlang.org/docs/compiler-plugins.html)
- IR API Reference: [KotlinConf Examples](https://github.com/JetBrains/kotlin/tree/master/plugins)

---

**This guide provides the foundation for understanding and using Kotlin IR APIs in KtFakes development, with focus on the Phase 2 generic scoping challenge.**