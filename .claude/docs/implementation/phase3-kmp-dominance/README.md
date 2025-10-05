# Phase 3: KMP Market Leadership (Q3 2025)

> **Strategic Goal**: Become THE testing solution for Kotlin Multiplatform
> **Target**: Capture the underserved KMP market (greenfield opportunity)
> **Timeline**: 8-10 weeks
> **Status**: Planning

## Executive Summary

Phase 3 represents the **most strategic opportunity** for Fakt. While Phases 1-2 improve JVM testing, Phase 3 targets a market where **existing solutions are failing catastrophically**.

Kotlin Multiplatform is rapidly growing, but the testing ecosystem is broken:
- **Runtime mocking impossible** on Native/Wasm (no JVM runtime)
- **KSP-based tools unstable**, breaking with Kotlin updates
- **Developers struggling**, resorting to architecture compromises
- **No reliable solution** exists for `commonTest` across all platforms

**Fakt's compiler plugin architecture solves this**. We're more stable than KSP, more powerful than platform-specific tools, and can provide feature parity across all KMP targets.

---

## The KMP Testing Crisis

### Research Findings

From [Mocking in Kotlin Multiplatform: KSP vs Compiler Plugins](https://medium.com/@mhristev/mocking-in-kotlin-multiplatform-ksp-vs-compiler-plugins-4424751b83d7):

**The Fundamental Constraint:**
> "Kotlin/Native and Kotlin/Wasm compile to native binaries without a JVM. Runtime reflection and dynamic class generation are impossible. MockK and Mockito are JVM-only and cannot be used in `commonTest`."

**KSP Tool Failures:**

1. **Mockative** (broken by Kotlin 2.0):
   - Source: [Kotlin Multiplatform Unit Testing Discussion](https://www.reddit.com/r/KotlinMultiplatform/comments/1iiwypq/kotlin_multiplatform_unit_testing_best_libraries/)
   > "Mockative simply stopped working after Kotlin 2.0. We had to migrate 10,000+ tests to Mokkery."

2. **Mokkery** (limited capabilities):
   - Source: [Mokkery Limitations](https://mokkery.dev/docs/Limitations/)
   - Cannot mock: top-level functions, extension functions, final classes, objects, sealed types
   - Community report: "Fragile, breaks with compiler updates"

3. **MocKMP** (source set detection broken):
   - Source: Research article
   - Kotlin 2.0 changes broke `commonTest` code generation
   - Maintainer burden: Constant updates needed

### Developer Impact

**Current State:**
- Developers avoid `commonTest`, write platform-specific tests instead
- Architecture decisions driven by testing limitations (interface-heavy designs)
- Test coverage lower on KMP projects due to testing difficulty
- Frustration and uncertainty about KMP viability for production

**Market Opportunity:**
- **Greenfield**: No dominant, stable solution exists
- **Growing Market**: KMP adoption accelerating (iOS, Wasm, server-side)
- **High Pain**: Developers desperate for reliable tooling
- **First Mover Advantage**: Be THE recommended solution for KMP testing

---

## Fakt's Strategic Advantage

### Compiler Plugin > KSP

| Aspect | KSP-based Tools | Fakt (Compiler Plugin) |
|--------|----------------|------------------------|
| **Stability** | Breaks with Kotlin updates | More resilient, deeper integration |
| **Capabilities** | Limited to visible symbols | Full IR access, call-site replacement |
| **commonTest** | Fragile source set detection | Robust multi-platform generation |
| **Performance** | Annotation processing overhead | Integrated compilation phase |
| **Maintenance** | High burden for maintainers | Aligned with Kotlin compiler evolution |

### Architectural Superiority

**KSP Limitations:**
- Operates on Kotlin Symbol API (stable but limited)
- Cannot perform call-site transformations
- Struggles with platform-specific source sets
- Fragile to KMP project structure changes

**Fakt Advantages:**
- Full compiler plugin (FIR + IR access)
- Can perform advanced transformations (call-site replacement)
- Detects source sets reliably (already working in current Fakt)
- Adapts to KMP evolution with Kotlin compiler

---

## Phase 3 Features

### 3.1 Full commonTest Support
**Priority**: CRITICAL
**Complexity**: High (4-6 weeks)
**Detailed Docs**: [kmp/APPROACH.md](./kmp/APPROACH.md)

**Goal**: All Fakt features work seamlessly in `commonTest` source sets.

**Challenges:**
1. **Platform-specific IR**: Each platform (JVM, Native, JS, Wasm) has different IR representations
2. **Source set detection**: Distinguish `commonTest`, `jvmTest`, `iosTest`, etc.
3. **Code generation targets**: Where to write generated fakes (common vs. platform-specific)
4. **Type resolution**: Cross-platform types (expect/actual)

**Solution**: Platform-aware generation with shared interface.

```kotlin
// commonMain
@Fake
interface UserRepository {
    suspend fun getUser(id: String): User
}

// Generated in commonTest (works across all platforms!)
expect class FakeUserRepositoryImpl : UserRepository

// Generated in each platform test source set
// jvmTest, iosTest, jsTest, wasmTest
actual class FakeUserRepositoryImpl : UserRepository {
    // Platform-specific implementation (if needed)
    // Otherwise, identical code across platforms
}
```

---

### 3.2 Cross-platform Feature Parity
**Priority**: HIGH
**Complexity**: Medium (integrated with 3.1)
**Detailed Docs**: [kmp/FEATURE-PARITY.md](./kmp/FEATURE-PARITY.md)

**Goal**: All Phase 1-2 features work on all KMP targets.

| Feature | JVM | Native (iOS) | JS | Wasm |
|---------|-----|--------------|-----|------|
| Interface faking | ✅ | 🎯 | 🎯 | 🎯 |
| Generic support | ✅ | 🎯 | 🎯 | 🎯 |
| SAM interfaces | ✅ | 🎯 | 🎯 | 🎯 |
| Final classes | 🎯 P1 | 🎯 P3 | 🎯 P3 | 🎯 P3 |
| Singleton objects | 🎯 P1 | 🎯 P3 | 🎯 P3 | 🎯 P3 |
| Top-level functions | 🎯 P1 | 🎯 P3 | 🎯 P3 | 🎯 P3 |
| Data class builders | 🎯 P2 | 🎯 P3 | 🎯 P3 | 🎯 P3 |
| Sealed hierarchies | 🎯 P2 | 🎯 P3 | 🎯 P3 | 🎯 P3 |
| Flow producers | 🎯 P2 | 🎯 P3 | 🎯 P3 | 🎯 P3 |

**Strategy**: Start with core features (interfaces, generics, SAM), expand to advanced features.

---

### 3.3 Platform-specific Optimizations
**Priority**: MEDIUM (Polish)
**Complexity**: Medium (ongoing)

**Goal**: Leverage platform-specific capabilities where beneficial.

**Examples:**
- **JVM**: Use existing JVM-optimized infrastructure
- **Native**: Memory-efficient fake instances
- **JS**: Browser-specific test utilities
- **Wasm**: Future-proof for Wasm GC evolution

---

## Implementation Plan

### Week 1-2: Source Set Detection & commonTest Infrastructure

#### 1.1 Robust Source Set Detection

```kotlin
class KmpSourceSetDetector(
    private val project: Project
) {

    fun detectKmpStructure(): KmpProjectStructure {
        val kotlinExtension = project.kotlinExtension

        return KmpProjectStructure(
            isKmpProject = kotlinExtension.isMultiplatform(),
            commonTest = kotlinExtension.sourceSets.findByName("commonTest"),
            platformTests = kotlinExtension.sourceSets.filter { it.name.endsWith("Test") },
            targets = kotlinExtension.targets.map { it.name }
        )
    }

    fun getOutputDir(sourceSet: KotlinSourceSet): File {
        return when (sourceSet.name) {
            "commonTest" -> File(project.buildDir, "generated/fakt/common/test/kotlin")
            "jvmTest" -> File(project.buildDir, "generated/fakt/jvm/test/kotlin")
            "iosTest" -> File(project.buildDir, "generated/fakt/ios/test/kotlin")
            "jsTest" -> File(project.buildDir, "generated/fakt/js/test/kotlin")
            "wasmTest" -> File(project.buildDir, "generated/fakt/wasm/test/kotlin")
            else -> File(project.buildDir, "generated/fakt/${sourceSet.name}/kotlin")
        }
    }
}

data class KmpProjectStructure(
    val isKmpProject: Boolean,
    val commonTest: KotlinSourceSet?,
    val platformTests: List<KotlinSourceSet>,
    val targets: List<String>
)
```

#### 1.2 expect/actual Code Generation

```kotlin
// For interfaces in commonMain, generate expect class in commonTest
fun generateExpectFake(irInterface: IrClass): IrClass {
    return irFactory.buildClass {
        name = Name.identifier("Fake${irInterface.name}Impl")
        kind = ClassKind.CLASS
        isExpect = true // ✅ Mark as expect
    }.apply {
        superTypes = listOf(irInterface.defaultType)

        // Add expect methods (no implementation)
        irInterface.functions.forEach { function ->
            addFunction {
                name = function.name
                returnType = function.returnType
                isExpect = true
            }
        }
    }
}

// For each platform, generate actual class
fun generateActualFake(
    irInterface: IrClass,
    platform: KotlinPlatform
): IrClass {
    return irFactory.buildClass {
        name = Name.identifier("Fake${irInterface.name}Impl")
        kind = ClassKind.CLASS
        isActual = true // ✅ Mark as actual
    }.apply {
        superTypes = listOf(irInterface.defaultType)

        // Full implementation (same as current Fakt)
        generateBehaviorProperties(this, irInterface)
        generateOverrideMethods(this, irInterface)
        generateConfigurationMethods(this, irInterface)
    }
}
```

### Week 3-5: Platform-specific IR Generation

#### 2.1 Platform Detection

```kotlin
class PlatformAwareIrGenerator(
    private val pluginContext: IrPluginContext
) {

    fun detectPlatform(): KotlinPlatform {
        return when {
            pluginContext.platform.isJvm() -> KotlinPlatform.JVM
            pluginContext.platform.isNative() -> KotlinPlatform.NATIVE
            pluginContext.platform.isJs() -> KotlinPlatform.JS
            pluginContext.platform.isWasm() -> KotlinPlatform.WASM
            else -> KotlinPlatform.UNKNOWN
        }
    }

    fun generateFake(
        irInterface: IrClass,
        targetPlatform: KotlinPlatform
    ): IrClass {
        return when (targetPlatform) {
            KotlinPlatform.JVM -> generateJvmFake(irInterface)
            KotlinPlatform.NATIVE -> generateNativeFake(irInterface)
            KotlinPlatform.JS -> generateJsFake(irInterface)
            KotlinPlatform.WASM -> generateWasmFake(irInterface)
            else -> error("Unsupported platform")
        }
    }
}

enum class KotlinPlatform {
    JVM, NATIVE, JS, WASM, UNKNOWN
}
```

#### 2.2 Platform-specific Considerations

**JVM**: Use existing infrastructure (already working)

**Native (iOS, etc.)**:
- No reflection, no dynamic proxies (already handled by compile-time generation)
- Memory management: Ensure fake instances are GC-friendly
- Coroutines: Full support (kotlinx-coroutines-core works on Native)

**JS**:
- Dynamic typing: Not an issue (we generate typed code)
- Coroutines: Full support via kotlinx-coroutines-core

**Wasm**:
- Future-focused (Wasm GC evolving)
- Similar constraints to Native
- Coroutines: Full support planned

### Week 6-8: Testing & Validation

#### 3.1 Multi-platform Test Matrix

```kotlin
// Test project structure
project/
├── commonMain/
│   └── UserRepository.kt (@Fake interface)
├── commonTest/
│   ├── FakeUserRepositoryImpl.kt (expect class - generated)
│   └── UserRepositoryTest.kt (shared tests)
├── jvmTest/
│   └── FakeUserRepositoryImpl.kt (actual class - generated)
├── iosTest/
│   └── FakeUserRepositoryImpl.kt (actual class - generated)
├── jsTest/
│   └── FakeUserRepositoryImpl.kt (actual class - generated)
└── wasmTest/
    └── FakeUserRepositoryImpl.kt (actual class - generated)
```

#### 3.2 Cross-platform Test Suite

```kotlin
// In commonTest (runs on ALL platforms)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTest {

    @Test
    fun `GIVEN fake repository WHEN getting user THEN should return configured user`() = runTest {
        // This test runs on JVM, iOS Native, JS, Wasm!
        val fake = fakeUserRepository {
            getUser { id -> User(id, "Test User") }
        }

        val user = fake.getUser("123")

        assertEquals("123", user.id)
        assertEquals("Test User", user.name)
    }
}
```

### Week 9-10: Documentation & Community Engagement

#### 4.1 KMP-specific Documentation

- [ ] **Setup Guide**: Configuring Fakt for KMP projects
- [ ] **expect/actual Patterns**: How Fakt generates cross-platform fakes
- [ ] **Platform-specific Caveats**: Known limitations per platform
- [ ] **Migration Guide**: From Mockative/Mokkery to Fakt

#### 4.2 Community Positioning

- [ ] **Announcement Blog**: "Stable KMP Testing with Fakt"
- [ ] **Comparison Article**: Fakt vs. KSP-based tools (stability, features)
- [ ] **Conference Talk**: KotlinConf proposal - "The Future of KMP Testing"
- [ ] **Sample Projects**: Real KMP apps using Fakt

---

## Success Criteria

### Must Have (P0)
- ✅ **commonTest Support**: All core features work in `commonTest`
- ✅ **iOS Native**: Full support for Native targets (highest demand)
- ✅ **Stability**: No breakages across Kotlin 2.x updates
- ✅ **JVM Parity**: Same features available on Native as JVM
- ✅ **expect/actual Generation**: Correct platform-specific code

### Should Have (P1)
- ✅ **JS Support**: Web and Node.js targets
- ✅ **Wasm Support**: Future-proof for Wasm evolution
- ✅ **Performance**: <10% compilation overhead even for KMP
- ✅ **Clear Errors**: Platform-specific error messages

### Nice to Have (P2)
- ⏳ **Platform Optimizations**: Native-specific memory patterns
- ⏳ **IDE Integration**: IntelliJ recognizes expect/actual fakes
- ⏳ **Gradle Plugin UX**: Easy KMP project setup

---

## Competitive Analysis

### Fakt vs. KSP-based Tools

| Feature | Mockative | Mokkery | MocKMP | **Fakt** |
|---------|-----------|---------|--------|----------|
| **Stability** | ❌ Broken (Kotlin 2.0) | ⚠️ Fragile | ⚠️ Fragile | ✅ **Compiler plugin** |
| **Interfaces** | ✅ | ✅ | ✅ | ✅ |
| **Final classes** | ❌ | ❌ | ❌ | ✅ **(Phase 1)** |
| **Objects** | ❌ | ❌ | ❌ | ✅ **(Phase 1)** |
| **Top-level fns** | ❌ | ❌ | ❌ | ✅ **(Phase 1)** |
| **Sealed types** | ❌ | ❌ | ❌ | ✅ **(Phase 2)** |
| **Data builders** | ❌ | ❌ | ❌ | ✅ **(Phase 2)** |
| **Flow producers** | ⚠️ Manual | ⚠️ Manual | ⚠️ Manual | ✅ **(Phase 2)** |
| **Performance** | ⚠️ Moderate | ⚠️ Moderate | ⚠️ Moderate | ✅ **Compile-time** |
| **Community** | ⚠️ Inactive | ⚠️ Small | ⚠️ Small | 🎯 **Active** |

**Clear Differentiation**: Fakt is the only stable, comprehensive solution.

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Platform IR differences too complex | Medium | High | Start with JVM + Native, add others incrementally |
| Kotlin compiler changes break plugin | Low | High | Test against multiple Kotlin versions, engage with JetBrains |
| KSP tools improve stability | Low | Medium | We still have feature superiority (call-site replacement) |
| KMP adoption slows | Very Low | High | KMP is JetBrains' strategic priority, accelerating |
| Developers don't trust compiler plugins | Low | Medium | Build reputation with Phases 1-2, demonstrate stability |

---

## Go-to-Market Strategy

### Phase 3A: Soft Launch (Week 8)
- Beta release for early adopters
- Gather feedback from KMP community
- Validate stability across platforms

### Phase 3B: Public Launch (Week 10)
- Official 1.0 release with full KMP support
- Blog post: "Fakt: The Stable KMP Testing Solution"
- Reddit, Twitter, Kotlin Slack announcements
- Submit to Kotlin Weekly, Android Weekly

### Phase 3C: Community Engagement (Ongoing)
- Conference talk submissions (KotlinConf, Droidcon)
- Sample projects (KMP app with Fakt)
- Integration with popular KMP frameworks (Ktor, SQLDelight, etc.)
- Partnership with KMP library maintainers

---

## Next Steps

1. ✅ Review this README
2. 🎯 Read detailed approach: [kmp/APPROACH.md](./kmp/APPROACH.md)
3. 🎯 Set up KMP test project (all targets: JVM, iOS, JS, Wasm)
4. 🎯 Implement source set detection (extend current Gradle plugin)
5. 🎯 Prototype expect/actual generation for simple interface
6. 🎯 Validate on iOS Native (most critical target)
7. 🎯 Expand to JS and Wasm
8. 🎯 Create comprehensive cross-platform test suite

---

## References

- **Critical Research**: [KSP vs Compiler Plugins](https://medium.com/@mhristev/mocking-in-kotlin-multiplatform-ksp-vs-compiler-plugins-4424751b83d7)
- **Community Pain**: [KMP Unit Testing Discussion](https://www.reddit.com/r/KotlinMultiplatform/comments/1iiwypq/kotlin_multiplatform_unit_testing_best_libraries/)
- **Mokkery Limitations**: [Official Docs](https://mokkery.dev/docs/Limitations/)
- **Main Roadmap**: [../roadmap.md](../roadmap.md)

---

**Phase 3 = KMP market leadership. The biggest strategic opportunity.** 🌍
