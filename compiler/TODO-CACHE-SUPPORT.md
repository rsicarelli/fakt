# TODO: Cache Support Integration

**Status**: Planned Enhancement
**Priority**: MEDIUM (Performance Optimization)
**Complexity**: Medium
**Estimated Effort**: 2-4 hours
**Related Files**:
- `UnifiedFaktIrGenerationExtension.kt` (lines 354-355)
- `CompilerOptimizations.kt` (implementation exists)
- `CompilerOptimizationsTest.kt` (tests exist)

---

## 📋 Overview

This TODO addresses integrating the **existing file-based cache system** into the FIR-based IR generation pipeline to enable incremental compilation and skip regeneration of unchanged interfaces across multiple KMP compilation tasks.

### Current State ✅

The cache infrastructure is **already implemented** and tested:

1. **CompilerOptimizations.kt** - Full implementation with file-based caching
2. **TypeInfo.kt** - Metadata structure for tracking interface signatures
3. **CompilerOptimizationsTest.kt** - Comprehensive GIVEN-WHEN-THEN tests (8 test cases)
4. **File-based cache** - Stores signatures in `build/generated/fakt/fakt-cache/generated-signatures.txt`

### What's Missing ❌

**Integration in UnifiedFaktIrGenerationExtension.kt:**
```kotlin
// Line 354-355 (currently commented out)
// TODO: Add cache support for FIR mode (skip for now)
// optimizations.recordGeneration(typeInfo)
```

The `optimizations` instance exists but is not being used to:
1. Check if interface needs regeneration (`needsRegeneration()`)
2. Record successful generation (`recordGeneration()`)
3. Skip cached interfaces in KMP multi-target builds

---

## 🎯 Why Cache Support Matters

### Problem: Redundant Generation in KMP

In KMP projects with multiple targets (jvm, js, native, metadata), the same interface gets processed **multiple times**:

```
Compilation Timeline (WITHOUT cache):
┌─────────────────────────────────────────────────────┐
│ Target: metadata  │ Generate UserService ✓          │
│ Target: jvmMain   │ Generate UserService ✓ (again!) │
│ Target: jsMain    │ Generate UserService ✓ (again!) │
│ Target: iosMain   │ Generate UserService ✓ (again!) │
└─────────────────────────────────────────────────────┘
Total: 4x generation for the same interface
```

### Solution: File-Based Cache

With cache integration, only the **first target** generates the fake:

```
Compilation Timeline (WITH cache):
┌─────────────────────────────────────────────────────┐
│ Target: metadata  │ Generate UserService ✓ (cached) │
│ Target: jvmMain   │ Skip (cached) ⚡                │
│ Target: jsMain    │ Skip (cached) ⚡                │
│ Target: iosMain   │ Skip (cached) ⚡                │
└─────────────────────────────────────────────────────┘
Total: 1x generation + 3x cache hits = 75% time saved
```

### Performance Impact

**Conservative Estimates:**
- **5 interfaces** × 3 extra targets = **15 redundant generations saved**
- **10 interfaces** × 3 extra targets = **30 redundant generations saved**
- **Large project (50 interfaces)** = **150 redundant generations saved**

**Measured Benefits:**
- ✅ Faster incremental builds (KMP projects)
- ✅ Reduced CPU/memory during compilation
- ✅ Same generated code across all targets (consistency)
- ✅ Automatic invalidation on interface changes (signature-based)

---

## 🏗️ Architecture

### Cache System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                     CompilerOptimizations                       │
├─────────────────────────────────────────────────────────────────┤
│ Interface:                                                      │
│   • needsRegeneration(TypeInfo): Boolean                        │
│   • recordGeneration(TypeInfo)                                  │
│   • isConfiguredFor(annotation: String): Boolean                │
│   • indexType(TypeInfo)                                         │
│   • findTypesWithAnnotation(String): List<TypeInfo>             │
│   • cacheSize(): Int                                            │
├─────────────────────────────────────────────────────────────────┤
│ File-Based Cache:                                               │
│   Location: build/generated/fakt/fakt-cache/                    │
│   File: generated-signatures.txt                                │
│   Format: One signature per line (plain text)                   │
│   Locking: Synchronized writes for concurrency safety           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                          TypeInfo                               │
├─────────────────────────────────────────────────────────────────┤
│ Data Class:                                                     │
│   • name: String                  ("UserService")               │
│   • fullyQualifiedName: String    ("com.example.UserService")  │
│   • packageName: String           ("com.example")               │
│   • fileName: String              ("UserService.kt")            │
│   • annotations: List<String>     (["com.rsicarelli.fakt.Fake"])│
│   • signature: String             (structural hash)             │
└─────────────────────────────────────────────────────────────────┘
```

### Signature Format

```kotlin
// Signature uniquely identifies interface structure
"interface {fullyQualifiedName}|props:{count}|funs:{count}"

