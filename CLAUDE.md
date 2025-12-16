# 🤖 CLAUDE.md - Fakt Compiler Plugin

> **Production-ready Kotlin compiler plugin for type-safe fake generation**
> **Last Updated**: November 2025

## 🎯 What is Fakt?

**Fakt** is a Kotlin compiler plugin that generates type-safe test fakes at compile time using the `@Fake` annotation. Fakt follows a two-phase FIR → IR compilation approach to analyze interfaces and generate production-quality fake implementations.

**Problem Solved:**
Writing test fakes manually is tedious and error-prone. Fakt generates type-safe fakes automatically with a clean DSL for configuring behavior, eliminating boilerplate while maintaining compile-time safety.

## 🏗️ Architecture

### **Two-Phase Compilation (FIR → IR)**

```
┌─────────────────────────────────────────────────────────────────┐
│  PHASE 1: FIR (Frontend Intermediate Representation)           │
│  ════════════════════════════════════════════════════════       │
│  • FaktFirExtensionRegistrar                                    │
│  • Detects @Fake annotations on interfaces                     │
│  • Validates interface structure                               │
│  • Passes validated interfaces to IR phase                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  PHASE 2: IR (Intermediate Representation)                     │
│  ════════════════════════════════════════════════════════       │
│  • UnifiedFaktIrGenerationExtension                             │
│  • InterfaceAnalyzer: Extracts interface metadata              │
│  • IrCodeGenerator: Generates IR nodes                         │
│  • Outputs: Implementation class + Factory + Config DSL        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  OUTPUT: Generated Kotlin Code (commonTest/ or test/)          │
│  ════════════════════════════════════════════════════════       │
│  • FakeXxxImpl.kt         - Fake implementation class          │
│  • fakeXxx() factory      - Type-safe factory function         │
│  • FakeXxxConfig          - Configuration DSL                  │
└─────────────────────────────────────────────────────────────────┘
```

### **Key Components**

```kotlin
fakt/
├── compiler/                          # Main compiler plugin
│   ├── FaktCompilerPluginRegistrar.kt   # Entry point (Metro pattern)
│   ├── UnifiedFaktIrGenerationExtension.kt  # IR generation
│   ├── fir/
│   │   └── FaktFirExtensionRegistrar.kt     # @Fake detection (FIR phase)
│   ├── analysis/
│   │   └── InterfaceAnalyzer.kt             # Interface structure analysis
│   ├── generation/
│   │   ├── ImplementationGenerator.kt       # Fake class generation
│   │   ├── FactoryGenerator.kt              # Factory function generation
│   │   └── ConfigurationDslGenerator.kt     # DSL generation
│   └── types/
│       ├── TypeResolver.kt                  # Type system handling
│       └── ImportResolver.kt                # Cross-module imports
├── annotations/                       # Multiplatform annotations (zero runtime overhead)
│   └── @Fake                             # Main annotation
├── gradle-plugin/                     # Gradle integration
└── samples/
    ├── kmp-single-module/             # ✅ Working KMP example
    └── kmp-multi-module/              # Complex KMP multi-module
```

## ⚡ Essential Commands

### **Development Workflow**

```bash
# 📤 Publish to Maven Local (⭐ USE THIS for development!)
make publish-local                                # Compiles + shadowJar + publishes (no signing locally!)

# 🧪 Test working example
make test-sample                                  # or: cd fakt && ./gradlew :samples:kmp-single-module:build

# ⚡ Quick rebuild cycle (no cache)
make quick-test                                   # Rebuild plugin + test sample fresh

# 💥 Nuclear option (full clean rebuild)
make full-rebuild                                 # Clean + rebuild everything

# 🔍 Debug compiler plugin output
make debug                                        # Show Fakt-specific logs

# ✨ Format code (required before commits)
make format                                       # or: cd fakt && ./gradlew spotlessApply

# 🧹 Clean build artifacts
make clean                                        # or: cd fakt && ./gradlew clean

# 🏗️ Build shadowJar only (debug/CI only - not needed for local dev!)
make shadowJar                                    # or: cd fakt && ./gradlew :compiler:shadowJar

# 📚 Show all commands
make help
```

**💡 Important:** `publish-local` (publishToMavenLocal) automatically:
- Compiles Kotlin sources
- Generates shadowJar with merged service files
- Creates sources/javadoc artifacts
- Publishes to `~/.m2/repository`
- **Skips signing locally** (no GPG credentials needed)

### **Logging & Telemetry System**

Fakt includes a professional telemetry system with 3 verbosity levels for troubleshooting and performance analysis.

