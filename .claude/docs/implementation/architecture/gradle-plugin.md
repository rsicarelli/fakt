# Gradle Plugin Architecture

> **Current Implementation Status**  
> **Date**: November 2025  
> **Module**: `gradle-plugin/`

## 🎯 Overview

The Gradle plugin provides the build system integration for Fakt, bridging Gradle's compilation lifecycle with the Fakt compiler plugin. It implements `KotlinCompilerPluginSupportPlugin` to hook into Kotlin compilation and configure fake generation.

## 🏗️ Architecture

### Module Structure

```
gradle-plugin/
├── src/main/kotlin/com/rsicarelli/fakt/gradle/
│   ├── FaktGradleSubplugin.kt          # Main plugin entry point
│   ├── FaktPluginExtension.kt          # User-facing DSL (fakt { })
│   ├── SourceSetDiscovery.kt           # Detects and analyzes source sets
│   ├── SourceSetConfigurator.kt        # Configures output directories
│   ├── FakeCollectorTask.kt            # Multi-module fake collection
│   ├── ExperimentalFaktMultiModule.kt  # Multi-module opt-in annotation
│   └── GradleFaktLogger.kt             # Gradle-aware logging
└── src/test/kotlin/                    # Comprehensive test suite
```

### Plugin Lifecycle

```
┌──────────────────────────────────────────────────────────────┐
│ 1. Plugin Application (apply)                               │
│    • Create fakt { } extension                              │
│    • Determine mode: generator or collector                 │
│    • Configure source sets OR register collector tasks      │
│    • Add runtime dependencies                               │
└──────────────────────────────────────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────────┐
│ 2. Compilation Check (isApplicable)                         │
│    • Called for EACH compilation (main, test, jvmMain, etc.)│
│    • Returns true ONLY for main compilations                │
│    • Skips test compilations (they consume fakes)           │
└──────────────────────────────────────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────────┐
│ 3. Compiler Configuration (applyToCompilation)              │
│    • Serialize extension options                            │
│    • Build source set context (hierarchy, output dirs)      │
│    • Pass to compiler plugin as SubpluginOptions            │
└──────────────────────────────────────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────────┐
│ 4. Compiler Plugin Execution                                │
│    • Analyzes @Fake annotations in main source sets         │
│    • Generates fake implementations to test source sets     │
└──────────────────────────────────────────────────────────────┘
```

## 📝 Current DSL Configuration

### FaktPluginExtension

```kotlin
fakt {
    // Control plugin activation
    enabled.set(true)  // Default: true

    // Control compiler logging verbosity
    logLevel.set(LogLevel.INFO)  // Default: INFO
    // Options: QUIET, INFO, DEBUG

    // Multi-module: Collect fakes from another project (experimental)
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(project(":foundation"))
}
```

### Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | `Property<Boolean>` | `true` | Enable/disable fake generation entirely |
| `logLevel` | `Property<LogLevel>` | `INFO` | Compiler logging verbosity (QUIET/INFO/DEBUG) |
| `collectFrom` | `Property<Project>` | Not set | Source project for multi-module fake collection |

### Log Levels

**QUIET**: No output except errors (fastest, minimal noise)
```
(no output unless errors occur)
```

**INFO** (default): Concise summary with key metrics
```
✅ 10 fakes generated in 1.2s (6 cached)
Discovery: 120ms | Analysis: 340ms | Generation: 580ms
Cache hit rate: 40% (6/15)
```

**DEBUG**: Detailed breakdown with FIR + IR details
```
[DISCOVERY] 120ms - 15 interfaces, 3 classes
[ANALYSIS] 340ms
  ├─ PredicateCombiner (18ms) - NoGenerics
  ├─ PairMapper<T,U,K,V> (42ms) ⚠️ - ClassLevel
[GENERATION] 580ms (avg 58ms/interface)
  ├─ FIR + IR node inspection, type resolution
  ├─ Import resolution, source set mapping
```

## 🏢 Multi-Module Support

### Operating Modes

Fakt operates in two distinct modes:

1. **Generator Mode** (default): Generates fakes from `@Fake` annotated interfaces
2. **Collector Mode**: Collects generated fakes from another project

### Collector Mode Usage

**Purpose**: Enable a dedicated test module to consume fakes from a foundation/core module.

```kotlin
// settings.gradle.kts
include(":foundation", ":foundation-fakes")

// foundation/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

kotlin {
    jvm()
    iosArm64()
    sourceSets {
        commonMain {
            // Contains @Fake interfaces
        }
    }
}

// foundation-fakes/build.gradle.kts
@file:OptIn(ExperimentalFaktMultiModule::class)

plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

fakt {
    // Collect fakes generated in :foundation
    collectFakesFrom(project(":foundation"))
    
    // Or using type-safe accessors:
    // collectFakesFrom(projects.foundation)
}

kotlin {
    jvm()
    iosArm64()
    // Fakes automatically placed in correct platform source sets
}
```

### How Collector Mode Works

1. **Task Registration**: Creates `collectFakesFrom<Target>` tasks per platform
2. **Source Discovery**: Finds generated fakes in source project's build directory
3. **Cross-Platform Mapping**: Maps platform-specific fakes correctly
4. **File Copying**: Copies fakes to appropriate source sets in collector module
5. **Dependency Management**: Sets up proper project dependencies

### FakeCollectorTask

