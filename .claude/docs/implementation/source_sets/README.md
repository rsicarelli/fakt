# Source Set Mapping Modernization

**Status**: 📋 Ready for Implementation
**Date**: 2025-01-05
**Research Source**: Gemini Deep Research - Kotlin Compiler Plugin Source Set Mapping
**Estimated Duration**: 3 weeks

---

## 🎯 Overview

This directory contains the complete plan to modernize Fakt's source set mapping from hardcoded pattern matching to convention-based programmatic discovery using modern Kotlin Gradle Plugin APIs.

**Problem**: 600+ lines of brittle, hardcoded source set name pattern matching spread across compiler and Gradle plugins.

**Solution**: Programmatic discovery using lazy Gradle APIs, BFS graph traversal, and serialized context passing between plugins.

**Result**: ~220 lines of maintainable, future-proof code that automatically supports all Kotlin targets and custom source sets.

---

## 📚 Documentation Index

### Start Here

1. **[ARCHITECTURE.md](./ARCHITECTURE.md)** ⭐
   - **What**: Complete architectural design of new system
   - **Why Read**: Understand the new approach vs old hardcoded patterns
   - **Key Topics**:
     - Two-phase discovery model (Gradle → Compiler)
     - Data models for serialization bridge
     - Comparison: Before (602 lines) vs After (220 lines)
     - Benefits: Future-proof, Android support, custom source sets
   - **Read Time**: 15 minutes

2. **[IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md)** ⭐
   - **What**: Week-by-week implementation plan with specific tasks
   - **Why Read**: Step-by-step guide for implementation
   - **Key Topics**:
     - Week 1: Foundation & data models
     - Week 2: Gradle plugin refactoring
     - Week 3: Compiler plugin & testing
     - Detailed task breakdown with success criteria
   - **Read Time**: 20 minutes

3. **[MIGRATION-GUIDE.md](./MIGRATION-GUIDE.md)** ⭐
   - **What**: Hands-on guide to migrate from old to new implementation
   - **Why Read**: Practical steps to implement the changes
   - **Key Topics**:
     - Step-by-step migration instructions
     - Code examples for each phase
     - Testing strategies
     - Rollback procedures
   - **Read Time**: 25 minutes

### Reference Documentation

4. **[CODE-PATTERNS.md](./CODE-PATTERNS.md)**
   - **What**: Copy-paste ready code snippets and utilities
   - **Why Read**: Reusable patterns for implementation
   - **Key Topics**:
     - BFS graph traversal implementation
     - Compilation classification
     - Serialization patterns
     - Testing patterns
     - Android-specific patterns
   - **Read Time**: 15 minutes

5. **[API-REFERENCE.md](./API-REFERENCE.md)**
   - **What**: Quick reference for Kotlin Gradle Plugin APIs
   - **Why Read**: Look up API signatures and usage
   - **Key Topics**:
     - KotlinMultiplatformExtension
     - KotlinSourceSet & dependsOn
     - KotlinCompilation & allKotlinSourceSets
     - Lazy Provider APIs
     - Compiler plugin APIs
   - **Read Time**: 10 minutes (reference, not sequential)

---

## 🚀 Quick Start

### For Contributors

**First Time Reading?**
1. Read [ARCHITECTURE.md](./ARCHITECTURE.md) to understand the new design
2. Review [IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md) for the plan
3. Start implementation following [MIGRATION-GUIDE.md](./MIGRATION-GUIDE.md)

**Need Code Examples?**
- Jump to [CODE-PATTERNS.md](./CODE-PATTERNS.md) for copy-paste ready snippets

**Need API Details?**
- Use [API-REFERENCE.md](./API-REFERENCE.md) as a quick lookup reference

---

### For Reviewers

**Review Checklist**:
1. Architecture approved? → Read [ARCHITECTURE.md](./ARCHITECTURE.md)
2. Implementation plan sound? → Read [IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md)
3. Migration path clear? → Read [MIGRATION-GUIDE.md](./MIGRATION-GUIDE.md)
4. Code patterns idiomatic? → Review [CODE-PATTERNS.md](./CODE-PATTERNS.md)

---

## 📊 At a Glance

### The Problem

```kotlin
// ❌ Current: SourceSetMapper.kt (411 lines)
private fun mapToTestSourceSet(moduleName: String): String {
    val normalizedName = moduleName.lowercase()
    return when {
        normalizedName.contains("commonmain") -> "commonTest"
        normalizedName.contains("jvmmain") -> "jvmTest"
        normalizedName.contains("iosarm64main") -> "iosArm64Test"
        // ... 50+ more hardcoded patterns
    }
}
```

