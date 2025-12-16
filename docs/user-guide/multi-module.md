# Multi-Module Support

Fakt's multi-module support enables fake reuse across multiple Gradle modules through dedicated collector modules.

!!! info "Experimental API"
    Multi-module support is marked `@ExperimentalFaktMultiModule`. The API is production-ready but may evolve based on real-world feedback. Explicit opt-in is required.

---

## What is Multi-Module Support?

Multi-module support allows you to:

- **Generate fakes once** in a producer module with `@Fake` interfaces
- **Collect fakes** in a dedicated collector module
- **Use fakes** across multiple consumer modules in tests

This eliminates fake duplication and enables clean dependency management in large projects.

---

## Architecture: Producer → Collector → Consumer

Fakt's multi-module pattern uses three distinct roles:

```
┌─────────────────────────────────────────────────────────────────┐
│  PRODUCER MODULE (:core:analytics)                              │
│  ════════════════════════════════════════════════════════       │
│  • Contains @Fake annotated interfaces                          │
│  • Fakt generates fakes at compile-time                         │
│  • Output: build/generated/fakt/commonTest/kotlin/              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  COLLECTOR MODULE (:core:analytics-fakes) †                     │
│  ════════════════════════════════════════════════════════       │
│  • Collects generated fakes from producer                       │
│  • FakeCollectorTask copies fakes with platform detection       │
│  • Output: build/generated/collected-fakes/{platform}/kotlin/   │
│  • Published as standard Gradle dependency                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  CONSUMER MODULES (:app, :features:login, etc.)                 │
│  ════════════════════════════════════════════════════════       │
│  • Depend on collector module in tests                          │
│  • Use fakes via generated factory functions                    │
│  • No direct dependency on producer's test code                 │
└─────────────────────────────────────────────────────────────────┘

† Naming is flexible - can be :analytics-fakes, :analytics-test,
  :analytics-test-fixtures, or any name you choose
```

---

## When to Use Multi-Module?

### ✅ Use Multi-Module When

- **Multiple modules need the same fakes** (e.g., `core/logger` used by 10+ feature modules)
- **Publishing fakes as artifacts** (Maven Central, internal repository)
- **Strict module boundaries** (DDD, Clean Architecture, modular monoliths)
- **Large teams with module ownership** (dedicated teams per module)
- **Shared test infrastructure** (common fakes for integration tests)

### ❌ Use Single-Module When

- **Single module or 2-3 closely related modules**
- **Fakes only used locally** (not shared across modules)
- **Small team or early prototyping** (prefer simplicity)
- **Rapid iteration** (multi-module adds slight build overhead)

---

## Setup

### Prerequisites

Before starting, ensure you have:

- ✅ Kotlin Multiplatform or JVM project with multiple Gradle modules
- ✅ Fakt plugin installed (see [Getting Started](../get-started/index.md))
- ✅ Basic understanding of Gradle module structure
- ✅ Type-safe project accessors enabled in `settings.gradle.kts`

!!! tip "Type-Safe Project Accessors"
    If you don't have type-safe accessors enabled, add to `settings.gradle.kts`:
    ```kotlin
    enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
    ```
    Then sync Gradle to generate `projects.*` accessors.

---

### Tutorial Overview

We'll create a simple multi-module setup:

```
my-project/
├── core/analytics/           # Producer (defines @Fake interfaces)
├── core/analytics-fakes/     # Collector (collects generated fakes)
└── app/                      # Consumer (uses fakes in tests)
```

**Time**: ~15 minutes

---

### Step 1: Create Producer Module

The producer module contains `@Fake` annotated interfaces.

```kotlin
// core/analytics/build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.21"
    id("com.rsicarelli.fakt") version "1.0.0-alpha01"
}

kotlin {
    jvm()
    iosArm64()

    sourceSets.commonMain.dependencies {
        implementation("com.rsicarelli.fakt:annotations:1.0.0-alpha01")
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

### Step 2: Create Collector Module

The collector module collects generated fakes and makes them available to other modules. Name it anything (`:core:analytics-fakes`, `:analytics-test`, etc.).

```kotlin
// core/analytics-fakes/build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.21"
    id("com.rsicarelli.fakt") version "1.0.0-alpha01"
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

