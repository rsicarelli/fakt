# /execute-roadmap

> **Purpose**: Execute Fakt extended roadmap with TDD RED-GREEN cycle
> **Usage**: `/execute-roadmap [phase] [feature]`
> **Scope**: Analyzes current progress, updates planning, creates detailed todo list with TDD

## 🎯 Command Overview

This command:
1. **Analyzes current state** - Which phase and feature are we on?
2. **Reviews progress** - What's done, what's pending?
3. **Creates TDD todo list** - RED → GREEN cycle for next steps
4. **Updates CHANGELOG** - Track daily progress per phase
5. **Updates current-status.md** - Overall project status
6. **Validates alignment** - Metro patterns, testing standards

## 📋 Command Arguments

```bash
# No args: Detect current phase and feature automatically
/execute-roadmap

# Specific phase:
/execute-roadmap phase1           # Start/resume Phase 1
/execute-roadmap phase2           # Start/resume Phase 2
/execute-roadmap phase3           # Start/resume Phase 3

# Specific feature within phase:
/execute-roadmap phase1 final-classes
/execute-roadmap phase1 singleton-objects
/execute-roadmap phase1 top-level-functions
/execute-roadmap phase2 data-classes
/execute-roadmap phase2 sealed-hierarchies
/execute-roadmap phase2 flow-producers
/execute-roadmap phase3 kmp

# Force fresh start on a feature:
/execute-roadmap phase1 singleton-objects --reset
```

## 🔍 What This Command Does

### Step 1: Analyze Current State

**Check these files for progress indicators**:

```kotlin
// PHASE 1: Performance Dominance

// 1.1 Final Classes
compiler/src/main/kotlin/.../analysis/InterfaceAnalyzer.kt
// If has: fun IrClass.isFakableClass() → Feature started
compiler/src/main/kotlin/.../codegen/ImplementationGenerator.kt
// If has: generateClassFake() → Feature in progress
compiler/src/test/kotlin/.../FinalClassFakeGenerationTest.kt
// Count tests to determine completion

// 1.2 Singleton Objects (CRITICAL - 1000x performance win!)
compiler/src/main/kotlin/.../ir/SingletonCallSiteDetector.kt
// If exists → Feature started
compiler/src/main/kotlin/.../ir/SingletonCallSiteReplacer.kt
// If exists → Call-site replacement implemented
compiler/src/test/kotlin/.../SingletonObjectFakeGenerationTest.kt
// Count tests, check performance benchmarks

// 1.3 Top-level Functions
compiler/src/main/kotlin/.../ir/TopLevelFunctionCallDetector.kt
// If exists → Feature started
compiler/src/main/kotlin/.../ir/TopLevelCallSiteReplacer.kt
// If exists → Call-site replacement implemented

// PHASE 2: Idiomatic Kotlin

// 2.1 Data Class Builders
compiler/src/main/kotlin/.../codegen/DataClassBuilderGenerator.kt
// If exists → Feature started
compiler/src/main/kotlin/.../codegen/SmartDefaultGenerator.kt
// If exists → Smart defaults implemented

// 2.2 Sealed Hierarchies
compiler/src/main/kotlin/.../codegen/SealedHierarchyHelperGenerator.kt
// If exists → Feature started

// 2.3 Flow Producers
compiler/src/main/kotlin/.../codegen/FlowProducerGenerator.kt
// If exists → Feature started

// PHASE 3: KMP Dominance

// 3.1 KMP Support
compiler/src/main/kotlin/.../kmp/KmpSourceSetDetector.kt
// If exists → KMP support started
compiler/src/main/kotlin/.../kmp/PlatformAwareIrGenerator.kt
// If exists → Multi-platform generation implemented
```

**Read phase-specific CHANGELOGs**:
```
.claude/docs/implementation/phase1-performance-dominance/CHANGELOG.md
.claude/docs/implementation/phase2-idiomatic-kotlin/CHANGELOG.md
.claude/docs/implementation/phase3-kmp-dominance/CHANGELOG.md
```

**Read overall status**:
```
.claude/docs/implementation/current-status.md
```

**Determine current phase and feature**:
- Phase 1 if: Any Phase 1 feature incomplete
  - Feature: final-classes if no IrClass.isFakableClass()
  - Feature: singleton-objects if final-classes done but no SingletonCallSiteDetector
  - Feature: top-level-functions if singletons done but no TopLevelCallDetector
