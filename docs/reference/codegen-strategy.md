# Code Generation Strategy

Understanding Fakt's architectural approach to generating test fakes.

---

## Overview

Fakt generates test fakes at compile-time using a **custom type-safe DSL** that produces readable Kotlin source files (`.kt`). This page explains why Fakt chose this approach over alternatives like IR-native generation or third-party libraries like KotlinPoet.

**TL;DR**: Fakt prioritizes **developer transparency** and **debuggability** over marginal performance gains. Generated fakes are readable `.kt` files you can inspect, debug, and understand—not compiler IR nodes or bytecode.

---

## The Approach: Type-Safe DSL

Fakt's code generation follows a **three-phase architecture**:

```
Phase 1: Build (Type-Safe DSL)
  ↓ codeFile { klass { function { } } }
Phase 2: Render (Model → String)
  ↓ Immutable data structures → Kotlin source code
Phase 3: Write (File I/O)
  ↓ Output: build/generated/fakt/.../FakeXxxImpl.kt
```

### How It Works

Instead of manipulating strings directly or generating IR nodes, Fakt uses a **builder DSL** to construct code:

```kotlin
// Simplified example of Fakt's DSL
val fakeClass = codeFile("com.example") {
    klass("FakeUserServiceImpl") {
        implements("UserService")

        property("getUserBehavior") {
            type("(String) -> User")
            mutable = true
            initializer = "{ id -> User(id, \"Default\") }"
        }

        function("getUser") {
            override = true
            parameter("id", "String")
            returns("User")
            body = "return getUserBehavior(id)"
        }
    }
}

// Render to string
val code = fakeClass.render()

// Write to disk
outputFile.writeText(code)
```

This DSL approach combines the benefits of type safety (compile-time validation) with the transparency of string-based output (readable `.kt` files).

---

## Why Not IR-Native Generation?

