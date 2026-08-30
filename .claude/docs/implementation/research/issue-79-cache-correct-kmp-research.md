# Issue #79 — Deep Research Plan: cracking cache-correct KMP fakes (the expect/actual blocker)

**Status:** historical — research phase (pre-implementation). Internal control doc, kept for the
reasoning trail. The blocker described below was solved by the metadata-driver producer
([metadata-producer-fir-emission.md](../architecture/metadata-producer-fir-emission.md)), and P8 has
since landed: `fakt.useExperimentalGenerateTask` defaults to `true`. Statements here about what
"cannot" be done yet describe the state at research time, not today's.
**Target Kotlin:** 2.3.20 (worker pins `FAKT_KOTLIN_VERSION = "2.3.20"`).
**Owner artifact for status:** GitHub issue #79.

---

## 0. Why we are researching (the decision to make)

The cache-correct commonMain **producer** drives `K2JVMCompiler` over **only** commonMain sources in
multiplatform mode. Any ordinary `expect` in commonMain (whose `actual` lives in a platform source set,
never fed to the producer) fails with `NO_ACTUAL_FOR_EXPECT` → the producer task throws → the build fails.
Proven by `FaktGenerateCommonProducerTest` (`.buildAndFail`). Real KMP commonMain almost always contains
`expect` declarations, so **we cannot flip the default (P8) until this is solved.**

We must choose between four candidate fixes. We do NOT yet have enough knowledge to choose:

