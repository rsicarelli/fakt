# KMP Multi-Module Support - Internal Documentation

> **⚠️ IMPORTANT: DOCUMENTATION STATUS**
>
> This directory documents a **planned but NOT adopted** implementation approach.
>
> **What's documented here**: Custom source sets with Gradle capabilities (advanced variant-based approach)
>
> **What was actually implemented**: Dedicated `-fakes` collector modules with `FakeCollectorTask` (pragmatic approach)
>
> **Why the change**: The custom source sets approach proved too complex for the benefit gained. The simpler dedicated modules approach is production-ready, easier to understand, and provides the same core functionality.

---

## Current Implementation (Actual)

**For the ACTUAL multi-module implementation**, see:

- **User Documentation**: [`docs/multi-module/`](../../../docs/multi-module/)
  - [Overview](../../../docs/multi-module/index.md) - Architecture and patterns
  - [Getting Started](../../../docs/multi-module/getting-started.md) - Step-by-step setup
  - [Technical Reference](../../../docs/multi-module/reference.md) - FakeCollectorTask details

- **Implementation Code**:
  - `compiler/src/main/kotlin/com/rsicarelli/fakt/gradle/FakeCollectorTask.kt`
  - `compiler/src/main/kotlin/com/rsicarelli/fakt/gradle/FaktGradleSubplugin.kt`

- **Working Sample**: [`samples/kmp-multi-module/`](../../../samples/kmp-multi-module/)

---

## What This Directory Contains

This directory preserves the **architectural research and decision-making process** that led to the multi-module feature, including:

1. **ARCHITECTURE-DECISION.md** - Evaluation of 5 approaches (dedicated modules, java-test-fixtures, classifiers, build/ artifacts, custom source sets)
2. **IMPLEMENTATION-ROADMAP.md** - 3-week plan for custom source sets implementation (NOT followed)
3. **TECHNICAL-REFERENCE.md** - Gradle variant attributes, configuration patterns
4. **CONVENTION-PLUGIN-BLUEPRINT.md** - Convention plugin design (for custom source sets)
5. **COMPARISON-MATRIX.md** - Feature comparison of all approaches
6. **FAQ.md** - Questions and answers about multi-module patterns

---

## Why Keep This Documentation?

**Historical Value**:
- Shows the research process that informed the final design
- Documents trade-offs considered before choosing dedicated modules
- Explains why we didn't use more complex Gradle features
- Useful reference for understanding Gradle variant system

**Future Consideration**:
- If custom source sets approach becomes simpler in future Gradle versions
- If we need advanced variant features for other purposes
- As reference for understanding Gradle GMM (Gradle Module Metadata)

---

## Actual Implementation Summary

The **production implementation** uses:

### Architecture: Producer → Collector → Consumer

```
:core:analytics (producer)
  ↓ Generates fakes in build/generated/fakt/
  ↓
:core:analytics-fakes (collector)
  ↓ FakeCollectorTask copies fakes with platform detection
  ↓ Output: build/generated/collected-fakes/{platform}/kotlin/
  ↓
:app (consumer)
  ↓ Standard dependency: implementation(projects.core.analyticsFakes)
```

### Key Components

1. **FakeCollectorTask**
   - Collects generated fakes from producer module
   - Intelligent platform detection via package analysis
   - Copies to appropriate source sets (commonMain, jvmMain, etc.)

2. **FaktPluginExtension.collectFakesFrom()**
   - DSL for configuring collector mode
   - Supports both `projects.xxx` and `project(":xxx")` syntax
   - Requires `@OptIn(ExperimentalFaktMultiModule::class)`

3. **Mode Detection** (FaktGradleSubplugin)
   - Generator mode (default): Generates fakes in producer
   - Collector mode: Collects fakes from producer

### Benefits of Actual Approach

✅ **Simplicity**: Standard Gradle dependencies, no custom capabilities
✅ **Familiarity**: Developers understand module pattern already
✅ **IDE Support**: First-class module support in all IDEs
✅ **Proven Pattern**: Used by community (java-test-fixtures equivalent)
✅ **Lower Complexity**: ~500 lines of code vs ~2000+ for custom source sets

### Trade-Offs Accepted

⚠️ **Module Proliferation**: 11 producers → 22 modules total (11 producers + 11 collectors)
⚠️ **Git Footprint**: Moderate (new module directories vs single build.gradle.kts change)

**Decision**: Complexity reduction outweighs module count increase

---

## Documentation Update Status

- [x] User-facing docs created in `docs/multi-module/`
- [x] README updated with actual implementation reference
- [ ] Individual files updated with "not implemented" notices
- [ ] New internal doc created: `.claude/docs/multi-module/collector-task-implementation.md`

---

## Next Steps for This Directory

**Option 1: Archive** (Recommended)
- Move to `.claude/docs/archive/kmp-multi-module-custom-source-sets/`
- Add comprehensive deprecation notice
- Keep as historical reference

**Option 2: Update In Place**
- Add deprecation notices to each file
- Update ARCHITECTURE-DECISION.md with "why we chose dedicated modules"
- Keep in current location with clear warnings

**Option 3: Delete**
- Remove entirely
- Decision rationale documented elsewhere

**Current Status**: Option 2 in progress (adding notices)

---

## Files in This Directory

| File | Status | Purpose |
|------|--------|---------|
| `README.md` | ✅ Updated | This file - explains actual vs planned |
| `ARCHITECTURE-DECISION.md` | ⚠️ Outdated | Documents custom source sets decision (NOT implemented) |
| `IMPLEMENTATION-ROADMAP.md` | ⚠️ Outdated | 3-week plan for custom source sets (NOT followed) |
| `TECHNICAL-REFERENCE.md` | ⚠️ Partial | Gradle fundamentals still useful, examples outdated |
| `CONVENTION-PLUGIN-BLUEPRINT.md` | ⚠️ Outdated | Convention plugin for custom source sets (NOT used) |
| `COMPARISON-MATRIX.md` | ⚠️ Outdated | Compares approaches, but conclusion changed |
| `FAQ.md` | ⚠️ Outdated | Answers assume custom source sets |

---

## For Contributors

If you're working on multi-module features:

1. **Read user docs first**: `docs/multi-module/` (actual implementation)
2. **Study working code**: `FakeCollectorTask.kt`, `samples/kmp-multi-module/`
3. **Then review this**: For context on alternatives considered
4. **Ask questions**: Open GitHub issue if unclear

---

## Last Updated

- **Original Documentation**: 2025-10-05 (custom source sets plan)
- **This README Update**: 2025-11-11 (actual implementation reference)
- **Status**: Preserved for historical reference, not active documentation
