---
allowed-tools: Read, Grep, Glob, Write, Edit, TodoWrite, Bash(./gradlew test:*), Bash(./gradlew compileTestKotlin:*), Bash(./gradlew compileKotlinJvm:*), Bash(./gradlew spotlessApply:*)
description: Deep behavior analysis and comprehensive unit test generation following GIVEN-WHEN-THEN patterns
model: claude-sonnet-4-20250514
---

# 🧪 Deep Behavior Analysis + Test Generation Agent

**Specialized agent for analyzing file behaviors and generating comprehensive unit test coverage**

## 🎯 MISSION CRITICAL OBJECTIVES

### **🚨 ABSOLUTE STANDARDS:**

- **✅ ALWAYS follow GIVEN-WHEN-THEN** - No exceptions (.claude/docs/validation/testing-guidelines.md)
- **✅ ALWAYS use vanilla JUnit5** - No custom matchers or BDD frameworks
- **✅ ALWAYS use @TestInstance(PER_CLASS)** - Required for all test classes
- **✅ ALWAYS analyze deeply** - Understand ALL behaviors, not just happy path
- **✅ ALWAYS validate tests** - Must compile and pass before completion

### **❌ NEVER DO THIS:**

- **❌ NEVER use "should" naming** - Use GIVEN-WHEN-THEN format
- **❌ NEVER skip edge cases** - Cover error paths and boundaries
- **❌ NEVER create redundant tests** - Each test validates unique behavior
- **❌ NEVER use mocks** - Use fakes with builder patterns
- **❌ NEVER share state** - Isolated instances per test

---

## 🎯 WORKFLOW - 5 PHASE STRATEGY

### **📊 The Strategy:**

```
🔍 PHASE 1: Deep Behavior Analysis
   ↓ Read and understand target file completely
   ↓ Identify all behaviors, edge cases, dependencies
   ↓ Map state changes and side effects
   ↓ Document error handling patterns

📝 PHASE 2: Test Case Generation
   ↓ Generate GIVEN-WHEN-THEN scenarios
   ↓ Categorize by behavior type
   ↓ Identify coverage gaps
   ↓ Prioritize by criticality

📋 PHASE 3: Todo List Creation
   ↓ Create TodoWrite with all test cases
   ↓ Mark priority and dependencies
   ↓ Track implementation progress

🔧 PHASE 4: Test Implementation Loop
   ↓ For each test case:
   ↓   → Implement following guidelines
   ↓   → Validate: Compile + Pass
   ↓   → Mark completed only when 100% working

✅ PHASE 5: Coverage Validation
   ↓ Verify all behaviors covered
   ↓ Check for redundant tests
   ↓ Ensure GIVEN-WHEN-THEN compliance
   ↓ Final validation: all tests pass
```

---

## 🔍 PHASE 1: DEEP BEHAVIOR ANALYSIS

### **Step 1.1: Read Target File Completely**

```bash
# Read the target file provided as hint/parameter
Read <target_file_path>

# Understand file type and purpose
echo "<target_file_path>" | grep -E "\.(kt|java)$"
```

### **Step 1.2: Identify Core Behaviors**

**🎯 Behavior Categories to Analyze:**

1. **Public API Behaviors**
   - What does this class/function DO?
   - What are the inputs and outputs?
   - What contracts does it fulfill?

2. **State Management**
   - Does it manage internal state?
   - How does state change?
   - Are there state transitions?

3. **Edge Cases & Boundaries**
   - Null/empty inputs?
   - Boundary values (min/max)?
   - Invalid inputs?
   - Concurrent access scenarios?

4. **Error Handling**
   - What can go wrong?
   - How are errors handled?
   - What exceptions are thrown?

5. **Dependencies & Side Effects**
   - External dependencies?
   - File/network I/O?
   - Side effects (logging, metrics)?

### **Step 1.3: Analysis Commands**

```bash
# Find all public methods/functions
Grep pattern="(fun|override fun|suspend fun)\s+\w+" <target_file>

# Find all properties
Grep pattern="(val|var)\s+\w+:" <target_file>

# Find error handling
Grep pattern="(throw|catch|try|require|check)" <target_file>

# Find nullable types (edge cases)
Grep pattern="\w+\?" <target_file>

# Find suspend functions (coroutine tests needed)
Grep pattern="suspend\s+fun" <target_file>
```

### **Step 1.4: Document Behaviors**

**Create behavior map:**

```markdown
## Behaviors Found in <FileName>

### Public API:
1. `methodName(params): ReturnType` - Does X
2. `property: Type` - Represents Y

### Edge Cases Identified:
1. Null handling: method accepts/returns null
2. Empty collections: handles empty lists
3. Boundary values: min/max constraints

### Error Scenarios:
1. Throws IllegalArgumentException when X
2. Returns null/default when Y
3. Requires non-null parameters

### Dependencies:
1. Depends on: ClassA, ClassB
2. Side effects: Logs to console
3. Concurrency: Thread-safe/unsafe
```

---

