# Test Coverage Improvement Plan

> **Status**: Pattern 4 complete - Expanding test coverage
> **Date**: 2025-10-03
> **Goal**: Comprehensive behavior testing without complex IR mocks

## 📊 Current Coverage Analysis

### Existing Tests (8 files)

| Test File | Coverage Area | Status |
|-----------|---------------|--------|
| `ServiceLoaderValidationTest.kt` | Plugin discovery | ✅ Complete |
| `FakeAnnotationDetectorSimpleTest.kt` | @Fake detection | ✅ Complete |
| `FaktCompilerPluginRegistrarSimpleTest.kt` | Plugin registration | ✅ Complete |
| `FaktCommandLineProcessorSimpleTest.kt` | CLI processing | ✅ Complete |
| `GenericPatternAnalyzerTest.kt` | Generic pattern detection | ✅ Complete |
| `CompilerOptimizationsTest.kt` | JDK 21 optimizations | ✅ Complete |
| `CodeGenerationModulesContractTest.kt` | Module contracts | ✅ Complete |
| `ExtractedModulesIntegrationTest.kt` | End-to-end integration | ✅ Complete |

### Coverage Gaps

| Component | Current Coverage | Priority | Approach |
|-----------|------------------|----------|----------|
| **TypeResolver** | ❌ None | 🔴 ALTA | String-based pattern tests |
| **Default Value Generation** | ❌ None | 🔴 ALTA | Edge case validation |
| **ImplementationGenerator** | ✅ Integration only | 🟡 MÉDIA | Pattern validation tests |
| **FactoryGenerator** | ✅ Integration only | 🟡 MÉDIA | DSL generation tests |
| **ImportResolver** | ❌ None | 🟢 BAIXA | Cross-module tests |
| **Edge Cases** | ⚠️ Partial | 🔴 ALTA | Regression tests |

---

## 🎯 Test Strategy

### 1. String-Based Pattern Tests ✅ RECOMENDADO

**Why**: Avoids complex IR mock setup while validating critical behavior

**Examples**:
```kotlin
@Test
fun `GIVEN nullable type WHEN generating default THEN should return null`() {
    // Test string patterns in generated code
    val generated = generateForType("String?")
    assertTrue(generated.contains("= null"))
}
```

**Benefits**:
- ✅ Fast to write
- ✅ Easy to maintain
- ✅ Validates actual output
- ✅ No IR dependency

### 2. Integration Tests ✅ CURRENT APPROACH

**Coverage**: End-to-end compilation and code generation

**Strengths**:
- ✅ Validates real-world scenarios
- ✅ Catches integration bugs
- ✅ Tests actual compiler behavior

**Limitations**:
- ❌ Slow feedback loop
- ❌ Hard to isolate failures
- ❌ Requires full compilation

### 3. Edge Case Validation ⚠️ NEEDED

**Focus**: Uncommon but important scenarios

**Priority Cases**:
- Deeply nested generic types
- Multiple nullable layers
- Varargs with complex types
- Suspend functions with generics
- Result types with custom types

---

## 📋 Proposed Test Files

### Priority 1: Critical Behavior (4-6h)

#### 1. `TypeResolverBehaviorTest.kt` (2h)
**Focus**: Type conversion edge cases without IR mocks

```kotlin
class TypeResolverBehaviorTest {
    @Test
    fun `GIVEN List of nullable String WHEN converting THEN should preserve nullability`()

    @Test
    fun `GIVEN suspend function type WHEN converting THEN should include suspend modifier`()

    @Test
    fun `GIVEN Result with generic WHEN converting THEN should handle type parameter`()

    @Test
    fun `GIVEN vararg Array type WHEN unwrapping THEN should extract element type`()

    @Test
    fun `GIVEN nested generic type WHEN converting THEN should handle recursion`()

    // 15+ edge case tests
}
```

#### 2. `DefaultValueGenerationTest.kt` (1.5h)
**Focus**: Smart defaults for all type categories

```kotlin
class DefaultValueGenerationTest {
    @Test
    fun `GIVEN primitive type WHEN generating default THEN should use correct value`()

    @Test
    fun `GIVEN collection type WHEN generating default THEN should use empty collection`()

    @Test
    fun `GIVEN Result type WHEN generating default THEN should use Result success`()

    @Test
    fun `GIVEN custom domain type WHEN generating default THEN should provide error message`()

    @Test
    fun `GIVEN nullable custom type WHEN generating default THEN should use null`()

    // 20+ default value tests
}
```

#### 3. `CodeGenerationEdgeCasesTest.kt` (1.5h)
**Focus**: Complex interface scenarios

```kotlin
class CodeGenerationEdgeCasesTest {
    @Test
    fun `GIVEN interface with 50 methods WHEN generating THEN should handle all`()

    @Test
    fun `GIVEN deeply nested generics WHEN generating THEN should preserve structure`()

    @Test
    fun `GIVEN multiple suspend functions WHEN generating THEN should handle all`()

    @Test
    fun `GIVEN varargs with generics WHEN generating THEN should unwrap correctly`()

    @Test
    fun `GIVEN function returning function WHEN generating THEN should handle higher-order`()

    // 15+ edge case tests
}
```

#### 4. `Pattern4RegressionTest.kt` (1h)
**Focus**: Validate Pattern 4 refactoring didn't break behavior

```kotlin
class Pattern4RegressionTest {
    @Test
    fun `GIVEN simple interface WHEN generating via extracted methods THEN should match original output`()

    @Test
    fun `GIVEN interface with primitives WHEN using category defaults THEN should match original`()

    @Test
    fun `GIVEN interface with collections WHEN using extracted defaults THEN should match original`()

    @Test
    fun `GIVEN varargs parameter WHEN using unwrapVarargsType THEN should match original`()

    @Test
    fun `GIVEN complex interface WHEN using all extractions THEN should match original output`()

    // 10+ regression tests
}
```

