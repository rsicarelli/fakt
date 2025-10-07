# Migration Guide: Hardcoded Patterns → Convention-Based Discovery

**Date**: 2025-01-05
**Audience**: Fakt Contributors
**Complexity**: Medium

---

## 🎯 Overview

This guide explains how to migrate from the old hardcoded source set mapping approach to the new convention-based discovery system.

**What's Changing**:
- ❌ Delete: `SourceSetMapper.kt` (411 lines)
- ❌ Delete: `SourceSetConfigurator.kt` (191 lines)
- ✅ Add: `SourceSetDiscovery.kt` (~120 lines)
- ✅ Add: `SourceSetResolver.kt` (~50 lines)
- ✅ Add: `compiler-api` module (data models)

---

## 📋 Prerequisites

Before starting migration:

1. **Backup current working state**
   ```bash
   git checkout -b feature/modern-source-set-mapping
   git commit -m "chore: backup before source set refactoring"
   ```

2. **Ensure all tests pass**
   ```bash
   make test
   ```

3. **Document current behavior**
   ```bash
   # Run with current implementation and save logs
   cd samples/single-module
   ../../gradlew clean build --info > /tmp/before-migration.log 2>&1
   ```

---

## 🔄 Migration Steps

### Step 1: Create compiler-api Module

**Objective**: Shared data models for Gradle ↔ Compiler communication.

#### 1.1 Create Module Structure

```bash
mkdir -p compiler-api/src/main/kotlin/com/rsicarelli/fakt/compiler/api
mkdir -p compiler-api/src/test/kotlin/com/rsicarelli/fakt/compiler/api
```

#### 1.2 Create build.gradle.kts

**File**: `compiler-api/build.gradle.kts`

```kotlin
plugins {
    id("com.rsicarelli.fakt.kotlin-jvm")
    kotlin("plugin.serialization") version "1.9.24"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

#### 1.3 Add to settings.gradle.kts

```kotlin
// In ktfake/settings.gradle.kts
include(":compiler-api")
```

#### 1.4 Create Data Models

**File**: `compiler-api/src/main/kotlin/com/rsicarelli/fakt/compiler/api/SourceSetContext.kt`

```kotlin
package com.rsicarelli.fakt.compiler.api

import kotlinx.serialization.Serializable

/**
 * Complete source set context for a single compilation.
 * Replaces all hardcoded pattern matching in compiler plugin.
 */
@Serializable
data class SourceSetContext(
    /** Compilation name (e.g., "main", "test", "integrationTest") */
    val compilationName: String,

    /** Target name (e.g., "jvm", "iosX64", "metadata") */
    val targetName: String,

    /** Platform type (e.g., "jvm", "native", "js", "common") */
    val platformType: String,

    /** Whether this is a test compilation */
    val isTest: Boolean,

    /** Primary source set for this compilation */
    val defaultSourceSet: SourceSetInfo,

    /** All source sets in dependsOn hierarchy */
    val allSourceSets: List<SourceSetInfo>,

    /** Absolute path to output directory */
    val outputDirectory: String
)

@Serializable
data class SourceSetInfo(
    /** Source set name (e.g., "jvmMain", "commonMain") */
    val name: String,

    /** Direct parent source sets from dependsOn */
    val parents: List<String>
)
```

#### 1.5 Update Dependencies

**File**: `gradle-plugin/build.gradle.kts`

```kotlin
dependencies {
    implementation(project(":compiler-api"))  // ← ADD THIS
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")  // ← ADD THIS
    // ... existing dependencies
}
```

**File**: `compiler/build.gradle.kts`

```kotlin
dependencies {
    implementation(project(":compiler-api"))  // ← ADD THIS
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")  // ← ADD THIS
    // ... existing dependencies
}
```

#### 1.6 Test Data Models

**File**: `compiler-api/src/test/kotlin/api/SourceSetContextTest.kt`

```kotlin
package com.rsicarelli.fakt.compiler.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceSetContextTest {

    @Test
    fun `GIVEN SourceSetContext WHEN serializing THEN should roundtrip`() {
        // Given
        val original = SourceSetContext(
            compilationName = "main",
            targetName = "jvm",
            platformType = "jvm",
            isTest = false,
            defaultSourceSet = SourceSetInfo("jvmMain", listOf("commonMain")),
            allSourceSets = listOf(
                SourceSetInfo("jvmMain", listOf("commonMain")),
                SourceSetInfo("commonMain", emptyList())
            ),
            outputDirectory = "/build/generated/fakt/main/jvm/kotlin"
        )

        // When
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<SourceSetContext>(json)

        // Then
        assertEquals(original, decoded)
    }
}
```

```bash
./gradlew :compiler-api:test
```

**✅ Success Criteria**:
- Module builds successfully
- Serialization test passes
- Can import from gradle-plugin and compiler modules

---

### Step 2: Implement Graph Traversal Utilities

**Objective**: Replace hardcoded fallback chains with BFS traversal.

#### 2.1 Create Utility File

**File**: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/SourceSetGraphTraversal.kt`

