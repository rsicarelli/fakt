# `:compiler-runner`

Headless entry point for Fakt's FIR + IR analysis pipeline. Lets a Gradle `@CacheableTask` run the
same FIR validation and metadata extraction that the in-process compiler subplugin runs today —
but outside `compileKotlin*`, so the generated `.kt` files become declared task outputs and the
build cache restores them correctly (issue [#79]).

## Why this module exists

Fakt's compiler plugin (in `:compiler`) is loaded by the Kotlin Gradle Plugin into `compileKotlin*`.
That works for code generation but breaks the Gradle build cache: the generated `.kt` files are
written as side effects, never declared as outputs, and so a cache hit on `compileKotlin*` restores
only `.class` files — leaving downstream test compilations to fail with `Unresolved reference`.

The committed fix (plan: `feat/cache-correct-PR-1-spike` and onward) wraps Fakt's analysis in a
separate `@CacheableTask` whose `@OutputDirectory` is wired into the consumer source set via
`KotlinSourceSet.kotlin.srcDir(taskProvider)`. Reference architecture: KSP2's `KspAATask`.

For that task to exist, Fakt needs an entry point that drives K2 + the existing FIR/IR extensions
without the KGP subplugin context. That is `FaktAnalysisRunner`.

## Public surface

- `FaktAnalysisRunner.run(sources, classpath, sourceSetContext): List<FakeDeclaration>` — boots
  K2 in-process, registers Fakt's existing `FaktFirExtensionRegistrar`, captures `FakeDeclaration`s
  via a custom IR extension, and returns them. Pure; no filesystem writes.

The IR-side rendering (turning each `FakeDeclaration` into a `RenderedFakeFile`) lives in
`:codegen-runtime` (`FaktCodegen.render`). The Gradle task in PR 2 calls `FaktAnalysisRunner.run`
to get declarations, then `FaktCodegen.render` to produce file contents, and writes them under
its declared `@OutputDirectory`.

## Status

Spike (PR 1). Validates the architecture before the Gradle task lift in PR 2. If the spike fails
its hard exit criteria (relocatable cache hit, KMP target sweep, no Metaspace OOM, configuration-
cache clean), this module is reverted and the team falls back to a `outputs.cacheIf { false }`
stopgap; KSP migration is explicitly out of scope per project owner.

[#79]: https://github.com/rsicarelli/fakt/issues/79
