# Contributing to Fakt

Thank you for your interest in contributing to Fakt! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Commit Convention](#commit-convention)
- [Community](#community)

---

## Getting Started

### Prerequisites

- **JDK 21** (Temurin recommended)
- **Kotlin 2.2.0+** ([compatibility matrix](https://rsicarelli.github.io/fakt/compatibility/))
- **Gradle 8.0+** (wrapper provided)
- **Git**

### Quick Start

```bash
# Clone the repository
git clone https://github.com/rsicarelli/fakt.git
cd fakt

# Publish plugin locally (⭐ use this for development!)
make publish-local
# or: ./gradlew publishToMavenLocal

# Run tests
make test
# or: ./gradlew test

# Test all samples
./gradlew :samples:jvm-single-module:build
./gradlew :samples:android-single-module:build
./gradlew :samples:kmp-single-module:build
./gradlew :samples:kmp-multi-module:build

# Or use Makefile shortcuts
make test-sample  # Tests kmp-single-module by default
```

**Testing Across Platforms:**
- `jvm-single-module`: Tests JVM-only compilation and code generation
- `android-single-module`: Tests Android Library setup and AGP integration
- `kmp-single-module`: Tests multiplatform code generation and KLIB propagation

```

---

## Development Setup

### 1. Fork and Clone

```bash
# Fork the repository on GitHub
# Clone your fork
git clone https://github.com/YOUR_USERNAME/fakt.git
cd fakt

# Add upstream remote
git remote add upstream https://github.com/rsicarelli/fakt.git
```

### 2. Create a Feature Branch

```bash
# Update main
git checkout main
git pull upstream main

# Create feature branch
git checkout -b feature/my-awesome-feature
```

### 3. Build and Test

```bash
# Publish plugin locally (⭐ correct workflow for development)
make publish-local
# This compiles, generates shadowJar, and publishes to ~/.m2/repository

# Run all validations
make test              # Tests
./gradlew detekt       # Static analysis
./gradlew spotlessCheck # Format check
./gradlew checkLicense  # License audit

# Test with samples (composite builds auto-rebuild plugin!)
make test-sample       # Single-module sample
```

### 4. IDE Setup

**IntelliJ IDEA (Recommended):**
1. Open `fakt/` directory
2. Import as Gradle project
3. Wait for Gradle sync
4. Enable Kotlin plugin
5. Configure JDK 21

**Code Style:**
- Formatting: Spotless (ktfmt Google style)
- Run `make format` or `./gradlew spotlessApply` before committing

---

## Making Changes

### Project Structure

```
fakt/
├── compiler/           # Main compiler plugin (FIR + IR)
├── compiler-api/       # Serialization models
├── gradle-plugin/      # Gradle plugin
├── annotations/        # @Fake annotation (KMP, zero runtime overhead)
├── samples/            # Integration test samples
└── build-logic/        # Convention plugins
```

### Key Areas

**Compiler Plugin (FIR + IR):**
- FIR phase: `compiler/src/main/kotlin/.../fir/`
- IR phase: `compiler/src/main/kotlin/.../codegen/`
- Always check [Metro](https://github.com/ZacSweers/metro) for patterns

**Code Generation:**
- Implementation: `compiler/src/main/kotlin/.../generation/ImplementationGenerator.kt`
- Factory: `compiler/src/main/kotlin/.../generation/FactoryGenerator.kt`
- Config DSL: `compiler/src/main/kotlin/.../generation/ConfigurationDslGenerator.kt`

**Testing:**
- Location: `compiler/src/test/kotlin/`
- **ABSOLUTE REQUIREMENT:** GIVEN-WHEN-THEN pattern
- Framework: Vanilla JUnit5 + kotlin-test
- See: `.claude/docs/validation/testing-guidelines.md`

---

## Testing Guidelines

### The Absolute Standard: GIVEN-WHEN-THEN

**✅ REQUIRED:**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyFeatureTest {
    @Test
    fun `GIVEN interface with suspending function WHEN generating fake THEN should compile successfully`() = runTest {
        // GIVEN
        val interface = createInterface()

        // WHEN
        val result = generateFake(interface)

        // THEN
        assertTrue(result.compiles)
    }
}
```

**❌ PROHIBITED:**
```kotlin
// "should" naming
fun `should generate fake for interface`()

// Custom BDD frameworks
class `MyFeatureSpec` : StringSpec({ ... })

// Mocks (use fakes instead)
val mock = mockk<Service>()
```

**Rules:**
- ✅ UPPERCASE GIVEN-WHEN-THEN in test names
- ✅ `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`
- ✅ Isolated instances (no shared state)
- ✅ Vanilla JUnit5 + kotlin-test assertions
- ✅ Fakes instead of mocks
- ❌ NO "should" pattern
- ❌ NO custom BDD frameworks
- ❌ NO custom matchers

**Full Specification:** `.claude/docs/validation/testing-guidelines.md`

---

## Pull Request Process

### 1. Before Submitting

**Run all validations (single command):**
```bash
# ⭐ Run all checks like CI does
make validate

# Or manually format first, then validate
make format        # Auto-fix formatting issues
make validate      # Run all validations
```

**The `validate` target runs:**
1. ✅ Format & lint checks (spotless, ktlint)
2. ✅ Static analysis (detekt)
3. ✅ License audit
4. ✅ All tests
5. ✅ Plugin publishing (local Maven)
6. ✅ Sample builds (integration test)

### 2. Commit Your Changes

**Use Developer Certificate of Origin (DCO):**
```bash
git add .
git commit -s -m "feat: add support for generic types"
```

The `-s` flag adds `Signed-off-by: Your Name <your.email@example.com>` to your commit.

**By signing off, you certify that:**
- You have the right to submit the contribution under Apache 2.0
- You agree to the [Developer Certificate of Origin](https://developercertificate.org/)

### 3. Push and Create PR

```bash
git push origin feature/my-awesome-feature
```

**Create Pull Request on GitHub:**
- Fill out the PR template
- Link any related issues
- Ensure all CI checks pass

### 4. Review Process

**What happens next:**
1. Automated checks run (ktlint, detekt, tests, etc.)
2. Maintainer reviews code
3. Discussion/changes if needed
4. Approval and merge

**CI Checks (must all pass):**
- ✅ validate-ktlint
- ✅ validate-detekt
- ✅ validate-spotless
- ✅ validate-licenses
- ✅ run-tests
- ✅ build-samples

---

## Commit Convention

While not strictly enforced, we encourage [Conventional Commits](https://www.conventionalcommits.org/) for clarity:

**Format:** `<type>(<scope>): <description>`

**Types:**
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `refactor:` - Code refactoring
- `test:` - Test additions/changes
- `chore:` - Build/tooling changes

**Examples:**
```bash
feat(compiler): add support for generic return types
fix(gradle-plugin): correct source set mapping
docs(readme): update installation instructions
refactor(ir): simplify IrTypeResolver logic
test(codegen): add tests for nested interfaces
chore(deps): update Kotlin to 2.2.11
```

**Breaking Changes:**
```bash
feat!: change @Fake annotation parameters

BREAKING CHANGE: The @Fake annotation now requires explicit target specification
```

---

## Community

### Where to post what

Fakt uses **Issues** for reproducible bugs and regressions only. Everything
else lives in **[Discussions](https://github.com/rsicarelli/fakt/discussions)**:

- **🙏 Q&A** — usage questions, "how do I…", build failures without a clear bug
- **💡 Ideas** — API changes, new annotations, pipeline improvements (design lives in public before code)
- **🙌 Show & tell** — projects, blog posts, or talks built with Fakt

If you open an Issue that's actually a question or a feature request,
expect it to be converted to a Discussion — it's not a rejection, it's filing.

### Getting Help

- **Bug reports:** [Issues → Bug Report](https://github.com/rsicarelli/fakt/issues/new?template=bug_report.yml)
- **Questions, ideas, show & tell:** [Discussions](https://github.com/rsicarelli/fakt/discussions)
- **Documentation:** [Read our guides](https://rsicarelli.github.io/fakt/)

### Reporting Bugs

**Use the bug report template:**
1. Go to [Issues → Bug Report](https://github.com/rsicarelli/fakt/issues/new?template=bug_report.yml)
2. Fill out the required information:
   - Fakt version
   - Kotlin version
   - Project type (single/multi-module, KMP)
   - What's happening (description + expected/actual behavior)
   - Steps to reproduce
   - Build logs & minimal reproduction (with DEBUG logging enabled)
   - Platform info (if KMP)

### Suggesting Ideas

**We love feature ideas!** Your input directly influences the roadmap.

**Open an idea discussion:**
1. Go to [Discussions → 💡 Ideas](https://github.com/rsicarelli/fakt/discussions/new?category=ideas)
2. Describe:
   - What problem this solves
   - Proposed solution (with code examples)
   - Alternatives considered (optional)
   - Your project setup (optional)

Browse [open ideas](https://github.com/rsicarelli/fakt/discussions/categories/ideas) and react to ones you'd use — reactions are how I prioritize.

---

## License

By contributing to Fakt, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).

---

## Questions?

If you have questions not covered here, feel free to reach out to [@rsicarelli](https://github.com/rsicarelli)

Thank you for contributing! 🚀
