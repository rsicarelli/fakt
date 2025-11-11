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

### Decision Example

**Scenario**: E-commerce app with 20 modules (5 core, 15 features)

- **Core modules**: `analytics`, `auth`, `logger`, `network`, `storage`
- **Feature modules**: `login`, `checkout`, `profile`, `search`, etc.

**Analysis**:
- All 15 features depend on `core/logger` and `core/analytics`
- Features share authentication via `core/auth`

**Decision**: **Use multi-module**
- Create 5 collector modules (one per core module)
- Features depend on collectors in tests
- Benefits: Fake reuse, clean dependencies, single source of truth

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
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest {
    @Test
    fun `GIVEN app WHEN tracking event THEN should log event`() {
        val events = mutableListOf<String>()

        val analytics = fakeAnalytics {
            track { event -> events.add(event) }
        }

        // Use analytics in your test...
        analytics.track("user_login")

        assertEquals(listOf("user_login"), events)
        assertEquals(1, analytics.trackCallCount.value)
    }
}
```

---

## How It Works Under the Hood

### 1. Producer: Fake Generation

When you build the producer module:

```bash
./gradlew :core:analytics:build
```

Fakt's IR generator creates fakes in test source sets:

```
core/analytics/build/generated/fakt/
├── commonTest/kotlin/com/example/core/analytics/
│   ├── FakeAnalyticsImpl.kt
│   ├── fakeAnalytics.kt (factory)
│   └── FakeAnalyticsConfig.kt (DSL)
├── jvmTest/kotlin/  (if JVM target)
└── iosTest/kotlin/  (if iOS target)
```

### 2. Collector: Intelligent Fake Collection

When you build the collector module:

```bash
./gradlew :core:analytics-fakes:build
```

`FakeCollectorTask` runs and:

1. **Discovers** all generated fakes from producer
2. **Analyzes** package structure to detect target platform
3. **Copies** fakes to appropriate source set directory
4. **Registers** as source roots for compilation

**Platform Detection Example**:

```kotlin
// Fake with package: com.example.jvm.database
// → Placed in: jvmMain/kotlin/

// Fake with package: com.example.ios.camera
// → Placed in: iosMain/kotlin/

// Fake with package: com.example.shared.network
// → Placed in: commonMain/kotlin/ (fallback)
```

See [Advanced Topics](advanced.md#platform-detection) for algorithm details.

### 3. Consumer: Standard Dependency

Consumers declare standard Gradle dependencies:

```kotlin
dependencies {
    commonTestImplementation(projects.core.analyticsFakes)
}
```

The collector module exposes:
- Original interfaces (via `api(projects.core.analytics)`)
- Generated fakes (compiled from collected sources)

---

## Real-World Example: kmp-multi-module Sample

Fakt includes a production-quality [sample](../samples/index.md#kmp-multi-module) with:

- **11 producer modules** (5 core + 6 features)
- **11 collector modules** (one per producer)
- **1 consumer module** (app using all fakes)

**Structure**:

```
samples/kmp-multi-module/
├── core/
│   ├── analytics/ → analytics-fakes/
│   ├── auth/ → auth-fakes/
│   ├── logger/ → logger-fakes/
│   ├── network/ → network-fakes/
│   └── storage/ → storage-fakes/
│
├── features/
│   ├── dashboard/ → dashboard-fakes/
│   ├── login/ → login-fakes/
│   ├── notifications/ → notifications-fakes/
│   ├── order/ → order-fakes/
│   ├── profile/ → profile-fakes/
│   └── settings/ → settings-fakes/
│
└── app/ (uses all 11 fake modules)
```

**Test Example** (composing multiple fakes):

```kotlin
// features/login/src/commonTest/kotlin/LoginUseCaseTest.kt
@Test
fun `GIVEN login use case WHEN login succeeds THEN should track event`() = runTest {
    // Arrange: Configure 4 different fakes from 4 collector modules
    val authProvider = fakeAuthProvider {
        login { Result.success(User("123", "Alice")) }
    }

    val logger = fakeLogger {
        info { message -> println("LOG: $message") }
    }

    val storage = fakeTokenStorage {
        save { token -> Result.success(Unit) }
    }

    val analytics = fakeAnalytics {
        track { event -> println("EVENT: $event") }
    }

    val useCase = LoginUseCase(authProvider, logger, storage, analytics)

    // Act
    val result = useCase.login(Credentials("alice", "password"))

    // Assert
    assertTrue(result.isSuccess)
    assertEquals(1, analytics.trackCallCount.value)
}
```

---

## Key Benefits

### ✅ Fake Reuse

Generate fakes once, use everywhere:

```kotlin
// Core module defines interface
core/logger @Fake interface Logger

