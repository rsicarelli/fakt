# Plugin Configuration

Complete reference for configuring the Fakt Gradle plugin.

---

## Complete Configuration Reference

All available configuration options in your module's `build.gradle.kts`:

```kotlin
// build.gradle.kts
import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    alias(libs.plugins.fakt)
}

fakt {
    // Enable or disable the plugin (default: true)
    enabled.set(true)

    // Control logging verbosity (default: INFO)
    logLevel.set(LogLevel.INFO)  // Options: QUIET, INFO, DEBUG

    // Control call history generation (default: true)
    enableCallHistory.set(true)  // Set to false for lightweight fakes

    // Control mutable fake generation (default: false)
    enableMutableFakes.set(false)  // Set to true for mutable fakes by default

    // Generate fakes to testFixtures source set (default: false)
    // JVM: requires `java-test-fixtures` plugin. Android: requires
    // `android { testFixtures { enable = true } }` (see the Test Fixtures guide).
    useGradleTestFixtures.set(false)

    // Multi-module: Collect fakes from another module (default: not set)
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

---

## Configuration Properties

<table>
<tr><th>Flag</th><th>Default</th><th>Example</th></tr>
<tr>
<td><strong>enabled</strong></td>
<td><code>true</code></td>
<td>

```kotlin
fakt {
    enabled.set(false)
}
```

</td>
</tr>
<tr>
<td><strong>logLevel</strong></td>
<td><code>INFO</code></td>
<td>

```kotlin
fakt {
    logLevel.set(LogLevel.DEBUG)
}
```

</td>
</tr>
<tr>
<td><strong>enableCallHistory</strong></td>
<td><code>true</code></td>
<td>

```kotlin
fakt {
    enableCallHistory.set(false)
}
```

</td>
</tr>
<tr>
<td><strong>enableMutableFakes</strong></td>
<td><code>false</code></td>
<td>

```kotlin
fakt {
    enableMutableFakes.set(true)
}
```

</td>
</tr>
<tr>
<td><strong>useGradleTestFixtures</strong></td>
<td><code>false</code></td>
<td>

```kotlin
fakt {
    useGradleTestFixtures.set(true)
}
```

</td>
</tr>
<tr>
<td><strong>collectFrom</strong></td>
<td>Not set</td>
<td>

```kotlin
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

</td>
</tr>
</table>

---

## Log Level Details

<table>
<tr><th>Level</th><th>Description</th><th>Example</th></tr>
<tr>
<td><strong>INFO</strong><br>(default)</td>
<td>

Concise summary with key metrics. Use for local development and monitoring cache effectiveness.

</td>
<td>

```kotlin
fakt {
    logLevel.set(LogLevel.INFO)
}
```

<strong>Output:</strong>

```
Fakt: 101 fakes generated in 35ms (50 cached)
  Interfaces: 101 | Classes: 0
  FIR: 6ms | IR: 29ms
  Cache: 50/101 (49%)
```

</td>
</tr>
<tr>
<td><strong>DEBUG</strong></td>
<td>

Detailed FIR + IR phase timing. Use for troubleshooting, performance analysis, and bug reports.

</td>
<td>

```kotlin
fakt {
    logLevel.set(LogLevel.DEBUG)
}
```

<strong>Output:</strong>

```
Registering FIR extension
Registering IR extension with FIR metadata access
Built IR class map with 149 classes
FIR→IR Transformation (interfaces: 101/101, took 1ms)
FIR + IR trace
├─ Total FIR time: 6ms
├─ Total IR time: 58ms
│  ├─ FIR analysis: 1 type parameters, 6 members (55µs)
│  └─ IR generation: FakeDataCacheImpl 83 LOC (766µs)
│  ├─ FIR analysis: 2 type parameters, 1 members (23µs)
│  └─ IR generation: FakeMapTransformerImpl 23 LOC (335µs)
```

</td>
</tr>
<tr>
<td><strong>QUIET</strong></td>
<td>

No output except errors. Use for CI/CD pipelines and production builds.

</td>
<td>

