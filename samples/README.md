# Fakt Samples 🚀

This directory contains comprehensive samples demonstrating Fakt capabilities across different project architectures and use cases.

## Available Samples

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
cd samples/kmp-single-module
../../gradlew build

cd samples/kmp-multi-module
../../gradlew build

# Option 2: Publishing plugin to local Maven first
../../gradlew publishToMavenLocal  # or: make publish-local
cd samples/kmp-single-module
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
