# FakeCollectorTask Implementation - Internal Reference

**Audience**: Contributors, maintainers, advanced developers

**Purpose**: Deep technical documentation of the FakeCollectorTask implementation

**Last Updated**: November 2025

---

## Overview

`FakeCollectorTask` is the core component of Fakt's multi-module support. It intelligently collects generated fakes from producer modules and places them in appropriate platform-specific source sets in collector modules.

**Source File**: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/FakeCollectorTask.kt`

---

## Architecture

### Component Hierarchy

```
FaktGradleSubplugin (entry point)
  ↓
FaktPluginExtension.collectFakesFrom() (user DSL)
  ↓
FakeCollectorTask.registerForKmpProject() (task registration)
  ↓
FakeCollectorTask.collectFakes() (task execution)
  ↓
collectWithPlatformDetection() (per-source-set collection)
  ↓
determinePlatformSourceSet() (platform detection)
  ↓
matchPackageToSourceSet() (dynamic source set matching)
```

### Class Definition

```kotlin
package com.rsicarelli.fakt.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*

@ExperimentalFaktMultiModule
abstract class FakeCollectorTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val sourceProjectPath: Property<String>

    @get:Internal  // Not @InputDirectory to allow missing directories
    abstract val sourceGeneratedDir: DirectoryProperty

    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val availableSourceSets: SetProperty<String>

    @get:Input
    abstract val logLevel: Property<LogLevel>

    @TaskAction
    fun collectFakes() { /* ... */ }

    companion object {
        fun registerForKmpProject(
            project: Project,
            extension: FaktPluginExtension
        ) { /* ... */ }

        fun registerSingleCollectorTask(
            project: Project,
            extension: FaktPluginExtension
        ) { /* ... */ }

        // PUBLIC methods for platform detection
        fun determinePlatformSourceSet(
            fileContent: String,
            availableSourceSets: Set<String> = emptySet()
        ): String { /* ... */ }

        private fun matchPackageToSourceSet(
            packageSegments: List<String>,
            availableSourceSets: Set<String>
        ): String { /* ... */ }

        private fun collectWithPlatformDetection(
            sourceDir: File,
            destinationBaseDir: File,
            availableSourceSets: Set<String>,
            faktLogger: GradleFaktLogger
        ): CollectionResult { /* ... */ }
    }
}
```

---

## Task Properties

### sourceProjectPath

**Type**: `Property<String>`

**Purpose**: Gradle path to producer module

**Configuration**: Automatically set from `collectFakesFrom()`

**Example Value**: `":core:analytics"`

**Why Property<T>**: Configuration cache compatibility (serializable)

### sourceGeneratedDir

**Type**: `DirectoryProperty`

**Purpose**: Root directory of generated fakes in producer

**Auto-detected from**: `sourceProject.layout.buildDirectory.dir("generated/fakt")`

**Example Value**: `core/analytics/build/generated/fakt/`

**Why DirectoryProperty**: Configuration cache safe, lazy evaluation

**Why @Internal**: Not using @InputDirectory to allow missing directories - warnings are handled gracefully in task execution instead of failing configuration

### destinationDir

**Type**: `DirectoryProperty`

**Purpose**: Root directory for collected fakes

**Auto-configured as**: `project.layout.buildDirectory.dir("generated/collected-fakes")`

**Example Value**: `core/analytics-fakes/build/generated/collected-fakes/`

**Why @OutputDirectory**: Gradle incremental compilation tracking

### availableSourceSets

**Type**: `SetProperty<String>`

**Purpose**: All main source sets available in collector module

**Auto-detected from**: Kotlin multiplatform extension's source sets

**Example Value**: `["commonMain", "jvmMain", "iosMain", "iosArm64Main", "jsMain"]`

**Why Set**: Unique source set names, order doesn't matter

**Filter**: Only source sets ending with "Main" (excludes "Test" source sets)

### logLevel

**Type**: `Property<LogLevel>`

**Purpose**: Logging verbosity control

**Values**: `QUIET`, `INFO`, `DEBUG`, `TRACE`

**Default**: `INFO`

---

## Task Registration

### Entry Point

```kotlin
// FaktGradleSubplugin.kt
override fun apply(target: Project): Provider<List<SubpluginOption>> {
    val extension = target.extensions.create<FaktPluginExtension>("fakt")

    target.afterEvaluate {
        if (extension.collectFrom.isPresent) {
            // COLLECTOR MODE
            FakeCollectorTask.registerForKmpProject(target, extension)
        } else {
            // GENERATOR MODE (default)
            SourceSetConfigurator.configureSourceSets(/*...*/)
        }
    }

    return target.provider { emptyList() }
}
```

### Registration Logic

```kotlin
companion object {
    fun registerForKmpProject(
        project: Project,
        extension: FaktPluginExtension
    ) {
        val srcProject = extension.collectFrom.orNull ?: return

        val kotlinExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        if (kotlinExtension == null) {
            // For non-KMP projects, create a single collector task
            registerSingleCollectorTask(project, extension)
            return
        }

        // Extract available source set names for dynamic platform detection
        val availableSourceSetNames = kotlinExtension.sourceSets.names

        // Create SINGLE collector task with platform detection
        val taskName = "collectFakes"
        val task = project.tasks.register(taskName, FakeCollectorTask::class.java) {
            it.sourceProjectPath.set(srcProject.path)

            // Point to root fakt directory - task will auto-discover subdirectories
            it.sourceGeneratedDir.set(
                srcProject.layout.buildDirectory.dir("generated/fakt")
            )

            // Base directory for platform-specific collection
            // Task will create subdirectories: commonMain/, jvmMain/, etc.
            it.destinationDir.set(
                project.layout.buildDirectory.dir("generated/collected-fakes/_placeholder")
            )

            // Configure available source sets for dynamic platform detection
            // This enables support for ALL KMP targets without hardcoding
            it.availableSourceSets.set(availableSourceSetNames)

            // Wire logLevel from extension for consistent telemetry
            it.logLevel.set(extension.logLevel)

            // Add dependency on source project's MAIN compilation tasks only
            // Avoid test compilations to prevent circular dependencies
            it.dependsOn(
                srcProject.tasks.matching { task ->
                    task.name.contains("compile", ignoreCase = true) &&
                    !task.name.contains("test", ignoreCase = true)
                }
            )
        }

        // Register ALL *Main source sets (commonMain, jvmMain, iosMain, etc.)
        kotlinExtension.sourceSets
            .matching { sourceSet -> sourceSet.name.endsWith("Main") }
            .configureEach { sourceSet ->
                val platformDir = task.map {
                    it.destinationDir.asFile
                        .get()
                        .parentFile  // up from _placeholder
                        .resolve("${sourceSet.name}/kotlin")
                }
                sourceSet.kotlin.srcDir(platformDir)

                // Wire task dependencies: ensure compilation tasks depend on collectFakes
                // This guarantees fakes are collected before any compilation that uses them
                // Uses type-based matching for robustness (only Kotlin tasks, not Java/Groovy)
                project.tasks.matching { compileTask ->
                    // Type-based: only Kotlin compilation tasks
                    (compileTask is KotlinCompile ||
                     compileTask is Kotlin2JsCompile ||
                     compileTask is KotlinNativeCompile) &&
                    // Name-based: match source set name
                    compileTask.name.contains(sourceSet.name, ignoreCase = true) &&
                    // Safety: avoid test compilations
                    !compileTask.name.contains("test", ignoreCase = true)
                }.configureEach { compileTask ->
                    compileTask.dependsOn(task)
                }
            }
    }
}
```

**Key Design Decisions**:

1. **Single Task Per Project**: One `collectFakes` task instead of per-target tasks
   - Why: Simplifies task graph, auto-discovers all source sets
   - Example: Just `collectFakes` (not `collectFakesJvmMain`, etc.)
   - Task internally handles platform distribution

2. **Auto-Discovery**: Task discovers source sets from `build/generated/fakt/` subdirectories
   - Why: Works with any number of source sets automatically
   - No hardcoding of commonTest, jvmTest, etc.

3. **Type-Based Task Matching**: Uses `instanceof` checks for compilation tasks
   - Why: More robust than string matching
   - Covers: KotlinCompile, Kotlin2JsCompile, KotlinNativeCompile
   - Avoids: Java/Groovy compilation tasks

4. **Depends on Source Compile Tasks**: Collector depends on producer's compilation
   - Why: Fakes must be generated before collection
   - Filter: Skip test compilations (would create circular dependencies)

5. **Lazy Evaluation**: Uses `task.map { }` for platform directories
   - Why: Configuration cache compatibility
   - Benefit: Task graph built lazily, evaluated at execution time

---

## Platform Detection Algorithm

### The Problem

Given a generated fake file, determine which KMP source set it should be placed in.

**Example**:
- File with package `com.example.jvm.database` → Should go in `jvmMain/kotlin/`
- File with package `com.example.ios.camera` → Should go in `iosMain/kotlin/`
- File with package `com.example.shared.api` → Should go in `commonMain/kotlin/` (fallback)

### The Algorithm

```kotlin
// Public function for platform detection
fun determinePlatformSourceSet(
    fileContent: String,
    availableSourceSets: Set<String> = emptySet()
): String {
    // Step 1: Extract package declaration (first 10 lines for performance)
    val packageDeclaration = fileContent
        .lines()
        .take(PACKAGE_SCAN_LINES)  // const val PACKAGE_SCAN_LINES = 10
        .firstOrNull { it.trim().startsWith("package ") }
        ?.removePrefix("package ")
        ?.trim()
        ?: return "commonMain"  // No package → default

    // Step 2: Split package into segments
    val packageSegments = packageDeclaration.split(".")
    // "com.example.ios.auth" → ["com", "example", "ios", "auth"]

    // Step 3: Dynamic matching using real source sets from project
    return matchPackageToSourceSet(packageSegments, availableSourceSets)
}

