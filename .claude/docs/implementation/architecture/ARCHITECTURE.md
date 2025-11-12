# Fakt Compiler Plugin Architecture

**Status**: Production Implementation
**Last Updated**: January 2025
**Philosophy**: MAP (Minimum Awesome Product) - Type-safe, maintainable, transparent

---

## Overview

Fakt is a Kotlin compiler plugin that generates type-safe test fakes from `@Fake` annotated interfaces and classes. It follows a **two-phase FIR → IR compilation approach** with **string-based code generation using type-safe DSL builders**.

### Design Principles

1. **Developer Transparency** - Generated `.kt` files are readable and debuggable
2. **Type Safety** - Leverages Kotlin's type system at compile time
3. **Modular Design** - Clear separation of concerns with dedicated modules
4. **Metro-Inspired** - Follows proven patterns from production DI frameworks

---

## Two-Phase Compilation Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│  PHASE 1: FIR (Frontend Intermediate Representation)           │
│  ══════════════════════════════════════════════════════════════ │
│  • FaktFirExtensionRegistrar - Plugin registration            │
│  • FakeInterfaceChecker - Validates @Fake usage               │
│  • FakeClassChecker - Validates @Fake on classes              │
│  • FirMetadataStorage - Stores validated metadata             │
│                                                                  │
│  Output: Validated metadata → FirMetadataStorage               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  PHASE 2: IR (Intermediate Representation)                     │
│  ══════════════════════════════════════════════════════════════ │
│  • UnifiedFaktIrGenerationExtension - Orchestration            │
│  • FirToIrTransformer - Metadata conversion                    │
│  • CodeGenerator - String-based generation                     │
│                                                                  │
│  Output: FakeXxxImpl.kt files in build/generated/fakt/         │
└─────────────────────────────────────────────────────────────────┘
```

### Phase 1: FIR Phase

**Location**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/`

**Responsibilities**:
- Detect `@Fake` annotations on interfaces and classes
- Validate interface/class structure for fake generation
- Store validated metadata in `FirMetadataStorage`
- Report compilation errors with precise source locations

**Key Components**:
```kotlin
FaktFirExtensionRegistrar.kt       # FIR plugin registration
FakeInterfaceChecker.kt            # Interface validation
FakeClassChecker.kt                # Class validation
FirMetadataStorage.kt              # Thread-safe metadata storage
```

### Phase 2: IR Phase

**Location**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/generation/`

**Responsibilities**:
- Read validated metadata from FIR phase
- Transform FIR metadata to IR types
- Generate Kotlin source code using type-safe DSL
- Write `.kt` files to `build/generated/fakt/{sourceSet}/kotlin/`

**Key Components**:
```kotlin
UnifiedFaktIrGenerationExtension.kt  # Main IR generation entry point
FirToIrTransformer.kt                # FIR → IR metadata transformation
CodeGenerator.kt                     # Orchestrates code generation
ImplementationGenerator.kt           # Generates fake implementations
FactoryGenerator.kt                  # Generates factory functions
ConfigurationDslGenerator.kt         # Generates configuration DSL
```

---

## Code Generation Architecture

### Strategy: Type-Safe DSL with String Rendering

Fakt generates Kotlin source code using a **type-safe DSL builder pattern**, NOT pure IR node manipulation.

**Why String-Based with DSL?**
- ✅ **IDE Visibility** - Generated `.kt` files appear in IDE
- ✅ **Debuggability** - Set breakpoints, inspect code
- ✅ **Stability** - Kotlin syntax is stable across versions
- ✅ **Type Safety** - DSL provides compile-time validation
- ✅ **Simplicity** - Easier to understand and maintain

### Code Generation Modules

**Location**: `compiler/src/main/kotlin/com/rsicarelli/fakt/codegen/`

```
codegen/
├── builder/                    # Type-safe DSL builders
│   ├── CodeFileBuilder.kt      # File structure: package, imports, declarations
│   ├── ClassBuilder.kt         # Class declarations with properties and methods
│   ├── FunctionBuilder.kt      # Function declarations
│   └── PropertyBuilder.kt      # Property declarations
│
├── model/                      # Immutable code representation
│   ├── CodeFile.kt             # Complete file model
│   └── CodeDeclaration.kt      # Class/function/property models
│
├── renderer/                   # String rendering
│   ├── CodeBuilder.kt          # Accumulates generated strings
│   └── Rendering.kt            # renderTo() extension functions
│
├── extensions/                 # High-level generation helpers
│   ├── FakeGenerator.kt        # Complete fake generation
│   ├── MethodExtensions.kt     # Method implementation generation
│   └── PropertyExtensions.kt   # Property implementation generation
│
└── strategy/                   # Default value strategies
    ├── DefaultValueResolver.kt # Resolves default values for types
    ├── PrimitiveDefaultStrategy.kt
    ├── CollectionDefaultStrategy.kt
    └── StdlibDefaultStrategy.kt
