<!--
  Copyright (C) 2025 Rodrigo Sicarelli
  SPDX-License-Identifier: Apache-2.0
-->
# kmp-android-target

Kotlin Multiplatform modules that declare `androidTarget()` through the `com.android.library`
plugin (AGP 8.11.1, the supported floor). Fakt generates their fakes from per-variant
`faktGenerate*` tasks whose outputs are cacheable, instead of the in-process compiler plugin.

The three modules cover the shapes the per-variant routing has to get right:

| Module | Targets | What it proves |
|--------|---------|----------------|
| `:shared` | `jvm()` + `androidTarget()` | `commonMain` fakes reach `commonTest`, `jvmTest` and every Android unit test. `androidMain` fakes (including one with an `android.content.Context` parameter) reach each variant's unit and instrumented tests, never `jvmTest`. `androidDebug` and `androidRelease` each declare `class BuildFlags` (same FQN, different members) and the `androidMain` `FlagReader` uses it, so each variant must analyse its own copy. `DebugMenu` lives in `androidDebug` only. |
| `:android-only` | `androidTarget()` alone | No `commonMain` compilation exists, so the debug-preferred variant emits the `commonMain` fakes once, into `commonTest`. There is no `src/androidDebug` (the empty-source-set case). |
| `:flavored` | `jvm()` + `androidTarget()`, flavor dimension `tier` (`free`, `paid`) | `androidFree` and `androidPaid` declare a `@Fake TierFeatures` with the same FQN and different members; `androidUnitTestFree` and `androidUnitTestPaid` each see only their own flavor's fake. |

Each variant emits its own copy of the `androidMain` fakes into its own output directory. The
shared `androidUnitTest` and `androidInstrumentedTest` source sets receive no generated directory.

## Tasks

```text
:shared:faktGenerateMetadataCommonMain   :shared:faktGenerateJvmMain
:shared:faktGenerateAndroidDebug         :shared:faktGenerateAndroidRelease
:android-only:faktGenerateCommonMain     :android-only:faktGenerateAndroidDebug
:flavored:faktGenerateAndroidFreeDebug   :flavored:faktGenerateAndroidPaidRelease   (and the other flavor/build-type pairs)
```

The sample sets `fakt { logLevel = INFO }` in every module: the cache-contract CI cell forbids
`Fakt: tolerated compiler error` lines, and they are only printed at INFO or above.

## Avoided on purpose

`@Fake` signatures use no types from AAR dependencies. Android compile classpaths contain `.aar`
files, which the generation worker does not read yet (see the limits in the plugin configuration
guide), so the sample keeps to `android.content.Context` from `android.jar`.

## Run

Needs an Android SDK (`ANDROID_HOME`) and the plugin in Maven Local (`make publish-local`).

```bash
make test-kmp-android-target
# or
./gradlew -p samples/kmp-android-target build compileDebugAndroidTestKotlinAndroid
```

Run the build twice: the configuration cache is on, and the second run must print
`Configuration cache entry reused`.
