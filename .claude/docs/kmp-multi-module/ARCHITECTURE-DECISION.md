# KMP Multi-Module Support: Architectural Decision Record

**Date**: 2025-10-05
**Status**: ✅ APPROVED
**Decision**: Custom Source Sets as Feature Variants
**Supersedes**: N/A (First multi-module implementation)

---

## Context and Problem Statement

Fakt generates test fakes at compile-time. The compiler plugin successfully generates fakes within a single module, but these fakes are **NOT accessible to dependent modules' tests**.

### The Core Challenge

```kotlin
// :foundation module
@Fake
interface Logger {
    fun info(message: String)
}
// ✅ Generates: FakeLoggerImpl, fakeLogger() factory

// :domain module (depends on :foundation)
class DomainModelsTest {
    @Test
    fun test() {
        val logger = fakeLogger() // ❌ Unresolved reference!
    }
}
```

**Problem**: Generated fakes exist in `:foundation/build/generated/` but are NOT:
1. Published as consumable artifacts
2. Available on `:domain`'s test classpath
3. Exposed for cross-module consumption

### User Concerns (Critical Requirements)

1. **❌ NO Module Proliferation**: Auto-creating `:module-fakes` siblings pollutes the project structure
2. **❌ NO Git Pollution**: New modules modify `settings.gradle.kts` and create directories
3. **❌ NO Build Convention Conflicts**: Each company has custom module conventions
4. **❌ NO Ownership Confusion**: Who owns the generated module? Plugin or user?
5. **✅ YES Excellent DX**: Fakes should "just work" with minimal configuration
6. **✅ YES Full KMP Support**: Must work across JVM, JS, Native targets

---

## Decision Drivers