- Phase 2 if: Phase 1 complete but Phase 2 features incomplete
- Phase 3 if: Phases 1-2 complete

### Step 2: Create TDD Todo List

**CRITICAL**: Every task MUST follow RED-GREEN cycle:

```
Task Pattern:
1. ❌ RED: Write failing test (GIVEN-WHEN-THEN)
2. ✅ GREEN: Implement minimum code to pass
3. 🔄 REFACTOR: Clean up (optional)
4. ✅ VERIFY: Run all tests
5. 📝 COMMIT: Proper commit message
```

**Example Todo Structure for Phase 1 - Singleton Objects**:

```markdown
## Current Phase: Phase 1 - Performance Dominance
## Current Feature: Singleton Objects (1000x+ Performance Win!)

### Week 1-2: Fake Generation

#### Task 1.1: Detect Singleton Objects
##### Subtask 1.1.1: RED - Write failing test for singleton detection
- [ ] Create SingletonObjectFakeGenerationTest.kt
- [ ] Write test: `GIVEN singleton object WHEN analyzing THEN should detect as fakable`
- [ ] **VERIFY**: Test fails (RED) ❌ - No detection logic exists
- [ ] **COMMIT**: "test: add failing test for singleton object detection"

##### Subtask 1.1.2: GREEN - Implement singleton detection
- [ ] Extend InterfaceAnalyzer.kt
- [ ] Add fun IrClass.isFakableObject(): Boolean
- [ ] Add AnalyzedType.SingletonObject data class
- [ ] **VERIFY**: Test passes (GREEN) ✅
- [ ] **COMMIT**: "feat: add singleton object detection in analyzer"
- [ ] **RUN ALL TESTS**: Ensure no regressions

##### Subtask 1.1.3: RED - Write failing test for companion object detection
- [ ] Add test: `GIVEN companion object WHEN analyzing THEN should detect`
- [ ] **VERIFY**: Test fails (RED) ❌
- [ ] **COMMIT**: "test: add failing test for companion object detection"

##### Subtask 1.1.4: GREEN - Implement companion detection
- [ ] Update isFakableObject() for companion objects
- [ ] **VERIFY**: Test passes (GREEN) ✅
- [ ] **COMMIT**: "feat: add companion object detection support"

#### Task 1.2: Generate Fake Class (Not Singleton!)
##### Subtask 1.2.1: RED - Write failing test for fake generation
- [ ] Add test: `GIVEN singleton object WHEN generating fake THEN should create regular class`
- [ ] **VERIFY**: Test fails (RED) ❌
- [ ] **COMMIT**: "test: add failing test for singleton fake generation"

##### Subtask 1.2.2: GREEN - Generate fake class
- [ ] Create generateSingletonFake() in ImplementationGenerator
- [ ] Generate regular class (not object!) with behavior properties
- [ ] **VERIFY**: Test passes (GREEN) ✅
- [ ] **COMMIT**: "feat: generate fake class for singleton objects"
- [ ] **RUN ALL TESTS**: Verify compilation

#### Task 1.3: Call-site Detection
##### Subtask 1.3.1: RED - Write failing test for call detection
- [ ] Create SingletonCallSiteDetectorTest.kt
- [ ] Add test: `GIVEN test code calling singleton WHEN detecting THEN should find calls`
- [ ] **VERIFY**: Test fails (RED) ❌
- [ ] **COMMIT**: "test: add failing test for singleton call-site detection"

##### Subtask 1.3.2: GREEN - Implement call detection
- [ ] Create SingletonCallSiteDetector.kt
- [ ] Implement visitCall() override
- [ ] Detect IrCall to singleton objects
- [ ] **VERIFY**: Test passes (GREEN) ✅
- [ ] **COMMIT**: "feat: implement singleton call-site detector"

[... Continue with Tasks 1.4-1.6 for call-site replacement ...]

### Week 5-6: Performance Benchmarks (CRITICAL!)

#### Task 6.1: Performance Validation
##### Subtask 6.1.1: Create benchmark test
- [ ] Create SingletonPerformanceBenchmarkTest.kt
- [ ] Test: Compare Fakt vs mockkObject (1000 iterations)
- [ ] **TARGET**: 1000x+ speedup
- [ ] **VERIFY**: Benchmark runs successfully

##### Subtask 6.1.2: Analyze results
- [ ] Run benchmark 5 times, record results
- [ ] Calculate average speedup
- [ ] Document in CHANGELOG
- [ ] **ACCEPTANCE**: Speedup >= 1000x ✅
```

