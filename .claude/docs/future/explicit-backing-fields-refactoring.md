# Explicit Backing Fields Refactoring Plan

> **Status**: Planned for future implementation
> **Kotlin Version**: 2.0 RC1+
> **Feature**: Explicit Backing Fields (Experimental)
> **Impact**: 50% reduction in call tracking code generation

## Overview

Refactor call tracking code generation to use Kotlin 2.0's Explicit Backing Fields feature, replacing the current backing field + public property pattern with a single property declaration.

### Reference

- [Kotlin KEEP Proposal](https://github.com/Kotlin/KEEP/blob/explicit-backing-fields/proposals/explicit-backing-fields.md)
- [Language Feature Documentation](https://kotlinlang.org/docs/whatsnew20.html#explicit-backing-fields)

---

## Current Pattern (4 lines)

```kotlin
private val _methodCallCount: MutableStateFlow<Int> = MutableStateFlow(0)

val methodCallCount: StateFlow<Int>
    get() = _methodCallCount
```

## New Pattern (2 lines)

```kotlin
val methodCallCount: StateFlow<Int>
    field = MutableStateFlow(0)
```

---

## Benefits

✅ **50% code reduction**: 2 lines vs 4 lines per tracked method/property
✅ **Eliminates naming conventions**: No more `_methodName` pattern
✅ **Cleaner generated code**: Matches modern Kotlin idioms
✅ **Better type safety**: Compiler enforces StateFlow/MutableStateFlow relationship
✅ **Idiomatic Kotlin 2.0**: Uses latest language features

---

## Implementation Phases

### Phase 1: PropertyBuilder Infrastructure

**1.1. Create BackingFieldSpec data class**
```kotlin
// Location: compiler/src/main/kotlin/com/rsicarelli/fakt/codegen/model/BackingFieldSpec.kt
data class BackingFieldSpec(
    val type: String? = null,  // Optional explicit type
    val initializer: String     // Required initializer expression
)
```

**1.2. Extend PropertyBuilder**
- File: `compiler/src/main/kotlin/com/rsicarelli/fakt/codegen/builder/PropertyBuilder.kt`
- Add `backingField: BackingFieldSpec?` property
- Add DSL method: `fun explicitBackingField(initializer: String)`
- Add validation: `require(backingField == null || getter == null)`

**1.3. Update CodeProperty model**
- Find and update `CodeProperty` data class
- Add `backingField: BackingFieldSpec?` field
- Update constructor validation

**1.4. Update property renderer**
- File: `compiler/src/main/kotlin/com/rsicarelli/fakt/codegen/renderer/Rendering.kt`
- Detect `backingField` presence in `CodeProperty.renderTo()`
- Emit `field = <initializer>` or `field: Type = <initializer>` syntax
- Skip getter/setter rendering when backing field exists

---

### Phase 2: Update Call Tracking Generation

**2.1. Refactor CallTrackingExtensions.kt**

File: `compiler/src/main/kotlin/com/rsicarelli/fakt/codegen/extensions/CallTrackingExtensions.kt`

**Before:**
```kotlin
fun ClassBuilder.callTrackingProperty(methodName: String) {
    val backingFieldName = "_${methodName}CallCount"
    val publicFieldName = "${methodName}CallCount"

    // Backing MutableStateFlow
    property(backingFieldName, "MutableStateFlow<Int>") {
        private()
        initializer = "MutableStateFlow(0)"
    }

    // Public StateFlow getter
    property(publicFieldName, "StateFlow<Int>") {
        getter = backingFieldName
    }
}
```

**After:**
```kotlin
fun ClassBuilder.callTrackingProperty(methodName: String) {
    val publicFieldName = "${methodName}CallCount"

    // Single property with explicit backing field
    property(publicFieldName, "StateFlow<Int>") {
        explicitBackingField(initializer = "MutableStateFlow(0)")
    }
}
```

**Apply same transformation to:**
- `propertyGetterTracking()` (lines 47-61)
- `propertySetterTracking()` (lines 74-89)

**2.2. Refactor ImplementationGenerator.kt** (V1 string-based generator)

File: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/generation/ImplementationGenerator.kt`

**Function 1: `generateCallTrackingFields()` (lines 443-465)**

**Before:**
```kotlin
// Line 446-447
appendLine("    private val _${function.name}CallCount = MutableStateFlow(0)")
appendLine("    val ${function.name}CallCount: StateFlow<Int> get() = _${function.name}CallCount")
```

**After:**
```kotlin
appendLine("    val ${function.name}CallCount: StateFlow<Int>")
appendLine("        field = MutableStateFlow(0)")
```

**Function 2: `generateClassCallTrackingFields()` (lines 1082-1127)**

Apply same transformation for:
- Abstract/open method tracking (lines ~1087-1099)
- Abstract/open property tracking (lines ~1104-1127)

---

### Phase 3: Configuration & Language Feature

**3.1. Enable language feature in sample projects**

File: `samples/kmp-single-module/build.gradle.kts`

```kotlin
kotlin {
    sourceSets.all {
        languageSettings {
            enableLanguageFeature("ExplicitBackingFields")
        }
    }
}
```

**3.2. Optional: Make feature configurable**

File: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/FaktExtension.kt`

```kotlin
abstract class FaktExtension {
    // ... existing properties ...

    /**
     * Use Kotlin 2.0 Explicit Backing Fields for call tracking.
     * Requires Kotlin 2.0+ and languageSettings.enableLanguageFeature("ExplicitBackingFields").
     *
     * Default: true
     */
    abstract val useExplicitBackingFields: Property<Boolean>
}
```

---

### Phase 4: Testing & Validation

**4.1. Verify generated code**
```bash
make quick-test
```

**4.2. Check specific files**
- `build/generated/fakt/commonTest/kotlin/.../FakeTrackedServiceImpl.kt`
- Verify call tracking still works
- Check proper formatting

**4.3. Run full test suite**
- All 415 tests should pass
- No compilation errors in generated code

---

## Technical Details

### Explicit Backing Fields Requirements

From the [KEEP proposal](https://github.com/Kotlin/KEEP/blob/explicit-backing-fields/proposals/explicit-backing-fields.md):

1. **Read-only properties only** - Cannot be `var`, must be `val`
2. **No custom getter allowed** - The `get()` accessor is implicit
3. **Private backing field** - Field is automatically private
4. **Type must be subtype** - Backing field type must be subtype of property type
5. **Language feature flag required**: `languageSettings.enableLanguageFeature("ExplicitBackingFields")`

### Perfect Match for Call Tracking

✅ StateFlow is **read-only** interface
✅ MutableStateFlow **is a subtype** of StateFlow
✅ Current implementation uses `val` (not `var`)
✅ Current getter is simple return (`get() = _backingField`)
✅ Eliminates manual backing field naming

---

## Example: Before & After

### Generated Code Comparison

**Before (Current Implementation):**
```kotlin
class FakeTrackedServiceImpl : TrackedService {
    // Method call tracking (4 lines)
    private val _simpleMethodCallCount: MutableStateFlow<Int> = MutableStateFlow(0)

    val simpleMethodCallCount: StateFlow<Int>
        get() = _simpleMethodCallCount

    private var simpleMethodBehavior: () -> String = { "" }

    override fun simpleMethod(): String {
        _simpleMethodCallCount.update { it + 1 }
        return simpleMethodBehavior()
    }

    // Property getter tracking (4 lines)
    private val _readOnlyPropertyCallCount: MutableStateFlow<Int> = MutableStateFlow(0)

    val readOnlyPropertyCallCount: StateFlow<Int>
        get() = _readOnlyPropertyCallCount

    private var readOnlyPropertyBehavior: () -> String = { "" }

    override val readOnlyProperty: String
        get() {
            _readOnlyPropertyCallCount.update { it + 1 }
            return readOnlyPropertyBehavior()
        }
}
```

**After (With Explicit Backing Fields):**
```kotlin
class FakeTrackedServiceImpl : TrackedService {
    // Method call tracking (2 lines) ✨
    val simpleMethodCallCount: StateFlow<Int>
        field = MutableStateFlow(0)

    private var simpleMethodBehavior: () -> String = { "" }

    override fun simpleMethod(): String {
        simpleMethodCallCount.update { it + 1 }  // Direct field access
        return simpleMethodBehavior()
    }

    // Property getter tracking (2 lines) ✨
    val readOnlyPropertyCallCount: StateFlow<Int>
        field = MutableStateFlow(0)

    private var readOnlyPropertyBehavior: () -> String = { "" }

    override val readOnlyProperty: String
        get() {
            readOnlyPropertyCallCount.update { it + 1 }  // Direct field access
            return readOnlyPropertyBehavior()
        }
}
```

**Reduction**: ~50% less code for call tracking infrastructure

---

## Migration Strategy

### Option 1: Direct Migration (Recommended)
- We're already on Kotlin 2.0
- Explicit Backing Fields is stable enough in RC1
- No backward compatibility needed

### Option 2: Gradual Migration (Conservative)
- Add configuration flag: `useExplicitBackingFields`
- Default to `true` for new projects
- Keep old pattern as fallback

**Recommendation**: Go with Option 1 (Direct Migration) since:
- Already using Kotlin 2.0
- Feature is in RC1 (stable enough)
- Simplifies codebase (no dual implementation)

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Experimental feature may change | Breaking changes in Kotlin updates | Track Kotlin release notes, update with GA |
| IDE may not fully support | Syntax highlighting issues | Document known issues, update with IDE releases |
| PropertyBuilder architecture changes | Potential bugs in generation | Comprehensive testing, isolated changes |
| Users on Kotlin < 2.0 | Cannot use generated code | Require Kotlin 2.0+ (already our baseline) |

---

## Files to Modify

### Core Infrastructure
1. **NEW**: `compiler/src/main/kotlin/com/rsicarelli/fakt/codegen/model/BackingFieldSpec.kt`
2. `compiler/src/main/kotlin/com/rsicarelli/fakt/codegen/builder/PropertyBuilder.kt`
3. `compiler/src/main/kotlin/com/rsicarelli/fakt/codegen/model/CodeProperty.kt` (find and update)
4. `compiler/src/main/kotlin/com/rsicarelli/fakt/codegen/renderer/Rendering.kt` (lines 172-210)

### Generation Code
5. `compiler/src/main/kotlin/com/rsicarelli/fakt/codegen/extensions/CallTrackingExtensions.kt` (lines 20-89)
6. `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/generation/ImplementationGenerator.kt` (lines 443-465, 1082-1127)

### Configuration
7. `samples/kmp-single-module/build.gradle.kts` (add language feature flag)
8. `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/FaktExtension.kt` (optional config)

### Testing
9. `samples/kmp-single-module/src/commonTest/kotlin/.../callTracking/CallTrackingTest.kt` (validation)

---

## Success Metrics

**Before**: ~8 lines per tracked method (backing field + getter + property + getter tracking)
**After**: ~4 lines per tracked method (50% reduction)

**Example Impact** (FakeTrackedServiceImpl with 11 tracked members):
- Current: ~88 lines of call tracking code
- With Explicit Backing Fields: ~44 lines
- **Saved**: 44 lines (50% reduction)

---

## Next Steps

1. ✅ Document plan (this file)
2. ⏸️ Implement PropertyBuilder infrastructure
3. ⏸️ Update CallTrackingExtensions.kt
4. ⏸️ Update ImplementationGenerator.kt
5. ⏸️ Add language feature flags
6. ⏸️ Test and validate
7. ⏸️ Update documentation

**Priority**: Medium (Nice-to-have improvement, not blocking)
**Effort**: ~4-6 hours
**Impact**: High (cleaner, more idiomatic generated code)

---

**Last Updated**: 2025-01-09
**Author**: AI Assistant via Claude Code
**Status**: Planned (not yet implemented)
