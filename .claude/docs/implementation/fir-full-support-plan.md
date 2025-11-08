# FIR Mode Full Feature Support - Implementation Plan

**Status**: Phase 3B in progress (Paused at 3B.3)
**Last Updated**: 2025-01-04
**Target**: 95%+ feature coverage for all 119 @Fake declarations in kmp-single-module

---

## Executive Summary

### Current Status (Completed)
- ✅ **Phase 3A**: FIR checkers created (FakeInterfaceChecker, FakeClassChecker)
- ✅ **Phase 4.1-4.3**: FIR→IR wiring complete, metadata storage works
- ✅ **Phase 3B.1**: Type parameter bounds extraction from FIR (class + method level)
- ✅ **Phase 3B.2**: Removed GenericPattern from FIR metadata (will be reconstructed in IR)

### Critical Issue Identified ⚠️
**Anti-Pattern**: Current `generateFromFirMetadata()` passes `IrClass` to existing pipeline, causing IR phase to re-analyze what FIR already validated. This violates FIR/IR separation.

**Correct Approach**:
1. FIR phase extracts **ALL** metadata (complete structural analysis)
2. Create proper **FIR→IR Communication API** (data-only, no IR types)
3. IR phase **ONLY** uses FIR metadata to generate code (NO analysis)
4. Follow **TDD** - write tests first for each phase

### Coverage Analysis
- **119 @Fake declarations** in kmp-single-module sample
  - 95 interfaces (80%), 24 classes (20%)
  - 64 SAM interfaces (54%)
- **Current FIR readiness**: ~30% (basic interfaces only)
- **Target**: 95%+ feature completeness

---

## 🎯 Phase 3B: Core Generic Support (Priority 1 - CURRENT)

**Goal**: Enable 70% of interfaces by supporting class-level generics + constraints
**Estimated Effort**: 2-3 days
**Status**: **IN PROGRESS** (3B.1 ✅, 3B.2 ✅, 3B.3 paused)

### Remaining Tasks

#### **Phase 3B.3: Design Proper FIR→IR Communication API** 🚨 NEXT
**Problem**: Current approach re-analyzes in IR what FIR already validated.

**Solution**: Create data-only API for FIR→IR communication:

```kotlin
// FIR Phase Output (already exists, needs enhancement)
data class ValidatedFakeInterface(
    val classId: ClassId,
    val simpleName: String,
    val packageName: String,
    val typeParameters: List<FirTypeParameterInfo>,  // ✅ Already has bounds
    val properties: List<FirPropertyInfo>,           // ✅ Already complete
    val functions: List<FirFunctionInfo>,            // ✅ Already has method generics
    val sourceLocation: FirSourceLocation,           // ✅ Already exists
)

// NEW: IR Phase needs this information for code generation
data class IrGenerationMetadata(
    val fqName: String,                              // Fully qualified name
    val simpleName: String,                          // Simple class name
    val packageName: String,                         // Package name
    val typeParameters: List<TypeParameterMetadata>, // Class-level generics
    val properties: List<PropertyMetadata>,          // All properties
    val functions: List<FunctionMetadata>,           // All functions
    val imports: Set<String>,                        // Required imports
    val genericPattern: GenericPatternMetadata,      // Classification
)

// Metadata types (no IR types, pure data)
data class TypeParameterMetadata(
    val name: String,                                // "T", "K", "V"
    val bounds: List<String>,                        // ["Comparable<T>"]
    val variance: Variance?,                         // in/out/null
)

data class PropertyMetadata(
    val name: String,
    val type: String,                                // Fully rendered type string
    val isMutable: Boolean,
    val isNullable: Boolean,
)

data class FunctionMetadata(
    val name: String,
    val parameters: List<ParameterMetadata>,
    val returnType: String,                          // Fully rendered type string
    val isSuspend: Boolean,
    val isInline: Boolean,
    val typeParameters: List<TypeParameterMetadata>, // Method-level generics
)

data class ParameterMetadata(
    val name: String,
    val type: String,                                // Fully rendered type string
    val hasDefaultValue: Boolean,
    val isVararg: Boolean,
)

// Generic pattern classification (no IrTypeParameter)
sealed class GenericPatternMetadata {
    object NoGenerics : GenericPatternMetadata()
    data class ClassLevel(
        val typeParameterNames: List<String>,        // ["T", "K", "V"]
        val bounds: Map<String, List<String>>,       // "T" -> ["Comparable<T>"]
    ) : GenericPatternMetadata()

    data class MethodLevel(
        val methodsWithGenerics: List<String>,       // Method names
    ) : GenericPatternMetadata()

    data class Mixed(
        val classTypeParameterNames: List<String>,
        val classBounds: Map<String, List<String>>,
        val methodsWithGenerics: List<String>,
    ) : GenericPatternMetadata()
}
```

