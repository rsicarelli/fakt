# Getting Started

Install the Fakt plugin, annotate an interface with `@Fake`, and start using type-safe fakes in your tests.

---

## Prerequisites

| Requirement      | Version    |
|------------------|------------|
| **Kotlin**       | 2.2.21+    |
| **Gradle**       | 8.0+       |
| **JVM**          | 11+        |

---

## Installation

### Multiplatform Projects

**Version Catalog (`gradle/libs.versions.toml`):**

```toml
[versions]
fakt = "1.0.0-alpha01"
kotlin = "2.2.21"

[plugins]
fakt = { id = "com.rsicarelli.fakt", version.ref = "fakt" }

[libraries]
fakt-annotations = { module = "com.rsicarelli.fakt:annotations", version.ref = "fakt" }
```

**Root `build.gradle.kts`:**

```kotlin
plugins {
    // ... your plugins
    alias(libs.plugins.fakt) apply false
}
```

**Module `build.gradle.kts`:**

```kotlin
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.fakt)
}

kotlin {
    // Your KMP targets
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.fakt.annotations)
        }
    }
}
```

**…and that's it!**

---

### Single-Platform Projects

Fakt works with single-platform Kotlin projects too:

**JVM-Only:**

```kotlin
plugins {
    kotlin("jvm")
    alias(libs.plugins.fakt)
}

dependencies {
    implementation(libs.fakt.annotations)
}
```

**💡 See working example:** [`samples/jvm-single-module`](https://github.com/rsicarelli/fakt/tree/main/samples/jvm-single-module)

**Android-Only:**

```kotlin
plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.fakt)
}

dependencies {
    implementation(libs.fakt.annotations)
}
```

**💡 See working example:** [`samples/android-single-module`](https://github.com/rsicarelli/fakt/tree/main/samples/android-single-module)

---

## Your First Fake

### Step 1: Annotate an Interface

Create an interface and mark it with `@Fake`:

```kotlin
// src/commonMain/kotlin/com/example/Analytics.kt
package com.example

import com.rsicarelli.fakt.Fake

@Fake
interface Analytics {
    fun track(event: String)
}
```

---

### Step 2: Build Your Project

Run Gradle build to generate the fake:

```bash
./gradlew build
```

Fakt generates `FakeAnalyticsImpl` in `build/generated/fakt/commonTest/kotlin/com/example/`.

!!! tip "Compilation Trigger"
    Fakt hooks into Kotlin compilation tasks. Running any compile task
    (`:compileKotlin`, `:build`, or IDE build/compile/assemble) triggers
    Fakt generation automatically.

---

### Step 3: Use in Tests

The generated fake includes a factory function and DSL:

```kotlin
// src/commonTest/kotlin/com/example/AnalyticsTest.kt
package com.example

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsTest {
    @Test
    fun `GIVEN configured fake WHEN tracking event THEN captures call`() {
        val events = mutableListOf<String>()

        val fake = fakeAnalytics {
            track { event -> events.add(event) }
        }

        fake.track("user_signup")

        assertEquals(listOf("user_signup"), events)
        assertEquals(1, fake.trackCallCount.value)
    }
}
```

---

## Understanding Generated Code

Fakt generates three components for each `@Fake` interface:

### 1. Implementation Class

```kotlin
class FakeAnalyticsImpl : Analytics {
    private var trackBehavior: (String) -> Unit = { }
    private val _trackCallCount = MutableStateFlow(0)
    val trackCallCount: StateFlow<Int> get() = _trackCallCount

    override fun track(event: String) {
        _trackCallCount.update { it + 1 }
        trackBehavior(event)
    }

    internal fun configureTrack(behavior: (String) -> Unit) {
        trackBehavior = behavior
    }
}
```

### 2. Factory Function

```kotlin
fun fakeAnalytics(
    configure: FakeAnalyticsConfig.() -> Unit = {}
): FakeAnalyticsImpl = FakeAnalyticsImpl().apply {
    FakeAnalyticsConfig(this).configure()
}
```

### 3. Configuration DSL

```kotlin
class FakeAnalyticsConfig(private val fake: FakeAnalyticsImpl) {
    fun track(behavior: (String) -> Unit) {
        fake.configureTrack(behavior)
    }
}
```

---

## Generated Code Location

Fakt generates code in the corresponding **test source set** of the module where the `@Fake` annotated interface lives. The generated package matches the original interface's package.

| Target        | Annotated In         | Generated Output                               |
|---------------|----------------------|------------------------------------------------|
| KMP Common    | `commonMain/`        | `build/generated/fakt/commonTest/kotlin/`      |
| JVM           | `jvmMain/`           | `build/generated/fakt/jvmTest/kotlin/`         |
| iOS           | `iosMain/`           | `build/generated/fakt/iosTest/kotlin/`         |
| Android       | `androidMain/`       | `build/generated/fakt/androidUnitTest/kotlin/` |
| JVM-only      | `main/` (src/main)   | `build/generated/fakt/test/kotlin/`            |

!!! note
    Generated code **never** appears in production builds. Fakt is test-only.

---

## Multi-Module Setup

For projects with multiple Gradle modules that need to share fakes, Fakt provides a producer-collector-consumer pattern:

```
producer (:core:analytics)        → Contains @Fake interfaces
     ↓
collector (:core:analytics-fakes) → Collects and exposes fakes
     ↓
consumer (:app, :features:*)      → Uses fakes in tests
```

**[Full multi-module setup guide →](../user-guide/multi-module.md)**

---

## IDE Support

Fakt-generated code appears in `build/generated/fakt/` and is automatically indexed by IntelliJ IDEA and Android Studio.

!!! tip "K2 IDE Mode"
    Enable K2 mode for better autocomplete of generated factories:

    **Settings → Languages & Frameworks → Kotlin → Enable K2 mode**

---

## Logging & Debugging

Fakt provides configurable logging to help troubleshoot code generation:

```kotlin
// build.gradle.kts
import com.rsicarelli.fakt.compiler.api.LogLevel

fakt {
    logLevel.set(LogLevel.DEBUG)  // Options: QUIET, INFO, DEBUG
}
```

**Example DEBUG output:**

```
Registering FIR extension
Registering IR extension with FIR metadata access
Built IR class map with 149 classes
FIR→IR Transformation (interfaces: 101/101, took 1ms)
FIR + IR trace
├─ Total FIR time: 6ms
├─ Total IR time: 58ms
│  ├─ FIR analysis: 1 type parameters, 6 members (55µs)
│  └─ IR generation: FakeAnalyticsImpl 83 LOC (766µs)
```

**[Full configuration reference →](../user-guide/plugin-configuration.md)**

---

## Next Steps

- **[Features](features.md)** - Complete feature reference
- **[Usage Guide](../user-guide/usage.md)** - Common patterns and examples
- **[Testing Patterns](../user-guide/testing-patterns.md)** - Best practices
- **[Multi-Module Setup](../user-guide/multi-module.md)** - Share fakes across Gradle modules
- **[Configuration](../user-guide/plugin-configuration.md)** - Log levels and plugin options
