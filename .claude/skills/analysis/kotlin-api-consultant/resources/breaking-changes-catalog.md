# Kotlin Compiler API Breaking Changes Catalog

Known breaking changes in Kotlin compiler APIs across versions - critical for maintaining compatibility.

## Kotlin 2.0.0 → 2.2.x

### IrGenerationExtension

**Status**: Stable (no breaking changes detected)

```kotlin
// Kotlin 2.0 through 2.2 - unchanged
interface IrGenerationExtension {
    fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)

    @FirIncompatiblePluginAPI
    val shouldAlsoBeAppliedInKaptStubGenerationMode: Boolean
        get() = false
}
```

**Recommendation**: ✅ Safe to use

---

### CompilerPluginRegistrar

**Status**: Stable with K2 requirement

**Critical Addition (Kotlin 1.9+)**:
```kotlin
// REQUIRED for Kotlin 2.x
override val supportsK2: Boolean = true
```

**Migration**:
```kotlin
// ❌ Kotlin 1.8 and below
class MyPlugin : CompilerPluginRegistrar() {
    // No supportsK2 field
}

// ✅ Kotlin 2.x
class MyPlugin : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true  // REQUIRED
}
```

**Recommendation**: Always set `supportsK2 = true`

---

### IrPluginContext

**Minor Addition (Kotlin 2.1+)**: New convenience methods

```kotlin
// New in 2.1
interface IrPluginContext {
    // Existing methods (stable)
    val irFactory: IrFactory
    val symbolTable: SymbolTable

    // NEW (2.1+): Convenience for referencing built-in types
    fun referenceBuiltInClass(classId: ClassId): IrClassSymbol?
}
```

**Impact**: Low (additive change, backwards compatible)

**Recommendation**: ✅ Use if available, fallback to manual resolution

---

## Kotlin 1.9.x → 2.0.0 (K2 Compiler)

### Major: FIR Compiler Architecture

**Breaking Change**: K1 (old frontend) → K2 (FIR frontend)

**Impact**:
- `FirExtensionRegistrar` required for K2 support
- `@FirIncompatiblePluginAPI` marks K1-only APIs
- Plugin must support both K1 and K2 during transition

**Migration**:
```kotlin
// Support both K1 and K2
class MyPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(config: CompilerConfiguration) {
        // FIR phase (K2 only)
        FirExtensionRegistrarAdapter.registerExtension(MyFirExtension())

        // IR phase (K1 and K2)
        IrGenerationExtension.registerExtension(MyIrExtension())
    }
}
```

**Recommendation**: Implement two-phase FIR → IR architecture

---

### @UnsafeApi Annotations

**Introduced**: Kotlin 2.0

**Purpose**: Mark APIs that may change without notice

```kotlin
@UnsafeApi
interface IrIntrinsicExtension {
    // Unstable API, may change
}
```

**Impact**: Medium (use at own risk)

**Recommendation**: Isolate behind abstraction layer

---

## Kotlin 1.8.x → 1.9.x

### IrFactory Changes

**Change**: Refactored factory methods

**Before (1.8)**:
```kotlin
irFactory.createClass(
    descriptor = descriptor,
    symbol = symbol
)
```

**After (1.9+)**:
```kotlin
irFactory.buildClass {
    name = Name.identifier("MyClass")
    kind = ClassKind.CLASS
}
```

**Impact**: High (different API shape)

**Recommendation**: Use builder pattern (1.9+)

---

### Symbol Table Changes

**Change**: SymbolTable API evolution

**Impact**: Medium (internal changes, public API mostly stable)

**Recommendation**: Use IrPluginContext.symbolTable, avoid direct SymbolTable manipulation

---

## Common Deprecations

### 1. Old IrFactory Methods

**Deprecated**: `createClass(descriptor, symbol)` style methods

**Replacement**: Builder pattern

```kotlin
// ❌ Deprecated
val irClass = irFactory.createClass(descriptor, symbol)

// ✅ Current
val irClass = irFactory.buildClass {
    name = Name.identifier("MyClass")
    kind = ClassKind.CLASS
    modality = Modality.FINAL
}
```

---

### 2. Direct Descriptor Access

**Deprecated**: Many `descriptor` properties on IR elements

**Replacement**: Use symbols and IR-native APIs

```kotlin
// ❌ Deprecated
val descriptor = irClass.descriptor

// ✅ Current
val symbol = irClass.symbol
val name = irClass.name
```

**Reason**: K2 compiler uses FIR, not descriptors

---

### 3. ComponentRegistrar

**Deprecated**: Old plugin registration API

**Replacement**: CompilerPluginRegistrar

```kotlin
// ❌ Deprecated (Kotlin 1.4 and below)
class MyPlugin : ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) { ... }
}

// ✅ Current (Kotlin 1.5+)
class MyPlugin : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) { ... }
}
```

---

## Experimental APIs to Watch

### @ExperimentalCompilerApi