**Type-Safe Configuration:**

```kotlin
// In build.gradle.kts
import com.rsicarelli.fakt.compiler.api.LogLevel

fakt {
    logLevel.set(LogLevel.INFO)
    logLevel.set(LogLevel.DEBUG)
    logLevel.set(LogLevel.QUIET)
}
```

### **Skills System (Auto-Activation)**

Fakt includes 12 specialized skills that **automatically activate** based on your prompts and context:

#### **Analysis Skills (4)**
- **`kotlin-api-consultant`** - Queries Kotlin compiler source for API validation, Metro alignment
- **`generic-scoping-analyzer`** - Analyzes generic type parameter scoping (class vs method level)
- **`compilation-error-analyzer`** - Systematic compilation error diagnosis and resolution
- **`interface-analyzer`** - Deep structural analysis of @Fake annotated interfaces

#### **Core Workflows (3)**
- **`bdd-test-runner`** - Executes BDD-compliant GIVEN-WHEN-THEN tests with compliance validation
- **`kotlin-ir-debugger`** - Step-by-step IR generation debugging and validation
- **`behavior-analyzer-tester`** - Deep behavior analysis and comprehensive test generation

#### **Validation (3)**
- **`compilation-validator`** - Production-grade compilation validation ensuring zero errors
- **`compiler-architecture-validator`** - Validates compiler plugin architecture and patterns
- **`implementation-tracker`** - Monitors implementation progress and phase completion

#### **Development & Knowledge (2)**
- **`skill-creator`** - Meta-skill for creating new Claude Code skills
- **`fakt-docs-navigator`** - Intelligent navigation through 80+ documentation files

**How Skills Work:**
- **Auto-Activation**: Skills automatically suggest themselves based on keywords and intent patterns
- **Priority Levels**: Critical (IR debugging, compilation) → High (API consultation, testing) → Medium → Low
- **Manual Invocation**: Use the Skill tool with skill name (e.g., "kotlin-api-consultant")
- **Configuration**: `.claude/skills/skill-rules.json` defines triggers and priorities

### **Summary: Critical Testing Practices**

✅ **Always test with published plugin** (`publishToMavenLocal`)
✅ **Use `--info` flag** to verify actual compiler options
✅ **Test both single-platform and KMP scenarios**
✅ **Verify generated code location** matches source set expectations
✅ **Check compilation output** - generated code must compile without errors

## ✅ Testing Guidelines

> **THE ABSOLUTE STANDARD**: Every test MUST follow GIVEN-WHEN-THEN pattern
> **Full Specification**: `.claude/docs/validation/testing-guidelines.md`

### **Required Framework**

- ✅ **Vanilla JUnit5** + Kotlin Test (NO custom matchers)
- ✅ **@TestInstance(TestInstance.Lifecycle.PER_CLASS)** (always required)
- ✅ **GIVEN-WHEN-THEN naming** (uppercase, BDD style)
- ✅ **runTest** for coroutines code
- ✅ **Isolated instances** per test (no shared state)
- ✅ **Fakes instead of mocks** with builder patterns

### **Prohibited Practices**

❌ "should" naming pattern
❌ Custom BDD frameworks
❌ Custom matchers (assertThat, etc.)
❌ Mocks (use fakes)
❌ @BeforeEach/@AfterEach (use isolated instances)

### **✅ ALWAYS DO:**

1. **🏆 Follow compiler plugin best practices**
   - Use industry-standard two-phase FIR → IR compilation patterns
   - Reference: `.claude/docs/` for architectural guidance

2. **🎯 Validate with Kotlin compiler source**
   - Compiler APIs change between versions
   - Use `kotlin-api-consultant` skill to verify
   - Reference: `kotlin/compiler/` (local source copy)

3. **⚡ TDD with vanilla JUnit5**
   - BDD naming: `GIVEN x WHEN y THEN z`
   - Isolated instances per test
   - Compilation validation: generated code MUST compile

4. **🧪 Test with published plugin**
   - Always `./gradlew publishToMavenLocal` before testing
   - Test both project dependencies AND published plugin
   - Use `--info` flag to debug compiler options

5. **📋 MAP quality standards**
   - Minimum Awesome Product always
   - Type-safe code generation
   - Professional error messages
   - Zero compilation errors

---

### **❌ NEVER DO:**

1. **🚨 Ignore Metro patterns**
   - Always check Metro solutions first
   - Two-phase FIR → IR is mandatory
   - Context patterns must be followed

