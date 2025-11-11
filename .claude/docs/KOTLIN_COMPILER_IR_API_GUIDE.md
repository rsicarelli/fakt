# Kotlin Compiler IR API Deep Dive Guide

> **Purpose**: Comprehensive reference for Kotlin IR APIs based on source code analysis  
> **Version**: Based on Kotlin 2.2.10 source code  
> **Scope**: Focus on IR-Native code generation for compiler plugins

## 🎯 **Overview**

The Kotlin IR (Intermediate Representation) API provides the foundation for compiler plugins to generate and manipulate Kotlin code at the IR level. This guide documents the key APIs needed for our modular IR-Native approach.

---

## 🏗️ **Core Architecture**

### **IR Hierarchy**
```
IrElement (base class)
├── IrStatement
│   ├── IrDeclaration
│   │   ├── IrClass
│   │   ├── IrSimpleFunction  
│   │   ├── IrProperty
│   │   ├── IrField
│   │   └── IrConstructor
│   └── IrExpression
│       ├── IrCall
│       ├── IrGetValue
│       └── IrReturn
└── IrTypeArgument
```

### **IR vs FIR**
- **FIR (Frontend IR)**: Used for resolution, type inference, simple desugaring
- **Backend IR**: Used for lowering, optimization, and code generation (what we use)

---

## 📦 **Core Modules**

### **Key Directories**
```
kotlin/compiler/ir/
├── ir.tree/                    # Core IR data structures
├── backend.common/             # Common backend utilities
├── ir.psi2ir/                 # PSI to IR conversion
└── serialization.*/           # IR serialization support
```

### **Essential Packages**
- `org.jetbrains.kotlin.ir.declarations` - IR declaration classes
- `org.jetbrains.kotlin.ir.builders` - IR node builders  
- `org.jetbrains.kotlin.ir.expressions` - IR expressions
- `org.jetbrains.kotlin.backend.common.extensions` - Plugin extension points

---

## 🔌 **Plugin Registration Framework**

### **CompilerPluginRegistrar (K2 Compatible)**
```kotlin
@ExperimentalCompilerApi
abstract class CompilerPluginRegistrar {
    abstract fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration)
    abstract val supportsK2: Boolean
}
```

**Usage Example**:
```kotlin
class KtFakeCompilerPlugin : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true
    
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(FaktIrGenerationExtension())
    }
}
```

### **Extension Registration**
```kotlin
class ExtensionStorage {
    fun <T : Any> ProjectExtensionDescriptor<T>.registerExtension(extension: T)
}
```

---

## 🎨 **IR Generation Extension**

### **IrGenerationExtension Interface**
```kotlin
interface IrGenerationExtension {
    companion object : ProjectExtensionDescriptor<IrGenerationExtension>(
        "org.jetbrains.kotlin.irGenerationExtension", 
        IrGenerationExtension::class.java
    )

    fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)
    fun getPlatformIntrinsicExtension(loweringContext: LoweringContext): IrIntrinsicExtension? = null
}
```

**Implementation Pattern**:
```kotlin
class FaktIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // 1. Find @Fake annotated interfaces
        val annotatedInterfaces = moduleFragment.findAnnotatedInterfaces("@Fake")
        
        // 2. Generate fake implementations
        annotatedInterfaces.forEach { interfaceClass ->
            val fakeImpl = generateFakeImplementation(interfaceClass, pluginContext)
            moduleFragment.files.first().declarations.add(fakeImpl)
        }
    }
}
```

---

## 🛠️ **IrPluginContext - Your Toolkit**

### **Core Properties**
```kotlin
interface IrPluginContext : IrGeneratorContext {
    val languageVersionSettings: LanguageVersionSettings
    val afterK2: Boolean                    // K2 compiler mode indicator
    val symbols: BuiltinSymbolsBase        // Built-in type symbols
    val platform: TargetPlatform?          // Target platform info
    val messageCollector: MessageCollector // For diagnostics
    val metadataDeclarationRegistrar: IrGeneratedDeclarationsRegistrar
}
```

