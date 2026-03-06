---
name: feature-option
description: Guides step-by-step through all 11 touchpoints for adding a new @Fake annotation option (like mutability or callHistory). Covers annotation enum, CLI processor, Gradle plugin, FIR metadata, IR bridge, analysis models, and code generators. Use when adding a new option to @Fake, adding a new enum to the annotation, making something configurable per-interface, implementing a new compiler plugin feature flag, adding a new Gradle extension property, or extending the three-tier resolution (annotation → plugin → default). Make sure to use this skill whenever a change touches more than one layer of the option pipeline, even for small additions like a new enum value.
allowed-tools: Read, Grep, Glob, AskUserQuestion
---

# Feature Option Guide

Adds a new option to the `@Fake` annotation, following the confirmed pattern from `mutability` and `callHistory`.

## Instructions

### 1. Gather Option Details

Use `AskUserQuestion` to collect:
- **Option name** (e.g., `threadSafety`)
- **Enum values** — always `DEFAULT` plus two semantically opposite values (e.g., `ENABLED`/`DISABLED`)
- **Plugin-level default** — the boolean value when annotation says `DEFAULT` (e.g., `true` or `false`)
- **What it controls** — which generators are affected

### 2. Walk Through All 11 Layers

Guide the implementation in this exact order. Each layer depends on the previous.

---

> **Layers 1-2** define the user-facing API that developers interact with — the enum they import and the annotation parameter they set.

#### Layer 1 — Annotation Enum (new file)

**File**: `annotations/src/commonMain/kotlin/com/rsicarelli/fakt/{OptionName}Mode.kt`

```kotlin
public enum class {OptionName}Mode {
    DEFAULT,   // Follow plugin-level setting
    {VALUE_A}, // Explicit enable
    {VALUE_B}, // Explicit disable
}
```

Pattern: `MutabilityMode(DEFAULT, IMMUTABLE, MUTABLE)`, `CallHistoryMode(DEFAULT, ENABLED, DISABLED)`.

#### Layer 2 — @Fake Annotation Parameter

**File**: `annotations/src/commonMain/kotlin/com/rsicarelli/fakt/Fake.kt`

Add parameter with `DEFAULT` default:

```kotlin
public annotation class Fake(
    val callHistory: CallHistoryMode = CallHistoryMode.DEFAULT,
    val mutability: MutabilityMode = MutabilityMode.DEFAULT,
    val {optionName}: {OptionName}Mode = {OptionName}Mode.DEFAULT, // NEW
)
```

> **Naming note**: Real field names are descriptive and may not follow the template literally. E.g., mutability uses `enableMutableFakes` (not `enableMutability`) and `generateMutableBehaviors` (not `generateMutability`). Choose names that best describe the feature's effect.

> **Layers 3-4** wire the option through the Gradle build system to the compiler — this is how users set project-wide defaults in `build.gradle.kts`.

#### Layer 3 — Gradle Plugin Extension

**File**: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/FaktPluginExtension.kt`

Add `Property<Boolean>` with convention default:

```kotlin
public val enable{OptionName}: Property<Boolean> =
    objects.property(Boolean::class.java).convention({defaultValue})
```

#### Layer 4 — Gradle Subplugin (passes option to compiler)

**File**: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/FaktGradleSubplugin.kt`

In `applyToCompilation`, add `SubpluginOption`:

```kotlin
add(SubpluginOption(key = "enable{OptionName}", value = extension.enable{OptionName}.get().toString()))
```

> **Layers 5-6** register the option with compiler plugin infrastructure — the CLI processor receives the value from Gradle, and FaktOptions makes it available throughout the compiler.

#### Layer 5 — Compiler CLI Processor

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/core/config/FaktCommandLineProcessor.kt`

Three additions in companion object + one in `processOption`:

```kotlin
// Companion:
val ENABLE_{OPTION}_KEY = CompilerConfigurationKey<Boolean>("fakt.enable{OptionName}")
val ENABLE_{OPTION}_OPTION = CliOption(
    optionName = "enable{OptionName}",
    valueDescription = "true|false",
    description = "Enable {description} by default (default: {defaultValue})",
    required = false,
)

// Add to pluginOptions list:
ENABLE_{OPTION}_OPTION

// In processOption when-branch:
"enable{OptionName}" -> configuration.put(ENABLE_{OPTION}_KEY, value.toBoolean())
```

#### Layer 6 — FaktOptions Data Class

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/core/config/FaktOptions.kt`

Add field + read in `load()`:

```kotlin
data class FaktOptions(
    // ...existing...
    val enable{OptionName}Default: Boolean = {defaultValue},
)

// In load():
val enable{OptionName} = configuration.get(FaktCommandLineProcessor.ENABLE_{OPTION}_KEY) ?: {defaultValue}
return FaktOptions(/* ...existing... */, enable{OptionName}Default = enable{OptionName})
```

> **Layers 7-8** extract the annotation value at compile time during the FIR phase — mirror enums avoid a runtime dependency on the annotations module, and checkers read the actual `@Fake` parameter value from source code.

#### Layer 7 — FIR Mirror Enum + Validated Metadata

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/metadata/FirFakeMetadata.kt`

```kotlin
// Mirror enum (avoids runtime dep on annotations):
enum class Fir{OptionName}Mode { DEFAULT, {VALUE_A}, {VALUE_B} }

