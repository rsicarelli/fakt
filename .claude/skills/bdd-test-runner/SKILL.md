---
name: bdd-test-runner
description: Runs BDD-compliant GIVEN-WHEN-THEN tests, validates naming compliance, and analyzes coverage. Use when running tests, checking BDD compliance, validating test patterns, or verifying that test names follow GIVEN-WHEN-THEN conventions. Make sure to use this skill whenever tests need to be executed — it validates naming compliance and catches convention violations that plain test runners miss.
allowed-tools: Read, Bash, Grep, Glob, TaskCreate, TaskUpdate
---

# BDD Test Runner & Compliance Validator

Executes GIVEN-WHEN-THEN tests with compliance validation and coverage analysis.

## Instructions

### 1. Understand Test Request

**Common requests:**
- "Run all tests" → `./gradlew test`
- "Run compiler tests" → `./gradlew :compiler:test`
- "Run specific tests" → `./gradlew :compiler:test --tests "*Pattern*"`
- "Validate test naming" → BDD compliance check only
- "Check coverage" → Coverage analysis

### 2. Pre-Execution BDD Compliance Check

```bash
# Find all test files
find compiler/src/test/kotlin -name "*Test.kt"

# Check for GIVEN-WHEN-THEN pattern
grep -r "fun \`GIVEN" compiler/src/test/kotlin/

# Check for forbidden "should" pattern (MUST BE ZERO)
grep -r "fun \`should" compiler/src/test/kotlin/
```

**Compliance checklist:**
- [ ] All test methods use GIVEN-WHEN-THEN naming (uppercase)
- [ ] No "should" pattern (forbidden)
- [ ] All classes have `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`
- [ ] Vanilla assertions only (no custom matchers)
- [ ] No mocks (use fakes)

**If violations found — report and do not proceed until acknowledged:**
```
BDD COMPLIANCE VIOLATION

Found {count} tests using "should" pattern (forbidden)
Files: {list}
Reference: .claude/docs/development/validation/testing-guidelines.md
```

### 3. Execute Tests

```bash
# All tests
cd fakt && ./gradlew test

# Compiler module
cd fakt && ./gradlew :compiler:test

# Pattern-based
cd fakt && ./gradlew :compiler:test --tests "*{Pattern}*"
```

### 4. Analyze Results

**Parse output:** total run, passed, failed, skipped, execution time.

**If failures:** categorize each failure and suggest relevant skills:
- Compilation errors → `compilation` skill
- Test logic errors → fix test or implementation
- Missing imports → check generated code

### 5. Coverage Analysis

```bash
# Count GIVEN-WHEN-THEN tests
grep -r "fun \`GIVEN" compiler/src/test/kotlin/ | wc -l

# Find implementation files without tests
find compiler/src/main/kotlin -name "*.kt" | while read file; do
    testFile="${file/src\/main/src\/test}"
    testFile="${testFile/.kt/Test.kt}"
    if [ ! -f "$testFile" ]; then
        echo "Missing tests for: $file"
    fi
done
```

### 6. Report

```
BDD TEST EXECUTION REPORT

Compliance: {PASSED/FAILED}
- GIVEN-WHEN-THEN naming: {count}/{total} ✅
- "should" violations: {count}

Execution:
- Total: {n} | Passed: {n} | Failed: {n} | Skipped: {n}
- Time: {duration}

Coverage:
- Implementation files: {n} | Test files: {n}
- Coverage gaps: {list if any}
```

**Follow-up suggestions:**
- If coverage gaps → offer to generate tests with `behavior-analyzer-tester`
- If failures → suggest fixes or relevant skills

## Related Skills

- **`behavior-analyzer-tester`** — Generate missing tests
- **`compilation`** — Validate code compiles
