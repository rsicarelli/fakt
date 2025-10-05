# Phase 1: Performance Dominance (Q1 2025)

> **Strategic Goal**: Solve critical JVM performance bottlenecks
> **Target**: Become the **fastest** Kotlin testing tool
> **Timeline**: 8-12 weeks
> **Status**: Planning

## Executive Summary

Phase 1 addresses the most severe performance pain points in Kotlin JVM testing. Existing tools (MockK, Mockito) rely on runtime bytecode manipulation to work around Kotlin's language features (final by default, singleton objects, static scope). These workarounds impose extreme performance penalties—up to **1300x slower** for certain operations.

Fakt's compiler plugin architecture eliminates these costs entirely by generating fakes at compile-time. This phase will establish Fakt as the performance leader and provide a compelling migration path for teams suffering from slow test suites.

---

## The Performance Crisis

### Research Findings

From extensive benchmarking and community reports:

1. **mockkObject**: **1300x slower** than injecting a faked dependency
   - Source: [Benchmarking MockK article](https://medium.com/@_kevinb/benchmarking-mockk-avoid-these-patterns-for-fast-unit-tests-220fc225da55)
   - Impact: Single object mock can add seconds to test execution
   - Developer advice: "Avoid at all costs" in performance-critical tests

2. **Mockito-inline** (final class workaround): **3x slower** test suites
   - Source: [Mocking Kotlin classes article](https://medium.com/21buttons-tech/mocking-kotlin-classes-with-mockito-the-fast-way-631824edd5ba)
   - Impact: Real-world projects report minutes of additional test time
   - Production cost: Developers avoid test coverage to reduce build time

3. **mockkStatic** (top-level functions): Severe performance degradation
   - Source: [MockK performance benchmarks](https://medium.com/@_kevinb/benchmarking-mockk-avoid-these-patterns-for-fast-unit-tests-220fc225da55)
   - Impact: Developers avoid complex logic in extension functions
   - Design constraint: Architecture decisions driven by testing limitations

### Root Cause

All performance issues stem from **runtime bytecode manipulation**:
- Class transformation at load time
- Reflection-based proxying
- Static field interception
- Complex classloader tricks

**Fakt's Solution**: Generate fakes at compile-time, zero runtime overhead.

---

## Phase 1 Features

### 1.1 Final Class Faking
**Priority**: HIGH
**Complexity**: Medium (2-4 weeks)
**Detailed Docs**: [finalClasses/APPROACH.md](./finalClasses/APPROACH.md)

**Problem**: Kotlin classes are final by default, breaking Mockito. Current solutions:
- `open` keyword (pollutes production code)
- `all-open` plugin (still pollutes with annotations)
- `mockito-inline` (3x slower)

**Fakt's Solution**: Compile-time fake generation for final classes without code modification.

**Success Metric**: Same performance as interface faking, no production code changes.

---

### 1.2 Singleton Object Faking
**Priority**: CRITICAL
**Complexity**: High (4-6 weeks)
**Detailed Docs**: [singletonObjects/APPROACH.md](./singletonObjects/APPROACH.md)

**Problem**: Singleton objects are first-class in Kotlin but impossible to replace at runtime.

**Current Solution**: `mockkObject` uses extreme bytecode manipulation.
- **1300x+ slower** than injection
- Major cause of slow test suites
- Community recommendation: "Never use it"

**Fakt's Solution**: Call-site replacement at compile-time.
- Rewrite `MySingleton.method()` → `MySingletonFake.method()` in test code
- Zero runtime overhead
- Type-safe, no reflection

**Success Metric**: **1000x+ performance improvement** over mockkObject.

---

### 1.3 Top-level & Extension Function Faking
**Priority**: HIGH
**Complexity**: High (4-6 weeks)
**Detailed Docs**: [topLevelFunctions/APPROACH.md](./topLevelFunctions/APPROACH.md)

**Problem**: Top-level and extension functions compile to static methods, difficult to intercept.

**Current Solution**: `mockkStatic("com.example.FileKt")`
- String-based reference (brittle)
- Severe performance penalty
- Breaks on file renames

**Fakt's Solution**: Type-safe call-site replacement.
- Analyze compiler-generated synthetic classes
- Rewrite calls in test source set
- Compile-time safety

**Success Metric**: Type-safe, 100x+ faster than mockkStatic.

---

### 1.4 Suspend Function Support (Baseline Requirement)
**Priority**: CRITICAL
**Complexity**: Low (already implemented for interfaces)
**Status**: ✅ Complete for interfaces, extend to Phase 1 features

**Requirement**: All generated fakes must correctly implement `suspend` functions.

**Implementation**: Preserve `suspend` modifier in generated code, ensure compatibility with `runTest` and coroutine contexts.

**Success Metric**: Generated fakes work seamlessly in coroutine tests without special handling.

---

## Implementation Strategy

### Week 1-2: Final Classes Foundation
- Extend `InterfaceAnalyzer` to handle non-interface types
- Update `ImplementationGenerator` for class hierarchy
- Handle constructor parameters, visibility modifiers
- Test with simple final classes

### Week 3-4: Final Classes Production
- Complex scenarios: inheritance, generics, inner classes
- Comprehensive test coverage (GIVEN-WHEN-THEN)
- Performance benchmarks vs. Mockito-inline
- Documentation and examples

### Week 5-7: Singleton Object Call-site Replacement (CRITICAL)
- IR analysis: Detect singleton usages in test source sets
- Call-site rewriting: Replace calls with fake references
- Handle `companion object` scenarios
- Edge cases: Nested objects, qualified names

### Week 8-10: Top-level Function Replacement
- Analyze synthetic class generation patterns
- Call-site replacement for top-level functions
- Extension function handling (receiver types)
- File-level scoping and imports

### Week 11-12: Integration & Performance Validation
- End-to-end tests with all Phase 1 features
- Performance benchmarks:
  - Final classes vs. Mockito-inline (target: 3x+ improvement)
  - Objects vs. mockkObject (target: 1000x+ improvement)
  - Top-level vs. mockkStatic (target: 100x+ improvement)
- Documentation finalization
- Public beta release

---

## Success Metrics

### Performance (Quantitative)
- ✅ **Final Classes**: Same speed as interface faking (<10ms overhead)
- ✅ **Singleton Objects**: 1000x+ faster than mockkObject
- ✅ **Top-level Functions**: 100x+ faster than mockkStatic
- ✅ **Compilation Time**: <5% increase for projects with 100+ fakes

### Developer Experience (Qualitative)
- ✅ **Zero Production Code Changes**: No `open`, no annotations
- ✅ **Type Safety**: No string-based references
- ✅ **Seamless Integration**: Works with existing test patterns
- ✅ **Clear Error Messages**: Compilation errors point to real issues

### Market Impact
- 🎯 **Migration Path**: Teams with slow tests adopt Fakt
- 🎯 **Performance Leader**: "Fastest Kotlin testing tool" positioning
- 🎯 **Community Buzz**: Benchmark articles, conference talks
- 🎯 **Early Adopters**: 5+ production teams using Phase 1 features

---

## Risk Mitigation

### Technical Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Call-site replacement too complex | Medium | High | Prototype early, consult Metro patterns |
| Performance claims don't materialize | Low | Critical | Benchmark continuously, conservative claims |
| Edge cases break compilation | Medium | Medium | Comprehensive test matrix, dogfooding |
| Kotlin compiler changes break plugin | Low | High | Test against multiple Kotlin versions |

### Market Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Developers don't care about performance | Low | High | Survey test suite times, showcase real savings |
| MockK improves performance | Low | Medium | Our solution is architecturally superior |
| Learning curve too steep | Medium | Medium | Excellent docs, migration guides |

---

## Testing Strategy

### GIVEN-WHEN-THEN Coverage

All Phase 1 features must follow the absolute standard:

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FinalClassFakeGenerationTest {

    @Test
    fun `GIVEN final class with methods WHEN generating fake THEN should compile and run`() = runTest {
        // Given
        val finalClass = createTestFinalClass("UserService") {
            method("getUser") { returns("User") }
        }

        // When
        val result = generator.generateFakeImplementation(finalClass)

        // Then
        assertTrue(result.compiles())
        assertTrue(result.isFinal == false) // Fake must be open
        assertTrue(result.hasMethod("getUser"))
    }
}
```

### Performance Test Suite

Create dedicated performance benchmarks:

```kotlin
@Test
fun `GIVEN singleton object fake WHEN compared to mockkObject THEN should be 1000x faster`() {
    val iterations = 1000

    // Baseline: mockkObject
    val mockkTime = measureTime {
        repeat(iterations) {
            mockkObject(MySingleton)
            every { MySingleton.getValue() } returns 42
            unmockkObject(MySingleton)
        }
    }

    // Fakt: compile-time fake
    val faktTime = measureTime {
        repeat(iterations) {
            val fake = fakeMySingleton { getValue { 42 } }
            fake.getValue()
        }
    }

    val speedup = mockkTime.inWholeMilliseconds / faktTime.inWholeMilliseconds
    assertTrue(speedup >= 1000, "Expected 1000x speedup, got ${speedup}x")
}
```

---

## Metro Alignment

### Relevant Metro Patterns

1. **Two-Phase Compilation**: FIR detection → IR generation
   - Phase 1 extends this for non-interface types
   - Call-site replacement is an IR transformation technique

2. **Context-driven Generation**: Metro's dependency injection uses similar call-site analysis
   - Study `MetroIrGenerationExtension` for IR transformation patterns
   - Use Metro's error handling for clear diagnostics

3. **Performance Focus**: Metro is production DI, performance-critical
   - Follow Metro's approach to zero-runtime-overhead generation
   - Benchmark-driven development

### Consultation Points

Before implementing call-site replacement:
- `/validate-metro-alignment` - Check if Metro has similar patterns
- Review Metro's IR transformation utilities
- Study Metro's approach to multi-phase code generation

---

## Documentation Deliverables

### For Developers
- [ ] **Migration Guide**: From MockK/Mockito to Fakt
- [ ] **Performance Comparison**: Benchmark data, charts
- [ ] **Usage Examples**: Real-world scenarios
- [ ] **Troubleshooting**: Common issues, solutions

### For Contributors
- [ ] **Implementation Guide**: How call-site replacement works
- [ ] **Testing Guide**: Performance test patterns
- [ ] **Metro Patterns**: Relevant techniques applied

### Public Facing
- [ ] **Announcement Blog Post**: "Fakt: 1000x Faster Kotlin Testing"
- [ ] **Benchmark Report**: Detailed performance analysis
- [ ] **Conference Talk Submission**: KotlinConf proposal

---

## Next Steps

1. ✅ Review this README
2. 🎯 Read detailed approach docs for each feature:
   - [Final Classes APPROACH.md](./finalClasses/APPROACH.md)
   - [Singleton Objects APPROACH.md](./singletonObjects/APPROACH.md)
   - [Top-level Functions APPROACH.md](./topLevelFunctions/APPROACH.md)
3. 🎯 Validate priorities with team/community
4. 🎯 Start with Final Classes (medium complexity, clear value)
5. 🎯 Prototype call-site replacement early (de-risk critical feature)

---

## References

- **Main Roadmap**: [../roadmap.md](../roadmap.md)
- **Research Document**: `Kotlin Test Fake Research Roadmap.md`
- **Performance Benchmarks**: [MockK Benchmark Article](https://medium.com/@_kevinb/benchmarking-mockk-avoid-these-patterns-for-fast-unit-tests-220fc225da55)
- **Metro Alignment**: [../../development/metro-alignment.md](../../development/metro-alignment.md)
- **Testing Guidelines**: [../../validation/testing-guidelines.md](../../validation/testing-guidelines.md)

---

**Phase 1 Success = Fakt becomes the performance leader in Kotlin testing.** 🚀
