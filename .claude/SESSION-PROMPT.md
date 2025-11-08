# 🚀 New Session: Continue FIR Full Feature Support Implementation

**Use this prompt to start a new Claude Code session and continue where we left off.**

---

## 📋 Quick Start Prompt for Claude

```
I'm continuing the implementation of full FIR mode support for the Fakt Kotlin compiler plugin.

CRITICAL CONTEXT:
1. Read this file first: .claude/docs/implementation/RESUME-FIR-IMPLEMENTATION.md
2. Then read the full plan: .claude/docs/implementation/fir-full-support-plan.md

CURRENT STATUS:
- Phase 3B.1 ✅ COMPLETE: Type parameter bounds extraction from FIR
- Phase 3B.2 ✅ COMPLETE: Removed GenericPattern from FIR metadata
- Phase 3B.3 ⏸️ PAUSED: Need to design proper FIR→IR communication API

CRITICAL ISSUE IDENTIFIED:
Current implementation has an architectural anti-pattern: the IR phase is re-analyzing
what FIR already validated by passing IrClass to existing pipeline. This violates the
"FIR analyzes, IR generates" principle.

NEXT STEPS (Phase 3B.3 - TDD Approach):
1. Start with tests: Create FirToIrMappingTest.kt with test cases from RESUME doc
2. Create metadata API: IrGenerationMetadata.kt (data-only, NO IR types)
3. Implement mapper: FirToIrMapper.kt (pure data transformation)
4. Update IR generation: Use ONLY mapped metadata, no IrClass.declarations access
5. Run tests and verify no IR re-analysis happens

KEY PRINCIPLES:
- Follow TDD: Write tests BEFORE implementation
- FIR analyzes, IR generates (NO IR analysis in generation)
- Data-only API: Use strings for types, not IrType
- No shortcuts: User explicitly rejected hardcoded solutions
- Metro alignment: Follow proven patterns

FILES TO CREATE:
- compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/fir/FirToIrMappingTest.kt
- compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/IrGenerationMetadata.kt
- compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FirToIrMapper.kt

FILES TO UPDATE:
- compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt

All code examples and test cases are in RESUME-FIR-IMPLEMENTATION.md.

Let's continue with Phase 3B.3, starting with the TDD approach.
```

---

## 📖 Background Reading Order

1. **START HERE**: `.claude/docs/implementation/RESUME-FIR-IMPLEMENTATION.md`
   - Detailed context of what's been done
   - The anti-pattern we discovered
   - Complete code examples for Phase 3B.3
   - Step-by-step TDD implementation guide

2. **FULL PLAN**: `.claude/docs/implementation/fir-full-support-plan.md`
   - Multi-phase roadmap (3B → 3C → 3D → 4 → 5)
   - Feature coverage analysis (119 @Fake declarations)
   - Success criteria for each phase
   - Timeline estimates

3. **PROJECT DOCS**: `CLAUDE.md` (root level)
   - Overall Fakt architecture
   - Testing standards (GIVEN-WHEN-THEN)
   - Development principles
   - Available commands

---

## 🎯 What Phase 3B.3 Achieves

### Problem
Current `generateFromFirMetadata()` passes `IrClass` instances to existing pipeline, which triggers IR analysis again (via `InterfaceAnalyzer.analyze(irClass)`). This duplicates work already done in FIR phase.

### Solution
Create a **data-only API** for FIR→IR communication:
- FIR extracts ALL metadata (complete)
- Mapper converts FIR data to IR generation metadata (pure transformation)
- IR generates code using ONLY metadata (no IrClass access)

### Result
- ✅ Clean FIR/IR separation
- ✅ No duplicate analysis
- ✅ Follows Metro pattern
- ✅ Type-safe code generation

---

## 🔍 Quick Verification Commands

### Check Current State
```bash
# Verify you're in the right directory
pwd  # Should be: ktfakes-prototype/ktfake

# Check git status
git status

# Check modified files
git diff --stat

# Verify Phase 3B.1 + 3B.2 changes compiled
./gradlew :compiler:compileKotlin --no-daemon
```

### Verify Phase 3B.1 + 3B.2 Completed
```bash
# Check bounds extraction (Phase 3B.1)
grep -A 5 "val bounds = typeParam.bounds.map" \
  compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FakeInterfaceChecker.kt

# Check GenericPattern removed (Phase 3B.2)
grep "val genericPattern" \
  compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FirFakeMetadata.kt
# Should return nothing (removed)
```

---

## 📝 Files Changed (Phase 3B.1 + 3B.2)

