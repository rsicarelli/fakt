# v1.0 Legacy Sunset: Handover

**Status:** In progress. **Live checklist, order and open decisions: GitHub issue #150** (not here).
**Last Updated:** 2026-10-07 (#162 intermediate source sets)
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
| shared, a metadata compilation exists (`commonMain`, `webMain`, custom intermediates over several platform types) | METADATA producer (`faktGenerateMetadata<Set>`; an intermediate analyses its own sources and gets its ancestors' metadata klibs plus `-Xrefines-paths`) |
| shared, no metadata compilation (all targets JVM-typed), or an intermediate compiled only by JVM and Android-JVM (KGP disables its metadata compilation) | synthetic K2JVM producer on a deterministic representative compilation (`faktGenerate<Set>`, e.g. `faktGenerateDesktopAndServerMain`) |
| shared by one compiler only (for example a `sharedJvmMain` used only by `jvm`) | that target's consumer; its route tokens include every such ancestor |
| shared native (`nativeMain`, `appleMain`) | Native driver (#152) |
| all-JS or all-Wasm intermediate | not generated yet (follow-up) |

- The owned set is the key set of the route map (`SourceSetContext.outputDirectories`); empty
  means the old behaviour (emit everything analysed).
- `SourceSetContext.outputDirectories` is an explicit `sourceSet → outputDir` map. It replaced the
  removed `emitSourceSets` / `commonOutputDirectory` fields (and the `testCounterpartDirectory`
  string rewrites of `/commonTest/`, which break on absolute paths) on the FIR path.
- Test wiring (#164b) is **by association**, not by name. A test compilation receives the fakes of
  the main compilation it is associated with (`associatedCompilations`, read lazily inside a
  Callable, so associations added in a later `afterEvaluate` count). The generated dir is added as
  a source dir of the test compilation's source set and the compile task depends on the producer.
  - `testDirOwnerOf` (the owner registry) is the single owner rule: the plain `*Test` dir of a
    source set is not registered at all when the registry names an owner for it (an owned test
    set gets no plain dir). `commonTest` is folded into that registry; there is no separate
    common-test property or consumer-name function any more.
  - Variant and compilation names are matched **exactly**. A name that merely contains another
    (`debugMinified` vs `debug`, `preRelease` vs `release`) never receives its neighbour's fakes.
    Exact parsing is kept only for `testFixtures` and for compile tasks without a Kotlin
    compilation or association data (AGP 9 built-in Kotlin falls back to the exact name).
  - A late-associated custom compilation receives the fakes of the PLATFORM producer it is
    associated with (the target's own source sets). Intermediate fakes are wired when the
    intermediate producer is registered, so later associations are missed. `commonMain` fakes
    reach only `commonTest`; a custom compilation sees them only if its source set depends on
    `commonTest`.
  - Known limit: the graph reader decides "test-like" by association + name at the time it runs,
    so a custom compilation without "test" in its name that is associated late is main in early
    reads and test-like later (rare).
  - Known limit: intermediate-producer leaf wiring is still eager (D4).
  - Proofs: `samples/kmp-all-jvm` (`serverIntegrationTest`, associated late), and
    `samples/android-single-module` (`debugMinified`, `preRelease`; CI only).
- Ownership covers intermediate source sets too (#162, done for `webMain` and all-JVM
  intermediates). When the analysed source sets have an intermediate level the worker passes
  `-Xfragments` / `-Xfragment-sources` / `-Xfragment-refines` instead of lumping everything into
  `-Xcommon-sources`; the metadata driver rejects fragments and gets `-Xrefines-paths` instead.
- Known limits: dependsOn edges that user code adds in a later `afterEvaluate` are missed. On
  Kotlin < 2.2.20 `webMain` is not in the default hierarchy, so declare it manually with
  `dependsOn`. All-JS / all-Wasm intermediates are still not generated. Native intermediates are generated
  in-process (LEGACY_HYBRID), not cache-correct, until #152. A synthetic intermediate with an
  `expect` in the intermediate itself and an empty `commonMain` gets no fragments (flat shape):
  its errors are tolerated, but `forbid-tolerated` would fail.
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
- **Spike result (Sept 2026): all 8 criteria pass**, on Linux and on macOS (arm64), with Kotlin
  2.2.0 and 2.4.10. D1 is not needed. Full report on #152. The throwaway prototype (behind
  `-Pfakt.spike.native=true`) and the harness live on branch `ccr-aa1d21c5-gqlrfs`, in
  `spike/native/`: `REPORT.md`, `FINDINGS.md`, `bin/`, `fixture/`.
- **What the implementation (item 7) must do**, learned from the prototype:
  - **Leaf native main** → `NATIVE` consumer, routed like JS/Wasm: own source set emitted,
    ancestors passed as `-Xcommon-sources`, `-target=<konanTarget>`.
  - **`KotlinSharedNativeCompilation`** (`nativeMain`, `appleMain`, `iosMain`, …) → `NATIVE`
    producer that owns its source set. Today these compilations are `SUPPRESS`ed, so a
    leaf-only driver would silently drop every shared-native fake. Arguments, mirroring KGP:
    - `-produce library -Xmetadata-klib -no-default-libs -nostdlib`;
    - `-library` = the distribution stdlib + the compilation's dependency files, which already
      include the commonized klibs;
    - `-Xcommon-sources=<own>`;
    - **`-Xrefines-paths=<ancestor metadata klibs>`**. Without it, `actual`s in `nativeMain`
      have no `expect`;
    - `-target` = the host target if covered, else the first (KGP's choice).
  - **#165 first:** without the compilation's `-opt-in` values, every cinterop signature fails
    analysis.
  - **Worker:**
    - a separate fork whose classpath is the distribution's
      `kotlin-native-compiler-embeddable.jar` (self-contained on 2.2.0 and 2.4.10; ignore 2.2.0's
      extra `kotlin-native.jar`);
    - `systemProperty("konan.home", …)`;
    - `konanHome` `@Internal`; `kotlinNativeVersion` and `konanTarget` `@Input`.

    1g heap handles 10,000 fakes in a single source set (about 46 s, 1.2 GB RSS).
  - **Distribution lookup:** `kotlin.native.home`, then `konanDataDir` / `KONAN_DATA_DIR` /
    `~/.konan` + `kotlin-native-prebuilt-<host>-<ver>`. The version comes from
    `kotlin.native.version` or `getKotlinPluginVersion()`.
  - **Provisioning:**
    - KGP 2.4.10 provisions in a configuration-time `ValueSource` **and** in
      `:downloadKotlinNativeDistribution`;
    - KGP 2.2.0 has only the `ValueSource`.

    Either way it is done before tasks run. Don't depend on the task: it also downloads the LLVM
    toolchain, which Fakt never reads.
  - **CI:** the `kmp-multi-target` cell moves from `FAKT_PRESENCE_*` to `producers` /
    `cached-fakes` with `faktGenerateMetadataNativeMain` and `faktGenerateMetadataIosMain`. This
    was proven on the branch: 6/6 FROM-CACHE.
- **Found by the spike, tracked separately:**
  - #171: Kotlin < 2.4 can't read the published annotation klibs (klib ABI 2.4.0).
  - #172: `@OptIn` is copied into fakes without its import.
  - #173: the unread producer `FirMetadataCache` is rewritten after every interface, which is
    quadratic. It is removed by #164a.

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
- **Spotless and the configuration cache:** if `spotlessCheck` fails with "Add a step with
  [com.facebook:ktfmt:…] into the `spotlessPredeclare` block", rerun it with
  `--no-configuration-cache`.
- **JS/Wasm browser tests** need npm, which the cloud sandbox blocks. CI runs them.
- **Signed commits:** the session's stop hook flags unsigned commits. Plumbing such as `commit-tree`
  produces them; fix with `git rebase --exec "git commit --amend --no-edit --reset-author"`.

## 8. Sandbox and CI facts

- **No Android SDK** in the cloud sandbox: every Android cell (`android-single-module`,
  `android-test-fixtures`, compat-agp) runs only in CI. A new Android task name is confirmed by
  the first CI log, not locally.
- **No npm:** JS/Wasm browser tests are CI-only.
- **The memory cgroup kills Gradle daemons.** Run TestKit classes in small groups with
  `--max-workers=2`, `./gradlew --stop` between groups, never two Gradle builds at once, and kill
  orphan test JVMs by PID. A `limit-parallel` init script (capping `maxParallelForks` and workers)
  keeps a run inside the limit.
- **CI runs samples with the configuration cache ON.** Never validate a sample with
  `--no-configuration-cache`: a PR already failed in CI because it passed locally only that way.
  Run the sample's exact CI command; the second run must print `Configuration cache entry reused`.
  The two contract scripts add `--no-configuration-cache` themselves where they need it.
- **Spotless** needs `--no-configuration-cache` (see the gotcha above).
- **Run Tests job:** under runner load TestKit tests can hit the 5-minute timeout. One re-run of
  the job is acceptable.
