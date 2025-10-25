---
name: metro-pattern-validator
description: Validates Fakt implementation alignment with Metro architectural patterns including two-phase FIR→IR compilation, context-driven generation, CompilerPluginRegistrar structure, and IrGenerationExtension patterns. Use when checking Metro alignment, validating architectural patterns, verifying compiler plugin structure, or when user mentions "Metro patterns", "validate Metro", "architectural alignment", "check patterns", or specific component names.
allowed-tools: [Read, Grep, Glob, TodoWrite]
---

# Metro Pattern Compliance Validator

Validates Fakt compiler plugin implementation alignment with Metro dependency injection framework architectural patterns.

## Core Mission

Ensures Fakt follows proven Metro patterns for Kotlin compiler plugins: two-phase FIR→IR compilation, context-driven generation, proper extension registration, and professional code organization.

## Instructions

### 1. Determine Validation Scope

**Extract from conversation:**
- Specific component: "validate Metro alignment for UnifiedFaktIrGenerationExtension"
- General check: "check Metro patterns", "validate architecture"
- Default: Validate ALL components if no specific mention

**Components available for validation:**
1. **CompilerPluginRegistrar** - Plugin registration and configuration
2. **IrGenerationExtension** - IR generation logic
3. **FirExtensionRegistrar** - FIR phase detection
4. **Context Pattern** - IrFaktContext usage
5. **Error Handling** - Diagnostic patterns
6. **Testing Structure** - Test organization

**If unclear:**
```
Ask: "Would you like me to validate a specific component or check all Metro pattern alignment?"
Options: all | plugin-registrar | ir-extension | fir-extension | context | error-handling | testing
```

### 2. Load Metro Pattern Reference

**For detailed Metro patterns:**
- Consult `resources/metro-patterns-reference.md`
- Or use fakt-docs-navigator: `.claude/docs/development/metro-alignment.md`

**Metro Architecture Baseline:**
- Two-phase FIR → IR compilation
- Context-driven generation (IrMetroContext)
- CompilerPluginRegistrar with supportsK2 = true
- FirExtensionRegistrar + IrGenerationExtension
- Professional error handling with diagnostics

### 3. Validate CompilerPluginRegistrar Pattern

**Read Fakt implementation:**
```bash
Read compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/FaktCompilerPluginRegistrar.kt
```

**Metro Pattern Checklist:**
- [ ] Extends `CompilerPluginRegistrar`
- [ ] `override val supportsK2: Boolean = true`
- [ ] Options loading pattern (FaktOptions.load())
- [ ] FIR extension registration (FirExtensionRegistrarAdapter)
- [ ] IR extension registration (IrGenerationExtension)
- [ ] Proper enabled check before registration

**Comparison with Metro:**
```kotlin
// Metro pattern:
class MetroCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val options = MetroOptions.load(configuration)
        if (!options.enabled) return

        FirExtensionRegistrarAdapter.registerExtension(MetroFirExtensionRegistrar(...))
        IrGenerationExtension.registerExtension(MetroIrGenerationExtension(...))
    }
}

// Fakt should follow similar structure
```

**Validation Output:**
```
🔍 VALIDATING: CompilerPluginRegistrar Pattern

📋 Metro Pattern Requirements:
✅ CompilerPluginRegistrar inheritance
✅ supportsK2 = true
✅ Options loading
✅ FIR extension registration
✅ IR extension registration

📋 Fakt Implementation:
{Check each requirement and mark ✅ or ❌}

🎯 ALIGNMENT SCORE: {percentage}%
{List deviations if any}
```

### 4. Validate IrGenerationExtension Pattern

**Read Fakt implementation:**
```bash
Read compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt
```

**Metro Pattern Checklist:**
- [ ] Extends `IrGenerationExtension`
- [ ] Creates context object (IrFaktContext)
- [ ] Uses `context()` for scoping
- [ ] Separates `generate()` and internal generation logic
- [ ] Proper moduleFragment traversal
- [ ] Error handling with diagnostics

**Comparison with Metro:**
```kotlin
// Metro pattern:
class MetroIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val context = IrMetroContext(pluginContext, messageCollector, symbols)
        context(context) { generateInner(moduleFragment) }
    }

    context(context: IrMetroContext)
    private fun generateInner(moduleFragment: IrModuleFragment) {
        // Context-scoped generation logic
    }
}

// Fakt should align
```

