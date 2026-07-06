# Metadata-Driver Producer + FIR-Phase Emission (Issue #79)

**Status:** Approved design — implementation in progress on `feat/79-metadata-producer`
**Last Updated:** July 2026
**Research basis:** [issue-79-cache-correct-kmp-research.md](../research/issue-79-cache-correct-kmp-research.md)
(9-part investigation, verified against Kotlin `build-2.3.21-release-298` and KSP 2.3.0-290 primary source)
**Guard:** everything in this design ships behind `fakt.useExperimentalGenerateTask` (default `false`,
`FaktPluginExtension.kt`). Flipping the default (backlog P8) and removing the legacy path (P9) are
explicitly **out of scope** for this branch.

---

## 1. Problem: `NO_ACTUAL_FOR_EXPECT` is structural under a platform driver

The cache-correct KMP **commonMain producer** (`FaktGenerateTask` → worker → reflective compiler) drives
`K2JVMCompiler` over commonMain-only sources with `-Xmulti-platform` + `-Xcommon-sources`. That
combination creates **two module fragments** (common + platform), which activates the **IR actualizer** —
and the actualizer is the sole origin of `NO_ACTUAL_FOR_EXPECT` (`IrActualizationErrors.kt:26` in the
Kotlin compiler; it is *not* a FIR checker). Since the producer never feeds platform `actual`s, any
ordinary `expect` in commonMain is fatal. Locked by `FaktGenerateCommonProducerTest` test #4
(`.buildAndFail`). Real KMP commonMain almost always contains `expect` declarations, so the flag cannot
become the default until this is fixed.

Two facts make the fix a re-architecture rather than a flag tweak:

- `-Xexpect-actual-classes` does **not** suppress this error — it only mutes the "expect/actual classes
  are in Beta" warning. The worker comment claiming otherwise (`FaktCodegenWorkAction.kt:214-219`) is
  wrong and is deleted by this work.
- Once the actualizer reports the error, `convertToIr` skips `applyIrGenerationExtensions` entirely — so
  nothing is generated even if the error were downgraded.

## 2. Decision: one emitter, two drivers

### 2.1 The producer driver becomes `KotlinMetadataCompiler`

`org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler` (→ `MetadataCliPipeline`) is **frontend-only**:
FIR resolve + checkers → direct FIR→klib serialization. **No fir2ir, no actualizer** — the diagnostic is
structurally impossible, for every expect shape (top-level, member, value class), not just the easy cases.
It builds one session over exactly the sources passed (`metadataCompilationMode = true`), plugin **FIR
extensions register and run**, and `IrGenerationExtension` **never fires**. It ships inside
`kotlin-compiler-embeddable:2.3.20` — zero new dependencies, no relocation hazard — and is invoked through
the exact same reflective `exec(MessageCollector, Services, CommonCompilerArguments)` surface
`K2CompilerBridge` already uses.

This is the same pipeline the **legacy path already relies on**: KGP's `compileCommonMainKotlinMetadata`
runs Fakt's FIR checkers (and the producer cache write) in-process today without expect failures. The new
parts are only (a) driving it reflectively from the worker and (b) emitting `.kt` at FIR.

### 2.2 Generation re-hosts on the FIR phase

Because the metadata pipeline has no IR phase, `.kt` emission must happen at FIR. This is possible because
generation is **FIR-only-capable**: the `codegen-runtime` renderer consumes a pure string-based
`FakeDeclaration` (zero `org.jetbrains.kotlin` imports), and the FIR checker already extracts nearly the
full structural model. The worker's fir2ir run exists today only to fire `IrGenerationExtension` and hand
the translator `IrType`s for type-string rendering; the produced bytecode goes to a `@LocalState` scratch
dir and is never read. The genuinely IR-derived data is small and fully FIR-derivable:

| IR-derived today | FIR source of truth |
|---|---|
| Rendered member types + import FQNs (`RenderedType(shortName, fqns)`) | new `FirTypeRenderer` over `ConeKotlinType` (§5) |
| `isOperator` | `FirFunctionSymbol.status.isOperator` |
| Extension-receiver type | `receiverParameter?.typeRef?.coneType` |
| Class primary-constructor params | primary `FirConstructor` value parameters |
| Generic pattern | FIR type-parameter lists (pure derivation) |

Everything else (names, nullability, `isSuspend`/`isInline`, type-param bounds, inherited members,
annotations, visibility, call-history/mutability modes, default-value **code** via
`FirExpressionRenderer`) is already FIR-extracted. Default-value *resolution* happens in
`codegen-runtime` from type strings — no IR involvement.

