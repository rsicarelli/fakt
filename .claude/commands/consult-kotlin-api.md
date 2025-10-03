---
allowed-tools: Read, Grep, Glob, Bash(find:*), WebFetch, TodoWrite, Task
argument-hint: <api_class_or_interface> (required - Kotlin API to analyze and validate)
description: Query Kotlin compiler source for API validation, compatibility checks, and Metro alignment
model: claude-sonnet-4-20250514
---

# 🔍 Kotlin API Oracle & Compatibility Validator

**Automatic Kotlin source consultation with Metro pattern alignment**

## 📚 Context Integration

**This command leverages:**
- `.claude/docs/analysis/metro-inspiration.md` - Metro architectural patterns
- `.claude/docs/implementation/kotlin-api-reference.md` - Kotlin compiler API documentation
- `.claude/docs/analysis/generic-scoping-analysis.md` - API solutions for generic challenges
- `.claude/docs/troubleshooting/common-issues.md` - API compatibility issue patterns
- Kotlin compiler source at `/kotlin/compiler/` for real-time validation
- Metro usage patterns for best practice alignment

**🏆 API CONSULTATION BASELINE:**
- Real-time Kotlin source validation
- Metro pattern compliance checking
- Breaking change detection across versions
- Phase 2 generic scoping API discovery

## Purpose
Query Kotlin compiler source to validate APIs, understand patterns, and ensure compatibility.

## Usage
```bash
/consult-kotlin-api <api_class_or_interface>
```

## What This Command Does

### 1. **API Location**
- Find the API in `/kotlin/compiler/` source
- Identify correct module and package
- Locate interface/class definition

### 2. **API Analysis**
- Current method signatures
- Breaking changes detection
- Deprecation warnings
- New methods added

### 3. **Usage Patterns**
- How Kotlin internally uses this API
- Best practices from Kotlin source
- Common patterns and anti-patterns

### 4. **Metro Alignment Check**
- How Metro uses this API
- Compatibility with Metro patterns
- Recommended usage for KtFakes

## Supported APIs

### **IrGenerationExtension**
```bash
/consult-kotlin-api IrGenerationExtension
```

**Output:**
```
🔍 KOTLIN API: IrGenerationExtension

📍 Location: /kotlin/compiler/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/IrGenerationExtension.kt

📋 Current Interface (Kotlin 2.2.10):
interface IrGenerationExtension {
    fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)
    fun getPlatformIntrinsicExtension(loweringContext: LoweringContext): IrIntrinsicExtension?

    // K1/K2 compatibility
    @FirIncompatiblePluginAPI
    val shouldAlsoBeAppliedInKaptStubGenerationMode: Boolean get() = false
}

🏗️ Metro Usage Pattern:
class MetroIrGenerationExtension(...) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val context = IrMetroContext(pluginContext, messageCollector, ...)
        context(context) { generateInner(moduleFragment) }
    }
}

✅ KtFakes Alignment:
- Follow Metro pattern exactly ✅
- Use context pattern for organization ✅
- Handle K2 compatibility properly ✅

📚 Best Practices:
- Always create context object for organization
- Use context() for better scoping
- Handle errors gracefully with messageCollector
- Support both K1 and K2 compiler modes
```

### **IrPluginContext**
```bash
/consult-kotlin-api IrPluginContext
```

**Output:**
```
🔍 KOTLIN API: IrPluginContext

📍 Location: /kotlin/compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/IrPluginContext.kt

📋 Critical Properties for KtFakes:
interface IrPluginContext {
    val irFactory: IrFactory                    // ✅ For creating IR elements
    val symbolTable: SymbolTable                // ✅ For symbol resolution
    val moduleDescriptor: ModuleDescriptor      // ✅ Module information

    // Symbol reference methods (critical for us)
    fun referenceClass(classId: ClassId): IrClassSymbol?
    fun referenceFunction(callableId: CallableId): IrSimpleFunctionSymbol?
    fun referenceFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol>
}

🎯 KtFakes Usage:
context(context: IrKtFakeContext)
private fun generateFakeImplementation(fakeInterface: IrClass): IrClass {
    // Use irFactory for creating new IR elements
    val fakeClass = context.pluginContext.irFactory.buildClass {
        name = Name.identifier("Fake${fakeInterface.name}Impl")
        // ...
    }

    // Use symbolTable for resolving references
    val stringSymbol = context.pluginContext.referenceClass(ClassId.topLevel(FqName("kotlin.String")))
}
```

