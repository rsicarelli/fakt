# Multi-Module Support: Comparison Matrix

**Date**: 2025-10-05
**Purpose**: Side-by-side comparison of all evaluated architectural approaches

---

## Quick Decision Matrix

| Criterion                    | Custom Source Sets ✅ | Dedicated Modules | java-test-fixtures | Classifiers | Direct Artifacts |
|------------------------------|----------------------|-------------------|---------------------|-------------|------------------|
| **Zero module proliferation** | ✅ YES               | ❌ NO             | ✅ YES              | ✅ YES      | ✅ YES           |
| **Minimal Git footprint**     | ✅ YES               | ❌ NO             | ✅ YES              | ✅ YES      | ✅ YES           |
| **Full KMP support**          | ✅ YES               | ✅ YES            | ❌ NO (JVM only)    | ⚠️ PARTIAL  | ✅ YES           |
| **Excellent IDE experience**  | ✅ YES               | ✅ YES            | ✅ YES              | ⚠️ PARTIAL  | ❌ NO            |
| **Convention-agnostic**       | ✅ YES               | ❌ NO             | ✅ YES              | ✅ YES      | ✅ YES           |
| **Transitive dependencies**   | ✅ YES               | ✅ YES            | ✅ YES              | ❌ NO       | ⚠️ PARTIAL       |
| **GMM compatible**            | ✅ YES               | ✅ YES            | ✅ YES              | ❌ NO       | ⚠️ PARTIAL       |
| **Auto-wiring potential**     | ✅ YES               | ⚠️ COMPLEX        | ⚠️ COMPLEX          | ❌ NO       | ❌ NO            |
| **Implementation complexity** | ⚠️ HIGH              | ✅ LOW            | ✅ LOW              | ✅ LOW      | ✅ LOW           |

**Legend**:
- ✅ **Full support** - Works perfectly, no limitations
- ⚠️ **Partial support** - Works with caveats or workarounds
- ❌ **No support** - Fundamentally incompatible or severely limited

---

## Detailed Comparison

### 1. Custom Source Sets as Feature Variants ✅ **CHOSEN**

#### Architecture
```kotlin
:foundation
  ├── src/commonMain/       # Business code
  ├── src/fakes/            # Generated fakes (NEW source set)
  └── src/commonTest/       # Tests (depends on fakes)

:domain
  └── src/commonTest/       # Uses foundation fakes via capability
```

#### Configuration
```kotlin
// Producer (:foundation)
kotlin {
    sourceSets {
        val fakes = create("fakes") {
            dependsOn(commonMain.get())
        }
    }
}

configurations.create("jvmFakesElements") {
    isCanBeConsumed = true
    outgoing.capability("${group}:foundation-fakes:${version}")
}

// Consumer (:domain)
dependencies {
    commonTestImplementation(project(":foundation")) {
        capabilities {
            requireCapability("com.rsicarelli.fakt:foundation-fakes:1.0.0-SNAPSHOT")
        }
    }
}
```

