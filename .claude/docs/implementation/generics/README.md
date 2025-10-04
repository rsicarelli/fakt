# Generic Type Support Implementation

> **Status**: Planning Complete - Ready for Implementation ✅
> **Strategy**: Full IR Substitution with IrTypeSubstitutor
> **Timeline**: 2-3 weeks
> **Completion**: 0% (Planning: 100%)

## 📚 Documentation Index

### 🚀 Start Here

1. **[QUICK-START.md](./QUICK-START.md)** ⭐ **START HERE!**
   - Para começar implementação AGORA
   - Passo a passo da primeira semana
   - Checklist de validação
   - **Read First**: 15 min

2. **[ROADMAP.md](./ROADMAP.md)** - Executive Summary
   - Visão geral completa do projeto
   - Fases, métricas, file changes
   - Success criteria
   - **Read Second**: 10 min

---

### 📖 Implementation Phases

3. **[phase1-core-infrastructure.md](./phase1-core-infrastructure.md)** - Week 1
   - Create GenericIrSubstitutor.kt
   - Enhance TypeResolver
   - Remove generic filter
   - Tasks, tests, completion criteria

4. **[phase2-code-generation.md](./phase2-code-generation.md)** - Week 2
   - Update ImplementationGenerator
   - Update FactoryGenerator (reified)
   - Update ConfigurationDslGenerator
   - Integration with CodeGenerator

5. **[phase3-testing-integration.md](./phase3-testing-integration.md)** - Week 3
   - Full test matrix implementation
   - Edge case handling
   - Performance benchmarking
   - Documentation updates

---

### 🧪 Testing & Validation

6. **[test-matrix.md](./test-matrix.md)** - Comprehensive Test Scenarios
   - P0: Basic class-level generics (100% required)
   - P1: Method-level & mixed (95% required)
   - P2: Constraints & variance (90% required)
   - P3: Advanced edge cases (80% acceptable)
   - Multi-stage validation strategy

---

### 🔧 Technical References

7. **[technical-reference.md](./technical-reference.md)** - Deep Dive
   - IrTypeSubstitutor API guide
   - IrTypeParameterRemapper usage
   - Implementation patterns
   - Common pitfalls
   - Debugging tips
   - Quick reference cheat sheet

---

## 🎯 Quick Navigation

**I want to...**

| Goal | Document |
|------|----------|
| Start implementing NOW | [QUICK-START.md](./QUICK-START.md) |
| Understand overall strategy | [ROADMAP.md](./ROADMAP.md) |
| Work on Phase 1 | [phase1-core-infrastructure.md](./phase1-core-infrastructure.md) |
| Work on Phase 2 | [phase2-code-generation.md](./phase2-code-generation.md) |
| Work on Phase 3 | [phase3-testing-integration.md](./phase3-testing-integration.md) |
| Write tests | [test-matrix.md](./test-matrix.md) |
| Understand Kotlin IR APIs | [technical-reference.md](./technical-reference.md) |

---

## 📊 Current Status

### Phase 1: Core Infrastructure
- [ ] GenericIrSubstitutor.kt created
- [ ] TypeResolver enhanced
- [ ] Generic filter removed
- [ ] Integration test passing

### Phase 2: Code Generation
- [ ] ImplementationGenerator updated
- [ ] FactoryGenerator with reified params
- [ ] ConfigurationDslGenerator generic DSL
- [ ] CodeGenerator integration complete

### Phase 3: Testing & Integration
- [ ] P0 tests implemented & passing
- [ ] P1 tests implemented & passing
- [ ] P2 tests implemented & passing
- [ ] Edge cases handled
- [ ] Performance benchmarks meet targets
- [ ] Documentation updated
- [ ] Production validation complete

---

## 🎯 Success Metrics

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| P0 Tests Pass Rate | 100% | 0% | ⏳ Pending |
| P1 Tests Pass Rate | 95% | 0% | ⏳ Pending |
| P2 Tests Pass Rate | 90% | 0% | ⏳ Pending |
| Compilation Overhead | <10% | - | ⏳ Pending |
| Documentation Complete | 100% | 100% | ✅ Done |