### Step 3: Update Phase CHANGELOG

**Add daily entry to phase-specific CHANGELOG**:

```markdown
# Phase 1: Performance Dominance - CHANGELOG

## [DATE] - Singleton Objects - Week 1 Day 2

### What I Did
- ❌ RED: Wrote failing test for singleton detection
- ✅ GREEN: Implemented `IrClass.isFakableObject()` in InterfaceAnalyzer
- ❌ RED: Wrote failing test for companion object detection
- ✅ GREEN: Extended detection to handle companion objects

### Tests
- **Total**: 4 passing, 0 failing
- **New Tests**:
  - `GIVEN singleton object WHEN analyzing THEN should detect as fakable` ✅
  - `GIVEN companion object WHEN analyzing THEN should detect` ✅

### Code Changes
- Modified: `compiler/.../analysis/InterfaceAnalyzer.kt`
- Added: `AnalyzedType.SingletonObject` data class
- Tests: `SingletonObjectFakeGenerationTest.kt` (+2 tests)

### Blockers
- None

### Next Steps
- Tomorrow: Task 1.2 - Generate fake class (not singleton)
- Focus: Ensure generated code compiles correctly

### Performance Notes
- N/A (benchmarks in Week 5)

### Time Spent
- 3 hours

### Commits
- `test: add failing test for singleton object detection`
- `feat: add singleton object detection in analyzer`
- `test: add failing test for companion object detection`
- `feat: add companion object detection support`
```

### Step 4: Update Overall Status (current-status.md)

**Update project-wide status document**:

```markdown
## 📊 Status Atual do Projeto

### **✅ Funcionando (Production-Ready)**

#### **Core Infrastructure**
- ✅ Plugin discovery via Service Loader
- ✅ Two-phase FIR → IR compilation
[... existing items ...]

#### **Phase 1: Performance Dominance** (🔧 IN PROGRESS)
- ✅ Final class faking (compile-time generation)
- 🔧 **Singleton object faking** (ACTIVE - Week 1/6)
  - ✅ Detection logic complete
  - ⏳ Fake generation in progress
  - ⏳ Call-site replacement pending
  - ⏳ Performance benchmarks pending
- ⏳ Top-level function faking (not started)

#### **Phase 2: Idiomatic Kotlin** (⏳ PLANNED)
- ⏳ Data class builders (not started)
- ⏳ Sealed hierarchy helpers (not started)
- ⏳ Flow producers (not started)

#### **Phase 3: KMP Dominance** (⏳ PLANNED)
- ⏳ commonTest support (not started)
```

### Step 5: Validate Standards

**Check compliance with MAP quality**:
- [ ] All tests follow GIVEN-WHEN-THEN naming
- [ ] Using @TestInstance(TestInstance.Lifecycle.PER_CLASS)
- [ ] No custom BDD frameworks (vanilla JUnit5 only)
- [ ] Each commit has either test or implementation (RED or GREEN)
- [ ] Code formatted (`./gradlew spotlessApply`)
- [ ] Metro alignment validated for critical decisions
- [ ] Performance benchmarks for Phase 1 features
- [ ] Documentation updated (README, APPROACH docs)

### Step 6: Present Resume Summary

**Output Format**:

