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

## 🐛 Bugs Resolvidos e Lições Aprendidas

### **Bug #1: Property<Boolean>.toString() - Gradle Property Evaluation**

**Problema:**
```kotlin
// ❌ Wrong: Passes Property object instead of value
options.add(PluginOption(ENABLED_KEY.optionName, extension.enabled.toString()))
// Output: "ENABLED:Property(value=true)" instead of "ENABLED:true"
```

**Fix:**
```kotlin
// ✅ Correct: Use .get() to evaluate Gradle Property
options.add(PluginOption(ENABLED_KEY.optionName, extension.enabled.get().toString()))
```

**Lição Aprendida:** Gradle `Property<T>` objects must be explicitly evaluated with `.get()` before passing to compiler options. Always test with `--info` flag to see actual values passed to compiler.

---

### **Bug #2: Output Directory Mapping - Main vs Test Source Sets**

**Problema:**
```kotlin
// ❌ Wrong: Generated code for main compilation went to main sourceSet
// This caused fakes to be generated in production code instead of test code
compilation.output.classesDirs.from(generatedSourcesDir)
```

**Fix:**
```kotlin
// ✅ Correct: Map main compilations to their corresponding test directories
val outputCompilationName = when (compilation.name) {
    "main" -> "test"
    "jvmMain" -> "jvmTest"
    "commonMain" -> "commonTest"
    else -> compilation.name  // Already test compilation
}
```

**Lição Aprendida:** Fakes are generated **FROM** main interfaces **FOR** test usage. The plugin receives main compilation events but must write to test output directories. KMP projects require platform-specific mapping (jvmMain → jvmTest, etc.).

---

### **Bug #3: KMP commonTest Detection - Platform-Specific vs Shared Tests**

**Problema:**
```kotlin
// ❌ Wrong: KMP projects with commonTest couldn't see fakes in platform-specific directories
// Generated in: build/generated/fakt/jvm/test/kotlin/
// Expected in: common/test/kotlin/ (for shared test code)
```

**Fix:**
```kotlin
// ✅ Correct: Detect commonTest and generate in shared location
val isCommonTest = project.kotlinExtension
    .sourceSets
    .any { it.name == "commonTest" }

val outputPath = if (isCommonTest) {
    "common/test/kotlin"
} else {
    // Platform-specific path
}
```

**Lição Aprendida:** KMP projects have two test scenarios:
1. **Platform-specific tests** (`jvmTest`, `iosTest`) - Generate in platform directories
2. **Shared tests** (`commonTest`) - Generate in `common/test/kotlin` for cross-platform use

Always check for `commonTest` source set existence to determine KMP shared test scenario.

---

### **Summary: Critical Testing Practices**

✅ **Always test with published plugin** (`publishToMavenLocal`)
✅ **Use `--info` flag** to verify actual compiler options
✅ **Test both single-platform and KMP scenarios**
✅ **Verify generated code location** matches source set expectations
✅ **Check compilation output** - generated code must compile without errors

## ✅ Testing Guidelines

> **THE ABSOLUTE STANDARD**: Every test MUST follow GIVEN-WHEN-THEN pattern
> **Full Specification**: `.claude/docs/validation/testing-guidelines.md`

### **Golden Rule**

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnifiedFaktIrGenerationExtensionTest {

    @Test
    fun `GIVEN interface with suspend functions WHEN generating fake THEN should preserve suspend signatures`() = runTest {
        // Given - create isolated instances
        val asyncInterface = createTestInterface("AsyncService") {
            method("getUser") { suspend(); returns("User") }
        }
        val generator = UnifiedFaktIrGenerationExtension()

        // When
        val result = generator.generateFakeImplementation(asyncInterface)

        // Then
        assertTrue(result.hasMethod("getUser"))
        assertTrue(result.getMethod("getUser").isSuspend)
        assertTrue(result.compiles())
    }
}
```

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

## 🚀 Generic Type Support Implementation ✅ COMPLETE!

> **Status**: Phase 1-2 Complete ✅ | Phase 3 Performance Validated ✅
> **Strategy**: Full IR Substitution with IrTypeSubstitutor
> **Achievement**: Production-ready generic fake generation with type safety
> **Documentation**: `.claude/docs/implementation/generics/`
> **Last Updated**: October 4, 2025

### 🎯 Achievement

**Fakt now supports full type-safe generic interfaces!** ✅

```kotlin
// ✅ NOW WORKING - Generic interfaces fully supported!
@Fake interface Repository<T> {
    fun save(item: T): T
    fun findAll(): List<T>
}

