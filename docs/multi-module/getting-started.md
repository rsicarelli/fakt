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

```kotlin
// core/analytics/build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.20"
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

kotlin {
    jvm()
    iosArm64()

    sourceSets.commonMain.dependencies {
        implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
    }
}
```

Define `@Fake` interface:

```kotlin
// core/analytics/src/commonMain/kotlin/Analytics.kt
@Fake
interface Analytics {
    fun track(event: String)
    suspend fun identify(userId: String)
}
```

Build the module: `./gradlew :core:analytics:build`

Verify fakes generated in `build/generated/fakt/commonTest/kotlin/`

---

## Step 2: Create Collector Module

The collector module collects generated fakes and makes them available to other modules. Name it anything (`:core:analytics-fakes`, `:analytics-test`, etc.).

```kotlin
// core/analytics-fakes/build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.20"
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

kotlin {
    jvm()  // MUST match producer's targets
    iosArm64()

    sourceSets.commonMain.dependencies {
        api(projects.core.analytics)  // CRITICAL: Use api() to expose types
        implementation(libs.coroutines)  // Add dependencies used by fakes
    }
}

fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

**Key points**: Use `api()` for producer dependency, match all producer targets, declare transitive dependencies.

Build and verify: `./gradlew :core:analytics-fakes:build`

Verify fakes collected in `build/generated/collected-fakes/commonMain/kotlin/`

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

## Multi-Producer Example

For projects with multiple core modules:

**1. Create additional producers** (logger, auth, etc.) with `@Fake` interfaces

**2. Create corresponding collectors**:
```kotlin
// core/logger-fakes/build.gradle.kts
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.logger)
}
```

**3. Add all collectors as test dependencies**:
```kotlin
// app/build.gradle.kts
commonTest.dependencies {
    implementation(projects.core.analyticsFakes)
    implementation(projects.core.loggerFakes)
    implementation(projects.core.authFakes)
}
```

**4. Compose multiple fakes in tests**:
```kotlin
@Test
fun `test using multiple fakes`() = runTest {
    val analytics = fakeAnalytics { track { event -> /* ... */ } }
    val logger = fakeLogger { info { msg -> /* ... */ } }
    val auth = fakeAuthProvider { login { Result.success(User("123")) } }

    // Test your use case with composed fakes
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

---

## Next Steps

**You've successfully set up multi-module support!** 🎉

For troubleshooting, see [Troubleshooting Guide](../troubleshooting.md). Explore advanced topics:

- [Advanced Topics](advanced.md) - Platform detection, performance tuning, publishing, and API reference
- [Migration Guide](migration.md) - Migrate existing single-module projects
