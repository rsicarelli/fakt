# Technical Reference

Deep technical documentation for FakeCollectorTask and multi-module internals.

---

## FakeCollectorTask

The core component responsible for collecting generated fakes from producer modules.

### Overview

```kotlin
package com.rsicarelli.fakt.gradle

abstract class FakeCollectorTask : DefaultTask() {
    @Input abstract val sourceProjectPath: Property<String>
    @Internal abstract val sourceGeneratedDir: DirectoryProperty
    @OutputDirectory abstract val destinationDir: DirectoryProperty
    @Input abstract val availableSourceSets: SetProperty<String>
    @Input abstract val logLevel: Property<LogLevel>
    
    @TaskAction
    fun collectFakes() { /* ... */ }
}
```

### Task Properties

#### sourceProjectPath

**Type**: `Property<String>`

**Description**: Gradle path to producer module

**Example**: `":core:analytics"`

**Configuration**:
```kotlin
// Via collectFakesFrom()
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

#### sourceGeneratedDir

**Type**: `DirectoryProperty`

**Description**: Root directory of generated fakes in producer

**Example**: `core/analytics/build/generated/fakt/`

**Auto-detected** from source project's build directory.

#### destinationDir

**Type**: `DirectoryProperty`

**Description**: Root directory for collected fakes in collector

**Example**: `core/analytics-fakes/build/generated/collected-fakes/`

**Auto-configured** based on collector's build directory.

#### availableSourceSets

**Type**: `SetProperty<String>`

**Description**: All KMP source sets available in collector module

**Example**: `["commonMain", "jvmMain", "iosMain", "iosArm64Main"]`

**Auto-detected** from collector's Kotlin multiplatform configuration.

#### logLevel

**Type**: `Property<LogLevel>`

**Description**: Logging verbosity

**Values**:
- `LogLevel.QUIET` - No output
- `LogLevel.INFO` - Summary (default)
- `LogLevel.DEBUG` - Detailed
- `LogLevel.TRACE` - Everything

---

## Platform Detection Algorithm

### Implementation

```kotlin
fun determinePlatformSourceSet(
    fileContent: String,
    availableSourceSets: Set<String>
): String {
    // 1. Extract package declaration (first 10 lines)
    val packageDeclaration = fileContent
        .lines()
        .take(10)
        .firstOrNull { it.trim().startsWith("package ") }
        ?.removePrefix("package ")
        ?.trim()
        ?: return "commonMain"

    // 2. Split into segments
    val segments = packageDeclaration.split(".")

    // 3. Find matching source sets
    val matches = segments.flatMap { segment ->
        availableSourceSets
            .filter { sourceSet ->
                sourceSet.startsWith(segment, ignoreCase = true) && 
                sourceSet.endsWith("Main")
            }
            .map { it to segment }
    }.distinct()

    // 4. Return shortest match (most general)
    return matches.minByOrNull { (sourceSet, _) -> sourceSet.length }?.first
        ?: "commonMain"
}
```

### Examples

#### Example 1: JVM-Specific Package

```kotlin
// Input
package com.example.jvm.database

// Process
segments = ["com", "example", "jvm", "database"]
availableSourceSets = ["commonMain", "jvmMain", "iosMain"]

// Matching
"jvm" matches "jvmMain" (starts with "jvm", ends with "Main")

// Output
"jvmMain"
```

#### Example 2: iOS with Multiple Variants

```kotlin
// Input
package com.example.ios.camera

// Process
segments = ["com", "example", "ios", "camera"]
availableSourceSets = ["commonMain", "iosMain", "iosArm64Main", "iosX64Main", "iosSimulatorArm64Main"]

// Matching
"ios" matches:
- "iosMain" (length: 7)
- "iosArm64Main" (length: 13)
- "iosX64Main" (length: 10)
- "iosSimulatorArm64Main" (length: 22)

// Output (shortest)
"iosMain"
```

#### Example 3: No Match (Fallback)

```kotlin
// Input
package com.example.business.logic

// Process
segments = ["com", "example", "business", "logic"]
availableSourceSets = ["commonMain", "jvmMain", "jsMain"]

// Matching
No segment matches any source set

