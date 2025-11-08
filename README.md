# Fakt

**Type-safe test fake generation for Kotlin Multiplatform via FIR/IR compiler plugin**

[![Build](https://img.shields.io/github/actions/workflow/status/rsicarelli/fakt/ci.yml)](https://github.com/rsicarelli/fakt/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.rsicarelli.fakt/runtime)](https://search.maven.org/search?q=g:com.rsicarelli.fakt)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10%2B-blue)](https://kotlinlang.org)

---

## What Is Fakt?

A **Kotlin compiler plugin** that generates type-safe test fakes at compile-time. It uses a two-phase **FIR → IR** compilation architecture to analyze `@Fake` annotated interfaces and classes, then generates production-quality fake implementations with expressive configuration DSLs.

```kotlin
import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService {
    fun track(event: String)
}
```

### Usage in Tests

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsServiceTest {
    @Test
    fun `GIVEN AnalyticsService fake WHEN configuring track THEN should execute behavior`() {
        var capturedEvent: String? = null
        val fake = fakeAnalyticsService {
            track { event -> capturedEvent = event }
        }

        fake.track("user_clicked_button")

        assertEquals("user_clicked_button", capturedEvent)
        assertEquals(1, fake.trackCallCount.value)
    }
}
```

---

## Quick Start

### 1. Apply the Gradle Plugin

```kotlin
// root build.gradle.kts
plugins {
  	id("org.jetbrains.kotlin.multiplatform") version "x.y.z" apply false
	  id("com.rsicarelli.fakt") version "x.y.z" apply false
}
```

### 2. Add runtime dependency

```kotlin
// module build.gradle.kts
plugins {
  	id("org.jetbrains.kotlin.multiplatform")
	  id("com.rsicarelli.fakt")
}

kotlin {
		sourceSets { 
        commonMain.dependencies { 
						implementation("com.rsicarelli.fakt:runtime:x.y.z")
        }
    }
}
```

### 3. Annotate Your Interfaces

```kotlin
// src/commonMain/kotlin/com/example/MyService.kt
import com.rsicarelli.fakt.Fake

@Fake
interface MyService {
    suspend fun getData(id: String): Result<Data>
}
```

### 4. Build your module

```kotlin
./gradlew module:build
```

### 5. Use Generated Fakes in Tests

```kotlin
// src/commonTest/kotlin/com/example/MyServiceTest.kt
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MyServiceTest {
    @Test
    fun testService() = runTest {
        val fake = fakeMyService {
            getData { id -> Result.success(Data(id, "test")) }
        }

        val result = fake.getData("123")
        // assertions...
    }
}
```

✨**That's it!** Fakt generates:
- `FakeMyServiceImpl` class
- `fakeMyService {}` factory function
- `FakeMyServiceConfig` DSL for configuration

---

## Requirements

- **Kotlin:** 2.2.20+
- **Gradle:** 8.0+
- **JVM:** 11+ (for compiler plugin execution)

---

## Features

**At a Glance:**
- ✅ **All class types** - Interfaces, abstract classes, open/final classes, companion objects
- ✅ **Complete type system** - Full generics support (class-level, method-level, constraints, variance), SAM interfaces, complex stdlib types
- ✅ **Kotlin features** - Suspend functions, properties (val/var), methods, default parameters, inheritance
- ✅ **Call tracking** - Automatic call counting per method/property via StateFlow (thread-safe, reactive)
- ✅ **All KMP platforms** - Android, iOS, JVM, Native (macOS, Linux, Windows), JavaScript, WebAssembly
- ⚠️ **Multi-module** - Experimental (requires dedicated `-fakes` modules)

**Not Supported:**
- 🔜 **Data classes** - Cannot fake directly (work fine as parameter/return types)
- ❌ **Sealed hierarchies** - Cannot fake `sealed class`/`sealed interface` (work fine as parameter/return types)
- ❌ **Default parameters in interfaces** - `fun example(parameter: String = "")` not supported

**Limitations:**

- ❌ **Generated code formatting** - Uses its own code style. Exclude `build/generated/fakt/` from linters.

---

## Platform Support

Fakt works at the **IR (Intermediate Representation) level**, which means it supports all Kotlin Multiplatform targets:

| Platform | Targets |
|----------|---------|
| **Android** | `androidTarget()` |
| **iOS** | `iosArm64()`, `iosX64()`, `iosSimulatorArm64()` |
| **JVM** | `jvm()` |
| **JavaScript** | `js(IR)` - Browser & Node.js |
| **WebAssembly** | `wasmJs()` |
| **macOS** | `macosArm64()`, `macosX64()` |
| **Linux** | `linuxArm64()`, `linuxX64()` |
| **Windows** | `mingwX64()` |
| **watchOS** | `watchosArm64()`, `watchosX64()`, `watchosSimulatorArm64()` |
| **tvOS** | `tvosArm64()`, `tvosX64()`, `tvosSimulatorArm64()` |

**Single-Platform Projects:**

- ✅ JVM-only Kotlin projects
- ✅ Android-only projects
- ✅ Any single-target KMP project

---

### Multi-Module Support (Experimental)

Fakt follows the community pattern of dedicated test modules (e.g., `-fakes`, `-test-fixtures`). The plugin collects generated fakes from a source module and exposes them through a separate Gradle module that other modules can depend on.

```kotlin
// Producer module: :core:analytics
@Fake interface Analytics

// Dedicated module: :core:analytics-fakes/build.gradle.kts
plugins {
    id("com.rsicarelli.fakt")
}

fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(project(":core:analytics"))
}

// Consumer module: :app/build.gradle.kts
dependencies {
    commonTest {
        implementation(projects.core.analyticsFakes)  // Access fakes
    }
}
```

---

## Source Set Support

Fakt generates fakes in the test source set corresponding to where `@Fake` is defined:

```kotlin
// Source src/androidMain/kotlin/MyService.kt
@Fake interface AndroidService

// Generated fake build/generated/fakt/androidUnitTest/kotlin/
fun fakeAndroidService(...)

// Source src/iosMain/kotlin/MyService.kt
@Fake interface IOSService

// Generated fake build/generated/fakt/iosTest/kotlin/
fun fakeIOSService(...)
```

Supports all KMP source sets (commonMain, jvmMain, androidMain, iosMain, macosMain, linuxMain, etc.)

> [!NOTE]
> Fully tested with Default Hierarchy Template. Please file a bug if you have issues with custom hierarchy templates!

--------

## Compilation Performance

Fakt generates code directly from **Kotlin's IR** without reflection or third-party libraries like KotlinPoet. This approach ensures maximum performance and zero runtime overhead.

**Intelligent caching** minimizes compilation overhead across multiple KMP targets:

**First compilation**: fakes are generated in the first `compileX` Gradle task

```
DISCOVERY: 1ms (100 interfaces, 21 classes)
GENERATION: 39ms (121 new fakes, avg 333µs/fake, 3,867 LOC)
TOTAL: 40ms
```

**Cached compilations** (all other targets):

```
iosArm64:					 1ms (121 from cache)
android:           1ms (121 from cache)
...
```

> [!TIP]
> Enable `LogLevel.TRACE` or `LogLevel.DEBUG` to see detailed performance metrics in your builds.

---

## Configuration

### Logging & Telemetry

Fakt includes a professional telemetry system with 4 verbosity levels:

```kotlin
// build.gradle.kts
import com.rsicarelli.fakt.compiler.api.LogLevel

fakt {
  	enabled.set(true) // Default
    logLevel.set(LogLevel.INFO)  // Default - concise summary
  	collectFakesFrom(...) // Experimental opt-in required
}
```

**Log Levels:**

- **`INFO`** (default) - Concise summary:

  ```
  > Task :compileKotlinIosArm64
  i: Fakt: ✅ 121 fakes (121 new) | 40ms
  
  > Task :compileKotlinLinuxX64
  i: Fakt: ✅ 121 fakes (121 cached) | 959µs
  ```

- **`QUIET`** - Zero output (CI/CD builds or minimal setup)
- **`DEBUG`** - Detailed breakdown per interface
- **`TRACE`** - Full IR details, type resolution, debugging info (have a small overhead in Compiler Plugin)

---

## Contributing

Contributions welcome! Please:
1. Follow GIVEN-WHEN-THEN testing standard
2. Ensure all generated code compiles
3. Test both single-platform and KMP scenarios
4. Run `make format` before committing

---

## License

```
Copyright (C) 2025 Rodrigo Sicarelli

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