// Generated code (production-ready):
class FakeRepositoryImpl<T> : Repository<T> {
    private var saveBehavior: (T) -> T = { it }
    private var findAllBehavior: () -> List<T> = { emptyList() }

    override fun save(item: T): T = saveBehavior(item)
    override fun findAll(): List<T> = findAllBehavior()
}

inline fun <reified T> fakeRepository(
    configure: FakeRepositoryConfig<T>.() -> Unit = {}
): Repository<T> = FakeRepositoryImpl<T>().apply {
    FakeRepositoryConfig<T>(this).configure()
}

// Usage - Fully type-safe without casting!
val userRepo = fakeRepository<User> {
    save { user -> user.copy(id = "saved-${user.id}") }
    findAll { listOf(User("1", "Alice"), User("2", "Bob")) }
}

val user: User = userRepo.save(User("123", "Test"))  // ✅ TYPE SAFE!
assertEquals("saved-123", user.id)  // ✅ No casting needed!
```

### 📚 Documentation Index

**Start Here**:
- **[QUICK-START.md](./.claude/docs/implementation/generics/QUICK-START.md)** ⭐ - Passo a passo para começar
- **[ROADMAP.md](./.claude/docs/implementation/generics/ROADMAP.md)** - Visão geral e estratégia completa
- **[CHEAT-SHEET.md](./.claude/docs/implementation/generics/CHEAT-SHEET.md)** - Quick reference durante desenvolvimento

**Phase Guides**:
- **[Phase 1: Core Infrastructure](./.claude/docs/implementation/generics/phase1-core-infrastructure.md)** - Week 1
- **[Phase 2: Code Generation](./.claude/docs/implementation/generics/phase2-code-generation.md)** - Week 2
- **[Phase 3: Testing & Integration](./.claude/docs/implementation/generics/phase3-testing-integration.md)** - Week 3

**Technical References**:
- **[Test Matrix](./.claude/docs/implementation/generics/test-matrix.md)** - 50+ test scenarios (P0-P3)
- **[Technical Reference](./.claude/docs/implementation/generics/technical-reference.md)** - Kotlin IR APIs deep dive
- **[CHANGELOG.md](./.claude/docs/implementation/generics/CHANGELOG.md)** - Track daily progress

### 📅 Implementation Phases

#### **Phase 1: Core Infrastructure** (Week 1 - Days 1-5)
**Goal**: Remove generic filter, create GenericIrSubstitutor, enhance TypeResolver

**Key Deliverables**:
- ✅ GenericIrSubstitutor.kt created with IrTypeSubstitutor integration
- ✅ TypeResolver enhanced to preserve type parameters
- ✅ Generic filter removed (line 189 in UnifiedFaktIrGenerationExtension)
- ✅ Integration test: `Repository<T>` compiles without errors

**Files Modified**:
- `compiler/src/main/kotlin/.../ir/GenericIrSubstitutor.kt` (NEW)
- `compiler/src/main/kotlin/.../types/TypeResolver.kt`
- `compiler/src/main/kotlin/.../ir/UnifiedFaktIrGenerationExtension.kt`
- `compiler/src/main/kotlin/.../ir/analysis/InterfaceAnalyzer.kt`

---

#### **Phase 2: Code Generation** (Week 2 - Days 6-10)
**Goal**: Update all generators to produce generic code

**Key Deliverables**:
- ✅ ImplementationGenerator generates `class Fake<T> : Interface<T>`
- ✅ FactoryGenerator generates `inline fun <reified T> fakeFoo()`
- ✅ ConfigurationDslGenerator generates `class FakeConfig<T>`
- ✅ Integration test: Generated code compiles and is type-safe at use-site

**Files Modified**:
- `compiler/src/main/kotlin/.../codegen/ImplementationGenerator.kt`
- `compiler/src/main/kotlin/.../codegen/FactoryGenerator.kt`
- `compiler/src/main/kotlin/.../codegen/ConfigurationDslGenerator.kt`
- `compiler/src/main/kotlin/.../codegen/CodeGenerator.kt`

---

#### **Phase 3: Testing & Integration** (Week 3 - Days 11-15)
**Goal**: Comprehensive test coverage, edge cases, production validation

**Key Deliverables**:
- ✅ P0 tests passing (100% - basic generics)
- ✅ P1 tests passing (95% - method-level & mixed)
- ✅ P2 tests passing (90% - constraints & variance)
- ✅ Edge cases handled (star projections, recursive generics)
- ✅ Performance benchmarks (<10% overhead)
- ✅ Documentation updated
- ✅ Production validation with publishToMavenLocal

**Files Created**:
- `compiler/src/test/kotlin/.../GenericFakeGenerationTest.kt`
- `compiler/src/main/kotlin/.../ir/GenericEdgeCaseHandler.kt`
- Updated samples with generic examples

---

### 🎯 Strategy: Full IR Substitution

**Why Full IR Instead of Type Erasure?**

1. **Type Safety**: Preserves complete type information at compile time
2. **Metro Alignment**: Uses proven patterns from production DI framework
3. **Developer Experience**: `fakeRepository<User> {}` is intuitive and type-safe
4. **Future-Proof**: Supports all generic scenarios (class, method, mixed, constraints)
5. **MAP Quality**: Minimum Awesome Product demands excellence

**Core APIs Used**:
- `IrTypeSubstitutor` - Class-level generic substitution
- `IrTypeParameterRemapper` - Method-level generic remapping
- `GenericPatternAnalyzer` - Already exists! Detects patterns
- `kotlin-compile-testing` - Multi-stage validation (generation → structure → use-site type safety)

### 📊 Progress Tracking

| Phase | Status | Completion | Tests Passing |
|-------|--------|------------|---------------|
| Planning | ✅ Done | 100% | N/A |
| Phase 1 | ✅ Done | 100% | 4/4 unit tests ✅ |
| Phase 2 | ✅ Done | 100% | 36/36 integration tests ✅ |
| Phase 3 (Performance) | ✅ Done | 100% | Validated ✅ |

**Test Matrix Progress** (36 tests passing):
- ✅ P0 (Basic): 22/22 passing (100%) - Class-level, multiple params, nested
- ✅ P1 (Constraints): 6/6 passing (100%) - Type constraints (T : Comparable)
- ✅ P2 (Method/Mixed): 8/8 passing (100%) - Method-level & mixed generics
- ⏳ P3 (Edge Cases): Deferred - Variance, star projections, recursive (optional)

**Performance Metrics** (October 4, 2025):
- Compilation time: 0.445s for 9 generic interfaces
- Per-interface overhead: ~49ms
- All tests executing in <20ms total
- Zero errors, zero warnings

**Track Progress**: See [CHANGELOG.md](./.claude/docs/implementation/generics/CHANGELOG.md) for daily updates

### 🚨 Critical Success Factors

1. **TDD Absolutely**: GIVEN-WHEN-THEN tests written BEFORE implementation
2. **Metro Patterns**: Check alignment before major architectural decisions
3. **Multi-Stage Validation**: Test generation → structure → **use-site type safety** (most critical!)
4. **Incremental Progress**: Complete one phase before starting next
5. **Performance Monitoring**: Track compilation time overhead (<10% target)

### 🔗 Quick Commands

```bash
# Read planning documentation
cat .claude/docs/implementation/generics/QUICK-START.md
cat .claude/docs/implementation/generics/ROADMAP.md

