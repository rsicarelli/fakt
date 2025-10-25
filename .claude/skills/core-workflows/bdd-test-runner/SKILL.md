---
name: bdd-test-runner
description: Executes BDD-compliant GIVEN-WHEN-THEN tests with vanilla JUnit5, validates test naming compliance, analyzes coverage, and ensures tests follow project testing standards. Use when running tests, validating test patterns, checking coverage, or when user mentions "run tests", "BDD", "GIVEN-WHEN-THEN", "test coverage", or "validate tests".
allowed-tools: [Read, Bash, Grep, Glob, TodoWrite]
---

# BDD Test Runner & Compliance Validator

Executes GIVEN-WHEN-THEN tests with comprehensive compliance validation and coverage analysis for the Fakt compiler plugin.

## Core Mission

This Skill enforces THE ABSOLUTE TESTING STANDARD: GIVEN-WHEN-THEN pattern with vanilla JUnit5 + kotlin-test, ensuring all tests follow project guidelines and Metro-inspired patterns.

## Instructions

### 1. Understand Test Request

**Extract from conversation:**
- Test pattern to run (e.g., "Generic*", "Compiler*", "all")
- Scope (compiler tests, sample tests, integration tests)
- Validation requirements (naming compliance, coverage analysis)

**Common requests:**
- "Run all tests" → Execute full test suite
- "Run compiler tests" → Focus on compiler module
- "Validate test naming" → BDD compliance check only
- "Check coverage" → Coverage analysis

### 2. Pre-Execution BDD Compliance Check

**Before running tests, validate BDD compliance:**

```bash
# Find all test files
find compiler/src/test/kotlin -name "*Test.kt"

# Check for GIVEN-WHEN-THEN pattern
grep -r "fun \`GIVEN" compiler/src/test/kotlin/

# Check for forbidden "should" pattern (MUST BE ZERO)
grep -r "fun \`should" compiler/src/test/kotlin/
```

**Compliance checklist:**
- [ ] All test methods use `GIVEN-WHEN-THEN` naming (uppercase)
- [ ] No "should" pattern found (forbidden in this project)
- [ ] All test classes have `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`
- [ ] Using vanilla assertions (assertEquals, assertTrue, etc.)

**If non-compliant tests found:**
```
🚨 BDD COMPLIANCE VIOLATION DETECTED

❌ Found {count} tests using "should" pattern (forbidden)
📋 Tests must follow GIVEN-WHEN-THEN pattern

Files to fix:
- {list of non-compliant files}

📚 Reference: resources/testing-guidelines-reference.md
```

**Do not proceed with execution until compliance is confirmed or user acknowledges violations.**

### 3. Execute Tests

**Determine test scope:**

**All tests:**
```bash
cd ktfake && ./gradlew test
```

**Compiler module only:**
```bash
cd ktfake && ./gradlew :compiler:test
```

**Pattern-based:**
```bash
cd ktfake && ./gradlew :compiler:test --tests "*{Pattern}*"

# Examples:
./gradlew :compiler:test --tests "*Generic*"
./gradlew :compiler:test --tests "*GIVEN*WHEN*THEN*"
./gradlew :compiler:test --tests "*CompilerPluginRegistrar*"
```

**Capture output:**
- Total tests run
- Tests passed/failed/skipped
- Execution time
- Any warnings or errors

### 4. Analyze Results

**Parse test output:**

**Success scenario:**
```
✅ 53 tests passed (0 failed, 0 skipped)
⏱️ Execution time: 4.2s
📋 All tests follow GIVEN-WHEN-THEN pattern
```

**Failure scenario:**
```
❌ 3 tests failed (50 passed, 0 skipped)

Failed tests:
1. GIVEN interface with generics WHEN generating THEN should preserve types
   → Error: Type mismatch (Phase 2 limitation)

2. GIVEN complex interface WHEN generating THEN should compile
   → Error: Missing import for cross-module type

3. GIVEN async interface WHEN generating factory THEN should work
   → Error: Suspend function handling edge case

💡 Suggested actions:
- For generic issues: Consult generic-type-handling.md
- For compilation: Run /validate-compilation
- For async: Check suspend function patterns
```

### 5. Coverage Analysis

**Compare actual vs claimed coverage:**

```bash
# Count total test files
find compiler/src/test/kotlin -name "*Test.kt" | wc -l

# Count GIVEN-WHEN-THEN tests
grep -r "fun \`GIVEN" compiler/src/test/kotlin/ | wc -l

# Identify coverage gaps
find compiler/src/main/kotlin -name "*.kt" | while read file; do
    testFile="${file/src\/main/src\/test}"
    testFile="${testFile/.kt/Test.kt}"
    if [ ! -f "$testFile" ]; then
        echo "Missing tests for: $file"
    fi
done
```