#### Pros
- ✅ **Zero new modules**: Fakes live in same module as business code
- ✅ **Minimal Git impact**: Only `build.gradle.kts` modified (or convention plugin applied)
- ✅ **Full KMP support**: Works for JVM, JS, Native, Wasm
- ✅ **Excellent IDE**: Full indexing, navigation, completion
- ✅ **Convention-agnostic**: Plugin controls its own source set, doesn't care about company conventions
- ✅ **GMM native**: Uses capabilities correctly
- ✅ **Auto-wiring friendly**: Plugin can detect project dependencies and add capabilities automatically
- ✅ **Clean separation**: Fakes have their own dependency scope (won't leak to main)

#### Cons
- ⚠️ **High initial complexity**: Requires sophisticated convention plugin
- ⚠️ **Advanced Gradle knowledge**: Developers must understand capabilities (mitigated by docs)
- ⚠️ **One-time setup cost**: Convention plugin development effort

#### Git Footprint
```bash
$ git status
modified:   foundation/build.gradle.kts  # Added: id("fakt-convention")
# THAT'S IT! No new directories, no settings.gradle.kts changes
```

#### User Experience
```kotlin
// Step 1: Apply convention plugin (one line)
plugins {
    id("fakt-convention")
}

// Step 2: Use fakes in other modules (automatic with auto-wiring)
// NO MANUAL CONFIGURATION NEEDED!
```

---

### 2. Dedicated `:module-fakes` Modules ❌ **REJECTED**

#### Architecture
```kotlin
:foundation                # Original module
:foundation-fakes          # NEW module (auto-created by plugin)
  └── src/commonMain/      # Fakes in Main source set
:domain                    # Depends on :foundation-fakes
```

#### Configuration
```kotlin
// settings.gradle.kts (modified by plugin)
include(":foundation")
include(":foundation-fakes")  // Auto-created

// foundation-fakes/build.gradle.kts (auto-generated)
plugins {
    kotlin("multiplatform")
    // ??? Company-specific plugins ???
}

dependencies {
    api(project(":foundation"))  // Depend on original module
}

// Consumer (:domain)
dependencies {
    testImplementation(project(":foundation-fakes"))
}
```

#### Pros
- ✅ **Simple Gradle model**: Standard project dependency
- ✅ **Excellent IDE support**: Modules are first-class citizens
- ✅ **Full KMP compatibility**: Each module can be KMP
- ✅ **Low implementation complexity**: Straightforward module creation

#### Cons
- ❌ **High Git pollution**: New directories, `settings.gradle.kts` modified
- ❌ **Module proliferation**: 10 modules → 20 modules
- ❌ **Build convention conflicts**: Plugin must know company build conventions
- ❌ **Naming inconsistency**: No universal standard (`:fakes`, `:test-fakes`, `:test-utils`?)
- ❌ **Ownership confusion**: Plugin-generated but user-maintained?
- ❌ **Complex auto-wiring**: Plugin must modify `settings.gradle.kts` at runtime

#### Git Footprint
```bash
$ git status
modified:   settings.gradle.kts          # Added: include(":foundation-fakes")
new file:   foundation-fakes/build.gradle.kts
new file:   foundation-fakes/src/commonMain/kotlin/...
# 😱 Repository structure polluted!
```

#### User Experience
```kotlin
// Step 1: Enable Fakt plugin
plugins {
    id("com.rsicarelli.fakt")
}

// Step 2: Plugin auto-creates :foundation-fakes module
// User sees new directory appear in project structure
// IDE prompts: "New module detected, reimport?"

// Step 3: User confused
// - "Who owns this module? Can I modify it?"
// - "Should this be in .gitignore?"
// - "Why is settings.gradle.kts modified?"
```

#### Why Rejected
> **Critical User Concern**: "isso pode ser muito desafiador, pois cada empresa pode ter um próprio build-config específico. Isso também adicionaria mudanças no git do usuário."
>
> Creating modules requires knowing company-specific build conventions, which varies wildly across organizations. This approach is too invasive for repository structure.

---

### 3. java-test-fixtures Plugin ❌ **REJECTED**

#### Architecture
```kotlin
:foundation
  ├── src/main/           # Main code
  ├── src/testFixtures/   # Fakes (via java-test-fixtures)
  └── src/test/           # Tests

:domain
  └── src/test/           # Uses testFixtures(project(":foundation"))
```

#### Configuration
```kotlin
// Producer (:foundation)
plugins {
    `java-test-fixtures`
}

// Consumer (:domain)
dependencies {
    testImplementation(testFixtures(project(":foundation")))
}
```

#### Pros
- ✅ **Standard Gradle feature**: Built-in, well-documented
- ✅ **Good IDE support**: IntelliJ recognizes test fixtures
- ✅ **Publishing support**: Gradle publishes fixtures automatically

#### Cons
- ❌ **JVM-only**: No support for Native, JS targets
- ❌ **KMP incompatibility**: Recent Kotlin versions require platform-specific configs
  - Must use `jvmTestFixturesImplementation`, `jsTestFixturesImplementation`, etc.
  - No `commonTestFixtures` source set
- ❌ **Android complexity**: Requires `android.experimental.enableTestFixturesKotlinSupport=true`
- ❌ **Fragile integration**: Behavior changes across Gradle versions
- ❌ **Sealed to JVM ecosystem**: Fundamentally designed for Java/JVM

#### Compatibility Matrix

| Target Platform | Support Level |
|-----------------|---------------|
| JVM             | ✅ Full       |
| JS              | ❌ None       |
| Native (iOS)    | ❌ None       |
| Wasm            | ❌ None       |
| Android         | ⚠️ Experimental |

#### Why Rejected
> **Fundamental KMP incompatibility**. Fakt is a KMP-first project that must support all Kotlin targets equally. java-test-fixtures is JVM-centric and doesn't align with KMP architecture.

**Source**: Gemini Deep Research Report 2, Section 2.1

---

### 4. Classifier-Based Artifacts ❌ **REJECTED**

#### Architecture
```kotlin
:foundation
  └── Published artifacts:
      ├── foundation-1.0.jar                # Main artifact
      └── foundation-1.0-test-fakes.jar     # Fakes (classifier)
```

#### Configuration
```kotlin
// Producer (:foundation)
publishing {
    publications {
        create<MavenPublication>("main") {
            artifact(mainJar)
        }
        create<MavenPublication>("fakes") {
            artifact(fakesJar) {
                classifier = "test-fakes"
            }
        }
    }
}

// Consumer (:domain)
dependencies {
    testImplementation("com.example:foundation:1.0:test-fakes")
}
```

#### Pros
- ✅ **Familiar pattern**: Maven-style classifiers
- ✅ **Minimal build config**: Simple publishing setup
- ✅ **Works across Maven/Ivy**: Broad repository support

#### Cons
- ❌ **No transitive dependencies**: Maven POM limitation
  - Fakes' dependencies don't propagate to consumers
  - Must manually declare all transitive deps
- ❌ **GMM incompatible**: Bypasses Gradle Module Metadata
- ❌ **Manual target selection**: Consumer must specify `-jvm.jar` vs `-native.klib`
- ❌ **Defeats KMP resolution**: Bypasses variant-aware system
- ❌ **Poor IDE experience**: Classifiers don't integrate with source sets

#### Why Rejected
> **Legacy Maven pattern incompatible with modern KMP**. Gradle Module Metadata provides superior variant resolution, and classifiers bypass this entirely.

**Source**: Gemini Deep Research Report 2, Section 3.2

---

### 5. Direct Generated Artifact Consumption ❌ **REJECTED**

#### Architecture
```kotlin
:foundation
  └── build/generated/fakt/kotlin/  # Generated fakes

:domain
  └── Directly consumes build/generated/ directory
```

#### Configuration
```kotlin
// Producer (:foundation)
configurations.create("fakesElements") {
    isCanBeConsumed = true
    outgoing.artifact(file("build/generated/fakt/kotlin"))
}

// Consumer (:domain)
dependencies {
    testImplementation(project(":foundation")) {
        // Resolves to fakesElements configuration
    }
}
```

#### Pros
- ✅ **Zero structural changes**: No new source sets or modules
- ✅ **Simple initial implementation**: Minimal Gradle code
- ✅ **Fast to prototype**: Can validate concept quickly

#### Cons
- ❌ **IDE CATASTROPHE**: No source set indexing
  - No code completion
  - No "Go to Definition"
  - No refactoring support
  - Debugging shows decompiled bytecode
- ❌ **Build cache issues**: Brittle incremental compilation
  - Changes in foundation don't trigger domain recompilation
  - Cache keys unstable
- ❌ **No source attachment**: IDE can't link compiled output to source

#### Why Rejected
> **Destroys developer experience**. IDE integration is non-negotiable for professional tools. Developers expect IntelliJ-quality navigation and completion.

**Source**: Gemini Deep Research Report 2, Section 3.3

---

## Feature Comparison Table

### Developer Experience

| Feature                     | Custom Source Sets | Dedicated Modules | testFixtures | Classifiers | Direct Artifacts |
|-----------------------------|-------------------|-------------------|--------------|-------------|------------------|
| Code completion             | ✅ Full           | ✅ Full           | ✅ Full      | ⚠️ Partial  | ❌ None          |
| Go to Definition            | ✅ Full           | ✅ Full           | ✅ Full      | ⚠️ Partial  | ❌ None          |
| Refactoring support         | ✅ Full           | ✅ Full           | ✅ Full      | ❌ None     | ❌ None          |
| Debugging experience        | ✅ Full           | ✅ Full           | ✅ Full      | ⚠️ Partial  | ❌ Decompiled    |
| Incremental compilation     | ✅ Full           | ✅ Full           | ✅ Full      | ✅ Full     | ❌ Broken        |
| Configuration cache compat  | ✅ Yes            | ✅ Yes            | ✅ Yes       | ✅ Yes      | ⚠️ Fragile       |

---

### Build System Integration

| Feature                     | Custom Source Sets | Dedicated Modules | testFixtures | Classifiers | Direct Artifacts |
|-----------------------------|-------------------|-------------------|--------------|-------------|------------------|
| Transitive dependencies     | ✅ Full           | ✅ Full           | ✅ Full      | ❌ None     | ⚠️ Manual        |
| Gradle Module Metadata      | ✅ Full           | ✅ Full           | ✅ Full      | ❌ Bypass   | ⚠️ Partial       |
| Maven Central publishing    | ✅ Full           | ✅ Full           | ✅ Full      | ⚠️ Limited  | ❌ None          |
| Variant-aware resolution    | ✅ Full           | ✅ Full           | ✅ Full      | ❌ Bypass   | ⚠️ Partial       |
| Multi-repository support    | ✅ Full           | ✅ Full           | ✅ Full      | ✅ Full     | ❌ Local only    |

---

### Platform Support

| Platform | Custom Source Sets | Dedicated Modules | testFixtures | Classifiers | Direct Artifacts |
|----------|-------------------|-------------------|--------------|-------------|------------------|
| JVM      | ✅                | ✅                | ✅           | ✅          | ✅               |
| JS       | ✅                | ✅                | ❌           | ⚠️          | ✅               |
| Native   | ✅                | ✅                | ❌           | ⚠️          | ✅               |
| Wasm     | ✅                | ✅                | ❌           | ⚠️          | ✅               |
| Android  | ✅                | ✅                | ⚠️           | ✅          | ✅               |

---

### Implementation Effort

| Aspect                      | Custom Source Sets | Dedicated Modules | testFixtures | Classifiers | Direct Artifacts |
|-----------------------------|-------------------|-------------------|--------------|-------------|------------------|
| Plugin development effort   | 🔴 High (3 weeks) | 🟡 Medium (1 week)| 🟢 Low (2 days) | 🟢 Low (2 days) | 🟢 Low (2 days) |
| User configuration required | 🟢 Minimal (1 line)| 🟡 Medium        | 🟢 Minimal      | 🔴 High        | 🔴 High         |
| Documentation complexity    | 🟡 Medium         | 🟢 Low            | 🟢 Low          | 🟢 Low         | 🟢 Low          |
| Maintenance burden          | 🟡 Medium         | 🔴 High           | 🟢 Low          | 🟢 Low         | 🔴 High         |

**Legend**:
- 🟢 **Low** - Quick to implement, easy to maintain
- 🟡 **Medium** - Moderate effort required
- 🔴 **High** - Significant effort or ongoing burden

---

## Real-World Usage Comparison

### Scenario 1: Small Single-Module Project

**Setup**: 1 module with 5 interfaces

| Approach           | User Action                          | Result                        |
|--------------------|--------------------------------------|-------------------------------|
| Custom Source Sets | Apply `id("fakt-convention")`        | ✅ Works, zero extra config   |
| Dedicated Modules  | Apply plugin                         | ✅ Works, creates `:app-fakes`|
| testFixtures       | Apply `java-test-fixtures`           | ⚠️ JVM only                   |
| Classifiers        | Manual publishing config             | ❌ Too complex                |
| Direct Artifacts   | Manual configuration setup           | ❌ No IDE support             |

**Winner**: Tie between Custom Source Sets and testFixtures (if JVM-only acceptable)

---

### Scenario 2: Medium Multi-Module KMP Project

**Setup**: 5 modules (foundation → domain → features → ui → app), targeting JVM + iOS

| Approach           | Modules Created | Git Changes                  | KMP Support |
|--------------------|-----------------|------------------------------|-------------|
| Custom Source Sets | 0               | 5 `build.gradle.kts` (apply plugin) | ✅ Full     |
| Dedicated Modules  | 5               | `settings.gradle.kts` + 10 files    | ✅ Full     |
| testFixtures       | 0               | Not applicable                      | ❌ iOS fails|
| Classifiers        | 0               | 5 publishing configs                | ⚠️ Manual   |
| Direct Artifacts   | 0               | 10+ configuration files             | ⚠️ Brittle  |

**Winner**: Custom Source Sets (zero modules, full KMP support)

---

### Scenario 3: Large Enterprise Project

**Setup**: 50+ modules, custom build conventions, published to Maven Central

| Approach           | Build Conventions | Auto-Wiring | Publishing | Scalability |
|--------------------|-------------------|-------------|------------|-------------|
| Custom Source Sets | ✅ Agnostic       | ✅ Possible | ✅ Full    | ✅ Excellent|
| Dedicated Modules  | ❌ Must know      | ⚠️ Complex  | ✅ Full    | ⚠️ 100 modules|
| testFixtures       | ✅ Agnostic       | ⚠️ Complex  | ✅ Full    | ❌ JVM only |
| Classifiers        | ✅ Agnostic       | ❌ No       | ⚠️ Limited | ⚠️ Manual deps|
| Direct Artifacts   | ✅ Agnostic       | ❌ No       | ❌ No      | ❌ Breaks    |

**Winner**: Custom Source Sets (only option that scales with enterprise requirements)

---

## Community Precedents

### What Others Do

| Project              | Approach               | Notes                                    |
|----------------------|------------------------|------------------------------------------|
| OkHttp (Square)      | Dedicated modules      | `:okhttp-testing-support` module         |
| Jetpack Compose      | testFixtures           | JVM-only, Android-specific               |
| kotlinx.coroutines   | Dedicated modules      | `:kotlinx-coroutines-test` module        |
| Exposed (JetBrains)  | Dedicated modules      | `:exposed-test-utils` module             |
| Ktor                 | Dedicated modules      | `:ktor-client-mock` module               |

**Observation**: Most KMP libraries use dedicated modules because **there was no better alternative**.

**Our Innovation**: Custom Source Sets provides a superior approach that wasn't feasible before Gradle Module Metadata matured.

---

## Decision Trade-Off Analysis

### Custom Source Sets Choice

**What We Accept**:
- Higher initial plugin development complexity (3 weeks)
- Advanced Gradle knowledge required (mitigated by docs)
- Newer pattern (less community precedent)

**What We Gain**:
- Zero repository pollution
- Build convention independence
- Excellent developer experience
- Full KMP support
- Professional-quality tool

**Philosophy**:
> We accept higher **one-time implementation complexity** in exchange for superior **long-term user experience** and **architectural purity**.

The complexity is **encapsulated and reusable** - users apply `id("fakt-convention")` and everything just works.

---

## Appendix: Decision Timeline

### Research Phase (Oct 4-5, 2025)

1. **Initial Plan**: Dedicated modules approach (common pattern)
2. **User Challenge**: "isso pode ser muito desafiador... Será que essa é melhor opção em termos de devxp?"
3. **Gemini Research 1**: Confirmed dedicated modules are standard, found testFixtures alternative
4. **Critical Pivot**: User questioned necessity of module creation
5. **Gemini Research 2**: Discovered Custom Source Sets approach (Section 2 & 5)
6. **Final Decision**: Custom Source Sets as optimal solution

### Key Insight

> "Será que as pessoas nao fazem isso por falta de uma alternativa?"
>
> The community uses dedicated modules not because it's ideal, but because **they lack a better alternative**. Gradle Module Metadata now enables a superior approach.

---

## References

### Research Sources

1. **Gemini Deep Research 1**: "A Framework for Cross-Module Test Artifacts"
   - 45 citations
   - Covered: Dedicated modules, testFixtures, community patterns
   - File: `/Users/rsicarelli/Downloads/KMP Test Fakes Cross-Module Access.md`

2. **Gemini Deep Research 2**: "Advanced Techniques Without Dedicated Modules"
   - 34 citations
   - Covered: Custom source sets, classifiers, capabilities, direct artifacts
   - File: `/Users/rsicarelli/Downloads/KMP Test Utilities Without Modules.md`

### Gradle Documentation

- [Variant-Aware Resolution](https://docs.gradle.org/current/userguide/variant_aware_resolution.html)
- [Feature Variants and Optional Dependencies](https://docs.gradle.org/current/userguide/feature_variants.html)
- [Gradle Module Metadata Spec](https://github.com/gradle/gradle/blob/master/subprojects/docs/src/docs/design/gradle-module-metadata-latest-specification.md)

---

**Next**: See [CONVENTION-PLUGIN-BLUEPRINT.md](./CONVENTION-PLUGIN-BLUEPRINT.md) for implementation specification.