// 15 feature modules reuse the same fake
features/login     → depends on logger-fakes
features/checkout  → depends on logger-fakes
features/profile   → depends on logger-fakes
// ... 12 more features
```

### ✅ Clean Dependencies

Consumers depend on collector modules, not producer test code:

```kotlin
// ❌ BAD: Direct dependency on producer's test source set
testImplementation(projects.core.analytics) {
    capabilities {
        requireCapability("com.example:analytics-test-fixtures")
    }
}

// ✅ GOOD: Standard dependency on collector module
testImplementation(projects.core.analyticsFakes)
```

### ✅ Publishable Artifacts

Collectors are standard Gradle modules and can be published:

```bash
./gradlew :core:analytics-fakes:publishToMavenLocal
./gradlew :core:analytics-fakes:publish
```

Useful for:
- Internal artifact repositories
- Shared test infrastructure across projects
- Multi-repo setups

### ✅ Platform Awareness

Fakt automatically places fakes in the correct KMP source set:

- JVM-specific fakes → `jvmMain/kotlin/`
- iOS-specific fakes → `iosMain/kotlin/`
- Shared fakes → `commonMain/kotlin/`

No manual configuration required.

---

## Getting Started

Ready to set up multi-module support? Follow the [Getting Started Guide](getting-started.md) for a step-by-step tutorial.

**Next Steps**:

- [Getting Started](getting-started.md) - 15-minute tutorial
- [Advanced Topics](advanced.md) - Platform detection, performance, publishing
- [Troubleshooting](troubleshooting.md) - Common issues & solutions
- [Migration Guide](migration.md) - Single-module → Multi-module
- [Technical Reference](reference.md) - FakeCollectorTask deep dive

---

## Comparison with Single-Module

| Aspect | Single-Module | Multi-Module |
|--------|---------------|--------------|
| **Setup** | Zero config (default) | Requires collector modules |
| **Fake Access** | Local to module only | Cross-module reuse |
| **Dependencies** | No extra dependencies | Collector module dependencies |
| **Build Time** | Fast (no collection overhead) | Slight overhead (collection task) |
| **Publishable** | No (test code not published) | Yes (collectors are modules) |
| **Best For** | 1-3 modules, rapid prototyping | Large projects, shared infrastructure |

---

## Requirements

- **Fakt**: 1.0.0-SNAPSHOT+
- **Kotlin**: 2.2.10+
- **Gradle**: 8.0+
- **Multi-Module Project**: Producer + Collector + Consumer setup

---

## Frequently Asked Questions

### Can I mix single-module and multi-module?

**Yes**. Some modules can use single-module (fakes stay local), while others use multi-module (fakes collected and shared). Choose per module based on needs.

### Do I need a collector for every producer?

**No**. Only create collectors for modules whose fakes you want to share. If a module's fakes are only used locally, skip the collector.

### What if I don't follow the `-fakes` naming convention?

**No problem**. Fakt doesn't enforce any naming convention. Name your collector modules however you prefer:

- `:core:analytics-fakes` (recommended convention)
- `:core:analytics-test`
- `:testFixtures:analytics`
- `:test:analytics`
- Anything else

### Can I publish collectors to Maven Central?

**Yes**. Collectors are standard Gradle modules and can be published like any other artifact. Useful for shared test infrastructure.

### Does multi-module work with Android?

**Yes**. Fully supported for Android projects (single-platform or KMP).

### What's the build time impact?

Minimal. First target compilation adds ~5-10ms per fake module for collection. Subsequent targets are cached (~1-2ms). See [Performance](advanced.md#performance-optimization) for details.

---

## Next Steps

- [Getting Started](getting-started.md) - Set up your first multi-module project
- [Advanced Topics](advanced.md) - Deep technical details
- [Troubleshooting](troubleshooting.md) - Debug common issues
