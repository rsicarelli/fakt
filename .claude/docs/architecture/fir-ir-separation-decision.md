# FIR/IR Phase Separation - Architecture Decision

> **Status**: Approved for implementation
> **Date**: January 2025
> **Author**: Research based on Metro patterns analysis

## 🎯 Decision

Refactor Fakt to properly separate FIR (analysis/validation) and IR (code generation) phases, following Metro's proven two-phase architecture.

---

## 📊 Current State Analysis

### ❌ Problem: Everything in IR Phase

Currently, **all work happens in IR phase**:

```kotlin
// FaktFirExtensionRegistrar.kt - EMPTY
override fun ExtensionStorage.configurePlugin() {
    // FIR phase: Extension registration placeholder
    // The FIR API is complex and evolving. For now, the main annotation detection
    // happens in the IR phase which has a more stable API surface.
}
```

```kotlin
// UnifiedFaktIrGenerationExtension.kt - DOES EVERYTHING
override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    // ❌ Discovery (should be FIR)
    val fakeInterfaces = discoverFakeInterfaces(moduleFragment)

    // ❌ Validation (should be FIR)
    val validInterfaces = fakeInterfaces.filter { isValidFakeInterface(it) }

    // ❌ Type Analysis (should be FIR)
    val interfaceAnalysis = interfaceAnalyzer.analyzeInterfaceDynamically(fakeInterface)

    // ✅ Code Generation (correct phase)
    codeGenerator.generateWorkingFakeImplementation(...)
}
```

**Issues**:
1. **Architectural mismatch** - doesn't follow Metro pattern
2. **Late error detection** - errors found in IR instead of FIR
3. **Performance impact** - analysis repeated for every module
4. **Poor source locations** - IR has worse location info than FIR
5. **Semantic confusion** - mixing "what to generate" with "how to generate"

---

## ✅ Target State: Metro-Aligned Architecture

### Metro's Pattern (Reference)

```kotlin
// MetroCompilerPluginRegistrar.kt:42
FirExtensionRegistrarAdapter.registerExtension(
    MetroFirExtensionRegistrar(classIds, options)
)

// MetroFirExtensionRegistrar.kt:39
+::MetroFirCheckers  // ← Validation happens HERE
```

**Metro FIR Phase** (`metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/fir/`):
- `MetroFirCheckers` - Validation and error reporting
- `InjectConstructorChecker` - Validates `@Inject` usage
- `BindingContainerClassChecker` - Validates DI container structure
- Multiple dedicated checkers for each concern
- **Zero code generation** - pure validation

**Metro IR Phase**:
- `MetroIrGenerationExtension` - Only generates IR nodes
- Assumes FIR validation passed
- No structural validation

### Fakt Target Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  FIR PHASE: Analysis & Validation                              │
│  ══════════════════════════════════════════════════════════════ │
│                                                                  │
│  1. FaktFirCheckers                                             │
│     ├─ FakeInterfaceChecker: Is it an interface?               │
│     ├─ FakeValidationChecker: Not sealed? Not external?        │
│     ├─ FakeTypeAnalyzer: Extract type parameters & bounds       │
│     └─ FakePatternAnalyzer: Classify generic pattern            │
│                                                                  │
│  2. Store metadata in FirMetadataStorage                        │
│     └─ Thread-safe map: FQN → ValidatedFakeMetadata            │
│                                                                  │
│  Output: List of validated interfaces ready for generation      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  IR PHASE: Code Generation ONLY                                 │
│  ══════════════════════════════════════════════════════════════ │
│                                                                  │
│  1. Read validated metadata from FirMetadataStorage             │
│  2. Generate IR nodes:                                          │
│     ├─ FakeXxxImpl class                                        │
│     ├─ fakeXxx() factory function                               │
│     └─ FakeXxxConfig DSL                                        │
│                                                                  │
│  3. Write to test source sets                                   │
│                                                                  │
│  NO validation, NO analysis, ONLY generation                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🏗️ Implementation Strategy

### Decision: Shared Options Pattern (Like Metro)

**Chosen Approach**: Pass shared `classIds` and `options` to both FIR and IR extensions.

```kotlin
// FaktCompilerPluginRegistrar.kt (following MetroCompilerPluginRegistrar.kt:23-57)
override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val options = FaktOptions.load(configuration)
    if (!options.enabled) return

    val fakeAnnotations = listOf("com.rsicarelli.fakt.Fake")

    // Shared configuration available to both phases
    val sharedContext = FaktSharedContext(
        fakeAnnotations = fakeAnnotations,
        options = options,
        metadataStorage = FirMetadataStorage() // ← FIR writes, IR reads
    )

    // FIR Phase: Validation & analysis
    FirExtensionRegistrarAdapter.registerExtension(
        FaktFirExtensionRegistrar(sharedContext)
    )

    // IR Phase: Code generation
    IrGenerationExtension.registerExtension(
        UnifiedFaktIrGenerationExtension(
            logger = logger,
            sharedContext = sharedContext // ← Access to validated metadata
        )
    )
}
```

**Why this approach?**
- ✅ **Proven** - Metro uses this pattern successfully
- ✅ **Simple** - No complex serialization
- ✅ **Type-safe** - Shared Kotlin objects
- ✅ **Backward compatible** - Can add feature flag for gradual migration

