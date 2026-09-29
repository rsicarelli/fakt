# v1.0 Legacy Sunset: Handover

**Status:** In progress. **Live checklist, order and open decisions: GitHub issue #150** (not here).
**Last Updated:** September 2026
**Delete when:** 1.0 ships (fold the surviving design notes into `architecture/ARCHITECTURE.md`).

This file holds the *durable* context a fresh session needs: what the end state is, why the
decisions were made, and how to verify work locally. It deliberately has no checkboxes. If you are
looking for "what next", open #150.

---

## 1. End state

- One generation path: `FaktGenerateTask` → Gradle Worker → embedded K2 (`kotlin-compiler-embeddable`,
  `FaktGradleSubplugin.FAKT_KOTLIN_VERSION = 2.4.10`) with the Fakt compiler jar as `-Xplugin`.
- Fakes are emitted by the **FIR checkers** (`compiler/.../fir/generation/`) as declared task outputs,
  so they are cache-correct and relocatable.
- Gone: the in-process plugin inside `compileKotlin*` (IR emission, `compiler/.../ir/`),
  `EmitPhase`, and `fakt.useExperimentalGenerateTask`.
- Routing today: `FaktGradleSubplugin.cacheCorrectDecision`. Anything that returns `LEGACY` or
  `LEGACY_HYBRID` is a shape that still needs closing.

## 2. Key code on the task path

| Concern | Location |
|---|---|
| Routing decision | `gradle-plugin/.../FaktGradleSubplugin.kt` (`cacheCorrectDecision`) |
| Task + worker | `gradle-plugin/.../FaktGenerateTask.kt`, `worker/FaktCodegenWorkAction.kt` |
| Task wiring | `gradle-plugin/.../FaktGenerateTaskWiring.kt` |
| Android seams | `gradle-plugin/.../android/AndroidIntegration.kt`, `android/AndroidVariantSources.kt` |
| FIR emission | `compiler/.../fir/generation/FirToFakeDeclarationTranslator.kt` |
| Codegen (shared) | `codegen-runtime/.../codegen/generator/` |

## 3. Decisions and rationale

### 3.1 AGP API floor 8.11.1

`gradle-plugin` compiles AGP against `libs.agp.api.floor` (`compileOnly`, 8.11.1), not the AGP that
build-logic uses. Every AGP symbol referenced must exist in the floor. Behaviour that only newer AGP
has is reached through `android/AndroidIntegration`. Runtime proof:
`samples/compat-agp/agp-{8.11,8.12,9.0,9.4}`.

### 3.2 KMP `androidTarget()` stays in-process until #163

It is compiled per variant (`debug`/`release`), so a single producer is wrong. It is routed `LEGACY`
via `unreadableSourcesReason`. There is a pre-existing bug even in-process: `androidMain` fakes land
in `generated/fakt/androidTest` and never reach `androidUnitTest`. #163 fixes both.

### 3.3 One PR per issue

Each PR ships a sample and a CI cell (`test-samples`, plus `test-cache-correctness` with
`producers`/`cached-fakes`). That way the legacy path never loses a shape the task path hasn't
proven.

### 3.4 Source-set ownership (#160 → #162 → #163)

A pure function assigns every **main** source set exactly one owner:

| Source set | Owner |
|---|---|
| target-exclusive | that target's main compilation(s), one per Android variant |
| shared, a metadata compilation exists | METADATA producer |
| shared, no metadata compilation (all targets JVM-typed) | synthetic K2JVM producer on a deterministic representative compilation |
| shared native (`nativeMain`, `appleMain`) | Native driver (#152) |

- `emitSourceSets` = the owned set.
- `SourceSetContext` gets an explicit `sourceSet → outputDir` map. It replaces
  `commonOutputDirectory` / `testCounterpartDirectory`, which does string rewrites of `/commonTest/`
  and breaks on absolute paths.
- Test wiring: the generated dir goes to the **lowest** matching `*Test` source set.
- Why: #160 (no owner), #162 (intermediates routed SUPPRESS, consumers treat ancestors as
  analysis-only) and #163 (per-variant) are one class of bug. A pure function can be unit-tested
  without Gradle.

### 3.5 Native: approach A (K2Native driver), #152

- The K/N distribution (`kotlin-native-prebuilt`, under `~/.konan`) ships
  `konan/lib/kotlin-native-compiler-embeddable.jar`. It contains
  `org.jetbrains.kotlin.cli.bc.K2Native`, a CLICompiler with the same `exec` surface as the JVM/JS
  drivers.
- Plan: a `NATIVE` `CompilerDriver`.
  - The worker fork gets `-Dkonan.home`.
  - Args: `-produce library -Xmetadata-klib`.
  - Shared-native uses KGP's shared-native metadata compilation with commonized klibs.
  - The Linux distribution includes the ios/macos platform klibs.
- Rejected: the metadata compiler over native klibs, because it can't resolve cinterop forward
  declarations (`cnames.structs.*`).