// Add to BOTH ValidatedFakeInterface and ValidatedFakeClass:
val {optionName}Mode: Fir{OptionName}Mode = Fir{OptionName}Mode.DEFAULT,
```

#### Layer 8 — FIR Checkers (annotation extraction)

**Files**: `compiler/.../fir/checkers/FakeInterfaceChecker.kt` + `FakeClassChecker.kt`

Add private extraction method in both:

```kotlin
private fun extract{OptionName}Mode(declaration: FirClass, session: FirSession): Fir{OptionName}Mode {
    val fakeAnnotation = declaration.annotations.find { annotation ->
        annotation.toAnnotationClassIdSafe(session) == FAKE_ANNOTATION_CLASS_ID
    } ?: return Fir{OptionName}Mode.DEFAULT

    val arg = fakeAnnotation.argumentMapping.mapping.entries
        .find { it.key.asString() == "{optionName}" }?.value

    return when (arg) {
        is FirPropertyAccessExpression -> {
            val enumEntry = arg.calleeReference.toResolvedEnumEntrySymbol()
            when (enumEntry?.name?.asString()) {
                "{VALUE_A}" -> Fir{OptionName}Mode.{VALUE_A}
                "{VALUE_B}" -> Fir{OptionName}Mode.{VALUE_B}
                else -> Fir{OptionName}Mode.DEFAULT
            }
        }
        else -> Fir{OptionName}Mode.DEFAULT
    }
}
```

Call in `analyzeMetadata()` and pass to `ValidatedFakeInterface`/`ValidatedFakeClass` constructor.

> **Layers 9-10** bridge the FIR metadata to code generation in the IR phase — the resolver function implements the three-tier resolution (annotation → plugin → default), and analysis models expose the final resolved boolean that generators consume.

#### Layer 9 — IR Generation Metadata Bridge

**File**: `compiler/.../ir/transform/IrGenerationMetadata.kt`

Three additions:

**a)** Field in `IrGenerationConfig`:
```kotlin
val {optionName}Mode: Fir{OptionName}Mode = Fir{OptionName}Mode.DEFAULT,
```

**b)** Accessor on `IrGenerationMetadata` and `IrClassGenerationMetadata`:
```kotlin
val {optionName}Mode: Fir{OptionName}Mode get() = config.{optionName}Mode
```

**c)** Resolver function + usage in `toInterfaceAnalysis()` and `toClassAnalysis()`:
```kotlin
private fun resolve{OptionName}Enabled(
    annotationMode: Fir{OptionName}Mode,
    pluginDefault: Boolean,
): Boolean = when (annotationMode) {
    Fir{OptionName}Mode.{VALUE_A} -> true
    Fir{OptionName}Mode.{VALUE_B} -> false
    Fir{OptionName}Mode.DEFAULT -> pluginDefault
}
```

#### Layer 10 — Analysis Models (final boolean)

**File**: `compiler/.../ir/analysis/AnalysisModels.kt`

Add resolved boolean to both `InterfaceAnalysis` and `ClassAnalysis`:

```kotlin
val generate{OptionName}: Boolean = {defaultValue},
```

> **Layer 11** is where the option actually affects generated code — generators read the resolved boolean and conditionally emit different Kotlin source.

#### Layer 11 — Code Generators (consume the boolean)

Modify generators based on what the option controls:

- **`ImplementationGenerator.kt`** — pass `analysis.generate{OptionName}` to `generateCompleteFake()`
- **`ConfigurationDslGenerator.kt`** — conditionally generate DSL elements
- **`FakeGenerator.kt`** / extensions — conditional code generation

### 3. Validate Implementation

Checklist:
- [ ] Enum file created in `annotations/` with `DEFAULT` + two values
- [ ] `@Fake` annotation has new parameter with `DEFAULT` default
- [ ] Gradle extension has `Property<Boolean>` with convention
- [ ] Subplugin passes option via `SubpluginOption`
- [ ] CLI processor has KEY, OPTION, pluginOptions entry, processOption branch
- [ ] `FaktOptions` has field + `load()` reads it
- [ ] FIR mirror enum + fields in both `ValidatedFakeInterface` and `ValidatedFakeClass`
- [ ] Both checkers have `extract{OptionName}Mode()` + call in `analyzeMetadata()`
- [ ] IR metadata has config field, accessor, resolver function
- [ ] Both `toInterfaceAnalysis()` and `toClassAnalysis()` pass resolved boolean
- [ ] `InterfaceAnalysis` and `ClassAnalysis` have `generate{OptionName}: Boolean`
- [ ] Generators consume the boolean
- [ ] `make publish-local && make test-sample` passes
- [ ] `make format` applied

### 4. Three-Tier Resolution Pattern

Every option follows this resolution:

```
Annotation value (per-interface)
    ↓ if DEFAULT
Plugin setting (build.gradle.kts)
    ↓ if not set
Hardcoded default (in FaktOptions)
```

This allows: global default via Gradle, per-interface override via annotation, sensible fallback.

## Related Skills

- `workflow` — full development cycle for implementing the feature
- `compiler-architecture-validator` — validate FIR/IR separation
- `codegen` — understand code generation pipeline for Layer 11
