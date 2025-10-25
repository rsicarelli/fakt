# Fakt

**Type-safe test fake generation for Kotlin Multiplatform via FIR/IR compiler plugin**

[![Build](https://img.shields.io/github/actions/workflow/status/rsicarelli/fakt/ci.yml)](https://github.com/rsicarelli/fakt/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.rsicarelli.fakt/runtime)](https://search.maven.org/search?q=g:com.rsicarelli.fakt)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10%2B-blue)](https://kotlinlang.org)

---

## What Is Fakt?

Fakt is a **Kotlin compiler plugin** that generates type-safe test fakes at compile-time. It uses a two-phase **FIR → IR** compilation architecture to analyze `@Fake` annotated interfaces and classes, then generates production-quality fake implementations with expressive configuration DSLs.

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

## Features

**At a Glance:**
- ✅ **All class types** - Interfaces, abstract classes, open/final classes, companion objects
- ✅ **Complete type system** - Full generics support (class-level, method-level, constraints, variance), SAM interfaces, complex stdlib types
- ✅ **Kotlin features** - Suspend functions, properties (val/var), methods, default parameters, inheritance
- ✅ **All KMP platforms** - Android, iOS, JVM, Native (macOS, Linux, Windows), JavaScript, WebAssembly
- ⚠️ **Multi-module** - Experimental (requires dedicated `-fakes` modules)

<details>
<summary><b>📋 Complete Feature Breakdown</b></summary>

### What You Can Fake

- ✅ **Interfaces** - Primary target for fake generation
- ✅ **Abstract Classes** - Supported with proper inheritance
- ✅ **Open Classes** - Generate fakes for extensible classes
- ✅ **Final Classes** - Works with sealed implementation classes
- ✅ **Companion Objects** - Static-like members supported

### Type System Support

**Generics:**
- Basic generics (`interface Repository<T>`)
- Method-level generics (`fun <T> process(item: T): T`)
- Class-level generics (`interface Cache<K, V>`)
- Generic constraints (`<T : Comparable<T>>`)
- Variance (`in`, `out`, invariant)

**Function Types:**
- SAM interfaces (Single Abstract Method)
- Higher-order functions
- Lambda expressions

**Standard Library Types:**
- `Result<T>`, `Pair<A, B>`, `Triple<A, B, C>`
- `Sequence<T>`, `Lazy<T>`
- Nullable types (`T?`)
- Collections (`List`, `Set`, `Map`)

**Complex Types (as parameters/return types):**
- Sealed classes
- Enums (including rich enums with properties/methods)
- Data classes

### Kotlin Language Features

- ✅ **Suspend Functions** - Full coroutine support
- ✅ **Properties** (`val`/`var`) - Configurable getters/setters
- ✅ **Methods** - Instance and extension methods
- ✅ **Default Parameters** - Preserved in generated fakes
- ✅ **Inheritance** - Multi-level inheritance hierarchies
- ✅ **Meta-Annotations** - Custom annotations via `@GeneratesFake`

### Platform Support

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

</details>

---

### Multi-Module Support (Experimental)

⚠️ **Currently requires dedicated `-fakes` modules:**

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

**Future**: Custom source sets approach planned (no dedicated modules required).

---

## Configuration

### Logging & Telemetry

Fakt includes a professional telemetry system with 4 verbosity levels:

```kotlin
// build.gradle.kts
import com.rsicarelli.fakt.compiler.api.LogLevel

fakt {
    logLevel.set(LogLevel.INFO)  // Default - concise summary
}
```

**Log Levels:**

- **`QUIET`** - Zero output (CI/CD builds)
- **`INFO`** (default) - Concise summary:
  ```
  ✅ 10 fakes generated in 1.2s (6 cached)
     Discovery: 120ms | Analysis: 340ms | Generation: 580ms | I/O: 160ms
     Cache hit rate: 40% (6/15)
  📁 build/generated/fakt/commonTest/kotlin
  ```

- **`DEBUG`** - Detailed breakdown per interface
- **`TRACE`** - Full IR details, type resolution, debugging info

**When to use:**
- `QUIET` - CI/CD builds (zero overhead)
- `INFO` - Normal development (<1ms overhead)
- `DEBUG` - Troubleshooting generation issues (~5-10ms overhead)
- `TRACE` - Deep debugging, bug reports (~20-50ms overhead)

---

## How It Works

Fakt uses a **two-phase FIR → IR compilation architecture**, inspired by [Metro DI framework](https://github.com/ZacSweers/metro):

### Phase 1: FIR (Frontend Intermediate Representation)
```
FaktFirExtensionRegistrar
  ↓
Detects @Fake annotations
  ↓
Validates interface structure
  ↓
Passes validated interfaces to IR phase
```

### Phase 2: IR (Intermediate Representation)
```
UnifiedFaktIrGenerationExtension
  ↓
InterfaceAnalyzer (extracts metadata)
  ↓
IrCodeGenerator (generates IR nodes)
  ↓
Output: Implementation + Factory + DSL
```

### Generated Code Structure

For each `@Fake` annotated interface, Fakt generates:

1. **Implementation Class** (`FakeXxxImpl`)
   - Implements interface/extends class
   - Behavior properties for each method/property
   - Default implementations (no-op for Unit, sensible defaults for others)
   - Call tracking via StateFlow

2. **Factory Function** (`fakeXxx {}`)
   - Creates new fake instance
   - Accepts configuration lambda
   - Type-safe, isolated instances

3. **Configuration DSL** (`FakeXxxConfig`)
   - Type-safe API for configuring behaviors
   - IDE autocompletion support
   - Compile-time validation

**Output Location:** `build/generated/fakt/{sourceSet}/kotlin/`

For deep technical details, see [CLAUDE.md](CLAUDE.md).

---

## Requirements

- **Kotlin:** 2.2.10+
- **Gradle:** 8.0+
- **JVM:** 11+ (for compiler plugin execution)

---

## Documentation

- **[CLAUDE.md](CLAUDE.md)** - Complete technical reference ("The Bible")
- **[Samples](samples/kmp-single-module/)** - Working KMP examples with 100+ test scenarios
- **[Testing Guidelines](.claude/docs/validation/testing-guidelines.md)** - GIVEN-WHEN-THEN standard
- **[Multi-Module Design](.claude/docs/multi-module/README.md)** - Architecture documentation

---

## Project Status

**Fakt follows the MAP (Minimum Awesome Product) philosophy:**
- ✅ Production-quality code generation
- ✅ Type-safe DSL without `Any` casting
- ✅ Comprehensive test coverage (100+ scenarios)
- ✅ Professional error messages
- ✅ Works with real-world KMP projects

**Known Limitations:**
- Multi-module support requires dedicated `-fakes` modules (experimental)
- Call tracking always enabled (no opt-out currently)

For current implementation status, see [implementation docs](.claude/docs/implementation/).

---

## Contributing

Contributions welcome! Please:
1. Follow GIVEN-WHEN-THEN testing standard
2. Ensure all generated code compiles
3. Test both single-platform and KMP scenarios
4. Run `make format` before committing

See [CLAUDE.md](CLAUDE.md) for development guidelines.

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
