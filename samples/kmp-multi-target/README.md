# KMP Multi-Target Sample

> **Purpose**: Validates that Fakt generates fakes correctly for platform-specific source sets, not just `commonMain/commonTest`.

## 📋 Overview

This sample demonstrates Fakt's ability to:
1. ✅ Generate fakes for `commonMain` interfaces in `commonTest`
2. ✅ Generate fakes for platform-specific interfaces in their corresponding test source sets
3. ✅ Maintain source set isolation (JVM fakes don't leak to iOS tests, etc.)
4. ✅ Support the full KMP hierarchy (common → jvm/ios/js/wasm/native)

## 🏗️ Structure

```
kmp-multi-target/
├── commonMain/      → CommonPlatformService.kt (@Fake)
├── commonTest/      → CommonPlatformServiceTest.kt (tests own fake)
├── jvmMain/         → JvmOnlyService.kt (@Fake)
├── jvmTest/         → JvmOnlyServiceTest.kt (tests own fake)
│                    → JvmHierarchyTest.kt (tests commonMain fake) ⭐
├── iosMain/         → IosOnlyService.kt (@Fake)
├── iosTest/         → IosOnlyServiceTest.kt (tests own fake)
│                    → IosHierarchyTest.kt (tests commonMain + nativeMain fakes) ⭐⭐
├── jsMain/          → JsOnlyService.kt (@Fake)
├── jsTest/          → JsOnlyServiceTest.kt (tests own fake)
│                    → JsHierarchyTest.kt (tests commonMain fake) ⭐
├── wasmJsMain/      → WasmOnlyService.kt (@Fake)
├── wasmJsTest/      → WasmOnlyServiceTest.kt (tests own fake)
│                    → WasmHierarchyTest.kt (tests commonMain fake) ⭐
├── nativeMain/      → NativeOnlyService.kt (@Fake)
└── nativeTest/      → NativeOnlyServiceTest.kt (tests own fake)
                     → NativeHierarchyTest.kt (tests commonMain fake) ⭐
```

**⭐ = Hierarchy Test**: Validates that child source sets can access fakes from parent source sets
**⭐⭐ = Multi-Level Hierarchy**: iOS tests access from commonMain (grandparent) + nativeMain (parent)

## 🎯 Platform-Specific Services

### **CommonPlatformService** (all platforms)
- Logger-like functionality available on all platforms
- Generated fake available in `commonTest`
- Accessible from all platform tests

### **JvmOnlyService** (JVM only)
- File I/O and system properties (JVM-specific)
- Generated fake ONLY in `jvmTest`
- NOT available in iOS, JS, WASM, or Native tests

### **IosOnlyService** (iOS only)
- UserDefaults, device info (iOS-specific)
- Generated fake ONLY in `iosTest`
- Shared across iosX64, iosArm64, iosSimulatorArm64

### **JsOnlyService** (JavaScript only)
- DOM manipulation, localStorage (browser-specific)
- Generated fake ONLY in `jsTest`
- Available in both browser and Node.js environments

### **WasmOnlyService** (WebAssembly only)
- WASM module interactions, memory management
- Generated fake ONLY in `wasmJsTest`
- WebAssembly-specific capabilities

### **NativeOnlyService** (all native targets)
- C interop, native memory management
- Generated fake in `nativeTest`
- Shared across iOS, macOS, Linux, Windows native targets

## 🔗 KMP Hierarchy Tests

**CRITICAL**: These tests validate that Fakt respects the KMP source set hierarchy.

### **Hierarchy Structure**

```
commonMain (root)
├── jvmMain
├── jsMain
├── wasmJsMain
└── nativeMain
    └── iosMain (inherits from both commonMain + nativeMain)
```

### **What Each Hierarchy Test Validates**

| Test File | Source Set | Validates Access To |
|-----------|------------|---------------------|
| `JvmHierarchyTest.kt` | jvmTest | ✅ `FakeCommonPlatformServiceImpl` (from commonMain)<br>✅ `FakeJvmOnlyServiceImpl` (from jvmMain) |
| `IosHierarchyTest.kt` | iosTest | ✅ `FakeCommonPlatformServiceImpl` (from commonMain)<br>✅ `FakeNativeOnlyServiceImpl` (from nativeMain)<br>✅ `FakeIosOnlyServiceImpl` (from iosMain)<br>**⭐ Multi-level inheritance** |
| `JsHierarchyTest.kt` | jsTest | ✅ `FakeCommonPlatformServiceImpl` (from commonMain)<br>✅ `FakeJsOnlyServiceImpl` (from jsMain) |
| `WasmHierarchyTest.kt` | wasmJsTest | ✅ `FakeCommonPlatformServiceImpl` (from commonMain)<br>✅ `FakeWasmOnlyServiceImpl` (from wasmJsMain) |
| `NativeHierarchyTest.kt` | nativeTest | ✅ `FakeCommonPlatformServiceImpl` (from commonMain)<br>✅ `FakeNativeOnlyServiceImpl` (from nativeMain) |

### **Isolation Tests** (what should NOT work)

- ❌ `jvmTest` cannot access `FakeIosOnlyServiceImpl`
- ❌ `iosTest` cannot access `FakeJvmOnlyServiceImpl`
- ❌ `jsTest` cannot access `FakeNativeOnlyServiceImpl`
- ❌ Horizontal source sets are isolated from each other

## ✅ Expected Behavior

### **Compilation**
```bash
# Should compile without errors
./gradlew build

# Should generate fakes in correct locations:
# - build/generated/kmp/commonTest/kotlin/FakeCommonPlatformServiceImpl.kt
# - build/generated/kmp/jvmTest/kotlin/FakeJvmOnlyServiceImpl.kt
# - build/generated/kmp/iosTest/kotlin/FakeIosOnlyServiceImpl.kt
# - build/generated/kmp/jsTest/kotlin/FakeJsOnlyServiceImpl.kt
# - build/generated/kmp/wasmJsTest/kotlin/FakeWasmOnlyServiceImpl.kt
# - build/generated/kmp/nativeTest/kotlin/FakeNativeOnlyServiceImpl.kt
```

### **Testing**
```bash
# Run all tests across all platforms
./gradlew allTests

# Run platform-specific tests
./gradlew jvmTest
./gradlew iosX64Test
./gradlew jsTest
./gradlew wasmJsTest
```

### **Source Set Isolation**
- ✅ `JvmOnlyService` fake is NOT accessible from `iosTest`
- ✅ `IosOnlyService` fake is NOT accessible from `jvmTest`
- ✅ `CommonPlatformService` fake IS accessible from all platform tests
- ✅ Each fake is generated only once in its target source set

## 🔍 Validation Criteria

For this sample to be successful, Fakt must:

1. **Detect `@Fake` in all source sets**
   - FIR phase processes commonMain, jvmMain, iosMain, jsMain, wasmJsMain, nativeMain

2. **Generate fakes in correct test source sets**
   - commonMain → commonTest
   - jvmMain → jvmTest
   - iosMain → iosTest
   - jsMain → jsTest
   - wasmJsMain → wasmJsTest
   - nativeMain → nativeTest

3. **Respect source set hierarchy**
   - Native hierarchy: nativeMain → iosMain → specific targets
   - Generated code follows Kotlin's source set dependencies

4. **Compile generated code**
   - All generated fakes compile without errors
   - No type resolution issues
   - No import conflicts

5. **Execute tests successfully**
   - All tests pass on their respective platforms
   - Call tracking works correctly
   - Configuration DSL is type-safe

## 🚨 Known Challenges

This sample helps identify:

- **FIR Phase**: Does it process all source sets or only common?
- **IR Phase**: Can it target specific test source sets?
- **Import Resolution**: Are imports correct for each platform?
- **Source Set Mapping**: Does Fakt know which test source set corresponds to each main source set?

## 🏃 Quick Start

```bash
# From project root, publish Fakt to mavenLocal
make publish-local

# Navigate to sample
cd samples/kmp-multi-target

# Build and test
./gradlew build
./gradlew allTests

# Check generated code
ls -la build/generated/kmp/*/kotlin/Fake*.kt
```

## 📊 Success Metrics

### **Code Generation**
- ✅ 6 interfaces compiled (@Fake in each source set)
- ✅ 6 fakes generated (one per source set)
- ✅ Fakes generated in correct test source sets

### **Test Execution**
- ✅ 6 basic test classes pass (one per source set)
- ✅ 5 hierarchy test classes pass (jvm, ios, js, wasm, native)
- ✅ **Total: 11 test classes** with **45+ test cases**

### **Hierarchy Validation**
- ✅ `commonMain` fakes accessible from ALL child source sets
- ✅ `nativeMain` fakes accessible from `iosTest` (multi-level)
- ✅ Platform-specific fakes isolated (jvm ≠ ios ≠ js ≠ wasm)

### **Quality**
- ✅ 0 compilation errors
- ✅ 0 import resolution errors
- ✅ Source set isolation maintained
- ✅ KMP hierarchy respected

---

**Status**: 🚧 Ready for testing (requires network connectivity)