**Issues**:
- ❌ 50+ hardcoded source set names
- ❌ Breaks with custom source sets (`integrationTest`)
- ❌ No Android variant support
- ❌ Requires updates for every new Kotlin target
- ❌ Brittle string pattern matching

---

### The Solution

```kotlin
// ✅ New: SourceSetDiscovery.kt (~120 lines)
fun buildContext(compilation: KotlinCompilation<*>): SourceSetContext {
    return SourceSetContext(
        compilationName = compilation.name,
        targetName = compilation.target.name,
        platformType = compilation.platformType.name,
        isTest = isTestCompilation(compilation),  // Heuristics-based
        allSourceSets = compilation.allKotlinSourceSets.map { it.toInfo() },  // Built-in API!
        outputDirectory = resolveOutputDirectory(compilation)
    )
}
```

**Benefits**:
- ✅ Zero hardcoded source set names
- ✅ Works with any source set structure
- ✅ Supports Android variants automatically
- ✅ Future-proof for new Kotlin targets
- ✅ Programmatic discovery using official APIs

---

## 🗺️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│  GRADLE PLUGIN (Configuration Time)                        │
│  ═══════════════════════════════════════════════════════    │
│                                                             │
│  1. Iterate ALL compilations (lazy)                        │
│  2. For each: build SourceSetContext                       │
│     - Use compilation.allKotlinSourceSets                  │
│     - Traverse dependsOn graph with BFS                    │
│     - Detect test vs main with heuristics                  │
│  3. Serialize to JSON + Base64                             │
│  4. Pass via SubpluginOption                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              ↓
                    (Serialized Context)
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  COMPILER PLUGIN (Compilation Time)                        │
│  ═══════════════════════════════════════════════════════    │
│                                                             │
│  1. Receive Base64 JSON via CommandLineProcessor           │
│  2. Deserialize to SourceSetContext                        │
│  3. Use context directly (NO pattern matching!)            │
│  4. Generate code to correct output directory              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Key Innovation**: Single source of truth (Gradle plugin discovers everything programmatically)

---

## 📈 Impact Analysis

### Code Reduction

| Component | Before | After | Change |
|-----------|--------|-------|--------|
| SourceSetMapper.kt | 411 lines | **DELETED** | -100% |
| SourceSetConfigurator.kt | 191 lines | **DELETED** | -100% |
| **Total Old** | **602 lines** | - | - |
| compiler-api (new) | - | 25 lines | Data models |
| SourceSetDiscovery.kt | - | 120 lines | Discovery logic |
| ContextSerializer.kt | - | 25 lines | Serialization |
| SourceSetResolver.kt | - | 50 lines | Simple resolver |
| **Total New** | - | **220 lines** | **-63%** |

### Maintenance Burden

| Aspect | Before | After |
|--------|--------|-------|
| New Kotlin target | Update 3+ places | Zero changes |
| Custom source set | Breaks | Works automatically |
| Android variants | Not supported | Works automatically |
| Custom hierarchy | Breaks | Works automatically |
| Updates needed | Every Kotlin release | Never |

### Performance

| Metric | Expected Impact |
|--------|-----------------|
| Configuration time | +50-100ms (one-time) |
| Compilation time | No change |
| Memory | +5-10MB (serialized context) |
| Configuration cache | **✅ Now compatible!** |

---

## 🎯 Key Technologies

### Kotlin Gradle Plugin APIs

- `KotlinMultiplatformExtension` - Access to targets and source sets
- `KotlinCompilation.allKotlinSourceSets` - Get full hierarchy
- `KotlinSourceSet.dependsOn` - Traverse source set graph
- Lazy APIs (`Provider`, `named`, `configureEach`) - Configuration avoidance

### Serialization

- `kotlinx.serialization` - JSON serialization
- `java.util.Base64` - Command-line safe encoding

### Graph Algorithms

- Breadth-First Search (BFS) - Traverse `dependsOn` graph
- Set-based deduplication - Handle diamond dependencies

---

## ✅ Success Criteria

Implementation is successful when:

1. **Functionality**
   - ✅ All existing tests pass
   - ✅ All sample projects build successfully
   - ✅ Generated code compiles without errors
   - ✅ Generated code in correct directories

2. **New Features**
   - ✅ Custom source sets work (`integrationTest`, etc.)
   - ✅ Android build variants work (`debugTest`, `releaseTest`)
   - ✅ Custom hierarchies work (`applyHierarchyTemplate { ... }`)