```

### Type-Safe DSL Example

```kotlin
// compiler/src/main/kotlin/com/rsicarelli/fakt/codegen/builder/CodeFileBuilder.kt
val file = codeFile("com.example") {
    header = "Generated by Fakt"
    import("kotlinx.coroutines.flow.StateFlow")

    klass("FakeUserServiceImpl") {
        implements("UserService")

        property("getUserBehavior", "() -> User") {
            private()
            mutable()
            initializer = "{ User(\"default\") }"
        }

        function("getUser") {
            returnType = "User"
            override()
            body = "getUserBehavior()"
        }
    }
}

// Render to string
val builder = CodeBuilder()
file.renderTo(builder)
val sourceCode = builder.build()  // Complete .kt file as string
```

---

## Component Architecture

### Shared Context (Metro Pattern)

**Location**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/core/context/`

```kotlin
// FaktSharedContext.kt - Shared between FIR and IR phases
data class FaktSharedContext(
    val fakeAnnotations: List<String>,
    val options: FaktOptions,
    val metadataStorage: FirMetadataStorage,  // FIR writes, IR reads
)
```

**Benefits**:
- Thread-safe communication between FIR and IR phases
- Follows Metro's proven architectural pattern
- No serialization overhead

### Type System

**Location**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/core/types/`

```kotlin
TypeResolution.kt           # IrType → Kotlin string conversion
TypeRenderer.kt             # Type rendering with generics
GenericTypeHandler.kt       # Generic type parameter handling
DefaultValueProvider.kt     # Smart default value generation
FunctionTypeHandler.kt      # Function type handling
```

**Capabilities**:
- Convert `IrType` to readable Kotlin type strings
- Handle generic type parameters (class-level and method-level)
- Generate appropriate default values based on type
- Resolve nullability and type projections

### Telemetry System

**Location**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/core/telemetry/`

```kotlin
FaktLogger.kt              # Multi-level logging (QUIET, INFO, DEBUG, TRACE)
FaktTelemetry.kt           # Performance metrics collection
CompilationReport.kt       # Generation statistics
MetricsCollector.kt        # Compile-time metrics
PhaseTracker.kt            # Phase timing
```

**Log Levels** (configured via Gradle):
- `QUIET` - Silent (CI/CD)
- `INFO` - Concise summary (default, recommended)
- `DEBUG` - Detailed breakdown (troubleshooting)
- `TRACE` - Everything (deep debugging)

### Import Resolution

**Location**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/core/context/ImportResolver.kt`

**Responsibilities**:
- Detect cross-module type references
- Generate required import statements
- Handle FQN vs simple name resolution
- Avoid redundant imports

---

## Generated Code Structure

For each `@Fake` annotated interface/class, Fakt generates a single `.kt` file:

```kotlin
// Generated: build/generated/fakt/commonTest/kotlin/com/example/FakeUserServiceImpl.kt
package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Implementation class
internal class FakeUserServiceImpl : UserService {
    // Behavior properties
    private var getUserBehavior: () -> User = { User("default") }

