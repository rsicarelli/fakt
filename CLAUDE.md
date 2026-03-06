# CLAUDE.md - Fakt Compiler Plugin

Kotlin compiler plugin that generates type-safe test fakes at compile time using the `@Fake` annotation. Two-phase FIR -> IR compilation.

## Architecture

Fakt uses a two-phase compilation approach:

1. **FIR Phase** (`FaktFirExtensionRegistrar`) - Detects `@Fake` annotations, validates interfaces
2. **IR Phase** (`UnifiedFaktIrGenerationExtension`) - Generates implementation class + factory + config DSL

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
#    - InterfaceAnalyzer for analysis
#    - generation/ for code generation
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
- Keep FIR and IR phases separate
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
compiler/
├── FaktCompilerPluginRegistrar.kt          # Entry point: FIR + IR registration
├── UnifiedFaktIrGenerationExtension.kt     # Main IR generation
├── fir/FaktFirExtensionRegistrar.kt        # @Fake detection (FIR phase)
├── analysis/InterfaceAnalyzer.kt           # Interface metadata extraction
└── generation/
    ├── ImplementationGenerator.kt          # Fake class generation
    ├── FactoryGenerator.kt                 # Factory function generation
    └── ConfigurationDslGenerator.kt        # DSL generation
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
