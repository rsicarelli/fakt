# KMP Multi-Module Support Documentation

**Status**: Design Complete - Ready for Implementation
**Strategy**: Custom Source Sets as Feature Variants
**Target**: 3-week implementation timeline
**Last Updated**: 2025-10-05

---

## 📚 Documentation Index

### **Start Here**

1. **[ARCHITECTURE-DECISION.md](./ARCHITECTURE-DECISION.md)** ⭐
   - Problem statement and context
   - Evaluation of 5 architectural approaches
   - Decision rationale and trade-offs
   - **Why custom source sets** over dedicated modules

2. **[IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md)** ⭐
   - 3-week implementation plan
   - Day-by-day tasks with code examples
   - Success metrics and validation steps
   - Risk mitigation strategies

---

### **Technical References**

3. **[TECHNICAL-REFERENCE.md](./TECHNICAL-REFERENCE.md)**
   - Deep dive into Gradle fundamentals
   - Variant attributes matrix for KMP targets
   - Configuration anatomy and patterns
   - Complete working examples
   - Debugging guide and troubleshooting

4. **[CONVENTION-PLUGIN-BLUEPRINT.md](./CONVENTION-PLUGIN-BLUEPRINT.md)**
   - Complete convention plugin implementation
   - Source set configuration patterns
   - Variant configuration with capabilities
   - Testing strategy and IDE integration
   - Gradle 9.0 and Kotlin 2.2.20 specific features

5. **[COMPARISON-MATRIX.md](./COMPARISON-MATRIX.md)**
   - Side-by-side comparison of all approaches
   - Feature comparison tables (DX, build system, platforms)
   - Real-world scenario analysis
   - Community precedents and our innovation

---

### **User Documentation**

6. **[FAQ.md](./FAQ.md)**
   - General questions and answers
   - Setup and configuration guide
   - KMP-specific questions
   - IDE integration tips
   - Publishing and distribution
   - Troubleshooting common issues

---

## 🎯 Quick Overview

### The Problem

Fakt generates test fakes at compile-time within a single module. These fakes are **NOT accessible** to dependent modules' tests:

```kotlin
// :foundation module
@Fake interface Logger
// ✅ Generates: FakeLoggerImpl, fakeLogger() factory

// :domain module (depends on :foundation)
import foundation.fakeLogger  // ❌ Unresolved reference!
```

---

### The Solution

**Custom Source Sets as Feature Variants** using Gradle capabilities:

```kotlin
// Producer (:foundation)
plugins {
    id("fakt-convention")  // Creates 'fakes' source set
}

// Gradle exposes fakes as consumable variant with unique capability

// Consumer (:domain)
dependencies {
    commonTestImplementation(project(":foundation")) {
        capabilities {
            requireCapability("com.rsicarelli.fakt:foundation-fakes:1.0.0")
        }
    }
}

// Usage
import foundation.fakeLogger  // ✅ Works!
val logger = fakeLogger { info { message -> println(message) } }
```

---

### Key Benefits

- ✅ **Zero module proliferation** - No `:foundation-fakes` modules created
- ✅ **Minimal Git footprint** - Only `build.gradle.kts` modified
- ✅ **Full KMP support** - Works for JVM, JS, Native, Wasm, Android
- ✅ **Excellent IDE experience** - Full indexing, navigation, debugging
- ✅ **Convention-agnostic** - Works regardless of company build patterns
- ✅ **Auto-wiring capable** - Plugin can detect and configure automatically

---

## 📅 Implementation Timeline

### **Week 1: Convention Plugin Core** (Days 1-5)
- Create `fakt-convention.gradle.kts`
- Configure source sets (`fakes`, `jvmFakes`, etc.)
- Set up consumable variants with capabilities
- Integration testing

**Deliverable**: Working convention plugin, manual capability declaration

---

### **Week 2: Compiler Integration** (Days 6-10)
- Update generation path: `build/generated/` → `src/fakes/kotlin/`
- Register generated sources with source sets
- IDE indexing validation
- Compilation verification

**Deliverable**: Fakes generated to source sets, cross-module imports work

---

### **Week 3: Auto-Wiring & Polish** (Days 11-15)
- Automatic capability requirement injection
- Configuration DSL (`fakt { autoWireDependencies = true }`)
- Comprehensive documentation
- Sample projects

**Deliverable**: Production-ready feature, zero manual configuration

---

## 🚀 Getting Started (Future)

Once implemented, usage will be:

### Producer Module (`:foundation`)

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("fakt-convention")  // One line!
}

kotlin {
    jvm()
    js(IR) { nodejs() }
}
```

```kotlin
// src/commonMain/kotlin/foundation/Logger.kt
package foundation

import com.rsicarelli.fakt.Fake

