# Fakt Samples 🚀

This directory contains comprehensive samples demonstrating Fakt capabilities across different project architectures and use cases.

## Available Samples

### 🎯 **jvm-single-module**
A single-platform JVM module demonstrating Fakt for JVM-only projects:
- **Basic Interfaces**: Simple property and method faking
- **Suspend Functions**: Async/coroutine support with proper handling
- **Data Classes**: Value object faking (User, Product models)
- **Complex Scenarios**: Multi-parameter methods, nullable types, default values
- **Call Tracking**: Built-in call count tracking with AtomicInteger

**Source Set Targets**: `main` → `test`

### 📱 **android-single-module**
An Android Library module showcasing Fakt for Android projects:
- **Android Library Setup**: compileSdk=35, minSdk=24
- **Unit Tests**: Standard JUnit tests in `src/test/kotlin`
- **Same Scenarios**: Identical to jvm-single-module for consistency
- **AGP Compatibility**: Works with Android Gradle Plugin 8.12.3+

**Source Set Targets**: `main` → `test` (unit tests), `androidTest` (instrumented tests supported)

### 📦 **kmp-single-module**
A single KMP module demonstrating core Fakt features:
- **Basic Interfaces**: Simple property and method faking
- **Suspend Functions**: Async/coroutine support with proper handling
- **Data Classes**: Value object faking with copy semantics
- **Higher-Order Functions**: Function type parameters and lambdas
- **Generic Types**: Collections, Result types, and custom generics
- **Complex Scenarios**: Multi-parameter methods, nullable types, default values

**Source Set Targets**: `commonMain` → `commonTest`, `jvmMain` → `jvmTest`

### 🏗️ **kmp-multi-module**
A complex multi-module KMP project showcasing:
- **Cross-Module Faking**: Interfaces defined in one module, faked in another
- **Platform-Specific Fakes**: Different implementations per target platform
- **Shared Libraries**: Common interfaces with platform-specific faking
- **API/Implementation Split**: Clean architecture with faked boundaries
- **Advanced KMP Setup**: All targets (JVM, Android, iOS, JS, Native)

**Source Set Targets**: All KMP targets with proper test source set mapping

### 🎯 **kmp-multi-target** ⭐ NEW
A comprehensive KMP hierarchy validation sample demonstrating:
- **Platform-Specific Source Sets**: One @Fake interface per source set (jvm, ios, js, wasm, native)
- **Hierarchy Validation**: Tests that child source sets can access parent fakes
- **Multi-Level Inheritance**: iOS tests access commonMain + nativeMain + iosMain fakes
- **Source Set Isolation**: Platform-specific fakes don't leak across siblings
- **45+ Test Cases**: 30 basic tests + 15 hierarchy validation tests
- **Complete KMP Coverage**: Validates fake generation for ALL source sets

**Key Validations:**
- ✅ `commonMain` fakes accessible from ALL child source sets
- ✅ `nativeMain` fakes accessible from `iosTest` (multi-level)
- ✅ Platform-specific fakes isolated (jvm ≠ ios ≠ js ≠ wasm)
- ❌ Horizontal isolation maintained (jvmTest cannot access iosFakes)

**Source Set Targets**: `commonMain`, `jvmMain`, `iosMain`, `jsMain`, `wasmJsMain`, `nativeMain` → corresponding test source sets

### 📤 **fake-publishing** ⭐ NEW
A two-project sample validating Maven artifact publishing workflow:
- **Publisher Project**: Library with @Fake interfaces published to Maven Local
- **Consumer Project**: Separate Gradle project consuming published fakes
- **No Plugin Required**: Consumer uses fakes without Fakt compiler plugin
- **Real-World Workflow**: Validates library author publishing scenario

**Key Validations:**
- ✅ Fakes included in published JAR artifacts
- ✅ Factory functions accessible from external projects
- ✅ DSL configuration works at runtime
- ✅ KMP metadata properly exported

**Structure**: Two independent Gradle projects (`kmp-publisher` → `kmp-consumer`)

## 🎯 **Testing Philosophy**

Each sample serves as a **battle test** for the Fakt compiler plugin:

1. **Compilation Success**: All generated code must compile without errors
2. **Type Safety**: No `Any` casts, proper generic handling throughout
3. **Runtime Verification**: Factory functions work, DSL configuration functions
4. **Platform Coverage**: Every KMP target supported with appropriate test generation
5. **Edge Case Handling**: Complex types, nested generics, suspend functions, etc.

## 🔧 **Source Set Mapping Strategy**

Fakt generates fakes at the appropriate test source set level:

| Source Location    | Generated Location | Description                |
|--------------------|--------------------|----------------------------|
| `commonMain/`      | `commonTest/`      | Shared multiplatform fakes |
| `jvmMain/`         | `jvmTest/`         | JVM-specific fakes         |
| `androidMain/`     | `androidTest/`     | Android-specific fakes     |
| `iosMain/`         | `iosTest/`         | iOS-specific fakes         |
| `jsMain/`          | `jsTest/`          | JavaScript-specific fakes  |
| `main/` (JVM-only) | `test/`            | Non-KMP JVM projects       |

## 🚀 **Running the Samples**

```bash
# Option 1: Using composite builds (⭐ recommended for development)
# Samples auto-rebuild the plugin when needed!

# JVM single-module sample
cd samples/jvm-single-module
../../gradlew build

# Android single-module sample
cd samples/android-single-module
../../gradlew build

# KMP samples
cd samples/kmp-single-module
../../gradlew build

cd samples/kmp-multi-target  # ⭐ NEW: Hierarchy validation sample
../../gradlew allTests

cd samples/kmp-multi-module
../../gradlew build

# Option 2: Publishing plugin to local Maven first
../../gradlew publishToMavenLocal  # or: make publish-local
cd samples/jvm-single-module
../../gradlew build

# Clean and regenerate all fakes
../../gradlew clean build --no-build-cache
```

**💡 Note:** Samples use **composite builds** by default, which automatically rebuild the plugin when source changes. You typically don't need to run `publishToMavenLocal` unless testing published artifacts specifically.

## 📋 **Sample Validation Checklist**

Each sample must pass:

- ✅ **Compilation**: Zero errors in generated code
- ✅ **Type Safety**: Proper generics, no Any casts
- ✅ **Factory Functions**: `fakeServiceName {}` DSL works
- ✅ **Configuration**: Type-safe behavior setup
- ✅ **Platform Support**: All KMP targets compile and test
- ✅ **Source Set Mapping**: Fakes generated in correct test locations
- ✅ **Complex Types**: Generics, suspend functions, collections handled properly

## 🛠️ **Development Workflow**

When adding new features to Fakt:

1. **Add Test Cases**: Create complex interfaces in samples
2. **Verify Generation**: Check generated code quality and compilation
3. **Run Tests**: Ensure factory functions and DSL work correctly
4. **Platform Testing**: Test across all KMP targets
5. **Edge Cases**: Add increasingly complex type scenarios

These samples ensure Fakt maintains **MAP (Minimum Awesome Product)** quality standards! 🎯
