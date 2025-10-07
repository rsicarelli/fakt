# Modern Source Set Mapping Architecture

**Date**: 2025-01-05
**Status**: Approved for Implementation
**Research Source**: Gemini Deep Research - Kotlin Compiler Plugin Source Set Mapping

---

## 🎯 Executive Summary

This document describes the modern, convention-based architecture for source set mapping in Fakt, replacing 600+ lines of hardcoded pattern matching with programmatic discovery using Kotlin Gradle Plugin APIs.

**Key Principles:**
1. **Lazy Configuration** - Use `Provider`, `configureEach`, `named` exclusively
2. **Convention Over Configuration** - Discover actual project structure programmatically
3. **Serialization Bridge** - Pass rich context from Gradle → Compiler via JSON
4. **KSP-Inspired Patterns** - Follow proven patterns from mature compiler plugins

---

## 🏗️ Current Architecture (Problems)

### Gradle Plugin (`SourceSetConfigurator.kt` - 191 lines)

```kotlin
// ❌ Hardcoded source set names
when (sourceSet.name) {
    "commonTest" -> { /* ... */ }
    "jvmTest" -> { /* ... */ }
    "iosArm64Test" -> { /* ... */ }
    // ... many more cases
}

// ❌ Pattern matching for unknown source sets
if (sourceSet.name.endsWith("Test") && sourceSet.name != "commonTest") {
    val targetName = sourceSet.name.removeSuffix("Test")
    // Guess the directory
}
```

### Compiler Plugin (`SourceSetMapper.kt` - 411 lines)

```kotlin
// ❌ String pattern matching on module names
private fun mapToTestSourceSet(moduleName: String): String {
    val normalizedName = moduleName.lowercase()
    return when {
        normalizedName.contains("commonmain") -> "commonTest"
        normalizedName.contains("jvmmain") -> "jvmTest"
        normalizedName.contains("iosarm64main") -> "iosArm64Test"
        // ... 50+ more hardcoded patterns
    }
}

// ❌ Fallback chains for guessing
private fun buildFallbackChain(moduleName: String): List<String> =
    when {
        moduleName.contains("ios") -> listOf("appleTest", "nativeTest", "commonTest")
        // ... more guessing
    }
```

### Problems

1. **Maintenance Burden** - Every new Kotlin target requires code updates
2. **Custom Source Sets Fail** - Users with `integrationTest` or custom names break
3. **Brittle Pattern Matching** - String contains/endsWith is fragile
4. **No Trust in User Config** - We guess instead of querying actual structure
5. **Duplication** - Logic duplicated across Gradle + Compiler plugins
6. **No Android Variant Support** - Can't handle `debugTest`, `releaseTest`, etc.

---

## ✨ New Architecture (Solution)

### Two-Phase Model

```
┌─────────────────────────────────────────────────────────────┐
│  PHASE 1: Gradle Plugin (Configuration Time)               │
│  ═══════════════════════════════════════════════════        │
│                                                             │
│  1. Discover ALL targets programmatically                  │
│  2. Iterate ALL compilations per target                    │
│  3. Build source set hierarchy map for each compilation    │
│  4. Serialize to JSON + Base64 encode                      │
│  5. Pass via SubpluginOption to compiler                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              ↓
                    (Serialized JSON Bridge)
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  PHASE 2: Compiler Plugin (Compilation Time)               │
│  ═══════════════════════════════════════════════════        │
│                                                             │
│  1. Receive SubpluginOption with Base64 JSON               │
│  2. Deserialize to SourceSetContext data model             │
│  3. Use context directly (NO pattern matching)             │
│  4. Generate code to correct output directory              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔑 Key Components

### 1. Shared Data Model (New Module: `compiler-api`)

```kotlin
// Serializable data classes shared between Gradle + Compiler plugins
package com.rsicarelli.fakt.compiler.api

import kotlinx.serialization.Serializable

/**
 * Complete source set context for a single compilation.
 * Passed from Gradle plugin → Compiler plugin via SubpluginOption.
 */
