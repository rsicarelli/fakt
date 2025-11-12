# Getting Started with Multi-Module

Set up Fakt multi-module support in 15 minutes with this step-by-step tutorial.

---

## Prerequisites

Before starting, ensure you have:

- ✅ Kotlin Multiplatform or JVM project with multiple Gradle modules
- ✅ Fakt plugin installed (see [Installation](../introduction/installation.md))
- ✅ Basic understanding of Gradle module structure
- ✅ Type-safe project accessors enabled in `settings.gradle.kts`

!!! tip "Type-Safe Project Accessors"
    If you don't have type-safe accessors enabled, add to `settings.gradle.kts`:
    ```kotlin
    enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
    ```
    Then sync Gradle to generate `projects.*` accessors.

---

## Tutorial Overview

We'll create a simple multi-module setup:

```
my-project/
├── core/analytics/           # Producer (defines @Fake interfaces)
├── core/analytics-fakes/     # Collector (collects generated fakes)
└── app/                      # Consumer (uses fakes in tests)
```

**Time**: ~15 minutes

---

## Step 1: Create Producer Module

The producer module contains `@Fake` annotated interfaces.

### Create Module Structure

```bash
mkdir -p core/analytics/src/commonMain/kotlin/com/example/core/analytics
```

### Configure build.gradle.kts

```kotlin
// core/analytics/build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.20"
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

kotlin {
    // Configure your KMP targets
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// Optional: Enable detailed logging
fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.INFO)
}
```

### Define @Fake Interface

```kotlin
// core/analytics/src/commonMain/kotlin/com/example/core/analytics/Analytics.kt
package com.example.core.analytics

import com.rsicarelli.fakt.Fake

@Fake
interface Analytics {
    fun track(event: String)
    suspend fun identify(userId: String)
    val sessionId: String
}
```

### Build the Producer

```bash
./gradlew :core:analytics:build
```

**Verify**: Check that fakes were generated:

```bash
ls core/analytics/build/generated/fakt/commonTest/kotlin/com/example/core/analytics/
# Should see:
# - FakeAnalyticsImpl.kt
# - fakeAnalytics.kt
# - FakeAnalyticsConfig.kt
```

---

## Step 2: Create Collector Module

The collector module collects generated fakes and makes them available to other modules.

### Create Module Structure

```bash
mkdir -p core/analytics-fakes/src
```

!!! info "Naming Convention"
    We use `-fakes` suffix as a recommended convention, but you can name it anything:

    - `core/analytics-fakes` ✅ (recommended)
    - `core/analytics-test` ✅
    - `core/analytics-test-fixtures` ✅
    - `test/analytics` ✅

### Configure build.gradle.kts

```kotlin
// core/analytics-fakes/build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.20"
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

kotlin {
    // MUST match producer's targets
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                // CRITICAL: api() exposes original types to consumers
                api(projects.core.analytics)

                // Add dependencies needed by generated fakes
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            }
        }
    }
}

fakt {
    // Enable multi-module mode
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)

    // Optional: Enable detailed logging
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.INFO)
}
```

### Key Configuration Points

#### 1. Use `api()` for Producer Dependency

```kotlin
// ✅ CORRECT: api() exposes types to consumers
api(projects.core.analytics)

// ❌ WRONG: implementation() hides types
implementation(projects.core.analytics)
```

**Why**: Consumers need access to original interface types (e.g., `Analytics`). Using `api()` makes them transitive.

#### 2. Match Producer's Targets

```kotlin
// Producer has:
kotlin {
    jvm()
    iosArm64()
}

// Collector MUST have same targets:
kotlin {
    jvm()
    iosArm64()
}
```

**Why**: FakeCollectorTask generates platform-specific sources. Mismatched targets cause compilation errors.

#### 3. Declare Transitive Dependencies

If generated fakes use coroutines, add:

```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
```

Check generated code to identify needed dependencies.

### Build the Collector

```bash
./gradlew :core:analytics-fakes:build
```

**Verify**: Check that fakes were collected:

```bash
ls core/analytics-fakes/build/generated/collected-fakes/commonMain/kotlin/com/example/core/analytics/
# Should see:
# - FakeAnalyticsImpl.kt
# - fakeAnalytics.kt
# - FakeAnalyticsConfig.kt
```

---

## Step 3: Register Modules in settings.gradle.kts

Add both modules to your project:

```kotlin
// settings.gradle.kts
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "my-project"

include(":core:analytics")
include(":core:analytics-fakes")
include(":app")
```