```kotlin
fakt {
    logLevel.set(LogLevel.QUIET)
}
```

<strong>Output:</strong> None (silent)

</td>
</tr>
</table>

---

## Call History Configuration

Control whether generated fakes include call tracking and verification capabilities.

<table>
<tr><th>Setting</th><th>Description</th><th>Example</th></tr>
<tr>
<td><strong>true</strong><br>(default)</td>
<td>

Full call history with:

- `methodNameCalls` StateFlow properties
- `methodNameCallHistory` lists
- `verifyMethodName { }` DSL

</td>
<td>

```kotlin
fakt {
    enableCallHistory.set(true)
}
```

</td>
</tr>
<tr>
<td><strong>false</strong></td>
<td>

Lightweight fakes with only behavior configuration. No call history overhead.

</td>
<td>

```kotlin
fakt {
    enableCallHistory.set(false)
}
```

</td>
</tr>
</table>

### Per-Interface Override

Individual interfaces can override the project default:

```kotlin
import com.rsicarelli.fakt.CallHistoryMode

// Always generate call history (even if plugin default is false)
@Fake(callHistory = CallHistoryMode.ENABLED)
interface PaymentService { ... }

// Never generate call history (even if plugin default is true)
@Fake(callHistory = CallHistoryMode.DISABLED)
interface Logger { ... }

// Follow plugin default
@Fake  // or @Fake(callHistory = CallHistoryMode.DEFAULT)
interface UserService { ... }
```

**Resolution order:** Annotation setting takes precedence over plugin default.

---

## Mutable Fakes Configuration

Control whether generated fakes are mutable (reconfigurable mid-test) or immutable (fixed at construction). For an in-depth exploration of when to use each mode, see **[Immutable vs Mutable](immutable-vs-mutable.md)**.

<table>
<tr><th>Setting</th><th>Description</th><th>Example</th></tr>
<tr>
<td><strong>false</strong><br>(default)</td>
<td>

Immutable fakes with:

- `private val` behavior properties
- Behavior fixed at construction time
- No `modify {}` method

</td>
<td>

```kotlin
fakt {
    enableMutableFakes.set(false)
}
```

</td>
</tr>
<tr>
<td><strong>true</strong></td>
<td>

Mutable fakes with:

- `@Volatile private var` behavior properties
- `modify {}` method for selective reconfiguration
- Mid-test behavior changes

</td>
<td>

```kotlin
fakt {
    enableMutableFakes.set(true)
}
```

</td>
</tr>
</table>

### Per-Interface Override

Individual interfaces can override the project default:

```kotlin
import com.rsicarelli.fakt.MutabilityMode

// Always generate mutable fake (even if plugin default is false)
@Fake(mutability = MutabilityMode.MUTABLE)
interface UserRepository { ... }

// Always generate immutable fake (even if plugin default is true)
@Fake(mutability = MutabilityMode.IMMUTABLE)
interface Logger { ... }

// Follow plugin default
@Fake  // or @Fake(mutability = MutabilityMode.DEFAULT)
interface UserService { ... }
```

**Resolution order:** Annotation setting takes precedence over plugin default.

---

## Multi-Module Configuration

<table>
<tr><th>Mode</th><th>Example</th></tr>
<tr>
<td><strong>Type-safe accessor</strong></td>
<td>