// Private helper for actual matching logic
private fun matchPackageToSourceSet(
    packageSegments: List<String>,
    availableSourceSets: Set<String>
): String {
    // Find all source sets that match package segments (case-insensitive prefix match)
    val matchedSourceSets = packageSegments.flatMap { segment ->
        availableSourceSets
            .filter { sourceSet ->
                // Match if source set name starts with segment
                // Examples: "ios" matches "iosMain", "iosArm64Main"
                sourceSet.startsWith(segment, ignoreCase = true) &&
                sourceSet.endsWith("Main")
            }
            .map { sourceSet -> sourceSet to segment }
    }.distinct()

    // Return shortest match (most general = hierarchical parent)
    return matchedSourceSets.minByOrNull { (sourceSet, _) -> sourceSet.length }?.first
        ?: "commonMain"  // No match → fallback
}
```

### Why "Shortest Match"?

**Problem**: Multiple source sets may match the same segment

**Example**:
```
Package: com.example.ios.camera
Available: [commonMain, iosMain, iosArm64Main, iosX64Main, iosSimulatorArm64Main]

Segment "ios" matches:
- iosMain (length: 7) ← SHORTEST
- iosArm64Main (length: 13)
- iosX64Main (length: 10)
- iosSimulatorArm64Main (length: 22)
```

**Solution**: Choose **iosMain** (shortest = most general = hierarchical parent)

**Rationale**:
- KMP source sets are hierarchical: `commonMain` → `iosMain` → `iosArm64Main`
- Fakes in `iosMain` are available to ALL iOS targets
- Fakes in `iosArm64Main` are only for that specific architecture
- Unless explicitly platform-specific, fakes should be in the most general source set

### Edge Cases

**Case 1: No package declaration**

```kotlin
// File has no package statement
// → Returns "commonMain" (safe fallback)
```

**Case 2: Package with no platform identifier**

```kotlin
package com.example.business.logic
// No segment matches any source set
// → Returns "commonMain" (fallback)
```

**Case 3: Architecture-specific package**

```kotlin
package com.example.iosArm64.device

