# Convention Plugin Blueprint

**Date**: 2025-10-05
**Target Gradle**: 9.0+
**Target Kotlin**: 2.2.20+
**Plugin ID**: `fakt-convention`

---

## Overview

This document provides the complete specification for the `fakt-convention` Gradle plugin that enables cross-module fake consumption via custom source sets and Gradle capabilities.

**Goal**: Users apply `id("fakt-convention")` to a module, and fakes are automatically:
1. Generated to `src/fakes/kotlin/` (not `build/generated/`)
2. Exposed as consumable variants with unique capabilities
3. Available to dependent modules' tests
4. Fully indexed by IDE (navigation, completion, debugging)

---

## Table of Contents

1. [Plugin Structure](#plugin-structure)
2. [Source Set Configuration](#source-set-configuration)
3. [Variant Configuration](#variant-configuration)
4. [Complete Implementation](#complete-implementation)
5. [Testing Strategy](#testing-strategy)
6. [IDE Integration](#ide-integration)
7. [Publishing Support](#publishing-support)

---

## Plugin Structure

### Directory Layout

```
buildSrc/                                   # OR: gradle/plugins/
├── build.gradle.kts
├── settings.gradle.kts
└── src/main/kotlin/
    └── fakt-convention.gradle.kts          # Convention plugin
```

**Alternative**: Precompiled script plugin in `gradle/plugins/` (Gradle 9.0+ recommended)

```
gradle/
└── plugins/
    └── src/main/kotlin/
        └── fakt-convention.gradle.kts
```

---

### build.gradle.kts (buildSrc or gradle/plugins)

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")
}

kotlin {
    jvmToolchain(17)  // Gradle 9.0 requires Java 17+
}
```

---

## Source Set Configuration

### Goal

Create a `fakes` source set hierarchy that:
- Depends on `commonMain` (can access business interfaces)
- Is accessed by `commonTest` (tests can use fakes)
- Has per-target variants (`jvmFakes`, `jsFakes`, etc.)

### Implementation

```kotlin
// fakt-convention.gradle.kts
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    kotlin("multiplatform")
}

// Get Kotlin extension
val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

kotlin.run {
    sourceSets {
        // 1. Create common fakes source set
        val fakes = maybeCreate("fakes").apply {
            // Depend on commonMain to access interfaces
            dependsOn(getByName("commonMain"))
        }

        // 2. Local test access: commonTest depends on fakes
        getByName("commonTest").dependsOn(fakes)
    }

    // 3. Create per-target fakes source sets
    targets.configureEach { target ->
        val targetName = target.name

        // Skip metadata and common targets
        if (targetName == "metadata" || targetName == "common") return@configureEach

        sourceSets {
            // Create platform-specific fakes source set
            val targetFakes = maybeCreate("${targetName}Fakes").apply {
                // Depend on common fakes
                dependsOn(getByName("fakes"))

                // Depend on platform's main source set
                val platformMain = findByName("${targetName}Main")
                if (platformMain != null) {
                    dependsOn(platformMain)
                }
            }

            // Platform tests depend on platform fakes
            val platformTest = findByName("${targetName}Test")
            platformTest?.dependsOn(targetFakes)
        }
    }
}
```

**Result**:
```
Source Set Hierarchy:
commonMain
    ↓
  fakes (commonFakes)
    ↓
jvmFakes ← jvmMain
    ↓
jvmTest

commonTest ← fakes
```

---

## Variant Configuration

### Goal

Create consumable configurations (variants) that:
- Expose fakes compilation output
- Have correct attributes for variant matching
- Declare unique capabilities for selection

### Attribute Matching Primer

Gradle matches variants by comparing attributes:

```kotlin
// Consumer's testImplementation has attributes:
// - org.gradle.usage = java-api
// - org.jetbrains.kotlin.platform.type = jvm
// - org.gradle.category = library

// Producer's jvmFakesElements must have IDENTICAL attributes
// + unique capability: "foundation-fakes"
```

### Implementation

```kotlin
// fakt-convention.gradle.kts (continued)

kotlin.targets.configureEach { target ->
    val targetName = target.name
    if (targetName == "metadata" || targetName == "common") return@configureEach

    // 1. Get the main API configuration (template for attributes)
    val mainApiElements = configurations.findByName(target.apiElementsConfigurationName)
        ?: return@configureEach

    // 2. Find or create fakes compilation
    val fakesCompilation = target.compilations.findByName("fakes")
        ?: target.compilations.create("fakes") {
            // Associate with main compilation for dependency sharing
            associateWith(target.compilations.getByName("main"))

            // Set default source set
            defaultSourceSet.set(sourceSets.getByName("${targetName}Fakes"))
        }

    // 3. Create consumable configuration for fakes
    val fakesElements = configurations.create("${targetName}FakesElements") {
        description = "Exposes fakes for target '${targetName}'"

        // Configuration role: Consumable only
        isCanBeConsumed = true
        isCanBeResolved = false

        // Mark as visible for external consumers
        isVisible = false  // Internal implementation detail

        // 4. Copy ALL attributes from main API variant
        attributes {
            // Use attribute copying to ensure exact match
            mainApiElements.attributes.keySet().forEach { key ->
                @Suppress("UNCHECKED_CAST")
                val attributeKey = key as org.gradle.api.attributes.Attribute<Any>
                val value = mainApiElements.attributes.getAttribute(attributeKey)
                if (value != null) {
                    attribute(attributeKey, value)
                }
            }
        }

        // 5. Attach fakes compilation output as artifact
        outgoing.artifact(fakesCompilation.output.classesDirs) {
            type = "jar"  // Mark as JAR type for proper resolution
        }

        // 6. Declare unique capability
        outgoing.capability("${project.group}:${project.name}-fakes:${project.version}")
    }

    // 7. Extend from fakes source set dependencies
    val fakesSourceSet = sourceSets.getByName("${targetName}Fakes")
    val fakesApi = configurations.findByName("${fakesSourceSet.name}Api")
    val fakesImpl = configurations.findByName("${fakesSourceSet.name}Implementation")

    if (fakesApi != null) {
        fakesElements.extendsFrom(fakesApi)
    }
    if (fakesImpl != null) {
        fakesElements.extendsFrom(fakesImpl)
    }
}
```

**Key Insight**: We copy attributes from `mainApiElements` to ensure exact match. This avoids hardcoding attribute values that may change between Kotlin/Gradle versions.

---

## Complete Implementation

### Full fakt-convention.gradle.kts

```kotlin
/**
 * Fakt Convention Plugin
 *
 * Configures a Kotlin Multiplatform module to:
 * 1. Create 'fakes' source sets for generated fake implementations
 * 2. Expose fakes as consumable Gradle variants with unique capabilities
 * 3. Enable cross-module fake consumption in dependent modules' tests
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     id("fakt-convention")
 * }
 * ```
 *
 * @since 1.0.0
 */

import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

plugins {
    kotlin("multiplatform")
}

// ========================================
// 1. Source Set Configuration
// ========================================

val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

kotlin.run {
    sourceSets {
        // Common fakes source set
        val fakes = maybeCreate("fakes").apply {
            dependsOn(getByName("commonMain"))

            // Register generated sources directory
            kotlin.srcDir(layout.projectDirectory.dir("src/fakes/kotlin"))
        }

        // Local test access
        getByName("commonTest").dependsOn(fakes)
    }

    // Per-target fakes source sets
    targets.configureEach { target ->
        val targetName = target.name

        // Skip special targets
        if (targetName in listOf("metadata", "common")) return@configureEach

        sourceSets {
            val targetFakes = maybeCreate("${targetName}Fakes").apply {
                dependsOn(getByName("fakes"))

                // Depend on platform main if exists
                findByName("${targetName}Main")?.let { dependsOn(it) }

                // Register platform-specific generated directory
                kotlin.srcDir(layout.projectDirectory.dir("src/${targetName}Fakes/kotlin"))
            }

            // Platform tests access platform fakes
            findByName("${targetName}Test")?.dependsOn(targetFakes)
        }
    }
}

// ========================================
// 2. Variant Configuration
// ========================================

afterEvaluate {
    kotlin.targets.configureEach { target ->
        val targetName = target.name
        if (targetName in listOf("metadata", "common")) return@configureEach

        // Find main API configuration
        val mainApiElements = configurations.findByName(target.apiElementsConfigurationName)
            ?: run {
                logger.warn("Fakt: Could not find API elements for target $targetName")
                return@configureEach
            }

        // Create or get fakes compilation
        val fakesCompilation = target.compilations.findByName("fakes")
            ?: target.compilations.create("fakes") {
                associateWith(target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME))
                defaultSourceSet.set(sourceSets.getByName("${targetName}Fakes"))
            }

        // Create consumable variant
        configurations.create("${targetName}FakesElements") {
            description = "Fakt fakes variant for $targetName"

            isCanBeConsumed = true
            isCanBeResolved = false
            isVisible = false

            // Copy attributes from main variant
            attributes {
                mainApiElements.attributes.keySet().forEach { key ->
                    @Suppress("UNCHECKED_CAST")
                    val attr = key as Attribute<Any>
                    mainApiElements.attributes.getAttribute(attr)?.let { value ->
                        attribute(attr, value)
                    }
                }
            }

            // Attach compilation output
            outgoing.artifact(fakesCompilation.output.classesDirs) {
                type = "jar"
            }

            // Declare capability
            val capability = "${project.group}:${project.name}-fakes:${project.version}"
            outgoing.capability(capability)

            logger.info("Fakt: Created variant $name with capability: $capability")

            // Extend from fakes dependencies
            val fakesSourceSet = sourceSets.getByName("${targetName}Fakes")
            configurations.findByName("${fakesSourceSet.name}Api")?.let { extendsFrom(it) }
            configurations.findByName("${fakesSourceSet.name}Implementation")?.let { extendsFrom(it) }
        }
    }
}

// ========================================
// 3. Logging and Validation
// ========================================

afterEvaluate {
    logger.lifecycle("Fakt Convention applied to ${project.name}")
    logger.lifecycle("Fakt: Created fakes source sets for targets: ${kotlin.targets.names.filter { it !in listOf("metadata", "common") }}")
}
```

---

## Testing Strategy

### Unit Tests for Convention Plugin

```kotlin
// buildSrc/src/test/kotlin/FaktConventionPluginTest.kt
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FaktConventionPluginTest {

    @Test
    fun `GIVEN KMP project WHEN applying fakt-convention THEN should create fakes source set`() {
        // Given
        val project = ProjectBuilder.builder().build()

        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.plugins.apply("fakt-convention")

        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.jvm()

        // When
        project.evaluate()

        // Then
        assertNotNull(kotlin.sourceSets.findByName("fakes"))
        assertNotNull(kotlin.sourceSets.findByName("jvmFakes"))
    }

    @Test
    fun `GIVEN KMP project WHEN applying fakt-convention THEN should create consumable configurations`() {
        // Given
        val project = ProjectBuilder.builder().build()

        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.plugins.apply("fakt-convention")

        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.jvm()

        // When
        project.evaluate()

        // Then
        val jvmFakesElements = project.configurations.findByName("jvmFakesElements")
        assertNotNull(jvmFakesElements)
        assertTrue(jvmFakesElements.isCanBeConsumed)
        assertTrue(!jvmFakesElements.isCanBeResolved)
    }
}
```

### Integration Test

```bash
# Create test project structure
mkdir -p test-project/{foundation,domain}

# test-project/foundation/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("fakt-convention")
}

kotlin {
    jvm()
}

# test-project/domain/build.gradle.kts
plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        commonTest {
            dependencies {
                implementation(project(":foundation")) {
                    capabilities {
                        requireCapability("com.test:foundation-fakes:1.0")
                    }
                }
            }
        }
    }
}

# Test
./gradlew :domain:dependencies --configuration commonTestCompileClasspath
# Expected: See foundation-fakes capability resolved
```

---

## IDE Integration

### IntelliJ IDEA Configuration

The convention plugin automatically configures source sets for IDE indexing. Ensure proper setup:

#### 1. Gradle JVM Settings

```bash
# gradle.properties
org.gradle.java.home=/path/to/jdk-17  # Gradle 9.0 requires Java 17+
kotlin.mpp.enableGranularSourceSetsMetadata=true
kotlin.native.enableKlibsCrossCompilation=true
```

#### 2. IDE Reimport

After applying `fakt-convention`:

1. **Gradle → Reload All Gradle Projects**
2. **File → Invalidate Caches → Invalidate and Restart**
3. Verify in **Project Structure → Modules**:
   - `foundation` module should show `fakes` and `jvmFakes` source sets

#### 3. Source Root Configuration

```kotlin
// If IDE doesn't detect automatically, explicitly mark:
idea {
    module {
        sourceDirs.add(file("src/fakes/kotlin"))
        sourceDirs.add(file("src/jvmFakes/kotlin"))

        testSources.from("src/fakes/kotlin")  // Mark as test sources
    }
}
```

---

## Publishing Support

### Maven Central Publishing

The fakes variant is automatically included in Gradle Module Metadata when publishing:

```kotlin
// foundation/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("fakt-convention")
    `maven-publish`
}

