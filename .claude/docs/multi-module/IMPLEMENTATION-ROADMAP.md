# Multi-Module Support: Implementation Roadmap

**Strategy**: Custom Source Sets as Feature Variants
**Duration**: 3 weeks
**Target**: Zero-config cross-module fake accessibility

---

## 🎯 Goals

### Primary
- ✅ Enable cross-module fake consumption (`domain` tests use `foundation` fakes)
- ✅ Zero new modules created
- ✅ Automatic dependency wiring (no manual configuration)

### Secondary
- ✅ Full KMP support (JVM, JS, Native)
- ✅ Excellent IDE experience (navigation, completion, debugging)
- ✅ Incremental compilation friendly

### Stretch
- ✅ Gradle configuration cache compatible
- ✅ Performance competitive with dedicated modules
- ✅ Comprehensive documentation and samples

---

## 📅 3-Week Timeline

```
Week 1: Convention Plugin Core
├─ Day 1-2: Source set creation
├─ Day 3-4: Variant configuration
└─ Day 5: Integration testing

Week 2: Compiler Plugin Integration
├─ Day 1-2: Update generation path
├─ Day 3-4: Source set registration
└─ Day 5: Validation and IDE testing

Week 3: Auto-Wiring & Polish
├─ Day 1-3: Dependency auto-wiring
├─ Day 4: Configuration DSL
└─ Day 5: Documentation and samples
```

---

## Week 1: Convention Plugin Core

**Objective**: Create reusable `fakt-convention.gradle.kts` that sets up `fakes` source sets and consumable variants.

### Day 1-2: Source Set Creation

**Goal**: Establish `fakes` source set hierarchy for all targets.

**Tasks**:
1. Create `buildSrc/src/main/kotlin/fakt-convention.gradle.kts`
2. Implement common `fakes` source set
3. Create per-target fakes source sets (jvmFakes, jsFakes, etc.)

**Code Deliverable**:
```kotlin
// fakt-convention.gradle.kts
plugins {
    kotlin("multiplatform")
}

val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

kotlin.run {
    sourceSets {
        // 1. Create common fakes source set
        val fakes by creating {
            dependsOn(getByName("commonMain"))
        }

        // 2. Local test access
        getByName("commonTest").dependsOn(fakes)
    }

    // 3. Per-target fakes source sets
    targets.all { target ->
        sourceSets.create("${target.name}Fakes") {
            dependsOn(sourceSets.getByName("fakes"))
            dependsOn(target.compilations.getByName("main").defaultSourceSet)
        }
    }
}
```

**Validation**:
```bash
# Apply convention to samples/multi-module/foundation
./gradlew :samples:multi-module:foundation:sourceSets
# Expected: See 'fakes', 'jvmFakes', 'jsFakes' source sets
```

---

### Day 3-4: Variant Configuration

**Goal**: Create consumable configurations (variants) with correct attributes and capabilities.

**Tasks**:
1. Create per-target `*FakesElements` configurations
2. Copy attributes from main API variants
3. Declare unique `-fakes` capability
4. Attach compilation artifacts

**Code Deliverable**:
```kotlin
// Continuing in fakt-convention.gradle.kts
kotlin.targets.all { target ->
    val targetFakes = sourceSets.getByName("${target.name}Fakes")

    // 1. Create consumable configuration
    val fakesElements = configurations.create("${target.name}FakesElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        description = "Exposes fakes for target '${target.name}'"

        // 2. Expose fakes' dependencies
        extendsFrom(configurations.getByName("${targetFakes.name}Api"))
        extendsFrom(configurations.getByName("${targetFakes.name}Implementation"))
    }

    // 3. Attach main compilation's artifact
    val mainCompilation = target.compilations.getByName("main")
    fakesElements.outgoing.artifact(mainCompilation.output.allOutputs.first())

    // 4. Copy attributes from main API variant
    val mainApiElements = configurations.getByName(target.apiElementsConfigurationName)
    fakesElements.attributes.putAll(mainApiElements.attributes)

    // 5. Add unique capability
    fakesElements.outgoing.capability("${project.group}:${project.name}-fakes:${project.version}")
}
```