### **Reference Resolution Methods**
```kotlin
// K2 Compatible - Use these!
fun referenceClass(classId: ClassId): IrClassSymbol?
fun referenceConstructors(classId: ClassId): Collection<IrConstructorSymbol>
fun referenceFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol>
fun referenceProperties(callableId: CallableId): Collection<IrPropertySymbol>

// Deprecated - Avoid in K2
@FirIncompatiblePluginAPI
fun referenceClass(fqName: FqName): IrClassSymbol?
```

### **Built-in Symbols Access**
```kotlin
val builtIns = pluginContext.irBuiltIns
val stringType = builtIns.stringType
val unitType = builtIns.unitType
val anyType = builtIns.anyType
```

---

## 🏭 **IrFactory - Creating IR Nodes**

### **Class Creation**
```kotlin
fun IrFactory.createClass(
    startOffset: Int,
    endOffset: Int, 
    origin: IrDeclarationOrigin,
    name: Name,
    visibility: DescriptorVisibility,
    symbol: IrClassSymbol,
    kind: ClassKind,
    modality: Modality,
    isExternal: Boolean = false,
    isCompanion: Boolean = false,
    isInner: Boolean = false,
    isData: Boolean = false,
    isValue: Boolean = false,
    isExpect: Boolean = false,
    isFun: Boolean = false,
    source: SourceElement = SourceElement.NO_SOURCE
): IrClass
```

**Usage Example**:
```kotlin
val fakeClass = pluginContext.irFactory.createClass(
    startOffset = SYNTHETIC_OFFSET,
    endOffset = SYNTHETIC_OFFSET,
    origin = IrDeclarationOrigin.GENERATED_CLASS,
    name = Name.identifier("Fake${interfaceName}Impl"),
    visibility = DescriptorVisibilities.PUBLIC,
    symbol = IrClassSymbolImpl(),
    kind = ClassKind.CLASS,
    modality = Modality.FINAL
)
```

### **Function Creation**
```kotlin  
fun IrFactory.createSimpleFunction(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    visibility: DescriptorVisibility,
    isInline: Boolean,
    isExpected: Boolean,
    returnType: IrType?,
    modality: Modality,
    symbol: IrSimpleFunctionSymbol,
    isTailrec: Boolean,
    isSuspend: Boolean,
    isOperator: Boolean,
    isInfix: Boolean,
    isExternal: Boolean = false
): IrSimpleFunction
```

### **Property Creation**
```kotlin
fun IrFactory.createProperty(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    visibility: DescriptorVisibility,
    modality: Modality,
    symbol: IrPropertySymbol,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExternal: Boolean = false,
    isExpect: Boolean = false
): IrProperty
```

---

## 🔧 **IR Builders - High-Level Construction**

### **Declaration Builders**
```kotlin
// Class builder
inline fun IrFactory.buildClass(builder: IrClassBuilder.() -> Unit): IrClass

// Property builder  
inline fun IrFactory.buildProperty(builder: IrPropertyBuilder.() -> Unit): IrProperty

// Function builder
inline fun IrFactory.buildFunction(builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction
```

### **Convenience Extensions**
```kotlin
// Add field to class
fun IrClass.addField(fieldName: String, fieldType: IrType, visibility: DescriptorVisibility): IrField

// Add property to class
inline fun IrClass.addProperty(builder: IrPropertyBuilder.() -> Unit): IrProperty

// Add getter to property  
inline fun IrProperty.addGetter(builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction

// Add setter to property
inline fun IrProperty.addSetter(builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction
```

### **Builder Classes**
```kotlin
class IrClassBuilder {
    var startOffset: Int = UNDEFINED_OFFSET
    var endOffset: Int = UNDEFINED_OFFSET
    var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    var name: Name = SpecialNames.NO_NAME_PROVIDED
    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
    var kind: ClassKind = ClassKind.CLASS
    var modality: Modality = Modality.FINAL
    // ... other properties
}

class IrPropertyBuilder {
    var startOffset: Int = UNDEFINED_OFFSET
    var endOffset: Int = UNDEFINED_OFFSET  
    var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    var name: Name = SpecialNames.NO_NAME_PROVIDED
    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
    var modality: Modality = Modality.FINAL
    var isVar: Boolean = false
    var isConst: Boolean = false
    var isLateinit: Boolean = false
    // ... other properties
}
```

---

## 📊 **IR Declaration Analysis**