### 2.3 Why two drivers stay

The metadata frontend resolves dependencies via metadata klibs/`.kotlin_metadata`, **not** JVM `.class`
files. Single-platform JVM producers and KMP JVM/Android consumers have JVM classpaths, so they stay on
`K2JVMCompiler` — which still runs IR. Hence emission is selected by an explicit **`emitPhase`** contract
(§3), and `UnifiedFaktIrGenerationExtension` early-returns when `emitPhase == FIR` so the K2JVM-driven
paths don't double-generate. One emitter, two drivers:

```
BEFORE (producer, commonMain):
  FaktGenerateTask ──▶ worker ──▶ K2JVMCompiler(-Xmulti-platform -Xcommon-sources, JVM-target classpath hack)
      FIR checkers ──▶ in-memory storage ──▶ fir-metadata.json (producer write)
      fir2ir + IR ACTUALIZER  ✗ NO_ACTUAL_FOR_EXPECT on any unpaired expect
      IrGenerationExtension ──▶ IrToFakeDeclarationTranslator ──▶ FaktCodegen ──▶ .kt
      bytecode ──▶ scratch (never read)

AFTER (producer, commonMain):
  FaktGenerateTask ──▶ worker ──▶ KotlinMetadataCompiler(-Xmulti-platform, real commonMain klib classpath)
      FIR checkers ──▶ in-memory storage ──▶ fir-metadata.json (unchanged format)
                └─▶ FirFakeEmitter (emitPhase=FIR) ──▶ FirToFakeDeclarationTranslator ──▶ FaktCodegen ──▶ .kt
      metadata klib ──▶ scratch (never read)          (no fir2ir, no actualizer — expects are fine)

AFTER (single-JVM producer / KMP JVM-Android consumer):
  worker ──▶ K2JVMCompiler (unchanged driver)
      FIR checkers ──▶ FirFakeEmitter (emitPhase=FIR) ──▶ .kt
      IR phase runs but UnifiedFaktIrGenerationExtension early-returns (emitPhase == FIR)

LEGACY (default, in-process — byte-for-byte untouched):
  compileKotlin* ──▶ FIR checkers ──▶ IR extension ──▶ .kt   (emitPhase defaults to IR)
```

## 3. The `emitPhase` contract

- `compiler-api`: `@Serializable enum class EmitPhase { FIR, IR }`; new field
  `SourceSetContext.emitPhase: EmitPhase = EmitPhase.IR`.
- **Only** `FaktCodegenWorkAction.populateSourceSetContext` sets `FIR`, at **execution time** (like
  `metadataOutputPath`), so the task's `@Input` context JSON stays machine-independent and — because
  kotlinx-serialization omits defaults — **byte-identical for legacy** consumers.
- `FaktOptions.emitPhase` exposes it to the compiler plugin.
- **Not inferred from producer/consumer mode.** The legacy path also sets
  `metadataOutputPath`/`metadataCachePath` (via `SourceSetDiscovery.computeCachePaths` for every KMP
  compilation); inference would silently flip legacy KMP to FIR emission — exactly the regression the
  "legacy untouched" rule forbids.

Rollout staging: `FIR` is set (1) for `commonAnalysis` invocations when the metadata driver lands (PR-4),
then (2) unconditionally for all worker invocations (PR-5). The legacy in-process path never sets it and
keeps exercising the IR path until backlog P9.

## 4. FIR emission pipeline

New package `compiler/.../fir/generation/` (keeps the FIR/IR phase-separation rule intact):

- **`FirToFakeDeclarationTranslator`** — `ValidatedFakeInterface`/`ValidatedFakeClass` (+ rendered
  side-channels, §5) → pure `FakeDeclaration.Interface`/`Class`. Field-for-field mirror of
  `IrToFakeDeclarationTranslator` (which stays untouched as the parity template), reusing
  `resolveCallHistory`/`resolveMutability`; `PureGenericPattern` derived from FIR type-parameter lists.
- **`FirFakeEmitter`** — consults a collision seen-set on `FaktSharedContext` (first `(packageName,
  FakeXxxImpl)` wins; same error text as `reportAndDropOutputCollisions`), translates, then delegates to
  the existing render-and-write orchestrator.
- **`CodeGenerator`** moves `ir/generation/` → `core/generation/` (verified zero
  `org.jetbrains.kotlin` imports): it already owns `ImportResolver` + `selectOutputDirectory`
  (commonMain→commonTest mapping) + atomic-enough disk writes, and now serves both phases.
- **Hook:** in both checkers, immediately after `storeInterface`/`storeClass` and the producer
  `writeCache` — the established precedent for checker-driven side effects
  (`FakeInterfaceChecker.kt:132-139`) — gated by `options.emitPhase == FIR`.
