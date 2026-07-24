<!--
Copyright (C) 2025 Rodrigo Sicarelli
SPDX-License-Identifier: Apache-2.0
-->

# Runtime Benchmark — Fakt vs. mocking libraries

An **unbiased, reproducible** benchmark of the **test-execution time** of Fakt's compile-time
generated fakes against reflection/instrumentation and compiler-plugin mocking libraries.

Hypothesis: because Fakt generates plain Kotlin classes with **zero runtime reflection**, it is
much faster at *running* tests than reflection-based mocks. This module puts a number on that.

> The authoritative run is CI (`.github/workflows/benchmark.yml`), where every technology runs on
> the **same runner** (identical CPU). Locally: `make benchmark` from the repo root.

## Competitors (one isolated Gradle module each)

| Module          | Technology            | Kind                          |
|-----------------|-----------------------|-------------------------------|
| `:fakt`         | **Fakt**              | Compile-time generated fakes  |
| `:handwritten`  | Hand-written fakes    | Plain Kotlin (baseline)       |
| `:mockk`        | MockK                 | Reflection + bytecode (JVM)   |
| `:mockito`      | Mockito + mockito-kotlin | Reflection + proxies (JVM) |
| `:mokkery`      | Mokkery               | Compiler plugin (IR)          |

> `:mockative` (Mockative, KSP) is present in the source tree but **excluded from the default run**
> (commented out in `settings.gradle.kts`). Mockative 3.3.2 generates its mock API into KMP
> source-set flavors and does not wire concrete mocks for cross-module `@Mockable` types into a plain
> `kotlin("jvm")` module; reviving it needs a KMP layout. See the compatibility notes below.

**Isolation.** Each competitor is a separate Gradle subproject with an **independent classpath** —
the `:mockk` module never sees Fakt on its classpath, and vice-versa. Only `:fakt` applies the Fakt
compiler plugin. Shared, mock-agnostic infrastructure lives in `:harness` (the timing engine) and
`:contracts` (the plain domain interfaces consumed by every non-Fakt competitor). Fakt keeps its own
copy of the domain annotated with `@Fake`, byte-identical apart from that annotation.

## Fairness / methodology

- **JVM-only.** MockK and Mockito cannot run outside the JVM, so the timing table is JVM-only for
  parity. Fakt's run-everywhere (JVM/JS/Native/Wasm) capability is a separate qualitative point, not
  a timing row.
- **One measurement engine for all.** Every competitor is timed through the same `:harness`
  (`Bench`) — identical warmup, measured iterations and anti-dead-code-elimination sink. Config is
  injected by the `fakt-benchmark-jvm` convention plugin (`build-logic`), so no module can tune its
  own knobs. Test parallelism is **disabled** and a single JVM fork is used (a benchmark must not
  contend for CPU with itself).
- **Identical work.** The five scenarios do the same amount of work in every module; only the
  `create/configure/verify` calls differ, mirroring
  [`docs/user-guide/migration-from-mocks.md`](../../docs/user-guide/migration-from-mocks.md).
- **Baseline included.** If Fakt lands near the hand-written baseline while both beat the reflection
  libraries, that is the most credible possible result.
- **Raw data published.** CI uploads the per-scenario JSON so anyone can recompute the table.

### Why a custom harness instead of JMH?

Fakt generates its fakes only into the **test** source set, so a separate JMH source set could not
see them; and JMH's Gradle plugin lags bleeding-edge Kotlin. The harness is a transparent
`System.nanoTime()` loop with warmup + N measured iterations + a volatile sink — auditable, and
faithful to the "run it N times and average" model. Reported as mean per operation.

## Scenarios (Bloc A) and the mock-tax hot-spot each targets

Penalties are from `docs/why-fakt.md` → **"The Mock Tax"** (sources `[^1]`–`[^3]` there).

| Scenario              | Interface           | Targets (documented penalty)                         |
|-----------------------|---------------------|------------------------------------------------------|
| `verify-heavy`        | `AnalyticsService`  | Interaction verification — **47x** `verify {}`       |
| `relaxed-unstubbed`   | `ComplexApiService` | Unstubbed/relaxed calls — **3.7x** `relaxed` mocks   |
| `instantiation-churn` | all four            | Fresh-mock creation cost (instrumentation overhead)  |
| `property-heavy`      | `ConfigProvider`    | Property stubbing                                    |
| `suspend-heavy`       | `UserRepository`    | `suspend` stubbing (mocks pay coroutine wrapping)    |

Each measured iteration performs `create → configure → invoke ×K → verify`. Defaults (override with
`-Pfakt.benchmark.*`): warmup `2`, iterations `8`, invocations (K) `500`, churn `50`.

**Honesty note.** The shared domain is **interfaces** (Fakt requires an interface). Mockito therefore
uses cheap JDK dynamic proxies here, so the documented final-class `mock-maker-inline` **2.7–3x**
penalty is *not* exercised — it applies to mocking final classes, which is out of scope. Likewise the
huge `mockkObject` (1391x) / `mockkStatic` (146x) penalties are MockK-specific anti-patterns with no
cross-library equivalent; they belong in a separate showcase, not this apples-to-apples table.
Mockative has no "relaxed" mode, so its `relaxed-unstubbed` scenario explicitly stubs the members it
reads — the closest honest equivalent.

## JUnit5 feature amplifier (Bloc C)

Each competitor ships an identical `@ParameterizedTest` (`*FeatureTest`) that creates a **fresh**
fake/mock per case, so a technology's per-instance creation cost is paid once per input — exactly how
the mock tax compounds in real suites. The suite wall-clock (from the JUnit XML `time` attribute,
startup-inclusive) is reported separately from the precise Bloc-A numbers.

## Running

```bash
make benchmark            # from repo root: publish-local + run all competitors + print the table
```

Or directly (after `./gradlew publishToMavenLocal` at the repo root):

```bash
cd samples/runtime-benchmark
./gradlew benchmark --continue                       # all competitors
./gradlew :mockk:test                                # a single competitor
./gradlew benchmark -Pfakt.benchmark.invocations=2000 -Pfakt.benchmark.iterations=15
kotlin ../../.github/scripts/benchmark-summary.main.kts . "Runtime Benchmark"   # print the table
```

### Verifying isolation

```bash
./gradlew :mockk:dependencies    | grep -i rsicarelli.fakt   # -> no matches (MockK never sees Fakt)
./gradlew :fakt:dependencies     | grep -i rsicarelli.fakt   # -> Fakt annotations present
```

## Version-compatibility notes (bleeding-edge Kotlin)

Kotlin here is **2.3.20**. Versions pinned in `gradle/libs.versions.toml`:

- **Mokkery `3.3.0`** — supports Kotlin 2.3.0–2.3.21.
- **Mockative `3.3.2`** + KSP `2.3.7` (KSP2 standalone versioning) — **currently excluded.** KSP runs
  and generates Mockative's base API, but into KMP source-set flavors (`commonMain/jvmMain/...`) and
  without concrete mocks for the cross-module `@Mockable` interfaces, so a plain `kotlin("jvm")`
  module can't resolve `io.mockative.mock`. Reviving Mockative means giving this module a KMP layout
  (or declaring the interfaces in-module with `@Mock` properties). The summary degrades gracefully —
  it simply shows the technologies that produced results.
- **MockK `1.14.2`**, **Mockito `5.x`** — plain JVM libraries, version-robust. Mockito returns `null`
  for unstubbed object methods (its relaxed behavior), which the `relaxed-unstubbed` scenario reads
  null-safely.