Some compiler plugins (like [Metro](https://github.com/zacsweers/metro)) generate code directly as **IR (Intermediate Representation) nodes**, bypassing the need to write `.kt` files. This approach has performance benefits but different trade-offs.

### Why Fakt Chose Differently

**Metro's Use Case (Dependency Injection)**:
- Generated code: Internal framework infrastructure
- Developer interaction: Rarely read generated code
- Performance priority: High (DI graph resolution at runtime)

**Fakt's Use Case (Test Fakes)**:
- Generated code: Test implementations
- Developer interaction: **Frequently read and debug generated fakes**
- Performance priority: Lower (test code generation at compile-time)

### IR-Native Trade-offs for Fakt

If Fakt used IR-native generation:

**Drawbacks**:
- ❌ **No readable `.kt` files** - Developers see decompiled bytecode in IDEs
- ❌ **Harder debugging** - Can't set breakpoints in generated code easily
- ❌ **API instability** - Kotlin IR APIs marked `@UnsafeApi` can change between versions
- ❌ **Higher complexity** - Requires deep FIR + IR expertise (~2-3x codebase size)

**Benefits**:
- ✅ Performance gain: ~10-50ms per fake (skips source → IR compilation)

**Decision**: For test fake generation, **transparency and debuggability outweigh marginal performance gains**.

!!! info "When IR-Native Makes Sense"
    IR-native generation is excellent for frameworks generating internal infrastructure code that developers don't need to read. For Fakt's use case (test fakes developers debug constantly), readable source files provide better developer experience.

---

## Why Not KotlinPoet?

[KotlinPoet](https://github.com/square/kotlinpoet) is a popular library for generating Kotlin code using a fluent API. It's used by many annotation processors and code generators.

### Why Fakt Built a Custom Solution

**KotlinPoet Trade-offs**:
- ❌ **Extra dependency** - Adds ~500KB to compiler plugin distribution
- ❌ **Learning curve** - Team must learn external API
- ❌ **Indirection** - KotlinPoet generates strings internally anyway
- ❌ **Generic API** - Designed for general use, not optimized for fake patterns

**Fakt's Custom DSL Benefits**:
- ✅ **Zero dependencies** - Leaner compiler plugin
- ✅ **Tailored patterns** - Built specifically for fake generation patterns
- ✅ **Direct control** - Full control over output format and structure
- ✅ **Simpler mental model** - One tool, one purpose

!!! tip "Not a General-Purpose Tool"
    Fakt's DSL is **not** a general-purpose code generator. It's optimized specifically for generating test fakes with call tracking, behavior configuration, and StateFlow counters. This specialization allows for cleaner APIs and better defaults.

---

## Benefits of This Approach

Fakt's type-safe DSL + string-based output provides:

### 1. **Transparency**

Generated fakes are **readable Kotlin source files**:

```kotlin
// build/generated/fakt/.../FakeUserServiceImpl.kt
class FakeUserServiceImpl : UserService {
    private var getUserBehavior: (String) -> User = { id ->
        User(id, "Default")
    }

    override fun getUser(id: String): User {
        return getUserBehavior(id)
    }
}
```

Developers can:
- Read generated code directly in their IDE
- Understand implementation without digging through IR dumps
- Verify correctness by inspection

### 2. **Debuggability**

Since generated code is real `.kt` files:
- ✅ Set breakpoints in generated fakes
- ✅ Step through execution line-by-line
- ✅ Inspect variables and behavior during tests
- ✅ Verify generated code matches expectations

This is **critical** for test code where developers need to understand why a test passes or fails.

### 3. **Stability**

**Kotlin syntax is stable**. Once Fakt generates valid Kotlin code, it continues working across Kotlin versions.

**IR APIs are unstable**. Marked `@UnsafeApi`, these APIs can change between Kotlin releases, requiring updates to Fakt's implementation.

### 4. **Simplicity**

Type-safe DSL is:
- ✅ **Easier to maintain** - Changes to fake patterns update DSL, not low-level IR manipulation
- ✅ **Easier to test** - Validate generated code as strings, not IR node structures
- ✅ **Easier to contribute** - Contributors need Kotlin knowledge, not IR expertise

### 5. **Zero Runtime Dependencies**

Generated fakes have **no runtime dependencies**:
- No reflection libraries
- No code generation utilities
- Just plain Kotlin code that compiles to native binaries

This is essential for Kotlin Multiplatform where runtime dependencies may not be available on all targets (Native, WASM).

---

## Deep Dive: Architecture Details

> This section is for contributors and maintainers working on Fakt's internals.

### DSL Layer Architecture

Fakt's code generation is structured in five layers:

```
┌─────────────────────────────────────────────────┐
│  Extension Functions                            │
│  High-level patterns: stateFlowProperty(),      │
│  behaviorProperty(), generateCompleteFake()     │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Builders (Mutable DSL API)                     │
│  ClassBuilder, FunctionBuilder, PropertyBuilder │
│  Kotlin DSL syntax for code construction        │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Model Layer (Immutable Data Structures)        │
│  CodeFile, CodeClass, CodeFunction, CodeProperty│
│  Pure data models representing Kotlin code      │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Strategy Layer                                 │
│  Default value resolution: primitives,          │
│  collections, Result<T>, generic types          │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Renderer (String Generation)                   │
│  renderTo(builder) - Convert models to strings  │
│  CodeBuilder - String accumulation + formatting │
└─────────────────────────────────────────────────┘
```

### Key Components

**1. Model Layer** (`codegen/model/CodeFile.kt`)
- Immutable data classes: `CodeFile`, `CodeClass`, `CodeFunction`, `CodeProperty`
- Type-safe representation of Kotlin code structures
- No string manipulation at this level

**2. Builder Layer** (`codegen/builder/`)
- `ClassBuilder`, `FunctionBuilder`, `PropertyBuilder`
- Mutable builders for DSL construction
- Convert to immutable models via `build()` method

**3. Extension Functions** (`codegen/extensions/FakeGenerator.kt`)
- High-level patterns: `generateCompleteFake()`, `stateFlowProperty()`
- Reusable fake generation patterns
- Compose lower-level builders into complete fake classes

**4. Renderer** (`codegen/renderer/Rendering.kt`)
- `renderTo(builder: CodeBuilder)` - Convert immutable models to strings
- `CodeBuilder` - String accumulation with indentation support
- Handles formatting, modifiers, annotations

**5. IR Bridge** (`compiler/ir/generation/ImplementationGenerator.kt`)
- Connects IR analysis phase to code generation DSL
- Converts `InterfaceAnalysis` (IR metadata) into DSL calls
- Entry point: `generateImplementation(analysis, packageName, imports)`

### File References

For contributors working on code generation:

| Component | File Path | Lines |
|-----------|-----------|-------|
| **Models** | `compiler/src/.../codegen/model/CodeFile.kt` | 273 |
| **Builders** | `compiler/src/.../codegen/builder/ClassBuilder.kt` | ~200 |
| **Extensions** | `compiler/src/.../codegen/extensions/FakeGenerator.kt` | 1159 |
| **Renderer** | `compiler/src/.../codegen/renderer/Rendering.kt` | ~150 |
| **IR Bridge** | `compiler/src/.../ir/generation/ImplementationGenerator.kt` | ~70 |
| **File Writer** | `compiler/src/.../ir/generation/CodeGenerator.kt` | ~120 |

---

## Trade-offs & When to Reconsider

### Accepted Trade-off: Two-Pass Compilation

Generated `.kt` files are parsed again by the Kotlin compiler:

```
Source code → IR → Generated .kt files → IR → Bytecode
              ↑                           ↑
          Fakt plugin             Kotlin compiler
```

IR-native generation would skip the second IR pass:

```
Source code → IR → Generated IR nodes → Bytecode
              ↑
          Fakt plugin
```

For test code generation, this overhead is **negligible** compared to the benefits of readable source files.

### When to Reconsider This Decision

Fakt would reconsider string-based generation if:

1. **Scale increases** - Projects generate 1000+ fakes where compilation time becomes critical
2. **Complexity grows** - Generated code patterns become difficult to maintain with string-based approach
3. **Runtime generation** - Need to generate fakes dynamically at runtime (not current use case)
4. **Advanced transformations** - Generic type manipulation beyond current capabilities

For now, **transparency and debuggability** remain higher priorities than marginal performance gains.

---

## Next Steps

### For Users

Understanding code generation strategy helps when:
- Debugging generated fakes
- Understanding performance characteristics
- Evaluating Fakt for your project

Related documentation:
- [API Reference](api.md) - Generated fake APIs
- [Limitations](limitations.md) - Current generation limitations
- [Performance Guide](../guides/performance.md) - Compilation benchmarks

### For Contributors

Working on code generation:
1. **Read ADR**: `.claude/docs/codegen-v2/ADR.md` - Complete architecture decision record
2. **Study DSL**: `.claude/docs/codegen-v2/README.md` - Codegen V2 overview
3. **Check approach doc**: `.claude/docs/architecture/code-generation-approach.md` - Authoritative decision document
4. **Explore code**: Start with `ImplementationGenerator.kt` → `FakeGenerator.kt` → `CodeFile.kt`

---

## Summary

Fakt's code generation strategy reflects its core philosophy:

> **Test code should be readable, debuggable, and transparent.**

By choosing **type-safe DSL + string-based generation**, Fakt delivers:
- ✅ Readable `.kt` files developers can inspect and understand
- ✅ Full debuggability with breakpoints and variable inspection
- ✅ Stability across Kotlin versions (syntax vs IR APIs)
- ✅ Zero runtime dependencies for cross-platform support

This approach may not be the fastest possible solution, but it's the **right solution for test fake generation**.
