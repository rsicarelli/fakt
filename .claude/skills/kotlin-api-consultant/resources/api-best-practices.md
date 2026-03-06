# Kotlin Compiler API Best Practices

Patterns for using Kotlin compiler APIs effectively and safely.

## Core Principles

### 1. Context-Driven Development

Bundle dependencies in context objects instead of parameter explosion.

```kotlin
// Good: context object
data class IrFaktContext(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
    val options: FaktOptions
) {
    val irFactory = pluginContext.irFactory
    val irBuiltIns = pluginContext.irBuiltIns
}

context(IrFaktContext)
fun generateFake(interfaceClass: IrClass) {
    val factory = irFactory // direct access
}
```

### 2. Fail Fast, Fail Gracefully

Validate early, report clearly, don't crash.

```kotlin
context(IrFaktContext)
fun processFakeInterface(interfaceClass: IrClass) {
    if (!interfaceClass.isInterface) {
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            "@Fake can only be applied to interfaces",
            CompilerMessageLocationWithRange.create(interfaceClass)
        )
        return
    }
    generateFake(interfaceClass)
}
```

## API-Specific Patterns

### IrGenerationExtension

```kotlin
class MyIrExtension(
    private val messageCollector: MessageCollector,
    private val options: MyOptions
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val context = IrMyContext(pluginContext, messageCollector, options)
        context(context) { generateInner(moduleFragment) }
    }

    context(IrMyContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        // All generation logic here
    }
}
```

### IrPluginContext — Use Helpers, Not Internals

```kotlin
// Good: use provided helper methods
val stringSymbol = pluginContext.referenceClass(
    ClassId.topLevel(FqName("kotlin.String"))
)
val irClass = pluginContext.irFactory.buildClass {
    name = Name.identifier("MyClass")
}

// Bad: access internal symbol table directly
val symbol = pluginContext.symbolTable.referenceClass(...)  // Fragile
pluginContext.bindingContext.get(...)  // K2 incompatible
```

### IrFactory — Use Builder DSL

```kotlin
// Good: builder DSL
val irClass = irFactory.buildClass {
    name = Name.identifier("FakeUserServiceImpl")
    kind = ClassKind.CLASS
    modality = Modality.FINAL
    visibility = DescriptorVisibilities.PUBLIC
}.apply {
    parent = interfaceClass.parent
    createImplicitParameterDeclarationWithWrappedDescriptor()
}

// Bad: manual construction
val irClass = IrClassImpl(...)  // Fragile, error-prone
```

### MessageCollector — Structured Error Reporting

```kotlin
// Good: severity + location
messageCollector.report(
    CompilerMessageSeverity.ERROR,
    "Clear message: what's wrong and why",
    CompilerMessageLocationWithRange.create(irElement)
)

// Bad: no location, wrong severity
messageCollector.report(CompilerMessageSeverity.ERROR, "Error")
```

## Safety Patterns

### Null-Safe Symbol Resolution

```kotlin
context(IrFaktContext)
fun resolveType(classId: ClassId): IrClassSymbol? {
    val symbol = pluginContext.referenceClass(classId)
    if (symbol == null) {
        messageCollector.report(ERROR, "Cannot resolve class: ${classId.asFqNameString()}")
        return null
    }
    return symbol
}
```

### Defensive Copying

```kotlin
// Good: create new IR elements
fun createFakeMethod(originalMethod: IrSimpleFunction): IrSimpleFunction {
    return irFactory.buildFunction {
        name = Name.identifier("fake${originalMethod.name}")
        returnType = originalMethod.returnType
    }
}

// Bad: mutate original IR
originalMethod.name = Name.identifier("fakeMethod")  // Never do this
```

## Performance Patterns

### Lazy Initialization

```kotlin
class Symbols(private val pluginContext: IrPluginContext) {
    val string: IrClassSymbol by lazy {
        pluginContext.referenceClass(ClassId.topLevel(FqName("kotlin.String")))!!
    }
}
```

### Batch Operations

```kotlin
// Good: collect then process
context(IrFaktContext)
fun processModule(moduleFragment: IrModuleFragment) {
    val fakeInterfaces = mutableListOf<IrClass>()
    moduleFragment.accept(object : IrElementVisitorVoid() {
        override fun visitClass(declaration: IrClass) {
            super.visitClass(declaration)
            if (hasFakeAnnotation(declaration)) fakeInterfaces.add(declaration)
        }
    }, null)
    fakeInterfaces.forEach { generateFake(it) }
}
```

## Anti-Patterns to Avoid

- **God Classes**: Separate concerns (InterfaceAnalyzer, CodeGenerator, etc.)
- **Magic Strings**: Use `ClassId.topLevel(FqName(...))` instead of raw string comparison
- **Silent Failures**: Always report errors via MessageCollector
- **Descriptor Usage**: Use IR-native APIs (`irClass.name` not `irClass.descriptor.name`) for K2 compatibility

## Checklist

Before using any Kotlin compiler API:
- [ ] Verify API stability (no @UnsafeApi / @ExperimentalCompilerApi)
- [ ] Check K2 compatibility (no @FirIncompatiblePluginAPI)
- [ ] Use context pattern for dependencies
- [ ] Add null checks for symbol resolution
- [ ] Use MessageCollector for errors
- [ ] Use builder pattern for IR creation
- [ ] No descriptor usage (K2 ready)