```markdown
# 📊 Fakt Roadmap Execution Resume

## Current Status
- **Phase**: Phase 1 - Performance Dominance
- **Feature**: Singleton Objects (Week 1/6)
- **Progress**: 15% of feature complete
- **Last Activity**: [Date] - Singleton detection implemented
- **Tests Passing**: 4/4 total (100%)
- **Performance**: Benchmarks pending (Week 5)

## What's Done ✅

### Phase 1: Performance Dominance
- ✅ Final Classes
  - Fake generation working
  - Tests passing (15/15)
  - Documentation complete
- 🔧 Singleton Objects (IN PROGRESS)
  - ✅ Detection logic (singleton + companion)
  - ⏳ Fake generation (in progress)
  - ⏳ Call-site replacement (pending)
  - ⏳ Performance benchmarks (pending Week 5)

### Overall Project
- ✅ Interfaces + Generics (Phase 0)
- ✅ SAM interfaces (80% - 2 bugs to fix)

## What's Next ⏳

### Immediate (Today/Tomorrow)
1. ❌ RED: Write failing test for fake class generation
2. ✅ GREEN: Implement generateSingletonFake()
3. ❌ RED: Write failing test for global fake instance
4. ✅ GREEN: Generate global instance property
5. 🔄 REFACTOR: Clean up generated code structure

### This Week
- Complete Task 1.2: Fake generation
- Start Task 1.3: Call-site detection
- Target: 30% feature completion by end of week

### This Month (Phase 1 Goal)
- Complete singleton objects (Week 6)
- Complete top-level functions (Week 4)
- Validate 1000x+ performance improvement
- Publish Phase 1 results

## Blockers 🚨
- None currently

## Critical Path Items
- ⚠️ **Performance Benchmarks** (Week 5) - Must validate 1000x claim!
- ⚠️ **Call-site Replacement** (Week 3-4) - Most complex part
- ⚠️ **Testing in Sample Project** (Week 6) - End-to-end validation

## Todo List Created
- **Total items**: 45
- **RED tasks**: 22 (write tests)
- **GREEN tasks**: 22 (implement)
- **REFACTOR tasks**: 1
- **Current task**: 1.2.1 (RED - fake generation test)

## Quick Commands

```bash
# Run current feature tests
./gradlew :compiler:test --tests "*SingletonObject*"

# Check current phase guide
cat .claude/docs/implementation/phase1-performance-dominance/README.md

# Read feature approach
cat .claude/docs/implementation/phase1-performance-dominance/singletonObjects/APPROACH.md

# Update phase CHANGELOG
vim .claude/docs/implementation/phase1-performance-dominance/CHANGELOG.md

# Format code
./gradlew spotlessApply

# Rebuild and test sample
make quick-test
```

## Research References
- **Performance Data**: [Benchmarking MockK](https://medium.com/@_kevinb/benchmarking-mockk-avoid-these-patterns-for-fast-unit-tests-220fc225da55)
- **KMP Analysis**: [KSP vs Compiler Plugins](https://medium.com/@mhristev/mocking-in-kotlin-multiplatform-ksp-vs-compiler-plugins-4424751b83d7)
- **Community Validation**: [Now in Android Testing Strategy](https://github.com/android/nowinandroid/wiki/Testing-strategy-and-how-to-test)
```

---

## 🔧 Implementation Logic

### Phase Detection Algorithm