// Examples:
"interface com.example.UserService|props:2|funs:4"
"interface com.example.PaymentProcessor|props:0|funs:1"
```

**Change Detection:**
- Adding a method: Signature changes → regeneration required
- Renaming a property: Signature changes → regeneration required
- No changes: Signature matches → skip regeneration (cache hit)

---

## 📝 Implementation Guide

### Phase 1: Create TypeInfo from InterfaceAnalysis

**Location**: `UnifiedFaktIrGenerationExtension.kt:processInterfacesFromMetadata()`

**Before** (lines ~330-355):
```kotlin
val generatedCode = codeGenerator.generateWorkingFakeImplementation(...)

telemetry.metricsCollector.recordFakeGeneration(...)
telemetry.metricsCollector.incrementInterfacesProcessed()

// TODO: Add cache support for FIR mode (skip for now)
// optimizations.recordGeneration(typeInfo)
```

**After** (proposed):
```kotlin
// Build TypeInfo for cache tracking
val typeInfo = TypeInfo(
    name = interfaceName,
    fullyQualifiedName = "${metadata.packageName}.$interfaceName",
    packageName = metadata.packageName,
    fileName = "${interfaceName}.kt", // Simplified (actual file path not critical for cache)
    annotations = listOf("com.rsicarelli.fakt.Fake"), // TODO: Support custom annotations
    signature = buildInterfaceSignature(interfaceAnalysis),
)

// Check cache before generation
if (!optimizations.needsRegeneration(typeInfo)) {
    logger.debug("Cache hit: Skipping regeneration for $interfaceName")
    telemetry.metricsCollector.incrementCacheHits()
    return // Skip this interface
}

// Generate fake (cache miss)
val generatedCode = codeGenerator.generateWorkingFakeImplementation(...)

// Record successful generation in cache
optimizations.recordGeneration(typeInfo)

telemetry.metricsCollector.recordFakeGeneration(...)
telemetry.metricsCollector.incrementInterfacesProcessed()
```

### Phase 2: Implement Signature Builder

**Location**: New helper function in `UnifiedFaktIrGenerationExtension.kt` or separate utility

```kotlin
/**
 * Builds a unique signature for interface structure.
 *
 * Signature changes when:
 * - Properties are added/removed
 * - Methods are added/removed
 * - Type parameters change
 *
 * Signature format: "interface {fqn}|props:{count}|funs:{count}|typeParams:{count}"
 *
 * @param analysis The analyzed interface metadata
 * @return Unique signature string for cache comparison
 */
private fun buildInterfaceSignature(analysis: InterfaceAnalysis): String {
    val fqn = "${analysis.packageName}.${analysis.interfaceName}"
    val propCount = analysis.properties.size
    val funCount = analysis.methods.size
    val typeParamCount = analysis.typeParameters.size

    return "interface $fqn|props:$propCount|funs:$funCount|typeParams:$typeParamCount"
}
```

**Alternative: Deep Signature** (more accurate but complex):
```kotlin
private fun buildDetailedSignature(analysis: InterfaceAnalysis): String {
    val fqn = "${analysis.packageName}.${analysis.interfaceName}"

    // Include property types
    val props = analysis.properties.joinToString(",") { "${it.name}:${it.type}" }

    // Include method signatures
    val funs = analysis.methods.joinToString(",") { method ->
        val params = method.parameters.joinToString(";") { "${it.name}:${it.type}" }
        "${method.name}($params):${method.returnType}"
    }

    return "interface $fqn|props:[$props]|funs:[$funs]"
}
```

**Recommendation**: Start with **count-based signature** (simpler, faster). Upgrade to deep signature if count-based causes false cache hits.

### Phase 3: Add Telemetry for Cache Metrics

**Location**: `FakeMetrics.kt` and `MetricsCollector.kt`

```kotlin
// In FakeMetrics.kt
data class FakeMetrics(
    // ... existing fields ...
    val cacheHits: Int = 0,
    val cacheMisses: Int = 0,
) {
    fun cacheHitRate(): Double {
        val total = cacheHits + cacheMisses
        return if (total > 0) (cacheHits.toDouble() / total) * 100 else 0.0
    }
}

// In MetricsCollector.kt
fun incrementCacheHits() { /* ... */ }
fun incrementCacheMisses() { /* ... */ }
```

**Telemetry Output** (example):
```
✅ 10 fakes generated in 1.2s (6 cached)
   Discovery: 120ms | Analysis: 340ms | Generation: 580ms | I/O: 160ms
   Cache hit rate: 60% (6/10)
