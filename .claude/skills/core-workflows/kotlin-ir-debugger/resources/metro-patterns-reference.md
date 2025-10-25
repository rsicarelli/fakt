# Metro Patterns Reference

> **Loaded on-demand** for Metro architectural alignment validation

## Metro's IrGenerationExtension Pattern

### Core Architecture

Metro uses a two-phase FIR → IR compilation approach that Fakt emulates:

```kotlin
// Metro pattern:
class MetroIrGenerationExtension : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        // Context-driven generation
        val metroContext = IrMetroContext(pluginContext)

        // Process declarations
        moduleFragment.files.forEach { file ->
            file.transformDeclarations(metroContext)
        }
    }
}

// Fakt equivalent:
class UnifiedFaktIrGenerationExtension : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        // Context-driven generation (Metro-inspired)
        val faktContext = IrFaktContext(pluginContext)

        // Process @Fake annotated interfaces
        moduleFragment.files.forEach { file ->
            file.transformFakeInterfaces(faktContext)
        }
    }
}
```

## Context Pattern

### Metro's Context

```kotlin
class IrMetroContext(
    val pluginContext: IrPluginContext
) {
    val irFactory = pluginContext.irFactory
    val irBuiltIns = pluginContext.irBuiltIns

    // Diagnostic messaging
    fun reportError(message: String, location: IrElement)
}
```

### Fakt's Context (Metro-Aligned)

```kotlin
class IrFaktContext(
    val pluginContext: IrPluginContext
) {
    val irFactory = pluginContext.irFactory
    val irBuiltIns = pluginContext.irBuiltIns

    // Metro-style diagnostic reporting
    fun reportError(message: String, location: IrElement) {
        // Similar error handling pattern
    }
}
```

## Class Generation Pattern

### Metro Style

```kotlin
context(IrMetroContext)
private fun generateComponent(
    annotatedClass: IrClass
): IrClass {
    return irFactory.buildClass {
        name = Name.identifier("Generated${annotatedClass.name}")
        kind = ClassKind.CLASS
        modality = Modality.FINAL
    }.apply {
        // Metro's structured class building
        createImplicitParameterDeclarationWithWrappedDescriptor()
        addConstructor { /* ... */ }
        addMethods { /* ... */ }
    }
}
```

### Fakt Alignment

```kotlin
context(IrFaktContext)
private fun generateFakeImpl(
    fakeInterface: IrClass
): IrClass {
    return irFactory.buildClass {
        name = Name.identifier("Fake${fakeInterface.name}Impl")
        kind = ClassKind.CLASS
        modality = Modality.FINAL
    }.apply {
        // Metro-inspired class building
        createImplicitParameterDeclarationWithWrappedDescriptor()
        addConstructor { /* ... */ }
        implementInterfaceMethods { /* ... */ }
    }
}
```

## Error Handling Pattern

### Metro's Diagnostic Approach

```kotlin
// Metro reports errors with clear context
if (!isValidComponent(element)) {
    context.reportError(
        "Component must be an interface or abstract class",
        element
    )
    return // Graceful failure
}
```

### Fakt Alignment

```kotlin
// Fakt follows Metro's diagnostic pattern
if (!hasValidFakeAnnotation(element)) {
    context.reportError(
        "@Fake annotation must be applied to interfaces only",
        element
    )
    return // Graceful failure
}
```

## API Compatibility Checks

### Metro's Approach

Metro validates Kotlin APIs at runtime to ensure compatibility:

```kotlin
// Check API availability
private fun validateApis() {
    check(::IrFactory.isInitialized) { "IrFactory not available" }
    check(::IrPluginContext.isInitialized) { "Plugin context missing" }
}
```

### Fakt Should Follow

```kotlin
// Validate Kotlin compiler APIs
private fun validateKotlinApis() {
    check(pluginContext.irFactory != null) { "IrFactory unavailable" }
    check(pluginContext.irBuiltIns != null) { "IrBuiltIns unavailable" }
}
```

## Metro Testing Patterns

Metro uses comprehensive compiler-tests/ structure:

```
metro/compiler-tests/
├── src/
│   ├── main/        # Test fixtures
│   └── test/        # Compilation tests
```

Fakt should align:
```
ktfake/compiler/src/test/kotlin/
├── fixtures/        # Test interfaces
└── compilation/     # Compilation validation tests
```

## Key Metro Principles to Follow

1. **Context-Driven Generation** - Always use context objects, never direct pluginContext
2. **Graceful Error Handling** - Report errors with diagnostics, don't crash
3. **API Validation** - Check Kotlin APIs before use
4. **Structured Generation** - Clear separation: analysis → generation → validation
5. **Testing Rigor** - Comprehensive compiler-level tests

## Validation Checklist

When debugging IR generation, verify Metro alignment:

- [ ] Using context pattern (IrFaktContext)?
- [ ] Error handling with diagnostics?
- [ ] API validation before critical operations?
- [ ] Structured generation pipeline (FIR → IR)?
- [ ] Compilation tests for generated code?

## References

- Metro Source: `/metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/ir/`
- Metro IrGenerationExtension: `MetroIrGenerationExtension.kt`
- Fakt Implementation: `ktfake/compiler/src/main/kotlin/.../UnifiedFaktIrGenerationExtension.kt`