Available: [commonMain, iosMain, iosArm64Main]
Matches: iosArm64Main (exact match)
// → Returns "iosArm64Main" (no shorter match exists)
```

**Case 4: Multiple platform identifiers**

```kotlin
package com.example.jvm.ios.hybrid  // Unlikely but possible

Available: [commonMain, jvmMain, iosMain]
Matches:
- jvmMain (from "jvm")
- iosMain (from "ios")
// → Returns whichever is shortest (both same length = first alphabetically)
```

---

## Task Execution

### collectFakes() Implementation

```kotlin
@TaskAction
fun collectFakes() {
    val startTime = System.nanoTime()
    val faktLogger = GradleFaktLogger(logger, logLevel.get())
    val faktRootDir = sourceGeneratedDir.asFile.get()

    if (!faktRootDir.exists()) {
        val srcProjectName = sourceProjectPath.orNull?.substringAfterLast(":") ?: "unknown"
        faktLogger.warn(
            "No fakes found in source module '$srcProjectName'. " +
            "Verify that source module has @Fake annotated interfaces, " +
            "or remove this collector module if not needed."
        )
        return
    }

    // Auto-discover all source set directories (commonTest, jvmTest, etc.)
    val sourceSetDirs = faktRootDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

    if (sourceSetDirs.isEmpty()) {
        faktLogger.warn("No generated fakes found in $faktRootDir")
        return
    }

    // Destination base directory (parent of platform-specific dirs)
    val destinationBaseDir = destinationDir.asFile.get().parentFile

    var totalCollected = 0
    val platformStats = mutableMapOf<String, Int>()

    // Process each source set directory with platform detection
    sourceSetDirs.forEach { sourceSetDir ->
        val sourceSetStartTime = System.nanoTime()
        val kotlinDir = sourceSetDir.resolve("kotlin")

        if (!kotlinDir.exists() || !kotlinDir.isDirectory) {
            faktLogger.debug("Skipping ${sourceSetDir.name} (no kotlin directory)")
            return@forEach
        }

        // Use platform detection for this source set
        val sourceSetNames = availableSourceSets.getOrElse(mutableSetOf())
        val result = collectWithPlatformDetection(
            sourceDir = kotlinDir,
            destinationBaseDir = destinationBaseDir,
            availableSourceSets = sourceSetNames,
            faktLogger = faktLogger
        )

        totalCollected += result.collectedCount
        result.platformDistribution.forEach { (platform, count) ->
            platformStats[platform] = (platformStats[platform] ?: 0) + count
        }

        val sourceSetDuration = System.nanoTime() - sourceSetStartTime
        faktLogger.debug(
            "Collected ${result.collectedCount} fake(s) from ${sourceSetDir.name} " +
            "(${TimeFormatter.format(sourceSetDuration)})"
        )
    }

    // Calculate total duration
    val totalDuration = System.nanoTime() - startTime
    val srcProjectName = sourceProjectPath.orNull?.substringAfterLast(":") ?: "unknown"

    // Log summary (INFO level)
    faktLogger.info(
        "✅ $totalCollected fake(s) collected from $srcProjectName | " +
        TimeFormatter.format(totalDuration)
    )

    // Log platform distribution (INFO level)
    platformStats.forEach { (platform, count) ->
        faktLogger.info("  ├─ $platform: $count file(s)")
    }
}