**Implementation Steps**:
1. **Write Tests First (TDD)**:
   ```kotlin
   // FirToIrMappingTest.kt
   @Test
   fun `GIVEN simple interface WHEN converting to IR metadata THEN all fields mapped`()

   @Test
   fun `GIVEN generic interface WHEN converting THEN type parameters preserved`()

   @Test
   fun `GIVEN interface with bounds WHEN converting THEN bounds rendered correctly`()
   ```

2. **Create Mapping Layer**:
   ```kotlin
   // FirToIrMapper.kt
   class FirToIrMapper {
       fun mapToIrMetadata(
           validatedInterface: ValidatedFakeInterface,
       ): IrGenerationMetadata {
           // Pure data transformation, no IR analysis
       }
   }
   ```

3. **Update IR Generation**:
   ```kotlin
   private fun generateFromFirMetadata(
       moduleFragment: IrModuleFragment,
       pluginContext: IrPluginContext,
   ) {
       val validatedInterfaces = sharedContext.metadataStorage.getAllInterfaces()

       // Map FIR data to IR generation metadata
       val mapper = FirToIrMapper()
       val irMetadata = validatedInterfaces.map { mapper.mapToIrMetadata(it) }

       // Generate code ONLY from metadata (no IrClass analysis)
       irMetadata.forEach { metadata ->
           generateFakeImplementation(metadata, moduleFragment)
       }
   }
   ```

**Success Criteria**:
- ✅ No `IrClass.declarations` access in IR generation
- ✅ All structural info comes from FIR metadata
- ✅ Tests pass for mapping layer
- ✅ Generated code identical to legacy mode

---

#### **Phase 3B.4: Add FIR Diagnostic Error Reporting**
**Current**: TODOs with commented-out `reporter.reportOn()` calls
**Target**: Professional error messages with source locations

**Files to Update**:
- `FakeInterfaceChecker.kt` (lines 70, 77, 84)
- `FakeClassChecker.kt` (lines 59, 67, 74, 81)
- `FirMetroErrors.kt` (already has message constants)

**Implementation**:
```kotlin
// Replace TODOs with actual error reporting
if (declaration.classKind != ClassKind.INTERFACE) {
    reporter.reportOn(
        source,
        FirMetroErrors.FAKE_MUST_BE_INTERFACE,
        context
    )
    return
}
```

**Tests**:
```kotlin
@Test
fun `GIVEN sealed interface WHEN checked THEN reports FAKE_CANNOT_BE_SEALED error`()

@Test
fun `GIVEN local interface WHEN checked THEN reports FAKE_CANNOT_BE_LOCAL error`()
```

---

#### **Phase 3B.5: End-to-End Testing with Class-Level Generics**

**Test Interfaces** (from kmp-single-module):
1. `SimpleRepository<T>` - Single type parameter
2. `KeyValueStore<K, V>` - Two type parameters
3. `TripleStore<A, B, C>` - Three type parameters
4. `SortedRepository<T : Comparable<T>>` - Constrained generic

**Test Plan**:
```kotlin
// Enable FIR mode in sample
fakt {
    useFirAnalysis.set(true)
}

// Verify generated fakes compile and work
val repo = fakeSimpleRepository<User> {
    save { user -> user }
}

val kv = fakeKeyValueStore<String, Int> {
    get { key -> 42 }
}

val sorted = fakeSortedRepository<User> {
    sort { emptyList() }
}
```

**Success Criteria**:
- ✅ 19 generic interfaces generate correctly
- ✅ Generated code compiles without errors
- ✅ All tests pass
- ✅ ~70% of sample interfaces working

