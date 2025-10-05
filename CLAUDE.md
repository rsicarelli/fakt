# 🤖 CLAUDE.md - Fakt Compiler Plugin

> **Metro-inspired Kotlin compiler plugin for type-safe fake generation**
> **Status**: MAP (Minimum Awesome Product) - Core architecture complete, final polish in progress
> **Last Updated**: January 2025

## 🎯 What is Fakt?

**Fakt** (formerly ktfakes-prototype) is a Kotlin compiler plugin that generates type-safe test fakes at compile time using the `@Fake` annotation. Inspired by the [Metro Dependency Injection framework](https://github.com/slackhq/metro), Fakt follows a two-phase FIR → IR compilation approach to analyze interfaces and generate production-quality fake implementations.

**Key Differences from Metro:**
- **Metro**: Dependency injection code generation for production use
- **Fakt**: Test fake generation for testing scenarios only
- **Shared Patterns**: Two-phase FIR/IR compilation, CompilerPluginRegistrar structure, IrGenerationExtension patterns

**Problem Solved:**
Writing test fakes manually is tedious and error-prone. Fakt generates type-safe fakes automatically with a clean DSL for configuring behavior, eliminating boilerplate while maintaining compile-time safety.

## 🏗️ Architecture

### **Two-Phase Compilation (Metro-Inspired)**

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
ktfake/
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
├── runtime/                           # Multiplatform annotations
│   └── @Fake                             # Main annotation
├── gradle-plugin/                     # Gradle integration
└── samples/
    ├── single-module/                 # ✅ Working example
    ├── kmp-comprehensive-test/        # KMP testing
    └── published-modules-test/        # Multi-module testing
```

## ⚡ Essential Commands

### **Development Workflow**

```bash
# 🏗️ Build compiler plugin
make shadowJar                                    # or: cd ktfake && ./gradlew :compiler:shadowJar

# 🧪 Test working example
make test-sample                                  # or: cd ktfake && ./gradlew :samples:single-module:build

# ⚡ Quick rebuild cycle (no cache)
make quick-test                                   # Rebuild plugin + test sample fresh

# 💥 Nuclear option (full clean rebuild)
make full-rebuild                                 # Clean + rebuild everything

# 🔍 Debug compiler plugin output
make debug                                        # Show Fakt-specific logs

# ✨ Format code (required before commits)
make format                                       # or: cd ktfake && ./gradlew spotlessApply

# 🧹 Clean build artifacts
make clean                                        # or: cd ktfake && ./gradlew clean

# 📚 Show all commands
make help
```

### **Slash Commands (Claude Code)**

```bash
# 🔬 Debug IR generation for specific interface
/debug-ir-generation <interface_name>

# 📚 Query Kotlin compiler source code
/consult-kotlin-api <api_class>

# 🏆 Check Metro pattern alignment
/validate-metro-alignment

# 🧪 Run BDD-style tests
/run-bdd-tests <pattern>

# 📊 Check implementation status
/check-implementation-status

# 🔍 Analyze interface structure
/analyze-interface-structure <interface_name>
```

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

### 📚 Documentation Index

**Start Here**:
- **[QUICK-START.md](./.claude/docs/implementation/QUICK-START.md)** ⭐ - Passo a passo para começar
- **[ROADMAP.md](./.claude/docs/implementation/ROADMAP.md)** - Visão geral e estratégia completa

**Phase Guides**:
- **[Phase 1: Core Infrastructure](./.claude/docs/implementation/generics/phase1-core-infrastructure.md)** - Week 1
- **[Phase 2: Code Generation](./.claude/docs/implementation/generics/phase2-code-generation.md)** - Week 2
- **[Phase 3: Testing & Integration](./.claude/docs/implementation/generics/phase3-testing-integration.md)** - Week 3

**Technical References**:
- **[Test Matrix](./.claude/docs/implementation/generics/test-matrix.md)** - 50+ test scenarios (P0-P3)
- **[Technical Reference](./.claude/docs/implementation/generics/technical-reference.md)** - Kotlin IR APIs deep dive
- **[CHANGELOG.md](./.claude/docs/implementation/generics/CHANGELOG.md)** - Track daily progress

### **✅ SEMPRE FAZER:**

1. **🏆 Consultar Metro patterns primeiro**
   - Metro é nossa inspiração arquitetural
   - Use `/validate-metro-alignment` antes de decisões arquiteturais
   - Referência: `.claude/docs/development/metro-alignment.md`

2. **🎯 Validar com Kotlin compiler source**
   - APIs do compilador mudam entre versões
   - Use `/consult-kotlin-api <class>` para verificar
   - Referência: `kotlin/compiler/` (local source copy)

3. **⚡ TDD com vanilla JUnit5**
   - BDD naming: `GIVEN x WHEN y THEN z`
   - Isolated instances per test
   - Compilation validation: generated code MUST compile

4. **🧪 Test with published plugin**
   - Always `./gradlew publishToMavenLocal` before testing
   - Test both project dependencies AND published plugin
   - Use `--info` flag to debug compiler options

5. **📋 MAP quality standards**
   - Minimum Awesome Product sempre
   - Type-safe code generation
   - Professional error messages
   - Zero compilation errors

---

### **❌ JAMAIS FAZER:**

1. **🚨 Ignorar Metro patterns**
   - Sempre check Metro solutions first
   - Two-phase FIR → IR é obrigatório
   - Context patterns devem ser seguidos

2. **🚨 Skip Kotlin API validation**
   - APIs marcadas como `@UnsafeApi` podem mudar
   - Sempre verificar com `/consult-kotlin-api`
   - Test against multiple Kotlin versions when possible

3. **🚨 Marketing over reality**
   - Real technical status sempre
   - Document known issues openly
   - Progress metrics devem ser honestos

4. **🚨 Skip compilation testing**
   - Generated code deve compilar sem erros
   - Test both single-module and KMP scenarios
   - Verify output in correct source set (test vs main)

5. **🚨 Custom test frameworks**
   - Vanilla JUnit5 only
   - NO custom matchers or BDD libraries
   - Follow GIVEN-WHEN-THEN standard absolutely

---

### **🎯 Metro Alignment Rules:**

- **📐 Follow Metro architecture** - FIR → IR two-phase compilation
- **🔧 Use Metro patterns** - CompilerPluginRegistrar, IrGenerationExtension
- **🧪 Metro testing approach** - compiler-tests/ structure (future)
- **📊 Metro quality standards** - Binary compatibility, API validation

## 📚 Referências Críticas

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
- **Current Status**: `.claude/docs/implementation/current-status.md`
- **Architecture**: `.claude/docs/architecture/unified-ir-native.md`
- **Decision Tree**: `.claude/docs/development/decision-tree.md`

### **Quick Reference**
- **Makefile Commands**: `make help`
- **Gradle Tasks**: `cd ktfake && ./gradlew tasks`
- **Debug Compilation**: `make debug` or `--info` flag

## 🎯 Do's and Don'ts

### **✅ SEMPRE FAZER**

#### **Development**
- ✅ Use `make` commands from project root (avoid `cd ktfake/` constantly)
- ✅ Test with `publishToMavenLocal` before claiming success
- ✅ Verify generated code compiles without errors
- ✅ Check both single-platform and KMP scenarios
- ✅ Use `--info` flag to debug compiler plugin behavior
- ✅ Follow Metro patterns as architectural inspiration
- ✅ Write GIVEN-WHEN-THEN tests for all new features
- ✅ Format code with `make format` before commits

#### **Architecture**
- ✅ Consult Metro patterns before major decisions
- ✅ Validate Kotlin API usage with `/consult-kotlin-api`
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

### **❌ JAMAIS FAZER**

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

## 📄 Convenções de Código

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

### **Generated Code Patterns**

```kotlin
// Implementation class pattern
class Fake{Interface}Impl : {Interface} {
    // Behavior properties for each method/property
    private var {method}Behavior: ({params}) -> {return} = { default }

    // Override interface members
    override fun {method}({params}): {return} = {method}Behavior({params})

    // Internal configuration methods
    internal fun configure{Method}(behavior: ({params}) -> {return}) {
        {method}Behavior = behavior
    }
}

// Factory function pattern
fun fake{Interface}(configure: Fake{Interface}Config.() -> Unit = {}): {Interface} {
    return Fake{Interface}Impl().apply {
        Fake{Interface}Config(this).configure()
    }
}

// Configuration DSL pattern
class Fake{Interface}Config(private val fake: Fake{Interface}Impl) {
    fun {method}(behavior: ({params}) -> {return}) {
        fake.configure{Method}(behavior)
    }
}
```

## 🔄 Development Workflow

### **For New Features**

```bash
# 1. Write failing test first (TDD)
# In ktfake/compiler/src/test/kotlin/
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
cat ktfake/samples/single-module/build/generated/fakt/test/kotlin/FakeXxxImpl.kt

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
ktfake/compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
└── FaktCompilerPluginRegistrar.kt    # Service Loader entry, FIR + IR registration
```

**Core Generation:**
```kotlin
ktfake/compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
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
ktfake/samples/single-module/                  # Working example project
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

3. **Current Status**
   - `.claude/docs/implementation/current-status.md`
   - Real progress tracking (no marketing)
   - Known issues and limitations

4. **Makefile Commands**
   - `make help` - Show all available commands
   - Root-level commands avoid `cd ktfake/` constantly
