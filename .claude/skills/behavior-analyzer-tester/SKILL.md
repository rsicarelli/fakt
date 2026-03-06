---
name: behavior-analyzer-tester
description: Analyzes code behavior and generates comprehensive GIVEN-WHEN-THEN unit tests with vanilla JUnit5. Use when generating tests, analyzing behavior for test coverage, creating unit tests for Fakt components, or writing tests for new or modified code. Make sure to use this skill whenever new tests need to be written — it ensures BDD naming compliance, proper test isolation, and coverage of edge cases that are easy to overlook.
allowed-tools: Read, Write, Edit, Grep, Glob, Bash, TaskCreate, TaskUpdate
---

# Behavior Analyzer & Test Generator

Analyzes code to identify all behaviors and generates complete GIVEN-WHEN-THEN test coverage.

## Instructions

### 1. Identify Target File

**Extract file path from conversation:**
- "analyze UserService.kt", "generate tests for PaymentProcessor"
- If missing, ask: "Which file would you like me to analyze?"

### 2. Deep Behavior Analysis

**Read the target file and analyze:**

**2.1 Public API** — Find all public methods, properties, their signatures, parameters, return types.

**2.2 Edge Cases** — Nullable types, empty collections, boundary values.

**2.3 Error Handling** — Exceptions thrown, `require`/`check` validation, error conditions.

**2.4 State Management** — Mutable properties, state transitions, side effects.

**2.5 Async/Concurrency** — Suspend functions, coroutine scopes.

**Create behavior map:**
```markdown
## Behaviors Found in {FileName}

### Public API ({X} methods, {Y} properties):
1. `methodName(param: Type): ReturnType` - Description

### Edge Cases:
- Null handling: {scenarios}
- Empty collections: {scenarios}

### Error Scenarios:
- Throws XException when {condition}

### State Transitions:
- {initial} → {action} → {new state}
```

### 3. Check Existing Tests

```bash
# Find existing test file
Glob pattern="**/*{ClassName}Test.kt"

# If exists, read to identify coverage gaps
```

### 4. Generate Test Cases

**For each behavior, create a GIVEN-WHEN-THEN scenario:**

**Prioritize:**
- HIGH: Public API, critical paths, error handling
- MEDIUM: Edge cases, state transitions
- LOW: Additional validations

**Create tasks for tracking:**
```
TaskCreate: each test case as a separate task
```

### 5. Implement Tests

**Test file template:**
```kotlin
package {package_name}

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class {ClassName}Test {

    @Test
    fun `GIVEN valid input WHEN processing THEN returns expected result`() = runTest {
        // GIVEN
        val sut = ClassName()  // Isolated instance

        // WHEN
        val result = sut.process(input)

        // THEN
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN null input WHEN processing THEN throws IllegalArgumentException`() {
        // GIVEN
        val sut = ClassName()

        // WHEN & THEN
        assertFailsWith<IllegalArgumentException> {
            sut.process(null)
        }
    }
}
```

**For each test:**
1. Mark task as in_progress
2. Implement the test
3. Compile: `./gradlew compileTestKotlin`
4. Run: `./gradlew test --tests "{ClassName}Test.{testMethodName}"`
5. Mark task as completed when passing

### 6. Validate Coverage & Compliance

- [ ] All behaviors from analysis have corresponding tests
- [ ] GIVEN-WHEN-THEN naming (uppercase)
- [ ] `@TestInstance(PER_CLASS)` on class
- [ ] Vanilla assertions only (assertEquals, assertTrue, assertNotNull, assertFailsWith)
- [ ] Isolated instances per test (no shared state)
- [ ] `runTest` for suspend functions
- [ ] No mocks, no "should" naming
- [ ] No duplicate test scenarios

```bash
# Check for forbidden "should" pattern
Grep pattern='fun \`should' {test_file}

# Format
./gradlew spotlessApply

# Final run
./gradlew test --tests "{ClassName}Test"
```

### 7. Summary Report

```
BEHAVIOR ANALYSIS & TEST GENERATION COMPLETE

Target: {file_path}
Tests Generated: {count} (happy: {n}, edge: {n}, error: {n}, async: {n})
Coverage: {methods_covered}/{total} methods
All tests pass: yes/no
Compliance: GIVEN-WHEN-THEN ✅ | Vanilla assertions ✅ | No mocks ✅
```

## Related Skills

- **`bdd-test-runner`** — Run and validate generated tests
- **`compilation`** — Validate generated tests compile