---

## 📋 Todo List Overview

Tracking in main todo list:

1. ✅ Review documentation
2. ⏳ Phase 1.1: GenericIrSubstitutor
3. ⏳ Phase 1.2: TypeResolver
4. ⏳ Phase 1.3: Remove filter
5. ⏳ Phase 1.4: Integration test
6. ⏳ Phase 2.1: ImplementationGenerator
7. ⏳ Phase 2.2: FactoryGenerator
8. ⏳ Phase 2.3: ConfigurationDslGenerator
9. ⏳ Phase 2.4: Integration test
10. ⏳ Phase 3.1: Test matrix (P0-P2)
11. ⏳ Phase 3.2: Edge cases
12. ⏳ Phase 3.3: Performance
13. ⏳ Phase 3.4: Examples
14. ⏳ Phase 3.5: Docs
15. ⏳ Final validation

---

## 🔗 Related Documentation

### Project Documentation
- [Testing Guidelines](../../validation/testing-guidelines.md) - THE ABSOLUTE STANDARD
- [Metro Alignment](../../development/metro-alignment.md) - Architectural patterns
- [Current Status](../../implementation/current-status.md) - Project progress
- [Decision Tree](../../development/decision-tree.md) - When to use what

### Kotlin APIs
- Local: `kotlin/compiler/ir/backend.common/src/.../IrTypeSubstitutor.kt`
- Query: `/consult-kotlin-api IrTypeSubstitutor`

### Reference Implementations
- Metro DI: `metro/compiler/src/.../ir/MetroIrGenerationExtension.kt`
- Validation: `/validate-metro-alignment`

---

## 🚀 Getting Started

```bash
# 1. Read documentation (1 hour)
cd .claude/docs/implementation/generics/
cat QUICK-START.md
cat ROADMAP.md
cat technical-reference.md

# 2. Start implementation
# Follow QUICK-START.md step by step

# 3. Track progress
# Use todo list to mark completion
```

---

## 💡 Key Insights

### Why Full IR Substitution?

1. **Type Safety**: Preserves full type information at compile time
2. **Metro Alignment**: Uses proven patterns from production DI framework
3. **Developer Experience**: `fakeRepository<User> {}` is intuitive
4. **Future-Proof**: Supports all generic scenarios (class, method, mixed)

### Why Not Type Erasure?

1. **Type Safety Lost**: `Repository<Any>` loses compile-time checks
2. **Manual Casting**: Developers must cast manually (bad UX)
3. **Not MAP Quality**: Minimum Awesome Product requires better
4. **Already Have Infrastructure**: GenericPatternAnalyzer exists

### Why IrTypeSubstitutor?

1. **Official Kotlin API**: Maintained by JetBrains
2. **Battle-Tested**: Used in kotlinx.serialization, Compose
3. **Handles Edge Cases**: Star projections, variance, constraints
4. **Metro Uses It**: Proven in production DI scenarios

---

## 🚨 Critical Rules

1. **TDD**: Write tests first (GIVEN-WHEN-THEN)
2. **Metro Patterns**: Check alignment before major decisions
3. **Type Safety**: Validate at use-site (Stage 3 testing)
4. **Incremental**: Complete one phase before next
5. **Documentation**: Update as you go

---

## 🎓 Learning Resources

### Kotlin IR
- IrTypeSubstitutor: [technical-reference.md](./technical-reference.md)
- Metro patterns: `metro/compiler/src/`
- Kotlin source: `kotlin/compiler/ir/`

### Testing
- kotlin-compile-testing: Multi-stage validation
- Test matrix: [test-matrix.md](./test-matrix.md)
- BDD patterns: [Testing Guidelines](../../validation/testing-guidelines.md)

---

**Ready to start?** → [QUICK-START.md](./QUICK-START.md) 🚀

---

Last Updated: January 2025
Status: Planning Complete ✅
Next Step: Phase 1, Task 1.1 - Create GenericIrSubstitutor.kt