@Serializable
data class SourceSetContext(
    val compilationName: String,           // e.g., "main", "test", "integrationTest"
    val targetName: String,                // e.g., "jvm", "iosX64", "metadata"
    val platformType: String,              // e.g., "jvm", "native", "js", "common"
    val isTest: Boolean,                   // true for test compilations
    val defaultSourceSet: SourceSetInfo,   // Primary source set (e.g., jvmMain)
    val allSourceSets: List<SourceSetInfo>, // Full dependsOn hierarchy
    val outputDirectory: String            // Where to generate code
)

@Serializable
data class SourceSetInfo(
    val name: String,                      // e.g., "jvmMain", "commonMain"
    val parents: List<String>              // Direct dependsOn parents
)
```

### 2. Gradle Plugin - Discovery Engine

**File**: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/SourceSetDiscovery.kt`

```kotlin
package com.rsicarelli.fakt.gradle

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo

/**
 * Discovers source set structure using modern lazy Gradle APIs.
 * Follows research recommendations from Gemini Deep Research report.
 */
internal class SourceSetDiscovery(
    private val project: Project
) {

    /**
     * Build complete source set context for a compilation.
     * Called from applyToCompilation() for each compilation.
     */
    fun buildContext(compilation: KotlinCompilation<*>): SourceSetContext {
        return SourceSetContext(
            compilationName = compilation.name,
            targetName = compilation.target.name,
            platformType = compilation.platformType.name.lowercase(),
            isTest = isTestCompilation(compilation),
            defaultSourceSet = buildSourceSetInfo(compilation.defaultSourceSet),
            allSourceSets = compilation.allKotlinSourceSets.map { buildSourceSetInfo(it) },
            outputDirectory = resolveOutputDirectory(compilation)
        )
    }

    /**
     * Build source set info including parent hierarchy.
     * Uses BFS traversal to find all transitive parents.
     */
    private fun buildSourceSetInfo(sourceSet: KotlinSourceSet): SourceSetInfo {
        val parents = getAllParentSourceSets(sourceSet)
            .filter { it != sourceSet } // Exclude self
            .map { it.name }
            .sorted()

        return SourceSetInfo(
            name = sourceSet.name,
            parents = parents
        )
    }

    /**
     * Traverse dependsOn graph upwards using BFS.
     * Implementation from research document Section 2.4.
     */
    private fun getAllParentSourceSets(sourceSet: KotlinSourceSet): Set<KotlinSourceSet> {
        val allParents = mutableSetOf<KotlinSourceSet>()
        val queue = ArrayDeque<KotlinSourceSet>()

        queue.add(sourceSet)
        allParents.add(sourceSet)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            current.dependsOn.forEach { parent ->
                if (allParents.add(parent)) {
                    queue.add(parent)
                }
            }
        }

        return allParents
    }

    /**
     * Detect test compilations using heuristics.
     * Implementation from research document Section 3.3.
     */
    private fun isTestCompilation(compilation: KotlinCompilation<*>): Boolean {
        // Standard test compilation name
        if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME) {
            return true
        }

        // Convention for custom test suites (e.g., "integrationTest")
        if (compilation.name.endsWith("Test", ignoreCase = true)) {
            return true
        }

        // Check if associated with main compilation (testing pattern)
        val mainCompilation = compilation.target.compilations
            .findByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        if (mainCompilation != null &&
            compilation.allAssociatedCompilations.contains(mainCompilation)) {
            return true
        }

        return false
    }

    /**
     * Resolve output directory for generated code.
     * Pattern: build/generated/fakt/{compilation.name}/kotlin
     */
    private fun resolveOutputDirectory(compilation: KotlinCompilation<*>): String {
        val buildDir = project.layout.buildDirectory.get().asFile
        val compilationDir = if (isTestCompilation(compilation)) {
            "test/${compilation.target.name}"
        } else {
            "main/${compilation.target.name}"
        }
        return buildDir.resolve("generated/fakt/$compilationDir/kotlin").absolutePath
    }
}
```

### 3. Gradle Plugin - Serialization Bridge

**File**: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/ContextSerializer.kt`

```kotlin
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.Base64

/**
 * Serializes source set context for transport to compiler plugin.
 * Uses JSON + Base64 encoding for command-line safety.
 */