- **`UnifiedFaktIrGenerationExtension.generateFromFirMetadata`** early-returns when `emitPhase == FIR`.

**Rendering happens inside `analyzeMetadata`, not post-hoc.** The in-memory `Fir*Info` models carry only
`ConeKotlinType.toString()` strings; the live `ConeKotlinType`s and the `FirSession` (needed for typealias
expansion) exist only during checker analysis. So the models gain **in-memory rendered side-channels**
(`FirRenderedType(shortName, fqns, isTypeParameter, requiresCollectionErasure)` on properties/params,
`renderedReturnType`/`isOperator`/`extensionReceiverRendered` on functions, `constructorParameters` on
`ValidatedFakeClass`) — mirroring how `FirToIrTransformer` pre-computes `RenderedType` today.

**The `fir-metadata.json` cache format does not change** (no serializer edit, no version bump). The new
fields are FIR-to-emitter side-channels only. Cache-loaded declarations never emit: checker-driven
emission fires only for **source** declarations, so consumer dedup holds by construction and the
byte-identity relocation test stays green for free.

## 5. `FirTypeRenderer` parity contract

New `compiler/.../fir/types/FirTypeRenderer.kt`:
`render(coneType, session, preserveTypeParameters): RenderedType` + semantics (`isTypeParameter`,
`requiresCollectionErasure`). The behavior contract is **byte parity** with the IrType renderer stack
(`TypeRenderer`/`GenericTypeHandler`/`FunctionTypeHandler`/`IrTypeSemantics`):

1. `fullyExpandedType(session)` first — IR types arrive typealias-expanded; Cone types may carry the
   abbreviation.
2. Nullability, including **parenthesized** nullable (suspend) function types: `((Int) -> Unit)?`.
3. Primitives by `ClassId`: String, Int, Boolean, Unit, Long, Float, Double, Char, Byte, Short, Nothing,
   Any.
4. `kotlin.FunctionN` → `(A, B) -> R`; `kotlin.coroutines.SuspendFunctionN` → `suspend (A) -> R`;
   extension-function-type receiver rendered as first parameter (match `FunctionTypeHandler`, pinned by a
   shared fixture).
5. `ConeTypeParameterType` → name when `preserveTypeParameters`, else `Any`.
6. Nested generic arguments recursively; `ConeStarProjection` → `*`; **`in`/`out` projection keywords
   dropped** (the IR renderer emits none).
7. NoGenerics erasure set (mirror `IrTypeSemantics.ERASABLE_COLLECTION_CLASSES`):
   `kotlin.collections.{List,MutableList,Set,MutableSet,Map,MutableMap,Collection}`, `kotlin.Result`,
   `kotlin.Array`.
8. Import FQN collection: recursive over type args; excludes primitives, type parameters, the outer
   `FunctionN`/`SuspendFunctionN` class, and `kotlin`/`kotlin.*` packages.
9. Defensive: flexible (Java) types render the lower bound; error types fall back to a logged best-effort
   string, never throw.

**The gate:** `FirIrEmissionParityTest` — each corpus fixture compiled twice in-process (`emitPhase=IR`
vs `FIR`), generated outputs **byte-diffed**. Corpus: existing integration fixtures plus star projections,
nullable/suspend function types, erasure shapes, vararg, typealiases in signatures, extension receivers,
operators, nested generics, inherited members, annotation propagation, and a Java-interop fixture
(flexible types — gates the JVM-path flip).

**Maintenance rule:** a parity failure is always fixed in `FirTypeRenderer` plus a new matrix row in
`FirTypeRendererTest` — never by forking generated-output expectations per phase.

## 6. Driver invocation reference

Verified against Kotlin `build-2.3.21-release-298`:

- FQN: `org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler` (the `K2MetadataCompiler` name is a
  hidden-deprecated alias — use the new name). Args class:
  `org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments`.
- Same `CLICompiler.exec(MessageCollector, Services, CommonCompilerArguments)` reflective surface; exit
  code checked via `Enum.name == "OK"` as today. `K2Fqns` gains the two FQNs; the bridge is parameterized
  by driver.