// Helper data class for collection results
private data class CollectionResult(
    val collectedCount: Int,
    val platformDistribution: Map<String, Int>
)

// Helper method for platform-aware collection
private fun collectWithPlatformDetection(
    sourceDir: File,
    destinationBaseDir: File,
    availableSourceSets: Set<String>,
    faktLogger: GradleFaktLogger
): CollectionResult {
    var collected = 0
    val platformCounts = mutableMapOf<String, Int>()

    sourceDir.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .forEach { sourceFile ->
            // Read file and detect platform
            val fileContent = sourceFile.readText()
            val platform = determinePlatformSourceSet(fileContent, availableSourceSets)

            // Calculate destination path
            val relativePath = sourceFile.relativeTo(sourceDir)
            val destFile = destinationBaseDir
                .resolve("$platform/kotlin")  // Platform-specific directory
                .resolve(relativePath)        // Preserve package structure

            // Copy file
            destFile.parentFile.mkdirs()
            sourceFile.copyTo(destFile, overwrite = true)

            collected++
            platformCounts[platform] = (platformCounts[platform] ?: 0) + 1

            faktLogger.trace("Collected ${sourceFile.name} → $platform/")
        }

    return CollectionResult(collected, platformCounts)
}
```

**New Components**:

1. **GradleFaktLogger**: Wrapper around Gradle's logger that respects logLevel
   - Provides `info()`, `debug()`, `trace()`, `warn()` methods
   - Filters output based on configured logLevel

2. **TimeFormatter**: Formats nanosecond durations into readable strings
   - Example: `42_000_000 ns` → `"42ms"`
   - Used for performance tracking

3. **CollectionResult**: Data class for per-source-set statistics
   - Tracks files collected and platform distribution
   - Enables detailed per-source-set timing

### Performance Characteristics

**Time Complexity**:
- File discovery: O(n) where n = number of generated files
- Package extraction: O(1) per file (only reads first 10 lines)
- Platform detection: O(m × k) where m = package segments, k = source sets (typically < 50 operations)
- File copy: O(n) where n = number of files

**Space Complexity**:
- Memory: O(n) for file list
- Disk: 2× (original in producer + copy in collector)

**Typical Performance**:
- 100 fake files: ~40ms (first compilation)
- 100 fake files: ~1ms (cached, no changes)
- 1000 fake files: ~200-400ms (first compilation)

---

## Incremental Compilation Support

### How It Works

Gradle tracks:
- **Inputs**: `sourceGeneratedDir` (producer's generated files)
- **Outputs**: `destinationDir` (collector's collected files)

**Scenarios**:

1. **First build**: Full collection (all files)
2. **No changes**: Task skipped (UP-TO-DATE)
3. **Producer changed**: Rerun collection (only if producer's output changed)
4. **Collector config changed**: Rerun collection

**Example**:

```bash
# First build
./gradlew :core:analytics-fakes:build
> Task :core:analytics-fakes:collectFakesCommonMain (40ms)
✅ Collected 10 fakes