---

## 🔧 Phase 3C: Method-Level Generics + Class Enhancement (Priority 2)

**Goal**: Enable 85% of interfaces + 50% of classes
**Estimated Effort**: 2-3 days
**Status**: Pending Phase 3B completion

### Tasks

1. **Method-Level Generic Support**
   - Extract method-level type parameters (already done in FirFunctionInfo)
   - IR phase: Detect MethodLevelGenerics pattern
   - Generate identity functions for method generics
   - Test: `DataProcessor`, `fun <T> transform(input: T): T`

2. **Mixed Generics Pattern**
   - Detect interfaces with both class + method type parameters
   - GenericPatternMetadata.Mixed classification
   - Combine class generic replacement + method identity functions
   - Test: `GenericRepository<T>` with `fun <R> map(item: T): R`

3. **FakeClassChecker Enhancement**
   - Extract abstract vs open members (FakeClassChecker.kt:109 TODO)
   - Separate: `abstractProperties`, `openProperties`, `abstractMethods`, `openMethods`
   - Use FIR visibility/modality APIs
   - Store in ValidatedFakeClass

4. **Class Inheritance Analysis**
   - Detect superclass types
   - Extract inherited abstract/open members
   - Handle interface implementations
   - Test: `FileRepository extends AbstractRepository`

**Success Criteria**:
- ✅ 8 method-generic interfaces work
- ✅ 12+ classes (abstract/open) generate fakes
- ✅ ~85% of interfaces + 50% of classes covered

---

## 🚀 Phase 3D: Advanced Features (Priority 3)

**Goal**: Achieve 95%+ feature completeness
**Estimated Effort**: 2-3 days
**Status**: Pending Phase 3B + 3C

### Tasks

1. **Variance Annotation Preservation** (in/out)
   - Extract variance from FirTypeParameter
   - Preserve in generated fake signatures
   - Test: `Producer<out T>`, `Consumer<in T>`
   - Affects: 12 SAM interfaces

2. **Source Location Extraction** (FakeInterfaceChecker.kt:368)
   - Investigate KtSourceElement API
   - Extract file path, line/column from FirClass.source
   - Return proper FirSourceLocation (not UNKNOWN)
   - Use in error messages

3. **Protected Member Support**
   - Detect protected visibility
   - Generate fakes with protected members
   - Test: `BaseFragment` with protected lifecycle hooks

4. **Advanced Type Features**
   - Star projections (`*`)
   - Recursive generics (already works via bounds)
   - Nested generic types
   - Test edge cases

5. **Comprehensive Error Validation**
   - External/expect declaration detection
   - Minimum member count validation
   - Better error messages with suggestions

**Success Criteria**:
- ✅ Variance preserved in generated code
- ✅ Source locations accurate in error messages
- ✅ Protected members generate correctly
- ✅ Edge cases handled gracefully
- ✅ 95%+ of sample features working

---

## 🧹 Phase 4: Legacy Cleanup + Default FIR

**Goal**: Remove IR discovery code, enable FIR by default
**Estimated Effort**: 1 day
**Status**: Pending Phase 3 completion

### Tasks

1. **Remove IR Discovery Code**
   - Delete `generateFromIrDiscovery()` function
   - Remove InterfaceAnalyzer IR-based logic
   - Keep only `generateFromFirMetadata()` path
   - Clean up dual-mode conditionals

2. **Enable FIR Mode by Default**
   - Change `FaktOptions.useFirAnalysis` default to `true`
   - Update documentation
   - Gradle plugin default: `useFirAnalysis.set(true)`

3. **Remove Feature Flag**
   - After validation, remove `useFirAnalysis` option entirely
   - Single code path: FIR→IR always
   - Simplify architecture

4. **Performance Optimization**
   - Benchmark FIR mode vs legacy (if kept)
   - Optimize metadata storage (thread safety already done)
   - Profile compilation time impact

**Success Criteria**:
- ✅ Legacy IR discovery removed
- ✅ FIR mode enabled by default
- ✅ Single clean code path
- ✅ Performance validated

---

## 🧪 Phase 5: Comprehensive FIR Testing