**Validation**:
```bash
# Check published variants
./gradlew :samples:multi-module:foundation:outgoingVariants
# Expected: See jvmFakesElements, jsFakesElements with "-fakes" capability
```

---

### Day 5: Integration Testing

**Goal**: Validate convention plugin works end-to-end.

**Tasks**:
1. Apply convention to `:foundation` module
2. Manually consume fakes in `:domain` tests
3. Verify capability resolution works
4. Test with multiple targets (JVM, JS)

**Test Case**:
```kotlin
// domain/build.gradle.kts
dependencies {
    commonTestImplementation(project(":samples:multi-module:foundation")) {
        capabilities {
            requireCapability("com.rsicarelli.fakt:foundation-fakes:1.0.0-SNAPSHOT")
        }
    }
}
```

**Validation**:
```bash
# Build domain tests
./gradlew :samples:multi-module:domain:compileTestKotlinJvm
# Expected: Resolves foundation-fakes variant successfully
```

**Deliverables**:
- [ ] `fakt-convention.gradle.kts` in `buildSrc/`
- [ ] Unit tests for source set creation logic
- [ ] Integration test in samples showing manual consumption
- [ ] Documentation of configuration attributes

---

## Week 2: Compiler Plugin Integration

**Objective**: Update Fakt compiler plugin to generate code into `src/fakes/` instead of `build/generated/`.

### Day 1-2: Update Generation Path

**Goal**: Modify `SourceSetConfigurator` to target fakes source set.

**Tasks**:
1. Update `getGeneratedSourcesDirectory()` logic
2. Detect if `fakt-convention` is applied
3. Generate to `src/<target>Fakes/kotlin/`

**Code Deliverable**:
```kotlin
// SourceSetConfigurator.kt
fun getGeneratedSourcesDirectory(compilation: KotlinCompilation<*>): String {
    val targetName = compilation.target.name
    val projectDir = project.projectDir

    // Check if fakt-convention is applied
    val hasFaktConvention = project.plugins.hasPlugin("fakt-convention")

    return if (hasFaktConvention) {
        // Generate to fakes source set
        File(projectDir, "src/${targetName}Fakes/kotlin").absolutePath
    } else {
        // Fallback to build/generated (backward compat)
        File(buildDir, "generated/fakt/$targetName/test/kotlin").absolutePath
    }
}
```

**Validation**:
```bash
# Build foundation with convention applied
./gradlew :samples:multi-module:foundation:compileKotlinJvm
# Expected: FakeLogger.kt appears in src/jvmFakes/kotlin/
```

---

### Day 3-4: Source Set Registration

**Goal**: Ensure generated code is included in fakes source set compilation.

**Tasks**:
1. Register generated directory with fakes source set's `kotlin.srcDir()`
2. Ensure compilation tasks pick up generated code
3. Handle incremental compilation correctly

**Code Deliverable**:
```kotlin
// SourceSetConfigurator.kt
private fun registerGeneratedSources(kotlin: KotlinMultiplatformExtension) {
    kotlin.targets.all { target ->
        val targetFakesName = "${target.name}Fakes"
        val fakesSourceSet = kotlin.sourceSets.findByName(targetFakesName) ?: return@all

        // Add generated directory to source set
        val generatedDir = file("src/$targetFakesName/kotlin")
        fakesSourceSet.kotlin.srcDir(generatedDir)

        project.logger.info("Fakt: Registered $generatedDir for source set $targetFakesName")
    }
}
```

**Validation**:
```bash
# Check compilation includes generated code
./gradlew :samples:multi-module:foundation:compileTestKotlinJvm --info
# Expected: Logs show src/jvmFakes/kotlin/ in source paths
```

---

### Day 5: Validation and IDE Testing

**Goal**: Verify IDE indexing and developer experience.

