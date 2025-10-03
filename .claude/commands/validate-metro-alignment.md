---
allowed-tools: Read, Grep, Glob, Bash(find:*), TodoWrite, Task
argument-hint: [component_name] (optional - specific component to validate, default: all)
description: Validate KtFakes implementation alignment with Metro architectural patterns
model: claude-sonnet-4-20250514
---

# 🏗️ Metro Pattern Compliance Validator

**Architectural alignment verification with Metro dependency injection patterns**

## 📚 Context Integration

**This command leverages:**
- `.claude/docs/analysis/metro-inspiration.md` - Complete Metro architectural analysis
- `.claude/docs/architecture/unified-ir-native.md` - KtFakes architectural foundation
- `.claude/docs/implementation/kotlin-api-reference.md` - Kotlin compiler API usage
- Real Metro source code for pattern comparison
- KtFakes implementation code for alignment validation

**🏆 METRO ALIGNMENT BASELINE:**
- IrGenerationExtension pattern following Metro structure
- Context-driven architecture with proper scoping
- Two-phase FIR → IR compilation approach
- Error handling and diagnostic patterns

## Usage
```bash
/validate-metro-alignment [component_name]
```

## What This Command Does

### 1. **Architectural Pattern Validation**
- Compare KtFakes structure with Metro structure
- Verify two-phase FIR → IR compilation
- Check context usage patterns
- Validate extension registration approach

### 2. **Code Pattern Analysis**
- Context pattern implementation
- Error handling alignment
- Testing structure comparison
- Configuration patterns

### 3. **API Usage Alignment**
- IrGenerationExtension usage
- CompilerPluginRegistrar pattern
- Symbol resolution approaches
- Code generation patterns

## Validation Categories

### **1. Plugin Registration Pattern**

**Metro Reference:**
```kotlin
// metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/MetroCompilerPluginRegistrar.kt
class MetroCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val options = MetroOptions.load(configuration)
        if (!options.enabled) return

        FirExtensionRegistrarAdapter.registerExtension(MetroFirExtensionRegistrar(...))
        IrGenerationExtension.registerExtension(MetroIrGenerationExtension(...))
    }
}
```

**KtFakes Validation:**
```
🔍 CHECKING: Plugin Registration Pattern

📋 Metro Pattern:
- CompilerPluginRegistrar inheritance ✅
- supportsK2 = true ✅
- Options loading pattern ✅
- FIR + IR extension registration ✅

📋 KtFakes Implementation:
- CompilerPluginRegistrar: ✅ Implemented
- supportsK2: ✅ Set to true
- Options pattern: ⚠️  Needs KtFakeOptions implementation
- FIR extension: ⚠️  Needs FirExtensionRegistrar
- IR extension: ✅ UnifiedKtFakesIrGenerationExtension

🎯 ALIGNMENT SCORE: 70% - Missing FIR phase and options
```

### **2. IrGenerationExtension Pattern**

**Metro Reference:**
```kotlin
class MetroIrGenerationExtension(...) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val context = IrMetroContext(pluginContext, messageCollector, symbols, ...)
        context(context) { generateInner(moduleFragment) }
    }

    context(context: IrMetroContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        // Generation logic with context
    }
}
```

**KtFakes Validation:**
```
🔍 CHECKING: IrGenerationExtension Pattern

📋 Metro Pattern:
- IrGenerationExtension inheritance ✅
- Context object creation ✅
- context() usage for scoping ✅
- Separate generateInner method ✅

📋 KtFakes Implementation:
- IrGenerationExtension: ✅ UnifiedKtFakesIrGenerationExtension
- Context pattern: ❌ Missing IrKtFakeContext
- context() scoping: ❌ Not implemented
- Separation of concerns: ⚠️  Partial - methods exist but not organized

🎯 ALIGNMENT SCORE: 40% - Missing context pattern
```

### **3. Context Pattern Implementation**

**Metro Reference:**
```kotlin
data class IrMetroContext(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
    val symbols: Symbols,
    val options: MetroOptions,
    // ... other context data
) {
    // Context-specific functionality
}
```

**KtFakes Validation:**
```
🔍 CHECKING: Context Pattern

📋 Metro Pattern:
- Dedicated context class ✅
- Bundles all generation dependencies ✅
- Used with context() for scoping ✅
- Contains symbols, options, messageCollector ✅

📋 KtFakes Implementation:
- IrKtFakeContext: ❌ Missing - needs implementation
- Parameter bundling: ❌ Parameters passed individually
- context() usage: ❌ Not implemented
- Scoped generation methods: ❌ Missing

🎯 ALIGNMENT SCORE: 0% - Context pattern not implemented

💡 RECOMMENDATION: Implement IrKtFakeContext following Metro pattern:
```kotlin
data class IrKtFakeContext(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
    val options: KtFakeOptions,
    // ... KtFakes-specific context
)
```

### **4. Testing Structure Pattern**

**Metro Reference:**
```
metro/
├── compiler/src/test/        # Legacy tests
├── compiler-tests/           # Modern JetBrains testing
└── samples/                 # Integration examples
```

**KtFakes Validation:**
```
🔍 CHECKING: Testing Structure