```kotlin
package com.rsicarelli.fakt.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Utilities for traversing the KotlinSourceSet dependsOn graph.
 * Replaces hardcoded fallback chains in SourceSetMapper.kt.
 */
internal object SourceSetGraphTraversal {

    /**
     * Traverse dependsOn graph upwards using BFS to find all parent source sets.
     * Includes the starting source set in the result.
     *
     * Example:
     *   jvmMain depends on commonMain
     *   → returns Set(jvmMain, commonMain)
     *
     * Example with hierarchy:
     *   iosX64Main → iosMain → appleMain → nativeMain → commonMain
     *   → returns Set(iosX64Main, iosMain, appleMain, nativeMain, commonMain)
     */
    fun getAllParentSourceSets(sourceSet: KotlinSourceSet): Set<KotlinSourceSet> {
        val allParents = mutableSetOf<KotlinSourceSet>()
        val queue = ArrayDeque<KotlinSourceSet>()

        // Start with the initial source set
        queue.add(sourceSet)
        allParents.add(sourceSet)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            // For each direct parent, add it to the set and queue if not seen before
            current.dependsOn.forEach { parent ->
                if (allParents.add(parent)) {
                    queue.add(parent)
                }
            }
        }

        return allParents
    }

    /**
     * Build a map of source set name → parent names for serialization.
     */
    fun buildHierarchyMap(sourceSet: KotlinSourceSet): Map<String, List<String>> {
        val allSourceSets = getAllParentSourceSets(sourceSet)

        return allSourceSets.associate { current ->
            current.name to current.dependsOn.map { it.name }.sorted()
        }
    }
}
```

#### 2.2 Add Tests

**File**: `gradle-plugin/src/test/kotlin/gradle/SourceSetGraphTraversalTest.kt`

```kotlin
package com.rsicarelli.fakt.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceSetGraphTraversalTest {

    @Test
    fun `GIVEN simple hierarchy WHEN traversing THEN should find all parents`() {
        // Given
        val commonMain = mockSourceSet("commonMain", dependsOn = emptyList())
        val jvmMain = mockSourceSet("jvmMain", dependsOn = listOf(commonMain))

        // When
        val parents = SourceSetGraphTraversal.getAllParentSourceSets(jvmMain)

        // Then
        assertEquals(2, parents.size)
        assertTrue(parents.contains(jvmMain))
        assertTrue(parents.contains(commonMain))
    }

    @Test
    fun `GIVEN diamond dependency WHEN traversing THEN should handle correctly`() {
        // Given: Diamond pattern
        //        commonMain
        //        /        \
        //   nativeMain   appleMain
        //        \        /
        //         iosMain
        val commonMain = mockSourceSet("commonMain")
        val nativeMain = mockSourceSet("nativeMain", dependsOn = listOf(commonMain))
        val appleMain = mockSourceSet("appleMain", dependsOn = listOf(commonMain))
        val iosMain = mockSourceSet("iosMain", dependsOn = listOf(nativeMain, appleMain))

        // When
        val parents = SourceSetGraphTraversal.getAllParentSourceSets(iosMain)

        // Then
        assertEquals(4, parents.size)
        assertTrue(parents.contains(iosMain))
        assertTrue(parents.contains(nativeMain))
        assertTrue(parents.contains(appleMain))
        assertTrue(parents.contains(commonMain))
    }

    @Test
    fun `GIVEN source set WHEN building hierarchy map THEN should create correct structure`() {
        // Given
        val commonMain = mockSourceSet("commonMain")
        val jvmMain = mockSourceSet("jvmMain", dependsOn = listOf(commonMain))

        // When
        val map = SourceSetGraphTraversal.buildHierarchyMap(jvmMain)

        // Then
        assertEquals(listOf("commonMain"), map["jvmMain"])
        assertEquals(emptyList(), map["commonMain"])
    }

    private fun mockSourceSet(
        name: String,
        dependsOn: List<KotlinSourceSet> = emptyList()
    ): KotlinSourceSet {
        // Implementation using mockk or manual mock
        // See TestHelpers.kt for details
    }
}
```