### **IrClass Analysis**
```kotlin
abstract class IrClass : IrDeclarationBase() {
    abstract val symbol: IrClassSymbol
    abstract var kind: ClassKind               // CLASS, INTERFACE, ENUM_CLASS, etc.
    abstract var modality: Modality           // FINAL, OPEN, ABSTRACT, SEALED
    abstract var isCompanion: Boolean
    abstract var isInner: Boolean
    abstract var isData: Boolean
    abstract var isValue: Boolean
    abstract var superTypes: List<IrType>     // Implemented interfaces + superclass
    abstract var thisReceiver: IrValueParameter?
    abstract var sealedSubclasses: List<IrClassSymbol>
    
    // From IrDeclarationContainer
    override val declarations: MutableList<IrDeclaration>
    
    // From IrTypeParametersContainer  
    override val typeParameters: List<IrTypeParameter>
}
```

**Usage Example**:
```kotlin
fun analyzeInterface(interfaceClass: IrClass): InterfaceInfo {
    val properties = interfaceClass.declarations
        .filterIsInstance<IrProperty>()
        .map { analyzeProperty(it) }
        
    val functions = interfaceClass.declarations
        .filterIsInstance<IrSimpleFunction>()
        .filter { !it.isSpecialFunction() }
        .map { analyzeFunction(it) }
        
    return InterfaceInfo(
        name = interfaceClass.name.asString(),
        packageName = interfaceClass.packageFqName?.asString() ?: "",
        properties = properties,
        functions = functions,
        typeParameters = interfaceClass.typeParameters.map { analyzeTypeParameter(it) }
    )
}
```

### **IrProperty Analysis**
```kotlin
abstract class IrProperty : IrDeclarationBase() {
    abstract val symbol: IrPropertySymbol
    abstract var isVar: Boolean              // val vs var
    abstract var isConst: Boolean
    abstract var isLateinit: Boolean
    abstract var isDelegated: Boolean
    abstract var backingField: IrField?     // Backing field if any
    abstract var getter: IrSimpleFunction?  // Getter function
    abstract var setter: IrSimpleFunction?  // Setter function (only for var)
}
```

**Analysis Helper**:
```kotlin
fun analyzeProperty(property: IrProperty): PropertyInfo {
    val type = property.getter?.returnType ?: property.backingField?.type!!
    
    return PropertyInfo(
        name = property.name.asString(),
        type = type,
        isMutable = property.isVar,
        isNullable = type.isMarkedNullable(),
        hasBackingField = property.backingField != null,
        isConst = property.isConst,
        isLateinit = property.isLateinit,
        isDelegated = property.isDelegated
    )
}
```

### **IrSimpleFunction Analysis**
```kotlin
abstract class IrSimpleFunction : IrFunction() {
    abstract val symbol: IrSimpleFunctionSymbol  
    abstract var isTailrec: Boolean
    abstract var isSuspend: Boolean              // suspend function
    abstract var isOperator: Boolean             // operator function
    abstract var isInfix: Boolean                // infix function
    abstract var correspondingPropertySymbol: IrPropertySymbol? // For getters/setters
    
    // From IrFunction
    abstract var returnType: IrType
    abstract var parameters: List<IrValueParameter>  // All parameters
    abstract var body: IrBody?
}
```

**Analysis Helper**:
```kotlin
fun analyzeFunction(function: IrSimpleFunction): FunctionInfo {
    return FunctionInfo(
        name = function.name.asString(),
        returnType = function.returnType,
        parameters = function.parameters
            .filter { it.kind == IrParameterKind.Regular }
            .map { analyzeParameter(it) },
        typeParameters = function.typeParameters.map { analyzeTypeParameter(it) },
        isSuspend = function.isSuspend,
        isInline = function.isInline,
        isOperator = function.isOperator,
        isInfix = function.isInfix,
        modality = function.modality,
        visibility = function.visibility
    )
}
```

---

## 🎭 **Code Generation Patterns**

