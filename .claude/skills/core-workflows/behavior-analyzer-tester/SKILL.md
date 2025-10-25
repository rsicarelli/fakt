---
name: behavior-analyzer-tester
description: Performs deep behavior analysis of code and generates comprehensive unit tests following GIVEN-WHEN-THEN patterns with vanilla JUnit5. Use when generating tests, analyzing behavior, improving test coverage, creating unit tests, or when user mentions "generate tests", "test coverage", "analyze behavior", "GIVEN-WHEN-THEN tests", "unit tests", or provides file path to analyze.
allowed-tools: [Read, Write, Edit, Grep, Glob, Bash, TodoWrite]
---

# Behavior Analyzer & Test Generator

Deep behavior analysis and comprehensive unit test generation following Fakt's ABSOLUTE TESTING STANDARD (GIVEN-WHEN-THEN with vanilla JUnit5).

## Core Mission

This Skill analyzes code to identify ALL behaviors (happy path, edge cases, error handling, state transitions) and generates complete test coverage following strict GIVEN-WHEN-THEN patterns without custom matchers or mocks.

## Instructions

### 1. Identify Target File

**Extract from conversation:**
- Look for file path in user's messages
- Patterns: "analyze UserService.kt", "generate tests for PaymentProcessor", "test coverage for kotlin/DataCache.kt"
- File extensions: `.kt`, `.java`

**If ambiguous or missing:**
```
Ask: "Which file would you like me to analyze and generate tests for? Please provide the file path."
```

**Validation:**
```bash
# Verify file exists
if [[ ! -f "{file_path}" ]]; then
    echo "❌ File not found: {file_path}"
    exit 1
fi
```

### 2. PHASE 1: Deep Behavior Analysis

**Read target file completely:**
```bash
Read {file_path}
```

**Analyze and categorize behaviors:**

**2.1 Public API Behaviors**
```bash
# Find all public methods/functions
Grep pattern="(fun|override fun|suspend fun)\s+\w+" {file_path} -n

# Find properties
Grep pattern="(val|var)\s+\w+:" {file_path} -n
```

**Document:**
- Method signatures
- Parameters and return types
- Suspend modifiers
- Property types and mutability

**2.2 Edge Cases & Boundaries**
```bash
# Find nullable types
Grep pattern="\w+\?" {file_path} -n

# Find collections (empty collection tests needed)
Grep pattern="List<|Set<|Map<" {file_path} -n

# Find boundary values
Grep pattern="(Int.MAX|Int.MIN|Long.MAX)" {file_path} -n
```

**Document:**
- Null handling scenarios
- Empty collection handling
- Boundary value constraints

**2.3 Error Handling**
```bash
# Find error handling patterns
Grep pattern="(throw|catch|try|require|check)" {file_path} -n

# Find exceptions
Grep pattern="Exception|Error" {file_path} -n
```

**Document:**
- What exceptions are thrown
- Error conditions
- Validation logic (require/check)

**2.4 State Management**
```bash
# Find mutable state
Grep pattern="(var|private var)\s+\w+" {file_path} -n
```

**Document:**
- Mutable properties
- State transitions
- Side effects

**2.5 Async/Concurrency**
```bash
# Find suspend functions
Grep pattern="suspend\s+fun" {file_path} -n

# Find coroutine scopes
Grep pattern="CoroutineScope|launch|async" {file_path} -n
```

**Document:**
- Suspend functions (need runTest)
- Coroutine usage
- Thread safety concerns

**Create behavior map:**
```markdown
## Behaviors Found in {FileName}

### Public API (X methods, Y properties):
1. `methodName(param: Type): ReturnType` - Description
2. `property: Type` - Purpose

### Edge Cases Identified:
- Null handling: {scenarios}
- Empty collections: {scenarios}
- Boundary values: {scenarios}

### Error Scenarios:
- Throws XException when {condition}
- Returns null when {condition}

### State Transitions:
- {initial state} → {action} → {new state}

### Dependencies:
- Requires: {external dependencies}
- Side effects: {logging, I/O, etc.}
```

### 3. PHASE 2: Test Case Generation

**For each behavior, generate GIVEN-WHEN-THEN scenario:**

**Template:**
```kotlin
@Test
fun `GIVEN {precondition} WHEN {action} THEN {expected result}`()
```

**Categorize by type:**

**Happy Path Tests:**
```kotlin
GIVEN valid inputs WHEN executing method THEN should return expected output
GIVEN initial state WHEN action performed THEN should succeed
```

**Edge Case Tests:**
```kotlin
GIVEN null input WHEN processing THEN should handle gracefully
GIVEN empty list WHEN iterating THEN should return empty result
GIVEN max value WHEN incrementing THEN should handle overflow
```