# Validate Kotlin APIs
/consult-kotlin-api IrTypeSubstitutor

# Check Metro alignment
/validate-metro-alignment

# Start Phase 1
# Follow: .claude/docs/implementation/generics/phase1-core-infrastructure.md
```

---

## 📊 Status Atual do Projeto

### **✅ Funcionando (Production-Ready)**

#### **Core Infrastructure**
- ✅ Plugin discovery via Service Loader
- ✅ Two-phase FIR → IR compilation
- ✅ Gradle plugin integration
- ✅ Maven publishing to mavenLocal
- ✅ Shadow JAR packaging

#### **Interface Support**
- ✅ Basic interfaces (methods + properties)
- ✅ Suspend functions (`suspend fun login()`)
- ✅ Properties (val/var with getters)
- ✅ Method-only interfaces
- ✅ Property-only interfaces
- ✅ Multiple interfaces in single module
- ✅ **Generic interfaces** (`interface Repo<T>`) - **NEW!** 🎉
- ✅ **Multiple type parameters** (`KeyValueStore<K, V>`)
- ✅ **Nested generics** (`Map<K, List<V>>`)
- ✅ **Type constraints** (`<T : Comparable<T>>`)
- ✅ **Method-level generics** (`fun <T> process()`)
- ✅ **Mixed generics** (class + method type parameters)

#### **Code Generation**
- ✅ Implementation classes (`FakeXxxImpl`)
- ✅ Factory functions (`fakeXxx {}`)
- ✅ Configuration DSL (`FakeXxxConfig`)
- ✅ Type-safe behavior configuration
- ✅ Thread-safe fake instances

#### **Multiplatform Support**
- ✅ KMP project detection
- ✅ commonTest source set support
- ✅ Platform-specific test directories (jvmTest, iosTest)
- ✅ Shared test code generation

---

### **❌ Não Funcionando (Conhecido)**

#### **Advanced Features**
- ❌ Inline functions
- ❌ Operator overloading
- ❌ Delegation (by keyword)
- ❌ Call tracking (`@Fake(trackCalls = true)`)
- ❌ Builder patterns (`@Fake(builder = true)`)

#### **Edge Cases**
- ❌ Nested interfaces
- ❌ Sealed interfaces
- ❌ Functional interfaces (SAM)
- ❌ Interfaces with companion objects

---

### **🔧 Em Progresso**

#### **Advanced Generic Features** (Optional Future Enhancements)
- ⏳ Variance annotations (`out T`, `in T`) - Deferred for future release
- ⏳ Star projections (`List<*>`) - Deferred for future release
- ⏳ Recursive generics (`Node<T : Node<T>>`) - Deferred for future release

> **Note**: Core generic support is ✅ complete. These advanced edge cases are optional enhancements.

#### **Type System Improvements**
- 🔧 Cross-module type imports (in progress)
- 🔧 Function type resolution (`(T) -> R` syntax)

#### **Error Handling**
- 🔧 Better diagnostic messages
- 🔧 Compilation error reporting
- 🔧 Invalid interface detection

#### **Performance**
- ✅ ~~Incremental compilation support~~ (Working)
- ✅ ~~Build cache optimization~~ (Working)
- ✅ ~~Compilation time benchmarks~~ (Complete - 0.445s for 9 interfaces)

## 🚨 Regras Críticas

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

### **For Bug Fixes**

```bash
# 1. Reproduce bug with test
@Test
fun `GIVEN interface causing bug WHEN generating THEN should not fail`() = runTest {
    // Reproduce bug scenario
}