---

### Priority 2: Enhanced Coverage (2-3h)

#### 5. `FactoryGenerationBehaviorTest.kt` (1h)
**Focus**: Factory function and DSL generation patterns

```kotlin
class FactoryGenerationBehaviorTest {
    @Test
    fun `GIVEN simple interface WHEN generating factory THEN should create correct signature`()

    @Test
    fun `GIVEN interface with properties WHEN generating DSL THEN should create all configurators`()

    @Test
    fun `GIVEN suspend functions WHEN generating DSL THEN should preserve suspend in config`()

    // 10+ factory/DSL tests
}
```

#### 6. `ImportResolutionTest.kt` (1h)
**Focus**: Cross-module import handling

```kotlin
class ImportResolutionTest {
    @Test
    fun `GIVEN type from different module WHEN resolving THEN should add import`()

    @Test
    fun `GIVEN kotlin stdlib type WHEN resolving THEN should not add redundant import`()

    @Test
    fun `GIVEN custom Result type WHEN resolving THEN should distinguish from kotlin Result`()

    // 10+ import resolution tests
}
```

#### 7. `GenericTypeHandlingTest.kt` (1h)
**Focus**: Generic type scoping and preservation

```kotlin
class GenericTypeHandlingTest {
    @Test
    fun `GIVEN interface-level generic WHEN erasing THEN should use Any`()

    @Test
    fun `GIVEN method-level generic WHEN preserving THEN should keep type parameter`()

    @Test
    fun `GIVEN bounded generic WHEN handling THEN should respect constraints`()

    // 12+ generic handling tests
}
```

---

## 🎯 Implementation Strategy

### Phase 1: Critical Behavior Tests (Week 1)
**Time**: 4-6 hours
**Files**: TypeResolverBehaviorTest, DefaultValueGenerationTest, CodeGenerationEdgeCasesTest, Pattern4RegressionTest

**Approach**:
1. Start with string-based pattern matching
2. Focus on known edge cases from Pattern 4 refactoring
3. Validate behavior without complex IR setups
4. Use sample generated code as reference

**Success Criteria**:
- ✅ 60+ new test cases
- ✅ All Pattern 4 extractions validated
- ✅ Edge cases documented and tested
- ✅ 100% of tests passing

### Phase 2: Enhanced Coverage (Week 2)
**Time**: 2-3 hours
**Files**: FactoryGenerationBehaviorTest, ImportResolutionTest, GenericTypeHandlingTest

**Approach**:
1. Build on Phase 1 patterns
2. Cover remaining gaps
3. Add cross-module scenarios
4. Document known limitations

**Success Criteria**:
- ✅ 30+ additional test cases
- ✅ Cross-module scenarios covered
- ✅ Generic handling validated
- ✅ 100% of tests passing

---

## 📊 Expected Coverage Metrics

### Before Enhancement
- **Test Files**: 8
- **Test Cases**: ~40
- **Coverage Areas**: Plugin infrastructure, basic integration
- **Edge Cases**: Minimal

### After Enhancement
- **Test Files**: 15 (+87%)
- **Test Cases**: ~130 (+225%)
- **Coverage Areas**: All critical components
- **Edge Cases**: Comprehensive

### Coverage by Component

| Component | Before | After | Improvement |
|-----------|--------|-------|-------------|
| Plugin Infrastructure | ✅ 90% | ✅ 95% | +5% |
| Type Resolution | ❌ 0% | ✅ 85% | +85% |
| Code Generation | ⚠️ 40% | ✅ 80% | +40% |
| Default Values | ❌ 0% | ✅ 90% | +90% |
| Edge Cases | ⚠️ 20% | ✅ 75% | +55% |
| **Overall** | **⚠️ 30%** | **✅ 85%** | **+55%** |

---

## 🚀 Next Steps

### Immediate (Today)
1. ✅ Create `TypeResolverBehaviorTest.kt` with 15+ tests
2. ✅ Create `DefaultValueGenerationTest.kt` with 20+ tests
3. ✅ Validate all tests pass

### This Week
1. Complete `CodeGenerationEdgeCasesTest.kt`
2. Complete `Pattern4RegressionTest.kt`
3. Update coverage documentation

### Next Week
1. Phase 2 enhanced coverage
2. Cross-module scenario tests
3. Final coverage report

---

## 📝 Testing Principles

### ✅ DO
- Use string pattern matching for generated code validation
- Focus on behavior, not implementation details
- Test edge cases discovered during development
- Keep tests fast and maintainable
- Document known limitations

### ❌ DON'T
- Create complex IR mocks (high maintenance cost)
- Test compiler internals directly (brittle)
- Duplicate integration test coverage
- Add tests without clear value
- Ignore failing tests

---

## 🎯 Success Metrics

### Test Quality
- ✅ All tests follow GIVEN-WHEN-THEN pattern
- ✅ Clear, descriptive test names
- ✅ Fast execution (< 5s total)
- ✅ 100% passing rate
- ✅ Zero flaky tests

### Coverage Quality
- ✅ Critical paths tested
- ✅ Edge cases documented
- ✅ Regression prevention
- ✅ Clear failure messages
- ✅ Easy to maintain

---

**Status**: Ready to implement Phase 1 ✅
**Estimated Time**: 4-6 hours for Phase 1
**Expected Outcome**: 85% overall coverage with pragmatic, maintainable tests
