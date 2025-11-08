# TODO Audit - Compiler Module

> Internal documentation categorizing TODO comments with implementation rationale.
> Last updated: 2025-01-08

## Summary

- **Total TODOs**: 21 comments
- **Real Development Tasks**: 15
- **Code Generation Defaults**: 6 (not actionable)
- **Obsolete**: 1 (delete)
- **Actionable**: 14

---

## Category 1: Delete (1)

### ✅ Obsolete - Already Implemented

**UnifiedFaktIrGenerationExtension.kt:227**
```kotlin
// TODO: Handle classes (not yet implemented)
```
**Status**: DELETE - Classes are handled (lines 213-233)

---

## Category 2: FIR API Stability (4) - Post-Kotlin 2.1

### Not Needed Until FIR APIs Stabilize

#### Type Rendering (2 occurrences)
**FakeInterfaceChecker.kt:217, 288**
```kotlin
// TODO: Implement proper ConeType→String rendering
```
**Current**: Uses `coneType.toString()` → produces `kotlin/Foo` (invalid syntax)
**Workaround**: `FirToIrTransformer.sanitizeTypeBound()` fixes it
**Blocker**: FIR ConeType rendering APIs unstable across Kotlin versions
**Needed**: No - workaround is sufficient
**Roadmap Hint**: Consider if Kotlin 2.1+ provides stable rendering API

---

#### Source Location Extraction (2 occurrences)
**FakeInterfaceChecker.kt:348, 355**
```kotlin
// TODO: Implement full source location extraction using proper KtSourceElement API
```
**Current**: Uses `FirSourceLocation.UNKNOWN`
**Impact**: Error messages less precise but functional
**Blocker**: KtSourceElement APIs vary by Kotlin version
**Needed**: No - current error reporting is adequate
**Roadmap Hint**: Low priority enhancement for better DX

---

## Category 3: Validation (2) - Edge Cases

### Not Critical for MVP

#### External/Expect Declarations
**FakeInterfaceChecker.kt:87**
```kotlin
// TODO: Check for external/expect declarations
```
**Needed**: Maybe - for KMP expect/actual support
**Effort**: Medium - requires FIR expect/actual resolution
**Roadmap Hint**: Phase 2 - KMP maturity (after v1.0)

---

#### Empty Interface Check
**FakeInterfaceChecker.kt:88**
```kotlin
// TODO: Validate interface has at least one member
```
**Needed**: No - marker interfaces are valid (e.g., `Serializable`)
**Rationale**: Empty interfaces compile and may be intentional
**Action**: Document why we allow empty interfaces, remove TODO

---

## Category 4: Telemetry (5) - Nice to Have

### Low Priority - Metrics Only

#### Import Count Tracking (4 occurrences)
**UnifiedFaktIrGenerationExtension.kt:433, 531, 640, 727**
```kotlin
importCount = 0, // TODO: Track import count from ImportResolver
```
**Needed**: No - doesn't affect code generation
**Value**: DEBUG/TRACE log polish only
**Effort**: Low - ImportResolver has the data
**Roadmap Hint**: Telemetry polish milestone (post-v1.0)

---

#### Class Type Parameter Tracking
**UnifiedFaktIrGenerationExtension.kt:722**
```kotlin
typeParamCount = 0, // TODO: Track class type parameters
```
**Needed**: No - telemetry only
**Effort**: Low - available in ClassAnalysis
**Roadmap Hint**: Same as import count tracking

---

## Category 5: Performance (1) - Incremental Compilation

### Medium Priority - Build Performance

#### FIR Mode Cache Support
**UnifiedFaktIrGenerationExtension.kt:537**
```kotlin
// TODO: Add cache support for FIR mode (skip for now)
```
**Current**: Cache only works in legacy IR mode
**Impact**: FIR mode misses incremental compilation benefits
**Needed**: Yes - for production performance parity
**Effort**: Medium - signature computation for FIR metadata
**Blocker**: Need stable signature strategy for FIR types
**Roadmap Hint**: Performance optimization milestone (v1.1-v1.2)

---

## Category 6: Missing Features (2) - Quick Wins

### Should Implement Soon

#### Class Validation
**UnifiedFaktIrGenerationExtension.kt:608**
```kotlin
// TODO: add class-specific validation if needed
```
**Needed**: Yes - classes lack validateAndLogPattern call
**Impact**: No generic pattern warnings for classes
**Effort**: Low - copy interface validation pattern
**Roadmap Hint**: Include in next cleanup pass

---

#### Legacy Default Value Rendering
**IrAnalysisHelper.kt:62**
```kotlin
defaultValueCode = null, // TODO - render IR default value if needed for legacy path
```
**Needed**: No if deprecating legacy mode, Yes if keeping long-term
**Impact**: Only affects legacy IR discovery mode
**Effort**: High - IR expression rendering is complex
**Roadmap Hint**: Skip if legacy mode deprecated in v1.4/v2.0

---

## Category 7: Code Generation Defaults (6) - Not TODOs

### Runtime Placeholders - No Action Needed

**TypeResolver.kt:206, 354, 364, 386, 424**
```kotlin
"TODO(\"Nothing type has no values\")"
"TODO(\"Unknown type\")"
"TODO(\"Implement default for $className\")"
"{ TODO(\"Function not implemented\") }"
"TODO(\"Empty enum $className\")"
```
**Rationale**: These are generated code defaults for types with no sensible values
**Action**: None - these are correct runtime placeholders
**Example**: `Nothing` type truly has no values; `TODO()` is the right default

---

## Roadmap Hints

### v1.0 (Current) - Stability First
- ✅ Skip FIR API-dependent features (type rendering, source locations)
- ✅ Skip telemetry polish
- ✅ Current workarounds are production-ready

### v1.1-v1.2 - Performance & Polish
- **FIR mode cache support** (incremental compilation parity)
- **Class validation** (quick win)
- **Telemetry enhancements** (import/type param tracking)

### v1.4-v2.0 - Post-Legacy Deprecation
- Decide on legacy mode IR default value rendering
- Revisit FIR API features if Kotlin 2.1+ stabilizes APIs

### v2.0+ - KMP Maturity
- **External/expect declaration support** (KMP advanced scenarios)

---

## Action Items

1. **Delete**: Line 227 obsolete comment
2. **Document**: Why empty interfaces are allowed (line 88)
3. **Implement**: Class validation (line 608) - low effort, good DX
4. **Defer**: Everything else to appropriate milestone

---

## Notes

- **FIR API Dependency**: 4 TODOs blocked by upstream Kotlin compiler API stability
- **Telemetry**: 5 TODOs are metrics-only (zero functional impact)
- **Code Defaults**: 6 "TODOs" are actually correct runtime behavior
- **Real Work**: Only 3-4 actionable items for near-term roadmap