// Output (fallback)
"commonMain"
```

---

## Task Registration

### KMP Projects

```kotlin
companion object {
    fun registerForKmpProject(
        project: Project,
        extension: FaktPluginExtension
    ) {
        val kotlin = project.extensions.getByType<KotlinMultiplatformExtension>()

        kotlin.targets.all { target ->
            target.compilations.all { compilation ->
                val taskName = "collectFakes${target.name.capitalize()}${compilation.name.capitalize()}"

                val task = project.tasks.register<FakeCollectorTask>(taskName) {
                    group = "fakt"
                    description = "Collect fakes from ${extension.collectFrom.get()}"

                    sourceProjectPath.set(extension.collectFrom)
                    logLevel.set(extension.logLevel)

                    // Auto-detect source sets
                    availableSourceSets.set(
                        kotlin.sourceSets.names.filter { it.endsWith("Main") }.toSet()
                    }

                    // Wire dependencies
                    dependsOn(sourceProject.tasks.matching { /* compile tasks */ })
                }

                // Register collected sources
                compilation.defaultSourceSet.kotlin.srcDir(
                    task.flatMap { it.destinationDir }
                )
            }
        }
    }
}
```

---

## Task Dependencies

### Automatic Wiring

FakeCollectorTask automatically depends on producer's compilation tasks:

```kotlin
// Pseudo-code
sourceProject.tasks.matching { task ->
    task.name.contains("compile", ignoreCase = true) &&
    !task.name.contains("test", ignoreCase = true)  // Skip test compilations
}.forEach { compileTask ->
    collectTask.dependsOn(compileTask)
}
```

**Why skip test compilations?**
- Test compilations may depend on collector modules (circular)
- Main compilations generate fakes (contain `@Fake` annotations)
- Test compilations use fakes (contain test code)

---

## Configuration Cache Compatibility

Fakt is fully compatible with Gradle configuration cache:

```bash
./gradlew build --configuration-cache
```

### Implementation

```kotlin
abstract class FakeCollectorTask : DefaultTask() {
    // ✅ Use Property<T> (serializable)
    @Input abstract val sourceProjectPath: Property<String>

    // ✅ Use DirectoryProperty (serializable)
    @OutputDirectory abstract val destinationDir: DirectoryProperty

    // ❌ Don't use Project references (not serializable)
    // private lateinit var project: Project  // Would break config cache
}
```

---

## Performance Characteristics

### Time Complexity

- **File discovery**: O(n) where n = number of generated files
- **Package extraction**: O(1) per file (only reads first 10 lines)
- **Platform detection**: O(m * k) where m = package segments, k = source sets
- **File copy**: O(n) where n = number of files

### Space Complexity

- **Memory**: O(n) for file list
- **Disk**: 2x (original + collected)

### Optimization

**Incremental**: Only reprocesses changed files

```bash
# First build
collectFakes: 40ms (100 files)

# No changes
collectFakes: UP-TO-DATE (0ms)

# 1 file changed
collectFakes: 5ms (1 file reprocessed)
```

---

## API Reference

### collectFakesFrom()

```kotlin
// Extension method on FaktPluginExtension

// Option 1: Type-safe project accessor
@ExperimentalFaktMultiModule
fun collectFakesFrom(project: ProjectDependency)

// Option 2: Traditional project reference
@ExperimentalFaktMultiModule
fun collectFakesFrom(project: Project)
```

**Both approaches are equally valid** - choose based on preference:
- Type-safe accessors: Better IDE support, requires `TYPESAFE_PROJECT_ACCESSORS`
- Traditional: Works everywhere, no feature preview needed

**Example (type-safe)**:
```kotlin
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

**Example (traditional)**:
```kotlin
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(project(":core:analytics"))
}
```

---

## Gradle Integration

### Plugin Lifecycle

```
1. Plugin applied: FaktGradleSubplugin.apply(project)
   ↓
2. Extension created: project.extensions.create<FaktPluginExtension>("fakt")
   ↓
3. Configuration phase: User configures collectFakesFrom()
   ↓
4. afterEvaluate: Mode detection (collector vs generator)
   ↓
5. Task registration: FakeCollectorTask.registerForKmpProject()
   ↓
6. Execution phase: collectFakes task runs
   ↓
7. Compilation: Collector compiles collected fakes
```

---

## Source Set Registration

Collected fakes are registered as source roots:

```kotlin
compilation.defaultSourceSet.kotlin.srcDir(
    task.flatMap { it.destinationDir }
)
```

**Result**: IDE and compiler recognize collected fakes as first-class sources.

---

## Next Steps

- [Getting Started](getting-started.md) - Setup guide
- [Advanced Topics](advanced.md) - Platform detection deep dive
- [Troubleshooting](troubleshooting.md) - Common issues