**Validation Output:**
```
🔍 VALIDATING: IrGenerationExtension Pattern

📋 Metro Pattern Requirements:
✅ IrGenerationExtension inheritance
✅ Context object creation
✅ context() scoping usage
✅ Separated generation logic
✅ Module traversal patterns

📋 Fakt Implementation:
{Check each requirement}

🎯 ALIGNMENT SCORE: {percentage}%
{List deviations}
```

### 5. Validate Context Pattern

**Check for IrFaktContext:**
```bash
Grep pattern="class.*Context" compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
```

**Metro Context Pattern Requirements:**
- [ ] Dedicated context class (IrFaktContext)
- [ ] Bundles all generation dependencies
- [ ] Contains pluginContext, messageCollector, options
- [ ] Used with `context()` for scoping
- [ ] Provides context-specific utilities

**Comparison:**
```kotlin
// Metro pattern:
data class IrMetroContext(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
    val symbols: Symbols,
    val options: MetroOptions
) {
    val irFactory = pluginContext.irFactory
    val irBuiltIns = pluginContext.irBuiltIns
}

// Fakt should have similar IrFaktContext
```

**Validation Output:**
```
🔍 VALIDATING: Context Pattern

📋 Metro Pattern Requirements:
✅ Dedicated context class
✅ Bundles dependencies
✅ Used for scoping
✅ Provides utilities

📋 Fakt Implementation:
{Check each}

🎯 ALIGNMENT SCORE: {percentage}%
```

### 6. Validate FIR Phase Implementation

**Check for FirExtensionRegistrar:**
```bash
Read compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FaktFirExtensionRegistrar.kt
```

**Metro FIR Pattern Requirements:**
- [ ] FirExtensionRegistrar implementation
- [ ] @Fake annotation detection in FIR phase
- [ ] Validation before IR phase
- [ ] Passes metadata to IR phase
- [ ] Proper error reporting

**Validation Output:**
```
🔍 VALIDATING: FIR Phase Implementation

📋 Metro Pattern Requirements:
✅ FirExtensionRegistrar present
✅ Annotation detection
✅ Validation logic
✅ Metadata passing

📋 Fakt Implementation:
{Check each}

🎯 ALIGNMENT SCORE: {percentage}%
```

### 7. Validate Error Handling Patterns

**Check diagnostic patterns:**
```bash
Grep pattern="(messageCollector|reportError|reportWarning)" compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
```

**Metro Error Handling Requirements:**
- [ ] MessageCollector usage
- [ ] Proper error reporting with location
- [ ] Warning for non-critical issues
- [ ] Graceful failures (no crashes)
- [ ] User-friendly error messages

**Comparison:**
```kotlin
// Metro pattern:
if (!isValidComponent(element)) {
    context.messageCollector.report(
        CompilerMessageSeverity.ERROR,
        "Invalid component structure",
        CompilerMessageLocationWithRange.create(element.location)!!
    )
    return
}

// Fakt should follow similar diagnostic patterns
```

**Validation Output:**
```
🔍 VALIDATING: Error Handling Patterns

📋 Metro Pattern Requirements:
✅ MessageCollector usage
✅ Error reporting with location
✅ Warning for non-critical
✅ Graceful failures

📋 Fakt Implementation:
{Check each}

🎯 ALIGNMENT SCORE: {percentage}%
```

### 8. Validate Testing Structure

**Check test organization:**
```bash
ls -la compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/
```

**Metro Testing Pattern Requirements:**
- [ ] Separate compiler-tests/ module (optional, future)
- [ ] Comprehensive integration tests
- [ ] Test fixtures for common scenarios
- [ ] Compilation validation tests
- [ ] GIVEN-WHEN-THEN naming

**Note**: Metro has `compiler-tests/` module. Fakt can follow similar organization in future.

**Validation Output:**
```
🔍 VALIDATING: Testing Structure

📋 Metro Pattern Recommendations:
⚠️ Dedicated compiler-tests module (future improvement)
✅ Integration tests present
✅ GIVEN-WHEN-THEN patterns
✅ Compilation validation

📋 Fakt Implementation:
{Check each}

🎯 ALIGNMENT SCORE: {percentage}%
```

### 9. Generate Comprehensive Report

**Overall alignment report:**

