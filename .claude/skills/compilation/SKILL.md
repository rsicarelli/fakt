---
name: compilation
description: Validates compilation of generated Fakt fakes and diagnoses build failures — where fakes are generated, which path produced them, error classification, and root cause analysis. Use when compilation fails, generated code has errors, no fakes were generated, validating changes compile before committing, or checking if generated code is valid. Make sure to use this skill whenever a build error mentions generated fakes or the Fakt plugin — even if the root cause might be elsewhere, this skill's error classification pinpoints the actual source.
allowed-tools: Read, Bash, Grep, Glob
---

# Compilation Validator & Error Analyzer

Validates that generated fakes compile and diagnoses failures when they don't.

## Instructions

### 1. Publish the Plugin First

Samples consume the plugin from Maven Local, so a stale publish is the most common cause of "my change had no effect":

```bash
make publish-local
```

### 2. Build and Capture Output

Samples are separate Gradle builds, not subprojects of the root build:

```bash
./gradlew -p samples/jvm-single-module build 2>&1 | tee compilation.log
./gradlew -p samples/android-single-module build 2>&1 | tee -a compilation.log
./gradlew -p samples/kmp-single-module compileKotlinJvm --no-build-cache 2>&1 | tee -a compilation.log
./gradlew -p samples/kmp-multi-module build 2>&1 | tee -a compilation.log
```

`make test-sample` runs the KMP single-module sample end to end.

### 3. Know Which Path Generated the Fakes

- **Default (cache-correct):** `faktGenerate<Target><Compilation>` tasks (`FaktGenerateTask`) run an embedded K2 compiler in a Gradle worker; FIR checkers emit the fakes (`compiler/.../fir/generation/`).
- **Legacy in-process:** Native platform mains (`LEGACY_HYBRID`), compilations the task can't drive (`LEGACY`), or any build with `-Pfakt.useExperimentalGenerateTask=false`. Generation runs inside `compileKotlin*` via `UnifiedFaktIrGenerationExtension`.

Routing lives in `FaktGradleSubplugin.kt` (`CacheCorrectDecision`). If a failure only reproduces on one path, compare with the other by toggling the property above.

### 4. Validate Generated Files

Generated sources land in each sample's own build directory:

```bash
find samples/<name>/build/generated/fakt -name "Fake*.kt"
```

Per `@Fake` type, expect `FakeXxxImpl`, the `fakeXxx {}` factory, and `FakeXxxConfig`.

**If nothing was generated**, check:
1. The plugin is applied and `fakt { enabled }` isn't set to `false`
2. The type is annotated with `@Fake` in a main source set (`main`/`commonMain`)
3. The `faktGenerate*` task ran: `./gradlew -p samples/<name> tasks --all | grep faktGenerate`
4. Logs: set `fakt { logLevel.set(LogLevel.DEBUG) }`, or run `make debug`

### 5. Check Generated Code Against the Source

- Parameter, return, nullable and generic types (class-level, method-level, bounded) match the source; all are supported, so a mismatch is a bug
- `suspend` is preserved on overrides and behavior lambdas
- Defaults follow `codegen/strategy/`: primitives `0`/`""`/`false`, nullable `null`, collections `empty*()`, `Flow` → `emptyFlow()`, `Result<T>` → `Result.success(default)`, `Unit` → `Unit`; any other type gets a behavior that throws "requires explicit configuration"
- `FakeXxxConfig` holds behaviors as nullable `internal var`s; the fake is immutable after construction unless mutable fakes are enabled

### 6. Classify the Error

| Category | Indicators | Where to look |
|----------|-----------|---------------|
| **Plugin loading** | `CompilerPluginRegistrar` / ServiceLoader errors | `make publish-local`; `compiler/src/main/resources/META-INF/services/` |
| **FIR emission** (default) | `faktGenerate*` task or worker failure, fake missing | `compiler/.../fir/generation/` (`FirFakeEmitter`, `FirToFakeDeclarationTranslator`) |
| **IR generation** (legacy) | `IrGenerationExtension` stack traces | `ir/generation/UnifiedFaktIrGenerationExtension.kt` |
| **FIR detection** | `@Fake` validation diagnostics | `fir/FaktFirExtensionRegistrar.kt`, `fir/checkers/` |
| **Generated code** | Errors in `build/generated/fakt/...` | Read the file; fix the generator in `codegen-runtime/` |
| **Imports / cross-module** | `Unresolved reference` in generated code | `compiler/.../core/context/ImportResolver.kt`; type must be visible to the test source set |
| **Kotlin version** | `NoSuchMethodError` / `NoClassDefFoundError` on compiler classes | A compiler API missing in an older supported Kotlin — see `kotlin-api-consultant` |

Before filing a limitation as a bug, check `docs/user-guide/known-issues.md`.

### 7. Report

```
COMPILATION VALIDATION REPORT

Path: {default FaktGenerateTask | legacy in-process}
Generation: {count} files generated
Compilation: {Success/Failed} ({error_count} errors)

ERRORS:
1. [{category}] {description} — File: {path}:{line} — Fix: {solution}
```

## Related Skills

- **`bdd-test-runner`** — Run tests after successful validation
- **`kotlin-api-consultant`** — Compiler API differences across supported Kotlin versions
- **`codegen`** — Fix the generator behind a bad generated file