### FIR→IR Metadata Passing

```kotlin
/**
 * Metadata validated in FIR phase and passed to IR phase.
 *
 * Following Metro pattern: shared data structures between phases.
 */
data class ValidatedFakeInterface(
    val fqName: String,
    val simpleName: String,
    val packageName: String,
    val typeParameters: List<FirTypeParameterInfo>,
    val properties: List<FirPropertyInfo>,
    val functions: List<FirFunctionInfo>,
    val genericPattern: GenericPattern,
    val sourceLocation: SourceLocation, // For error reporting in IR if needed
)

/**
 * Thread-safe storage for FIR→IR communication.
 *
 * Lifetime: Single compilation session only.
 */
class FirMetadataStorage {
    private val validatedInterfaces = ConcurrentHashMap<String, ValidatedFakeInterface>()

    fun store(metadata: ValidatedFakeInterface) {
        validatedInterfaces[metadata.fqName] = metadata
    }

    fun getAll(): Collection<ValidatedFakeInterface> = validatedInterfaces.values

    fun get(fqName: String): ValidatedFakeInterface? = validatedInterfaces[fqName]
}
```

---

## 📋 Migration Strategy: Dual-Mode with Feature Flag

To ensure zero regressions, implement dual-mode support:

```kotlin
// FaktOptions.kt
data class FaktOptions(
    val enabled: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO,
    val useFirAnalysis: Boolean = false, // ← Feature flag (default OFF for safety)
    val outputDir: String? = null,
)
```

```kotlin
// UnifiedFaktIrGenerationExtension.kt (backward compatible)
override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val interfaces = if (sharedContext.options.useFirAnalysis) {
        // NEW: Load from FIR metadata
        loadValidatedInterfacesFromFir(sharedContext.metadataStorage)
    } else {
        // OLD: IR-phase discovery (current behavior)
        discoverFakeInterfaces(moduleFragment)
    }

    // Rest of generation logic unchanged
    interfaces.forEach { generateFakeImplementation(it) }
}
```

**Migration phases**:
1. ✅ Implement FIR validation (with flag OFF)
2. ✅ Test both modes produce identical output
3. ✅ Enable flag by default (`useFirAnalysis = true`)
4. ✅ Remove old IR-discovery code after validation

---

## 🎯 Benefits

### **1. Metro Alignment** ✅
- Follows proven production-quality architecture
- Same patterns as successful DI framework
- Easier to learn from Metro examples

### **2. Better Error Messages** ✅
```kotlin
// FIR phase - excellent source location
error: @Fake can only be applied to interfaces, not sealed interfaces
  @Fake interface SealedUserService // ← Exact location
  ^~~~~

// vs Current IR phase - poor location
error: Invalid @Fake usage in file UserService.kt
```

### **3. Performance** ✅
- FIR analysis once per interface definition
- IR generation once per module consuming it
- No repeated analysis across modules

### **4. Early Validation** ✅
- Errors caught in FIR phase (earlier)
- Failed builds don't reach IR generation
- Faster feedback loop

### **5. Cleaner Code** ✅
- Separation of concerns
- FIR: "What is valid?"
- IR: "How to generate?"

---

## 📊 Comparison: Metro vs Fakt

| Aspect | Metro (Reference) | Fakt (Current) | Fakt (Target) |
|--------|-------------------|----------------|---------------|
| **FIR Phase** | Validation checkers | Empty placeholder | Validation checkers ✅ |
| **IR Phase** | Code generation only | Everything | Code generation only ✅ |
| **Metadata Passing** | Shared options + storage | N/A | Shared context ✅ |
| **Error Detection** | FIR phase (early) | IR phase (late) | FIR phase (early) ✅ |
| **Architecture** | Two-phase clean | Single-phase mixed | Two-phase clean ✅ |

---

## 🚀 Implementation Phases

**Phase 1: Research** ✅
- [x] Study Metro FIR implementation
- [x] Understand FIR→IR metadata passing
- [x] Create this decision document

**Phase 2: FIR Metadata Structures** (Next)
- [ ] Create `FirFakeMetadata.kt`
- [ ] Create `FirMetadataStorage.kt`
- [ ] Create `FaktSharedContext.kt`
- [ ] Write unit tests

**Phase 3-7**: See main multi-phase plan

---

## 📚 References

### Metro Source Code (Local)
- `metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/MetroCompilerPluginRegistrar.kt`
- `metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/fir/MetroFirExtensionRegistrar.kt`
- `metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/fir/checkers/InjectConstructorChecker.kt`
- `metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/fir/fir.kt`

### Kotlin FIR Documentation
- GitHub: `kotlin/docs/fir/fir-plugins.md`
- GitHub: `kotlin/compiler/fir/checkers/src/.../FirAdditionalCheckersExtension.kt`

### Fakt Current Implementation
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/FaktCompilerPluginRegistrar.kt`
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FaktFirExtensionRegistrar.kt` (empty)
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt` (everything)

---

## ✅ Decision Approval

**Approved by**: Architecture review
**Rationale**:
1. Follows proven Metro pattern
2. Addresses architectural debt
3. Improves error messages and performance
4. Backward compatible with feature flag
5. Clear migration path

**Next Step**: Proceed to Phase 2 - FIR Metadata Structures