**Sync Gradle** to generate type-safe accessors (`projects.core.analytics`, etc.).

---

## Step 4: Use Fakes in Consumer Module

Now use the collected fakes in your app or feature modules.

### Configure app/build.gradle.kts

```kotlin
// app/build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.20"
}

kotlin {
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                // Main code depends on original interfaces
                implementation(projects.core.analytics)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))

                // Tests depend on collector module
                implementation(projects.core.analyticsFakes)
            }
        }
    }
}
```

### Write a Test

```kotlin
// app/src/commonTest/kotlin/com/example/app/AppTest.kt
package com.example.app

import com.example.core.analytics.Analytics
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest {
    @Test
    fun `GIVEN analytics fake WHEN tracking event THEN should capture call`() {
        val events = mutableListOf<String>()

        val analytics: Analytics = fakeAnalytics {
            track { event -> events.add(event) }
            identify { userId -> println("User: $userId") }
            sessionId { "session-123" }
        }

        analytics.track("user_login")
        analytics.track("user_signup")

        assertEquals(listOf("user_login", "user_signup"), events)
        assertEquals(2, analytics.trackCallCount.value)
    }

    @Test
    fun `GIVEN analytics fake WHEN getting session THEN returns configured value`() {
        val analytics = fakeAnalytics {
            sessionId { "my-session-id" }
        }

        val sessionId = analytics.sessionId

        assertEquals("my-session-id", sessionId)
        assertEquals(1, analytics.sessionIdCallCount.value)
    }
}
```

### Run Tests

```bash
./gradlew :app:test
```

**Expected**: All tests pass ✅

---

## Step 5: Verify the Setup

### Build Entire Project

```bash
./gradlew build
```

### Check Generated Code Locations

**Producer** (`:core:analytics`):
```
core/analytics/build/generated/fakt/
├── commonTest/kotlin/com/example/core/analytics/
│   ├── FakeAnalyticsImpl.kt
│   ├── fakeAnalytics.kt
│   └── FakeAnalyticsConfig.kt
```

**Collector** (`:core:analytics-fakes`):
```
core/analytics-fakes/build/generated/collected-fakes/
├── commonMain/kotlin/com/example/core/analytics/
│   ├── FakeAnalyticsImpl.kt
│   ├── fakeAnalytics.kt
│   └── FakeAnalyticsConfig.kt
├── jvmMain/kotlin/  (if JVM-specific fakes exist)
└── iosMain/kotlin/  (if iOS-specific fakes exist)
```

**Consumer** (`:app`):
- No generated code (uses compiled fakes from collector dependency)

### Verify IDE Autocomplete

In your test file, type `fake` and verify IDE suggests:
- `fakeAnalytics()`

If not appearing, try:
1. **File → Reload All Gradle Projects**
2. **File → Invalidate Caches → Invalidate and Restart**

---

## Complete Example: Multi-Producer Setup

Now let's extend to multiple core modules.

### Add More Producers

```bash
mkdir -p core/logger/src/commonMain/kotlin/com/example/core/logger
mkdir -p core/auth/src/commonMain/kotlin/com/example/core/auth
```

```kotlin
// core/logger/src/commonMain/kotlin/Logger.kt
@Fake
interface Logger {
    fun info(message: String)
    fun error(message: String)
}

// core/auth/src/commonMain/kotlin/AuthProvider.kt
@Fake
interface AuthProvider {
    suspend fun login(credentials: Credentials): Result<User>
}
```

### Add Corresponding Collectors

```bash
mkdir -p core/logger-fakes/src
mkdir -p core/auth-fakes/src
```

```kotlin
// core/logger-fakes/build.gradle.kts
fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.logger)
}

// core/auth-fakes/build.gradle.kts
fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.auth)
}
```

### Use in Consumer

```kotlin
// app/build.gradle.kts
kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.analyticsFakes)
        implementation(projects.core.loggerFakes)
        implementation(projects.core.authFakes)
    }
}
```

```kotlin
// app/src/commonTest/kotlin/LoginTest.kt
@Test
fun `GIVEN login flow WHEN user logs in THEN should track and log`() = runTest {
    // Compose multiple fakes
    val analytics = fakeAnalytics {
        track { event -> println("Track: $event") }
    }

    val logger = fakeLogger {
        info { message -> println("Info: $message") }
    }

    val auth = fakeAuthProvider {
        login { Result.success(User("123", "Alice")) }
    }

    val loginUseCase = LoginUseCase(auth, analytics, logger)

    val result = loginUseCase.execute(Credentials("alice", "pass"))

    assertTrue(result.isSuccess)
    assertEquals(1, analytics.trackCallCount.value)
    assertEquals(1, logger.infoCallCount.value)
}
```

