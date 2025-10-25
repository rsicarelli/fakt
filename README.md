# Fakt

**Type-safe test fake generation for Kotlin Multiplatform via FIR/IR compiler plugin**

[![Build](https://img.shields.io/github/actions/workflow/status/rsicarelli/fakt/ci.yml)](https://github.com/rsicarelli/fakt/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.rsicarelli.fakt/runtime)](https://search.maven.org/search?q=g:com.rsicarelli.fakt)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10%2B-blue)](https://kotlinlang.org)

---

## What Is Fakt?

Fakt is a **Kotlin compiler plugin** that generates type-safe test fakes at compile-time. It uses a two-phase **FIR → IR** compilation architecture to analyze `@Fake` annotated interfaces and classes, then generates production-quality fake implementations with expressive configuration DSLs.

### Before Fakt - Manual Fake (40+ lines)

```kotlin
// Manual fake implementation - tedious, error-prone, hard to maintain
class FakeUserRepositoryImpl : UserRepository {
    private var findByIdBehavior: (String) -> User? = { _ -> null }
    private var saveBehavior: (User) -> User = { it }
    private var deleteBehavior: (String) -> Boolean = { _ -> false }
    private var findByAgeBehavior: (Int, Int) -> List<User> = { _, _ -> emptyList() }
    private var usersBehavior: () -> List<User> = { emptyList() }

    override fun findById(id: String): User? = findByIdBehavior(id)
    override fun save(user: User): User = saveBehavior(user)
    override fun delete(id: String): Boolean = deleteBehavior(id)
    override fun findByAge(minAge: Int, maxAge: Int): List<User> = findByAgeBehavior(minAge, maxAge)
    override val users: List<User> get() = usersBehavior()

    fun configureFindById(behavior: (String) -> User?) { findByIdBehavior = behavior }
    fun configureSave(behavior: (User) -> User) { saveBehavior = behavior }
    fun configureDelete(behavior: (String) -> Boolean) { deleteBehavior = behavior }
    fun configureFindByAge(behavior: (Int, Int) -> List<User>) { findByAgeBehavior = behavior }
    fun configureUsers(behavior: () -> List<User>) { usersBehavior = behavior }
}

// Factory function
fun fakeUserRepository(configure: FakeUserRepositoryConfig.() -> Unit = {}): FakeUserRepositoryImpl {
    return FakeUserRepositoryImpl().apply { FakeUserRepositoryConfig(this).configure() }
}

// Configuration DSL
class FakeUserRepositoryConfig(private val fake: FakeUserRepositoryImpl) {
    fun findById(behavior: (String) -> User?) { fake.configureFindById(behavior) }
    fun save(behavior: (User) -> User) { fake.configureSave(behavior) }
    fun delete(behavior: (String) -> Boolean) { fake.configureDelete(behavior) }
    fun findByAge(behavior: (Int, Int) -> List<User>) { fake.configureFindByAge(behavior) }
    fun users(behavior: () -> List<User>) { fake.configureUsers(behavior) }
}
```

### After Fakt - Single Annotation

```kotlin
import com.rsicarelli.fakt.Fake

@Fake
interface UserRepository {
    val users: List<User>
    fun findById(id: String): User?
    fun save(user: User): User
    fun delete(id: String): Boolean
    fun findByAge(minAge: Int, maxAge: Int = 100): List<User>
}
```

**Everything above is generated automatically at compile-time.**

### Usage in Tests

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class UserServiceTest {
    @Test
    fun `GIVEN user repository WHEN saving user THEN should return saved user`() {
        // Given - Configure fake with type-safe DSL
        val fake = fakeUserRepository {
            save { user -> user.copy(id = "generated-id") }
            findById { id -> User(id, "Test User", "test@example.com") }
        }

        // When
        val saved = fake.save(User("", "John Doe", "john@example.com"))
        val found = fake.findById("generated-id")

        // Then
        assertEquals("generated-id", saved.id)
        assertEquals("Test User", found?.name)
    }
}
```

---

## Quick Start

### 1. Apply the Gradle Plugin

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.10"
    id("com.rsicarelli.fakt") version "x.y.z"
}

kotlin {
    jvm()
    js(IR) { nodejs() }
    iosArm64()
    iosSimulatorArm64()
}
```

### 2. Annotate Your Interfaces

```kotlin
// src/commonMain/kotlin/com/example/MyService.kt
import com.rsicarelli.fakt.Fake

@Fake
interface MyService {
    suspend fun getData(id: String): Result<Data>
}
```

### 3. Use Generated Fakes in Tests

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

**That's it!** Fakt generates:
- `FakeMyServiceImpl` class
- `fakeMyService {}` factory function
- `FakeMyServiceConfig` DSL for configuration

---

## Features

### Core Capabilities

- ✅ **Interfaces** - Primary target for fake generation
- ✅ **Abstract Classes** - Supported with proper inheritance
- ✅ **Open Classes** - Generate fakes for extensible classes
- ✅ **Final Classes** - Works with sealed implementation classes
- ✅ **Suspend Functions** - Full coroutine support
- ✅ **Properties** (`val`/`var`) - Configurable getters/setters
- ✅ **Methods** - Instance and extension methods
- ✅ **Default Parameters** - Preserved in generated fakes
- ✅ **Companion Objects** - Static-like members supported

### Type System Support

- ✅ **Generics**
  - Basic generics (`interface Repository<T>`)
  - Method-level generics (`fun <T> process(item: T): T`)
  - Class-level generics (`interface Cache<K, V>`)
  - Generic constraints (`<T : Comparable<T>>`)
  - Variance (`in`, `out`, invariant)

- ✅ **SAM Interfaces** (Single Abstract Method)
  - Function types as parameters
  - Higher-order functions
  - Lambda expressions

- ✅ **Complex Standard Library Types**
  - `Result<T>`, `Pair<A, B>`, `Triple<A, B, C>`
  - `Sequence<T>`, `Lazy<T>`
  - Nullable types (`T?`)
  - Collections (`List`, `Set`, `Map`)

### Kotlin Language Features

- ✅ **Sealed Classes** - Complete hierarchy support
- ✅ **Enums** - Including enums with properties and methods
- ✅ **Data Classes** - As parameter/return types
- ✅ **Inheritance** - Multi-level inheritance hierarchies
- ✅ **Meta-Annotations** - Custom annotations via `@GeneratesFake`

### Platform Support

- ✅ **Kotlin Multiplatform**
  - JVM (Java 11+)
  - JavaScript (IR backend)
  - Native (all targets: iOS, macOS, Linux, Windows)
  - WebAssembly
  - Android

- ✅ **Single-Platform Kotlin**
  - JVM-only projects
  - Android-only projects

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