publishing {
    publications {
        // KMP plugin auto-creates publications
        // Fakes variant is included automatically in .module metadata
    }

    repositories {
        maven {
            name = "sonatype"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrhUsername") as String?
                password = project.findProperty("ossrhPassword") as String?
            }
        }
    }
}
```

**Published Files**:
```
foundation-1.0.0.jar           # Main artifact
foundation-1.0.0.module        # GMM (contains jvmFakesElements variant)
foundation-1.0.0-sources.jar   # Sources (includes src/fakes/)
foundation-1.0.0.pom           # Maven POM (legacy)
```

### Verifying Published Metadata

```bash
# Publish to mavenLocal
./gradlew publishToMavenLocal

# Inspect metadata
cat ~/.m2/repository/com/example/foundation/1.0.0/foundation-1.0.0.module | jq '.variants[] | select(.name | contains("Fakes"))'

# Expected output:
{
  "name": "jvmFakesElements",
  "attributes": {
    "org.gradle.usage": "java-api",
    "org.jetbrains.kotlin.platform.type": "jvm"
  },
  "capabilities": [
    {
      "group": "com.example",
      "name": "foundation-fakes",
      "version": "1.0.0"
    }
  ]
}
```

---

## Advanced Configuration

### Gradle 9.0 Features

```kotlin
// Use declarative DSL improvements (Gradle 9.0+)
kotlin {
    targets.configureEach { target ->
        // Lazy configuration API
        compilations.configureEach {
            if (name == "fakes") {
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
                    }
                }
            }
        }
    }
}
```

### Kotlin 2.2.20 Compatibility

```kotlin
// Ensure K2 compiler compatibility
kotlin {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}
```

---

## Troubleshooting

### Issue 1: Variant Not Found

**Symptom**:
```
Cannot find a variant of project :foundation that matches the consumer attributes
```

**Debug**:
```bash
# Check published variants
./gradlew :foundation:outgoingVariants --all

