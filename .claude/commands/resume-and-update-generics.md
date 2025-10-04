# /resume-and-update-generics

> **Purpose**: Resume and continue generic type implementation with TDD RED-GREEN cycle
> **Usage**: `/resume-and-update-generics [phase]`
> **Scope**: Analyzes current progress, updates planning, creates detailed todo list with TDD

## 🎯 Command Overview

This command:
1. **Analyzes current state** - Where are we in the implementation?
2. **Reviews progress** - What's done, what's pending?
3. **Creates TDD todo list** - RED → GREEN cycle for next steps
4. **Updates CHANGELOG** - Track daily progress
5. **Validates alignment** - Metro patterns, testing standards

## 📋 Command Arguments

```bash
# No args: Detect current phase automatically
/resume-and-update-generics

# Specific phase:
/resume-and-update-generics phase1
/resume-and-update-generics phase2
/resume-and-update-generics phase3

# Force fresh start:
/resume-and-update-generics --reset
```

## 🔍 What This Command Does

### Step 1: Analyze Current State

**Check these files for progress indicators**:
```kotlin
// 1. Generic filter still present?
compiler/src/main/kotlin/.../ir/UnifiedFaktIrGenerationExtension.kt:189
// If line 189 has: "interfaceAnalyzer.checkGenericSupport" → Phase 1 NOT started

// 2. GenericIrSubstitutor exists?
compiler/src/main/kotlin/.../ir/GenericIrSubstitutor.kt
// If missing → Phase 1 Task 1.1 pending

// 3. Tests exist?
compiler/src/test/kotlin/.../ir/GenericIrSubstitutorTest.kt
compiler/src/test/kotlin/.../GenericFakeGenerationTest.kt
// Count passing tests to determine progress

// 4. Generated code structure
samples/single-module/build/generated/fakt/test/kotlin/
// Check for: class Fake<T> vs class Fake
```

**Read CHANGELOG for manual updates**:
```
ktfake/.claude/docs/implementation/generics/CHANGELOG.md
```

**Determine current phase**:
- Phase 1 if: GenericIrSubstitutor not created
- Phase 2 if: GenericIrSubstitutor exists but generators not updated
- Phase 3 if: Generators updated but tests incomplete

### Step 2: Create TDD Todo List

**CRITICAL**: Every task MUST follow RED-GREEN cycle:

```
Task Pattern:
1. ❌ RED: Write failing test (GIVEN-WHEN-THEN)
2. ✅ GREEN: Implement minimum code to pass
3. 🔄 REFACTOR: Clean up (optional)
4. ✅ VERIFY: Run all tests
```

**Example Todo Structure**:
```markdown
## Current Phase: Phase 1 - Core Infrastructure

### Task 1.1: Create GenericIrSubstitutor.kt

#### Subtask 1.1.1: RED - Write failing test for substitution map
- [ ] Create GenericIrSubstitutorTest.kt
- [ ] Write test: `GIVEN interface with type param WHEN creating map THEN should map correctly`
- [ ] **VERIFY**: Test fails (RED) ❌
- [ ] Commit: "test: add failing test for GenericIrSubstitutor substitution map"

#### Subtask 1.1.2: GREEN - Implement substitution map
- [ ] Create GenericIrSubstitutor.kt
- [ ] Implement createSubstitutionMap()
- [ ] **VERIFY**: Test passes (GREEN) ✅
- [ ] Commit: "feat: implement GenericIrSubstitutor substitution map"

#### Subtask 1.1.3: RED - Write failing test for class-level substitutor
- [ ] Add test: `GIVEN substitution map WHEN creating substitutor THEN should substitute types`
- [ ] **VERIFY**: Test fails (RED) ❌

#### Subtask 1.1.4: GREEN - Implement class-level substitutor
- [ ] Implement createClassLevelSubstitutor()
- [ ] **VERIFY**: Test passes (GREEN) ✅
- [ ] Commit: "feat: add class-level substitutor creation"

[... continue pattern for all subtasks ...]
```

### Step 3: Update CHANGELOG

**Add daily entry**:
```markdown
### [DATE] - Phase X Day Y

**What I Did**:
- ❌ RED: Wrote failing test for [feature]
- ✅ GREEN: Implemented [feature], test passing
- 🔄 REFACTOR: Cleaned up [code]

**Tests**:
- Total: X passing, Y failing
- New: [test names]

**Blockers**:
- [Any issues]

**Next Steps**:
- Tomorrow: [Next task from todo list]

**Time Spent**: [X hours]
```

### Step 4: Validate Standards

