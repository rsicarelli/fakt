# Fakt

**Compile-time type-safe test fakes for Kotlin Multiplatform**

[![Build](https://img.shields.io/github/actions/workflow/status/rsicarelli/fakt/ci.yml)](https://github.com/rsicarelli/fakt/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.rsicarelli.fakt/runtime)](https://search.maven.org/search?q=g:com.rsicarelli.fakt)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10%2B-blue)](https://kotlinlang.org)
[![Documentation](https://img.shields.io/badge/docs-mkdocs-blue)](https://rsicarelli.github.io/fakt/)

---

Fakt is a Kotlin compiler plugin that generates test fakes at compile-time. No runtime reflection. No production dependencies. Just type-safe fakes that break when your interfaces change.

```kotlin
@Fake
interface AnalyticsService {
    fun track(event: String)
}

// Use in tests
val fake = fakeAnalyticsService {
    track { event -> println("Tracked: $event") }
}

fake.track("user_signup")
assertEquals(1, fake.trackCallCount.value)
```

---

## The Problem

Writing test fakes manually is tedious and error-prone:

```kotlin
// Manual fake: ~50 lines of boilerplate per interface
class FakeAnalyticsService : AnalyticsService {
    private var trackBehavior: ((String) -> Unit)? = null
    private var _trackCallCount = 0
    val trackCallCount: Int get() = _trackCallCount

    override fun track(event: String) {
        _trackCallCount++
        trackBehavior?.invoke(event) ?: Unit
    }

    fun configureTrack(behavior: (String) -> Unit) {
        trackBehavior = behavior
    }
}

// No DSL, manual configuration
val fake = FakeAnalyticsService().apply {
    configureTrack { event -> /* ... */ }
}
```

**Issues:**
- Repetitive boilerplate for every interface
- Manual call tracking (non-thread-safe)
- Refactoring breaks nothing at compile-time
- Maintenance burden scales with codebase

---

## The Solution

Add `@Fake` annotation. Fakt generates everything at compile-time:

```kotlin
@Fake
interface AnalyticsService {
    fun track(event: String)
    suspend fun identify(userId: String): Result<Unit>
}
```

**Generated** (`build/generated/fakt/commonTest/kotlin/`):

```kotlin
// Implementation with thread-safe call tracking
class FakeAnalyticsServiceImpl : AnalyticsService {
    // Automatic StateFlow-based call counting (reactive, thread-safe)
    private val _trackCallCount = MutableStateFlow(0)
    val trackCallCount: StateFlow<Int> get() = _trackCallCount

    private val _identifyCallCount = MutableStateFlow(0)
    val identifyCallCount: StateFlow<Int> get() = _identifyCallCount

    // Smart default behaviors
    private var trackBehavior: (String) -> Unit = { }
    private var identifyBehavior: suspend (String) -> Result<Unit> = { Result.success(Unit) }

    override fun track(event: String) {
        _trackCallCount.update { it + 1 }
        trackBehavior(event)
    }

    override suspend fun identify(userId: String): Result<Unit> {
        _identifyCallCount.update { it + 1 }
        return identifyBehavior(userId)
    }

    internal fun configureTrack(behavior: (String) -> Unit) { trackBehavior = behavior }
    internal fun configureIdentify(behavior: suspend (String) -> Result<Unit>) { identifyBehavior = behavior }
}

// Type-safe factory function
fun fakeAnalyticsService(
    configure: FakeAnalyticsServiceConfig.() -> Unit = {}
): FakeAnalyticsServiceImpl = FakeAnalyticsServiceImpl().apply {
    FakeAnalyticsServiceConfig(this).configure()
}

// Clean DSL for configuration
class FakeAnalyticsServiceConfig(private val fake: FakeAnalyticsServiceImpl) {
    fun track(behavior: (String) -> Unit) { fake.configureTrack(behavior) }
    fun identify(behavior: suspend (String) -> Result<Unit>) { fake.configureIdentify(behavior) }
}
```

**Usage in tests:**

```kotlin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsTest {
    @Test
    fun `GIVEN configured fake WHEN tracking event THEN executes behavior`() {
        val events = mutableListOf<String>()
        val fake = fakeAnalyticsService {
            track { event -> events.add(event) }
        }

        fake.track("user_signup")
        fake.track("user_login")

        assertEquals(listOf("user_signup", "user_login"), events)
        assertEquals(2, fake.trackCallCount.value)
    }

    @Test
    fun `GIVEN suspend function WHEN called THEN preserves coroutine semantics`() = runTest {
        val fake = fakeAnalyticsService {
            identify { userId -> Result.success(Unit) }
        }

        val result = fake.identify("user-123")

        assertTrue(result.isSuccess)
        assertEquals(1, fake.identifyCallCount.value)
    }
}
```

---

## Key Technical Achievements

### ✅ Compile-Time Safety + Universal KMP Support

Fakt generates code at the **IR (Intermediate Representation) level** during compilation. This means:

- **Type safety**: Refactoring interfaces instantly breaks tests at compile-time (no runtime surprises)
- **Zero reflection**: Works on ALL Kotlin Multiplatform targets (JVM, Android, iOS, Native, JS, WASM)
- **Platform universality**: Native targets, WebAssembly, JavaScript—everywhere Kotlin compiles

Unlike runtime mocking frameworks (MockK, Mockito), Fakt-generated fakes compile to native code without reflection.

### ✅ Zero Production Overhead

Fakt has **zero runtime cost** and **zero production dependencies**:

- **Annotation-only runtime**: The `@Fake` annotation has BINARY retention and zero dependencies
- **Test-only generation**: Fakes are generated in test source sets (`commonTest/`, `androidUnitTest/`, etc.)
- **No leakage**: Generated code never appears in production builds
- **IR-level generation**: Direct Kotlin IR manipulation, not KotlinPoet or text generation

Your production artifacts remain completely unaffected.

### ✅ Built-In StateFlow Call Tracking

Every generated fake includes reactive, thread-safe call tracking via Kotlin `StateFlow`:

```kotlin
val fake = fakeUserRepository()

fake.getUser("123")
fake.getUser("456")

// Thread-safe, reactive call counting
assertEquals(2, fake.getUserCallCount.value)

// Works with coroutine test utilities
fake.getUserCallCount.test {
    fake.getUser("789")
    assertEquals(3, awaitItem())
}
```

**Benefits:**
- Thread-safe out of the box (unlike manual `var count = 0`)
- Reactive testing support with Kotlin Flow APIs
- Mutable properties get BOTH getter and setter counters

### ✅ IR-Level Code Generation Philosophy

Fakt uses a **two-phase FIR → IR compilation architecture** inspired by production compiler plugins:

1. **FIR Phase**: Detects `@Fake` annotations and validates interface structure
2. **IR Phase**: Generates implementation classes, factory functions, and DSL directly as Kotlin IR nodes

**Why IR-level generation matters:**
- Full access to Kotlin's type system (generics, variance, constraints)
- Better performance than KSP or annotation processors
- Works with Kotlin's incremental compilation
- Future-proof with Kotlin compiler evolution

**Code generation principles:**
- Smart defaults (identity functions for generics, sensible primitives)
- Idiomatic Kotlin (extension functions, inline reified generics, StateFlow)
- Preserves language features (suspend functions, variance, constraints)
- Professional code quality (proper formatting, internal visibility, documented)

For architectural deep dives, see [`.claude/docs/architecture/`](/.claude/docs/architecture/).

---

## Quick Start

### 1. Add Fakt to your project

**Version Catalog** (`gradle/libs.versions.toml`):
```toml
[versions]
fakt = "1.0.0-SNAPSHOT"
kotlin = "2.2.10"

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
fakt = { id = "com.rsicarelli.fakt", version.ref = "fakt" }

[libraries]
fakt-runtime = { module = "com.rsicarelli.fakt:runtime", version.ref = "fakt" }
```

**Root** `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.fakt) apply false
}
```

**Module** `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

kotlin {
    // Your KMP targets (jvm, iosArm64, etc.)

    sourceSets {
        commonMain.dependencies {
            implementation(libs.fakt.runtime)
        }
    }
}
```

### 2. Annotate interfaces

```kotlin
// src/commonMain/kotlin/com/example/UserRepository.kt
import com.rsicarelli.fakt.Fake

@Fake
interface UserRepository {
    suspend fun getUser(id: String): Result<User>
    suspend fun saveUser(user: User): Result<Unit>
}
```

### 3. Build and use in tests

```bash
./gradlew build
```

```kotlin
// src/commonTest/kotlin/com/example/UserRepositoryTest.kt
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UserRepositoryTest {
    @Test
    fun `GIVEN fake repository WHEN saving user THEN returns success`() = runTest {
        val fake = fakeUserRepository {
            saveUser { user -> Result.success(Unit) }
        }

        val result = fake.saveUser(User("123", "Alice"))

        assertTrue(result.isSuccess)
        assertEquals(1, fake.saveUserCallCount.value)
    }
}
```

---

## Platform Support

Fakt works on **all Kotlin Multiplatform targets** without reflection:

| Platform         | Targets                                                     | Status |
|------------------|-------------------------------------------------------------|--------|
| **JVM**          | `jvm()`                                                     | ✅      |
| **Android**      | `androidTarget()`                                           | ✅      |
| **iOS**          | `iosArm64()`, `iosX64()`, `iosSimulatorArm64()`             | ✅      |
| **macOS**        | `macosArm64()`, `macosX64()`                                | ✅      |
| **Linux**        | `linuxArm64()`, `linuxX64()`                                | ✅      |
| **Windows**      | `mingwX64()`                                                | ✅      |
| **JavaScript**   | `js(IR)` - Browser & Node.js                                | ✅      |
| **WebAssembly**  | `wasmJs()`                                                  | ✅      |
| **watchOS**      | `watchosArm64()`, `watchosX64()`, `watchosSimulatorArm64()` | ✅      |
| **tvOS**         | `tvosArm64()`, `tvosX64()`, `tvosSimulatorArm64()`          | ✅      |

**Single-platform projects** (JVM-only, Android-only) are fully supported.

---

## Multi-Module Support (Experimental)

Fakt supports cross-module fake consumption via dedicated `-fakes` modules:

```kotlin
// Producer module: :core:analytics
@Fake
interface Analytics

// Collector module: :core:analytics-fakes/build.gradle.kts
plugins {
    id("com.rsicarelli.fakt")
}

fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}

// Consumer module: :app/build.gradle.kts
dependencies {
    commonTest {
        implementation(projects.core.analyticsFakes)  // Use generated fakes
    }
}
```

**For complete multi-module documentation**, see:
- [Multi-Module Overview](https://rsicarelli.github.io/fakt/multi-module/) - Architecture and patterns
- [Getting Started Guide](https://rsicarelli.github.io/fakt/multi-module/getting-started/) - Step-by-step setup
- [kmp-multi-module sample](/ktfake/samples/kmp-multi-module/) - Complete working example

---

## Performance

Fakt generates code at compile-time with **intelligent caching** across KMP targets:

**First target compilation** (e.g., `compileKotlinIosArm64`):
```
DISCOVERY: 1ms (100 interfaces, 21 classes)
GENERATION: 39ms (121 new fakes, avg 333µs/fake, 3,867 LOC)
TOTAL: 40ms
```

**Subsequent targets** (all cached):
```
compileKotlinJvm:         1ms (121 from cache)
compileKotlinAndroid:     1ms (121 from cache)
compileKotlinIosX64:      1ms (121 from cache)
```

**Cache hit rates** ensure minimal overhead even in large KMP projects.

**Telemetry configuration:**
```kotlin
import com.rsicarelli.fakt.compiler.api.LogLevel

fakt {
    logLevel.set(LogLevel.INFO)  // Default: concise summary
    // LogLevel.QUIET  - Zero output (CI/CD)
    // LogLevel.DEBUG  - Detailed breakdown
    // LogLevel.TRACE  - Full IR details
}
```

---

## What Fakt Supports

**Class Types:**
- ✅ Interfaces
- ✅ Abstract classes
- ✅ Open classes (overridable members only)
- ✅ Final classes with open members
- ✅ Companion objects

**Type System:**
- ✅ Full generics (class-level, method-level, constraints, variance)
- ✅ SAM interfaces (functional interfaces)
- ✅ Complex stdlib types (`Result<T>`, `List<T>`, etc.)
- ✅ Nullable types

**Kotlin Features:**
- ✅ Suspend functions
- ✅ Properties (`val`, `var`)
- ✅ Methods with default parameters
- ✅ Inheritance

**Current Limitations:**
- ❌ Data classes as `@Fake` targets (work fine as parameter/return types)
- ❌ Sealed hierarchies as `@Fake` targets (work fine as parameter/return types)
- ❌ Default parameters in interface methods

---

## Requirements

- **Kotlin:** 2.2.10+
- **Gradle:** 8.0+
- **JVM:** 11+

---

## Contributing

Contributions are welcome! Please:

1. Follow **GIVEN-WHEN-THEN** testing standard (see [`.claude/docs/validation/testing-guidelines.md`](/.claude/docs/validation/testing-guidelines.md))
2. Ensure all generated code compiles without errors
3. Test both single-platform and KMP scenarios
4. Run `make format` before committing

For development workflows, run `make help` to see all available commands.

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