# Verify attributes match
./gradlew :domain:dependencies --configuration commonTestCompileClasspath --debug
```

**Fix**: Ensure attributes are copied correctly from main variant.

---

### Issue 2: IDE Doesn't Index Fakes

**Symptom**: "Unresolved reference" in IDE but Gradle build works

**Fix**:
1. Invalidate caches
2. Check source directory exists: `mkdir -p src/fakes/kotlin`
3. Explicitly add to IDEA module (see IDE Integration section)

---

### Issue 3: Capability Not Declared

**Symptom**: Variant selected but wrong one (gets main variant instead of fakes)

**Fix**: Verify capability format exactly matches:
```kotlin
"${project.group}:${project.name}-fakes:${project.version}"
```

---

## Migration Guide

### From Dedicated Modules

```kotlin
// Before: Dedicated module
:foundation
:foundation-fakes  // Dedicated module

// foundation-fakes/build.gradle.kts
plugins {
    kotlin("multiplatform")
}

dependencies {
    api(project(":foundation"))
}

// After: Convention plugin
:foundation  // Single module

// foundation/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("fakt-convention")  // Just add this!
}

// Delete :foundation-fakes module
// Remove from settings.gradle.kts
```

**Consumer Changes**: None required if using auto-wiring (Week 3 feature).

---

## Performance Considerations

### Build Cache

```kotlin
// Mark fakes compilation as cacheable
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (name.contains("Fakes")) {
        outputs.cacheIf { true }
    }
}
```

### Incremental Compilation

Fakes source set supports incremental compilation by default:

```bash
# Verify incremental compilation
./gradlew :foundation:compileFakesKotlinJvm --info | grep "Incremental compilation"
# Expected: "Incremental compilation is enabled"
```

---

## References

### Gradle APIs Used

- `KotlinMultiplatformExtension` - Kotlin DSL
- `SourceSet.dependsOn()` - Source set hierarchy
- `Configuration.outgoing.capability()` - Capability declaration
- `Configuration.attributes` - Variant attributes
- `KotlinCompilation` - Compilation management

### Related Documentation

- [Kotlin Multiplatform DSL](https://kotlinlang.org/docs/multiplatform-dsl-reference.html)
- [Gradle Feature Variants](https://docs.gradle.org/current/userguide/feature_variants.html)
- [Precompiled Script Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins)

---

**Next Steps**: See [IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md) for Week 1 implementation plan.