internal object ContextSerializer {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    /**
     * Serialize context to Base64-encoded JSON string.
     * Safe for passing via SubpluginOption.
     */
    fun serialize(context: SourceSetContext): String {
        val jsonString = json.encodeToString(context)
        return Base64.getEncoder().encodeToString(jsonString.toByteArray())
    }

    /**
     * Deserialize from Base64-encoded JSON string.
     * Used by compiler plugin's CommandLineProcessor.
     */
    fun deserialize(encoded: String): SourceSetContext {
        val jsonString = String(Base64.getDecoder().decode(encoded))
        return json.decodeFromString(jsonString)
    }
}
```

### 4. Updated Gradle Subplugin

**File**: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/FaktGradleSubplugin.kt`

```kotlin
override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>
): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.project
    val extension = project.extensions.getByType(FaktPluginExtension::class.java)

    // Use lazy Provider (configuration avoidance)
    return project.provider {
        val discovery = SourceSetDiscovery(project)
        val context = discovery.buildContext(kotlinCompilation)
        val serialized = ContextSerializer.serialize(context)

        listOf(
            SubpluginOption(key = "enabled", value = extension.enabled.get().toString()),
            SubpluginOption(key = "debug", value = extension.debug.get().toString()),
            SubpluginOption(key = "sourceSetContext", value = serialized) // NEW!
        )
    }
}
```

### 5. Compiler Plugin - Context Receiver

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/FaktCommandLineProcessor.kt`

```kotlin
package com.rsicarelli.fakt.compiler

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.gradle.ContextSerializer
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

val KEY_SOURCE_SET_CONTEXT = CompilerConfigurationKey<SourceSetContext>("source set context")

@AutoService(CommandLineProcessor::class)
class FaktCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = "com.rsicarelli.fakt"

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption("enabled", "<true|false>", "Enable Fakt", required = false),
        CliOption("debug", "<true|false>", "Debug mode", required = false),
        CliOption("sourceSetContext", "<base64-json>", "Source set context", required = false)
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option.optionName) {
            "sourceSetContext" -> {
                // Deserialize context from Gradle plugin
                val context = ContextSerializer.deserialize(value)
                configuration.put(KEY_SOURCE_SET_CONTEXT, context)
            }
            // ... other options
        }
    }
}
```

### 6. Compiler Plugin - Simplified Mapper

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/output/SourceSetResolver.kt`

```kotlin
package com.rsicarelli.fakt.compiler.output

import com.rsicarelli.fakt.compiler.KEY_SOURCE_SET_CONTEXT
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.io.File

/**
 * Resolves source set information from Gradle-provided context.
 * REPLACES the 411-line SourceSetMapper.kt with ~50 lines.
 */
internal class SourceSetResolver(
    private val context: SourceSetContext,
    private val messageCollector: MessageCollector?
) {

    /**
     * Get output directory for generated code.
     * No fallback logic needed - Gradle already resolved everything!
     */
    fun getGeneratedSourcesDir(): File {
        val dir = File(context.outputDirectory)

        if (!dir.exists()) {
            dir.mkdirs()
        }

        messageCollector?.reportInfo(
            "Fakt: Generating to ${context.compilationName} " +
            "(${context.targetName}/${context.platformType}): ${dir.absolutePath}"
        )

        return dir
    }

    /**
     * Check if current compilation is a test context.
     */
    fun isTestContext(): Boolean = context.isTest

    /**
     * Get all source sets in the hierarchy.
     */
    fun getAllSourceSets(): List<String> =
        context.allSourceSets.map { it.name }

    /**
     * Get default source set name.
     */
    fun getDefaultSourceSet(): String = context.defaultSourceSet.name

    private fun MessageCollector.reportInfo(message: String) {
        this.report(
            org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO,
            message
        )
    }
}
```

---

## 📊 Architecture Comparison

### Before: Hardcoded Pattern Matching

| Component | Lines of Code | Approach | Maintainability |
|-----------|---------------|----------|-----------------|
| SourceSetMapper.kt | 411 | String pattern matching | ❌ High maintenance |
| SourceSetConfigurator.kt | 191 | Hardcoded names | ❌ Brittle |
| **Total** | **602** | **Imperative guessing** | **❌ Poor** |