**Tasks**:
1. Open project in IntelliJ IDEA
2. Test "Go to Definition" on `fakeLogger()` from domain module
3. Verify code completion works
4. Test debugging into fake implementation

**Manual Test Checklist**:
- [ ] IDE indexes fakes source set
- [ ] Code completion suggests `fakeLogger()`
- [ ] "Go to Definition" navigates to `FakeLoggerImpl.kt`
- [ ] Breakpoints work in fake code
- [ ] No red squiggles in consumer tests

**Deliverables**:
- [ ] Updated `SourceSetConfigurator.kt`
- [ ] Compiler plugin generates to `src/fakes/`
- [ ] IDE experience validated manually
- [ ] Screenshots of working navigation

---

## Week 3: Auto-Wiring & Polish

**Objective**: Implement automatic dependency wiring and finalize the feature.

### Day 1-3: Dependency Auto-Wiring

**Goal**: Automatically add capability requirements for project dependencies.

**Tasks**:
1. Detect `ProjectDependency` in test configurations
2. Check if dependency has `fakt-convention` applied
3. Auto-add capability requirement

**Code Deliverable**:
```kotlin
// FaktGradleSubplugin.kt
private fun autoWireFakesDependencies(project: Project) {
    project.afterEvaluate {
        // Find all test configurations
        val testConfigs = project.configurations.matching {
            it.name.contains("Test") &&
            (it.name.endsWith("Implementation") || it.name.endsWith("Api"))
        }

        testConfigs.all { testConfig ->
            // Iterate project dependencies
            testConfig.dependencies.withType<ProjectDependency>().all { projectDep ->
                val targetProject = projectDep.dependencyProject

                // Check if target has fakt-convention
                if (targetProject.plugins.hasPlugin("fakt-convention")) {
                    val fakesCapability = "${targetProject.group}:${targetProject.name}-fakes:${targetProject.version}"

                    // Create new dependency with capability
                    val fakesDep = project.dependencies.create(targetProject) as ProjectDependency
                    fakesDep.capabilities {
                        requireCapability(fakesCapability)
                    }

                    // Add to configuration
                    testConfig.dependencies.add(fakesDep)

                    project.logger.info("Fakt: Auto-wired fakes from ${targetProject.name}")
                }
            }
        }
    }
}
```

**Validation**:
```kotlin
// domain/build.gradle.kts - NO MANUAL CONFIG NEEDED!
dependencies {
    implementation(project(":foundation"))
    // Auto-wiring adds fakes automatically ✨
}
```

```bash
./gradlew :samples:multi-module:domain:dependencies --configuration commonTestCompileClasspath
# Expected: See foundation-fakes capability resolved
```

---

### Day 4: Configuration DSL

**Goal**: Provide user control over auto-wiring behavior.

**Tasks**:
1. Create `FaktExtension` with configuration options
2. Add `autoWireDependencies` property
3. Implement opt-out mechanism

**Code Deliverable**:
```kotlin
// FaktExtension.kt
abstract class FaktExtension {
    abstract val autoWireDependencies: Property<Boolean>

    abstract val fakesSourceSetName: Property<String>

    init {
        autoWireDependencies.convention(true)  // Default: enabled
        fakesSourceSetName.convention("fakes")
    }
}

// In plugin registration
val extension = project.extensions.create("fakt", FaktExtension::class.java)

// User configuration
// build.gradle.kts
fakt {
    autoWireDependencies.set(false) // Opt-out
}
```

**Validation**:
```bash
# With auto-wire disabled, should NOT add capabilities
./gradlew :samples:multi-module:domain:dependencies
# Expected: Only explicit capability declarations resolved
```

---

### Day 5: Documentation and Samples

**Goal**: Comprehensive documentation and working examples.

**Tasks**:
1. Update all `.claude/docs/multi-module/` files
2. Add inline code comments
3. Create troubleshooting guide
4. Write migration guide from dedicated modules