**Check compliance**:
- [ ] All tests follow GIVEN-WHEN-THEN naming
- [ ] Using @TestInstance(TestInstance.Lifecycle.PER_CLASS)
- [ ] No custom BDD frameworks (vanilla JUnit5 only)
- [ ] Each commit has either test or implementation (RED or GREEN)
- [ ] Code formatted (spotlessApply)

### Step 5: Present Resume Summary

**Output Format**:
```markdown
# 📊 Generic Implementation Resume

## Current Status
- **Phase**: Phase X - [Name]
- **Progress**: X% complete
- **Last Activity**: [Date] - [Last task]
- **Tests Passing**: X/Y total

## What's Done ✅
- [List of completed tasks]

## What's Next ⏳
- [Next 3-5 tasks in todo list]

## Blockers 🚨
- [Any blockers from CHANGELOG]

## Todo List Created
- Total items: X
- RED tasks: Y (write tests)
- GREEN tasks: Z (implement)

## Quick Commands
```bash
# Run tests
./gradlew :compiler:test --tests "*Generic*"

# Check current phase guide
cat .claude/docs/implementation/generics/phaseX-*.md

# Update CHANGELOG
# Add your daily entry to CHANGELOG.md
```
```

---

## 🔧 Implementation Logic

### Phase Detection Algorithm

```kotlin
fun detectCurrentPhase(): Phase {
    // Check Phase 1 completion
    val genericIrSubstitutorExists = fileExists("GenericIrSubstitutor.kt")
    val genericFilterRemoved = !fileContains(
        "UnifiedFaktIrGenerationExtension.kt",
        "interfaceAnalyzer.checkGenericSupport"
    )
    val typeResolverEnhanced = fileContains(
        "TypeResolver.kt",
        "irTypeToKotlinStringWithSubstitution"
    )

    if (!genericIrSubstitutorExists) {
        return Phase.PHASE1_TASK1 // Start from beginning
    }

    if (!genericFilterRemoved || !typeResolverEnhanced) {
        return Phase.PHASE1_IN_PROGRESS // Continue Phase 1
    }

    // Check Phase 2 completion
    val implementationGenUpdated = fileContains(
        "ImplementationGenerator.kt",
        "class \$fakeClassName\$typeParams : \$interfaceName\$typeParams"
    )
    val factoryGenUpdated = fileContains(
        "FactoryGenerator.kt",
        "inline fun <reified"
    )
    val configDslGenUpdated = fileContains(
        "ConfigurationDslGenerator.kt",
        "class.*Config<.*>"
    )

    if (!implementationGenUpdated || !factoryGenUpdated || !configDslGenUpdated) {
        return Phase.PHASE2_IN_PROGRESS
    }

    // Phase 3 or complete
    val testCount = countTests("GenericFakeGenerationTest.kt")
    val passingRate = calculatePassRate()

    if (passingRate < 0.95) {
        return Phase.PHASE3_IN_PROGRESS
    }

    return Phase.COMPLETE
}
```

### TDD Todo Generator

```kotlin
fun generateTDDTodos(currentPhase: Phase, lastCompletedTask: String?): List<Todo> {
    val todos = mutableListOf<Todo>()

    when (currentPhase) {
        Phase.PHASE1_TASK1 -> {
            // Task 1.1: GenericIrSubstitutor
            todos.add(Todo(
                id = "1.1.1",
                type = TodoType.RED,
                description = "Write failing test: substitution map creation",
                file = "GenericIrSubstitutorTest.kt",
                testName = "GIVEN interface with type param WHEN creating map THEN correct",
                acceptance = "Test fails (RED) - createSubstitutionMap not implemented"
            ))

            todos.add(Todo(
                id = "1.1.2",
                type = TodoType.GREEN,
                description = "Implement createSubstitutionMap()",
                file = "GenericIrSubstitutor.kt",
                acceptance = "Test passes (GREEN) - substitution map works"
            ))

            todos.add(Todo(
                id = "1.1.3",
                type = TodoType.RED,
                description = "Write failing test: class-level substitutor",
                file = "GenericIrSubstitutorTest.kt",
                testName = "GIVEN substitution map WHEN creating substitutor THEN substitutes",
                acceptance = "Test fails (RED)"
            ))

            todos.add(Todo(
                id = "1.1.4",
                type = TodoType.GREEN,
                description = "Implement createClassLevelSubstitutor()",
                file = "GenericIrSubstitutor.kt",
                acceptance = "Test passes (GREEN)"
            ))

            // ... continue pattern
        }

        Phase.PHASE2_IN_PROGRESS -> {
            // Similar TDD pattern for Phase 2 tasks
        }

        // ... other phases
    }

    return todos
}
```

