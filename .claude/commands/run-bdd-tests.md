---
allowed-tools: Read, Bash(./gradlew:*), Bash(find:*), Grep, TodoWrite, Task
argument-hint: [pattern|all|compiler] (optional - test pattern to execute, default: all)
description: Execute BDD-compliant GIVEN-WHEN-THEN tests with vanilla JUnit5 and coverage validation
model: claude-sonnet-4-20250514
---

# 🧪 BDD Test Executor & Compliance Validator

**GIVEN-WHEN-THEN test execution with comprehensive coverage analysis**

## 📚 Context Integration

**This command leverages:**
- `.claude/docs/validation/testing-guidelines.md` - THE ABSOLUTE STANDARD for testing
- `.claude/docs/analysis/metro-inspiration.md` - Metro testing pattern alignment
- `.claude/docs/implementation/current-status.md` - Current test coverage status
- `.claude/docs/troubleshooting/common-issues.md` - Test execution issue patterns
- Real test files for BDD compliance validation
- Gradle test execution for comprehensive coverage

**🏆 BDD TESTING BASELINE:**
- 53 tests passing with GIVEN-WHEN-THEN patterns
- @TestInstance(TestInstance.Lifecycle.PER_CLASS) lifecycle
- Vanilla JUnit5 + kotlin-test assertions
- Zero "should" patterns (forbidden in this project)

## Usage
```bash
/run-bdd-tests [pattern]
/run-bdd-tests all
/run-bdd-tests should_generate_*
/run-bdd-tests compiler
```

## What This Command Does

### 1. **BDD Compliance Validation**
- Verify test names follow `GIVEN-WHEN-THEN` pattern
- Validate @TestInstance(TestInstance.Lifecycle.PER_CLASS) usage
- Check for vanilla kotlin-test assertions only

### 2. **Test Execution**
- Run vanilla JUnit5 tests
- Execute pattern-matched test subsets
- Provide detailed execution results

### 3. **Coverage Analysis**
- Real vs claimed coverage validation
- Identify missing test coverage areas
- Compare with disabled legacy tests

### 4. **Metro Pattern Validation**
- Check if tests follow Metro testing approach
- Validate compiler plugin testing patterns
- Ensure compilation validation tests exist

## BDD Naming Compliance

### **✅ CORRECT GIVEN-WHEN-THEN Naming:**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnifiedKtFakesIrGenerationExtensionTest {

    @Test
    fun `GIVEN interface with methods WHEN generating fake THEN should create implementation class`() = runTest { ... }

    @Test
    fun `GIVEN interface with suspend functions WHEN generating fake THEN should preserve suspend signatures`() = runTest { ... }

    @Test
    fun `GIVEN interface with generic methods WHEN generating fake THEN should preserve type parameters`() = runTest { ... }

    @Test
    fun `GIVEN interface WHEN generating factory function THEN should create configuration DSL`() = runTest { ... }

    @Test
    fun `GIVEN generated fake code WHEN compiling THEN should compile without errors`() = runTest { ... }
}
```

### **❌ INCORRECT Naming:**
```kotlin
@Test
fun testFakeGeneration() { ... }                          // No BDD structure

@Test
fun `should generate fake implementation`() { ... }       // "should" pattern (forbidden)

@Test
fun generateFakeTest() { ... }                            // No GIVEN-WHEN-THEN

@Test
fun `fake generation works`() { ... }                     // Not GIVEN-WHEN-THEN

// Missing @TestInstance annotation (required)
class SomeTest { ... }
```

## Test Categories

### **1. Compiler Plugin Tests**
```bash
/run-bdd-tests compiler
```

**Expected GIVEN-WHEN-THEN Tests:**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnifiedKtFakesIrGenerationExtensionTest {

    @Test
    fun `GIVEN compiler context WHEN creating extension THEN should initialize successfully`() = runTest { ... }

    @Test
    fun `GIVEN interface declaration WHEN generating implementation THEN should create correct structure`() = runTest { ... }

    @Test
    fun `GIVEN interface name WHEN processing THEN should handle naming correctly`() = runTest { ... }

    @Test
    fun `GIVEN suspend functions WHEN generating THEN should preserve suspend signatures`() = runTest { ... }

    @Test
    fun `GIVEN interface WHEN generating factory THEN should create configuration DSL`() = runTest { ... }

    @Test
    fun `GIVEN generated code WHEN compiling THEN should compile successfully`() = runTest { ... }
}
```