# No changes
./gradlew :core:analytics-fakes:build
> Task :core:analytics-fakes:collectFakesCommonMain UP-TO-DATE (1ms)

# Producer changed (added @Fake interface)
./gradlew :core:analytics-fakes:build
> Task :core:analytics-fakes:collectFakesCommonMain (35ms)
✅ Collected 11 fakes (1 new)
```

---

## Configuration Cache Compatibility

### Design Principles

1. **No Project References at Execution Time**
   - All Project references resolved during configuration
   - Converted to Property<T> or DirectoryProperty

2. **Use Lazy Properties**
   - Property<T>, DirectoryProperty, FileCollection
   - All serializable for configuration cache

3. **Avoid Task Dependencies with Project References**
   - Use `task.flatMap { }` for lazy wiring
   - Register dependencies with TaskProvider<T>

### Verification

```bash
# Test configuration cache
./gradlew :core:analytics-fakes:build --configuration-cache

# Expected output:
# Calculating task graph as no cached configuration is available
# ... build output ...
# Configuration cache entry stored

# Second run (should reuse cache):
./gradlew :core:analytics-fakes:build --configuration-cache

# Expected output:
# Reusing configuration cache
# ... build output ...
```

---

## Testing Strategy

### Unit Testing

Currently **manual testing only** via `samples/kmp-multi-module/`.

**Future**: Add unit tests for:
- `determinePlatformSourceSet()` with various package patterns
- Task registration logic
- Platform matching algorithm edge cases

### Integration Testing

**Current**: `samples/kmp-multi-module/` with 11 producer/collector pairs

**Test Coverage**:
- ✅ Common platform (shared packages)
- ✅ JVM-specific fakes
- ✅ iOS-specific fakes
- ✅ Multiple targets per collector
- ✅ Complex dependency graphs

### Manual Verification

```bash
# Build sample
cd samples/kmp-multi-module
../../gradlew build

# Verify fakes collected (note: no _placeholder in path)
ls core/analytics-fakes/build/generated/collected-fakes/
# Should see: commonMain/, jvmMain/, iosMain/, etc.

# Check platform detection
cat core/analytics-fakes/build/generated/collected-fakes/commonMain/kotlin/com/example/FakeAnalyticsImpl.kt
# Verify correct file placement

# View single collectFakes task (not per-target)
../../gradlew tasks --group fakt
# Should show: collectFakes (not collectFakesJvmMain, etc.)