---

## 📊 Success Metrics

### Per Session
- [ ] At least 1 RED-GREEN cycle completed
- [ ] CHANGELOG updated with daily entry
- [ ] All tests pass before ending session
- [ ] Code formatted (spotlessApply)

### Per Phase
- [ ] All phase tasks in todo list complete
- [ ] Integration test for phase passing
- [ ] CHANGELOG has phase completion entry
- [ ] Ready for next phase

### Overall
- [ ] P0 tests: 100% passing
- [ ] P1 tests: 95% passing
- [ ] P2 tests: 90% passing
- [ ] Performance: <10% overhead

---

## 🎯 TDD Best Practices

### RED Phase (Write Failing Test)

```kotlin
// ALWAYS start with this structure
@Test
fun `GIVEN [scenario] WHEN [action] THEN [expected]`() = runTest {
    // Given - Setup isolated instances
    val mockInterface = createMockInterface("Repository", listOf("T"))
    val substitutor = GenericIrSubstitutor(mockPluginContext)

    // When - Execute the action
    val result = substitutor.createSubstitutionMap(mockInterface, mockSuperType)

    // Then - Assert expected outcome
    assertEquals(1, result.size)
    assertNotNull(result.entries.first())
}

// VERIFY: Run test → Should FAIL ❌
// Commit: "test: add failing test for substitution map"
```

### GREEN Phase (Implement)

```kotlin
// Implement MINIMUM code to pass the test
class GenericIrSubstitutor(private val pluginContext: IrPluginContext) {

    fun createSubstitutionMap(
        originalInterface: IrClass,
        superType: IrSimpleType
    ): Map<IrTypeParameterSymbol, IrTypeArgument> {
        // Minimum implementation to pass test
        return originalInterface.typeParameters
            .zip(superType.arguments)
            .associate { (param, arg) -> param.symbol to arg }
    }
}

// VERIFY: Run test → Should PASS ✅
// Commit: "feat: implement substitution map creation"
```

### REFACTOR Phase (Optional)

```kotlin
// Clean up if needed, but tests must still pass
// Add documentation, improve naming, extract methods
// VERIFY: All tests still pass ✅
```

---

## 🚨 Common Issues & Solutions

### Issue: "Don't know where to start"
**Solution**: Run `/resume-and-update-generics` - it will tell you exactly!

### Issue: "Test is passing but shouldn't (no implementation yet)"
**Solution**: Test is not strict enough. Add more assertions.

### Issue: "Can't get test to fail (RED)"
**Solution**: Comment out the code being tested, test should fail.

### Issue: "Test passes but feature doesn't work in integration"
**Solution**: Missing integration test. Add multi-stage validation.

### Issue: "Forgot to update CHANGELOG"
**Solution**: Run command again, it will prompt to update.

---

## 📝 Example Session

```bash
# Start session
/resume-and-update-generics

# Output:
📊 Generic Implementation Resume

Current Status:
- Phase: Phase 1 - Core Infrastructure
- Progress: 0% complete (just starting)
- Tests Passing: 0/0
- Next Task: 1.1.1 RED - Write failing test for substitution map

Todo List Created (10 items):
1. ❌ RED: Write test - substitution map
2. ✅ GREEN: Implement - substitution map
3. ❌ RED: Write test - class-level substitutor
4. ✅ GREEN: Implement - class-level substitutor
[...]

Quick Start:
```bash
# Create test file
touch compiler/src/test/kotlin/.../GenericIrSubstitutorTest.kt

# Follow Phase 1 guide
cat .claude/docs/implementation/generics/phase1-core-infrastructure.md
```

# User follows RED-GREEN cycle...

# End of session - update CHANGELOG
# Next session: run command again to resume
```

---

## 🔗 Related Documentation

- [ROADMAP.md](../docs/implementation/generics/ROADMAP.md) - Overall strategy
- [QUICK-START.md](../docs/implementation/generics/QUICK-START.md) - Getting started guide
- [phase1-core-infrastructure.md](../docs/implementation/generics/phase1-core-infrastructure.md) - Phase 1 details
- [phase2-code-generation.md](../docs/implementation/generics/phase2-code-generation.md) - Phase 2 details
- [phase3-testing-integration.md](../docs/implementation/generics/phase3-testing-integration.md) - Phase 3 details
- [test-matrix.md](../docs/implementation/generics/test-matrix.md) - Test scenarios
- [CHANGELOG.md](../docs/implementation/generics/CHANGELOG.md) - Progress tracking

---

**Remember**: RED → GREEN → REFACTOR. Never skip RED! 🔴➡️🟢➡️🔄