### **IrTypeParameter**
```bash
/consult-kotlin-api IrTypeParameter
```

**Output:**
```
🔍 KOTLIN API: IrTypeParameter (Phase 2 Generic Scoping)

📍 Location: /kotlin/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/declarations/IrTypeParameter.kt

📋 Generic Type System:
interface IrTypeParameter : IrDeclaration {
    val name: Name                              // Type parameter name (T, R, etc.)
    val index: Int                              // Position in type parameter list
    val isReified: Boolean                      // reified modifier
    val variance: Variance                      // in, out, invariant
    val superTypes: List<IrType>               // Upper bounds
}

interface IrTypeParameterSymbol : IrBindableSymbol<IrTypeParameter>

🎯 Generic Scoping Solution:
// How to resolve our Phase 2 challenge using Kotlin APIs
when (val classifier = irType.classifier) {
    is IrTypeParameterSymbol -> {
        val typeParameter = classifier.owner
        val name = typeParameter.name.asString()  // "T"
        val index = typeParameter.index           // 0 for first parameter

        // This is how we can solve method-level vs class-level scoping
        "typeParameter_${name}_${index}"  // Generate unique identifier
    }
}

🚨 Phase 2 Application:
This API gives us the tools to solve our generic scoping challenge by:
1. Properly identifying type parameters
2. Understanding their scope (method vs class level)
3. Generating appropriate casting code with proper suppressions
```

## Implementation

### **API Discovery Process**
```kotlin
fun consultKotlinApi(apiName: String): ApiConsultationResult {
    // 1. Search for API in Kotlin source
    val apiLocations = searchKotlinSource(apiName)

    // 2. Parse current interface/class definition
    val currentDefinition = parseApiDefinition(apiLocations.first())

    // 3. Check Metro usage
    val metroUsage = findMetroUsage(apiName)

    // 4. Generate recommendations
    val recommendations = generateRecommendations(currentDefinition, metroUsage)

    return ApiConsultationResult(currentDefinition, metroUsage, recommendations)
}
```

### **Breaking Change Detection**
```kotlin
fun detectBreakingChanges(apiName: String): List<BreakingChange> {
    // Compare with previous Kotlin version
    // Identify removed methods, changed signatures
    // Flag potential compatibility issues
}
```

## Quick API References

### **Most Used APIs in KtFakes:**
```bash
/consult-kotlin-api IrGenerationExtension    # Main extension interface
/consult-kotlin-api IrPluginContext          # Core context for IR manipulation
/consult-kotlin-api IrFactory                # Creating new IR elements
/consult-kotlin-api IrClass                  # Interface/class representation
/consult-kotlin-api IrFunction               # Method representation
/consult-kotlin-api IrTypeParameter          # Generic type parameters (Phase 2)
```

### **Metro Alignment APIs:**
```bash
/consult-kotlin-api CompilerPluginRegistrar  # Plugin registration
/consult-kotlin-api FirExtensionRegistrar    # FIR phase extensions
/consult-kotlin-api MessageCollector        # Error reporting
```

## Error Scenarios

### **API Not Found**
```
❌ ERROR: API 'NonExistentApi' not found in Kotlin source
💡 TIP: Check spelling and ensure it's a public API
🔍 SEARCH: Trying fuzzy search for similar APIs...
```

### **Deprecated API**
```
⚠️  WARNING: API 'OldIrFactory' is deprecated
🔄 REPLACEMENT: Use IrFactory instead
📅 DEPRECATED: Since Kotlin 1.9.0
🗑️  REMOVAL: Planned for Kotlin 2.0.0
```

### **Breaking Changes Detected**
```
🚨 BREAKING CHANGE: IrGenerationExtension.generate() signature changed
📋 OLD: generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)
📋 NEW: generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext, configuration: CompilerConfiguration)
🔧 ACTION REQUIRED: Update KtFakes implementation
```

## Related Commands
- `/debug-ir-generation <interface>` - Use validated APIs for generation
- `/validate-metro-alignment` - Check Metro usage patterns
- `/run-bdd-tests` - Test API compatibility

## Technical References
- Kotlin Compiler Source: `/kotlin/compiler/`
- Metro API Usage: `/metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/`
- KtFakes Implementation: `ktfake/compiler/`