**Error Path Tests:**
```kotlin
GIVEN invalid input WHEN validating THEN should throw IllegalArgumentException
GIVEN null parameter WHEN calling method THEN should throw NullPointerException
```

**State Transition Tests:**
```kotlin
GIVEN initial state WHEN action performed THEN should transition to new state
GIVEN logged out WHEN login called THEN should be logged in
```

**Async Tests:**
```kotlin
GIVEN suspend function WHEN executing THEN should complete successfully
GIVEN async operation WHEN awaiting THEN should return result
```

**Check for existing tests:**
```bash
# Find existing test file
Glob pattern="**/*{ClassName}Test.kt"

# If exists, read to avoid duplicates
Read {existing_test_file}

# Identify coverage gaps
```

### 4. PHASE 3: Create Todo List

**Generate comprehensive TodoWrite:**

```
TodoWrite with all test cases:
1. [pending] Test happy path: GIVEN valid user WHEN creating THEN returns user
2. [pending] Test null handling: GIVEN null input WHEN processing THEN throws exception
3. [pending] Test empty collection: GIVEN empty list WHEN summing THEN returns zero
4. [pending] Test boundary: GIVEN max value WHEN incrementing THEN handles overflow
... (all identified scenarios)
```

**Prioritize:**
- 🔴 HIGH: Public API, critical paths, error handling
- 🟡 MEDIUM: Edge cases, state transitions
- 🟢 LOW: Additional validations

### 5. PHASE 4: Test Implementation Loop

**5.1 Setup Test File**

**If test file doesn't exist:**

```kotlin
// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package {package_name}

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

/**
 * Tests for {ClassName} following GIVEN-WHEN-THEN patterns.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class {ClassName}Test {
    // Tests will be added here
}
```

**Write or Edit the test file:**
```bash
# If new file
Write {test_file_path} {content}

# If existing file (adding tests)
Edit {test_file_path} {old_content} {new_content}
```

**5.2 Implement Each Test**

**Mark as in_progress in TodoWrite**

**Pattern: Happy Path**
```kotlin
@Test
fun `GIVEN valid user data WHEN creating user THEN should return user instance`() = runTest {
    // GIVEN
    val userData = UserData(name = "John", email = "john@test.com")
    val creator = UserCreator()  // Isolated instance

    // WHEN
    val user = creator.create(userData)

    // THEN
    assertNotNull(user)
    assertEquals("John", user.name)
    assertEquals("john@test.com", user.email)
}
```

**Pattern: Null Handling**
```kotlin
@Test
fun `GIVEN null input WHEN processing THEN should throw IllegalArgumentException`() = runTest {
    // GIVEN
    val processor = DataProcessor()

    // WHEN & THEN
    assertFailsWith<IllegalArgumentException> {
        processor.process(null)
    }
}
```

**Pattern: Empty Collections**
```kotlin
@Test
fun `GIVEN empty list WHEN calculating sum THEN should return zero`() = runTest {
    // GIVEN
    val emptyList = emptyList<Int>()
    val calculator = SumCalculator()

    // WHEN
    val sum = calculator.sum(emptyList)

    // THEN
    assertEquals(0, sum)
}
```

**Pattern: Suspend Functions**
```kotlin
@Test
fun `GIVEN async operation WHEN executing THEN should complete successfully`() = runTest {
    // GIVEN
    val asyncService = AsyncService()

    // WHEN
    val result = asyncService.fetchData()  // suspend function

    // THEN
    assertNotNull(result)
    assertTrue(result.isSuccess)
}
```

**5.3 Validate Each Test**

```bash
# Compile test
./gradlew compileTestKotlin

# Run specific test
./gradlew test --tests "{ClassName}Test.{testMethodName}"

# Verify passes
# If fails: debug, fix, retry
# If passes: mark completed in TodoWrite
```

**Move to next test in todo list**

### 6. PHASE 5: Coverage Validation

**6.1 Run All Tests**
```bash
./gradlew test --tests "{ClassName}Test"
```

**6.2 Verify Coverage**
- [ ] All behaviors from Phase 1 have corresponding tests
- [ ] All public methods tested
- [ ] All edge cases covered
- [ ] All error paths tested
- [ ] State transitions validated

**6.3 GIVEN-WHEN-THEN Compliance Check**

**Validate ALL tests:**
- ✅ Use GIVEN-WHEN-THEN naming (uppercase)
- ✅ Use vanilla assertions (assertEquals, assertTrue, assertNotNull, etc.)
- ✅ Have @TestInstance(PER_CLASS) on class
- ✅ Use isolated instances (no shared state)
- ✅ Use runTest for suspend functions
- ✅ No mocks (use fakes if needed)
- ✅ No custom matchers

**If violations found:**
```bash
# Check for "should" pattern (forbidden)
Grep pattern='fun \`should' {test_file} -n

# If found, must fix before completing
```

