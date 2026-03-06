---
name: compiler-architecture-validator
description: Validates Fakt implementation follows compiler plugin best practices including two-phase FIR→IR compilation, context-driven generation, CompilerPluginRegistrar structure, and IrGenerationExtension patterns. Use when validating architectural patterns, verifying compiler plugin structure, checking code quality, or when user mentions "architecture", "validate patterns", "compiler plugin", "check structure", or specific component names.
allowed-tools: Read, Grep, Glob, TaskCreate, TaskUpdate
---

# Compiler Architecture Validator

Validates Fakt compiler plugin implementation follows industry-standard architectural patterns and best practices for Kotlin compiler plugins.

## Core Mission

Ensures Fakt follows best practices for Kotlin compiler plugins: two-phase FIR→IR compilation, context-driven generation, proper extension registration, and professional code organization.

## Instructions

### 1. Determine Validation Scope

**Extract from conversation:**
- Specific component: "validate UnifiedFaktIrGenerationExtension"
- General check: "validate architecture"
- Default: Validate ALL components if no specific mention

**Components available for validation:**
1. **CompilerPluginRegistrar** - Plugin registration and configuration
2. **IrGenerationExtension** - IR generation logic
3. **FirExtensionRegistrar** - FIR phase detection
4. **Context Pattern** - FaktSharedContext / IrFaktContext usage
5. **Error Handling** - Diagnostic patterns
6. **Testing Structure** - Test organization

**If unclear:**
```
Ask: "Would you like me to validate a specific component or check all architectural patterns?"
Options: all | plugin-registrar | ir-extension | fir-extension | context | error-handling | testing
```

### 2. Validate CompilerPluginRegistrar Pattern

**Read Fakt implementation:**
```bash
Read compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/FaktCompilerPluginRegistrar.kt
```

**Checklist:**
- [ ] Extends `CompilerPluginRegistrar`
- [ ] `override val supportsK2: Boolean = true`
- [ ] Options loading pattern (FaktOptions.load())
- [ ] FIR extension registration (FirExtensionRegistrarAdapter)
- [ ] IR extension registration (IrGenerationExtension)
- [ ] Proper enabled check before registration

**Expected pattern:**
```kotlin
class FaktCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val options = FaktOptions.load(configuration)
        if (!options.enabled) return

        FirExtensionRegistrarAdapter.registerExtension(FaktFirExtensionRegistrar(...))
        IrGenerationExtension.registerExtension(UnifiedFaktIrGenerationExtension(...))
    }
}
```

### 3. Validate IrGenerationExtension Pattern

**Read Fakt implementation:**
```bash
Read compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt
```

**Checklist:**
- [ ] Extends `IrGenerationExtension`
- [ ] Creates context object
- [ ] Separates `generate()` and internal generation logic
- [ ] Proper moduleFragment traversal
- [ ] Error handling with diagnostics

### 4. Validate Context Pattern

**Check for shared context:**
```bash
Grep pattern="class.*Context" compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
```

**Checklist:**
- [ ] Dedicated context class (FaktSharedContext or IrFaktContext)
- [ ] Bundles all generation dependencies
- [ ] Contains pluginContext, messageCollector, options
- [ ] Provides context-specific utilities

### 5. Validate FIR Phase Implementation

**Read:**
```bash
Read compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FaktFirExtensionRegistrar.kt
```

**Checklist:**
- [ ] FirExtensionRegistrar implementation
- [ ] @Fake annotation detection in FIR phase
- [ ] Validation before IR phase
- [ ] Passes metadata to IR phase
- [ ] Proper error reporting

### 6. Validate Error Handling Patterns

```bash
Grep pattern="(messageCollector|reportError|reportWarning)" compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
```

**Checklist:**
- [ ] MessageCollector usage
- [ ] Error reporting with source location
- [ ] Warning for non-critical issues
- [ ] Graceful failures (no crashes)
- [ ] User-friendly error messages

### 7. Validate Testing Structure

```bash
ls -la compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/
```

**Checklist:**
- [ ] Comprehensive integration tests
- [ ] GIVEN-WHEN-THEN naming
- [ ] Compilation validation tests
- [ ] Isolated test instances

### 8. Generate Report

```
ARCHITECTURE VALIDATION REPORT

COMPONENT SCORES:
1. CompilerPluginRegistrar: {score}%
2. IrGenerationExtension: {score}%
3. Context Pattern: {score}%
4. FIR Phase: {score}%
5. Error Handling: {score}%
6. Testing Structure: {score}%

KEY STRENGTHS:
- {List strong areas}

AREAS FOR IMPROVEMENT:
- {List deviations}

RECOMMENDATIONS:
1. {Recommendation}
...
```

## Related Skills

- **`kotlin-api-consultant`** - Validate Kotlin API usage
- **`fakt-docs-navigator`** - Access architecture documentation

## Best Practices

1. **Score objectively** - Base scores on actual code inspection
2. **Prioritize critical patterns** - CompilerPluginRegistrar and IrGenerationExtension first
3. **Document deviations** - Explain why Fakt deviates if intentional
4. **Actionable recommendations** - Suggest concrete next steps