| # | Approach | One-line bet | Biggest unknown |
|---|----------|--------------|-----------------|
| A | **Metadata-frontend driver** — drive the metadata compiler / frontend over commonMain | metadata mode is *designed* to compile commonMain alone → expects are fine | does source generation need IR? is the metadata entry point on the embeddable classpath? |
| B | **Analysis API standalone** — rebuild generation on `KaSymbol` (KSP2's model) | purpose-built for "read symbols → emit code" across KMP | stability, KMP support, artifact availability, rewrite cost |
| C | **Suppress `NO_ACTUAL_FOR_EXPECT` on K2JVM** — keep current driver, silence the diagnostic | smallest diff | is it suppressible? does suppression leave FIR usable, or corrupt it? |
| D | **Feed platform actuals** — add one target's actuals to the producer | keeps K2JVM honest | likely incoherent for a *shared* common producer (per-target divergence) |

**The pivotal question that ranks all four:** *does Fakt's `.kt`-source generation actually require the
IR (fir2ir) phase, or can it run purely at FIR / Analysis-API symbol level?* If frontend-only suffices,
A/B win and C/D become unnecessary. Resolve that first (see C1, R3).

---

## How to use this file

- **C1–C2** are *internal codebase* investigations (read our own source). Do these FIRST — cheap, and they
  may collapse the problem before any external research.
- **R1–R7** are *external deep-research* prompts (Kotlin compiler internals, KSP, Analysis API, docs, KEEP).
  Each is standalone and copy-pasteable into the deep-research tool. Prepend the **Shared context block**.
- **R8** is the synthesis prompt — run last, after R1–R7 land.
- Granularity is intentional: one answerable question per prompt. Don't merge them.

### Shared context block (prepend to every R-prompt)

> Context: "Fakt" is a Kotlin **compiler plugin** (two-phase FIR → IR) that generates test **fakes** for
> `@Fake`-annotated interfaces. A new cache-correct architecture (GitHub issue #79) moves generation out of
> `compileKotlin*` into a Gradle `@CacheableTask` that runs a worker hosting `kotlin-compiler-embeddable`
> **2.3.20**, driving `K2JVMCompiler` reflectively. For KMP it uses a **commonMain "producer"** task that
> generates platform-agnostic fake **source files** (`.kt`) which every target's test compilation consumes.
> The producer currently feeds **only commonMain sources** to `K2JVMCompiler` with `-Xmulti-platform` +
> `-Xexpect-actual-classes`; an unpaired `expect` in commonMain therefore fails with
> `NO_ACTUAL_FOR_EXPECT`. Fakt generates **source code** (not bytecode); it fundamentally needs to *read*
> an interface's shape (methods, properties, generics/variance, supertypes, nullability, suspend) and emit
> a source implementation. Answer for Kotlin **2.3.x / K2**. Prefer primary sources (JetBrains/kotlin repo,
> KEEP proposals, google/ksp, official Kotlin docs). Distinguish **fact** (cite file/URL) from **inference**.

---

## Part I — Internal pre-checks (do first; read our own code)

### C1 — Is the IR phase actually required to emit fake *source*, or is it incidental?

**Objective:** Determine whether the cache-correct worker's use of the IR phase is *load-bearing* for
generating `.kt` source, or whether all information the generator consumes is available at FIR /
analysis level.

**Do:** Read `UnifiedFaktIrGenerationExtension.kt`, `codegen-runtime` (the AST model + builders + renderer),
`InterfaceAnalyzer.kt`, and the worker (`FaktCodegenWorkAction.kt`, `CaptureIrGenerationExtension` history).
Trace: what data does the generator read to produce a fake, and at which phase is that data produced?
Does anything the renderer needs exist *only* after fir2ir (e.g. resolved default-value expressions,
synthetic members), or is it all FIR-symbol-level (signatures, types, supertypes)?

**Deliverable:** A verdict — "generation is FIR-only-capable" vs "generation genuinely needs IR because
X" — with the specific X cited at file:line. This single answer ranks approaches A–D.

### C2 — How does the LEGACY metadata-FIR-cache path run our FIR under commonMain *without* failing?

**Objective:** The legacy path (`SourceSetDiscovery` / `SourceSetConfigurator`) already writes
`fir-metadata.json` from the **real** `compileCommonMainKotlinMetadata` compilation — i.e. Fakt's FIR
extension already runs over commonMain in metadata mode and does NOT hit `NO_ACTUAL_FOR_EXPECT`. Understand
exactly why, and whether the cache-correct task can piggyback on / replicate that mechanism.

**Do:** Read the legacy metadata-cache wiring end to end. Confirm: is the FIR extension attached to the
genuine Gradle metadata compilation? What does it read/write? Then reconcile with memory note
`issue-79-kmp-metadata-driver-infeasible.md` ("metadata compilation has no IR phase") — was "infeasible"
concluded on the assumption that IR is required? If C1 says FIR-only suffices, is that conclusion void?

**Deliverable:** A reconciliation: exactly why legacy metadata FIR works, and whether the cache-correct
producer can reuse the metadata compilation (or its outputs) instead of driving K2JVM. Flag any reason it
can't (e.g., metadata task isn't cacheable-per-our-contract, or doesn't run IR we need per C1).

---

## Part II — External deep-research prompts

### R1 — KMP metadata compilation: why commonMain compiles alone without `NO_ACTUAL_FOR_EXPECT`

**Objective:** Nail the semantics of Kotlin's **metadata (common) compilation** and prove/disprove that it
is the natural home for the producer.

**Key questions:**
1. What exactly runs during `compileCommonMainKotlinMetadata` / the `K2MetadataCompiler` (a.k.a. metadata
   klib) path in K2? Which pipeline stages execute — FIR frontend only, or also fir2ir / a backend?
2. Why does compiling commonMain *by itself* NOT raise `NO_ACTUAL_FOR_EXPECT`? Is it that in metadata mode
   `expect` declarations are legitimately actual-less (actuals resolved only at platform compile time)?
   Cite where the compiler makes this distinction.
3. Can a compiler plugin's FIR extension run inside the metadata compilation and observe commonMain
   interfaces fully? Any limitation on symbol resolution in metadata mode?
4. Is there truly *no* IR phase in metadata mode, and precisely what does that preclude?

**Where to look:** JetBrains/kotlin (`K2MetadataCompiler`, metadata klib serialization, FIR metadata),
Kotlin docs on hierarchical KMP + metadata compilations, KEEP for klib/metadata.

**Deliverable:** A precise description of the metadata pipeline for 2.3.x, and a yes/no on "the producer can
be a metadata-frontend driver" — with the constraint(s) that decide it.

### R2 — `NO_ACTUAL_FOR_EXPECT`: origin, phase, and suppressibility on a JVM compilation

**Objective:** Scope the *cheap* fix (approach C): keep driving K2JVM but stop unpaired expects from being
fatal.

**Key questions:**
1. Which FIR checker/phase raises `NO_ACTUAL_FOR_EXPECT`, and what is its exact trigger?
2. What does `-Xexpect-actual-classes` actually do (we suspect only the Beta *warning*, not this error)?
3. Is there any supported lever to make unpaired expects non-fatal in a *platform* (JVM) compilation:
   a CLI flag, `-Xmetadata-klib`, `languageVersionSettings`, diagnostic suppression, `-Xsuppress-*`,
   `-Xexpect-actual-linker`, or a metadata/analysis-only JVM mode?
4. If suppressed, is the resulting FIR still sound enough for a plugin to *read* commonMain interfaces, or
   does suppression leave dangling/erroneous symbols that would break our checker or generation?

**Where to look:** JetBrains/kotlin FIR checkers (`FirExpectActualDeclarationChecker` and neighbors),
compiler CLI args, `CommonConfigurationKeys`, KEEP on expect/actual.

**Deliverable:** Verdict on approach C — is a safe suppression lever available in 2.3.20, and what's the
risk profile? Include the exact flag/API if one exists.

### R3 — Read-symbols-and-emit-source: what needs fir2ir vs what is FIR/Analysis-level?

**Objective:** Generalize C1 with authoritative knowledge: for *code generators*, what interface
information is available pre-IR (FIR symbols / Analysis API) vs only after fir2ir?

**Key questions:**
1. At FIR / Analysis-API level, can you fully resolve: function & property signatures, parameter names,
   nullability, generics + variance + bounds, supertypes, `suspend`, default-value *presence*, typealiases,
   value classes? Which of these are only reliable after fir2ir?
2. For Fakt's needs (emit a source implementation delegating to configurable behaviors), is any IR-only
   information required, or is FIR/Analysis sufficient?
3. How do established source generators (KSP, kapt-replacements) frame this — do any run post-IR, or all
   frontend-only?

**Where to look:** Kotlin Analysis API docs, KSP design docs, JetBrains/kotlin FIR symbol APIs.

**Deliverable:** A capability table (info item → available at FIR? at Analysis API? IR-only?) and a
conclusion on whether Fakt generation can be frontend-only.

### R4 — Kotlin Analysis API (Standalone / `KaSession`) for KMP code generation

**Objective:** Evaluate approach B — rebuild the producer's symbol reading on the Analysis API.

**Key questions:**
1. Does the **Standalone** Analysis API support setting up a session over a KMP **commonMain** source set
   (with classpath), and does it tolerate unpaired `expect`s (no `NO_ACTUAL_FOR_EXPECT`)?
2. Artifact reality for 2.3.x: what are the exact coordinates (`analysis-api-standalone`,
   `high-level-api-*`, etc.), are they published/consumable, and is any of it inside
   `kotlin-compiler-embeddable` or must we add deps? Stability/experimental status and API-churn risk?
3. What does session setup look like (project env, source roots, classpath), and what's the footprint/perf
   cost inside a Gradle worker per compilation?
4. Migration cost: could Fakt's `InterfaceAnalyzer` be re-expressed on `KaSymbol` while keeping the same
   `codegen-runtime` AST/renderer?

**Where to look:** Kotlin Analysis API guide, `analysis-api-standalone` samples, google/ksp (KSP2 uses it).

**Deliverable:** Feasibility + stability verdict for approach B, with coordinates, a minimal setup sketch,
and the rewrite surface.

### R5 — KSP2 `KspAATask`: the reference design for commonMain + producer→consumer wiring

**Objective:** The issue explicitly cited KSP2's `KspAATask` as the reference. Extract exactly how KSP
solves *our* problem.

**Key questions:**
1. How does KSP2 set up analysis for the **commonMain / metadata** target? Per-source-set? What entry point
   (Analysis API? a compiler driver)?
2. How does KSP avoid `NO_ACTUAL_FOR_EXPECT` on commonMain — lenient analysis, metadata mode, or feeding
   actuals? Be specific.
3. How does KSP's Gradle plugin attach its task to the metadata compilation and route **generated sources**
   from the common producer into each platform's compilation (the `srcDir` / task-dependency wiring)?
4. What did KSP learn the hard way (issues, caveats) about KMP + expect/actual + caching that we should
   pre-empt?

**Where to look:** google/ksp (`KspAATask`, multiplatform Gradle wiring), KSP multiplatform docs, KSP
issue tracker (KMP + expect/actual + metadata).

**Deliverable:** A "steal-this" summary: KSP's compilation setup + expect/actual handling + producer→
consumer Gradle wiring, mapped onto Fakt's producer/consumer model.

### R6 — `kotlin-compiler-embeddable` 2.3.20 surface audit (feasibility gate)

**Objective:** Bound which approaches are even *possible* with what the worker already ships.

**Key questions:**
1. Which entry points are present in `kotlin-compiler-embeddable:2.3.20`: `K2JVMCompiler` (known yes),
   `K2MetadataCompiler` / `KotlinMetadataCompiler`, `KotlinToJVMBytecodeCompiler`, any Analysis-API or FIR
   standalone building blocks?
2. For each approach (A metadata driver, B Analysis API, C suppress-on-K2JVM, D feed-actuals): what extra
   artifacts/coordinates would we need to add to the worker classpath, and are they ABI-stable across
   2.3.x?
3. Any classpath/shadowing hazards (relocation, duplicate FIR) from adding metadata or analysis-api deps
   next to the embeddable jar?

**Where to look:** the published `kotlin-compiler-embeddable` 2.3.20 jar contents / Maven, Kotlin release
notes, KGP internals.

**Deliverable:** A feasibility matrix: approach → shippable on embeddable today? → extra deps → stability.

### R7 — Feed-the-actuals viability for a *shared* common producer (approach D scoping)

**Objective:** Confirm or kill approach D quickly.

**Key questions:**
1. If the common producer additionally feeds *one* target's actuals (e.g. jvmMain), is the generated
   fake still correct for **all** targets, or does it become JVM-tainted (per-target divergence,
   platform-typed members)?
2. What happens with multiple/divergent actuals across targets? Is there any coherent "feed all actuals"
   variant, or does it defeat the single-shared-producer design?
3. Would this reintroduce cache-correctness or task-dependency problems (consumer↔producer cycles)?

**Deliverable:** A short verdict — is D ever coherent for a shared producer, or is it a dead end? If dead,
say so decisively so we stop considering it.

### R8 — Synthesis & recommendation (run last)

**Objective:** Turn R1–R7 + C1–C2 into a decision.

**Do:** Build a trade-off matrix across approaches A–D scored on: (1) correctly handles expect/actual for
all platforms, (2) preserves cache-correctness + relocatability, (3) feasible on embeddable 2.3.20 /
acceptable new deps, (4) implementation effort & blast radius, (5) forward-compat (K3/K4, API stability),
(6) unlocks Native/JS/Wasm too, or is JVM/common-only. Recommend ONE, with a target-architecture sketch,
the top 3 risks, and a spike to de-risk it before committing.

**Deliverable:** A ranked recommendation and a one-paragraph target design that the implementation prompt
can be written against.

---

## What the research must let us answer (exit criteria)

1. **Frontend-only?** Can Fakt generate fakes without fir2ir? (C1, R3) — *ranks everything.*
2. **Metadata driver on embeddable?** Is a metadata/analysis frontend drivable in our worker, and does it
   make expect/actual a non-issue? (R1, R4, R6)
3. **Cheap escape hatch?** Is suppressing `NO_ACTUAL_FOR_EXPECT` on K2JVM safe? (R2)
4. **Reference proof?** How does KSP2 already do this? (R5)
5. **Chosen approach + spike.** (R8)

Only after these do we write the implementation prompt (with: chosen approach, exact APIs/flags, the
producer wiring change, and the expect/actual sample + E2E test to add — currently zero coverage).

---

# RESULTS (verified 2026-07-06 against Kotlin 2.3.21-RC2 `build-2.3.21-release-298`, KSP 2.3.0-290 / Kotlin 2.3.20, Fakt HEAD `5b5e41e1`)

All 9 investigations complete. Every finding below is from primary source (local Kotlin/KSP/Fakt), cited in the agent transcripts.

## Findings by prompt

- **C1 — Does generation need IR? NO.** Generation is **FIR-only-capable**. `codegen-runtime`'s renderer
  (`FaktCodegen.render`) consumes a pure string `FakeDeclaration` with **zero** IR imports. The FIR checker
  (`FakeInterfaceChecker.analyzeMetadata`) already extracts the full model. The worker runs fir2ir *only*
  to fire `IrGenerationExtension.generate()` and hand it `IrType`s for type-string rendering; the bytecode
  goes to a scratch dir that is never read (`FaktCodegenWorkAction.kt:201-209`). To go FIR-only: (1) add a
  `ConeType→String` renderer to replace the `IrType`-based `TypeRenderer` (the `TypeResolution` facade is
  the only IR coupling), (2) capture 3 fields during FIR extraction that are currently read off IR nodes:
  `isOperator`, extension-receiver type, class constructor params. **No missing frontend data.**
- **C2 — Legacy metadata FIR cache.** Fakt already attaches to the *real* `compileCommonMainKotlinMetadata`
  and writes `fir-metadata.json` **from the FIR phase** (`FakeInterfaceChecker.kt:137-139`, with the comment
  "…even if IR phase doesn't run (metadata compilation doesn't have IR phase)"). It doesn't fail on expects
  because it piggybacks Kotlin's lenient metadata frontend. The expect failure in the new path is a
  **consequence of the cache-correct task needing to own generated `.kt` as `@OutputDirectory`**, which
  forced IR → a platform driver (K2JVM) → the two-fragment actualizer.
- **R1 — Metadata pipeline.** `KotlinMetadataCompiler` → `MetadataCliPipeline` is **frontend-only** (FIR
  resolve + checkers → direct FIR→klib serialize; **no fir2ir/actualizer**). Builds **one session over
  exactly the sources passed** (`metadataCompilationMode=true`). **FIR extensions register and run** over
  commonMain symbols. **`IrGenerationExtension` never fires.** Must pass **`-Xmulti-platform`** (else
  `NOT_A_MULTIPLATFORM_COMPILATION` on every `expect`).
- **R2 — The diagnostic.** `NO_ACTUAL_FOR_EXPECT` is an **IR-actualizer** diagnostic (`IrActualizationErrors.kt:26`),
  fires **only with ≥2 module fragments** (common+platform) — exactly what K2JVM + `-Xmulti-platform` +
  `-Xcommon-sources` creates. Metadata mode has no actualizer → **impossible** there. `-Xexpect-actual-classes`
  only mutes the Beta **warning** (the worker's comment at `FaktCodegenWorkAction.kt:218-219` is **wrong**).
  Escape hatch on K2JVM: **`-XXlenient-mode`** (2.2.0+, experimental `XX`) stubs missing **top-level** actuals,
  keeps `hasErrors=false` so IR generation runs — but does NOT cover members of partially-actualized classes,
  value classes, or non-`Any`-superclass expects (those still fail). Safe for reading unrelated `@Fake` (FIR
  resolves before actualization). Note: today, once the error fires, `convertToIr.kt` **skips
  `applyIrGenerationExtensions`** — that's why nothing is generated.
- **R4 — Analysis API.** Technically feasible and structurally solves expect/actual (lazy, diagnostic-based,
  no link; unpaired `expect` returns full symbols — proven by standalone common-only tests). Exposes every
  field Fakt needs. **But** ships as ~7 un-relocated `*-for-ide` jars, **not** in the embeddable; the
  Standalone surface is officially **"Unstable"** and Kotlin-version-coupled.
- **R5 — KSP2.** Proves frontend-only sidesteps the failure (empty diagnostic storage + common platform +
  resolve-only; a test with unpaired expects passes). **But** KSP's KMP model is **per-platform generation
  reading common inputs**, *not* a common-producer→consumer `srcDir` handoff — its dedicated common pass is
  unfinished (`FIXME: targets`), and it deliberately does NOT add common-generated sources to the common
  source set. Uses the Standalone AA (heavy classpath). Task is `@CacheableTask`, relocatable I/O.
- **R6 — Embeddable surface.** `kotlin-compiler-embeddable:2.3.20` ships **both** `K2JVMCompiler` **and**
  `KotlinMetadataCompiler` (zero extra deps, no relocation hazard). **Analysis API is NOT in it** — only
  un-relocated `*-for-ide` jars exist, which collide catastrophically with the shaded embeddable. `K2Native`
  is not in it either (needs the Konan superset).

## Trade-off matrix

| Approach | expect/actual | Cache-correct | On embeddable? | Effort | Fwd-compat | Unlocks Native/JS/Wasm + no-JVM-target KMP |
|---|---|---|---|---|---|---|
| **A. Metadata driver + FIR-hosted generation** | ✅ all cases (structural) | ✅ preserves `@CacheableTask` | ✅ `KotlinMetadataCompiler`, **0 deps** | **Med–High** (re-host gen to FIR + swap driver) | Med (reflective CLI, same style as today) | ✅ common fakes for **all** targets; **removes the JVM/Android-target requirement** |
| **C. `-XXlenient-mode` on K2JVM** | ⚠️ top-level expects only | ✅ unchanged | ✅ today | **Tiny** (one flag) | ⚠️ `XX` experimental flag | ❌ still needs a drivable JVM/Android target |
| **B. Analysis API (KSP-style)** | ✅ all cases | ✅ (KSP proves) | ❌ abandons embeddable; ~7 `*-for-ide` jars | **High** (rewrite analyzer on `KaSymbol` + classpath mgmt) | ⚠️ "Unstable" surface, version-coupled | ✅ but heavy classpath |
| **D. Feed platform actuals** | ⚠️ per-target taint | — | ✅ | Med | — | ❌ incoherent for a *shared* common producer — **dead** |

## RECOMMENDATION

**Primary: Approach A — drive `KotlinMetadataCompiler` over commonMain + re-host generation on a FIR hook.**
This is the durable, correct fix and the only one that is simultaneously (a) structurally immune to
`NO_ACTUAL_FOR_EXPECT` (no actualizer runs), (b) shippable **in-place on `kotlin-compiler-embeddable` with
zero new dependencies**, and (c) able to generate common fakes for **every** target — Native/JS/Wasm
included as consumers — while **eliminating the "requires a JVM/Android target" limitation** (metadata is
the common compiler; no platform needed). It also lets the producer feed commonMain's **real** klib
classpath instead of today's JVM-target-deps hack. Enabled by C1 (generation is FIR-only-capable) + C2
(cache already writes from FIR).

**Fast interim (optional): Approach C — add `-XXlenient-mode`** to the current K2JVM producer to unblock
the common (top-level `expect`) case quickly. Use ONLY as a stopgap with a **detect-and-fallback** guard for
the unsupported expect shapes (value-class / non-Any-superclass / member-level), because on default-on those
would otherwise regress real projects. Not a substitute for A.

**Reject for now: Approach B (Analysis API).** Most principled long-term, and what KSP uses, but it trades the
compile-failure problem for a **heavy, Kotlin-version-coupled, officially-Unstable** classpath that forces
abandoning the embeddable. Keep as a future spike only if A hits a wall. **Approach D is dead.**

### Spike to de-risk A before committing
1. In the worker, drive `org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler` over a commonMain source
   set with `-Xmulti-platform` + commonMain klib classpath; confirm Fakt's FIR checker runs and sees the
   `@Fake` interfaces, and confirm an unpaired `expect` in commonMain does **not** fail the run.
2. Prove the FIR-phase `.kt` write: emit one fake's source from the checker path (Fakt already writes the
   cache there) using a throwaway `ConeType→String` renderer for a couple of representative signatures.
3. If both hold, proceed to the full re-host (ConeType renderer + 3 captured fields + swap producer driver +
   rewire `@OutputDirectory`), then add the missing **expect/actual sample + E2E cache-correctness test**
   (currently zero coverage), then flip the default (P8) → remove legacy (P9).

### Still open / not yet researched
- **R7 (feed-actuals coherence)** — folded into the matrix as "dead"; not separately run.
- **Platform-specific `@Fake` declared directly in `nativeMain`/`jsMain`/`wasmJsMain`** (not commonMain):
  Approach A covers *common* fakes for all targets, but a `@Fake` living in a Native/JS/Wasm source set still
  can't be driven (no `K2NativeCompiler` in the embeddable). Those remain on legacy-hybrid unless a separate
  per-platform story is added. Scope this when writing the implementation plan.