**✅ Success Criteria**:
- BFS algorithm works correctly
- Handles diamond dependencies without duplication
- All tests pass

---

### Step 3: Implement Compilation Classifier

**Objective**: Replace string pattern matching for test detection.

#### 3.1 Create Classifier

**File**: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/CompilationClassifier.kt`

```kotlin
package com.rsicarelli.fakt.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

/**
 * Classifies compilations as test vs main using heuristics.
 * Replaces pattern matching like `moduleName.contains("test")`.
 */
internal object CompilationClassifier {

    /**
     * Determine if a compilation is for test code.
     *
     * Uses multiple heuristics:
     * 1. Standard "test" compilation name
     * 2. Convention: name ends with "Test"
     * 3. Associated with main compilation (test suite pattern)
     */
    fun isTestCompilation(compilation: KotlinCompilation<*>): Boolean {
        // Standard test compilation
        if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME) {
            return true
        }

        // Convention: integrationTest, e2eTest, etc.
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
}
```

#### 3.2 Add Tests

```kotlin
class CompilationClassifierTest {

    @Test
    fun `GIVEN test compilation WHEN classifying THEN should return true`() {
        val compilation = mockCompilation(name = "test")
        assertTrue(CompilationClassifier.isTestCompilation(compilation))
    }

    @Test
    fun `GIVEN integrationTest WHEN classifying THEN should return true`() {
        val compilation = mockCompilation(name = "integrationTest")
        assertTrue(CompilationClassifier.isTestCompilation(compilation))
    }

    @Test
    fun `GIVEN main compilation WHEN classifying THEN should return false`() {
        val compilation = mockCompilation(name = "main")
        assertFalse(CompilationClassifier.isTestCompilation(compilation))
    }
}
```

**✅ Success Criteria**:
- Correctly identifies all test compilation patterns
- No false positives for main compilations
- All tests pass

---

### Step 4: Implement SourceSetDiscovery

**Objective**: Programmatically discover source sets instead of hardcoding.

#### 4.1 Create Discovery Class

**File**: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/SourceSetDiscovery.kt`

See ARCHITECTURE.md Section 2 for full implementation.

Key method signatures:
```kotlin
internal class SourceSetDiscovery(private val project: Project) {
    fun buildContext(compilation: KotlinCompilation<*>): SourceSetContext
    private fun buildSourceSetInfo(sourceSet: KotlinSourceSet): SourceSetInfo
    private fun resolveOutputDirectory(compilation: KotlinCompilation<*>): String
}
```

**✅ Success Criteria**:
- Builds complete SourceSetContext for any compilation
- Discovers all source sets via `compilation.allKotlinSourceSets`
- No hardcoded source set names!

---

### Step 5: Implement Context Serialization

**File**: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/ContextSerializer.kt`

```kotlin
internal object ContextSerializer {
    private val json = Json { prettyPrint = false; encodeDefaults = true }

    fun serialize(context: SourceSetContext): String {
        val jsonString = json.encodeToString(context)
        return Base64.getEncoder().encodeToString(jsonString.toByteArray())
    }