!!! tip "Naming Flexibility"
    The collector module can be named anything you prefer:

    - `:core:analytics-fakes` ✅ (recommended convention)
    - `:core:analytics-test` ✅
    - `:core:analytics-test-fixtures` ✅
    - `:test:analytics` ✅
    - `:testFixtures:analytics` ✅

    Fakt doesn't impose any naming convention. Choose what fits your project best.

---

### Step 3: Register Modules in settings.gradle.kts

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

### Step 4: Use Fakes in Consumer Module

Now use the collected fakes in your app or feature modules.

#### Configure app/build.gradle.kts

```kotlin
// app/build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.21"
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

#### Write a Test

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
        }

        analytics.track("user_login")
        analytics.track("user_signup")

        assertEquals(listOf("user_login", "user_signup"), events)
        assertEquals(2, analytics.trackCallCount.value)
    }

    @Test
    fun `GIVEN analytics fake WHEN identify THEN should call suspend function`() = runTest {
        val analytics = fakeAnalytics {
            identify { userId -> println("User: $userId") }
        }

        analytics.identify("user-123")

        assertEquals(1, analytics.identifyCallCount.value)
    }
}
```

#### Run Tests

```bash
./gradlew :app:test
```

**Expected**: All tests pass ✅

---

### Step 5: Verify the Setup

#### Build Entire Project

```bash
./gradlew build
```

#### Check Generated Code Locations

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

#### Verify IDE Autocomplete

In your test file, type `fake` and verify IDE suggests:
- `fakeAnalytics()`

If not appearing, try:
1. **File → Reload All Gradle Projects**
2. **File → Invalidate Caches → Invalidate and Restart**

---

### Multi-Producer Example

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

### Configuration Patterns

#### Pattern 1: Type-Safe Project Accessors (Recommended)

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

#### Pattern 2: String-Based Paths

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

## Implementation Details

### How It Works

Fakt's multi-module flow follows three phases:

**1. Producer generates fakes** at compile-time in test source sets (`build/generated/fakt/commonTest/`)

**2. Collector copies fakes** using `FakeCollectorTask`:
- Discovers generated fakes from producer
- Analyzes package structure to detect target platform (e.g., `com.example.jvm.*` → `jvmMain/`)
- Copies fakes to collector's source sets (`build/generated/collected-fakes/commonMain/`)
- Registers as source roots for compilation

**3. Consumer uses fakes** as standard dependencies:
```kotlin
dependencies {
    commonTestImplementation(projects.core.analyticsFakes)
}
```

The collector exposes both original interfaces (via `api()`) and compiled fakes.

---

### Real-World Patterns

This producer-collector-consumer pattern is used in production apps and architectural patterns:

- Multi-module Android apps like [Now in Android (NIA)](https://github.com/android/nowinandroid)
- Clean Architecture projects with strict layer boundaries
- Domain-Driven Design (DDD) module structures

The pattern enables teams to maintain clear module boundaries while sharing test infrastructure efficiently.

---

### Key Benefits

- **Fake Reuse**: Generate fakes once in producer, use across multiple consumer modules
- **Clean Dependencies**: Standard Gradle dependencies (`implementation(projects.core.analyticsFakes)`)
- **Publishable Artifacts**: Collectors are normal modules that can be published to Maven Central or internal repos
- **Platform Awareness**: Automatic platform detection places fakes in correct KMP source sets (jvmMain, iosMain, commonMain)
- **Type Safety**: Compile-time errors if interfaces change, preventing broken tests

---

## Next Steps

**You've successfully set up multi-module support!** 🎉

**Learn More**:

- **[Troubleshooting](../help/troubleshooting.md)** - Common issues & solutions
- **[Examples](../examples/index.md#kmp-multi-module)** - Production-quality kmp-multi-module example (11 modules)
- **[Plugin Configuration](plugin-configuration.md)** - Advanced configuration options
- **[Performance & Optimization](performance.md)** - Build performance tuning