# Check task execution with timing
../../gradlew :core:analytics-fakes:collectFakes --info
# Should show timing information in output
```

---

## Known Limitations

1. **Package-Based Detection Only**
   - Platform detection relies on package structure
   - No support for custom platform mapping (yet)
   - Workaround: Use consistent package naming conventions

2. **No Transitive Collection**
   - Collectors don't collect from other collectors
   - Each collector must point directly to producer
   - Workaround: Create multiple collectors as needed

3. **All-or-Nothing Collection**
   - Can't exclude specific fakes from collection
   - All generated fakes in producer are collected
   - Workaround: Split producers into multiple modules

4. **No Custom Platform Rules**
   - Can't override platform detection algorithm
   - Can't define custom package → source set mappings
   - Future enhancement: `platformMapping { }` DSL

---

## Single-Platform Support

### registerSingleCollectorTask

**Purpose**: Handles JVM-only, Android, and JS projects (non-KMP)

```kotlin
fun registerSingleCollectorTask(
    project: Project,
    extension: FaktPluginExtension
) {
    // Detects project type and creates appropriate collector task
    // Supports: org.jetbrains.kotlin.jvm, com.android.library, org.jetbrains.kotlin.js
}
```

**Detection Strategy**:
1. Check for JVM plugin → register for `main` source set
2. Check for Android plugin → register for `main` source set
3. Check for JS plugin → register for `main` source set
4. Fallback to `main` if no specific plugin detected

**Task Dependencies**: Uses type-based matching like KMP version
- `KotlinCompile` for JVM
- `Kotlin2JsCompile` for JS
- Both work with Android projects

---

## Future Enhancements

### Planned

1. **Custom Platform Mapping DSL**
   ```kotlin
   fakt {
       platformMapping {
           "custom.jvm.*" to "jvmMain"
           "custom.native.*" to "nativeMain"
       }
   }
   ```

2. **Selective Collection**
   ```kotlin
   fakt {
       collectFakesFrom(projects.core.analytics) {
           include("**/Analytics*")
           exclude("**/Internal*")
       }
   }
   ```

3. **Multi-Producer Collection**
   ```kotlin
   fakt {
       collectFakesFrom(projects.core.analytics, projects.core.logger)
   }
   ```

### Under Consideration

4. **Auto-Wiring Dependencies**
   - Automatically add transitive dependencies from generated code
   - Analyze imports and add coroutines, serialization, etc.

5. **Convention Plugin**
   - Official `fakt-convention` plugin for collector boilerplate
   - Auto-configure targets, dependencies, source sets

6. **Publishing Support**
   - Helper tasks for publishing to Maven Central
   - POM generation with correct dependencies

---

## Debugging Guide

### Enable TRACE Logging

```kotlin
fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.TRACE)
}
```

```bash
./gradlew :core:analytics-fakes:build --info
```

**Output Example**:

```
[TRACE] Collected FakeAnalyticsImpl.kt → commonMain/
[TRACE] Collected fakeAnalytics.kt → commonMain/
[TRACE] Collected FakeAnalyticsConfig.kt → commonMain/
[DEBUG] Collection summary:
  Source: core/analytics/build/generated/fakt/
  Destination: core/analytics-fakes/build/generated/collected-fakes/
  Total files: 3
  commonMain: 3 files
```

### Task Dependency Graph

```bash
# Dry run to see task dependencies
./gradlew :core:analytics-fakes:build --dry-run

# Expected (single task):
# :core:analytics:compileKotlinJvm
# :core:analytics:compileKotlinMetadata
# :core:analytics-fakes:collectFakes  ← Single task for all platforms
# :core:analytics-fakes:compileKotlinJvm
# :core:analytics-fakes:compileKotlinMetadata
```

### Inspect Generated Code

```bash
# View collected fake
cat core/analytics-fakes/build/generated/collected-fakes/commonMain/kotlin/com/example/FakeAnalyticsImpl.kt

# Compare with original
cat core/analytics/build/generated/fakt/commonTest/kotlin/com/example/FakeAnalyticsImpl.kt

# Should be identical except for location
```

---

## Summary

**FakeCollectorTask** is the engine of Fakt's multi-module support:

- **Intelligent**: Platform detection via package analysis with dynamic source set matching
- **Fast**: Incremental compilation, ~1ms for cached builds, performance tracking with TimeFormatter
- **Flexible**: Works with all KMP targets automatically + single-platform support (JVM, Android, JS)
- **Safe**: Configuration cache compatible, proper Gradle task wiring with type-based matching
- **Simple**: Single `collectFakes` task per project (not per-target), auto-discovers source sets
- **Observable**: GradleFaktLogger with configurable verbosity (QUIET/INFO/DEBUG/TRACE)

**Current Implementation** (November 2025):
- Location: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/FakeCollectorTask.kt`
- ~570 lines of code
- Single task architecture (simplified from per-compilation tasks)
- Dynamic platform detection (no hardcoded platform lists)
- Type-based compilation task matching (robust)

**For Users**: See `docs/multi-module/` for usage documentation

**For Contributors**: This doc + source code in `FakeCollectorTask.kt`