2. **🚨 Skip Kotlin API validation**
   - APIs marked as `@UnsafeApi` can change
   - Always verify with `kotlin-api-consultant` skill
   - Test against multiple Kotlin versions when possible

3. **🚨 Marketing over reality**
   - Real technical status always
   - Document known issues openly
   - Progress metrics must be honest

4. **🚨 Skip compilation testing**
   - Generated code must compile without errors
   - Test both single-module and KMP scenarios
   - Verify output in correct source set (test vs main)

5. **🚨 Custom test frameworks**
   - Vanilla JUnit5 only
   - NO custom matchers or BDD libraries
   - Follow GIVEN-WHEN-THEN standard absolutely

---

## 📚 Critical References

### **Metro Source Code (Local)**
- **Compiler Plugin**: `metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/`
- **IR Generation**: `metro/compiler/src/.../ir/MetroIrGenerationExtension.kt`
- **Testing Structure**: `metro/compiler-tests/`
- **Samples**: `metro/samples/`

### **Kotlin Compiler APIs (Local)**
- **IR APIs**: `kotlin/compiler/ir/backend.common/src/.../extensions/`
- **FIR APIs**: `kotlin/compiler/fir/`
- **Plugin API**: `kotlin/compiler/plugin-api/src/`

### **Fakt Documentation**
- **Testing Guidelines**: `.claude/docs/validation/testing-guidelines.md` ⭐
- **Metro Alignment**: `.claude/docs/development/metro-alignment.md`
- **Current Status**: `.claude/docs/implementation/roadmap.md`
- **Architecture**: `.claude/docs/architecture/ARCHITECTURE.md`
- **Decision Tree**: `.claude/docs/development/decision-tree.md`

### **Quick Reference**
- **Makefile Commands**: `make help`
- **Gradle Tasks**: `cd fakt && ./gradlew tasks`
- **Debug Compilation**: `make debug` or `--info` flag

## 🎯 Do's and Don'ts

### **✅ ALWAYS DO**

#### **Development**
- ✅ Use `make` commands from project root (avoid `cd fakt/` constantly)
- ✅ Test with `publishToMavenLocal` before claiming success
- ✅ Verify generated code compiles without errors
- ✅ Check both single-platform and KMP scenarios
- ✅ Use `--info` flag to debug compiler plugin behavior
- ✅ Follow Metro patterns as architectural inspiration
- ✅ Write GIVEN-WHEN-THEN tests for all new features
- ✅ Format code with `make format` before commits

#### **Architecture**
- ✅ Consult Metro patterns before major decisions
- ✅ Validate Kotlin API usage with `kotlin-api-consultant` skill
- ✅ Keep FIR and IR phases separate
- ✅ Use modular design (analysis → generation → output)
- ✅ Generate code in test source sets only

#### **Testing**
- ✅ BDD naming: `GIVEN x WHEN y THEN z`
- ✅ Isolated instances per test
- ✅ Vanilla JUnit5 + kotlin-test assertions only
- ✅ Test compilation of generated code
- ✅ Use fakes instead of mocks in tests

---

### **❌ NEVER DO**

#### **Development**
- ❌ Skip compilation testing
- ❌ Use deprecated Kotlin APIs
- ❌ Ignore warnings in generated code
- ❌ Assume project dependencies work like published plugin
- ❌ Generate code in main/production source sets
- ❌ Use `buildDir` (deprecated in Gradle 8+)

#### **Architecture**
- ❌ Mix FIR and IR phase logic
- ❌ Skip Metro pattern consultation
- ❌ Use `Any` type for generics without strategy
- ❌ Ignore cross-module import resolution
- ❌ Hardcode output directories

#### **Testing**
- ❌ Use "should" naming pattern
- ❌ Custom BDD frameworks or matchers
- ❌ Shared state between tests
- ❌ @BeforeEach/@AfterEach hooks
- ❌ Mocks instead of fakes

---

### **🎯 Specific Guidelines**

#### **Generic Type Handling**
- ✅ Document current limitations openly
- ✅ Use identity functions for method-level generics
- ✅ Replace class-level generics with `Any`
- ❌ Claim generic support without thorough testing
- ❌ Generate code that doesn't compile

#### **Error Messages**
- ✅ Clear, actionable error messages
- ✅ Include interface name and location
- ✅ Suggest fixes when possible
- ❌ Cryptic compiler errors
- ❌ Silent failures

#### **Performance**
- ✅ Benchmark compilation time impact
- ✅ Optimize generated code size
- ✅ Support incremental compilation
- ❌ Generate unnecessary code
- ❌ Ignore build performance

## 📄 Code Conventions

