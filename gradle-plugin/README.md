# Gradle Plugin Module

> **Gradle integration layer for Fakt compiler plugin**

This module provides the Gradle plugin that integrates the Fakt compiler plugin into Kotlin builds. It implements `KotlinCompilerPluginSupportPlugin` to hook into the Kotlin compilation lifecycle and bridge Gradle's build system with the Fakt compiler plugin.

## 🎯 Overview

The gradle-plugin module handles:

- **Plugin Registration**: Registers Fakt compiler plugin with Kotlin compilations
- **Configuration DSL**: Provides the `fakt { }` configuration block for users
- **Source Set Management**: Configures output directories and source set dependencies
- **Multi-Module Support**: Handles fake collection across project boundaries (experimental)
- **Dependency Management**: Automatically adds runtime dependencies to test configurations

## 🏗️ Architecture

### Plugin Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Plugin Application (apply)                                  │
│    • Create `fakt { }` extension                               │
│    • Configure source sets OR register collector tasks         │
│    • Add runtime dependencies                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. Compilation Check (isApplicable)                            │
│    • Called for EACH compilation (main, test, jvmMain, etc.)  │
│    • Returns true ONLY for main compilations                   │
│    • Skips test compilations (they USE generated fakes)        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. Compiler Configuration (applyToCompilation)                 │
│    • Serialize extension options to compiler plugin args       │
│    • Build source set context (hierarchy, output dirs)         │
│    • Pass to Fakt compiler plugin as SubpluginOptions          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. Fakt Compiler Plugin Execution                              │
│    • Analyzes @Fake annotations in main source sets            │
│    • Generates fake implementations to test source sets        │
└─────────────────────────────────────────────────────────────────┘
```

### Integration with Kotlin Gradle Plugin (KGP)

```
User's build.gradle.kts
        ↓
    plugins {
      id("com.rsicarelli.fakt")
    }
        ↓
FaktGradleSubplugin.apply()
        ↓
Kotlin Gradle Plugin (KGP)
        ↓
For each KotlinCompilation:
  1. isApplicable(compilation) → true/false
  2. If true: applyToCompilation(compilation)
        ↓
Serialize to compiler args:
  -P plugin:com.rsicarelli.fakt:enabled=true
  -P plugin:com.rsicarelli.fakt:logLevel=INFO
  -P plugin:com.rsicarelli.fakt:sourceSetContext=<base64-json>
        ↓
Kotlin Compiler invoked with Fakt plugin
```

## 📦 Key Components

| Component                   | Purpose                                                                 | Type                                  |
|-----------------------------|-------------------------------------------------------------------------|---------------------------------------|
| **FaktGradleSubplugin**     | Main plugin entry point, implements `KotlinCompilerPluginSupportPlugin` | Public                                |
| **FaktPluginExtension**     | DSL for `fakt { }` configuration block                                  | Public                                |
| **SourceSetConfigurator**   | Configures source sets and output directories                           | Internal                              |
| **SourceSetDiscovery**      | Builds source set context for compiler plugin                           | Internal                              |
| **SourceSetGraphTraversal** | BFS traversal of KMP source set hierarchy                               | Internal                              |
| **CompilationClassifier**   | Determines if compilation is test vs main                               | Internal                              |
| **FakeCollectorTask**       | Multi-module fake collection (experimental)                             | Public (@ExperimentalFaktMultiModule) |
| **GradleFaktLogger**        | Level-aware logging for tasks                                           | Public                                |

## 🚀 Usage Examples

### Basic Configuration (Single-Module)

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.21"
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

fakt {
    enabled.set(true)  // Default: true
    logLevel.set(LogLevel.INFO)  // Default: INFO
}
```

### Advanced Configuration

```kotlin
// build.gradle.kts
import com.rsicarelli.fakt.compiler.api.LogLevel

fakt {
    // Enable/disable plugin
    enabled.set(true)

    // Logging verbosity: QUIET, INFO, DEBUG
    logLevel.set(LogLevel.DEBUG)

    // Call history generation (default: true)
    enableCallHistory.set(true)  // Set to false for lightweight fakes

    // Use FIR-based analysis (experimental)
    useFirAnalysis.set(true)
}
```

### Multi-Module Setup (Experimental)

```kotlin
// module-consumer/build.gradle.kts
@OptIn(ExperimentalFaktMultiModule::class)
fakt {
    // Option 1: String-based (traditional)
    collectFakesFrom(project(":module-source"))

    // Option 2: Type-safe accessor (recommended) ✨
    collectFakesFrom(projects.moduleSource)
}
```

**Enable type-safe project accessors in settings.gradle.kts:**
```kotlin
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
```

## 🧪 Testing Approach

### Plugin Testing Strategy

The gradle-plugin module uses **functional testing** with Gradle's `ProjectBuilder`:

```kotlin
@Test
fun `GIVEN plugin applied WHEN project evaluated THEN extension created`() {
    // GIVEN
    val project = ProjectBuilder.builder().build()

    // WHEN
    project.plugins.apply("com.rsicarelli.fakt")

    // THEN
    val extension = project.extensions.findByType(FaktPluginExtension::class.java)
    assertNotNull(extension)
}
```

### What We Test

1. **Plugin Application**: Extension creation, source set configuration
2. **Compilation Filtering**: `isApplicable()` logic for different compilations
3. **Option Serialization**: Correct SubpluginOptions passed to compiler
4. **Multi-Module**: Collector task registration and execution

### Running Tests

```bash
# Run all gradle-plugin tests
./gradlew :gradle-plugin:test

# Run specific test class
./gradlew :gradle-plugin:test --tests "*FaktGradleSubpluginTest*"
```

## 🛠️ Development Guide

### Project Structure