```kotlin
enum class RoadmapPhase {
    PHASE1_FINAL_CLASSES,
    PHASE1_SINGLETON_OBJECTS,
    PHASE1_TOP_LEVEL_FUNCTIONS,
    PHASE2_DATA_CLASSES,
    PHASE2_SEALED_HIERARCHIES,
    PHASE2_FLOW_PRODUCERS,
    PHASE2_ABSTRACT_CLASSES,
    PHASE3_KMP,
    COMPLETE
}

fun detectCurrentPhase(): RoadmapPhase {
    // Check Phase 1 features
    val finalClassesComplete = fileExists("compiler/.../analysis/InterfaceAnalyzer.kt") &&
        fileContains("InterfaceAnalyzer.kt", "fun IrClass.isFakableClass()") &&
        testsPassing("FinalClassFakeGenerationTest", minPassRate = 0.95)

    if (!finalClassesComplete) {
        return RoadmapPhase.PHASE1_FINAL_CLASSES
    }

    val singletonObjectsComplete = fileExists("compiler/.../ir/SingletonCallSiteDetector.kt") &&
        fileExists("compiler/.../ir/SingletonCallSiteReplacer.kt") &&
        testsPassing("SingletonObjectFakeGenerationTest", minPassRate = 0.95) &&
        benchmarkPassing("SingletonPerformanceBenchmark", minSpeedup = 1000.0)

    if (!singletonObjectsComplete) {
        return RoadmapPhase.PHASE1_SINGLETON_OBJECTS
    }

    val topLevelComplete = fileExists("compiler/.../ir/TopLevelCallSiteDetector.kt") &&
        fileExists("compiler/.../ir/TopLevelCallSiteReplacer.kt") &&
        testsPassing("TopLevelFunctionFakeGenerationTest", minPassRate = 0.95) &&
        benchmarkPassing("TopLevelPerformanceBenchmark", minSpeedup = 100.0)

    if (!topLevelComplete) {
        return RoadmapPhase.PHASE1_TOP_LEVEL_FUNCTIONS
    }

    // Check Phase 2 features
    val dataClassesComplete = fileExists("compiler/.../codegen/DataClassBuilderGenerator.kt") &&
        fileExists("compiler/.../codegen/SmartDefaultGenerator.kt") &&
        testsPassing("DataClassBuilderGenerationTest", minPassRate = 0.95)

    if (!dataClassesComplete) {
        return RoadmapPhase.PHASE2_DATA_CLASSES
    }

    val sealedHierarchiesComplete = fileExists("compiler/.../codegen/SealedHierarchyHelperGenerator.kt") &&
        testsPassing("SealedHierarchyHelperGenerationTest", minPassRate = 0.95)

    if (!sealedHierarchiesComplete) {
        return RoadmapPhase.PHASE2_SEALED_HIERARCHIES
    }

    val flowProducersComplete = fileExists("compiler/.../codegen/FlowProducerGenerator.kt") &&
        testsPassing("FlowProducerGenerationTest", minPassRate = 0.95)

    if (!flowProducersComplete) {
        return RoadmapPhase.PHASE2_FLOW_PRODUCERS
    }

    val abstractClassesComplete = testsPassing("AbstractClassFakeGenerationTest", minPassRate = 0.90)

    if (!abstractClassesComplete) {
        return RoadmapPhase.PHASE2_ABSTRACT_CLASSES
    }

    // Check Phase 3
    val kmpComplete = fileExists("compiler/.../kmp/KmpSourceSetDetector.kt") &&
        fileExists("compiler/.../kmp/PlatformAwareIrGenerator.kt") &&
        testsPassing("KmpFakeGenerationTest", minPassRate = 0.95) &&
        testsPassingOnAllPlatforms(platforms = listOf("jvm", "ios", "js", "wasm"))

    if (!kmpComplete) {
        return RoadmapPhase.PHASE3_KMP
    }

    return RoadmapPhase.COMPLETE
}
```

### TDD Todo Generator (Feature-Specific)

```kotlin
fun generateTDDTodos(phase: RoadmapPhase, lastCompletedTask: String?): List<Todo> {
    val todos = mutableListOf<Todo>()

    when (phase) {
        RoadmapPhase.PHASE1_SINGLETON_OBJECTS -> {
            // Week 1-2: Fake Generation
            todos.add(redTask(
                id = "1.1.1",
                description = "Write failing test: singleton detection",
                file = "SingletonObjectFakeGenerationTest.kt",
                testName = "GIVEN singleton object WHEN analyzing THEN should detect as fakable"
            ))
            todos.add(greenTask(
                id = "1.1.2",
                description = "Implement IrClass.isFakableObject()",
                file = "InterfaceAnalyzer.kt"
            ))
            // ... 40+ more tasks following RED-GREEN pattern
        }

        RoadmapPhase.PHASE2_DATA_CLASSES -> {
            todos.add(redTask(
                id = "2.1.1",
                description = "Write failing test: detect data class",
                file = "DataClassBuilderGenerationTest.kt",
                testName = "GIVEN data class WHEN analyzing THEN should generate builder"
            ))
            todos.add(greenTask(
                id = "2.1.2",
                description = "Implement data class detection",
                file = "InterfaceAnalyzer.kt"
            ))
            // ... continue pattern
        }

        // ... other phases
    }

    return todos
}

fun redTask(id: String, description: String, file: String, testName: String): Todo {
    return Todo(
        id = id,
        type = TodoType.RED,
        description = description,
        file = file,
        testName = testName,
        acceptance = "Test fails (RED) ❌"
    )
}

fun greenTask(id: String, description: String, file: String): Todo {
    return Todo(
        id = id,
        type = TodoType.GREEN,
        description = description,
        file = file,
        acceptance = "Test passes (GREEN) ✅"
    )
}
```

---

## 📊 Success Metrics

### Per Session
- [ ] At least 1 RED-GREEN cycle completed
- [ ] Phase CHANGELOG updated with daily entry
- [ ] All tests pass before ending session
- [ ] Code formatted (`spotlessApply`)
- [ ] Feature progress documented