## 📝 PHASE 2: TEST CASE GENERATION

### **Step 2.1: Generate GIVEN-WHEN-THEN Scenarios**

**🎯 Template for Each Behavior:**

```kotlin
@Test
fun `GIVEN <precondition> WHEN <action> THEN <expected result>`()

// Examples:
@Test
fun `GIVEN null input WHEN processing THEN should throw IllegalArgumentException`()

@Test
fun `GIVEN empty list WHEN calculating sum THEN should return zero`()

@Test
fun `GIVEN valid user WHEN authenticating THEN should return success result`()
```

### **Step 2.2: Categorize Test Cases**

**📊 Test Categories:**

1. **Happy Path Tests** (Basic functionality)
   ```
   GIVEN valid inputs WHEN executing THEN should produce expected output
   ```

2. **Edge Case Tests** (Boundaries)
   ```
   GIVEN null input WHEN processing THEN should handle gracefully
   GIVEN empty collection WHEN iterating THEN should return empty result
   GIVEN max value WHEN incrementing THEN should handle overflow
   ```

3. **Error Path Tests** (Exception handling)
   ```
   GIVEN invalid state WHEN calling method THEN should throw exception
   GIVEN missing dependency WHEN initializing THEN should fail fast
   ```

4. **State Transition Tests** (State changes)
   ```
   GIVEN initial state WHEN action performed THEN should transition to new state
   ```

5. **Concurrency Tests** (Thread safety)
   ```
   GIVEN multiple threads WHEN accessing shared state THEN should be thread-safe
   ```

### **Step 2.3: Identify Coverage Gaps**

```bash
# Check existing test file if exists
Glob pattern="**/*Test.kt" path="<directory_of_target>"

# If test exists, read it
Read <existing_test_file>

# Identify what's NOT tested yet
# Compare behaviors found vs test cases present
```

---

## 📋 PHASE 3: TODO LIST CREATION

### **Step 3.1: Create Comprehensive TodoWrite**

**Format:**

```
TodoWrite:
1. [PENDING] Test happy path: GIVEN valid input WHEN processing THEN returns success
2. [PENDING] Test null handling: GIVEN null input WHEN processing THEN throws exception
3. [PENDING] Test empty collection: GIVEN empty list WHEN summing THEN returns zero
4. [PENDING] Test boundary: GIVEN max value WHEN incrementing THEN handles overflow
5. [PENDING] Test state transition: GIVEN initial state WHEN action THEN new state
... (continue for all identified scenarios)
```

### **Step 3.2: Prioritize by Criticality**

**Priority Levels:**

- 🔴 **HIGH**: Public API, critical paths, error handling
- 🟡 **MEDIUM**: Edge cases, state transitions
- 🟢 **LOW**: Nice-to-have, additional validations

---

## 🔧 PHASE 4: TEST IMPLEMENTATION LOOP

### **Step 4.1: Test File Setup**

**If test file doesn't exist, create it:**

```kotlin
// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package <package_name>

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for <ClassName>.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class <ClassName>Test {
    // Tests here
}
```

### **Step 4.2: Implement Each Test**

**Per Test Implementation:**

```kotlin
@Test
fun `GIVEN <precondition> WHEN <action> THEN <expected>`() = runTest {
    // GIVEN
    val input = createTestInput()
    val sut = SystemUnderTest()  // Isolated instance

    // WHEN
    val result = sut.methodUnderTest(input)

    // THEN
    assertEquals(expectedValue, result)
    assertTrue(result.isValid)
}
```

### **Step 4.3: Testing Patterns by Scenario**

#### **Pattern 1: Happy Path**
```kotlin
@Test
fun `GIVEN valid user data WHEN creating user THEN should return user instance`() = runTest {
    // GIVEN
    val userData = UserData(name = "John", email = "john@example.com")

    // WHEN
    val user = createUser(userData)

    // THEN
    assertNotNull(user)
    assertEquals("John", user.name)
    assertEquals("john@example.com", user.email)
}
```

#### **Pattern 2: Null Handling**
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

#### **Pattern 3: Empty Collections**
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

#### **Pattern 4: Boundary Values**
```kotlin
@Test
fun `GIVEN max integer value WHEN incrementing THEN should handle overflow`() = runTest {
    // GIVEN
    val maxValue = Int.MAX_VALUE
    val calculator = SafeCalculator()

    // WHEN
    val result = calculator.increment(maxValue)

    // THEN
    assertTrue(result.isFailure) // or appropriate overflow handling
}
```

#### **Pattern 5: State Transitions**
```kotlin
@Test
fun `GIVEN initial state WHEN action performed THEN should transition to new state`() = runTest {
    // GIVEN
    val stateMachine = UserStateMachine()
    assertEquals(UserState.INITIAL, stateMachine.currentState)

    // WHEN
    stateMachine.login()

    // THEN
    assertEquals(UserState.LOGGED_IN, stateMachine.currentState)
}
```

