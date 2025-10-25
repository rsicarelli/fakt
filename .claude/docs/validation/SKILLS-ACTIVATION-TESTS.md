# Skills Activation Test Suite

> **Purpose**: Validation prompts to test autonomous Skill activation
> **Usage**: Test each prompt to verify correct Skill is activated
> **Success criteria**: Skill activates within 3-5s without manual invocation

## Test Methodology

**Eval-Driven Development Approach:**
1. Send test prompt as natural language
2. Observe which Skill (if any) activates
3. Refine `description` if activation fails or wrong Skill activates
4. Iterate until reliable activation achieved

**Success Indicators:**
- ✅ Correct Skill activates autonomously
- ✅ Activation latency <5 seconds
- ✅ Skill follows instructions correctly
- ✅ No user confusion about what's happening

**Failure Indicators:**
- ❌ Wrong Skill activates
- ❌ No Skill activates (falls back to general Claude)
- ❌ Multiple Skills compete/conflict
- ❌ Latency >10 seconds

---

## Skill #1: kotlin-ir-debugger

**Target Description:**
```
Debugs Kotlin compiler Intermediate Representation (IR) generation for @Fake annotated interfaces. Use when analyzing IR, inspecting compiler output, debugging IR generation issues, troubleshooting interface generation, or when user mentions interface names with "debug", "IR", "intermediate representation", or "compiler generation" context.
```

### Test Prompts (Should Activate)

**Direct mention:**
1. "Debug the IR generation for AsyncService"
2. "Can you analyze the IR for UserRepository?"
3. "I need to inspect the compiler IR for PaymentProcessor"
4. "Help me debug intermediate representation generation"

**Contextual triggers:**
5. "The UserService interface is generating wrong code, can you debug it?"
6. "Analyze what's happening with IR generation for this interface"
7. "Check the compiler output for MyInterface"
8. "Inspect the IR for Repository, something's wrong"

**Problem-solving context:**
9. "AsyncDataService isn't compiling correctly, debug the IR"
10. "I think there's an issue with how the compiler generates IR for this interface"

### Negative Tests (Should NOT Activate)

**General questions:**
- "What is IR in compiler theory?" (should use general knowledge)
- "Explain Kotlin compiler IR" (should use general knowledge or fakt-docs-navigator)

**Different Skill domain:**
- "Run tests for IR generation" (should activate bdd-test-runner)
- "What are the Metro patterns for IR?" (should activate fakt-docs-navigator or metro-pattern-validator)

---

## Skill #2: bdd-test-runner

**Target Description:**
```
Executes BDD-compliant GIVEN-WHEN-THEN tests with vanilla JUnit5, validates test naming compliance, analyzes coverage, and ensures tests follow project testing standards. Use when running tests, validating test patterns, checking coverage, or when user mentions "run tests", "BDD", "GIVEN-WHEN-THEN", "test coverage", or "validate tests".
```

### Test Prompts (Should Activate)

**Direct commands:**
1. "Run all tests"
2. "Execute the test suite"
3. "Run BDD tests"
4. "Run tests with GIVEN-WHEN-THEN pattern"

**Pattern-based:**
5. "Run tests matching Generic*"
6. "Execute compiler tests only"
7. "Run tests for the IR generator"

**Validation requests:**
8. "Validate test naming compliance"
9. "Check if tests follow BDD patterns"
10. "Analyze test coverage"
11. "Check test coverage gaps"

**Quality checks:**
12. "Make sure all tests use GIVEN-WHEN-THEN"
13. "Verify no tests use 'should' pattern"
14. "Check Metro testing alignment"

### Negative Tests (Should NOT Activate)

**Test generation (different Skill):**
- "Generate tests for UserService" (should activate behavior-analyzer-tester)
- "Create unit tests" (should activate behavior-analyzer-tester)

**General questions:**
- "What is BDD?" (should use general knowledge or fakt-docs-navigator)
- "Explain testing guidelines" (should activate fakt-docs-navigator)

---

## Skill #3: fakt-docs-navigator

**Target Description:**
```
Intelligent navigator for Fakt's 80+ documentation files covering architecture, testing guidelines, Metro patterns, generic type handling, implementation roadmaps, and troubleshooting. Use when user asks about project concepts, patterns, guidelines, Metro alignment, testing standards, generic types, phase implementation, or needs reference to specific documentation.
```

### Test Prompts (Should Activate)

**Conceptual questions:**
1. "What are the testing guidelines for this project?"
2. "Explain Metro patterns in Fakt"
3. "What's the status of generic type support?"
4. "How does the architecture work?"

**Documentation lookup:**
5. "What does the documentation say about generics?"
6. "Show me the Metro alignment guidelines"
7. "What's the implementation roadmap?"
8. "What are the Phase 2 goals?"