- Argument set for the common producer:
  - `freeArgs` = commonMain source paths (no `-Xcommon-sources` — single fragment, all sources common;
    the `/commonMain/` path filter is dropped for this driver)
  - `classpath` = commonMain's **real** `compileDependencyFiles` (metadata klibs; §7)
  - `moduleName` = `fakt-analysis`
  - `destination` = `scratchDir/metadata-klib` — **mandatory**; `MetadataConfigurationUpdater` errors with
    "Specify destination via -d" when absent. Output is a metadata klib we never read (`@LocalState`).
  - `multiPlatform = true` — redundant in 2.3.x (`K2MetadataCompilerArgumentsConfigurator` force-enables
    `LanguageFeature.MultiPlatformProjects`) but kept defensively for user-overridden `faktWorker`
    compiler versions.
  - `expectActualClasses = true` — mutes the expect/actual-classes Beta **warning** only (comment must say
    exactly that).
  - `pluginClasspaths`/`pluginOptions` unchanged.
  - **No** `noStdlib`/`noReflect` — those setters don't exist on `K2MetadataCompilerArguments`
    (JVM-only), and `K2CompilerBridge.setOnArgs` throws on missing setters; arg population branches per
    driver.
- "No source files" is a hard error on this driver; the task's `@SkipWhenEmpty` on `sources` already
  prevents empty invocations (NO-SOURCE).

### Spike findings (Step 1, both bets PASSED — verified 2026-07-06 on 2.3.20 embeddable)

Reflective harness: `KotlinMetadataCompiler.exec(PrintStream, vararg String)` over a commonMain fixture
containing an **unpaired `expect class` + `expect fun`** and a `@Fake interface` (property, nullable
generic return, suspend fun, expect-typed return), with the real `:compiler` shadowJar attached and
producer-mode `SourceSetContext`.

- **Bet (a) — driver:** exit `OK`; the unpaired expects produced **zero diagnostics** (no
  `NO_ACTUAL_FOR_EXPECT`, no error of any kind); Fakt's FIR checkers ran (`firCacheMode: PRODUCER`
  banner, `fir-metadata.json` written containing the interface); the metadata klib landed in scratch.
- **Bet (b) — FIR emission:** with a throwaway checker hook + crude `ConeType.toString()` sanitizer, a
  complete `FakeSessionServiceImpl.kt` (factory + config DSL + impl) was emitted from the FIR phase under
  the metadata driver — suspend lambda type, `List<String>?`, and the **expect type** `PlatformClock` all
  rendered and compilable in shape. The only defects were sanitizer artifacts (a `Simple(name=…)` leak in
  a default-value error message), i.e. precisely the gap the real `FirTypeRenderer` + parity gate close.
- **a1 (builtins/stdlib):** builtins do **not** resolve with an empty classpath — `kotlin.Any`,
  `kotlin.annotation.*` fail with "cannot access built-in declaration". The stdlib **metadata klib** must
  be on `-classpath`. The published `kotlin-stdlib-2.3.20-all.jar` is a *composite* archive (per-source-set
  klibs under `commonMain/…`), not readable directly; KGP's granular-metadata transform unpacks it, and
  `compileDependencyFiles` delivers the unpacked per-source-set klib dirs — feeding those (PR-4 wiring) is
  correct by construction. The unpacked `commonMain` klib dir on `-classpath` resolved everything.
  JVM `.class` jars are useless to this driver (confirms the two-driver split and the TestKit fixture
  strategy: in-fixture `@Fake` declaration + one E2E against the real `:annotations` metadata klib).
- **a3 (`-Xmulti-platform`):** omitting it still exits `OK` in 2.3.x (configurator force-enables MPP for
  the metadata driver) — kept defensively. Omitting `-Xexpect-actual-classes` exits `OK` with only the
  Beta warning — confirms it is warning-muting only.
- **a4 (`-d`):** omitting destination → `error: specify destination via -d`, `COMPILATION_ERROR`.
- **Bonus observation:** the checker-driven `fir-metadata.json` write happens even when the compilation
  later fails (it fired under the missing-stdlib COMPILATION_ERROR run). Task success still gates on exit
  `OK`, so no contract change — but worth knowing when debugging partial outputs.
- Environment note: the string-args `exec` overload needs `kotlin-reflect` on the worker JVM classpath
  (CLI argument parsing); the worker's Gradle-resolved `faktWorker` configuration already carries the
  embeddable's transitive deps, and the production path populates args objects directly, so no change.

## 7. Cache-key correctness

- **Klib classpath hole:** Gradle's `@CompileClasspath` normalizer fingerprints `.class` entries and can
  treat a klib (zip without `.class`) as effectively empty → changed common dependencies would not
  invalidate the producer. Fix: new `@get:Classpath commonKlibClasspath` input on `FaktGenerateTask`;
  commonMain producers feed their own `compileDependencyFiles` there. The
  `commonProducerClasspath()` JVM-target-borrowing hack is **deleted**.