- **Spike exit criteria** (all must pass):
  1. A leaf `linuxX64Main` fake using `platform.posix` is byte-identical to the in-process output.
  2. `nativeMain`/`appleMain` work via `-Xmetadata-klib`, including iOS forward declarations.
  3. User cinterop klibs work.
  4. The distribution is located without KGP internals and offline. The spike also settles whether
     provisioning happens at configuration time or in a task.
  5. `~/.konan/dependencies` is never accessed.
  6. The cache key is relocatable and configuration-cache-safe.
  7. It works on Kotlin 2.2.0 and 2.4.10.
  8. Heap use and run time are acceptable.
- If criterion 2 or 4 fails, the maintainer decides between a Native-only in-process fallback and
  declaring Native unsupported in 1.0 (#150, D1).

## 4. Known gaps that are not shape-specific

Each one is tracked in #150 "Up next". Why each matters:

- **Compiler options and other compiler plugins are not forwarded to the worker** (#165).
  - `FaktCodegenWorkAction.kt` sets only free args, classpath and module name.
  - Main code that needs any of these fails `check(exit == OK)`, a regression versus in-process:
    `-Xcontext-parameters`, optIn, explicit API, `-Xjdk-release`, language/API version.
  - Code that relies on other compiler plugins breaks the same way: serialization `serializer()`,
    Parcelize, FIR-generating DI plugins.
- **Worker version pin** (#166). 2.4.10 can't read metadata from a newer user Kotlin.
- **CI is ubuntu-only** (#167). There is no Windows run (path bugs) and no macOS/Apple coverage.
- **Collector mode** (`FakeCollectorTask`) reads other projects' tasks, which breaks Isolated
  Projects (#168). It ships in 1.0.
- **Worker heap** is fixed at 1g (`FaktGenerateTask.kt`, `WORKER_MAX_HEAP`). Tracked in #164.

## 5. Removal ordering constraints

- `getPluginArtifact` is abstract and resolved by KGP for every compilation. It can't just be
  deleted (#150, D3).
- `SourceSetConfigurator.configureKmpTestSourceSetDirs` runs on the **default** path too
  (`FaktGradleSubplugin.kt` ~397). It needs replacement wiring for native and intermediate test sets
  first.
- **Keep** `SourceSetContext.outputDirectory`. The worker uses it.
- Convert `FirIrEmissionParityTest` to golden snapshots **before** deleting `ir/`.
- Move `FAKT_PRESENCE_*` checks only after #152. Delete the flag last.

## 6. Local verification

```bash
export ANDROID_HOME=/root/android-sdk   # cloud sandbox
./gradlew spotlessApply spotlessCheck :gradle-plugin:detekt apiCheck :gradle-plugin:test publishToMavenLocal
./gradlew :compiler:test :compiler-api:test            # if compiler/ or compiler-api/ changed
./gradlew -p samples/<kmp-sample> allTests --continue && ./gradlew -p samples/<kmp-sample> check
FAKT_CACHED_FAKES="FakeXImpl ..." ./.github/scripts/cache-correctness-check.sh samples/<sample> <producer...>
./.github/scripts/clean-rebuild-cache-check.sh samples/<sample> <consumerTask>
(cd samples/compat-agp/agp-9.4 && ./gradlew testDebugUnitTest --no-daemon)   # own wrapper
```

Make equivalents: `make publish-local`, `make validate`, `make test-kmp-single-target`,
`make test-compat-agp-9.4`, `make test-clean-rebuild-cache`, `make test-kmp-android-lint`.

## 7. Gotchas

- **Own wrappers:** `samples/compat-agp/agp-*` and `samples/kmp-android-lint` pin their own Gradle.
  `cd` into them instead of using root `./gradlew -p`.
- **ProjectBuilderWarmUp:** gradle-plugin tests run in parallel.
  - `ProjectBuilderImpl`'s static initializer races SLF4J and poisons the class, so later tests fail
    with `NoClassDefFoundError`.
  - `helpers/ProjectBuilderWarmUp` (a JUnit `LauncherSessionListener`, registered in
    `META-INF/services`) initialises it first. Keep it registered, and don't "fix" the error by
    serializing tests.
- **apiCheck:** run the root `apiCheck`, which covers `gradle-plugin.api` and `compiler-api.api`.
  Use `apiDump` only for intended changes.
- **Detekt** uses the stock thresholds (`buildUponDefaultConfig`, with an optional per-module
  `detekt-baseline.xml`).
  - LongMethod 60, LargeClass 600, TooManyFunctions 11, CyclomaticComplexMethod 15,
    NestedBlockDepth 4, ReturnCount 2.
  - Extract functions or files instead of growing the baseline.
- **Maven 429s** in sample runs are rate limits, not regressions. Retry, run samples one at a time,
  or use `--offline` once the caches are warm.
- **JS/Wasm browser tests** need npm, which the cloud sandbox blocks. CI runs them.
- **Signed commits:** the session's stop hook flags unsigned commits. Plumbing such as `commit-tree`
  produces them; fix with `git rebase --exec "git commit --amend --no-edit --reset-author"`.
