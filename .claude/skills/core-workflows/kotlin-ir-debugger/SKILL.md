---
name: kotlin-ir-debugger
description: Debugs Kotlin compiler Intermediate Representation (IR) generation for @Fake annotated interfaces. Use when analyzing IR, inspecting compiler output, debugging IR generation issues, troubleshooting interface generation, or when user mentions interface names with "debug", "IR", "intermediate representation", or "compiler generation" context. Follows Metro architectural patterns.
allowed-tools: [Read, Grep, Glob, Bash, TodoWrite]
---

# Kotlin IR Generation Debugger

Metro-inspired step-by-step debugging of Kotlin compiler IR generation for `@Fake` annotated interfaces in the Fakt compiler plugin.

## Core Mission

This Skill performs comprehensive analysis and debugging of the IR generation pipeline, following Metro architectural patterns and validating against Kotlin compiler APIs.

## Instructions

### 1. Identify Target Interface

**Extract interface name from conversation:**
- Look for interface names in user's recent messages
- Common patterns: "debug AsyncService", "analyze UserRepository IR", "inspect IrClass for PaymentProcessor"
- If ambiguous or missing, ask: "Which interface would you like me to debug the IR generation for?"

**Validation:**
- Interface must have `@Fake` annotation
- Must be in samples/ or compiler test directories
- Verify it's an actual interface, not a class

### 2. Locate Source File

```bash
# Use Glob to find interface file
Pattern: **/*{InterfaceName}.kt

# Common locations:
- samples/kmp-single-module/src/commonMain/kotlin/**
- samples/kmp-multi-module/**/src/commonMain/kotlin/**
- compiler/src/test/kotlin/**
```

**Verify:**
- File contains `interface {InterfaceName}`
- Has `@Fake` annotation
- Check module and source set (commonMain vs commonTest)

### 3. Analyze Interface Structure

**Extract key metadata:**
- Methods (regular, suspend, with generics)
- Properties (val/var, nullable, with types)
- Type parameters (class-level vs method-level generics)
- Annotation parameters (trackCalls, etc.)

**Reference supporting docs:**
- For detailed analysis patterns → consult `resources/interface-analysis-patterns.md`
- For generic handling → consult `resources/generic-type-handling.md`

### 4. Metro Pattern Validation

**Check alignment with Metro's IrGenerationExtension:**
- Context-driven generation pattern
- Error handling with diagnostics
- API compatibility checks

**For deep Metro patterns:**
- Consult `resources/metro-patterns-reference.md`

### 5. Execute IR Generation Debug

**Follow this sequence:**

1. **FIR Phase Check**
   - Verify `@Fake` annotation detected in FIR phase
   - Check FaktFirExtensionRegistrar logs

2. **IR Phase Generation**
   - Trace UnifiedFaktIrGenerationExtension execution
   - Analyze InterfaceAnalyzer output
   - Check IrCodeGenerator for implementation class
   - Verify factory function generation
   - Validate configuration DSL generation

3. **API Validation**
   - Verify IrFactory.buildClass availability
   - Check IrPluginContext.irFactory exists
   - Confirm IrClass.isInterface property

4. **Compilation Check**
   - Locate generated file in build/generated/fakt/
   - Verify it compiles without errors
   - Check type safety preservation

### 6. Output Structured Results

**Format:**
```
🔍 DEBUGGING IR GENERATION: {InterfaceName}

📋 Interface Analysis:
- Name: {name}
- Package: {package}
- Methods: {count} ({list names})
- Properties: {count} ({list names})
- Generics: {type parameters if any}
- @Fake parameters: {annotation params}

🏗️ Metro Pattern Application:
- Metro IrGenerationExtension pattern: ✅/❌
- Context pattern (IrFaktContext): ✅/❌
- Error handling diagnostic style: ✅/❌

🔧 Kotlin API Validation:
- IrFactory.buildClass: ✅/❌
- IrPluginContext.irFactory: ✅/❌
- IrClass.isInterface: ✅/❌

⚡ Generation Pipeline:
1. FIR @Fake detection: ✅/❌
2. Implementation class: ✅/❌
3. Method implementations: ✅/❌
4. Factory function: ✅/❌
5. Configuration DSL: ✅/❌

{Status}: Generated Fake{Interface}Impl compiles successfully
📄 Output: {path to generated file}
```

### 7. Handle Error Scenarios

**Interface Not Found:**
```
❌ ERROR: Interface '{name}' not found
💡 TIP: Check interface name spelling and ensure @Fake annotation is present
```

**Generic Type Issues:**
```
🚨 WARNING: Generic type parameter <T> detected
📋 Status: Known limitation - Phase 2 generic scoping
🔧 Current approach: Dynamic casting with identity functions
📚 Details: Consult resources/generic-type-handling.md
```

**Compilation Failures:**
```
❌ ERROR: Generated code compilation failed
🔍 Issue: {specific error message}
🔧 Suggested fix: {actionable suggestion}
📚 Troubleshooting: Consult resources/troubleshooting-guide.md
```

### 8. Suggest Follow-Up Actions

Based on findings, suggest relevant actions:
- If compilation fails → offer to analyze error with compilation-error-analyzer Skill
- If Metro patterns violated → suggest validate-metro-alignment Skill
- If API issues found → suggest consult-kotlin-api Skill
- If tests needed → suggest run-bdd-tests Skill

## Supporting Files

This Skill uses progressive disclosure for detailed documentation:

- **`resources/interface-analysis-patterns.md`** - Comprehensive interface analysis techniques (loaded on-demand)
- **`resources/metro-patterns-reference.md`** - Metro IrGenerationExtension alignment guide (loaded on-demand)
- **`resources/generic-type-handling.md`** - Generic type parameter strategies (loaded on-demand)
- **`resources/troubleshooting-guide.md`** - Common IR generation issues and fixes (loaded on-demand)

## Related Skills

This Skill can compose with:
- **`kotlin-api-consultant`** - Validate Kotlin compiler APIs
- **`metro-pattern-validator`** - Check Metro alignment
- **`compilation-validator`** - Verify generated code compilation
- **`bdd-test-runner`** - Run tests after debugging

## Best Practices

1. **Always verify @Fake annotation** before attempting IR debugging
2. **Check both FIR and IR phases** - issues can occur in either
3. **Consult Metro patterns** for architectural alignment
4. **Validate compilation** of generated code as final check
5. **Follow GIVEN-WHEN-THEN** testing guidelines after fixes

## Known Limitations

- Generic type parameter scoping is a known Phase 2 challenge
- Cross-module imports may require manual validation
- Suspend function handling is fully supported but check edge cases
- KMP multi-module projects need extra validation for source sets