**Coverage report format:**
```
📊 TEST COVERAGE ANALYSIS

Total implementation files: {count}
Total test files: {count}
Coverage ratio: {percentage}%

GIVEN-WHEN-THEN tests: 53
Legacy tests (disabled): {count}
Coverage gaps identified: {count}

Missing coverage for:
- {list of files without tests}

📚 Reference: `.claude/docs/validation/testing-guidelines.md`
```

### 6. Metro Pattern Validation

**Check if tests follow Metro testing approach:**

**Metro testing patterns:**
- Compiler plugin tests in compiler/src/test/
- Compilation validation tests
- Generated code verification
- API compatibility tests

**Validation:**
```bash
# Check for compilation tests
grep -r "compiles successfully" compiler/src/test/kotlin/

# Check for Metro-style context tests
grep -r "IrPluginContext" compiler/src/test/kotlin/

# Check for generated code validation
grep -r "build/generated" compiler/src/test/kotlin/
```

**For detailed Metro patterns:**
- Consult `resources/metro-testing-patterns.md`

### 7. Output Structured Report

**Standard test execution report:**
```
🧪 BDD TEST EXECUTION REPORT

📋 Compliance Status: ✅ PASSED
- GIVEN-WHEN-THEN naming: 53/53 tests ✅
- @TestInstance annotation: All classes ✅
- Vanilla assertions: No custom matchers ✅
- "should" pattern: 0 violations ✅

⚡ Execution Results:
- Total: 53 tests
- Passed: 53 ✅
- Failed: 0
- Skipped: 0
- Time: 4.2s

📊 Coverage: 85%
- Implementation files: 20
- Test files: 17
- Coverage gaps: 3 files

🏆 Metro Alignment: ✅
- Compiler plugin tests: Present
- Compilation validation: Present
- Generated code tests: Present

📁 Test output: build/test-results/
```

### 8. Suggest Follow-Up Actions

Based on test results:

**If tests fail:**
- Analyze errors and suggest relevant Skills
- For generic issues → `generic-scoping-analyzer`
- For compilation → `compilation-validator`
- For IR issues → `kotlin-ir-debugger`

**If coverage gaps:**
- Suggest files needing tests
- Offer to generate tests with `behavior-analyzer-tester`

**If compliance violations:**
- List non-compliant tests
- Suggest fixing patterns
- Reference testing guidelines

### 9. Update Test Status Tracking

**If user requests, update status files:**
```bash
# Update test metrics in current-status.md (optional)
echo "Last test run: $(date)" >> .claude/docs/implementation/test-status.md
echo "Tests passed: 53/53" >> .claude/docs/implementation/test-status.md
```

## Supporting Files

Progressive disclosure for detailed testing documentation:

- **`resources/testing-guidelines-reference.md`** - Complete GIVEN-WHEN-THEN standard (loaded on-demand)
- **`resources/metro-testing-patterns.md`** - Metro compiler testing approach (loaded on-demand)
- **`resources/coverage-analysis-guide.md`** - Coverage analysis techniques (loaded on-demand)
- **`resources/test-failure-diagnostics.md`** - Common test failure patterns (loaded on-demand)

## Test Execution Patterns

### Quick Test Run
```
User: "Run all tests"
→ Execute: ./gradlew test
→ Report: Pass/fail summary
```

### Compliance Check Only
```
User: "Validate test naming"
→ Skip execution
→ Check: GIVEN-WHEN-THEN compliance
→ Report: Violations if any
```

### Pattern-Based Testing
```
User: "Run generic tests"
→ Execute: ./gradlew test --tests "*Generic*"
→ Report: Subset results
```

### Full Analysis
```
User: "Run tests and check coverage"
→ Execute: ./gradlew test
→ Analyze: Coverage gaps
→ Report: Comprehensive analysis
```

## Related Skills

This Skill composes with:
- **`behavior-analyzer-tester`** - Generate missing tests
- **`compilation-validator`** - Validate generated code
- **`kotlin-ir-debugger`** - Debug IR generation issues
- **`metro-pattern-validator`** - Check Metro alignment

## Best Practices

1. **Always validate BDD compliance** before execution
2. **Report coverage gaps** to maintain quality
3. **Suggest actionable fixes** for failures
4. **Reference testing guidelines** for violations
5. **Track test metrics** over time

## Known Testing Standards

From `.claude/docs/validation/testing-guidelines.md`:

- ✅ **GIVEN-WHEN-THEN** naming (uppercase, mandatory)
- ✅ **@TestInstance(PER_CLASS)** lifecycle (required)
- ✅ **Vanilla JUnit5** + kotlin-test (no custom matchers)
- ✅ **runTest** for coroutines
- ✅ **Isolated instances** per test (no shared state)
- ❌ **"should" pattern** (forbidden in this project)
- ❌ **Custom BDD frameworks** (not allowed)
- ❌ **Mocks** (use fakes instead)

## Current Status (Phase 1)

- 53 tests passing with GIVEN-WHEN-THEN pattern
- 85% compilation success rate target
- Zero "should" violations (enforced)
- Metro-aligned testing approach