### Per Feature
- [ ] All feature tasks in todo list complete
- [ ] Integration test for feature passing
- [ ] Performance benchmarks passing (Phase 1 only)
- [ ] Phase CHANGELOG has feature completion entry
- [ ] current-status.md updated

### Per Phase
- [ ] All phase features complete
- [ ] Phase README reflects completion
- [ ] Overall performance targets met
- [ ] Ready for next phase
- [ ] Announcement/blog post draft prepared

### Overall
- [ ] Phase 1: 10x+ performance improvement validated
- [ ] Phase 2: 50%+ boilerplate reduction measured
- [ ] Phase 3: All platforms (JVM, iOS, JS, Wasm) working
- [ ] Community feedback positive
- [ ] Documentation complete

---

## 🎯 TDD Best Practices (Roadmap-Specific)

### RED Phase - Performance Features (Phase 1)

```kotlin
// Example: Singleton object fake
@Test
fun `GIVEN singleton object with methods WHEN generating fake THEN should create regular class`() = runTest {
    // Given
    val singletonObject = """
        @Fake
        object AnalyticsService {
            fun trackEvent(name: String) { }
        }
    """.trimIndent()

    // When
    val result = compilationTestHelper.compile(singletonObject)

    // Then - Expected to FAIL (no implementation yet)
    assertTrue(result.hasClass("FakeAnalyticsService"))
    assertFalse(result.getClass("FakeAnalyticsService").isObject) // Should be class, not object
    assertTrue(result.compiles())
}

// VERIFY: Run test → Should FAIL ❌ (no fake generation logic)
// COMMIT: "test: add failing test for singleton fake generation"
```

### GREEN Phase - Implement Minimum

```kotlin
// In ImplementationGenerator.kt
fun generateSingletonFake(objectType: AnalyzedType.SingletonObject): IrClass {
    // Minimum implementation to pass the test
    val fakeClass = irFactory.buildClass {
        name = Name.identifier("Fake${objectType.declaration.name}")
        kind = ClassKind.CLASS // NOT OBJECT! Regular class
        modality = Modality.OPEN
    }

    // Add behavior properties and methods
    generateBehaviorProperties(fakeClass, objectType)
    generateMethods(fakeClass, objectType)

    return fakeClass
}

// VERIFY: Run test → Should PASS ✅
// COMMIT: "feat: generate fake class for singleton objects"
```

### Performance Benchmark Tests (Critical for Phase 1!)

```kotlin
@Test
fun `GIVEN singleton object fake WHEN compared to mockkObject THEN should be 1000x faster`() {
    val iterations = 1000

    // Baseline: mockkObject
    val mockkTime = measureTime {
        repeat(iterations) {
            mockkObject(AnalyticsService)
            every { AnalyticsService.trackEvent(any()) } just Runs
            AnalyticsService.trackEvent("test")
            unmockkObject(AnalyticsService)
        }
    }

    // Fakt: compile-time fake
    val faktTime = measureTime {
        repeat(iterations) {
            configureFakeAnalyticsService {
                trackEvent { _ -> /* no-op */ }
            }
            fakeAnalyticsServiceInstance.trackEvent("test")
        }
    }

    val speedup = mockkTime.inWholeMilliseconds / faktTime.inWholeMilliseconds

    // CRITICAL: Must meet the 1000x claim!
    assertTrue(
        speedup >= 1000,
        "Expected 1000x+ speedup, got ${speedup}x. mockkTime=${mockkTime}, faktTime=${faktTime}"
    )

    // Document in CHANGELOG
    println("Performance: ${speedup}x speedup achieved!")
}
```

---

## 🚨 Common Issues & Solutions

### Issue: "Don't know which feature to work on"
**Solution**: Run `/execute-roadmap` - it will detect and tell you!

### Issue: "Should I work on Phase 2 while Phase 1 is incomplete?"
**Solution**: NO! Complete phases sequentially. Each builds on the previous.

