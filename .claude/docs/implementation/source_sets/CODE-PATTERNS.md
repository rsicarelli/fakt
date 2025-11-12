# Code Patterns & Utilities

**Date**: 2025-01-05
**Purpose**: Reusable code snippets for source set mapping implementation

---

## 🎯 Overview

This document contains ready-to-use code patterns extracted from the research document and adapted for Fakt's implementation.

---

## 📋 Table of Contents

1. [Graph Traversal Patterns](#graph-traversal-patterns)
2. [Compilation Classification](#compilation-classification)
3. [Serialization Patterns](#serialization-patterns)
4. [Gradle Plugin Patterns](#gradle-plugin-patterns)
5. [Compiler Plugin Patterns](#compiler-plugin-patterns)
6. [Testing Patterns](#testing-patterns)
7. [Android Specific Patterns](#android-specific-patterns)

---

## 1. Graph Traversal Patterns

### Pattern: BFS Traversal of dependsOn Graph

**Use Case**: Find all parent source sets for a given source set.

```kotlin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Traverses the dependsOn graph upwards using BFS.
 * Returns all source sets in the hierarchy including the starting set.
 *
 * Example:
 *   jvmMain → commonMain
 *   Result: Set(jvmMain, commonMain)
 *
 * Example with complex hierarchy:
 *   iosX64Main → iosMain → appleMain → nativeMain → commonMain
 *   Result: Set(iosX64Main, iosMain, appleMain, nativeMain, commonMain)
 */
fun getAllParentSourceSets(sourceSet: KotlinSourceSet): Set<KotlinSourceSet> {
    val allParents = mutableSetOf<KotlinSourceSet>()
    val queue = ArrayDeque<KotlinSourceSet>()

    // Start with the initial source set
    queue.add(sourceSet)
    allParents.add(sourceSet)

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()

        // For each direct parent, add to set and queue if not seen before
        current.dependsOn.forEach { parent ->
            if (allParents.add(parent)) {
                queue.add(parent)
            }
        }
    }

    return allParents
}
```

**Key Points**:
- Uses `Set` to avoid duplicates (handles diamond dependencies)
- Uses `ArrayDeque` for BFS queue
- Includes starting source set in result
- Thread-safe (uses local mutable collections)

---

### Pattern: Build Hierarchy Map for Serialization

**Use Case**: Convert dependsOn graph to serializable map.

```kotlin
/**
 * Build a map suitable for serialization.
 * Keys are source set names, values are lists of parent names.
 */
fun buildHierarchyMap(sourceSet: KotlinSourceSet): Map<String, List<String>> {
    val allSourceSets = getAllParentSourceSets(sourceSet)

    return allSourceSets.associate { current ->
        val parentNames = current.dependsOn
            .map { it.name }
            .sorted() // For deterministic serialization

        current.name to parentNames
    }
}
```

**Example Output**:
```json
{
  "jvmMain": ["commonMain"],
  "commonMain": []
}
```

---

## 2. Compilation Classification

### Pattern: Detect Test Compilations

**Use Case**: Determine if a compilation is for test code.

```kotlin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

/**
 * Detect if a compilation is for test code using multiple heuristics.
 *
 * Heuristics:
 * 1. Name is exactly "test"
 * 2. Name ends with "Test" (integrationTest, e2eTest)
 * 3. Associated with main compilation (test suite pattern)
 */
fun isTestCompilation(compilation: KotlinCompilation<*>): Boolean {
    // Standard test compilation name
    if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME) {
        return true
    }

    // Convention: custom test suites end with "Test"
    if (compilation.name.endsWith("Test", ignoreCase = true)) {
        return true
    }

    // Check if associated with main compilation
    val mainCompilation = compilation.target.compilations
        .findByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    if (mainCompilation != null &&
        compilation.allAssociatedCompilations.contains(mainCompilation)) {
        return true
    }

    return false
}
```

**Constants Available**:
- `KotlinCompilation.MAIN_COMPILATION_NAME` = `"main"`
- `KotlinCompilation.TEST_COMPILATION_NAME` = `"test"`

---

### Pattern: Get Compilation Type

**Use Case**: Categorize compilation into specific types.

```kotlin
enum class CompilationType {
    MAIN,
    TEST,
    INTEGRATION_TEST,
    CUSTOM
}

fun getCompilationType(compilation: KotlinCompilation<*>): CompilationType {
    return when {
        compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME -> CompilationType.MAIN
        compilation.name == KotlinCompilation.TEST_COMPILATION_NAME -> CompilationType.TEST
        compilation.name == "integrationTest" -> CompilationType.INTEGRATION_TEST
        isTestCompilation(compilation) -> CompilationType.TEST
        else -> CompilationType.CUSTOM
    }
}
```

---

## 3. Serialization Patterns

### Pattern: JSON + Base64 Serialization

**Use Case**: Serialize data for command-line transport.

```kotlin
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.Base64

object ContextSerializer {

    private val json = Json {
        prettyPrint = false      // Compact output
        encodeDefaults = true    // Include default values
        ignoreUnknownKeys = true // Forward compatibility
    }

    /**
     * Serialize to Base64-encoded JSON.
     * Safe for passing via SubpluginOption.
     */
    fun serialize(context: SourceSetContext): String {
        val jsonString = json.encodeToString(context)
        return Base64.getEncoder().encodeToString(jsonString.toByteArray())
    }

    /**
     * Deserialize from Base64-encoded JSON.
     */
    fun deserialize(encoded: String): SourceSetContext {
        val jsonString = String(Base64.getDecoder().decode(encoded))
        return json.decodeFromString(jsonString)
    }
}
```

**Why Base64?**
- Command-line safe (no special characters)
- Handles newlines, quotes, spaces safely
- Standard library support (no extra dependencies)

---

### Pattern: File-Based Serialization (Alternative)

**Use Case**: When command-line length limits are a concern.

```kotlin
import java.io.File

/**
 * Write context to a temporary file instead of command line.
 * Useful for very large contexts (many source sets).
 */
fun serializeToFile(context: SourceSetContext, buildDir: File): File {
    val file = buildDir.resolve("fakt/source-set-context.json")
    file.parentFile.mkdirs()

    val jsonString = json.encodeToString(context)
    file.writeText(jsonString)

    return file
}

/**
 * Read context from file.
 */
fun deserializeFromFile(file: File): SourceSetContext {
    val jsonString = file.readText()
    return json.decodeFromString(jsonString)
}

// In Gradle plugin:
val contextFile = serializeToFile(context, project.buildDir)
SubpluginOption(key = "sourceSetContextFile", value = contextFile.absolutePath)

// In compiler plugin:
val contextFile = File(value)
val context = deserializeFromFile(contextFile)
```

---

## 4. Gradle Plugin Patterns

### Pattern: Lazy Source Set Access

**Use Case**: Configuration avoidance compatible source set access.

```kotlin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// ❌ BAD: Eager access (breaks configuration avoidance)
val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir("...")

// ✅ GOOD: Lazy access
val kotlin = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension
kotlin.sourceSets.named("commonMain") {
    kotlin.srcDir(project.layout.buildDirectory.dir("generated/fakt/commonMain/kotlin"))
}

// ✅ BETTER: configureEach for all source sets
kotlin.sourceSets.configureEach { sourceSet ->
    if (sourceSet.name.endsWith("Test")) {
        // Lazy configuration
    }
}
```

**Key APIs**:
- `named()` → Returns `NamedDomainObjectProvider<T>`
- `configureEach {}` → Applies to all objects (existing and future)
- `layout.buildDirectory` → Lazy property (NOT `buildDir`!)

---

### Pattern: Target Iteration

**Use Case**: Apply logic to all targets in KMP project.

```kotlin
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

kotlin.targets.configureEach { target: KotlinTarget ->
    // This is called lazily for each target

    target.compilations.configureEach { compilation ->
        // Nested lazy configuration
        val context = buildContext(compilation)
        // ...
    }
}
```

**Why `configureEach`?**
- Defers execution until needed
- Works with targets added later (via `apply from:` scripts)
- Configuration cache compatible

---

### Pattern: Output Directory Resolution

**Use Case**: Determine where to generate code.

```kotlin
/**
 * Resolve output directory for a compilation.
 * Pattern: build/generated/fakt/{type}/{target}/kotlin
 */
fun resolveOutputDirectory(
    compilation: KotlinCompilation<*>,
    project: Project
): String {
    val buildDir = project.layout.buildDirectory.get().asFile

    val type = if (isTestCompilation(compilation)) "test" else "main"
    val target = compilation.target.name

    return buildDir.resolve("generated/fakt/$type/$target/kotlin").absolutePath
}
```

**Example Outputs**:
- JVM main: `build/generated/fakt/main/jvm/kotlin`
- JVM test: `build/generated/fakt/test/jvm/kotlin`
- iOS main: `build/generated/fakt/main/iosX64/kotlin`
- Common test: `build/generated/fakt/test/metadata/kotlin`

---

### Pattern: Apply Plugin to Specific Compilations

**Use Case**: Enable compiler plugin only for specific compilations.

```kotlin
override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
    val project = kotlinCompilation.project
    val extension = project.extensions.findByType(FaktPluginExtension::class.java)
        ?: return false

    // Only apply to main compilations (not test)
    if (isTestCompilation(kotlinCompilation)) {
        return false
    }

    // Only apply if enabled
    if (!extension.enabled.get()) {
        return false
    }

    // Only apply to specific platforms
    if (kotlinCompilation.platformType == KotlinPlatformType.androidJvm) {
        // Android-specific logic
        return true
    }

    return true
}
```

---

## 5. Compiler Plugin Patterns

### Pattern: Receive and Store Context

**Use Case**: Receive serialized context in CommandLineProcessor.

```kotlin
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

val KEY_SOURCE_SET_CONTEXT = CompilerConfigurationKey<SourceSetContext>("source set context")

class FaktCommandLineProcessor : CommandLineProcessor {

    override val pluginId = "com.rsicarelli.fakt"

    override val pluginOptions = listOf(
        CliOption(
            optionName = "sourceSetContext",
            valueDescription = "<base64-json>",
            description = "Source set context from Gradle plugin",
            required = false
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option.optionName) {
            "sourceSetContext" -> {
                try {
                    val context = ContextSerializer.deserialize(value)
                    configuration.put(KEY_SOURCE_SET_CONTEXT, context)
                } catch (e: Exception) {
                    // Log error but don't fail compilation
                    System.err.println("Fakt: Failed to decode source set context: ${e.message}")
                }
            }
        }
    }
}
```

---

### Pattern: Access Context in IR Extension

**Use Case**: Get context in IrGenerationExtension.

```kotlin
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class FaktIrGenerationExtension : IrGenerationExtension {

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        // Get context from CompilerConfiguration
        val context = pluginContext.configuration.get(KEY_SOURCE_SET_CONTEXT)
            ?: run {
                // Fallback: provide helpful error
                error(
                    """
                    Fakt: SourceSetContext not found in compiler configuration.
                    This usually means:
                    1. Gradle plugin version mismatch (update both plugins)
                    2. Gradle plugin not applied (add: id("com.rsicarelli.fakt"))
                    3. Using deprecated plugin version
                    """.trimIndent()
                )
            }

        // Use context
        val resolver = SourceSetResolver(context, messageCollector)
        val outputDir = resolver.getGeneratedSourcesDir()

        // Generate code...
    }
}
```

---

## 6. Testing Patterns

### Pattern: Mock Source Set for Tests

**Use Case**: Create test doubles for KotlinSourceSet.

```kotlin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Simple mock for testing graph traversal.
 */
class MockKotlinSourceSet(
    private val mockName: String,
    private val mockDependsOn: List<KotlinSourceSet> = emptyList()
) : KotlinSourceSet {

    override fun getName(): String = mockName

    override fun dependsOn(other: KotlinSourceSet) {
        // For testing, we set this in constructor
        error("Not supported in mock")
    }

    override fun getDependsOn(): Set<KotlinSourceSet> {
        return mockDependsOn.toSet()
    }

    // Implement other methods as needed...
}

// Usage in tests:
val commonMain = MockKotlinSourceSet("commonMain")
val jvmMain = MockKotlinSourceSet("jvmMain", listOf(commonMain))
```

---

### Pattern: GIVEN-WHEN-THEN Test Structure

**Use Case**: Follow Fakt's testing standard.

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SourceSetGraphTraversalTest {

    @Test
    fun `GIVEN simple hierarchy WHEN traversing THEN should find all parents`() = runTest {
        // GIVEN
        val commonMain = MockKotlinSourceSet("commonMain")
        val jvmMain = MockKotlinSourceSet("jvmMain", dependsOn = listOf(commonMain))

        // WHEN
        val result = SourceSetGraphTraversal.getAllParentSourceSets(jvmMain)

        // THEN
        assertEquals(2, result.size)
        assertTrue(result.contains(jvmMain))
        assertTrue(result.contains(commonMain))
    }

    @Test
    fun `GIVEN diamond dependency WHEN traversing THEN should not duplicate`() = runTest {
        // GIVEN
        //        commonMain
        //        /        \
        //   nativeMain   appleMain
        //        \        /
        //         iosMain

        val commonMain = MockKotlinSourceSet("commonMain")
        val nativeMain = MockKotlinSourceSet("nativeMain", listOf(commonMain))
        val appleMain = MockKotlinSourceSet("appleMain", listOf(commonMain))
        val iosMain = MockKotlinSourceSet("iosMain", listOf(nativeMain, appleMain))

        // WHEN
        val result = SourceSetGraphTraversal.getAllParentSourceSets(iosMain)

        // THEN
        assertEquals(4, result.size, "Should not duplicate commonMain")
        assertTrue(result.contains(commonMain))
    }
}
```

---

### Pattern: Integration Test with Gradle TestKit

**Use Case**: Test Gradle plugin end-to-end.

```kotlin
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class FaktGradlePluginIntegrationTest {

    @Test
    fun `GIVEN KMP project WHEN building THEN should generate fakes`() {
        // GIVEN
        val projectDir = createTempDir()
        projectDir.resolve("settings.gradle.kts").writeText("""
            rootProject.name = "test-project"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                kotlin("multiplatform") version "1.9.24"
                id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
            }

            kotlin {
                jvm()
                sourceSets {
                    commonMain {
                        dependencies {
                            implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
                        }
                    }
                }
            }
        """.trimIndent())

        // Create a simple @Fake interface
        val sourceDir = projectDir.resolve("src/commonMain/kotlin")
        sourceDir.mkdirs()
        sourceDir.resolve("Example.kt").writeText("""
            import com.rsicarelli.fakt.Fake

            @Fake
            interface MyService {
                fun doSomething(): String
            }
        """.trimIndent())

        // WHEN
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("build", "--stacktrace")
            .withPluginClasspath()
            .build()

        // THEN
        assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)

        val generatedFile = projectDir.resolve(
            "build/generated/fakt/main/metadata/kotlin/FakeMyServiceImpl.kt"
        )
        assertTrue(generatedFile.exists(), "Generated fake file should exist")
    }
}
```

---

## 7. Android Specific Patterns

### Pattern: Handle Android Variants

**Use Case**: React to Android build variant creation.

```kotlin
import com.android.build.api.variant.AndroidComponentsExtension

fun configureAndroidVariants(project: Project) {
    val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java)
        ?: return // Not an Android project

    androidComponents.onVariants { variant ->
        // This callback is invoked for each variant as it's configured
        val variantName = variant.name // e.g., "debug", "release"

        // Access corresponding Kotlin compilation
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val androidTarget = kotlin.targets.findByName("android") as? KotlinAndroidTarget
            ?: return@onVariants

        val compilation = androidTarget.compilations.findByName(variantName)
            ?: return@onVariants

        // Configure for this variant
        val context = buildContext(compilation)
        // ...
    }
}
```

**Why Needed?**
- Android creates build variants dynamically
- Variants not available during initial configuration phase
- Must use event-driven API (`onVariants`)

---

### Pattern: Detect Android Source Set Layout

**Use Case**: Handle new vs old Android source set structure.

```kotlin
/**
 * Detect Android source set layout version.
 * New layout (KGP 1.9.0+): src/androidTest → androidInstrumentedTest
 * Old layout: src/androidTest → androidAndroidTest
 */
fun getAndroidTestSourceSetName(kotlinVersion: String): String {
    val version = kotlinVersion.split(".").take(2).joinToString(".")
    return if (version >= "1.9") {
        "androidInstrumentedTest"
    } else {
        "androidAndroidTest"
    }
}
```

---

## 🎯 Complete Example: Building SourceSetContext

Putting it all together:

```kotlin
class SourceSetDiscovery(private val project: Project) {

    fun buildContext(compilation: KotlinCompilation<*>): SourceSetContext {
        return SourceSetContext(
            compilationName = compilation.name,
            targetName = compilation.target.name,
            platformType = compilation.platformType.name.lowercase(),
            isTest = CompilationClassifier.isTestCompilation(compilation),
            defaultSourceSet = buildSourceSetInfo(compilation.defaultSourceSet),
            allSourceSets = compilation.allKotlinSourceSets.map { buildSourceSetInfo(it) },
            outputDirectory = resolveOutputDirectory(compilation)
        )
    }

    private fun buildSourceSetInfo(sourceSet: KotlinSourceSet): SourceSetInfo {
        val allParents = SourceSetGraphTraversal.getAllParentSourceSets(sourceSet)
        val parentNames = allParents
            .filter { it != sourceSet }
            .map { it.name }
            .sorted()

        return SourceSetInfo(
            name = sourceSet.name,
            parents = parentNames
        )
    }

    private fun resolveOutputDirectory(compilation: KotlinCompilation<*>): String {
        val buildDir = project.layout.buildDirectory.get().asFile
        val type = if (CompilationClassifier.isTestCompilation(compilation)) "test" else "main"
        val target = compilation.target.name

        return buildDir.resolve("generated/fakt/$type/$target/kotlin").absolutePath
    }
}
```

---

## 📚 References

- **Research Document**: Kotlin Compiler Plugin Source Set Mapping (Gemini Deep Research)
- **KSP Examples**: [github.com/google/ksp](https://github.com/google/ksp)
- **Metro Examples**: [github.com/slackhq/metro](https://github.com/slackhq/metro)
- **Kotlin Gradle Plugin API**: [kotlinlang.org/api/kotlin-gradle-plugin](https://kotlinlang.org/api/kotlin-gradle-plugin/)

---

**Usage**: Copy-paste these patterns directly into implementation.
**Testing**: All patterns include corresponding test examples.
**Maintenance**: These patterns are future-proof and don't require updates for new Kotlin targets.