    // Call tracking
    private val _getUserCalls = MutableStateFlow(0)
    val getUserCalls: StateFlow<Int> = _getUserCalls

    // Implementation
    override fun getUser(): User {
        _getUserCalls.value++
        return getUserBehavior()
    }

    // Configuration
    internal fun configureGetUser(behavior: () -> User) {
        getUserBehavior = behavior
    }
}

// Factory function
fun fakeUserService(configure: FakeUserServiceConfig.() -> Unit = {}): UserService {
    return FakeUserServiceImpl().apply {
        FakeUserServiceConfig(this).configure()
    }
}

// Configuration DSL
class FakeUserServiceConfig(private val fake: FakeUserServiceImpl) {
    fun getUser(behavior: () -> User) {
        fake.configureGetUser(behavior)
    }
}
```

---

## Modular Compilation Flow

```kotlin
// 1. Plugin Registration (FaktCompilerPluginRegistrar.kt)
FirExtensionRegistrarAdapter.registerExtension(FaktFirExtensionRegistrar)
IrGenerationExtension.registerExtension(UnifiedFaktIrGenerationExtension)

// 2. FIR Phase (FaktFirExtensionRegistrar.kt)
@Fake interface UserService { ... }
  → FakeInterfaceChecker validates
  → FirMetadataStorage stores metadata

// 3. IR Phase (UnifiedFaktIrGenerationExtension.kt)
load FIR metadata from FirMetadataStorage
  → FirToIrTransformer converts to IR types
  → CodeGenerator orchestrates generation
    → ImplementationGenerator (uses DSL)
    → FactoryGenerator (uses DSL)
    → ConfigurationDslGenerator (uses DSL)
  → Write .kt files to disk

// 4. Kotlin Compiler
Parses generated .kt files
  → Compiles to bytecode (normal Kotlin compilation)
```

---

## Key Design Decisions

### Why Not Pure IR Node Generation?

**Metro uses pure IR generation**, but Fakt's requirements differ:

| Aspect | Metro (DI Framework) | Fakt (Test Fakes) |
|--------|----------------------|-------------------|
| **Visibility** | Internal infrastructure | Developers read/debug frequently |
| **Performance** | Runtime DI critical | Compile-time generation (~10-50ms/fake) |
| **Debugging** | IR dumps (complex) | Source code (readable) |
| **Complexity** | Justified for DI | Overkill for fakes |

**When we'd reconsider IR-native:**
- Generating 1000+ fakes per build (performance critical)
- Runtime code generation requirements
- Advanced type transformations beyond current capabilities

### Why Type-Safe DSL Instead of Raw `buildString {}`?

**Benefits**:
- Type-safe construction (compile-time validation)
- Immutable models prevent accidental mutation
- Composable, reusable builders
- Clean separation: build → model → render

**Trade-offs Accepted**:
- Small overhead (DSL construction + rendering)
- Additional abstraction layer
- Negligible for test code generation (~10-50ms/fake)

---

## Optimization Features

### Incremental Compilation Support

**Location**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/core/optimization/`

```kotlin
CompilerOptimizations.kt    # Caching and incremental compilation
SignatureBuilder.kt         # Generates stable signatures for caching
```

**Capabilities**:
- Signature-based caching (interface structure hash)
- Skip generation if signature unchanged
- Supports Gradle incremental compilation

### Source Set Resolution

**Location**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/core/context/SourceSetResolver.kt`

**Capabilities**:
- Detects source set type (test, main, commonTest, etc.)
- Determines correct output directory
- Supports KMP multi-module projects

---

## Known Limitations

### Generic Type Scoping (Phase 2 Work)

**Challenge**: Method-level generic type parameters not accessible at class level

```kotlin
interface DataService {
    suspend fun <T> process(data: T): T  // <T> not in class scope
}