# 2. Debug with --info flag
make debug

# 3. Fix issue in source
# Edit compiler/src/main/kotlin/...

# 4. Verify fix
make quick-test

# 5. Update documentation if needed
# Add to "Bugs Resolvidos" section if critical
```

### **For Metro Pattern Updates**

```bash
# 1. Review Metro source code
cd metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/

# 2. Identify applicable pattern
# Example: Error handling, type resolution, context usage

# 3. Validate with command
/validate-metro-alignment

# 4. Apply pattern to Fakt
# Update compiler/src/main/kotlin/... with Metro-inspired approach

# 5. Document decision
# Update .claude/docs/development/metro-alignment.md
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

### **Current State (October 2025)**

**What Works (Production-Ready):**
- ✅ Basic interface fake generation (methods + properties)
- ✅ Suspend functions fully supported
- ✅ Type-safe factory functions and DSL
- ✅ KMP project support (commonTest + platform-specific)
- ✅ Published plugin working via mavenLocal
- ✅ End-to-end compilation in single-module sample
- ✅ **Generic type support** - Full type-safe generic interfaces! 🎉
  - Class-level generics (`Repository<T>`)
  - Multiple type parameters (`KeyValueStore<K, V>`)
  - Nested generics (`Map<K, List<V>>`)
  - Type constraints (`<T : Comparable<T>>`)
  - Method-level generics (`fun <T> process()`)
  - Mixed generics (class + method parameters)

**What Doesn't Work (Known Limitations):**
- ❌ Inline functions
- ❌ Advanced features (call tracking, builder patterns)
- ❌ Advanced generic edge cases (variance, star projections, recursive generics)

**What's In Progress (Future Enhancements):**
- 🔧 Improved error diagnostics
- 🔧 Cross-module type imports
- 🔧 Advanced generic edge cases (optional)

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

---

This context should provide everything needed to understand Fakt's architecture, development workflow, and quality standards. Remember: We build MAPs, not MVPs. Every feature should be production-ready and delightful! 🚀