- Locked by a new TestKit test: change a klib dependency → producer re-executes.
- Relocatability invariants unchanged: placeholder context JSON (`@Input`) never carries absolute paths;
  `emitPhase` serializes only when non-default; `fir-metadata.json` stays byte-deterministic
  (`generatedAt=0`, zeroed timings, canonicalized source paths). Existing FROM-CACHE-across-dirs and
  byte-identity tests remain the lock.
- **Output hygiene:** the worker clears `generatedKotlinDir` at start (stale-fake correctness on source
  deletion; safe — the dir is task-owned, and LEGACY_HYBRID platform compiles are ordered after the
  producer).

## 8. Routing table (after)

`FaktGradleSubplugin.cacheCorrectDecision` — the `kmp.targets.none { isDrivablePlatform } -> LEGACY`
branch is **deleted** (the producer no longer needs a JVM classpath):

| Compilation | Decision | Driver / phase |
|---|---|---|
| Single-platform JVM `main` | REGISTER_PRODUCER | K2JVM, FIR-emit |
| Single-platform non-JVM | LEGACY | in-process, IR |
| KMP `commonMain` (any target set, **incl. no JVM/Android target**) | REGISTER_PRODUCER | **KotlinMetadataCompiler**, FIR-emit |
| KMP other `platformType == common` metadata compilations | SUPPRESS | — |
| KMP JVM/Android platform `main` | REGISTER_CONSUMER | K2JVM, FIR-emit |
| KMP Native/JS/Wasm platform `main` | LEGACY_HYBRID | in-process, IR (ordered after producer) |

Platform-declared `@Fake` in `nativeMain`/`jsMain`/`wasmJsMain`/`iosMain` stays on LEGACY_HYBRID —
unchanged, correct, but not cache-correct (no Native/JS driver in the embeddable). Out of scope here;
CI presence checks continue to lock it.

## 9. Known hazards & out-of-scope

- **`tryLoadCache` early-return hazard (dormant):** `commonFirMetadata` is never wired by
  `FaktGenerateTaskWiring.register`, so worker "consumer" tasks actually run in plain mode. If consumer
  mode is ever wired, `MetadataCacheManager.tryLoadCache` returns `true` for **every** declaration once
  loaded, and the checker's early return would skip analysis (and FIR emission) of the platform's own
  source declarations. Do not wire consumer mode without fixing the short-circuit to be per-ClassId.
- **Worker options gap (pre-existing):** the worker forwards only 4 plugin options; `enableCallHistory`/
  `enableMutableFakes` extension settings are not among them. The FIR emitter resolves modes exactly as
  the IR path does, so FIR/IR parity holds — the worker-vs-legacy defaults gap is a separate issue to
  file, not fixed here.
- **Out of scope:** flipping `useExperimentalGenerateTask` default (P8); removing the legacy path (P9);
  cache-correct platform-declared fakes for Native/JS/Wasm; `actual typealias` and `@Fake`-on-expect
  scenarios (checker rejects `FAKE_CANNOT_BE_EXPECT` by design).

## 10. Verification map

| Invariant | Lock |
|---|---|
| Unpaired `expect` in commonMain no longer fails the producer | `FaktGenerateCommonProducerTest` #4 — flipped to assert SUCCESS + fake generated |
| FIR emission is byte-identical to IR emission | `FirIrEmissionParityTest` dual-compile corpus |
| ConeType rendering correctness per shape | `FirTypeRendererTest` BDD matrix |
| New FIR-captured fields (operator/extension/ctor params) | `FirFakeEmitterTest` content assertions |
| Legacy `@Input` JSON byte-parity | `SourceSetContext` serialization test (default omitted) |
| Producer cacheability + relocation | existing #5 (UP-TO-DATE), #6/#8 (FROM-CACHE across dirs), #7 (byte-identical fir-metadata.json) |
| Klib dependency invalidation | new TestKit test (change klib → re-execute) |
| Stale fakes removed on source deletion | new TestKit stale-file test |
| No JVM-only imports leak into common fakes | existing #1 (`java.`/`javax.` scan) |
| No-JVM-target KMP is cache-correct | new `samples/kmp-no-jvm` + CI cache-correctness row |
| Expect/actual end-to-end (the #79 reproducer) | `kmp-multi-target` expect fixtures + existing CI cache row |
| Platform-declared fakes still generated (not cache-correct) | existing CI presence checks |

Implementation sequence, work breakdown, risk register, and DOD live in the approved plan
(`feat/79-metadata-producer` series: emitPhase contract → renderer → emitter+parity gate → metadata
driver → JVM-path unification → routing+samples → docs).