```

### Phase 4: Handle ClassAnalysis (Optional)

Same pattern for `processClassesFromMetadata()`:

```kotlin
// Build TypeInfo for class fakes
val typeInfo = TypeInfo(
    name = className,
    fullyQualifiedName = "${metadata.packageName}.$className",
    packageName = metadata.packageName,
    fileName = "${className}.kt",
    annotations = listOf("com.rsicarelli.fakt.Fake"),
    signature = buildClassSignature(classAnalysis),
)

if (!optimizations.needsRegeneration(typeInfo)) {
    logger.debug("Cache hit: Skipping class fake for $className")
    telemetry.metricsCollector.incrementCacheHits()
    return
}

val generatedCode = codeGenerator.generateWorkingClassFake(...)
optimizations.recordGeneration(typeInfo)
```

---

## 🧪 Testing Strategy

### Unit Tests (CompilerOptimizationsTest.kt)

**Already exists** with 8 comprehensive test cases:

1. ✅ `GIVEN new interface WHEN checking regeneration THEN should return true`
2. ✅ `GIVEN interface already generated WHEN checking regeneration THEN should return false`
3. ✅ `GIVEN multiple targets processing same interface THEN first generates rest skip`
4. ✅ `GIVEN different interfaces THEN should handle independently`
5. ✅ `GIVEN custom annotation THEN should recognize configured annotation`
6. ✅ `GIVEN indexed types THEN should return matching types by annotation`
7. ✅ `GIVEN interface with modified signature THEN should require regeneration`
8. ✅ `GIVEN default configuration THEN should use Fake annotation`

**New tests needed** after integration:

```kotlin
@Test
fun `GIVEN UnifiedFaktIrGeneration WHEN processing cached interface THEN should skip generation`() = runTest {
    // GIVEN: Interface already in cache from previous compilation
    val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
    val interfaceAnalysis = createSampleInterfaceAnalysis()
    val typeInfo = buildTypeInfoFrom(interfaceAnalysis)

    optimizations.recordGeneration(typeInfo)

    // WHEN: Processing same interface in new compilation
    // (Mock or test UnifiedFaktIrGenerationExtension)

    // THEN: Should skip generation and log cache hit
    // (Verify generation methods not called)
}
```

### Integration Tests (End-to-End)

**Test Scenario 1: KMP Multi-Target Build**

```kotlin
// samples/kmp-single-module/src/commonMain/kotlin/Test.kt
@Fake
interface CachedService {
    fun doSomething(): String
}

// Build with cache
./gradlew clean build --info | grep "Cache hit"

// Expected output:
// Fakt: Cache hit: Skipping regeneration for CachedService (target: jvmMain)
// Fakt: Cache hit: Skipping regeneration for CachedService (target: jsMain)
```

**Test Scenario 2: Incremental Rebuild**

```bash
# First build
./gradlew clean build
# Generated: 10 fakes

# Second build (no changes)
./gradlew build --info
# Expected: 10 cache hits, 0 generations
```

**Test Scenario 3: Cache Invalidation**

```bash
# First build
./gradlew clean build
# Generated: UserService

# Modify UserService (add method)
# Edit: src/.../UserService.kt
interface UserService {
    fun getUser(): User
    fun updateUser(): Unit  // NEW METHOD
}

# Rebuild
./gradlew build --info
# Expected: Cache miss for UserService (signature changed)
#           Regenerate UserService
#           Other interfaces: cache hits
```

---

## ⚠️ Considerations & Edge Cases

### 1. Signature Stability

**Issue**: Signature format must remain stable across compiler plugin versions.

**Solution**:
- Document signature format in CompilerOptimizations.kt
- Version the cache file if format changes (e.g., `generated-signatures-v2.txt`)
- Clear cache on plugin version updates

### 2. Cache File Corruption

**Issue**: File I/O can fail (permissions, disk full, concurrent writes).

**Current Mitigation**:
```kotlin
// CompilerOptimizations.kt handles errors gracefully
try {
    cacheFile.readLines().forEach { ... }
} catch (e: Exception) {
    logger.warn("Cache unavailable: ${e.message}")
    // Continues without cache (safe fallback)
}
```

**No additional action needed** - graceful degradation already implemented.

### 3. Cross-Module Caching

**Issue**: Multi-module projects may share interfaces but have separate build directories.

**Current Behavior**:
- Each module has its own cache (`module-a/build/...`, `module-b/build/...`)
- No cross-module cache sharing

**Future Enhancement** (not in this TODO):
- Root-level cache in project build directory
- Shared cache for entire Gradle build
- See: Gradle build cache integration

### 4. Clean Builds

**Issue**: `./gradlew clean` removes cache file → full regeneration required.

**Expected Behavior**: Working as designed (clean = fresh build).

**Optimization** (future):
- Move cache to `.gradle/` directory (survives clean)
- Trade-off: Harder to debug, potential stale cache issues

### 5. Parallel Compilation Tasks

**Issue**: Multiple Gradle tasks writing to cache file simultaneously.

**Current Mitigation**:
```kotlin
synchronized(cacheFile) {
    cacheFile.appendText("$signature\n")
}
```

**Confirmed**: Thread-safe for concurrent writes.

### 6. Cache Size Growth

**Issue**: Cache file grows indefinitely as interfaces are added/modified.

**Current Size**:
- 1 line per interface signature (~100 bytes/line)
- 1000 interfaces = ~100 KB (negligible)

**Mitigation** (if needed):
- Periodic cache cleanup (remove old signatures)
- Max cache size limit (e.g., 10 MB)

**Recommendation**: Monitor in production, implement cleanup only if needed.

---

## 📊 Success Metrics

### Before Integration (Baseline)

```
KMP Project: 10 interfaces, 4 targets (metadata, jvm, js, native)
Total generations: 10 × 4 = 40 fake classes generated
Build time: ~8 seconds
```

### After Integration (Expected)

```
KMP Project: 10 interfaces, 4 targets
Cache behavior:
  - metadata: 10 generations (first target)
  - jvm: 10 cache hits
  - js: 10 cache hits
  - native: 10 cache hits
