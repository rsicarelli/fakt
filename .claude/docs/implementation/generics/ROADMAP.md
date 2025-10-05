# 🚀 Generic Type Support - Full Production Roadmap

> **Strategy**: Full IR Substitution with IrTypeSubstitutor
> **Timeline**: 2-3 weeks
> **Status**: Planning Complete - Ready for Implementation
> **Last Updated**: January 2025

## 📋 Quick Reference

- **Phase 1**: [Core Infrastructure](./phase1-core-infrastructure.md) - Week 1 ✅
- **Phase 2**: [Code Generation](./phase2-code-generation.md) - Week 2 ✅
- **Phase 3**: [Testing & Integration](./phase3-testing-integration.md) - Week 3 ✅
- **Phase 4**: [SAM Interface Support](./phase4-sam-interfaces.md) - Week 4 (80% Complete) ⏳
- **Technical Reference**: [IrTypeSubstitutor Guide](./technical-reference.md)
- **Test Matrix**: [Comprehensive Test Scenarios](./test-matrix.md)

## 🎯 Executive Summary

### Current State
- ✅ GenericPatternAnalyzer exists and categorizes patterns correctly
- ✅ InterfaceAnalyzer extracts type parameters
- ✅ Two-phase FIR → IR architecture ready
- ❌ Line 189 in UnifiedFaktIrGenerationExtension skips all generics
- ❌ Type erasure (Any) used instead of proper substitution

### Target State
- ✅ Full generic support: `interface Repository<T>` → `class FakeRepositoryImpl<T>`
- ✅ Reified factory functions: `inline fun <reified T> fakeRepository()`
- ✅ Type-safe DSL configuration
- ✅ 95%+ compilation success rate
- ✅ <10% compilation time overhead

## 📊 Implementation Phases

### Phase 1: Core Infrastructure (Week 1)
**Goal**: Build GenericIrSubstitutor and enhance TypeResolver

**Tasks**:
1. Create `GenericIrSubstitutor.kt` with IrTypeSubstitutor integration
2. Enhance `TypeResolver.kt` with substitution methods
3. Remove generic filter in `UnifiedFaktIrGenerationExtension.kt:189`
4. Unit tests for basic substitution

**Deliverable**: Generic interfaces no longer skipped, basic substitution working

### Phase 2: Code Generation (Week 2)
**Goal**: Update all generators to produce generic code

**Tasks**:
1. Update `ImplementationGenerator.kt` to generate `class Fake<T> : Interface<T>`
2. Update `FactoryGenerator.kt` with reified factory functions
3. Update `ConfigurationDslGenerator.kt` for generic DSL
4. Integration with CodeGenerator

**Deliverable**: Generated code preserves type parameters

### Phase 3: Testing & Integration (Week 3)
**Goal**: Comprehensive testing and edge case handling

**Tasks**:
1. Implement full test matrix with kotlin-compile-testing
2. Handle edge cases (star projections, recursive generics)
3. Performance benchmarking
4. Documentation updates

**Deliverable**: Production-ready generic support

### Phase 4: SAM Interface Support (Week 4) ⭐ BONUS!
**Goal**: Validate and complete SAM (fun interface) support

**Discovery**: SAM support came 80% "for free" from Phases 1-3 infrastructure!

**Tasks**:
1. Fix 2 edge case bugs (varargs, star projections)
2. Run and validate 77 SAM tests (95%+ target)
3. Code quality review (ktlint, naming conventions)
4. Final documentation update

**Deliverable**: 88 SAM interfaces with production-quality fakes

**Key Insight**: This phase demonstrates the ROI of solid architecture. By building
a robust generic type system in Phases 1-3, SAM interface support required minimal
additional effort. **Investment in quality foundations yields exponential returns.**

## 🎯 Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Compilation Success | 95% | kotlin-compile-testing pass rate |
| Type Safety | 100% | Use-site type-check success |
| Performance Impact | <10% | Gradle build benchmarks |
| Test Coverage | >90% | P0 + P1 scenarios |

## 🚨 Critical Dependencies

1. **Kotlin Compiler APIs**
   - `IrTypeSubstitutor` (org.jetbrains.kotlin.ir.types)
   - `IrTypeParameterRemapper` (org.jetbrains.kotlin.ir.util)
   - Validate with `/consult-kotlin-api IrTypeSubstitutor`

2. **Existing Infrastructure**
   - GenericPatternAnalyzer (already exists)
   - InterfaceAnalyzer (needs minor updates)
   - Two-phase compilation pipeline (ready)

3. **Testing Framework**
   - kotlin-compile-testing for multi-stage validation
   - JUnit5 with GIVEN-WHEN-THEN pattern

## 📁 File Changes Summary

### New Files (3)
- `compiler/src/main/kotlin/.../ir/GenericIrSubstitutor.kt`
- `compiler/src/test/kotlin/.../ir/GenericIrSubstitutorTest.kt`
- `compiler/src/test/kotlin/.../GenericFakeGenerationTest.kt`

### Modified Files (7)
- `UnifiedFaktIrGenerationExtension.kt` - Remove filter
- `InterfaceAnalyzer.kt` - Allow generics
- `TypeResolver.kt` - Add substitution
- `ImplementationGenerator.kt` - Generic classes
- `FactoryGenerator.kt` - Reified factories
- `ConfigurationDslGenerator.kt` - Generic DSL
- `CodeGenerator.kt` - Integration

## 🧪 Test Matrix Overview

### P0: Basic Class-Level Generics
```kotlin
interface Repository<T> { fun save(item: T): T }
→ class FakeRepositoryImpl<T> : Repository<T>
→ inline fun <reified T> fakeRepository()
```

### P1: Method-Level & Mixed Generics
```kotlin
interface Processor<T> { fun <R> transform(item: T): R }
→ Handle both class-level T and method-level R
```

### P2: Constraints & Variance
```kotlin
interface NumberService<T : Number>
interface Producer<out T>
```

### P3: Advanced Edge Cases
```kotlin
fun process(items: List<*>) // Star projections
interface Node<T : Node<T>> // Recursive generics
```

## 🔗 Related Documentation

- [Metro Alignment](./../development/metro-alignment.md)
- [Testing Guidelines](./../validation/testing-guidelines.md)
- [Current Status](./../current-status.md)
- [Decision Tree](./../development/decision-tree.md)

## 📝 Next Steps

1. Review all phase documents
2. Create detailed todo list
3. Start with Phase 1, Task 1: GenericIrSubstitutor.kt
4. TDD approach: write tests first
5. Validate with Metro patterns

---

**Note**: Este roadmap usa a estratégia "Full Production" porque:
- Fakt é um projeto MAP (Minimum Awesome Product), não MVP
- Type safety é não negociável
- Já temos a infraestrutura (GenericPatternAnalyzer)
- IrTypeSubstitutor é a API oficial do Kotlin para isso