### **Naming Conventions**

```kotlin
// Generated class naming
@Fake interface UserService
// → FakeUserServiceImpl (implementation class)
// → fakeUserService {} (factory function)
// → FakeUserServiceConfig (DSL config class)

// Package structure
com.example.services.UserService
// → com.example.services.FakeUserServiceImpl (same package)

// Behavior properties naming
interface UserService {
    fun getUser(): User
}
// → private var getUserBehavior: () -> User = { ... }
// → fun configureGetUser(behavior: () -> User) { getUserBehavior = behavior }
```

### **Code Style**

```kotlin
// File headers (managed by Spotless)
// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// License: Apache 2.0
// Formatting: ktfmt Google style
// Max line length: 100 characters
// Import order: Standard Kotlin → Third-party → Project
```

## 🔄 Development Workflow

### **For New Features**

```bash
# 1. Write failing test first (TDD)
# In fakt/compiler/src/test/kotlin/
@Test
fun `GIVEN interface with feature X WHEN generating fake THEN should handle correctly`() = runTest {
    // Test implementation
}

# 2. Implement feature in appropriate module
# - InterfaceAnalyzer for analysis phase
# - IrCodeGenerator for generation phase
# - ConfigurationDslGenerator for DSL creation

# 3. Rebuild and test
make shadowJar
make test-sample

# 4. Verify generated code
cat fakt/samples/kmp-single-module/build/generated/fakt/test/kotlin/FakeXxxImpl.kt

# 5. Format and validate
make format
make test
```

## 🎯 Context for AI Development

### **Project Philosophy**

**MAP (Minimum Awesome Product) vs MVP**
- We don't build "just working" MVPs
- Every feature must be production-quality and delightful
- Kotlin developers expect professional tools (MockK/Mockito quality)
- Type safety and developer experience are non-negotiable

**Metro-Inspired Architecture**
- Follow proven patterns from production DI framework
- Two-phase FIR → IR compilation is mandatory
- Context-driven generation with proper error handling
- Professional code quality matches Metro standards

**TDD Compiler Plugin Development**
- Test-first development for compiler features
- GIVEN-WHEN-THEN pattern is THE ABSOLUTE STANDARD
- Compilation validation is critical (generated code must work)
- Vanilla JUnit5 only (no custom frameworks)

---

### **Success Metrics**

**Technical Quality:**
- ✅ Zero compilation errors in generated code
- ✅ Type-safe DSL without `Any` casting
- ✅ Professional code formatting and structure
- ✅ Clear, actionable error messages

**Developer Experience:**
- ✅ Intuitive API (`@Fake` annotation + `fakeXxx {}` factory)
- ✅ Clean generated code (readable, idiomatic Kotlin)
- ✅ Fast compilation (minimal overhead)
- ✅ Works with KMP and single-platform projects

**Project Health:**
- ✅ Comprehensive GIVEN-WHEN-THEN test coverage
- ✅ Metro pattern alignment verified
- ✅ Documentation up-to-date with code
- ✅ Known issues documented openly

---

### **Key Files to Understand**

**Entry Point:**
```kotlin
fakt/compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
└── FaktCompilerPluginRegistrar.kt    # Service Loader entry, FIR + IR registration
```

**Core Generation:**
```kotlin
fakt/compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
├── UnifiedFaktIrGenerationExtension.kt  # Main IR generation logic
├── analysis/InterfaceAnalyzer.kt        # Interface metadata extraction
└── generation/
    ├── ImplementationGenerator.kt       # Fake class generation
    ├── FactoryGenerator.kt              # Factory function generation
    └── ConfigurationDslGenerator.kt     # DSL generation
```

**Testing:**
```kotlin
.claude/docs/validation/testing-guidelines.md  # THE ABSOLUTE STANDARD
fakt/samples/kmp-single-module/              # Working KMP example project
```

---

### **Critical Documentation**

1. **Testing Guidelines** (⭐ MUST READ)
   - `.claude/docs/validation/testing-guidelines.md`
   - GIVEN-WHEN-THEN pattern is mandatory
   - Vanilla JUnit5 + kotlin-test only

2. **Metro Alignment**
   - `.claude/docs/development/metro-alignment.md`
   - Architectural inspiration and patterns
   - When to consult Metro source

3. **Implementation Roadmap**
   - `.claude/docs/implementation/roadmap.md`
   - Current progress and phase tracking
   - Known issues and limitations

4. **Makefile Commands**
   - `make help` - Show all available commands
   - Root-level commands avoid `cd fakt/` constantly