### Issue: "Performance benchmark not meeting 1000x target"
**Solution**:
1. Check measurement methodology (warmup, iterations)
2. Profile the code to find bottlenecks
3. Validate mockkObject baseline is correct
4. Document actual speedup honestly (don't fake numbers!)

### Issue: "Feature is working but tests incomplete"
**Solution**: Feature is NOT complete until tests pass. TDD is mandatory!

### Issue: "How do I know when a feature is done?"
**Solution**: Check completion criteria:
- [ ] All RED-GREEN cycles complete
- [ ] Tests passing (95%+ for P0/P1 features)
- [ ] Performance benchmarks passing (Phase 1)
- [ ] CHANGELOG updated
- [ ] current-status.md updated
- [ ] Integration test in sample project passing

---

## 📝 Example Session - Phase 1, Singleton Objects

```bash
# Start session
/execute-roadmap

# Output:
📊 Fakt Roadmap Execution Resume

Current Status:
- Phase: Phase 1 - Performance Dominance
- Feature: Singleton Objects (Week 1/6)
- Progress: 0% complete (just starting)
- Tests Passing: 0/0
- Next Task: 1.1.1 RED - Write failing test for singleton detection

Todo List Created (45 items):
Week 1-2: Fake Generation
1. ❌ RED: Test singleton detection
2. ✅ GREEN: Implement detection
3. ❌ RED: Test companion detection
4. ✅ GREEN: Implement companion support
[...]

Week 5-6: Performance Benchmarks (CRITICAL!)
40. ❌ RED: Write performance benchmark test
41. ✅ GREEN: Achieve 1000x+ speedup
42. 📝 DOCUMENT: Record results in CHANGELOG

Quick Start:
```bash
# Read approach document
cat .claude/docs/implementation/phase1-performance-dominance/singletonObjects/APPROACH.md

# Create test file
touch compiler/src/test/kotlin/.../SingletonObjectFakeGenerationTest.kt

# Follow TDD: RED → GREEN → REFACTOR
```

# User follows RED-GREEN cycle for Week 1...

# End of Day 1 - update CHANGELOG
vim .claude/docs/implementation/phase1-performance-dominance/CHANGELOG.md

# End of Week 1 - review progress
/execute-roadmap  # Shows 15% complete, next tasks

# Continue until feature complete (Week 6)
```

---

## 🔗 Related Documentation

### Strategic
- [Main Roadmap](../docs/implementation/roadmap.md) - Overall strategy and phases
- [Current Status](../docs/implementation/current-status.md) - Project-wide progress

### Phase-Specific
- [Phase 1: Performance Dominance](../docs/implementation/phase1-performance-dominance/README.md)
  - [Final Classes APPROACH](../docs/implementation/phase1-performance-dominance/finalClasses/APPROACH.md)
  - [Singleton Objects APPROACH](../docs/implementation/phase1-performance-dominance/singletonObjects/APPROACH.md)
  - [Top-level Functions APPROACH](../docs/implementation/phase1-performance-dominance/topLevelFunctions/APPROACH.md)

- [Phase 2: Idiomatic Kotlin](../docs/implementation/phase2-idiomatic-kotlin/README.md)
  - [Data Classes APPROACH](../docs/implementation/phase2-idiomatic-kotlin/dataClasses/APPROACH.md)

- [Phase 3: KMP Dominance](../docs/implementation/phase3-kmp-dominance/README.md)

### Research Foundation
- [Kotlin Test Fake Research Roadmap](Research document from Gemini Deep Research)
- Performance benchmarks, community feedback, competitive analysis

---

## 🎯 Critical Success Factors

### Phase 1 (Performance)
- ⚠️ **1000x+ speedup for singleton objects** - MUST be validated!
- ⚠️ **100x+ speedup for top-level functions** - Benchmark required
- ⚠️ **Call-site replacement working** - Most complex part

### Phase 2 (DX)
- ⚠️ **50%+ boilerplate reduction** - Measure lines of code saved
- ⚠️ **Smart defaults are actually smart** - User testing
- ⚠️ **Flow producers useful** - Integration with Turbine patterns

### Phase 3 (KMP)
- ⚠️ **All platforms working** - JVM, iOS, JS, Wasm
- ⚠️ **Stability across Kotlin updates** - Test with multiple versions
- ⚠️ **Better than KSP tools** - Clear competitive advantage

---

**Remember**: RED → GREEN → REFACTOR. Every. Single. Time. 🔴➡️🟢➡️🔄

**Remember**: We're not building MVPs, we're building MAPs (Minimum Awesome Products). Every feature must be production-quality! 🚀