```
gradle-plugin/
├── src/
│   ├── main/kotlin/com/rsicarelli/fakt/gradle/
│   │   ├── FaktGradleSubplugin.kt          # Main plugin
│   │   ├── FaktPluginExtension.kt          # DSL extension
│   │   ├── SourceSetConfigurator.kt        # Output directory config
│   │   ├── SourceSetDiscovery.kt           # Context builder
│   │   ├── SourceSetGraphTraversal.kt      # BFS hierarchy
│   │   ├── CompilationClassifier.kt        # Test vs main
│   │   ├── FakeCollectorTask.kt            # Multi-module support
│   │   ├── GradleFaktLogger.kt             # Logging utility
│   │   └── ExperimentalFaktMultiModule.kt  # Opt-in annotation
│   └── test/kotlin/...                      # Functional tests
├── build.gradle.kts
└── README.md (this file)
```

### Building the Plugin

```bash
# Build and publish to local Maven
./gradlew :gradle-plugin:publishToMavenLocal

# Test in sample projects (see working examples)
cd samples/jvm-single-module && ./gradlew build --info
cd samples/android-single-module && ./gradlew build --info
cd samples/kmp-single-module && ./gradlew build --info
```

**See working examples:**
- JVM-only: `samples/jvm-single-module`
- Android: `samples/android-single-module`
- KMP: `samples/kmp-single-module`

```

### Debugging

Enable debug logging to see plugin lifecycle:

```bash
./gradlew build --info | grep "Fakt:"
```

Expected output:
```
Fakt: Applied Gradle plugin to project sample-project
Fakt: Checking compilation 'main' - applicable: true
Fakt: Applying compiler plugin to compilation main
Fakt: Configured compiler plugin with 5 options
```

### Adding New Configuration Options

**1. Add property to FaktPluginExtension:**

```kotlin
// FaktPluginExtension.kt
abstract val myNewOption: Property<Boolean>

init {
    myNewOption.convention(false)
}
```

**2. Serialize in applyToCompilation:**

```kotlin
// FaktGradleSubplugin.kt
add(SubpluginOption(key = "myNewOption", value = extension.myNewOption.get().toString()))
```

**3. Read in compiler plugin:**

```kotlin
// compiler/src/.../FaktCommandLineProcessor.kt
"myNewOption" -> configuration.myNewOption = value.toBoolean()
```

## 🔍 Troubleshooting

### Plugin Not Applied

**Symptom**: No `fakt { }` extension available

**Solution**: Ensure plugin is applied AFTER Kotlin plugin:

```kotlin
plugins {
    kotlin("multiplatform") // Must come first
    id("com.rsicarelli.fakt")
}
```

### Generated Code Not Found

**Symptom**: Unresolved reference to `fakeXxx()` function

**Causes**:
1. Plugin not applied to correct compilation
2. Output directory not configured in source sets
3. `@Fake` annotation in test source set (must be in main)

**Debug**:
```bash
./gradlew build --info | grep "Fakt:"
# Check: "applicable: true" for main compilations only
# Check: Output directory matches test source set
```

### Collector Mode Issues

**Symptom**: Fakes not copied from source module

**Solution**: Ensure source module is evaluated first:

```kotlin
// settings.gradle.kts
include(":module-source")  // Must come before consumer
include(":module-consumer")
```

### KMP Source Set Resolution

**Symptom**: Fakes not visible in all platform tests

**Solution**: Check commonMain detection logic:

- If `@Fake` in `commonMain` → fakes generated to `commonTest`
- If `@Fake` in `jvmMain` → fakes generated to `jvmTest`

Use `--info` logging to verify output directory.

## 📚 Gradle API Compatibility

| Gradle Version | Status | Notes |
|----------------|--------|-------|
| 8.10+ | ✅ Tested | Recommended (KMP 2.0 support) |
| 8.5 - 8.9 | ✅ Compatible | Standard KMP projects |
| 7.x | ⚠️ Untested | May work, but not officially supported |
| < 7.0 | ❌ Not supported | Missing Kotlin Gradle Plugin APIs |

### Kotlin Gradle Plugin (KGP) Compatibility

- **Minimum**: 2.0.0
- **Recommended**: 2.2.21+
- **Tested**: 2.2.21

## 🤝 Contributing

### Guidelines

1. **Follow Gradle Best Practices**:
   - Avoid `afterEvaluate` when possible (use Providers)
   - Make plugins configuration-cache compatible
   - Use lazy APIs (`project.provider`, `Property<T>`)

2. **Testing Requirements**:
   - GIVEN-WHEN-THEN naming pattern (mandatory)
   - Vanilla JUnit5 + kotlin-test
   - Test both single-platform and KMP scenarios

3. **Documentation Standards**:
   - KDoc on all public APIs
   - @param/@return tags required
   - Usage examples for complex APIs

### Running Checks

```bash
# Format code
./gradlew :gradle-plugin:spotlessApply

# Lint
./gradlew :gradle-plugin:detekt

# Tests
./gradlew :gradle-plugin:test

# Documentation (see docs/ for MkDocs site)
```

## 📖 Additional Resources

- **Main Project README**: `../README.md`
- **Compiler Plugin**: `../compiler/`
- **Annotations Module**: `../annotations/`
- **Samples**:
  - JVM-only: `../samples/jvm-single-module/`
  - Android: `../samples/android-single-module/`
  - KMP: `../samples/kmp-single-module/`
- **Gradle Plugin Guide**: https://docs.gradle.org/current/userguide/custom_plugins.html
- **Kotlin Compiler Plugins**: https://kotlinlang.org/docs/compiler-plugins.html

## 📄 License

Apache License 2.0 - See `../LICENSE` for details.

---

**Maintained by**: Rodrigo Sicarelli ([@rsicarelli](https://github.com/rsicarelli))