### Modified Files
1. **FakeInterfaceChecker.kt**
   - Lines 171-180: Class-level type parameter bounds extraction
   - Lines 283-302: Method-level type parameter bounds extraction
   - Removed: `analyzeGenericPattern()` function

2. **FirFakeMetadata.kt**
   - Removed: `GenericPattern` import
   - Removed: `genericPattern` field from `ValidatedFakeInterface`
   - Updated: Documentation explaining FIR→IR reconstruction

### New Files (Already Created)
1. `.claude/docs/implementation/fir-full-support-plan.md` - Complete roadmap
2. `.claude/docs/implementation/RESUME-FIR-IMPLEMENTATION.md` - Session resume guide
3. This file: `.claude/SESSION-PROMPT.md`

---

## ⚠️ Important Reminders

### Critical Anti-Pattern to Fix
The current IR generation passes `IrClass` to `processInterfaces()`, causing:
- Re-analysis of structure FIR already validated
- Violation of FIR/IR separation principle
- Duplication of work

**Phase 3B.3 fixes this** by creating a pure data API.

### Development Principles
1. **TDD First**: Write tests before implementation
2. **FIR Analyzes, IR Generates**: No IR structural analysis
3. **Data-Only API**: Use strings, not IR types
4. **No Shortcuts**: Build it correctly
5. **Metro Alignment**: Follow proven patterns

### Testing Standard (Always)
- GIVEN-WHEN-THEN naming
- Vanilla JUnit5 + kotlin-test
- `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`
- No custom BDD frameworks
- Isolated test instances

---

## 🎬 Example Session Start

**You**: *paste the Quick Start Prompt above*

**Claude should**:
1. Read RESUME-FIR-IMPLEMENTATION.md
2. Understand the anti-pattern issue
3. Start Phase 3B.3 with TDD:
   - Create FirToIrMappingTest.kt first
   - Run tests (should fail)
   - Create IrGenerationMetadata.kt
   - Create FirToIrMapper.kt
   - Run tests (should pass)
   - Update UnifiedFaktIrGenerationExtension.kt

---

## 📊 Progress Tracking

### Completed ✅
- Phase 3A: FIR checkers (FakeInterfaceChecker, FakeClassChecker)
- Phase 4.1-4.3: FIR→IR wiring (FaktSharedContext, FirMetadataStorage)
- Phase 3B.1: Type parameter bounds extraction
- Phase 3B.2: Remove GenericPattern from FIR

### Current 🔄
- Phase 3B.3: Design FIR→IR communication API (TDD)

### Next Up 📅
- Phase 3B.4: Add FIR diagnostic error reporting
- Phase 3B.5: Test class-level generics end-to-end
- Phase 3C: Method-level generics + FakeClassChecker enhancement
- Phase 3D: Advanced features (variance, source locations)
- Phase 4: Legacy cleanup + enable FIR by default
- Phase 5: Comprehensive FIR testing

---

## 🎯 Success Criteria for Phase 3B.3

When Phase 3B.3 is complete, these must be true:

1. ✅ `FirToIrMappingTest.kt` exists with 5+ test cases
2. ✅ All tests pass
3. ✅ `IrGenerationMetadata` contains NO IR types (strings only)
4. ✅ `FirToIrMapper` performs pure data transformation
5. ✅ `generateFakeFromMetadata()` uses ONLY metadata
6. ✅ No `IrClass.declarations` access in generation path
7. ✅ No calls to `InterfaceAnalyzer.analyze()` in FIR mode
8. ✅ Generated code still compiles (verify with sample)

---

## 🆘 If You Get Stuck

### Read These Files
1. `RESUME-FIR-IMPLEMENTATION.md` - Has complete code examples
2. `fir-full-support-plan.md` - Has the big picture
3. `CLAUDE.md` - Has project standards

### Common Issues
- **"Where do I start?"** → Read RESUME doc, start with tests
- **"What's the anti-pattern?"** → IR re-analyzing what FIR did
- **"How to fix it?"** → Create data-only mapper, no IrClass in generation
- **"What about generics?"** → Use string representations, reconstruct pattern

### Metro Reference
Check how Metro does FIR→IR communication:
- `metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/`
- Look for data classes passed between phases
- Note: They use shared context pattern (we already have this)

---

## 📞 Session Continuity

This session ended at token ~112k/200k after:
- Implementing Phase 3B.1 (bounds extraction)
- Implementing Phase 3B.2 (remove GenericPattern)
- Identifying the architectural anti-pattern
- Creating comprehensive documentation
- Pausing before Phase 3B.3 to resume fresh

**Next session should start with Phase 3B.3 using the TDD approach.**

Good luck! 🚀
