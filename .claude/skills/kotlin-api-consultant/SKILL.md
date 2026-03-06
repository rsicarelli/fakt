---
name: kotlin-api-consultant
description: Queries Kotlin compiler source code for API validation, compatibility checks, breaking change detection, and best practice recommendations. Use when validating Kotlin APIs, checking compiler compatibility, analyzing API usage patterns, detecting breaking changes, or when user mentions "Kotlin API", "compiler API", "IrGenerationExtension", "IrPluginContext", "API validation", or specific Kotlin compiler class/interface names.
allowed-tools: Read, Grep, Glob, Bash, WebFetch
---

# Kotlin API Consultant

Validates Kotlin compiler API usage, detects breaking changes, and provides best practice recommendations for compiler plugin development.

## Core Mission

Provides Kotlin compiler API validation by consulting source code, detecting breaking changes, and ensuring API compatibility across Kotlin versions.

## Instructions

### 1. Identify Target API

**Extract from conversation:**
- API class/interface name from user's message
- Look for patterns: "check IrGenerationExtension", "validate IrPluginContext API"
- Common APIs: IrGenerationExtension, IrPluginContext, IrFactory, IrClass, IrTypeParameter, CompilerPluginRegistrar

**If unclear or missing:**
```
Ask: "Which Kotlin compiler API would you like me to consult?"
Suggest: IrGenerationExtension | IrPluginContext | IrFactory | CompilerPluginRegistrar | Other
```

### 2. Locate API in Kotlin Compiler Source

**Search Kotlin source on GitHub or local copy if available:**

Common package locations:
- `org.jetbrains.kotlin.backend.common.extensions` (IR extension APIs)
- `org.jetbrains.kotlin.ir.declarations` (IR tree elements)
- `org.jetbrains.kotlin.ir.expressions` (IR expressions)
- `org.jetbrains.kotlin.ir.types` (Type system)
- `org.jetbrains.kotlin.fir` (FIR APIs)
- `org.jetbrains.kotlin.compiler.plugin` (Plugin registration)

**If not found:**
```
API '${API_NAME}' not found

Suggestions:
1. Check spelling (case-sensitive)
2. Check if it's an internal/experimental API
3. Consult: https://kotlinlang.org/docs/compiler-plugins.html
```

### 3. Analyze API Definition

**Extract key information:**
- [ ] Package and imports
- [ ] Interface/class declaration
- [ ] Generic type parameters
- [ ] Method signatures
- [ ] Annotations (@UnsafeApi, @FirIncompatiblePluginAPI, @Deprecated)
- [ ] Default implementations

**Critical annotation markers:**
```kotlin
@UnsafeApi                          // May change without notice
@FirIncompatiblePluginAPI          // K1 only, not K2
@Deprecated(message = "...", level = ERROR)  // Removal planned
@ExperimentalCompilerApi           // Unstable, may change
```

### 4. Detect Breaking Changes

**Breaking change indicators:**
- Removed methods
- Changed method signatures
- Added abstract methods to interface
- Changed return types
- New required type parameters
- Deprecation with ReplaceWith (migration path)

### 5. Generate API Report

```
KOTLIN API CONSULTATION: ${API_NAME}

LOCATION:
Package: org.jetbrains.kotlin...
Module: ...

DEFINITION (Kotlin ${VERSION}):
[Interface/class definition]

STABILITY: Stable / Experimental / Deprecated
K2 COMPATIBLE: Yes / No / Partial

FAKT USAGE:
- [How Fakt uses this API]
- [Alignment status]

WARNINGS:
- [Any @UnsafeApi warnings]
- [Deprecation notices]

RECOMMENDATIONS:
1. [Recommendation]
...
```

### 6. Provide Usage Recommendations

**If API is stable:** Safe to use directly.
**If API has @UnsafeApi:** Consider abstraction layer for isolation.
**If API is deprecated:** Show migration path with replacement API.

## Supporting Files

- **`resources/api-lookup-patterns.md`** - Strategies for finding APIs
- **`resources/breaking-changes-catalog.md`** - Known breaking changes across Kotlin versions
- **`resources/api-best-practices.md`** - Best practices for compiler plugin APIs

## Related Skills

- **`compiler-architecture-validator`** - Validate architectural patterns
- **`compilation-error-analyzer`** - Debug compilation errors
- **`fakt-docs-navigator`** - Access documentation

## API Categories

| Category | Examples | Stability |
|----------|----------|-----------|
| Core IR | IrGenerationExtension, IrPluginContext | Stable |
| FIR Phase | FirExtensionRegistrar | K2 specific |
| Plugin System | CompilerPluginRegistrar | Stable |
| Type System | IrTypeParameter, IrType | Moderate |
| Experimental | Various @ExperimentalCompilerApi | Unstable |

## Validation Checklist

Before using an API in Fakt:
- [ ] API located and analyzed
- [ ] Current definition understood
- [ ] Breaking changes checked
- [ ] Deprecation status confirmed
- [ ] K1/K2 compatibility verified