---

## Configuration Patterns

### Pattern 1: Type-Safe Project Accessors (Recommended)

```kotlin
fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

**Benefits**:
- IDE autocomplete
- Compile-time safety
- Refactoring support

### Pattern 2: String-Based Paths

```kotlin
fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(project(":core:analytics"))
}
```

**Use When**:
- Type-safe accessors not available
- Dynamic module names
- Cross-project references

### Pattern 3: With Custom Logging

```kotlin
fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.DEBUG)

    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

**Log Levels**:
- `QUIET` - Zero output (CI/CD)
- `INFO` - Concise summary (default)
- `DEBUG` - Detailed breakdown
- `TRACE` - Full IR details

---

## Version Catalog Integration

Centralize versions using Gradle version catalogs:

### gradle/libs.versions.toml

```toml
[versions]
fakt = "1.0.0-SNAPSHOT"
kotlin = "2.2.20"
coroutines = "1.10.1"

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
fakt = { id = "com.rsicarelli.fakt", version.ref = "fakt" }

[libraries]
fakt-runtime = { module = "com.rsicarelli.fakt:runtime", version.ref = "fakt" }
coroutines = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
```

### Using in build.gradle.kts

```kotlin
// Producer
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

dependencies {
    commonMainImplementation(libs.fakt.runtime)
}

// Collector
kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.analytics)
        implementation(libs.coroutines)
    }
}
```

---

## Troubleshooting Quick Guide

### Issue: "No fakes found in source module"

**Cause**: Producer module has no `@Fake` interfaces or they weren't generated.

**Solution**:
1. Verify `@Fake` annotation exists:
   ```bash
   grep -r "@Fake" core/analytics/src/
   ```
2. Check fakes were generated:
   ```bash
   ls core/analytics/build/generated/fakt/
   ```
3. Rebuild producer:
   ```bash
   ./gradlew :core:analytics:clean :core:analytics:build
   ```

### Issue: "Unresolved reference: fakeAnalytics"

**Cause**: Consumer doesn't depend on collector module.

**Solution**:
1. Add dependency in consumer's `build.gradle.kts`:
   ```kotlin
   commonTestImplementation(projects.core.analyticsFakes)
   ```
2. Sync Gradle and rebuild:
   ```bash
   ./gradlew --refresh-dependencies build
   ```

### Issue: Targets Mismatch Error

**Error**: `Cannot find source set 'iosMain'`

**Cause**: Collector has different targets than producer.

**Solution**: Ensure collector has ALL producer's targets:

```kotlin
// Producer
kotlin {
    jvm()
    iosArm64()
}

// Collector MUST match
kotlin {
    jvm()
    iosArm64()
}
```

For more issues, see [Troubleshooting Guide](troubleshooting.md).

---

## Next Steps

**You've successfully set up multi-module support!** 🎉

Explore advanced topics:

- [Advanced Topics](advanced.md) - Platform detection, performance tuning, publishing
- [Troubleshooting](troubleshooting.md) - Comprehensive debugging guide
- [Migration Guide](migration.md) - Migrate existing single-module projects
- [Technical Reference](reference.md) - FakeCollectorTask internals

---

## Complete Build Configuration Example

Here's a complete working example with all three modules:

### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()  // For SNAPSHOT versions
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "my-project"

include(":core:analytics")
include(":core:analytics-fakes")
include(":app")
```

### core/analytics/build.gradle.kts (Producer)

```kotlin
plugins {
    kotlin("multiplatform") version "2.2.20"
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

kotlin {
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.INFO)
}
```

### core/analytics-fakes/build.gradle.kts (Collector)

```kotlin
plugins {
    kotlin("multiplatform") version "2.2.20"
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

kotlin {
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets.commonMain {
        dependencies {
            api(projects.core.analytics)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
        }
    }
}

fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)

    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.INFO)
}
```

### app/build.gradle.kts (Consumer)

```kotlin
plugins {
    kotlin("multiplatform") version "2.2.20"
}

kotlin {
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.analytics)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.core.analyticsFakes)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
            }
        }
    }
}
```

Copy these configurations and adjust package names to match your project!