3. **Quality**
   - ✅ Configuration cache compatible
   - ✅ Incremental compilation works
   - ✅ No deprecation warnings
   - ✅ IDE recognizes generated sources

4. **Performance**
   - ✅ Configuration time increase < 100ms
   - ✅ No compilation time regression
   - ✅ Memory increase < 10MB

5. **Maintainability**
   - ✅ Zero hardcoded source set names
   - ✅ Code reduction > 50%
   - ✅ Clear separation of concerns
   - ✅ Comprehensive documentation

---

## 🚨 Known Limitations

### What This Solves

- ✅ Hardcoded source set name patterns
- ✅ Manual fallback chain maintenance
- ✅ Custom source set support
- ✅ Android variant support
- ✅ Future Kotlin targets (automatic)

### What This Doesn't Solve

- ❌ Generic type handling (separate epic)
- ❌ Cross-module fake dependencies (multi-module support, separate epic)
- ❌ Custom compilation names (plugin needs configuration)

---

## 🔗 Related Documentation

### External Resources

- **Research Document**: `/Users/rsicarelli/Downloads/Kotlin Compiler Plugin Source Set Mapping.md`
- **KSP Multiplatform**: [kotlinlang.org/docs/ksp-multiplatform](https://kotlinlang.org/docs/ksp-multiplatform.html)
- **Kotlin 1.9.20 Hierarchy**: [kotlinlang.org/docs/whatsnew1920](https://kotlinlang.org/docs/whatsnew1920.html)
- **Metro Framework**: [github.com/slackhq/metro](https://github.com/slackhq/metro)

### Internal Documentation

- **CLAUDE.md**: Main project documentation
- **Testing Guidelines**: `.claude/docs/validation/testing-guidelines.md`
- **Metro Alignment**: `.claude/docs/development/metro-alignment.md`

---

## 💬 FAQ

### Q: Why not just add more patterns to SourceSetMapper?

**A**: Pattern matching is fundamentally flawed:
- Requires maintenance for every new Kotlin target
- Breaks with custom source sets
- No support for Android variants
- Brittle string matching

### Q: What if Gradle plugin version mismatches compiler plugin?

**A**: Graceful degradation:
1. Compiler plugin checks for context
2. If missing, logs clear error message
3. Suggests updating Gradle plugin version
4. (Future: fallback to old behavior with deprecation warning)

### Q: Will this break existing projects?

**A**: No breaking changes:
- Same public API
- Same generated code location
- Same behavior (but more robust)
- Transparent migration

### Q: Performance impact?

**A**: Minimal:
- One-time cost during configuration (~50-100ms)
- No compilation time impact
- Memory overhead negligible (~5-10MB for context)
- Configuration cache now works (net performance gain!)

### Q: What about custom hierarchies?

**A**: Fully supported:
- Traverses actual `dependsOn` graph
- Works with `applyHierarchyTemplate { ... }`
- No assumptions about structure

---

## 📞 Support

### For Questions

- Review [ARCHITECTURE.md](./ARCHITECTURE.md) for design questions
- Check [API-REFERENCE.md](./API-REFERENCE.md) for API questions
- See [CODE-PATTERNS.md](./CODE-PATTERNS.md) for implementation questions

### For Issues During Implementation

1. Check [MIGRATION-GUIDE.md](./MIGRATION-GUIDE.md) rollback procedures
2. Review [IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md) success criteria
3. Consult research document for edge cases
4. Ask in GitHub Discussions

---

## 🎯 Next Steps

### Phase 1: Review (Week 0)

- [ ] Read all documentation
- [ ] Review architecture design
- [ ] Approve implementation plan
- [ ] Set up feature branch

### Phase 2: Implementation (Weeks 1-3)

Follow [IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md):
- Week 1: Foundation & data models
- Week 2: Gradle plugin refactoring
- Week 3: Compiler plugin & testing

### Phase 3: Validation (Week 4)

- [ ] All tests passing
- [ ] All samples working
- [ ] Configuration cache validated
- [ ] Performance benchmarked
- [ ] Documentation updated

### Phase 4: Release

- [ ] Beta release (`v1.1.0-beta.1`)
- [ ] Community testing
- [ ] Stable release (`v1.1.0`)
- [ ] Announcement

---

**Status**: ✅ Ready for Implementation
**Owner**: Fakt Core Team
**Priority**: High
**Complexity**: Medium
**Risk**: Low (proven patterns from KSP, Metro)

---

*Last Updated: 2025-01-05*