**Deliverables**:
- [ ] README.md for multi-module samples
- [ ] FAQ.md with common issues
- [ ] TROUBLESHOOTING.md guide
- [ ] API documentation for `FaktExtension`

**Sample Structure**:
```
samples/multi-module/
├─ foundation/
│   ├─ build.gradle.kts (applies fakt-convention)
│   ├─ src/commonMain/kotlin/Logger.kt
│   └─ src/fakes/kotlin/FakeLoggerImpl.kt (generated)
├─ domain/
│   ├─ build.gradle.kts (dependency on :foundation)
│   └─ src/commonTest/.../DomainModelsTest.kt (uses fakeLogger())
└─ features/
    └─ ... (full chain validation)
```

---

## 📊 Success Metrics

### Quantitative

- [ ] **Zero modules created**: Git status shows ONLY `build.gradle.kts` modifications
- [ ] **19 tests passing**: All multi-module tests green
- [ ] **< 5s incremental build**: Compilation performance target
- [ ] **100% IDE navigation**: All "Go to Definition" commands work
- [ ] **Zero manual capability declarations**: Auto-wiring handles all cases

### Qualitative

- [ ] **User Experience**: Apply plugin with 1 line, fakes "just work"
- [ ] **Documentation Quality**: Can onboard new team member in < 1 hour
- [ ] **Error Messages**: Clear actionable guidance when misconfigured
- [ ] **Code Quality**: Passes all ktlint and detekt checks

---

## 🚧 Risk Mitigation

### Risk 1: Gradle Attribute Mismatch
**Symptom**: Variant resolution fails with "No matching variant found"
**Mitigation**:
- Comprehensive attribute logging during development
- Unit tests for each target's attribute matrix
- Reference table in TECHNICAL-REFERENCE.md

### Risk 2: IDE Indexing Failure
**Symptom**: IDE doesn't recognize fakes source set
**Mitigation**:
- Manual testing on IntelliJ IDEA and Android Studio
- Explicit `kotlin.srcDir()` registration
- Reimport project after applying convention

### Risk 3: Circular Dependency in Auto-Wiring
**Symptom**: Gradle fails with "Circular dependency detected"
**Mitigation**:
- Use `afterEvaluate` carefully
- Track visited projects to prevent loops
- Provide opt-out via `autoWireDependencies.set(false)`

### Risk 4: Build Performance Regression
**Symptom**: Compilation slower than dedicated modules
**Mitigation**:
- Benchmark against baseline (current multi-module tests)
- Optimize configuration cache usage
- Profile with Gradle build scans

---

## 📈 Post-Implementation

### Week 4+: Iteration and Feedback

**Tasks**:
1. Gather user feedback from early adopters
2. Performance profiling and optimization
3. Edge case handling (circular deps, complex hierarchies)
4. Publishing support (Maven Central, etc.)

### Future Enhancements

- [ ] Support for Android-specific test fixtures integration
- [ ] Gradle plugin portal publication
- [ ] IntelliJ IDEA plugin for better navigation
- [ ] Migration tool from dedicated modules → custom source sets

---

## 🎯 Definition of Done

**Feature is complete when**:

1. ✅ Convention plugin applied with `id("fakt-convention")`
2. ✅ Fakes generate to `src/fakes/kotlin/`
3. ✅ Cross-module tests pass (foundation → domain → features → app)
4. ✅ IDE navigation works (Go to Definition, completion, debugging)
5. ✅ Auto-wiring enabled by default, opt-out available
6. ✅ Documentation complete and reviewed
7. ✅ Sample project demonstrates full workflow
8. ✅ Performance meets < 5s incremental build target

---

## 📚 References

- **Architecture Decision**: See ARCHITECTURE-DECISION.md
- **Technical Deep Dive**: See TECHNICAL-REFERENCE.md
- **Convention Plugin Blueprint**: See CONVENTION-PLUGIN-BLUEPRINT.md
- **Comparison Matrix**: See COMPARISON-MATRIX.md

---

**Next Steps**: Begin Week 1, Day 1 - Create convention plugin structure.