```kotlin
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

</td>
</tr>
<tr>
<td><strong>String-based path</strong></td>
<td>

```kotlin
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(project(":core:analytics"))
}
```

</td>
</tr>
</table>

For complete multi-module documentation, see **[Multi-Module Guide](multi-module.md)**.

---

## Cache-Correct Generation

Fakt generates fakes in dedicated, cacheable `faktGenerate*` Gradle tasks. The generated `.kt` files
are declared task outputs, so when Gradle's build cache restores a compilation the fakes come back
with it — no empty or missing fakes on a cache hit.

**Support matrix:**

| `@Fake` declared in        | Generated | Cache-correct |
|----------------------------|-----------|---------------|
| `commonMain`               | ✅        | ✅            |
| JVM / Android platform main| ✅        | ✅            |
| JS / Wasm platform main    | ✅        | ✅            |
| Native platform main       | ✅        | ✅            |
| Shared native (`nativeMain`, `appleMain`, `iosMain`, `linuxMain`) | ✅ | ✅ |

Native runs on the same task path as the other platforms, through the Kotlin/Native compiler. See
[Kotlin/Native](#kotlinnative) below.

!!! tip "Single-target multiplatform projects"
    A multiplatform project with exactly one target (for example `kotlin { jvm() }` and nothing
    else) gets no `commonMain` compilation from Kotlin, so its lone platform task
    (`faktGenerateJvmMain`) generates every fake: the ones declared in `commonMain` go to
    `commonTest`, the ones declared in `jvmMain` go to `jvmTest`. This works for JVM, JS and Wasm
    single targets.

!!! tip "Projects where every target is JVM"
    When every target is a JVM target, Kotlin gives `commonMain` no compilation of its own. Fakt
    then adds a `faktGenerateCommonMain` task for it, so fakes declared in `commonMain` still reach
    `commonTest`. Each platform main (for example `faktGenerateJvmMain`) keeps its own task and its
    own fakes.

    The usual way to get this shape is the Android KMP library target with
    `android.kmp.use.jvm.platform.type=true` in `gradle.properties`, next to `jvm()`. Both
    targets are then JVM-typed.

    Two `jvm()` targets (`jvm("desktop")` and `jvm("server")`) also give this shape, but Kotlin
    rejects them by default (`KotlinTargetAlreadyDeclaredError`). It only builds with the internal
    property `kotlin.internal.suppressGradlePluginErrors=KotlinTargetAlreadyDeclaredError`, which
    is not a supported setting. `samples/kmp-all-jvm` uses it.

    Intermediate source sets are generated too (#162): a fake in `webMain` (shared by `js` and
    `wasmJs`) gets `faktGenerateMetadataWebMain`, and a fake in an intermediate that only JVM targets
    share (for example `desktopAndServerMain`) gets `faktGenerateDesktopAndServerMain`. The fakes
    land in the matching test source set (`webTest`, `desktopAndServerTest`) when it exists.
    `samples/kmp-multi-target` and `samples/kmp-all-jvm` show both.

    Not generated yet: fakes in intermediate source sets that only JS targets or only Wasm targets
    share, and fakes in projects whose targets are all JS or all Wasm. These are follow-up work.
    Native intermediates (`nativeMain`, `appleMain`, `iosMain`, `linuxMain`) are generated too, by
    the Kotlin/Native compiler (see [Kotlin/Native](#kotlinnative)).

    Two limits apply to intermediate source sets:

    - On Kotlin older than 2.2.20 `webMain` is not part of the default hierarchy template. Declare
      it yourself with `dependsOn` (`webMain` depends on `commonMain`; `jsMain` and `wasmJsMain`
      depend on `webMain`).
    - Fakt reads the `dependsOn` edges when the Kotlin plugin configures the project. Edges that
      your own build code adds in a later `afterEvaluate` are missed, so the intermediate's fakes
      are not generated. Declare the hierarchy in the `kotlin { sourceSets { } }` block.

### Kotlin/Native

Native fakes are generated by the Kotlin/Native compiler, not the JVM one:

- A leaf native main (`linuxX64Main`, `iosArm64Main`) is generated by the compilation that uses it,
  like JS and Wasm.
- Every shared-native source set (`nativeMain`, `appleMain`, `iosMain`, `linuxMain`, and
  `commonMain` of a project whose targets are all Native) gets its own `faktGenerate*` task.
- A project with a single Native target gets one task, like any single-target project.
- Native test source sets receive the fakes the same way as the other platforms.

The generation worker runs on **your** Kotlin/Native distribution (the one Kotlin Gradle Plugin
already downloaded for the build), so it uses your Kotlin/Native version, not the Kotlin version
Fakt's own worker uses. Fakt does not add a dependency on `:downloadKotlinNativeDistribution` and
never reads `~/.konan/dependencies`. Your own cinterop and link tasks still use it.

Fakt looks for the distribution in the same order as Kotlin Gradle Plugin:

1. the `konan.data.dir` Gradle property (`<dir>/kotlin-native-prebuilt-<os>-<arch>-<version>`)
2. the `kotlin.native.home` Gradle property
3. the default directory: the `KONAN_DATA_DIR` environment variable, else `~/.konan`

When both `konan.data.dir` and `kotlin.native.home` are set, `konan.data.dir` wins. The version is
`kotlin.native.version` if set, else the version of the Kotlin Gradle plugin. If no distribution is
found, the task fails with a message naming the path it looked at, `kotlin.native.home` and
`konan.data.dir`.

`kotlin.native.home`, `konan.data.dir` and `kotlin.native.version` are read from Gradle properties
only (`gradle.properties` or `-P`). Fakt does not read `local.properties`, which Kotlin Gradle
plugin also consults, so a value set only there is ignored by Fakt.

The cache key holds the sorted Native targets and the Kotlin/Native version, and no absolute path
and no host target, so it is relocatable: the build can move between directories. The target list
does not depend on the host, but the Native dependency klibs (the standard library, the platform
libraries, commonizer output) come from your host's distribution and are part of the key. Cache
hits across operating systems are therefore not guaranteed.

!!! warning "Kotlin/Native limits"
    - Native needs Kotlin 2.4 or newer for now: older compilers cannot read the published
      annotations klibs ([#171](https://github.com/rsicarelli/fakt/issues/171)).
    - Windows hosts are not covered yet ([#167](https://github.com/rsicarelli/fakt/issues/167)).
    - The fakes of a source set that only one target compiles (for example `mingwMain` with a
      single `mingwX64` target) reach that target's test compilation (`mingwX64Test`). Write the
      tests that use them in `mingwX64Test`: the intermediate `mingwTest` is its parent, cannot see
      them, and keeps an empty generated directory
      ([#187](https://github.com/rsicarelli/fakt/issues/187)).
    - An `@OptIn` on the interface loses its import in the fake
      ([#172](https://github.com/rsicarelli/fakt/issues/172)). For cinterop types, opt in for the
      whole module instead, for example `optIn("kotlinx.cinterop.ExperimentalForeignApi")` in
      `compilerOptions`.
    - The generation worker heap is fixed at 1g.

!!! tip "Custom test compilations and Android build types"
    Generated fakes reach a test compilation **by association**: a custom test compilation, such
    as `compilations.create("integrationTest")` on a JVM target, receives the fakes of the platform
    compilation it is associated with (the target's own source sets). This includes associations
    made after evaluation, for example
    `afterEvaluate { integrationTest.associateWith(compilations.getByName("main")) }`.
    `samples/kmp-all-jvm` has such a `serverIntegrationTest`.

    Fakes of an intermediate source set are wired when its producer is registered, so an
    association made later does not receive them. Fakes of `commonMain` reach only `commonTest`; a
    custom compilation sees them only if its source set depends on `commonTest`.

    On Android, each build type or flavour's fakes reach only that variant's tests. A build type
    whose name contains another's, such as `debugMinified` next to `debug`, gets its own fakes in
    `testDebugMinifiedUnitTest` and never the ones of `debug`. `samples/android-single-module`
    declares `debugMinified` and `preRelease` to prove it.

    In a multiplatform module that declares `androidTarget()`, the fakes of `androidMain` reach each
    variant's unit and instrumented tests: every variant (`debug`, `release`, or `freeDebug` with
    flavors) generates its own copy of the `androidMain` fakes into its own output directory, and
    the shared `androidUnitTest` and `androidInstrumentedTest` source sets receive none. That is
    what lets a type declared only in `androidDebug` or `androidRelease` (the same name with
    different members per build type) appear in an `androidMain` fake's signature. When
    `androidTarget()` is the only target, `commonMain` has no metadata compilation: its fakes reach
    `commonTest` from `faktGenerateCommonMain`, which runs on the debug variant.
    `samples/kmp-android-target` covers all three shapes.

    Types from AAR dependencies (`androidx`) can appear in a `@Fake` signature: for an Android
    compilation Fakt gives the generation worker the unpacked class jars of the AARs, not the
    `.aar` files.

!!! note "Project shapes that keep the in-process path"
    One shape falls back to generating inside `compileKotlin*`. Fakes are still generated for
    every `@Fake` — they just aren't declared task outputs, so those modules' `compileKotlin*`
    tasks opt out of the Gradle build cache (see below). Fakt logs a warning naming the reason
    whenever a module lands here.

    - **Android modules on AGP's built-in Kotlin support whose Android Gradle Plugin isn't visible
      to Fakt** (for example, AGP only in a subproject's build script while Fakt is on the root
      build classpath). AGP 9's built-in Kotlin keeps sources in its variant model, which Fakt
      reads through AGP's API. Put AGP on the same build classpath as Fakt, for example with
      `plugins { id("com.android.library") apply false }` in the root build, and apply Fakt in
      the `plugins { }` block rather than after evaluation.

**Default:** `true`.

### Opting out

The previous behaviour — generation running inside `compileKotlin*` as a side effect — is still
available. Turn it off via the extension or a Gradle property (the property wins over the extension,
so a command-line opt-out always applies):

```kotlin
fakt {
    useExperimentalGenerateTask.set(false)
}
```

```bash
gradle build -Pfakt.useExperimentalGenerateTask=false
```

!!! warning "Temporary escape hatch"
    The path is kept only as an escape hatch and will be removed in a future release — please
    [open an issue](https://github.com/rsicarelli/fakt/issues) if you need it.

### Why the in-process path opts out of the build cache

On the in-process path the fakes are written as a side effect of `compileKotlin*`, and no task
declares them. That makes the task's build-cache entry incomplete: after a `clean`, Gradle sees
unchanged inputs, restores the compilation from the cache, and never runs the compiler — so the
`.kt` files are never written. The build then continues with an empty generated directory:

```text
> Task :core:compileKotlin FROM-CACHE
> Task :core:compileTestFixturesKotlin NO-SOURCE
> Task :app:compileTestKotlin FAILED
e: UserServiceTest.kt:5:54 Unresolved reference 'fakeCacheManager'.
```

So Fakt takes those `compileKotlin*` tasks out of the build cache
([issue #142](https://github.com/rsicarelli/fakt/issues/142)). Only that one task per module is
affected — the `test` and `testFixtures` compilations read the generated files as *source*, so they
are already part of those tasks' input fingerprints and keep caching normally. Up-to-date checks are
untouched too: an unchanged build still skips the task, and only a `clean` (which deletes the fakes
anyway) re-runs it.

Nothing changes on the cache-correct path, where `faktGenerate*` declares the generated directory as
a real task output and the cache restores the fakes along with it.

---

## IDE Integration

### IntelliJ IDEA / Android Studio

Generated fakes appear in `build/generated/fakt/` and are automatically indexed.

**Enable K2 Mode for better autocomplete:**

1. **Settings** → **Languages & Frameworks** → **Kotlin**
2. Enable **K2 mode**
3. Restart IDE

K2 mode improves factory function autocomplete and type inference.

### Generated Sources Location

| Source Set | Generated Output |
|-----------|------------------|
| `commonTest/` | `build/generated/fakt/commonTest/kotlin/` |
| `jvmTest/` | `build/generated/fakt/jvmTest/kotlin/` |
| `iosTest/` | `build/generated/fakt/iosTest/kotlin/` |
| `androidUnitTest/` | `build/generated/fakt/androidUnitTest/kotlin/` |
| `testFixtures/` | `build/generated/fakt/testFixtures/kotlin/` (requires `useGradleTestFixtures`) |

---

## Next Steps

- **[Test Fixtures (JVM)](test-fixtures.md)** - Cross-module fakes for JVM projects
- **[Multi-Module (KMP)](multi-module.md)** - Cross-module fakes with collector modules
- **[Usage Guide](usage.md)** - Comprehensive usage patterns and examples
- **[Troubleshooting](../help/troubleshooting.md)** - Common configuration issues