### **2. Code Generation Tests**
```bash
/run-bdd-tests should_generate_*
```

**Pattern-Matched Tests:**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeGenerationTest {

    @Test
    fun `GIVEN basic interface WHEN generating THEN should create implementation`() = runTest { ... }

    @Test
    fun `GIVEN suspend functions WHEN generating THEN should create suspend implementations`() = runTest { ... }

    @Test
    fun `GIVEN interface properties WHEN generating THEN should create property implementations`() = runTest { ... }

    @Test
    fun `GIVEN interface WHEN generating factory THEN should use correct naming`() = runTest { ... }

    @Test
    fun `GIVEN interface WHEN generating DSL THEN should create configuration class`() = runTest { ... }
}
```

### **3. Type System Tests**
```bash
/run-bdd-tests type_system
```

**Generic Type Handling:**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypeSystemTest {

    @Test
    fun `GIVEN generic interface WHEN processing types THEN should handle parameters correctly`() = runTest { ... }

    @Test
    fun `GIVEN generated code WHEN validating THEN should preserve type safety`() = runTest { ... }

    @Test
    fun `GIVEN generic methods WHEN generating THEN should apply dynamic casting with suppressions`() = runTest { ... }

    @Test
    fun `GIVEN generic methods WHEN generating defaults THEN should use identity functions`() = runTest { ... }
}
```

### **4. Integration Tests**
```bash
/run-bdd-tests integration
```

**End-to-End Validation:**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest {

    @Test
    fun `GIVEN generated fakes WHEN compiling test project THEN should compile successfully`() = runTest { ... }

    @Test
    fun `GIVEN multiple interfaces WHEN processing same project THEN should work correctly`() = runTest { ... }

    @Test
    fun `GIVEN generated fakes WHEN integrating with JUnit5 THEN should work seamlessly`() = runTest { ... }
}
```

## Command Output

### **BDD Compliance Report**
```
🧪 BDD TEST COMPLIANCE REPORT

📊 TOTAL TESTS: 53
✅ BDD COMPLIANT: 41 (77%)
❌ NON-COMPLIANT: 12 (23%)

📋 COMPLIANT EXAMPLES:
- ✅ `GIVEN compiler context WHEN creating extension THEN should initialize successfully`
- ✅ `GIVEN interface declaration WHEN generating implementation THEN should create correct structure`
- ✅ `GIVEN suspend functions WHEN generating THEN should preserve suspend signatures`

❌ NON-COMPLIANT TESTS:
- testBasicFunctionality (no GIVEN-WHEN-THEN)
- `should generate implementation` ("should" pattern forbidden)
- generateFactoryFunction (not BDD style)
- Missing @TestInstance(TestInstance.Lifecycle.PER_CLASS)

💡 RECOMMENDATIONS:
1. Rename tests to follow 'GIVEN-WHEN-THEN' pattern
2. Add @TestInstance(TestInstance.Lifecycle.PER_CLASS) to all test classes
3. Use runTest for coroutines code
4. Use vanilla kotlin-test assertions only (assertEquals, assertTrue, etc.)
5. Focus on behavior description, not implementation details
```

### **Test Execution Results**
```
🔧 EXECUTING BDD TESTS: should_generate_*

⚡ RUNNING TESTS:
- `GIVEN basic interface WHEN generating THEN should create implementation` ✅ PASSED (142ms)
- `GIVEN suspend functions WHEN generating THEN should create suspend implementations` ✅ PASSED (89ms)
- `GIVEN interface WHEN generating factory THEN should use correct naming` ✅ PASSED (76ms)
- `GIVEN interface WHEN generating DSL THEN should create configuration class` ⚠️  SKIPPED (disabled)
- `GIVEN generic interface WHEN processing types THEN should handle parameters correctly` ❌ FAILED (see details)

📊 RESULTS SUMMARY:
- ✅ PASSED: 3/5 (60%)
- ❌ FAILED: 1/5 (20%)
- ⚠️  SKIPPED: 1/5 (20%)

❌ FAILURE DETAILS:
`GIVEN generic interface WHEN processing types THEN should handle parameters correctly`:
- Issue: Type parameter <T> not preserved in generated code
- Related: Phase 2 generic scoping challenge
- Status: Known architectural limitation
```

### **Coverage Analysis**
```
📊 COVERAGE ANALYSIS vs DOCUMENTED CLAIMS