// CURRENT GENERATION (workaround with casting):
class FakeDataServiceImpl : DataService {
    private var processBehavior: suspend (Any?) -> Any? = { it }

    override suspend fun <T> process(data: T): T {
        @Suppress("UNCHECKED_CAST")
        return processBehavior(data) as T
    }
}
```

**Status**: Functional workaround implemented, full type-safe solution in Phase 2

---

## File Structure Reference

```
ktfake/
├── compiler/
│   └── src/main/kotlin/com/rsicarelli/fakt/
│       ├── compiler/
│       │   ├── FaktCompilerPluginRegistrar.kt       # Entry point
│       │   ├── fir/
│       │   │   ├── FaktFirExtensionRegistrar.kt
│       │   │   ├── checkers/
│       │   │   │   ├── FakeInterfaceChecker.kt
│       │   │   │   └── FakeClassChecker.kt
│       │   │   └── metadata/
│       │   │       └── FirMetadataStorage.kt
│       │   ├── ir/
│       │   │   ├── generation/
│       │   │   │   ├── UnifiedFaktIrGenerationExtension.kt
│       │   │   │   ├── CodeGenerator.kt
│       │   │   │   ├── ImplementationGenerator.kt
│       │   │   │   ├── FactoryGenerator.kt
│       │   │   │   └── ConfigurationDslGenerator.kt
│       │   │   └── transform/
│       │   │       └── FirToIrTransformer.kt
│       │   └── core/
│       │       ├── config/
│       │       │   └── FaktOptions.kt
│       │       ├── context/
│       │       │   ├── FaktSharedContext.kt
│       │       │   ├── ImportResolver.kt
│       │       │   └── SourceSetResolver.kt
│       │       ├── types/
│       │       │   ├── TypeResolution.kt
│       │       │   ├── GenericTypeHandler.kt
│       │       │   └── DefaultValueProvider.kt
│       │       ├── telemetry/
│       │       │   ├── FaktLogger.kt
│       │       │   └── FaktTelemetry.kt
│       │       └── optimization/
│       │           └── CompilerOptimizations.kt
│       └── codegen/
│           ├── builder/              # Type-safe DSL builders
│           ├── model/                # Immutable code models
│           ├── renderer/             # String rendering
│           ├── extensions/           # High-level generators
│           └── strategy/             # Default value strategies
│
├── runtime/                          # @Fake annotation (multiplatform)
├── gradle-plugin/                    # Gradle integration
└── samples/                          # Example projects
```

---

## Related Documentation

- **Compiler Optimizations**: `.claude/docs/architecture/compiler-optimizations.md` - Caching and incremental compilation
- **Gradle Plugin**: `.claude/docs/architecture/gradle-plugin.md` - Build system integration
- **Testing Guidelines**: `.claude/docs/validation/testing-guidelines.md` - BDD testing standards
- **Metro Alignment**: `.claude/docs/development/metro-alignment.md` - Architectural inspiration
- **Current Status**: `.claude/docs/implementation/current-status.md` - Implementation progress
- **Decision Tree**: `.claude/docs/development/decision-tree.md` - Architectural decisions

---

## Summary

Fakt implements a **two-phase FIR → IR compilation pipeline** with **type-safe DSL-based code generation**:

✅ **FIR Phase** - Validates `@Fake` annotations, stores metadata
✅ **IR Phase** - Transforms metadata, generates code
✅ **Type-Safe DSL** - Builders create immutable models
✅ **String Rendering** - Models render to `.kt` files
✅ **Transparent** - Generated code is readable and debuggable
✅ **Modular** - Clean separation of concerns
✅ **Performance** - Incremental compilation, caching support

This architecture prioritizes **developer experience, maintainability, and transparency** while maintaining professional code quality and type safety.