**6.4 Remove Redundant Tests**
- Check for duplicate test scenarios
- Remove tests that don't add unique value
- Keep only tests that validate distinct behaviors

**6.5 Final Format & Validation**
```bash
# Format code
./gradlew spotlessApply

# Run all tests one final time
./gradlew test --tests "{ClassName}Test"

# Ensure all pass
```

### 7. Output Summary

**Provide comprehensive report:**

```
✅ BEHAVIOR ANALYSIS & TEST GENERATION COMPLETE

📁 Target File:
{file_path}

📊 Analysis Results:
- Public methods: {count}
- Properties: {count}
- Edge cases identified: {count}
- Error scenarios: {count}
- State transitions: {count}

🧪 Tests Generated:
- Total tests: {count}
- Happy path: {count}
- Edge cases: {count}
- Error paths: {count}
- State transitions: {count}
- Async/suspend: {count}

✅ Compliance:
- GIVEN-WHEN-THEN naming: ✅
- Vanilla assertions: ✅
- @TestInstance(PER_CLASS): ✅
- Isolated instances: ✅
- No mocks: ✅
- All tests pass: ✅

📄 Test File:
{test_file_path}

📋 Coverage:
- Methods covered: {count}/{total}
- Behaviors covered: {count}/{total}
- Coverage percentage: {percentage}%

🎯 Next Steps:
- Run tests: ./gradlew test --tests "{ClassName}Test"
- Review generated tests
- Add any additional edge cases if needed
```

## Supporting Files

Progressive disclosure for testing patterns:

- **`resources/testing-patterns.md`** - Complete GIVEN-WHEN-THEN patterns library (loaded on-demand)
- **`resources/edge-case-catalog.md`** - Common edge cases to test (loaded on-demand)
- **`resources/assertion-guide.md`** - Vanilla assertion usage guide (loaded on-demand)

## Related Skills

This Skill composes with:
- **`bdd-test-runner`** - Run generated tests
- **`fakt-docs-navigator`** - Consult testing guidelines
- **`compilation-validator`** - Validate generated tests compile

## Best Practices

1. **Analyze thoroughly** - Don't skip edge cases or error paths
2. **Generate unique tests** - Each test validates distinct behavior
3. **Follow ABSOLUTE STANDARD** - GIVEN-WHEN-THEN without exceptions
4. **Validate continuously** - Compile and run after each test
5. **Use TodoWrite** - Track progress through all test cases
6. **Isolated instances** - No shared state between tests
7. **Vanilla assertions only** - No custom matchers or BDD frameworks

## Testing Patterns Library

**For detailed patterns, consult:**
- `resources/testing-patterns.md` - When implementing specific test types
- `.claude/docs/validation/testing-guidelines.md` - For THE ABSOLUTE STANDARD

## Success Criteria

### Per Test:
- ✅ Follows GIVEN-WHEN-THEN naming exactly
- ✅ Uses vanilla JUnit5 + kotlin-test assertions
- ✅ Has @TestInstance(PER_CLASS) on class
- ✅ Uses isolated instances
- ✅ Compiles successfully
- ✅ Passes when run
- ✅ Tests unique behavior (not redundant)

### Overall Coverage:
- ✅ All public API behaviors tested
- ✅ All edge cases covered (null, empty, boundaries)
- ✅ All error paths tested
- ✅ State transitions validated
- ✅ Suspend functions use runTest
- ✅ No redundant tests
- ✅ 100% tests pass
- ✅ Code formatted with spotlessApply

## Execution Strategy

**Workflow:**
1. Extract target file from conversation
2. Perform deep behavior analysis
3. Generate GIVEN-WHEN-THEN test scenarios
4. Create comprehensive TodoWrite
5. Implement tests one by one (mark in_progress → completed)
6. Validate each test (compile + run)
7. Check overall coverage and compliance
8. Format code and final validation
9. Provide summary report

**Quality Gates:**
- ❌ Never mark test completed if doesn't compile
- ❌ Never mark completed if doesn't pass
- ❌ Never use "should" naming (forbidden)
- ✅ Only complete when test is 100% working and compliant

## Known Limitations

- Requires file path from user (cannot guess)
- Best for single file analysis (not entire modules)
- Generated tests may need manual refinement for complex business logic
- Cannot generate tests for private methods (focus on public API)

## Phase Timing Estimates

- Phase 1 (Analysis): ~2-5 minutes
- Phase 2 (Test Generation): ~1-3 minutes
- Phase 3 (Todo Creation): ~1 minute
- Phase 4 (Implementation): ~2-5 minutes per test
- Phase 5 (Validation): ~2-3 minutes

**Total**: Varies by file complexity (simple: 10-15 min, complex: 30-60 min)