    fun deserialize(encoded: String): SourceSetContext {
        val jsonString = String(Base64.getDecoder().decode(encoded))
        return json.decodeFromString(jsonString)
    }
}
```

**✅ Success Criteria**:
- Serialization roundtrip works perfectly
- Base64 encoding makes output command-line safe
- Reasonable size (< 10KB for typical projects)

---

### Step 6: Update FaktGradleSubplugin

**Objective**: Pass serialized context to compiler via SubpluginOption.

#### 6.1 Update applyToCompilation

**File**: `gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/FaktGradleSubplugin.kt`

```kotlin
override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>
): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.project
    val extension = project.extensions.getByType(FaktPluginExtension::class.java)

    // Use lazy Provider (configuration avoidance!)
    return project.provider {
        val discovery = SourceSetDiscovery(project)
        val context = discovery.buildContext(kotlinCompilation)
        val serialized = ContextSerializer.serialize(context)

        buildList {
            add(SubpluginOption(key = "enabled", value = extension.enabled.get().toString()))
            add(SubpluginOption(key = "debug", value = extension.debug.get().toString()))
            add(SubpluginOption(key = "sourceSetContext", value = serialized)) // NEW!
        }
    }
}
```

**Before (Old Code)**:
```kotlin
// ❌ Old: Hardcoded output directory resolution
val configurator = SourceSetConfigurator(project)
val outputDir = configurator.getGeneratedSourcesDirectory(kotlinCompilation)
add(SubpluginOption(key = "outputDir", value = outputDir))
```

**After (New Code)**:
```kotlin
// ✅ New: Pass complete context
val context = discovery.buildContext(kotlinCompilation)
val serialized = ContextSerializer.serialize(context)
add(SubpluginOption(key = "sourceSetContext", value = serialized))
```

**✅ Success Criteria**:
- Lazy Provider pattern used
- Serialized context passed correctly
- Works for all compilation types

---

### Step 7: Update Compiler Plugin

**Objective**: Receive context and replace SourceSetMapper.

#### 7.1 Update CommandLineProcessor

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/FaktCommandLineProcessor.kt`

```kotlin
val KEY_SOURCE_SET_CONTEXT = CompilerConfigurationKey<SourceSetContext>("source set context")

@AutoService(CommandLineProcessor::class)
class FaktCommandLineProcessor : CommandLineProcessor {

    override val pluginOptions = listOf(
        CliOption("enabled", "<true|false>", "Enable Fakt"),
        CliOption("debug", "<true|false>", "Debug mode"),
        CliOption("sourceSetContext", "<base64-json>", "Source set context") // NEW!
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
                    // Fallback: log error and continue with default behavior
                    println("Fakt: Failed to deserialize source set context: ${e.message}")
                }
            }
            // ... other options
        }
    }
}
```

#### 7.2 Create SourceSetResolver

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/output/SourceSetResolver.kt`

```kotlin
package com.rsicarelli.fakt.compiler.output

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.io.File

/**
 * Resolves source set information from Gradle-provided context.
 * REPLACES SourceSetMapper.kt (411 lines → ~50 lines).
 */