📋 CLAIMED (Documentation): 75% Phase 1 complete
📋 ACTUAL (Test Results):

✅ WORKING FEATURES (validated by tests):
- Basic interface implementation generation
- Suspend function handling
- Factory function generation
- Configuration DSL structure

🚨 MISSING/BROKEN (test failures):
- Generic type parameter preservation
- Cross-module import generation
- Call tracking functionality
- Builder pattern support

💯 REAL COMPLETION: 45% (not 75%)

🎯 PRIORITY TEST GAPS:
1. Generic type scoping validation tests
2. Compilation validation for generated code
3. Metro pattern compliance tests
4. Performance benchmark tests
```

## Test Implementation Guidance

### **Writing New GIVEN-WHEN-THEN Tests**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewFeatureTest {

    @Test
    fun `GIVEN interface with methods WHEN generating fake THEN should create implementation`() = runTest {
        // Given - Create isolated instances
        val testInterface = createTestInterface("UserService") {
            method("getUser") { returns("User") }
            method("updateUser") { suspend(); returns("Boolean") }
        }
        val generator = FakeImplementationGenerator()

        // When - Execute generation
        val result = generator.generate(testInterface)

        // Then - Use vanilla assertions
        assertEquals("FakeUserServiceImpl", result.className)
        assertTrue(result.compiles())
        assertTrue(result.implementsInterface(testInterface))
    }
}
```

### **Metro Pattern Testing**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetroPatternTest {

    @Test
    fun `GIVEN extension WHEN validating architecture THEN should follow Metro context pattern`() = runTest {
        // Given
        val extension = UnifiedKtFakesIrGenerationExtension(...)
        val validator = MetroPatternValidator()

        // When
        val result = validator.validate(extension)

        // Then
        assertTrue(result.hasContextPattern)
        assertTrue(result.followsMetroErrorHandling)
    }
}
```

## Integration with Development Workflow

### **TDD Workflow Integration**
```bash
# 1. Write failing BDD test
/run-bdd-tests should_handle_new_feature

# 2. Implement feature following Metro patterns
/validate-metro-alignment implementation

# 3. Validate Kotlin API compatibility
/consult-kotlin-api RelevantApi

# 4. Verify test passes
/run-bdd-tests should_handle_new_feature

# 5. Run full test suite
/run-bdd-tests all
```

### **Pre-Commit Validation**
```bash
# Validate BDD compliance before commit
/run-bdd-tests all --validate-bdd-only

# Check Metro alignment
/validate-metro-alignment

# Ensure compilation works
/debug-ir-generation <test_interface>
```

## Error Scenarios

### **Test Execution Failures**
```
❌ TEST FAILURE: should_generate_complex_generics

🔍 ROOT CAUSE: Generic type parameter <T> becomes Any in generated code
📍 LOCATION: UnifiedKtFakesIrGenerationExtension.irTypeToKotlinString()
🎯 PHASE: Phase 2 generic scoping challenge

🔧 IMMEDIATE ACTIONS:
1. Document as known limitation
2. Add workaround with dynamic casting
3. Track in Phase 2 implementation

💡 LONG-TERM SOLUTION:
Implement Metro-inspired type resolution patterns
```

### **BDD Compliance Issues**
```
⚠️  BDD COMPLIANCE WARNING: 12 tests need renaming

📋 SUGGESTED RENAMES:
- testBasicGeneration → `GIVEN interface WHEN generating fake THEN should create implementation`
- validateSuspendFunctions → `GIVEN suspend functions WHEN generating THEN should preserve suspend signatures`
- factoryFunctionTest → `GIVEN interface WHEN creating factory THEN should generate configuration DSL`

🔧 AUTO-FIX AVAILABLE:
Run /fix-bdd-naming to automatically suggest renames
```

## Related Commands
- `/debug-ir-generation <interface>` - Debug specific generation issues
- `/validate-metro-alignment` - Check Metro pattern compliance
- `/consult-kotlin-api` - Validate API usage in tests

## Technical References
- Metro Testing: `/metro/compiler-tests/`
- KtFakes Tests: `ktfake/compiler/src/test/`
- BDD Guidelines: `.claude/docs/validation/junit5-bdd-validation.md`