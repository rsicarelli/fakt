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

## Quick Example

### Producer Module

Define `@Fake` interfaces in your core module:

```kotlin
// :core:analytics/src/commonMain/kotlin/Analytics.kt
package com.example.core.analytics

import com.rsicarelli.fakt.Fake

@Fake
interface Analytics {
    fun track(event: String)
    suspend fun identify(userId: String)
}
```

Build the module—Fakt generates fakes in `build/generated/fakt/commonTest/kotlin/`.

### Collector Module

Create a dedicated module to collect the generated fakes:

```kotlin
// :core:analytics-fakes/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

kotlin {
    // Configure your KMP targets (jvm, ios, etc.)

    sourceSets.commonMain.dependencies {
        // CRITICAL: api() exposes original types to consumers
        api(projects.core.analytics)

        // Add dependencies needed by generated fakes
        implementation(libs.coroutines)
    }
}

fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

!!! tip "Naming Flexibility"
    The collector module can be named anything you prefer:

    - `:core:analytics-fakes` ✅ (recommended convention)
    - `:core:analytics-test` ✅
    - `:core:analytics-test-fixtures` ✅
    - `:test:analytics` ✅
    - `:testFixtures:analytics` ✅

    Fakt doesn't impose any naming convention. Choose what fits your project best.

### Consumer Module

Use the collected fakes in your tests:

```kotlin
// :app/build.gradle.kts
kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.analyticsFakes)
    }
}
```

```kotlin
// :app/src/commonTest/kotlin/AppTest.kt
@Test
fun `GIVEN app WHEN tracking event THEN should log event`() {
    val events = mutableListOf<String>()
    val analytics = fakeAnalytics { track { event -> events.add(event) } }

    analytics.track("user_login")

    assertEquals(listOf("user_login"), events)
    assertEquals(1, analytics.trackCallCount.value)
}
```

---

## How It Works

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

See [Advanced Topics](multi-module-advanced.md#platform-detection) for platform detection algorithm and [Advanced Topics](multi-module-advanced.md#fakecollectortask-api) for task details.

---

## Key Benefits

- **Fake Reuse**: Generate fakes once in producer, use across multiple consumer modules
- **Clean Dependencies**: Standard Gradle dependencies (`implementation(projects.core.analyticsFakes)`)
- **Publishable Artifacts**: Collectors are normal modules that can be published to Maven Central or internal repos
- **Platform Awareness**: Automatic platform detection places fakes in correct KMP source sets (jvmMain, iosMain, commonMain)
- **Type Safety**: Compile-time errors if interfaces change, preventing broken tests

---

## Getting Started

Ready to set up multi-module support? Follow the [Getting Started Guide](multi-module-setup.md) for a step-by-step tutorial.

**Learn More**:

- **[Getting Started](multi-module-setup.md)** - 15-minute step-by-step tutorial
- **[Advanced Topics](multi-module-advanced.md)** - Platform detection, performance, publishing, and API reference
- **[Samples](../examples/index.md#kmp-multi-module)** - Production-quality kmp-multi-module example (11 modules)
- **[Troubleshooting](../help/troubleshooting.md)** - Common issues & solutions
- **[Migration Guide](../help/multi-module-migration.md)** - Single-module → Multi-module migration
