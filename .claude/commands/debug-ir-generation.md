---
allowed-tools: Read, Grep, Glob, Bash(./gradlew:*), Bash(find:*), Bash(kotlinc:*), TodoWrite, Task
argument-hint: <interface_name> (required - name of interface to debug IR generation)
description: Step-by-step debugging of IR generation process for specific interfaces with Metro pattern validation
model: claude-sonnet-4-20250514
---

# 🔬 IR Generation Deep Debug & Validation

**Metro-inspired debugging with comprehensive interface analysis**

## 📚 Context Integration

**This command leverages:**
- `.claude/docs/analysis/metro-inspiration.md` - Metro architectural patterns for IR generation
- `.claude/docs/implementation/kotlin-api-reference.md` - Kotlin compiler API validation
- `.claude/docs/patterns/ir-generation-flow.md` - Complete IR generation workflow
- `.claude/docs/troubleshooting/common-issues.md` - Known issue patterns and solutions
- `.claude/docs/analysis/generic-scoping-analysis.md` - Generic type handling challenges
- Real IR generation code in `ktfake/compiler/src/main/kotlin/`

**🏆 METRO PATTERN BASELINE:**
- Context-driven generation following Metro IrGenerationExtension
- Comprehensive error handling with diagnostic patterns
- Metro validation approach for API compatibility
- Step-by-step generation debugging similar to Metro's approach

## Purpose
Debug IR generation process for specific interfaces, following Metro patterns with Kotlin API validation.

## Usage
```bash
/debug-ir-generation <interface_name>
```

## What This Command Does

### 1. **Interface Analysis**
- Locate the specified interface in the codebase
- Analyze `@Fake` annotation and parameters
- Check interface structure (methods, properties, generics)

### 2. **Metro Pattern Consultation**
- Compare with Metro's dependency injection generation
- Apply Metro context patterns (`IrMetroContext` → `IrKtFakeContext`)
- Use Metro error handling patterns

### 3. **Kotlin API Validation**
- Verify IR generation APIs are current
- Check `IrGenerationExtension` compatibility
- Validate `IrPluginContext` usage

### 4. **Step-by-Step Generation Debug**
- FIR phase: `@Fake` detection and validation
- IR phase: Implementation class generation
- Factory function generation
- Configuration DSL generation

## Implementation Steps

### Phase 1: Interface Discovery
```kotlin
// 1. Find interface by name
val targetInterface = moduleFragment.files
    .flatMap { it.declarations }
    .filterIsInstance<IrClass>()
    .find { it.name.asString() == interfaceName && it.isInterface }

// 2. Validate @Fake annotation
val fakeAnnotation = targetInterface?.annotations
    ?.find { it.symbol.owner.parentAsClass.name.asString() == "Fake" }
```

### Phase 2: Metro Pattern Application
```kotlin
// Follow Metro IrGenerationExtension pattern
context(context: IrKtFakeContext)
private fun debugGenerateImplementation(fakeInterface: IrClass) {
    println("Metro-inspired generation for: ${fakeInterface.name}")

    // Apply Metro context pattern
    val implementationClass = context.irFactory.buildClass {
        name = Name.identifier("Fake${fakeInterface.name}Impl")
        // ... Metro-style class building
    }
}
```

### Phase 3: Kotlin API Verification
```kotlin
// Verify APIs against Kotlin source
fun validateKotlinAPIs() {
    // Check IrFactory.buildClass still available
    // Verify IrPluginContext.irFactory exists
    // Confirm IrClass.isInterface property
}
```

### Phase 4: Generated Code Validation
```kotlin
// Ensure generated code compiles
fun validateGeneratedCode(generatedClass: IrClass) {
    // Check all interface methods implemented
    // Verify type safety preservation
    // Validate suspend function handling
}
```

## Output Format

```
🔍 DEBUGGING IR GENERATION: UserService

📋 Interface Analysis:
- Name: UserService
- Package: com.example.services
- Methods: 3 (getUser, updateUser, deleteUser)
- Properties: 1 (currentUser)
- Generics: None
- @Fake parameters: trackCalls=false

🏗️ Metro Pattern Application:
- Using Metro IrGenerationExtension pattern ✅
- Context pattern applied: IrKtFakeContext ✅
- Error handling: Metro diagnostic style ✅

🔧 Kotlin API Validation:
- IrFactory.buildClass: Available ✅
- IrPluginContext.irFactory: Available ✅
- IrClass.isInterface: Available ✅

⚡ Generation Steps:
1. FIR Phase: @Fake detection ✅
2. Implementation class generation ✅
3. Method implementations ✅
4. Factory function generation ✅
5. Configuration DSL generation ✅

✅ RESULT: Generated FakeUserServiceImpl compiles successfully
📄 Output: build/generated/ktfake/test/kotlin/FakeUserServiceImpl.kt
```

## Error Scenarios

### Interface Not Found
```
❌ ERROR: Interface 'NonExistentService' not found
💡 TIP: Check interface name and ensure @Fake annotation is present
```

### Generic Type Issues
```
🚨 WARNING: Generic type parameter <T> detected
📋 Current Status: Phase 2 generic scoping challenge
🔧 Workaround: Dynamic casting with identity functions applied
```

### Compilation Failures
```
❌ ERROR: Generated code compilation failed
🔍 Issue: Missing import for cross-module type
🔧 Fix: Adding import generation (Phase 2.4 priority)
```

## Related Commands
- `/consult-kotlin-api IrGenerationExtension` - Validate IR APIs
- `/validate-metro-alignment` - Check Metro pattern compliance
- `/run-bdd-tests` - Test generation results (follow [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md))

## Technical References
- Metro IrGenerationExtension: `/metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/ir/MetroIrGenerationExtension.kt`
- Kotlin IR APIs: `/kotlin/compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/`
- KtFakes Implementation: `ktfake/compiler/src/main/kotlin/.../UnifiedKtFakesIrGenerationExtension.kt`