**Goal**: Ensure FIR mode reliability with dedicated tests
**Estimated Effort**: 2 days
**Status**: Pending Phase 4 completion

### Tasks

1. **FIR Mode Unit Tests**
   - Test FakeInterfaceChecker validation logic
   - Test FirMetadataStorage thread safety
   - Test GenericPatternMetadata classification
   - Test error diagnostic reporting
   - Test FirToIrMapper conversion

2. **FIR vs IR Comparison Tests**
   - Generate fakes with FIR mode
   - Generate fakes with legacy mode (before removal)
   - Compare outputs bytecode-for-bytecode
   - Ensure identical behavior

3. **Error Message Tests**
   - Test all validation errors (sealed, local, wrong kind, etc.)
   - Verify source locations accurate
   - Test suggestion quality

4. **Integration Tests**
   - Full kmp-single-module build with FIR mode
   - All 119 @Fake declarations must work
   - All 90+ tests must pass
   - No compilation errors

5. **Edge Case Testing**
   - Complex nested generics
   - Multiple inheritance levels
   - Cross-module @Fake references (if supported)
   - Large interfaces (50+ members)

**Success Criteria**:
- ✅ 50+ FIR-specific unit tests
- ✅ 100% kmp-single-module sample passes with FIR mode
- ✅ All error messages tested
- ✅ Edge cases covered

---

## 📊 Success Metrics & Validation

### Phase 3B Success (70% coverage)
- 19 class-generic interfaces work
- 15+ constraint-based interfaces work
- Error diagnostics functional
- ~66 interfaces total

### Phase 3C Success (85% coverage)
- 8 method-generic interfaces work
- 12+ classes generate fakes
- ~80 interfaces + 12 classes

### Phase 3D Success (95% coverage)
- 12 variance interfaces work
- All edge cases handled
- ~90 interfaces + 20 classes

### Phase 5 Success (100% quality)
- All 119 @Fake declarations work
- All 90+ tests pass
- Zero compilation errors
- Performance validated

---

## 🔄 Development Principles

1. **FIR/IR Separation**: FIR analyzes, IR generates (NO IR analysis)
2. **TDD**: Write tests before implementation
3. **Metro Alignment**: Follow proven patterns from Metro compiler plugin
4. **Type Safety**: All generic handling preserves type safety
5. **Error Quality**: Professional error messages like Metro/Kotlin compiler
6. **No Shortcuts**: User explicitly rejected hardcoded solutions

---

## ⏱️ Timeline Estimate

- **Phase 3B**: 2-3 days (core generics + diagnostics + proper API)
- **Phase 3C**: 2-3 days (method generics + classes)
- **Phase 3D**: 2-3 days (advanced features)
- **Phase 4**: 1 day (cleanup + default FIR)
- **Phase 5**: 2 days (comprehensive testing)

**Total**: ~10-14 days for complete FIR mode implementation covering all 119 @Fake declarations.

---

## 📝 Files Modified So Far

### Completed (Phase 3B.1 + 3B.2)
1. **FakeInterfaceChecker.kt** (lines 171-180, 283-302)
   - ✅ Type parameter bounds extraction (class + method level)
   - ✅ Removed analyzeGenericPattern function

2. **FirFakeMetadata.kt**
   - ✅ Removed GenericPattern import
   - ✅ Removed genericPattern field from ValidatedFakeInterface

### Next to Modify (Phase 3B.3)
1. **Create**: `FirToIrMapper.kt` (new file)
2. **Create**: `FirToIrMappingTest.kt` (new file)
3. **Create**: `IrGenerationMetadata.kt` (new file with all metadata types)
4. **Update**: `UnifiedFaktIrGenerationExtension.kt::generateFromFirMetadata()`

---

## 🎯 Immediate Next Session Actions

1. **Start with TDD**: Write `FirToIrMappingTest.kt` first
2. **Create Metadata API**: Define `IrGenerationMetadata` and related types
3. **Implement Mapper**: Create `FirToIrMapper` with pure data transformation
4. **Update IR Generation**: Use only mapped metadata, no IrClass analysis
5. **Validate**: Run tests, verify no IR re-analysis

**Critical**: Follow the "FIR analyzes, IR generates" principle strictly.