📋 Metro Pattern:
- Legacy tests in compiler/src/test ✅
- Modern compiler-tests directory ✅
- Real-world samples directory ✅
- BDD naming conventions ✅ (see [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md))

📋 KtFakes Implementation:
- Legacy tests: ✅ compiler/src/test exists
- Modern compiler-tests: ❌ Missing directory
- Samples: ✅ test-sample exists
- BDD naming: ⚠️  Partial compliance (follow [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md))

🎯 ALIGNMENT SCORE: 60% - Missing modern testing infrastructure

💡 RECOMMENDATION: Create ktfake/compiler-tests/ following Metro structure
```

### **5. Error Handling Pattern**

**Metro Reference:**
```kotlin
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

**KtFakes Validation:**
```
🔍 CHECKING: Error Handling Pattern

📋 Metro Pattern:
- Dedicated error reporting functions ✅
- MessageCollector usage ✅
- Source location information ✅
- Severity levels (ERROR, WARNING, INFO) ✅

📋 KtFakes Implementation:
- Error reporting: ⚠️  Basic implementation exists
- Source locations: ❌ Missing detailed source info
- Severity levels: ⚠️  Limited usage
- Diagnostic messages: ⚠️  Basic but could be improved

🎯 ALIGNMENT SCORE: 45% - Basic but needs enhancement
```

## Detailed Validation Results

### **Component-Specific Validation**

```bash
/validate-metro-alignment compiler
```

**Output:**
```
🏗️ METRO ALIGNMENT REPORT: Compiler Module

📊 OVERALL ALIGNMENT: 52%

🔧 CRITICAL GAPS:
1. ❌ IrKtFakeContext missing - implement Metro context pattern
2. ❌ FirExtensionRegistrar missing - add FIR phase support
3. ❌ KtFakeOptions missing - implement Metro options pattern
4. ❌ compiler-tests/ missing - add modern testing infrastructure

⚠️  PARTIAL IMPLEMENTATIONS:
1. UnifiedKtFakesIrGenerationExtension exists but needs context pattern
2. Error handling basic but needs Metro-style diagnostics
3. Testing exists but needs BDD compliance improvement (follow [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md))

✅ ALIGNED COMPONENTS:
1. CompilerPluginRegistrar inheritance ✅
2. IrGenerationExtension implementation ✅
3. Basic IR generation working ✅
4. Integration testing with test-sample ✅

🎯 PRIORITY FIXES:
1. HIGH: Implement IrKtFakeContext pattern
2. HIGH: Add FIR phase extension
3. MEDIUM: Create KtFakeOptions system
4. MEDIUM: Add compiler-tests/ directory
```

### **Specific Pattern Validation**

```bash
/validate-metro-alignment context-pattern
```

**Output:**
```
🔍 METRO PATTERN: Context Pattern Validation

❌ CURRENT STATE: Not implemented

📋 METRO IMPLEMENTATION:
data class IrMetroContext(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
    val symbols: Symbols,
    val options: MetroOptions,
    val lookupTracker: LookupTracker?,
    val expectActualTracker: ExpectActualTracker,
)

🎯 REQUIRED KTFAKES IMPLEMENTATION:
data class IrKtFakeContext(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
    val options: KtFakeOptions,
    // Add KtFakes-specific context
) {
    // KtFakes-specific helper methods
    fun generateFakeImplementation(fakeInterface: IrClass): IrClass { ... }
    fun generateFactoryFunction(fakeInterface: IrClass): IrFunction { ... }
}

🔧 USAGE PATTERN:
class UnifiedKtFakesIrGenerationExtension(...) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val context = IrKtFakeContext(pluginContext, messageCollector, options)
        context(context) { generateInner(moduleFragment) }
    }

    context(context: IrKtFakeContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        // All generation with context
    }
}
```

## Implementation Roadmap

### **Phase 1: Core Pattern Implementation**
```
1. ✅ Basic IrGenerationExtension (done)
2. 🔄 Implement IrKtFakeContext pattern
3. 🔄 Add context() scoping
4. 🔄 Implement KtFakeOptions
```

### **Phase 2: FIR Phase Addition**
```
1. Add FirExtensionRegistrar
2. Implement @Fake annotation detection in FIR
3. Add FIR validation and error reporting
4. Connect FIR → IR pipeline
```

### **Phase 3: Testing Infrastructure**
```
1. Create compiler-tests/ directory
2. Add JetBrains testing infrastructure
3. Implement BDD naming throughout
4. Add integration testing improvements
```

## Related Commands
- `/debug-ir-generation <interface>` - Test Metro-aligned generation
- `/consult-kotlin-api IrGenerationExtension` - Validate Metro API usage
- `/run-bdd-tests` - Test current Metro alignment

## Technical References
- Metro Architecture: `/metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/`
- KtFakes Current: `ktfake/compiler/src/main/kotlin/`
- Alignment Goals: `.claude/docs/development/metro-alignment.md`