### After: Convention-Based Discovery

| Component | Lines of Code | Approach | Maintainability |
|-----------|---------------|----------|------------------|
| SourceSetContext.kt | 25 | Data model | ✅ Zero maintenance |
| SourceSetDiscovery.kt | 120 | Programmatic discovery | ✅ Self-updating |
| ContextSerializer.kt | 25 | JSON serialization | ✅ Zero maintenance |
| SourceSetResolver.kt | 50 | Direct usage | ✅ Zero maintenance |
| **Total** | **220** | **Declarative discovery** | **✅ Excellent** |

**Reduction**: 602 → 220 lines (-63%)
**Maintenance**: High → Zero (no hardcoded patterns to update)

---

## 🎯 Benefits

### 1. Future-Proof
- ✅ Automatically supports new Kotlin targets (wasm-wasi, etc.)
- ✅ Works with custom source sets (`integrationTest`, `e2eTest`, etc.)
- ✅ Compatible with custom hierarchies (`applyHierarchyTemplate { ... }`)

### 2. Android Support
- ✅ Handles build variants (`debug`, `release`, `staging`)
- ✅ Works with product flavors (`free`, `paid`)
- ✅ Supports `androidInstrumentedTest` and `androidUnitTest`

### 3. Performance
- ✅ Lazy configuration (no eager evaluation)
- ✅ Configuration cache compatible
- ✅ Minimal build overhead

### 4. Developer Experience
- ✅ No guessing - uses actual project structure
- ✅ Clear error messages when context missing
- ✅ Works out-of-the-box with default hierarchy

### 5. Maintenance
- ✅ No code changes needed for new Kotlin versions
- ✅ No hardcoded target/source set names
- ✅ Single source of truth (Gradle plugin discovery)

---

## 🔍 Edge Cases Handled

### 1. Custom Source Sets
```kotlin
// User creates custom test suite
kotlin {
    sourceSets {
        val integrationTest by creating {
            dependsOn(commonMain)
        }
    }

    targets.all {
        compilations.create("integrationTest") {
            associateWith(compilations.getByName("main"))
        }
    }
}
```

**Before**: ❌ Breaks - not in hardcoded list
**After**: ✅ Works - discovered programmatically via `target.compilations`

### 2. Android Variants
```kotlin
android {
    buildTypes {
        debug { }
        release { }
        staging { }
    }
}
```

**Before**: ❌ Only supports `androidUnitTest`
**After**: ✅ Works for `debugTest`, `releaseTest`, `stagingTest`

### 3. Custom Hierarchies
```kotlin
kotlin {
    applyHierarchyTemplate {
        common {
            group("jvmAndMacos") {
                withJvm()
                withMacos()
            }
        }
    }
}
```

**Before**: ❌ Not in fallback chain
**After**: ✅ Discovered via `dependsOn` traversal

---

## 🚀 Migration Path

See [MIGRATION-GUIDE.md](./MIGRATION-GUIDE.md) for detailed migration steps.

**Summary:**
1. Phase 1: Add shared `compiler-api` module with data models
2. Phase 2: Implement `SourceSetDiscovery` in Gradle plugin
3. Phase 3: Update `applyToCompilation` to pass serialized context
4. Phase 4: Replace `SourceSetMapper` with `SourceSetResolver`
5. Phase 5: Test with all sample projects
6. Phase 6: Delete old hardcoded implementations

---

## 📚 References

- **Research Document**: Gemini Deep Research - Kotlin Compiler Plugin Source Set Mapping
- **KSP Pattern**: [kotlinlang.org/docs/ksp-multiplatform.html](https://kotlinlang.org/docs/ksp-multiplatform.html)
- **Default Hierarchy**: [kotlinlang.org/docs/whatsnew1920.html](https://kotlinlang.org/docs/whatsnew1920.html)
- **Gradle Lazy APIs**: [docs.gradle.org/current/userguide/kotlin_dsl.html](https://docs.gradle.org/current/userguide/kotlin_dsl.html)

---

**Status**: ✅ Ready for Implementation
**Estimated Effort**: 3 weeks (following Implementation Roadmap)
**Risk**: Low (proven patterns from KSP, Metro, kotlinx.serialization)