The `FakeCollectorTask` handles the mechanics of fake collection:

```kotlin
abstract class FakeCollectorTask : DefaultTask() {
    @get:InputDirectory
    abstract val sourceFakesDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val destinationSourceSet: DirectoryProperty
    
    @TaskAction
    fun collect() {
        // 1. Find all generated fake files
        // 2. Parse package declarations
        // 3. Map to correct source set structure
        // 4. Copy with package structure preservation
    }
}
```

## 🔌 Compiler Plugin Integration

### SubpluginOptions Serialization

The plugin serializes configuration into compiler plugin arguments:

```kotlin
override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>
): Provider<List<SubpluginOption>> {
    return provider {
        val context = SourceSetDiscovery.discoverContext(...)
        
        listOf(
            SubpluginOption(key = "enabled", value = extension.enabled.get().toString()),
            SubpluginOption(key = "logLevel", value = extension.logLevel.get().name),
            SubpluginOption(key = "outputDir", value = context.outputDirectory),
            SubpluginOption(key = "sourceSetContext", value = context.toJson())
        )
    }
}
```

### SourceSetContext

Critical data structure passed to the compiler plugin:

```kotlin
@Serializable
data class SourceSetContext(
    val name: String,                    // e.g., "jvmTest"
    val platform: Platform,              // JVM, JS, NATIVE, WASM, COMMON
    val outputDirectory: String,         // Where to generate fakes
    val hierarchy: List<String>,         // Source set dependencies
    val isTestSourceSet: Boolean,        // Always true for fake generation
    val associatedMainSourceSet: String  // Main source set name
)
```

## 🔍 Source Set Discovery

### SourceSetDiscovery

Analyzes Kotlin compilation structure to determine:

- Which source sets should generate fakes
- Platform-specific output directories
- Source set hierarchies for KMP projects
- Test/main source set associations

**Key Logic**:
```kotlin
fun discoverContext(
    compilation: KotlinCompilation<*>,
    project: Project
): SourceSetContext {
    // 1. Determine if this is a main compilation
    // 2. Find corresponding test source set
    // 3. Extract platform information
    // 4. Build source set hierarchy
    // 5. Calculate output directory
    // 6. Return serializable context
}
```

### Platform Detection

```kotlin
enum class Platform {
    JVM,     // JVM target
    JS,      // JavaScript target
    NATIVE,  // Native targets (iOS, macOS, Linux, etc.)
    WASM,    // WebAssembly targets
    COMMON   // Common metadata compilation
}
```

## 📦 Dependency Management

### Runtime Dependencies

The plugin automatically adds Fakt runtime to test configurations:

```kotlin
private fun addRuntimeDependencies(project: Project) {
    project.configurations.matching { 
        it.name.endsWith("TestImplementation", ignoreCase = true) 
    }.configureEach {
        project.dependencies.add(name, "com.rsicarelli.fakt:runtime:$version")
    }
}
```

### Collector Mode Dependencies

When in collector mode, establishes project dependencies:

```kotlin
// foundation-fakes depends on foundation's test compilation output
project.dependencies.add("testImplementation", sourceProject)
```

## 🧪 Testing Strategy

### Test Coverage

The gradle-plugin module has comprehensive test coverage:

- **FaktGradleSubpluginTest**: Plugin application and mode selection
- **SourceSetDiscoveryTest**: Source set analysis and context building
- **FakeCollectorTaskTest**: Multi-module fake collection
- **FaktGradleSubpluginSerializationTest**: SourceSetContext serialization
- **SimplifiedSourceSetConfigurationTest**: Source set configuration logic
- **CompilationClassifierTest**: Compilation type detection

### Testing Patterns

All tests follow GIVEN-WHEN-THEN structure:

```kotlin
@Test
fun `GIVEN KMP project WHEN discovering context THEN should identify platform correctly`() {
    // GIVEN
    val compilation = createFakeCompilation(target = "jvm")
    
    // WHEN
    val context = SourceSetDiscovery.discoverContext(compilation, project)
    
    // THEN
    assertEquals(Platform.JVM, context.platform)
    assertTrue(context.outputDirectory.contains("jvmTest"))
}
```

## 🚀 Future Enhancements

While the current implementation is functional and production-ready, potential future enhancements include:

- **Custom annotations**: Support company-owned `@TestDouble` annotations
- **Configurable output directories**: User control over generation paths
- **Performance metrics**: Compilation time and memory reporting
- **Multi-module dashboards**: Aggregated reporting across projects
- **Auto-configuration**: Smart defaults based on project size

## 📚 Key Design Decisions

1. **Two-mode architecture**: Clean separation between generator and collector modes
2. **Experimental multi-module**: Gated behind opt-in annotation for API stability
3. **Metro alignment**: Follows Kotlin compiler plugin patterns (compileOnly KGP dependencies)
4. **SourceSetContext serialization**: JSON-based data passing to compiler plugin
5. **Test-focused generation**: Only generates fakes for main compilations, consumed by tests

## 🔗 Related Documentation

- **[Multi-Module Documentation](../multi-module/)** - Detailed multi-module patterns
- **[Source Sets Guide](../source_sets/)** - Source set configuration patterns
- **[Architecture Overview](./ARCHITECTURE.md)** - Overall Fakt architecture

---

**This document reflects the current implementation as of November 2025. For planned features and roadmap, see the project's GitHub issues and milestones.**
