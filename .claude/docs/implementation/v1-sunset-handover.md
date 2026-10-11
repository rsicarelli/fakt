# v1.0 Legacy Sunset: Handover

**Status:** In progress. **Live checklist, order and open decisions: GitHub issue #150** (not here).
**Last Updated:** 2026-10-11 (#152 Native on the task path)
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
- Routing today: `FaktGradleSubplugin.cacheCorrectDecision`. `LEGACY_HYBRID` and
  `wireLegacyHybridOrdering` are deleted. `LEGACY` remains only for the explicit opt-out
  (`fakt.useExperimentalGenerateTask=false`) and AGP 9 built-in Kotlin without a hook.

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

### 3.2 KMP `androidTarget()` is generated per variant (#163)

Kotlin compiles `androidTarget()` per Android variant (`debug`, `release`, flavors), so one producer
is wrong. Each variant compilation gets its own producer (`faktGenerateAndroidDebug`,
`faktGenerateAndroidFreeDebug`, ...) that analyses the variant's member source sets
(`[default set, androidMain, flavor, build type]`) and emits the `androidMain` fakes into that
variant's own output directory, so a type declared only in `androidDebug`/`androidRelease` can appear
in an `androidMain` fake. Unit and instrumented test compilations receive the fakes of the variant
they are associated with; the shared `androidUnitTest`/`androidInstrumentedTest` sets get no
directory. In single-target Android there is no `commonMain` metadata compilation: a synthetic
`faktGenerateCommonMain` on the debug-preferred variant emits it into `commonTest`.
The in-process legacy path used to put `androidMain` fakes in `generated/fakt/androidTest`, where
`androidUnitTest` never saw them; that only remains with `fakt.useExperimentalGenerateTask=false`
(removed in 1.0). `com.android.kotlin.multiplatform.library` reports platform type `jvm`, was already
on the task path and is untouched. Proof: `samples/kmp-android-target` (CI sample, cache and
clean-rebuild cells). Android `compileDependencyFiles` contain `.aar` files the worker cannot read, so for androidJvm
compilations `workerDependencyFiles()` (VariantCompilations.kt) keeps the jars and adds the
`android-classes-jar` artifact view; `AarTypeSource` in the sample (androidx `core`) proves it.

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
| shared native (`nativeMain`, `appleMain`, `iosMain`, `linuxMain`, `commonMain` of an all-native project) | Native driver producer (#152) |
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
  - Member sets (#163): an Android variant owns every source set of its compilation (`androidMain`,
    flavor and build-type sets included), each emitted into that variant's own directory only. No
    per-variant directory goes into `androidUnitTest`, `androidInstrumentedTest` or `commonTest`.
    Associations, `dependsOn` edges and the metadata `commonMain` compilation do not exist yet at the
    first `applyToCompilation` of an Android variant, so they are read lazily (inside a Callable).
  - Proofs: `samples/kmp-all-jvm` (`serverIntegrationTest`, associated late),
    `samples/android-single-module` (`debugMinified`, `preRelease`) and
    `samples/kmp-android-target` (variants, flavors, single target).
- Ownership covers intermediate source sets too (#162, done for `webMain` and all-JVM
  intermediates). When the analysed source sets have an intermediate level the worker passes
  `-Xfragments` / `-Xfragment-sources` / `-Xfragment-refines` instead of lumping everything into
  `-Xcommon-sources`; the metadata driver rejects fragments and gets `-Xrefines-paths` instead.
- Known limits: dependsOn edges that user code adds in a later `afterEvaluate` are missed. On
  Kotlin < 2.2.20 `webMain` is not in the default hierarchy, so declare it manually with
  `dependsOn`. All-JS / all-Wasm intermediates are still not generated. A synthetic intermediate with an
  `expect` in the intermediate itself and an empty `commonMain` gets no fragments (flat shape):
  its errors are tolerated, but `forbid-tolerated` would fail.
- Why: #160 (no owner), #162 (intermediates routed SUPPRESS, consumers treat ancestors as
  analysis-only) and #163 (per-variant) are one class of bug. A pure function can be unit-tested
  without Gradle.

### 3.5 Native: K2Native driver (#152), done in item 7

Native runs on the cache-correct task path. Code: `gradle-plugin/.../NativeDistribution.kt`,
`NativeTaskWiring.kt`, `CompilationRouting.kt`, `worker/NativeArguments.kt`, `WorkerForkPlan.kt`.

- **Approach.** The K/N distribution ships `konan/lib/kotlin-native-compiler-embeddable.jar` with
  `org.jetbrains.kotlin.cli.bc.K2Native`, a CLICompiler with the same `exec` surface as the JVM/JS
  drivers. A `NATIVE` `CompilerDriver` runs it. Rejected: the metadata compiler over native klibs,
  because it can't resolve cinterop forward declarations (`cnames.structs.*`).
- **Routing.**
  - Multi-target leaf native mains are consumers: K2Native, a **full klib** (not
    `-Xmetadata-klib`), fragments for the ancestors.
  - Every shared-native compilation (`nativeMain`, `appleMain`, `iosMain`, `linuxMain`, and
    `commonMain` of an all-native project) is a producer on the Native driver. It emits a metadata
    klib with `-Xrefines-paths=<ancestor metadata klibs>`; without it `actual`s in `nativeMain` have
    no `expect`.
  - Single-target Native is a single-target task.
  - Native test sets are wired by the owner registry: an owned set gets no plain
    `build/generated/fakt/<set>/kotlin` directory.
- **Worker.** A Native task forks on the user's own distribution:
  `kotlin-native-compiler-embeddable.jar` as classpath and `-Dkonan.home`. So it runs the user's
  K/N version, not `FAKT_KOTLIN_VERSION`. Heap is still the fixed 1g (#164c); 10,000 fakes in one
  source set took about 46 s and 1.2 GB RSS in the spike.
- **Distribution lookup** (KGP's real order, verified with `javap`; see the KDoc of
  `resolveKonanHome`): the `konan.data.dir` Gradle property
  (`<dir>/kotlin-native-prebuilt-<os>-<arch>-<version>`) first, then `kotlin.native.home`
  (alias `org.jetbrains.kotlin.native.home`), then the default directory (`KONAN_DATA_DIR`, else
  `~/.konan`). **`konan.data.dir` beats `kotlin.native.home`.** Version: `kotlin.native.version` or
  the applied KGP version.
- **Provisioning.** KGP provisions the distribution at configuration time (a `ValueSource`; 2.4.10
  also in `:downloadKotlinNativeDistribution`). Fakt does not depend on that task (it also downloads
  the LLVM toolchain, which Fakt never reads) and never reads `~/.konan/dependencies`. The user's own
  cinterop/link tasks do. A missing distribution fails the task with a message naming the path,
  `kotlin.native.home` and `konan.data.dir`.
- **Cache key.** `konanTargets` (sorted, host independent) and `kotlinNativeVersion` are `@Input`;
  `konanHome` is `@Internal`. Linux CI and macOS developers can share hits and the build is
  relocatable.
- **Samples and CI.** `kmp-multi-target` has posix/cinterop/`linuxMain`/`appleMain`/`iosMain` fakes
  and runs with the configuration cache on. New `samples/kmp-native-only` (`:all-native`,
  `:single`). `presence-*` and `FAKT_PRESENCE_*` are gone. New CI job `test-native-macos`. `run-tests`
  passes `-Pfakt.test.konanHome`, so the native TestKit suite (`FaktGenerateNativeTest`) runs in CI
  and skips locally unless that property is set.
- **Known limits, not done:**
  - #171: Kotlin < 2.4 can't read the published annotation klibs (klib ABI 2.4.0), so Native is
    blocked there.
  - Windows host not covered (#167, Windows half).
  - Collector mode (#168) is unchanged; native outputs flow into collectors automatically.
  - Unowned test sets (`mingwTest`, all-JS/Wasm intermediates) keep an empty plain dir.
  - #172: interface-level `@OptIn` loses its import. Samples use a module-wide
    `optIn("kotlinx.cinterop.ExperimentalForeignApi")` as a workaround.
  - `test-native-macos` is the only proof that Apple tests link and run against task-path fakes.
- Found by the spike, tracked separately: #173 (the unread producer `FirMetadataCache` is rewritten
  after every interface, which is quadratic; removed by #164a).
- Spike material (all 8 criteria passed, Linux and macOS arm64, Kotlin 2.2.0 and 2.4.10) lives on
  branch `ccr-aa1d21c5-gqlrfs`, `spike/native/`; the report is on #152.

## 4. Known gaps that are not shape-specific

Each one is tracked in #150 "Up next". Why each matters:

- **Compiler options and other compiler plugins are not forwarded to the worker** (#165).
  - `FaktCodegenWorkAction.kt` sets only free args, classpath and module name.
  - Main code that needs any of these fails `check(exit == OK)`, a regression versus in-process:
    `-Xcontext-parameters`, optIn, explicit API, `-Xjdk-release`, language/API version.
  - Code that relies on other compiler plugins breaks the same way: serialization `serializer()`,
    Parcelize, FIR-generating DI plugins.
- **Worker version pin** (#166). 2.4.10 can't read metadata from a newer user Kotlin.
- **CI has no Windows run** (#167, path bugs). macOS runs only `test-native-macos`.
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
- `FAKT_PRESENCE_*` checks are gone (moved in #152, item 7). Producers and cached-fakes cells cover
  Native now.

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
`make test-compat-agp-9.4`, `make test-clean-rebuild-cache`, `make test-kmp-android-lint`, `make test-kmp-android-target`.

## 7. Gotchas

- **Own wrappers:** `samples/compat-agp/agp-*` and `samples/kmp-android-lint` pin their own Gradle.
  `cd` into them instead of using root `./gradlew -p`. `samples/kmp-android-target` has no wrapper:
  AGP 8.11.1 works under the root Gradle, so it runs as `./gradlew -p samples/kmp-android-target`.
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

- **No Android SDK** in the cloud sandbox: the real Android cells (`android-single-module`,
  `android-test-fixtures`, compat-agp) run only in CI, and a new Android task name is confirmed by
  the first CI log. A fake SDK is enough for `faktGenerate*`, Kotlin compilation and
  `test*UnitTest` locally (not resource linking, lint or dexing). Rebuild one in five steps:
  1. `platforms/android-35/android.jar`: a jar `javac` builds from stubs of `android.content.Context`
     and `android.R$attr` (`R$attr` is mandatory, or AGP fails with "Missing attr resources"), plus
     `source.properties` and an empty `data/` next to it.
  2. `build-tools/{34,35,36}.0.0/` with `source.properties`, empty stub executables (`aapt`, `aapt2`,
     `aidl`, `dx`, `dexdump`, `zipalign`), `core-lambda-stubs.jar` and valid empty
     `lib/{dx,d8,apksigner}.jar`; without them AGP reports "Build Tools corrupted".
  3. `platform-tools/source.properties`.
  4. Point `sdk.dir` at the directory in an uncommitted `local.properties` (it is gitignored), or set
     `ANDROID_HOME`.
  5. Keep `compileOptions` out of the module: with Java 11 AGP wants `core-for-system-modules.jar`,
     which this SDK lacks. Skip `processDebugAndroidTestResources` (stub `aapt2` cannot link).
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
- **Run Tests job (test pool):** gradle-plugin tests run in one fork with a JUnit pool of 2
  (`parallelism` 2, `max-pool-size` 2 in `gradle-plugin/build.gradle.kts`). Without the cap,
  fork-join compensation ran about 7 TestKit builds at once, each with a 2g daemon, and runs hit
  the 5-minute timeout. If a run still times out, check the pool before re-running.