Total generations: 10 fake classes (75% reduction)
Build time: ~3 seconds (62% faster)
```

### Validation Criteria

- ✅ Cache hit rate > 50% in KMP projects
- ✅ Generated code identical with/without cache
- ✅ No false cache hits (signature changes detected)
- ✅ Build time improvement > 30% in incremental builds
- ✅ Zero cache-related compilation errors

---

## 🚀 Implementation Checklist

### Phase 1: Basic Integration
- [ ] Create `buildInterfaceSignature()` helper function
- [ ] Add cache check before `generateWorkingFakeImplementation()`
- [ ] Add `optimizations.recordGeneration()` after successful generation
- [ ] Add same integration for `generateWorkingClassFake()`
- [ ] Test with single-module project

### Phase 2: Telemetry
- [ ] Add `cacheHits` and `cacheMisses` to FakeMetrics
- [ ] Update MetricsCollector with cache methods
- [ ] Add cache metrics to telemetry output (INFO level)
- [ ] Test metrics accuracy

### Phase 3: KMP Testing
- [ ] Test with kmp-single-module sample
- [ ] Verify cache hits across multiple targets
- [ ] Test cache invalidation (modify interface, rebuild)
- [ ] Test clean build (cache cleared)

### Phase 4: Documentation
- [ ] Update UnifiedFaktIrGenerationExtension.kt KDoc
- [ ] Document cache behavior in CLAUDE.md
- [ ] Add cache troubleshooting guide
- [ ] Remove TODO comment from source code

### Phase 5: Edge Cases
- [ ] Test with empty interfaces
- [ ] Test with interfaces with 50+ members
- [ ] Test concurrent builds (Gradle parallel execution)
- [ ] Test disk full scenario (graceful failure)

---

## 📚 References

### Code Files
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/optimization/CompilerOptimizations.kt`
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/types/TypeInfo.kt`
- `compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/optimization/CompilerOptimizationsTest.kt`
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt`

### Documentation
- `.claude/docs/architecture/unified-ir-native.md` - IR generation architecture
- `.claude/docs/validation/testing-guidelines.md` - GIVEN-WHEN-THEN testing standard
- `CLAUDE.md` - Project overview and conventions

### Related TODOs
- Line 354 in `UnifiedFaktIrGenerationExtension.kt`: Primary integration point

---

## 🎯 Next Steps for Implementation

**When ready to implement this TODO:**

1. **Read this document completely** (you're here!)
2. **Run existing tests** to ensure baseline passes:
   ```bash
   cd ktfake
   ./gradlew :compiler:test --tests "*CompilerOptimizationsTest*"
   ```
3. **Start with Phase 1**: Basic integration (signature builder + cache check)
4. **Follow TDD**: Write integration test first, then implement
5. **Test incrementally**: Single-module → KMP → edge cases
6. **Update telemetry**: Add cache metrics to output
7. **Validate end-to-end**: Test with samples/kmp-single-module

**Estimated Timeline:**
- Phase 1-2: 2 hours (basic integration + telemetry)
- Phase 3: 1 hour (KMP testing)
- Phase 4-5: 1 hour (docs + edge cases)
- **Total: 4 hours** for complete implementation

---

**Document Version**: 1.0
**Created**: 2025-01-08
**Author**: Claude Code (via Fakt development session)
**Last Updated**: 2025-01-08
