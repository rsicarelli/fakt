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

## Skill #3: public-docs-navigator

**Target Description:**
```
Navigate Fakt's public MkDocs documentation site (docs/) for users learning about Fakt features, multi-module setup, testing patterns, and code generation strategy. Use when users ask about documentation, guides, multi-module setup, code generation strategy, testing patterns, or need help understanding Fakt features. Provides quick navigation to reference docs, usage guides, multi-module documentation, and troubleshooting resources.
```

### Test Prompts (Should Activate)

**Getting started:**
1. "How do I get started with Fakt?"
2. "Show me the installation guide"
3. "How do I create my first fake?"
4. "Quick start tutorial for Fakt"

**Feature documentation:**
5. "How do I use generics in Fakt?"
6. "Show me how to handle suspend functions"
7. "What are the configuration options?"
8. "How do properties work?"

**Multi-module:**
9. "How do I set up multi-module support?"
10. "Multi-module troubleshooting guide"
11. "How does KMP multi-module work?"
12. "Show me the FakeCollectorTask documentation"

**Strategic documentation:**
13. "Explain the code generation strategy"
14. "Why fakes over mocks?"
15. "Show me the performance comparison"
16. "What are the limitations?"

**Migration and guides:**
17. "How do I migrate from MockK?"
18. "What are the testing patterns?"
19. "Show me the migration guide"

### Negative Tests (Should NOT Activate)

**Internal contributor docs (use fakt-docs-navigator):**
- "What are the testing guidelines for contributors?" (should activate fakt-docs-navigator)
- "Show me the Metro alignment docs" (should activate fakt-docs-navigator)
- "What's the compiler architecture?" (should activate fakt-docs-navigator)

**Action requests:**
- "Generate a fake for UserService" (different task, not documentation)
- "Run tests" (should activate bdd-test-runner)

---

## Skill #4: fakt-docs-navigator

**Target Description:**
```
Navigate Fakt's internal contributor documentation (.claude/docs/, 66 files across 18 directories) covering compiler plugin architecture, testing guidelines, Metro patterns, generic type handling, implementation roadmaps, FIR/IR design, and troubleshooting. Use when discussing internal implementation, compiler architecture, Metro alignment, testing standards, codegen v2, or contributor-level technical details. For public user documentation, use public-docs-navigator instead.
```

### Test Prompts (Should Activate)

**Contributor guidelines:**
1. "What are the testing guidelines for this project?"
2. "Show me the BDD standards"
3. "What's THE ABSOLUTE STANDARD for testing?"

**Metro patterns:**
4. "Explain Metro patterns in Fakt"
5. "Show me the Metro alignment guidelines"
6. "How does Metro handle generics?"
7. "What are the Metro FIR/IR patterns?"

**Architecture:**
8. "How does the compiler architecture work?"
9. "What's the codegen v2 approach?"
10. "Show me the FIR implementation plan"
11. "What's the unified IR native design?"

**Implementation status:**
12. "What's the status of generic type support?"
13. "What's the implementation roadmap?"
14. "What are the Phase 2 goals?"
15. "Show me the current implementation status"

**Technical deep dives:**
16. "What does the documentation say about generic scoping?"
17. "Are there any known issues with class-level generics?"
18. "What's the validation approach for compilation?"

### Negative Tests (Should NOT Activate)

**Public user docs (use public-docs-navigator):**
- "How do I install Fakt?" (should activate public-docs-navigator)
- "Show me the multi-module setup guide" (should activate public-docs-navigator)
- "How do I use generics?" (should activate public-docs-navigator)

**Action requests:**
- "Debug the IR for UserService" (should activate kotlin-ir-debugger)
- "Run tests" (should activate bdd-test-runner)

---

## Skill #5: behavior-analyzer-tester

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
| public-docs-navigator | 19 | ? | ? | ?% |
| fakt-docs-navigator | 18 | ? | ? | ?% |
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
