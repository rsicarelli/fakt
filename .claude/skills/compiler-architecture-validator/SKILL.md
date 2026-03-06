---
name: compiler-architecture-validator
description: Validates Fakt follows compiler plugin best practices — two-phase FIR→IR, context-driven generation, CompilerPluginRegistrar structure. Use when validating architecture, checking plugin structure, reviewing compiler patterns, or verifying that FIR and IR phases remain properly separated. Make sure to use this skill whenever compiler plugin code is modified — phase mixing and registration issues are subtle bugs that only surface at runtime.
allowed-tools: Read, Grep, Glob, TaskCreate, TaskUpdate
---

# Compiler Architecture Validator

Validates Fakt compiler plugin follows industry-standard architectural patterns.

## Instructions

### 1. Determine Scope

**Extract from conversation:**
- Specific component: "validate UnifiedFaktIrGenerationExtension"
- General check: "validate architecture"
- Default: validate ALL components

**Components:**
1. CompilerPluginRegistrar — Plugin registration
2. IrGenerationExtension — IR generation logic
3. FirExtensionRegistrar — FIR phase detection
4. Context Pattern — FaktSharedContext / IrFaktContext
5. Error Handling — Diagnostic patterns

### 2. Validate CompilerPluginRegistrar

```bash
Read compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/FaktCompilerPluginRegistrar.kt
```

- [ ] Extends `CompilerPluginRegistrar`
- [ ] `override val supportsK2: Boolean = true`
- [ ] Options loading pattern (FaktOptions.load())
- [ ] FIR extension registration (FirExtensionRegistrarAdapter)
- [ ] IR extension registration (IrGenerationExtension)
- [ ] Proper enabled check before registration

### 3. Validate IrGenerationExtension

```bash
Read compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt
```

- [ ] Extends `IrGenerationExtension`
- [ ] Creates context object
- [ ] Separates `generate()` and internal logic
- [ ] Proper moduleFragment traversal
- [ ] Error handling with diagnostics

### 4. Validate Context Pattern

```bash
Grep pattern="class.*Context" compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
```

- [ ] Dedicated context class
- [ ] Bundles pluginContext, messageCollector, options
- [ ] Provides context-specific utilities

### 5. Validate FIR Phase

```bash
Read compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FaktFirExtensionRegistrar.kt
```

- [ ] FirExtensionRegistrar implementation
- [ ] @Fake annotation detection
- [ ] Validation before IR phase
- [ ] Proper error reporting

### 6. Validate Error Handling

```bash
Grep pattern="(messageCollector|reportError|reportWarning)" compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
```

- [ ] MessageCollector usage
- [ ] Error reporting with source location
- [ ] Graceful failures (no crashes)

### 7. Generate Report

```
ARCHITECTURE VALIDATION REPORT

1. CompilerPluginRegistrar: {score}%
2. IrGenerationExtension: {score}%
3. Context Pattern: {score}%
4. FIR Phase: {score}%
5. Error Handling: {score}%

Strengths: {list}
Deviations: {list with explanations}
Recommendations: {actionable items}
```

## Related Skills

- **`kotlin-api-consultant`** — Validate Kotlin API usage
- **`docs-navigator`** — Access architecture documentation