```
═══════════════════════════════════════════════════
🏗️ METRO PATTERN COMPLIANCE VALIDATION REPORT
═══════════════════════════════════════════════════

📊 OVERALL ALIGNMENT SCORE: {average}%

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ COMPONENT SCORES:

1. CompilerPluginRegistrar Pattern: {score}%
   {Brief status and key points}

2. IrGenerationExtension Pattern: {score}%
   {Brief status}

3. Context Pattern: {score}%
   {Brief status}

4. FIR Phase Implementation: {score}%
   {Brief status}

5. Error Handling: {score}%
   {Brief status}

6. Testing Structure: {score}%
   {Brief status}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 KEY ALIGNMENTS (What's working well):
- {List strong alignments}

⚠️  DEVIATIONS FROM METRO (Areas for improvement):
- {List deviations}

📋 RECOMMENDATIONS:
1. {Recommendation 1}
2. {Recommendation 2}
...

🔗 REFERENCES:
- Metro Source: metro/compiler/src/main/kotlin/.../
- Fakt Docs: .claude/docs/development/metro-alignment.md
- Pattern Guide: resources/metro-patterns-reference.md

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✨ NEXT STEPS:
{Actionable next steps to improve alignment}

═══════════════════════════════════════════════════
```

### 10. Suggest Improvements

**Based on validation results, suggest:**

**High Priority (Low alignment scores):**
- Missing patterns that are critical
- Architectural deviations that impact quality
- Safety/correctness issues

**Medium Priority:**
- Style/organization improvements
- Optional Metro patterns not yet adopted
- Documentation gaps

**Low Priority:**
- Nice-to-have Metro features
- Advanced patterns for future consideration

## Supporting Files

Progressive disclosure for Metro patterns:

- **`resources/metro-patterns-reference.md`** - Complete Metro pattern catalog (loaded on-demand)
- **`resources/compiler-plugin-best-practices.md`** - Best practices from Metro (loaded on-demand)
- **`resources/metro-code-examples.md`** - Code examples from Metro source (loaded on-demand)

## Related Skills

This Skill composes with:
- **`fakt-docs-navigator`** - Access Metro alignment docs
- **`kotlin-ir-debugger`** - Debug IR generation issues found
- **`kotlin-api-consultant`** - Validate Kotlin API usage

## Validation Scope Matrix

| Component | What's Validated | Metro Reference |
|-----------|------------------|-----------------|
| CompilerPluginRegistrar | Registration, options, K2 support | MetroCompilerPluginRegistrar.kt |
| IrGenerationExtension | Generation logic, context usage | MetroIrGenerationExtension.kt |
| FirExtensionRegistrar | Annotation detection, validation | MetroFirExtensionRegistrar.kt |
| Context Pattern | IrFaktContext structure | IrMetroContext in Metro |
| Error Handling | MessageCollector, diagnostics | Error handling throughout Metro |
| Testing | Test structure, organization | metro/compiler-tests/ |

## Best Practices

1. **Compare don't copy** - Understand Metro patterns but adapt to Fakt needs
2. **Score objectively** - Base scores on actual code inspection
3. **Prioritize critical patterns** - CompilerPluginRegistrar and IrGenerationExtension first
4. **Document deviations** - Explain why Fakt deviates if intentional
5. **Actionable recommendations** - Suggest concrete next steps

## Metro Alignment Philosophy

**Why follow Metro patterns:**
- Proven in production (Slack uses Metro)
- K2 compiler ready
- Professional architecture
- Clear separation of concerns
- Robust error handling

**When to deviate:**
- Fakt-specific requirements
- Simpler patterns work better
- Different problem domain (test fakes vs DI)

**Goal**: Not blind copying but principled adaptation

## Known Metro Patterns to Follow

1. **Two-Phase Compilation**: FIR detection → IR generation
2. **Context-Driven**: Bundle dependencies in context object
3. **Options Pattern**: Load configuration from CompilerConfiguration
4. **Error Diagnostics**: MessageCollector with proper severity
5. **K2 Support**: supportsK2 = true
6. **Testing Rigor**: Comprehensive compiler tests

## Current Fakt Status (Phase 1)

Expected alignment ~70-80% because:
- ✅ IR generation working
- ✅ Two-phase approach started
- ⚠️ Context pattern partial
- ⚠️ FIR phase basic
- ⚠️ Testing structure evolving

**Phase 2 Target**: 90%+ alignment with full Metro patterns
