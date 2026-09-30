---
name: kotlin-api-consultant
description: Checks Kotlin compiler API usage against the Kotlin versions Fakt supports — which path runs on which compiler, how compat shims are written, how to verify across versions, and how to bump Kotlin. Use when calling a new FIR/IR/compiler-plugin API, seeing NoSuchMethodError, NoSuchFieldError or NoClassDefFoundError from compiler classes, bumping the Kotlin version, or checking whether an API exists in the minimum supported Kotlin. Make sure to use this skill whenever compiler plugin code starts using a Kotlin compiler API it didn't use before — these APIs change between Kotlin releases and a break only shows up on an older supported version.
allowed-tools: Read, Grep, Glob, Bash, WebFetch
---

# Kotlin API Consultant

Keeps Fakt's compiler plugin working across every Kotlin version in the support matrix.

## Instructions

### 1. Know Which Compiler Runs the Code

- **Supported range:** the matrix in `docs/compatibility.md` is the source of truth (minimum and latest tested).
- **Build:** `compiler/` compiles `compileOnly` against `kotlin-compiler-embeddable` at the `kotlin` version in `gradle/libs.versions.toml`.
- **Default FIR path** (`FaktGenerateTask` worker): runs on the compiler Fakt pins — `FAKT_KOTLIN_VERSION` in `gradle-plugin/.../FaktGradleSubplugin.kt`. The user's Kotlin version doesn't matter here.
- **Legacy in-process path** (Native mains, `LEGACY`, or `-Pfakt.useExperimentalGenerateTask=false`): runs inside the user's own compiler, which can be as old as the minimum supported version.

So code reachable from the in-process path (the `ir/` package, `FaktCompilerPluginRegistrar`, and the FIR checkers it registers) must only use APIs present in every supported version.

### 2. Check the API Across the Range

Look the API up at the minimum and latest supported versions in the Kotlin repository (tags like `v2.2.0`), not just the version on the build classpath. Signs of trouble:

- The symbol doesn't exist at the minimum version, or its signature differs
- `@Deprecated(level = ERROR)`, or a replacement API introduced after the minimum
- An opt-in annotation (`@ExperimentalCompilerApi`, `@UnsafeApi`) the module doesn't already opt into

### 3. Bridge Differences with a Compat Shim

When the API differs across the range, isolate it in a small private helper suffixed `Compat`, next to its call site. Existing patterns:

- `FaktCompilerPluginRegistrar.registerExtensionCompat` — calls `registerExtension` reflectively; Kotlin 2.4.0 widened its receiver type, and a direct call compiled against 2.4 fails with `NoClassDefFoundError` on 2.2–2.3
- `FirToIrTransformer.extensionReceiverParameterCompat` — reads the extension receiver from the unified `parameters` list instead of the deprecated `extensionReceiverParameter`

Keep the shim's KDoc explaining which versions differ, so it can be deleted when the minimum moves past them.

### 4. Verify on Every Supported Version

```bash
make test-compat-all          # publish-local + jvmTest on every samples/compat/kotlin-*/
make test-compat-2.2.0        # one version
```

CI runs the same matrix (version list in `.github/workflows/development.yml`) through `.github/actions/test-compat-samples`.

### 5. Bumping Kotlin

Update `kotlin` in `gradle/libs.versions.toml` **and** `FAKT_KOTLIN_VERSION` (nothing checks that they match), add a `samples/compat/kotlin-<version>/` sample and its entry in `development.yml`, and update `docs/compatibility.md`.

## Related Skills

- **`compilation`** — Diagnose the build failure that surfaced the API problem
- **`codegen`** — Code generation, which doesn't touch compiler APIs