internal class SourceSetResolver(
    private val context: SourceSetContext,
    private val messageCollector: MessageCollector?
) {

    /**
     * Get output directory for generated code.
     * NO fallback logic - Gradle already resolved everything!
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

    fun isTestContext(): Boolean = context.isTest

    fun getAllSourceSets(): List<String> = context.allSourceSets.map { it.name }

    fun getDefaultSourceSet(): String = context.defaultSourceSet.name

    private fun MessageCollector.reportInfo(message: String) {
        report(org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO, message)
    }
}
```

#### 7.3 Update IrGenerationExtension

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt`

**Before**:
```kotlin
// ❌ Old: Use SourceSetMapper with hardcoded patterns
val mapper = SourceSetMapper(outputDir, messageCollector)
val outputDirectory = mapper.getGeneratedSourcesDir(moduleFragment)
```

**After**:
```kotlin
// ✅ New: Use SourceSetResolver with context from Gradle
val context = pluginContext.configuration.get(KEY_SOURCE_SET_CONTEXT)
    ?: error("SourceSetContext not provided by Gradle plugin. Please update Fakt Gradle plugin.")

val resolver = SourceSetResolver(context, messageCollector)
val outputDirectory = resolver.getGeneratedSourcesDir()
```

**✅ Success Criteria**:
- Compiler receives and decodes context
- SourceSetResolver returns correct directories
- No pattern matching logic remains
- All sample projects compile successfully

---

### Step 8: Delete Old Code

**Objective**: Remove deprecated implementations.

#### 8.1 Files to Delete

```bash
# Delete old implementations
git rm compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/output/SourceSetMapper.kt

# Keep SourceSetConfigurator for now (backward compat during migration)
# Will be cleaned up in final phase
```

#### 8.2 Update Documentation

**File**: `.claude/docs/architecture/unified-ir-native.md`

Update references to old `SourceSetMapper` → new `SourceSetResolver`.

**File**: `CLAUDE.md`

Update "Key Files" section:
```markdown
**Core Generation:**
compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
├── output/
│   └── SourceSetResolver.kt  # NEW: Receives context from Gradle (was SourceSetMapper.kt)
```

**✅ Success Criteria**:
- Old files deleted
- Documentation updated
- No broken references

---

## 🧪 Testing Migration

### Phase 1: Isolated Module Testing

Test each new component in isolation:

```bash
# Test compiler-api
./gradlew :compiler-api:test

# Test gradle-plugin utilities
./gradlew :gradle-plugin:test

# Test compiler plugin
./gradlew :compiler:test
```

### Phase 2: Sample Project Testing

Test with actual projects:

```bash
# Single-module project
cd samples/single-module
../../gradlew clean build --info | tee /tmp/after-migration.log

# Compare logs
diff /tmp/before-migration.log /tmp/after-migration.log

# Multi-module project
cd samples/multi-module
../../gradlew clean build --info

# KMP project
cd samples/kmp-comprehensive-test
../../gradlew clean build --info
```

### Phase 3: Configuration Cache Testing

```bash
# Test configuration cache compatibility
./gradlew clean build --configuration-cache
./gradlew build --configuration-cache  # Should use cache
```

### Phase 4: Incremental Compilation Testing

```bash
# Initial build
./gradlew build

# Modify a single file
echo "// Comment" >> samples/single-module/src/commonMain/kotlin/Example.kt

# Should be incremental
./gradlew build --info | grep "Incremental"
```

---

## 📊 Validation Checklist

Before considering migration complete:

- [ ] All unit tests pass (`./gradlew test`)
- [ ] All sample projects build successfully
- [ ] Generated code compiles without errors
- [ ] Generated code is in correct directories
- [ ] Configuration cache works
- [ ] Incremental compilation works
- [ ] No deprecation warnings
- [ ] IDE recognizes generated sources
- [ ] Detekt passes
- [ ] Spotless passes
- [ ] Documentation updated
- [ ] CHANGELOG.md updated

---

## 🚨 Rollback Procedure

If migration causes critical issues:

### Quick Rollback

```bash
# Revert to previous commit
git reset --hard HEAD~1

# Or revert specific files
git checkout HEAD~1 -- compiler/src/main/kotlin/compiler/output/SourceSetMapper.kt
git checkout HEAD~1 -- gradle-plugin/src/main/kotlin/gradle/SourceSetConfigurator.kt
```

### Gradual Rollback

Keep both implementations and use feature flag:

```kotlin
// In FaktPluginExtension
val useModernSourceSetMapping = project.objects.property<Boolean>()
    .convention(false) // Default to old implementation

// In FaktGradleSubplugin
if (extension.useModernSourceSetMapping.get()) {
    // New implementation
} else {
    // Old implementation
}
```

---

## 💡 Tips & Best Practices

### 1. Test Often

After each step, run tests immediately:
```bash
./gradlew :module:test
```

### 2. Use Debug Mode

Enable debug logging to see what's happening:
```kotlin
fakt {
    debug.set(true)
}
```

### 3. Compare Logs

Save logs before and after to verify behavior:
```bash
./gradlew build --info > after.log 2>&1
diff before.log after.log
```

### 4. Test Edge Cases

Don't just test default hierarchy - test:
- Custom source sets (`integrationTest`)
- Android variants (`debugTest`, `releaseTest`)
- Custom hierarchies (`applyHierarchyTemplate { ... }`)

### 5. Performance Monitoring

Benchmark build times:
```bash
# Before
time ./gradlew clean build

# After
time ./gradlew clean build
```

---

## 🎯 Success Metrics

Migration is successful when:

1. **Functionality**: All features work exactly as before
2. **Flexibility**: New features work (custom source sets, Android variants)
3. **Performance**: No significant build time increase (< 100ms)
4. **Maintainability**: Code is cleaner and easier to understand
5. **Future-Proof**: No hardcoded patterns remain

---

## 📚 Related Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) - New architecture design
- [IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md) - Detailed implementation phases
- [CODE-PATTERNS.md](./CODE-PATTERNS.md) - Reusable code snippets
- [API-REFERENCE.md](./API-REFERENCE.md) - Quick API reference

---

**Ready to Migrate**: ✅
**First Step**: Create `compiler-api` module
**Estimated Time**: 3 weeks following IMPLEMENTATION-ROADMAP.md