#### **Pattern 6: Suspend Functions**
```kotlin
@Test
fun `GIVEN async operation WHEN executing THEN should complete successfully`() = runTest {
    // GIVEN
    val asyncService = AsyncService()

    // WHEN
    val result = asyncService.fetchData() // suspend function

    // THEN
    assertNotNull(result)
    assertTrue(result.isSuccess)
}
```

### **Step 4.4: Validation per Test**

```bash
# 1. Mark test as IN_PROGRESS in TodoWrite

# 2. Compile test file
./gradlew compileTestKotlin

# 3. Run specific test
./gradlew test --tests "<ClassName>Test.<testMethodName>"

# 4. Verify test passes
# If fails: debug and fix
# If passes: mark as COMPLETED in TodoWrite

# 5. Move to next test case
```

---

## ✅ PHASE 5: COVERAGE VALIDATION

### **Step 5.1: Verify All Behaviors Covered**

```bash
# Run all tests for the class
./gradlew test --tests "<ClassName>Test"

# Check that all behaviors from Phase 1 have corresponding tests
# Compare behavior map vs implemented tests
```

### **Step 5.2: Check for Redundant Tests**

**❌ Remove if:**
- Two tests validate same behavior
- Test doesn't add value
- Test is too trivial (e.g., testing getters)

### **Step 5.3: GIVEN-WHEN-THEN Compliance**

**Validate:**
- ✅ All tests use GIVEN-WHEN-THEN naming
- ✅ All tests use vanilla assertions (assertEquals, assertTrue, etc.)
- ✅ All tests use @TestInstance(PER_CLASS)
- ✅ All tests have isolated instances
- ✅ No mocks (use fakes)
- ✅ runTest used for suspend functions

### **Step 5.4: Final Validation**

```bash
# Format code
./gradlew spotlessApply

# Run all tests
./gradlew test --tests "<ClassName>Test"

# Ensure zero failures
# SUCCESS: All tests pass, all behaviors covered
```

---

## 🎯 SUCCESS CRITERIA

### **Per Test:**
- ✅ Follows GIVEN-WHEN-THEN naming exactly
- ✅ Uses vanilla JUnit5 + kotlin-test assertions
- ✅ Has @TestInstance(PER_CLASS) on class
- ✅ Uses isolated instances (no shared state)
- ✅ Compiles successfully
- ✅ Passes when run
- ✅ Tests unique behavior (not redundant)

### **Overall Coverage:**
- ✅ All public API behaviors tested
- ✅ All edge cases covered (null, empty, boundaries)
- ✅ All error paths tested
- ✅ State transitions validated
- ✅ Suspend functions use runTest
- ✅ No redundant tests
- ✅ 100% tests pass
- ✅ Code formatted with spotlessApply

---

## 🔍 ANALYSIS PATTERNS

### **Pattern Detection Commands:**

```bash
# Find all behaviors to test
Grep pattern="fun\s+\w+\(" <target_file> -n

# Find nullable types (edge case tests needed)
Grep pattern="\w+\?" <target_file> -n

# Find error handling (error path tests needed)
Grep pattern="throw\s+\w+" <target_file> -n

# Find state changes (state transition tests needed)
Grep pattern="(var|private var)\s+\w+" <target_file> -n

# Find suspend functions (runTest needed)
Grep pattern="suspend\s+fun" <target_file> -n
```

---

## 🚨 CRITICAL VALIDATION RULES

### **❌ NEVER MARK COMPLETED IF:**
- Test doesn't compile
- Test fails when run
- Test doesn't follow GIVEN-WHEN-THEN
- Test uses "should" naming
- Test uses custom matchers
- Test has shared state
- Behavior not actually validated

### **✅ ONLY MARK COMPLETED WHEN:**
- Test compiles successfully
- Test passes when run
- Follows GIVEN-WHEN-THEN exactly
- Uses vanilla assertions
- Has isolated instances
- Validates unique behavior
- Code formatted

---

## 📚 TESTING GUIDELINES REFERENCE

**Always follow:** `.claude/docs/validation/testing-guidelines.md`

**Key Rules:**
1. GIVEN-WHEN-THEN naming (uppercase)
2. Vanilla JUnit5 + kotlin-test
3. @TestInstance(PER_CLASS)
4. runTest for coroutines
5. Isolated instances per test
6. No mocks, use fakes
7. No custom matchers

---

## 🔄 EXECUTION STRATEGY

### **Initialization:**
1. Receive target file path as hint/parameter
2. Read and analyze file completely
3. Document all behaviors found
4. Generate GIVEN-WHEN-THEN test cases
5. Create comprehensive TodoWrite

### **Implementation Loop:**
1. Mark test as IN_PROGRESS
2. Implement test following patterns
3. Compile and validate
4. Run test and verify it passes
5. Mark as COMPLETED
6. Move to next test

### **Completion:**
1. All behaviors from analysis have tests
2. All tests compile and pass
3. GIVEN-WHEN-THEN compliance verified
4. Code formatted
5. TodoWrite 100% completed

---

**This agent implements deep behavior analysis → comprehensive test generation → validation loop → maximum coverage.**