### **Complete Class Generation**
```kotlin
fun generateFakeClass(interfaceClass: IrClass, pluginContext: IrPluginContext): IrClass {
    return pluginContext.irFactory.buildClass {
        name = Name.identifier("Fake${interfaceClass.name}Impl")
        kind = ClassKind.CLASS
        modality = Modality.FINAL
        visibility = DescriptorVisibilities.PUBLIC
        
        // Set up inheritance
        superTypes = listOf(interfaceClass.defaultType)
        
        // Generate backing fields for properties
        interfaceClass.declarations.filterIsInstance<IrProperty>().forEach { property ->
            generatePropertyImplementation(this@buildClass, property)
        }
        
        // Generate function implementations
        interfaceClass.declarations.filterIsInstance<IrSimpleFunction>()
            .filter { !it.isSpecialFunction() }
            .forEach { function ->
                generateFunctionImplementation(this@buildClass, function, pluginContext)
            }
    }.also { fakeClass ->
        fakeClass.parent = interfaceClass.parent
    }
}
```

### **Property Implementation Generation**
```kotlin
fun generatePropertyImplementation(targetClass: IrClass, sourceProperty: IrProperty) {
    targetClass.addProperty {
        name = sourceProperty.name
        isVar = sourceProperty.isVar
        visibility = DescriptorVisibilities.PUBLIC
        modality = Modality.OPEN
        
        // Add backing field
        backingField = targetClass.addField(
            "_${sourceProperty.name}", 
            sourceProperty.getter?.returnType!!
        )
        
        // Add getter
        addGetter {
            returnType = sourceProperty.getter?.returnType!!
            body = createReturnStatement(
                IrGetFieldImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    backingField!!.symbol,
                    backingField!!.type
                )
            )
        }
        
        // Add setter if mutable
        if (sourceProperty.isVar) {
            addSetter {
                parameters = listOf(
                    buildValueParameter {
                        name = Name.identifier("value")
                        type = sourceProperty.getter?.returnType!!
                    }
                )
                returnType = pluginContext.irBuiltIns.unitType
                body = createSetterBody(backingField!!, parameters.first())
            }
        }
    }
}
```

### **Function Implementation Generation**
```kotlin
fun generateFunctionImplementation(
    targetClass: IrClass, 
    sourceFunction: IrSimpleFunction,
    pluginContext: IrPluginContext
): IrSimpleFunction {
    return pluginContext.irFactory.buildFunction {
        name = sourceFunction.name
        returnType = sourceFunction.returnType
        modality = Modality.OPEN
        visibility = DescriptorVisibilities.PUBLIC
        isSuspend = sourceFunction.isSuspend
        isInline = sourceFunction.isInline
        isOperator = sourceFunction.isOperator
        isInfix = sourceFunction.isInfix
        
        // Copy parameters
        parameters = sourceFunction.parameters.map { param ->
            buildValueParameter {
                name = param.name
                type = param.type
                kind = param.kind
                index = param.index
                hasDefaultValue = param.hasDefaultValue()
            }
        }
        
        // Generate default return
        body = createDefaultReturnBody(sourceFunction.returnType, pluginContext)
        
    }.also { impl ->
        impl.parent = targetClass
        targetClass.declarations.add(impl)
    }
}
```

### **Default Value Generation**
```kotlin
fun generateDefaultValue(type: IrType, pluginContext: IrPluginContext): IrExpression {
    val builtIns = pluginContext.irBuiltIns
    
    return when {
        type == builtIns.stringType -> IrConstImpl.string(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, type, "")
        type == builtIns.intType -> IrConstImpl.int(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, type, 0)
        type == builtIns.booleanType -> IrConstImpl.boolean(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, type, false)
        type == builtIns.unitType -> IrGetObjectValueImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, type, builtIns.unitClass
        )
        type.isNullable() -> IrConstImpl.constNull(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, type)
        else -> {
            // For complex types, try to create instance or return null
            generateComplexTypeDefault(type, pluginContext)
        }
    }
}
```

---

## 🧪 **Advanced Patterns**

### **Type Parameter Handling**
```kotlin
fun analyzeTypeParameters(irClass: IrClass): List<TypeParameterInfo> {
    return irClass.typeParameters.map { typeParam ->
        TypeParameterInfo(
            name = typeParam.name.asString(),
            bounds = typeParam.superTypes.map { it.render() },
            variance = typeParam.variance,
            isReified = typeParam.isReified
        )
    }
}

fun generateGenericFakeClass(interfaceClass: IrClass, pluginContext: IrPluginContext): IrClass {
    return pluginContext.irFactory.buildClass {
        // Copy type parameters
        typeParameters = interfaceClass.typeParameters.map { sourceTypeParam ->
            buildTypeParameter {
                name = sourceTypeParam.name
                bounds = sourceTypeParam.superTypes
                variance = sourceTypeParam.variance
                isReified = sourceTypeParam.isReified
            }
        }
        
        // Rest of class generation...
    }
}
```