**Best practices:**
9. "What's the correct way to test interfaces?"
10. "How should I handle suspend functions?"
11. "What are the multi-module setup guidelines?"

**Troubleshooting references:**
12. "What does the docs say about compilation errors?"
13. "Are there any known issues with generics?"
14. "What's the recommended approach for KMP?"

### Negative Tests (Should NOT Activate)

**Action requests (different Skills):**
- "Debug the IR for UserService" (should activate kotlin-ir-debugger)
- "Run tests" (should activate bdd-test-runner)
- "Validate Metro alignment" (should activate metro-pattern-validator)

**Code-specific:**
- "What does this code do?" (should use general Read/analysis, not docs)

---

## Skill #4: behavior-analyzer-tester

**Target Description:**
```
Deep behavior analysis and comprehensive unit test generation following GIVEN-WHEN-THEN patterns
```

### Test Prompts (Should Activate)

**Test generation:**
1. "Generate tests for UserService"
2. "Create unit tests for this interface"
3. "Write GIVEN-WHEN-THEN tests for PaymentProcessor"
4. "I need comprehensive test coverage for this class"

**Behavior analysis:**
5. "Analyze the behavior of this function and create tests"
6. "What behaviors should I test for this code?"
7. "Help me write tests that cover all edge cases"

**Test improvement:**
8. "Improve test coverage for GenericRepository"
9. "Add missing test cases for AsyncService"
10. "Generate tests following the project guidelines"

### Negative Tests (Should NOT Activate)

**Running existing tests:**
- "Run tests" (should activate bdd-test-runner)
- "Execute test suite" (should activate bdd-test-runner)

**Documentation:**
- "What are the testing guidelines?" (should activate fakt-docs-navigator)

---

## Cross-Skill Activation Tests

**Test Skill composition and prioritization:**

### Scenario 1: Debug + Test
```
Prompt: "Debug the IR for AsyncService and then run tests"

Expected:
1. kotlin-ir-debugger activates first
2. After debugging complete, bdd-test-runner activates
3. Both Skills compose into pipeline
```

### Scenario 2: Generate + Run Tests
```
Prompt: "Generate tests for UserRepository and run them"

Expected:
1. behavior-analyzer-tester activates first (generates tests)
2. bdd-test-runner activates second (runs generated tests)
3. Pipeline execution
```

### Scenario 3: Docs + Action
```
Prompt: "Check the Metro patterns and validate our IR generator against them"

Expected:
1. fakt-docs-navigator activates (retrieves Metro docs)
2. metro-pattern-validator activates (validates alignment)
3. Composed workflow
```

### Scenario 4: Ambiguous Intent
```
Prompt: "Fix the UserService"

Expected:
- Should ask clarifying question
- OR activate most relevant Skill based on context
- Should NOT activate wrong Skill
```

---

## Activation Refinement Workflow

**If test fails (wrong or no activation):**

### Step 1: Analyze Failure
```
- Which Skill activated? (if any)
- What was the semantic mismatch?
- Which keywords were present in prompt?
- Which keywords were missing in description?
```

### Step 2: Refine Description
```
Current description: "Debugs IR generation..."

If failed to activate on "inspect compiler output":
→ Add "inspect compiler output" to triggers

Updated description: "Debugs IR generation for interfaces. Use when analyzing IR, inspecting compiler output, ..."
```

### Step 3: Re-test
```
- Run same test prompt
- Verify activation
- Try variations
```

### Step 4: Document
```
Update this file with:
- Failed prompt
- Description change
- Success after refinement
```

---

## Metrics Tracking

**Track activation success rate:**

| Skill | Total Tests | Successful | Failed | Success Rate |
|-------|-------------|-----------|--------|--------------|
| kotlin-ir-debugger | 10 | ? | ? | ?% |
| bdd-test-runner | 14 | ? | ? | ?% |
| fakt-docs-navigator | 14 | ? | ? | ?% |
| behavior-analyzer-tester | 10 | ? | ? | ?% |

**Target: >90% success rate per Skill**

---

## Known Issues & Refinements

### Issue 1: [To be discovered during testing]
**Symptom:**
**Root cause:**
**Fix:**
**Test:**

### Issue 2: [To be discovered during testing]
**Symptom:**
**Root cause:**
**Fix:**
**Test:**

---

## Next Steps

1. **Run all test prompts** against each Skill
2. **Record activation success/failure**
3. **Refine descriptions** for failed cases
4. **Iterate** until >90% success rate
5. **Document** learnings in migration patterns

---

## Reference

- Gemini Research: Section 1.4 "Optimizing Descriptions for Autonomous Activation"
- Best Practice: "Evaluation-driven development" - test descriptions empirically
- Success Pattern: Specific, trigger-rich, third-person descriptions