@Fake
interface Logger {
    fun info(message: String)
}
```

**Result**: Fakes generated to `src/fakes/kotlin/foundation/FakeLoggerImpl.kt`

---

### Consumer Module (`:domain`)

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":foundation"))
            }
        }

        commonTest {
            dependencies {
                // With auto-wiring (Week 3): NO MANUAL CONFIG!
                // Plugin detects :foundation dependency and adds fakes automatically

                // Manual (Week 1-2):
                implementation(project(":foundation")) {
                    capabilities {
                        requireCapability("com.rsicarelli.fakt:foundation-fakes:1.0.0")
                    }
                }
            }
        }
    }
}
```

```kotlin
// src/commonTest/kotlin/domain/UserRepositoryTest.kt
package domain

import foundation.fakeLogger  // ✅ Cross-module import works!
import kotlin.test.Test

class UserRepositoryTest {
    @Test
    fun test() {
        val logger = fakeLogger {
            info { message -> println("Logged: $message") }
        }

        // Use logger in tests
    }
}
```

---

## 🏗️ Architecture Highlights

### Source Set Hierarchy

```
commonMain
    ↓
  fakes (new source set)
    ↓
jvmFakes ← jvmMain
    ↓
commonTest ← fakes
    ↓
jvmTest ← jvmFakes
```

### Gradle Variant Resolution

```
:foundation publishes:
├── jvmApiElements (main variant)
└── jvmFakesElements (fakes variant)
    ├── Attributes: { platform=jvm, usage=java-api }
    └── Capability: "foundation-fakes"

:domain consumes:
└── testImplementation(":foundation")
    └── requireCapability("foundation-fakes")
        → Resolves to jvmFakesElements ✅
```

---

## 📊 Success Metrics

### Quantitative

- [ ] **Zero modules created**: Git status shows ONLY `build.gradle.kts` changes
- [ ] **19 tests passing**: All multi-module test scenarios green
- [ ] **< 5s incremental build**: Compilation performance target
- [ ] **100% IDE navigation**: All "Go to Definition" commands work
- [ ] **Zero manual capability declarations** (with auto-wiring)

### Qualitative

- [ ] **User Experience**: Apply plugin with 1 line, fakes "just work"
- [ ] **Documentation Quality**: Onboard new team member in < 1 hour
- [ ] **Error Messages**: Clear actionable guidance when misconfigured
- [ ] **Code Quality**: Passes all ktlint and detekt checks

---

## 🔗 Related Documentation

### Project Documentation

- **[/ktfake/CLAUDE.md](../../CLAUDE.md)** - Project overview and development guidelines
- **[Testing Guidelines](../validation/testing-guidelines.md)** - GIVEN-WHEN-THEN standard
- **[Metro Alignment](../development/metro-alignment.md)** - Architectural inspiration
- **[Current Status](../implementation/current-status.md)** - Overall project status

### External References

- [Gradle Variant-Aware Resolution](https://docs.gradle.org/current/userguide/variant_aware_resolution.html)
- [Gradle Feature Variants](https://docs.gradle.org/current/userguide/feature_variants.html)
- [Kotlin Multiplatform DSL](https://kotlinlang.org/docs/multiplatform-dsl-reference.html)
- [Gradle Module Metadata Spec](https://github.com/gradle/gradle/blob/master/subprojects/docs/src/docs/design/gradle-module-metadata-latest-specification.md)

---

## 📝 Research Sources

This design is based on two comprehensive Gemini Deep Research reports:

1. **"A Framework for Cross-Module Test Artifacts"** (45 citations)
   - Analyzed community patterns (dedicated modules, test fixtures)
   - Evaluated standard approaches
   - File: `/Users/rsicarelli/Downloads/KMP Test Fakes Cross-Module Access.md`

2. **"Advanced Techniques Without Dedicated Modules"** (34 citations)
   - Discovered custom source sets approach
   - Deep dive on Gradle capabilities and GMM
   - File: `/Users/rsicarelli/Downloads/KMP Test Utilities Without Modules.md`

---

## 🎯 Next Steps

### For Implementers

1. Read [ARCHITECTURE-DECISION.md](./ARCHITECTURE-DECISION.md) to understand **why**
2. Review [IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md) for **what** to build
3. Follow [CONVENTION-PLUGIN-BLUEPRINT.md](./CONVENTION-PLUGIN-BLUEPRINT.md) for **how** to build
4. Use [TECHNICAL-REFERENCE.md](./TECHNICAL-REFERENCE.md) for deep technical details

### For Users (Future)

1. Read [FAQ.md](./FAQ.md) for common questions
2. Apply `id("fakt-convention")` to your modules
3. Enjoy cross-module fake access with zero configuration!

---

## 💬 Feedback

**During Implementation**:
- GitHub Discussions for questions
- GitHub Issues for bug reports
- Continuous documentation updates

**After Release**:
- User testing and feedback
- Performance benchmarking
- Edge case discovery

---

**Status**: Ready for Week 1 Implementation
**Estimated Completion**: 3 weeks from start
**Target Gradle**: 9.0+
**Target Kotlin**: 2.2.20+

---

**Last Updated**: 2025-10-05
**Next Milestone**: Begin Week 1, Day 1 - Create convention plugin structure