### Must Have
- Zero new modules created
- Minimal Git footprint (only `build.gradle.kts` changes)
- Convention-agnostic (doesn't assume company build patterns)
- Full KMP target support (JVM, JS, Native, Wasm)
- Excellent IDE experience (navigation, completion, debugging)

### Should Have
- Auto-wiring of dependencies (zero manual config)
- Clear error messages when misconfigured
- Incremental compilation friendly
- Gradle configuration cache compatible

### Nice to Have
- Customizable by power users
- Performance competitive with dedicated modules
- Compatible with Gradle 7.x+

---

## Options Considered

### **Option 1: Dedicated `:module-fakes` Modules** ❌ REJECTED

**Description**: Auto-create sibling modules like `:foundation-fakes` for each producer.

```
:foundation
:foundation-fakes          # Auto-created by plugin
  └─ commonMain/           # Fakes in Main source set
       └─ FakeLogger.kt

:domain
  └─ commonTest/
       dependencies {
           implementation(project(":foundation-fakes"))
       }
```

**Pros**:
- ✅ Follows established community pattern
- ✅ Simple Gradle dependency model
- ✅ Excellent IDE support
- ✅ Full KMP compatibility

**Cons**:
- ❌ **High Git pollution**: New directories, `settings.gradle.kts` modified
- ❌ **Build convention conflicts**: Plugin must know company conventions
- ❌ **Naming inconsistency**: No universal standard (`:fakes`, `:test-fakes`, `:test-utils`?)
- ❌ **Ownership confusion**: Plugin-generated but user-maintained?
- ❌ **Module proliferation**: 10 modules → 20 modules

**Verdict**: ❌ **REJECTED** - Too invasive for repository structure

**Source**: Gemini Deep Research Report 1 (45 citations)

---

### **Option 2: java-test-fixtures Plugin** ❌ REJECTED

**Description**: Use Gradle's `java-test-fixtures` plugin.

```kotlin
plugins {
    `java-test-fixtures`
}

// Consumer
dependencies {
    testImplementation(testFixtures(project(":foundation")))
}
```

**Pros**:
- ✅ Standard Gradle feature
- ✅ Built-in publishing support
- ✅ Well-documented

**Cons**:
- ❌ **JVM-only**: No support for Native, JS targets
- ❌ **Android complexity**: Requires `android.experimental.enableTestFixturesKotlinSupport=true`
- ❌ **KMP incompatibility**: Recent Kotlin versions require target-specific configs (`jvmTestFixturesImplementation`)
- ❌ **Fragile integration**: Evolving, version-dependent behavior

**Verdict**: ❌ **REJECTED** - Fundamentally incompatible with KMP

**Source**: Gemini Deep Research Report 2, Section 2.1

---

### **Option 3: Classifier-Based Artifacts** ❌ REJECTED

**Description**: Publish secondary artifacts with Maven classifiers.

```xml
<!-- Published artifacts -->
foundation-1.0.jar
foundation-1.0-test-fakes.jar  <!-- Classifier -->
```

**Pros**:
- ✅ Familiar Maven pattern
- ✅ Minimal build config

**Cons**:
- ❌ **No transitive dependencies**: Maven POM limitation
- ❌ **GMM incompatible**: Doesn't work with Gradle Module Metadata
- ❌ **Manual target selection**: Consumer must specify `-jvm.jar` vs `-native.klib`
- ❌ **Defeats KMP resolution**: Bypasses variant-aware system

**Verdict**: ❌ **REJECTED** - Legacy approach incompatible with modern KMP

**Source**: Gemini Deep Research Report 2, Section 3.2

---

### **Option 4: Direct Generated Artifact Consumption** ❌ REJECTED

**Description**: Expose `build/generated/` directly as consumable configuration.

```kotlin
configurations.create("fakesElements") {
    outgoing.artifact(file("build/generated/fakt/kotlin"))
}
```

**Pros**:
- ✅ Zero structural changes
- ✅ Simple initial implementation

**Cons**:
- ❌ **IDE CATASTROPHE**: No indexing, no navigation, no debugging
- ❌ **Broken code completion**: IDE can't link compiled output to source
- ❌ **No source attachment**: Debugging shows decompiled bytecode
- ❌ **Build cache issues**: Brittle incremental compilation

**Verdict**: ❌ **REJECTED** - Destroys developer experience

**Source**: Gemini Deep Research Report 2, Section 3.3

---

### **Option 5: Custom Source Sets as Feature Variants** ✅ **CHOSEN**

**Description**: Create a `fakes` source set within each module and expose it as a consumable variant via Gradle capabilities.

```kotlin
// Producer (:foundation)
kotlin {
    sourceSets {
        val fakes by creating {
            dependsOn(commonMain.get())
        }

        // Per-target fakes
        val jvmFakes by creating { dependsOn(fakes) }
    }
}

// Published variants:
// - jvmFakesElements (capability: "foundation-fakes")
// - jsFakesElements (capability: "foundation-fakes")

// Consumer (:domain)
dependencies {
    commonTestImplementation(project(":foundation")) {
        capabilities {
            requireCapability("${group}:foundation-fakes:${version}")
        }
    }
}
```

**Pros**:
- ✅ **Zero module proliferation**: No new modules created
- ✅ **Minimal Git impact**: Only `build.gradle.kts` modified
- ✅ **Convention-agnostic**: Plugin controls its own source set
- ✅ **Excellent IDE support**: Full indexing via source set model
- ✅ **Full KMP compatibility**: Works with all targets
- ✅ **Proper dependency isolation**: Fakes' deps don't leak to main
- ✅ **GMM native**: Uses capabilities + attributes correctly

**Cons**:
- ⚠️ **High initial complexity**: Requires sophisticated convention plugin
- ⚠️ **Advanced Gradle knowledge**: Attributes, capabilities, variants
- ⚠️ **One-time setup cost**: Convention plugin development

**Mitigation**: Encapsulate complexity in reusable convention plugin. Users apply with 1 line: `id("fakt-convention")`.

**Verdict**: ✅ **CHOSEN** - Best balance of all requirements

**Source**: Gemini Deep Research Report 2, Section 2 & Section 5

---

## Decision

**We adopt Custom Source Sets as Feature Variants.**

### Rationale

1. **Minimal Invasiveness**: Zero new modules, zero Git pollution
2. **KMP-Native**: Uses Gradle's variant-aware resolution correctly
3. **Excellent DX**: Full IDE support, zero manual configuration (with auto-wiring)
4. **Architectural Purity**: Clean separation via source sets
5. **Future-Proof**: Works with current and future KMP targets

### Trade-Off Accepted

We accept higher **initial implementation complexity** (convention plugin) in exchange for:
- Superior long-term DX
- Cleaner repository structure
- Framework-agnostic solution

The complexity is **encapsulated and reusable** - users don't see it.

---

## Implementation Strategy

### **Phase 1: Convention Plugin** (Week 1)
Create `fakt-convention.gradle.kts` that:
1. Creates `fakes` source set hierarchy
2. Generates per-target consumable configurations
3. Configures attributes and capabilities

### **Phase 2: Compiler Integration** (Week 2)
Update Fakt compiler plugin to:
1. Generate code to `src/fakes/kotlin/`
2. Register generated dir with fakes source set
3. Ensure compilation includes generated code

### **Phase 3: Auto-Wiring** (Week 3)
Implement dependency auto-detection:
1. Detect `ProjectDependency` in test configurations
2. Auto-add capability requirements
3. Provide opt-out via DSL

---

## Consequences

### Positive

- ✅ **Zero repository pollution**: Git history stays clean
- ✅ **Company-agnostic**: Works regardless of build conventions
- ✅ **Scalable**: Works for 1 or 100 modules identically
- ✅ **Professional DX**: Matches quality of established tools
- ✅ **KMP-ready**: Future targets work automatically

### Negative

- ⚠️ **Higher learning curve**: Team must understand capabilities (mitigated by docs)
- ⚠️ **Convention plugin maintenance**: Additional plugin to maintain (mitigated by thorough testing)

### Neutral

- ℹ️ **Departure from community norm**: Most KMP projects use dedicated modules (we're innovating)
- ℹ️ **Gradle version dependency**: Requires Gradle 7.x+ for full GMM support

---

## Validation

### Success Criteria

- [ ] 19 multi-module tests pass (`:foundation` → `:domain` → `:features` → `:app`)
- [ ] Zero new modules created
- [ ] IDE navigation works (`Go to Definition` on `fakeLogger()`)
- [ ] Auto-wiring enabled by default
- [ ] Works on JVM, JS, Native targets

### Benchmarks

- Incremental compilation: < 5 seconds (target)
- IDE indexing time: Comparable to main source sets
- Build cache hit rate: > 80% (target)

---

## References

### Primary Sources

1. **Gemini Deep Research 1**: "A Framework for Cross-Module Test Artifacts" (45 citations)
   - Community patterns analysis
   - Dedicated modules approach
   - java-test-fixtures deep dive

2. **Gemini Deep Research 2**: "Advanced Techniques Without Dedicated Modules" (34 citations)
   - Custom source sets solution
   - Gradle variant resolution deep dive
   - Convention plugin blueprint

### Key Technical Docs

- [Gradle Variant-Aware Resolution](https://docs.gradle.org/current/userguide/variant_aware_resolution.html)
- [Gradle Capabilities](https://docs.gradle.org/current/userguide/how_to_create_feature_variants_of_a_library.html)
- [KMP Source Sets](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-dsl-reference.html)

### Community Examples

- Android Gradle Plugin: `testFixtures.enable = true` (similar pattern)
- JetBrains Exposed: Uses dedicated modules (anti-pattern we're avoiding)
- Square OkHttp: Uses dedicated modules (anti-pattern we're avoiding)

---

## Appendix: Why NOT Dedicated Modules

### Git Impact Analysis

```bash
# Dedicated Modules Approach:
$ git status
modified:   settings.gradle.kts          # Added :foundation-fakes
new file:   foundation-fakes/build.gradle.kts
new file:   foundation-fakes/src/commonMain/...

# Custom Source Sets Approach:
$ git status
modified:   foundation/build.gradle.kts  # Applied convention plugin
# THAT'S IT!
```

### Build Convention Conflicts

```kotlin
// Company A's module convention:
plugins {
    id("company-a.kotlin-library")        // Custom
    id("company-a.publishing")            // Custom
    id("company-a.code-quality")          // Custom
}

// Company B's module convention:
plugins {
    id("com.company-b.kmp-module")        // Custom
    kotlin("multiplatform") version X     // Custom version
}

// Fakt would need to know BOTH conventions to create modules!
// With custom source sets: WE DON'T CARE! We control our own source set.
```

### Ownership Clarity

**Dedicated Modules**:
- Plugin creates `:foundation-fakes`
- User sees it in IDE, might modify
- Plugin regenerates, conflicts arise
- **Unclear ownership**

**Custom Source Sets**:
- Plugin manages `fakes` source set
- Lives in same module as business code
- User knows it's plugin-managed
- **Clear ownership**

---

## Approval

**Approved by**: Development Team
**Date**: 2025-10-05
**Next Steps**: Proceed to implementation (see IMPLEMENTATION-ROADMAP.md)