**Purpose**: Mark experimental features

**Risk**: May change or be removed

**Example**:
```kotlin
@ExperimentalCompilerApi
fun someNewFeature() { ... }
```

**Recommendation**: Avoid in production unless necessary, monitor release notes

---

### @FirIncompatiblePluginAPI

**Purpose**: Mark K1-only APIs that don't work in K2

**Example**:
```kotlin
@FirIncompatiblePluginAPI
val shouldAlsoBeAppliedInKaptStubGenerationMode: Boolean
```

**Impact**: These APIs won't work in K2 compiler

**Recommendation**: Avoid or provide K2 alternatives

---

## Version Compatibility Matrix

| API | 1.8 | 1.9 | 2.0 (K2) | 2.1 | 2.2 |
|-----|-----|-----|----------|-----|-----|
| CompilerPluginRegistrar | ✅ | ✅ | ✅ (needs supportsK2) | ✅ | ✅ |
| IrGenerationExtension | ✅ | ✅ | ✅ | ✅ | ✅ |
| FirExtensionRegistrar | ❌ | ⚠️ | ✅ | ✅ | ✅ |
| Builder pattern IrFactory | ❌ | ✅ | ✅ | ✅ | ✅ |
| supportsK2 field | ❌ | ⚠️ | ✅ Required | ✅ | ✅ |

**Legend**:
- ✅ Supported and stable
- ⚠️ Transitional (some support)
- ❌ Not available

---

## Breaking Change Detection Strategy

### 1. Monitor Release Notes
```
https://github.com/JetBrains/kotlin/releases
- Check "Compiler" section
- Look for "Breaking changes"
- Read plugin API updates
```

### 2. Git Diff Key Files
```bash
cd /kotlin

# Check API changes between versions
git diff v2.0.0..v2.1.0 -- \
  compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/ \
  compiler/plugin-api/src/

# Focus on interface changes
git log --oneline --all -- \
  compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/IrGenerationExtension.kt
```

### 3. Annotation Scanning
```bash
# Find newly deprecated APIs
grep -r "@Deprecated" /kotlin/compiler/plugin-api/ --include="*.kt"

# Find unsafe/experimental APIs
grep -r "@UnsafeApi\|@ExperimentalCompilerApi" /kotlin/compiler/ --include="*.kt"
```

### 4. Test Across Versions
```kotlin
// In build.gradle.kts
tasks.test {
    systemProperty("kotlin.version", "2.0.0")
}

// Test against multiple Kotlin versions in CI
```

---

## Mitigation Strategies

### 1. Version-Specific Code
```kotlin
// Check Kotlin version at runtime
val kotlinVersion = KotlinVersion.CURRENT

if (kotlinVersion >= KotlinVersion(2, 0)) {
    // Use K2-specific APIs
} else {
    // Use K1 fallback
}
```

### 2. Abstraction Layers
```kotlin
// Wrap compiler APIs behind own interfaces
interface FaktIrFactory {
    fun createClass(name: String): IrClass
}

// Adapt to Kotlin compiler version internally
class FaktIrFactoryImpl(private val irFactory: IrFactory) : FaktIrFactory {
    override fun createClass(name: String): IrClass {
        return irFactory.buildClass { this.name = Name.identifier(name) }
    }
}
```

### 3. Graceful Degradation
```kotlin
// Try new API, fall back to old
fun tryNewApi(): IrClass? {
    return try {
        // Attempt new API (may not exist in older Kotlin)
        irFactory.buildClass { ... }
    } catch (e: NoSuchMethodError) {
        // Fall back to old API or report limitation
        null
    }
}
```

---

## Fakt-Specific Concerns

### Current Kotlin Version
```
Target: Kotlin 2.2.21
Minimum: Kotlin 2.0.0
```

### Critical APIs Used
1. **IrGenerationExtension** - Stable ✅
2. **CompilerPluginRegistrar** - Stable (needs supportsK2) ✅
3. **IrPluginContext** - Stable ✅
4. **IrFactory (builder pattern)** - Stable ✅
5. **FirExtensionRegistrar** - K2 specific ✅

### Risk Assessment
- **Low risk**: Core APIs stable across 2.x
- **Medium risk**: Experimental/unsafe APIs if used
- **High risk**: Supporting Kotlin 1.x (don't do it)

### Recommendation
✅ Stay on Kotlin 2.x
✅ Monitor release notes
✅ Test with each minor version update
✅ Avoid @UnsafeApi / @ExperimentalCompilerApi

---

## Resources

- **Kotlin Releases**: https://github.com/JetBrains/kotlin/releases
- **Compiler Plugin Guide**: https://kotlinlang.org/docs/compiler-plugins.html
- **K2 Compiler Docs**: https://github.com/JetBrains/kotlin/blob/master/docs/fir/fir-basics.md
- **Metro Examples**: `/metro/compiler/` (real-world K2 support)
