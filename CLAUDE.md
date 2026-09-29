# CLAUDE.md - Fakt Compiler Plugin

Kotlin compiler plugin that generates type-safe test fakes at compile time using the `@Fake` annotation. By default the Gradle plugin runs the compiler in a cacheable `FaktGenerateTask` worker, and fakes are emitted at the FIR phase. The legacy in-process IR emission is being removed for 1.0 (see #150).

## Architecture

> **v1.0 in progress:** the legacy in-process path is being removed. Tracker: #150.
> Handover: `.claude/docs/implementation/v1-sunset-handover.md`.

1. **Default: FIR emission** (`FaktGenerateTask` -> Gradle Worker -> embedded K2 with Fakt as `-Xplugin`).
   `FaktFirExtensionRegistrar` detects and validates `@Fake`; the FIR checkers emit the fakes
   (`fir/generation/`) as declared, cache-correct task outputs.
2. **Legacy: IR emission** (`fakt.useExperimentalGenerateTask=false`, and shapes still routed
   `LEGACY`/`LEGACY_HYBRID`). The plugin runs inside `compileKotlin*`;
   `UnifiedFaktIrGenerationExtension` generates. **Do not add features here.** It is deleted for 1.0.

Both paths share the codegen in `codegen-runtime/` (the `FakeDeclaration` model -> generators).

Output per `@Fake` interface: `FakeXxxImpl` class, `fakeXxx {}` factory function, `FakeXxxConfig` DSL.

See [Architecture doc](.claude/docs/implementation/architecture/ARCHITECTURE.md) for details.

## Essential Commands

```bash
make publish-local    # Build + publish to Maven Local (use this for development)
make test-sample      # Test KMP sample project
make quick-test       # Rebuild plugin + test sample (no cache)
make full-rebuild     # Clean + rebuild everything
make debug            # Show Fakt-specific compiler logs
make format           # Format code (required before commits)
make clean            # Clean build artifacts
make help             # Show all commands
```

`publish-local` automatically compiles, builds shadowJar, creates artifacts, and publishes to `~/.m2/repository`. Skips GPG signing locally.

## Code Conventions

### Naming

```kotlin
@Fake interface UserService
// -> FakeUserServiceImpl    (implementation class, same package)
// -> fakeUserService {}     (factory function)
// -> FakeUserServiceConfig  (configuration DSL)

// Behavior properties (constructor parameters)
// -> private val getUserBehavior: () -> User = { ... }
// -> FakeUserServiceConfig: internal var getUserBehavior: (() -> User)? = null
```

### Style

- License: Apache 2.0 (managed by Spotless)
- Formatting: ktfmt Google style
- Max line length: 100 characters
- Import order: Standard Kotlin -> Third-party -> Project

## Testing Standard

Full spec: [Testing Guidelines](.claude/docs/development/validation/testing-guidelines.md)

**Required:**
- Vanilla JUnit5 + Kotlin Test only
- `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` always
- GIVEN-WHEN-THEN naming (uppercase, BDD style)
- `runTest` for coroutines
- Isolated instances per test (no shared state)
- Fakes instead of mocks

**Prohibited:**
- "should" naming pattern
- Custom BDD frameworks or matchers (no assertThat, etc.)
- Mocks
- @BeforeEach/@AfterEach

## Development Workflow

```bash
# 1. Write failing test first (TDD)
# 2. Implement in appropriate module:
#    - compiler/.../fir/ for analysis and emission
#    - codegen-runtime/ for the generated code shape
#    - gradle-plugin/ for task wiring and routing
# 3. Rebuild and test
make publish-local && make test-sample
# 4. Format
make format
```

Always test with published plugin (`publishToMavenLocal`), not just project dependencies.
Use `--info` flag to debug compiler plugin behavior.
Test both single-platform and KMP scenarios.

## Do's and Don'ts

### Always

- Use `make` commands from project root
- Test with `publishToMavenLocal` before claiming success
- Verify generated code compiles without errors
- Put new generation behaviour on the FIR/worker path; don't extend `ir/`
- Check #150 before starting v1.0 work
- Generate code in test source sets only
- Write GIVEN-WHEN-THEN tests for all new features
- Format with `make format` before commits
- Use modular design (analysis -> generation -> output)
- Clear, actionable error messages with interface name and location

### Never

- Skip compilation testing
- Use deprecated Kotlin APIs
- Generate code in main/production source sets
- Use `buildDir` (deprecated in Gradle 8+)
- Mix FIR and IR phase logic
- Ignore cross-module import resolution
- Hardcode output directories
- Use "should" naming or custom test frameworks
- Share state between tests or use @BeforeEach/@AfterEach
- Use mocks instead of fakes

## Key Files

```
compiler/.../compiler/
├── FaktCompilerPluginRegistrar.kt               # Entry point: FIR (+ legacy IR) registration
├── fir/FaktFirExtensionRegistrar.kt             # @Fake detection/validation
├── fir/generation/                              # FIR emission (default path)
└── ir/generation/UnifiedFaktIrGenerationExtension.kt  # LEGACY IR emission (removed in 1.0)
codegen-runtime/.../codegen/
├── analysis/FakeDeclaration.kt                  # Shared model
└── generator/                                   # ImplementationGenerator, ConfigurationDslGenerator
gradle-plugin/.../gradle/
├── FaktGradleSubplugin.kt                       # Routing (cacheCorrectDecision)
├── FaktGenerateTask.kt, worker/FaktCodegenWorkAction.kt
└── android/                                     # AGP seams
```

## Documentation

| Document | Path |
|----------|------|
| Architecture | `.claude/docs/implementation/architecture/ARCHITECTURE.md` |
| KMP Strategy | `.claude/docs/implementation/architecture/kmp-optimization-strategy.md` |
| Testing Guidelines | `.claude/docs/development/validation/testing-guidelines.md` |
| Kotlin API Reference | `.claude/docs/development/kotlin-api-reference.md` |
| Kotlin IR API | `.claude/docs/development/kotlin-compiler-ir-api.md` |
| Troubleshooting | `.claude/docs/troubleshooting/common-issues.md` |
| v1.0 sunset handover (tracker: #150) | `.claude/docs/implementation/v1-sunset-handover.md` |
