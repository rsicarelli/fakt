# Multi-Module Support: Technical Reference

**Date**: 2025-10-05
**Strategy**: Custom Source Sets as Feature Variants
**Gradle Version**: 7.6+
**Kotlin Version**: 1.9.x+

---

## Table of Contents

1. [Gradle Fundamentals](#gradle-fundamentals)
2. [Variant Attributes Matrix](#variant-attributes-matrix)
3. [Configuration Anatomy](#configuration-anatomy)
4. [Source Set Hierarchy](#source-set-hierarchy)
5. [Capabilities Declaration](#capabilities-declaration)
6. [Complete Working Example](#complete-working-example)
7. [Debugging Guide](#debugging-guide)
8. [Common Pitfalls](#common-pitfalls)

---

## Gradle Fundamentals

### Configuration Roles

Gradle configurations have two fundamental roles:

```kotlin
configurations {
    // CONSUMABLE: Other projects can depend on this
    create("jvmFakesElements") {
        isCanBeConsumed = true   // ✅ Can be consumed
        isCanBeResolved = false  // ❌ Cannot resolve dependencies
    }

    // RESOLVABLE: This project uses it to resolve dependencies
    create("testImplementation") {
        isCanBeConsumed = false  // ❌ Cannot be consumed
        isCanBeResolved = true   // ✅ Can resolve dependencies
    }

    // BUCKET: Just holds dependencies (extends from pattern)
    create("api") {
        isCanBeConsumed = false
        isCanBeResolved = false
    }
}
```

**Key Insight**: For cross-module fake consumption:
- **Producer** (`:foundation`) creates **CONSUMABLE** configurations
- **Consumer** (`:domain`) has **RESOLVABLE** configurations
- Gradle matches consumable → resolvable via **attributes** and **capabilities**

---

### Gradle Module Metadata (GMM)

```json
// Published module metadata (foundation-1.0.module)
{
  "variants": [
    {
      "name": "jvmApiElements",
      "attributes": {
        "org.gradle.usage": "java-api",
        "org.jetbrains.kotlin.platform.type": "jvm"
      }
    },
    {
      "name": "jvmFakesElements",  // Our custom variant!
      "attributes": {
        "org.gradle.usage": "java-api",
        "org.jetbrains.kotlin.platform.type": "jvm"
      },
      "capabilities": [
        {
          "group": "com.rsicarelli.fakt",
          "name": "foundation-fakes",  // Unique capability
          "version": "1.0.0-SNAPSHOT"
        }
      ]
    }
  ]
}
```

**Why This Matters**:
- GMM allows multiple variants per module
- Each variant has unique attributes + capabilities
- Consumer selects variant by declaring required capability

---

## Variant Attributes Matrix

### Standard Kotlin Multiplatform Attributes

| Attribute                            | JVM Value       | JS Value         | Native (iOS) Value |
|--------------------------------------|-----------------|------------------|--------------------|
| `org.gradle.usage`                   | `java-api`      | `kotlin-api`     | `kotlin-api`       |
| `org.jetbrains.kotlin.platform.type` | `jvm`           | `js`             | `native`           |
| `org.gradle.category`                | `library`       | `library`        | `library`          |
| `org.gradle.libraryelements`         | `jar`           | `jar`            | `klib`             |

### How Gradle Matches Variants

```kotlin
// Consumer (:domain) declares dependency
dependencies {
    testImplementation(project(":foundation")) {
        capabilities {
            requireCapability("com.rsicarelli.fakt:foundation-fakes:1.0.0-SNAPSHOT")
        }
    }
}

// Consumer's testImplementation configuration has attributes:
// - org.jetbrains.kotlin.platform.type = "jvm"
// - org.gradle.usage = "java-api"
// - PLUS capability requirement: "foundation-fakes"

// Gradle matches to producer's jvmFakesElements variant because:
// 1. Attributes match (jvm + java-api)
// 2. Capability matches (foundation-fakes)
```

**Critical Rule**: Fakes variant MUST copy **ALL** attributes from main API variant to ensure resolution works.

---

## Configuration Anatomy

### Producer Configuration (`:foundation`)

```kotlin
// fakt-convention.gradle.kts
kotlin.targets.all { target ->
    // 1. Get the main API configuration (template for attributes)
    val mainApiElements = configurations.getByName(target.apiElementsConfigurationName)
    // Example: "jvmApiElements"

    // 2. Create fakes consumable configuration
    val fakesElements = configurations.create("${target.name}FakesElements") {
        description = "Exposes fakes for target '${target.name}'"

        // Role: Consumable only
        isCanBeConsumed = true
        isCanBeResolved = false

        // 3. Inherit dependencies from fakes source set
        val fakesSourceSet = sourceSets.getByName("${target.name}Fakes")
        extendsFrom(configurations.getByName("${fakesSourceSet.name}Api"))
        extendsFrom(configurations.getByName("${fakesSourceSet.name}Implementation"))
    }

    // 4. Attach compilation output as artifact
    val fakesCompilation = target.compilations.getByName("fakes")
    fakesElements.outgoing.artifact(fakesCompilation.output.classesDirs.singleFile)

    // 5. CRITICAL: Copy all attributes from main variant
    fakesElements.attributes {
        // Copy from mainApiElements
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        // ... all attributes
    }

    // 6. Add unique capability
    fakesElements.outgoing.capability(
        "${project.group}:${project.name}-fakes:${project.version}"
    )
}
```

---

### Consumer Configuration (`:domain`)

```kotlin
// domain/build.gradle.kts
dependencies {
    // Automatic approach (future: auto-wiring)
    commonTestImplementation(project(":foundation")) {
        capabilities {
            requireCapability("com.rsicarelli.fakt:foundation-fakes:1.0.0-SNAPSHOT")
        }
    }

    // Manual approach (fallback)
    add("commonTestImplementation", project(":foundation")) {
        capabilities {
            requireCapability("com.rsicarelli.fakt:foundation-fakes:1.0.0-SNAPSHOT")
        }
    }
}
```

**What Happens**:
1. Gradle sees `commonTestImplementation` needs dependency on `:foundation`
2. Consumer declares capability requirement: `foundation-fakes`
3. Gradle queries `:foundation`'s consumable configurations
4. Finds `jvmFakesElements` (or `jsFakesElements`, etc.) with matching:
   - Attributes (platform type, usage)
   - Capability (`foundation-fakes`)
5. Resolves to fakes variant, adds to classpath
6. Test code can now import `foundation.fakeLogger`

---

## Source Set Hierarchy

### Foundation Module Structure

```
:foundation/
├── src/
│   ├── commonMain/kotlin/
│   │   └── foundation/
│   │       ├── Logger.kt              # @Fake interface
│   │       └── ConfigService.kt       # @Fake interface
│   │
│   ├── fakes/kotlin/                   # NEW: Common fakes source set
│   │   └── foundation/
│   │       ├── FakeLoggerImpl.kt      # Generated by compiler
│   │       └── FakeConfigServiceImpl.kt
│   │
│   ├── jvmFakes/kotlin/                # NEW: Platform-specific fakes
│   │   └── foundation/
│   │       └── (platform-specific overrides if needed)
│   │
│   ├── commonTest/kotlin/
│   │   └── foundation/
│   │       └── LoggerTest.kt          # Local tests (can use fakes)
│   │
│   └── jvmTest/kotlin/
│       └── (platform tests)
│
└── build.gradle.kts                    # Applies: id("fakt-convention")
```

### Source Set Dependency Graph

```
commonMain
    ↓
  fakes (depends on commonMain)
    ↓
jvmFakes (depends on fakes + jvmMain)
    ↓
commonTest (depends on fakes)
    ↓
jvmTest (depends on jvmFakes)
```

**Configuration DSL**:

```kotlin
// fakt-convention.gradle.kts
kotlin {
    sourceSets {
        // 1. Common fakes source set
        val fakes = create("fakes") {
            dependsOn(commonMain.get())
        }

        // 2. Local test access
        commonTest.get().dependsOn(fakes)

        // 3. Per-target fakes
        targets.all { target ->
            val targetFakes = create("${target.name}Fakes") {
                dependsOn(fakes)
                dependsOn(target.compilations.getByName("main").defaultSourceSet)
            }

            // 4. Platform tests depend on platform fakes
            val targetTest = findByName("${target.name}Test")
            targetTest?.dependsOn(targetFakes)
        }
    }
}
```

---

## Capabilities Declaration

### Producer Side (Foundation)

```kotlin
// Capability format: "${group}:${artifactId}-fakes:${version}"
val capability = "com.rsicarelli.fakt:foundation-fakes:1.0.0-SNAPSHOT"

configurations.getByName("jvmFakesElements") {
    outgoing.capability(capability)
}

// Published to Maven metadata:
// foundation-1.0.module contains:
{
  "capabilities": [
    {
      "group": "com.rsicarelli.fakt",
      "name": "foundation-fakes",
      "version": "1.0.0-SNAPSHOT"
    }
  ]
}
```

### Consumer Side (Domain)

```kotlin
// Method 1: Inline capability requirement
dependencies {
    commonTestImplementation(project(":foundation")) {
        capabilities {
            requireCapability("com.rsicarelli.fakt:foundation-fakes:1.0.0-SNAPSHOT")
        }
    }
}

// Method 2: Typed API
dependencies {
    commonTestImplementation(project(":foundation")) {
        capabilities {
            requireCapability(
                DefaultImmutableCapability(
                    "com.rsicarelli.fakt",
                    "foundation-fakes",
                    "1.0.0-SNAPSHOT"
                )
            )
        }
    }
}

// Method 3: Extension function (future helper)
dependencies {
    commonTestImplementation(project(":foundation").fakes())
}

// Extension implementation:
fun Project.fakes(): Dependency {
    return dependencies.project(mapOf("path" to path)).apply {
        (this as ModuleDependency).capabilities {
            requireCapability("${group}:${name}-fakes:${version}")
        }
    }
}
```

---

## Complete Working Example

### Step 1: Foundation Module Setup

```kotlin
// foundation/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("fakt-convention")  // Applies our convention plugin
}

kotlin {
    jvm()
    js(IR) { nodejs() }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
    }
}
```

```kotlin
// foundation/src/commonMain/kotlin/foundation/Logger.kt
package foundation

import com.rsicarelli.fakt.Fake

@Fake
interface Logger {
    fun info(message: String)
    suspend fun error(message: String, throwable: Throwable)
    val minLevel: LogLevel
}

enum class LogLevel { DEBUG, INFO, ERROR }
```

**Compiler Plugin Output** (generated to `src/fakes/kotlin/`):

```kotlin
// foundation/src/fakes/kotlin/foundation/FakeLoggerImpl.kt
package foundation

class FakeLoggerImpl : Logger {
    private var infoBehavior: (String) -> Unit = {}
    private var errorBehavior: suspend (String, Throwable) -> Unit = { _, _ -> }
    private var minLevelBehavior: () -> LogLevel = { LogLevel.DEBUG }

    override fun info(message: String) = infoBehavior(message)
    override suspend fun error(message: String, throwable: Throwable) = errorBehavior(message, throwable)
    override val minLevel: LogLevel get() = minLevelBehavior()

    internal fun configureInfo(behavior: (String) -> Unit) { infoBehavior = behavior }
    internal fun configureError(behavior: suspend (String, Throwable) -> Unit) { errorBehavior = behavior }
    internal fun configureMinLevel(behavior: () -> LogLevel) { minLevelBehavior = behavior }
}

fun fakeLogger(configure: FakeLoggerConfig.() -> Unit = {}): Logger {
    return FakeLoggerImpl().apply {
        FakeLoggerConfig(this).configure()
    }
}

class FakeLoggerConfig(private val fake: FakeLoggerImpl) {
    fun info(behavior: (String) -> Unit) = fake.configureInfo(behavior)
    fun error(behavior: suspend (String, Throwable) -> Unit) = fake.configureError(behavior)
    fun minLevel(behavior: () -> LogLevel) = fake.configureMinLevel(behavior)
}
```

---

### Step 2: Domain Module Consumption

```kotlin
// domain/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")  // Fakt gradle plugin
}

kotlin {
    jvm()
    js(IR) { nodejs() }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":foundation"))  // Main code dependency
            }
        }

        commonTest {
            dependencies {
                // AUTO-WIRED by Fakt plugin (future):
                // Plugin detects project(":foundation") and auto-adds fakes capability

                // MANUAL (current):
                implementation(project(":foundation")) {
                    capabilities {
                        requireCapability("com.rsicarelli.fakt:foundation-fakes:1.0.0-SNAPSHOT")
                    }
                }
            }
        }
    }
}
```

```kotlin
// domain/src/commonMain/kotlin/domain/UserRepository.kt
package domain

import foundation.Logger

interface UserRepository {
    suspend fun getUser(id: String, logger: Logger): User
}

data class User(val id: String, val name: String)
```

```kotlin
// domain/src/commonTest/kotlin/domain/UserRepositoryTest.kt
package domain

import foundation.fakeLogger  // ✅ Cross-module import works!
import foundation.LogLevel
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class UserRepositoryTest {
    @Test
    fun `GIVEN repository WHEN getting user THEN should log info`() = runTest {
        // Given
        val loggedMessages = mutableListOf<String>()
        val logger = fakeLogger {
            info { message -> loggedMessages.add(message) }
            minLevel { LogLevel.INFO }
        }

        val repository = FakeUserRepository()

        // When
        repository.getUser("123", logger)

        // Then
        assert(loggedMessages.contains("Fetching user 123"))
    }
}
```

---

### Step 3: Convention Plugin Implementation

```kotlin
// buildSrc/src/main/kotlin/fakt-convention.gradle.kts
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    kotlin("multiplatform")
}

val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

kotlin.run {
    // 1. Create common fakes source set
    sourceSets {
        val fakes = create("fakes") {
            dependsOn(commonMain.get())
        }
        commonTest.get().dependsOn(fakes)
    }

    // 2. Create per-target fakes source sets
    targets.all { target ->
        val targetFakes = sourceSets.create("${target.name}Fakes") {
            dependsOn(sourceSets.getByName("fakes"))
            dependsOn(target.compilations.getByName("main").defaultSourceSet)
        }

        // 3. Create fakes compilation
        target.compilations.create("fakes") {
            associateWith(target.compilations.getByName("main"))
            defaultSourceSet.set(targetFakes)
        }

        // 4. Create consumable configuration
        val mainApiElements = configurations.getByName(target.apiElementsConfigurationName)
        val fakesElements = configurations.create("${target.name}FakesElements") {
            isCanBeConsumed = true
            isCanBeResolved = false
            description = "Exposes fakes for ${target.name}"

            // Copy attributes from main API variant
            attributes.putAll(mainApiElements.attributes)

            // Add unique capability
            outgoing.capability("${project.group}:${project.name}-fakes:${project.version}")

            // Attach fakes compilation output
            val fakesCompilation = target.compilations.getByName("fakes")
            outgoing.artifact(fakesCompilation.output.classesDirs.singleFile)
        }
    }
}
```

---

## Debugging Guide

### Check Published Variants

```bash
# See all consumable configurations (producer)
./gradlew :foundation:outgoingVariants

# Expected output:
# --------------------------------------------------
# Variant jvmFakesElements
# --------------------------------------------------
# Capabilities:
#   - com.rsicarelli.fakt:foundation-fakes:1.0.0-SNAPSHOT (default capability)
# Attributes:
#   - org.gradle.usage            = java-api
#   - org.jetbrains.kotlin.platform.type = jvm
```

### Verify Dependency Resolution

```bash
# Check what variant was selected (consumer)
./gradlew :domain:dependencies --configuration commonTestCompileClasspath

# Expected output:
# commonTestCompileClasspath - Resolved configuration for compilation for source set 'commonTest'.
# \--- project :foundation
#      variant "jvmFakesElements" [
#          Requested attributes not found in the selected variant:
#              - Required capability com.rsicarelli.fakt:foundation-fakes:1.0.0-SNAPSHOT
#      ]
```

### Inspect Module Metadata

```bash
# Publish to mavenLocal
./gradlew :foundation:publishToMavenLocal

# View generated metadata
cat ~/.m2/repository/com/rsicarelli/fakt/foundation/1.0.0-SNAPSHOT/foundation-1.0.0-SNAPSHOT.module

# Search for fakes variant:
jq '.variants[] | select(.name | contains("Fakes"))' foundation-*.module
```

### Enable Gradle Resolution Logging

```kotlin
// settings.gradle.kts
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Enable dependency resolution debugging
gradle.startParameter.logLevel = LogLevel.INFO
```

```bash
# Run with --info flag
./gradlew :domain:compileTestKotlinJvm --info | grep -i "fakes"

# Expected logs:
# Selected variant 'jvmFakesElements' for project :foundation
# Dependency resolution matched capability: foundation-fakes
```

---

## Common Pitfalls

### Pitfall 1: Attributes Mismatch

**Symptom**:
```
FAILURE: Cannot find a variant of project :foundation that matches the consumer attributes
```

**Cause**: Fakes variant attributes don't match consumer requirements.

**Fix**: Ensure ALL attributes are copied from main API variant:

```kotlin
// ❌ Wrong: Partial attributes
fakesElements.attributes {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
    // Missing platform type!
}

// ✅ Correct: Copy all attributes
fakesElements.attributes.putAll(mainApiElements.attributes)
```

---

### Pitfall 2: Wrong Capability Format

**Symptom**: Variant not selected even though attributes match.

**Cause**: Capability string format incorrect.

**Fix**: Use exact format: `${group}:${name}-fakes:${version}`

```kotlin
// ❌ Wrong formats:
outgoing.capability("foundation-fakes")  // Missing group and version
outgoing.capability("${project.name}-fakes")  // Missing group
outgoing.capability("${group}:foundation:${version}")  // Missing -fakes suffix

// ✅ Correct:
outgoing.capability("${project.group}:${project.name}-fakes:${project.version}")
// Example: "com.rsicarelli.fakt:foundation-fakes:1.0.0-SNAPSHOT"
```

---

### Pitfall 3: Source Set Not Registered

**Symptom**: IDE doesn't recognize fakes, "Unresolved reference" errors.

**Cause**: Generated code directory not added to source set.

**Fix**: Explicitly register generated directory:

```kotlin
// In compiler plugin's SourceSetConfigurator
kotlin.sourceSets.getByName("${target.name}Fakes") {
    kotlin.srcDir(generatedSourcesDir)
}

// Verify in build script:
kotlin {
    sourceSets.all {
        println("Source set: $name")
        println("Kotlin dirs: ${kotlin.srcDirs}")
    }
}
```

---

### Pitfall 4: Build Order Issues

**Symptom**: "Could not resolve project :foundation" when consuming fakes.

**Cause**: Fakes not generated before consumer compilation.

**Fix**: Ensure proper task dependencies:

```kotlin
// In consumer (domain) module
tasks.named("compileTestKotlinJvm") {
    dependsOn(":foundation:compileFakesKotlinJvm")  // Explicit dependency
}

// OR: Use Gradle's automatic dependency resolution
dependencies {
    commonTestImplementation(project(":foundation")) {
        // Gradle infers task dependencies automatically
    }
}
```

---

### Pitfall 5: IDE Indexing Failure

**Symptom**: Fakes work in Gradle build but IDE shows errors.

**Cause**: IDE hasn't re-indexed after source set changes.

**Fix**:
1. **File → Invalidate Caches → Invalidate and Restart**
2. **Gradle → Reload All Gradle Projects**
3. Verify source set appears in Project Structure:
   - **File → Project Structure → Modules**
   - Check for `fakes` source set under foundation module

---

## Advanced Topics

### Publishing to Maven Central

```kotlin
// foundation/build.gradle.kts
publishing {
    publications {
        create<MavenPublication>("kotlinMultiplatform") {
            // Fakes variant is automatically included in GMM!
            // No special configuration needed
        }
    }
}
```

**Published Structure**:
```
foundation-1.0.0.jar           # Main artifact
foundation-1.0.0.module        # Gradle Module Metadata (contains fakes variant)
foundation-1.0.0-sources.jar   # Sources (includes fakes/ directory)
```

---

### Android-Specific Configuration

```kotlin
// Android modules need special handling
android {
    sourceSets {
        getByName("test") {
            // Android test source set depends on fakes
            kotlin.srcDir("src/fakes/kotlin")
        }
    }
}

// OR: Use test fixtures plugin for Android
android {
    testFixtures {
        enable = true
    }
}

// Then integrate with fakes source set
kotlin {
    sourceSets.getByName("testFixtures") {
        dependsOn(sourceSets.getByName("fakes"))
    }
}
```

---

### Configuration Cache Compatibility

```kotlin
// Ensure convention plugin is configuration cache safe
abstract class FaktConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // ✅ Use lazy APIs
        project.extensions.configure<KotlinMultiplatformExtension> {
            targets.configureEach { target ->
                // Configuration cache safe
            }
        }

        // ❌ Avoid eager APIs
        project.extensions.getByType<KotlinMultiplatformExtension>().targets.all { target ->
            // Not configuration cache safe!
        }
    }
}
```

---

## References

### Gradle Documentation
- [Variant-Aware Dependency Resolution](https://docs.gradle.org/current/userguide/variant_aware_resolution.html)
- [Modeling Feature Variants and Optional Dependencies](https://docs.gradle.org/current/userguide/feature_variants.html)
- [Gradle Module Metadata Specification](https://github.com/gradle/gradle/blob/master/subprojects/docs/src/docs/design/gradle-module-metadata-latest-specification.md)

### Kotlin Multiplatform
- [Hierarchical Project Structure](https://kotlinlang.org/docs/multiplatform-hierarchy.html)
- [Source Set DSL Reference](https://kotlinlang.org/docs/multiplatform-dsl-reference.html)

### Real-World Examples
- Android Gradle Plugin: `testFixtures.enable = true` pattern
- Jetpack Compose: Multi-variant publishing
- kotlinx.coroutines: KMP source set hierarchy

---

**Next**: See [COMPARISON-MATRIX.md](./COMPARISON-MATRIX.md) for side-by-side comparison with alternative approaches.