### **Suspend Function Handling**
```kotlin
fun generateSuspendFunction(sourceFunction: IrSimpleFunction, pluginContext: IrPluginContext): IrSimpleFunction {
    return pluginContext.irFactory.buildFunction {
        isSuspend = true
        returnType = sourceFunction.returnType
        
        // Suspend functions need special handling for return types
        body = when {
            sourceFunction.returnType == pluginContext.irBuiltIns.unitType -> {
                // For suspend Unit functions, return Unit
                createReturnStatement(
                    IrGetObjectValueImpl(
                        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                        pluginContext.irBuiltIns.unitType,
                        pluginContext.irBuiltIns.unitClass
                    )
                )
            }
            else -> {
                // For other suspend functions, return appropriate default
                createReturnStatement(generateDefaultValue(sourceFunction.returnType, pluginContext))
            }
        }
    }
}
```

### **Error Handling and Diagnostics**
```kotlin
fun reportError(pluginContext: IrPluginContext, message: String, element: IrElement? = null) {
    pluginContext.messageCollector.report(
        CompilerMessageSeverity.ERROR, 
        message,
        element?.let { 
            CompilerMessageLocationWithRange.create(
                it.file.path,
                it.startOffset,
                it.endOffset
            )
        }
    )
}

fun validateInterface(interfaceClass: IrClass, pluginContext: IrPluginContext): Boolean {
    if (interfaceClass.kind != ClassKind.INTERFACE) {
        reportError(pluginContext, "@Fake can only be applied to interfaces", interfaceClass)
        return false
    }
    
    if (interfaceClass.typeParameters.any { it.isReified }) {
        reportError(pluginContext, "Cannot fake interfaces with reified type parameters", interfaceClass)
        return false
    }
    
    return true
}
```

---

## 📚 **Best Practices**

### **Memory Management**
- Use `SYNTHETIC_OFFSET` for generated elements
- Set parent relationships correctly: `generatedElement.parent = parentElement`
- Use symbols from `IrPluginContext` when possible

### **K2 Compatibility**
- Avoid `@FirIncompatiblePluginAPI` methods
- Use `ClassId` and `CallableId` instead of `FqName` 
- Check `pluginContext.afterK2` for K2-specific behavior

### **Error Handling**
- Always validate input before generation
- Use `MessageCollector` for diagnostic messages
- Provide helpful error messages with source locations

### **Performance**
- Cache symbol lookups
- Avoid unnecessary IR traversals
- Use builders for complex node creation

---

## 🎯 **Implementation Roadmap**

### **Phase 1: Basic Structure**
1. Set up `CompilerPluginRegistrar`
2. Implement `IrGenerationExtension` 
3. Basic class and function generation

### **Phase 2: Complete Implementation** 
1. Property handling with getters/setters
2. Type parameter support  
3. Suspend function support
4. Generic type handling

### **Phase 3: Advanced Features**
1. Complex type defaults
2. Error handling and validation
3. IDE integration support
4. Performance optimization

---

## 🔗 **Key Files Reference**

| Component | File Location |
|-----------|---------------|
| **Core IR Types** | `ir.tree/src/org/jetbrains/kotlin/ir/declarations/` |
| **Plugin Framework** | `plugin-api/src/org/jetbrains/kotlin/compiler/plugin/` |
| **IR Factory** | `ir.tree/src/org/jetbrains/kotlin/ir/declarations/IrFactory.kt` |
| **IR Builders** | `ir.tree/src/org/jetbrains/kotlin/ir/builders/` |
| **Extension Points** | `backend.common/src/org/jetbrains/kotlin/backend/common/extensions/` |

---

## 💡 **Ready for Implementation**

This API guide provides the foundation for implementing our modular IR-Native code generation approach. The APIs are well-structured, type-safe, and provide all the capabilities needed for dynamic interface analysis and fake implementation generation.

**Next Steps**: Use this guide to implement the modular architecture outlined in `IR_NATIVE_DEMO.md`.