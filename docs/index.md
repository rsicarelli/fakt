# Fakt

**Compile-time type-safe test fakes for Kotlin Multiplatform**

[![Build](https://img.shields.io/github/actions/workflow/status/rsicarelli/fakt/ci.yml)](https://github.com/rsicarelli/fakt/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.rsicarelli.fakt/runtime)](https://search.maven.org/search?q=g:com.rsicarelli.fakt)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20%2B-blue)](https://kotlinlang.org)

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

## Why Fakt?

Writing test fakes manually is tedious and error-prone. Fakt generates type-safe fakes automatically at compile-time, eliminating boilerplate while maintaining compile-time safety.

**The problem with manual fakes:**

- Repetitive boilerplate for every interface
- Manual call tracking (non-thread-safe)
- Refactoring breaks nothing at compile-time
- Maintenance burden scales with codebase

**How Fakt solves this:**

- **Compile-time generation**: Zero reflection, works on ALL KMP targets
- **Type safety**: Refactoring breaks tests immediately
- **StateFlow tracking**: Thread-safe, reactive call counting
- **Zero overhead**: No production dependencies, test-only code
- **Clean DSL**: Intuitive configuration interface

---

## Key Features

### ✅ Universal Multiplatform Support

Works on **all Kotlin targets** without reflection: JVM, Android, iOS, Native, JavaScript, WebAssembly.

Unlike runtime mocking frameworks (MockK, Mockito), Fakt generates code at the IR level—native compilation everywhere.

### ✅ Zero Production Overhead

Fakt has **zero runtime cost** and **zero production dependencies**:

- Annotation-only runtime (BINARY retention, no dependencies)
- Test-only generation (generated in test source sets)
- No production leakage
- IR-level generation (not text-based)

### ✅ Built-In StateFlow Call Tracking

Every generated fake includes reactive, thread-safe call tracking via Kotlin `StateFlow`:

```kotlin
val fake = fakeUserRepository()

fake.getUser("123")
fake.getUser("456")

// Thread-safe, reactive call counting
assertEquals(2, fake.getUserCallCount.value)
```

### ✅ Full Language Support

- **Suspend functions**: Full coroutine support
- **Generics**: Class-level, method-level, constraints, variance
- **Properties**: val/var with getter/setter tracking
- **Smart defaults**: Identity functions for generics, sensible primitives

---

## Quick Example

**1. Annotate an interface:**

```kotlin
import com.rsicarelli.fakt.Fake

@Fake
interface UserRepository {
    suspend fun getUser(id: String): Result<User>
    suspend fun saveUser(user: User): Result<Unit>
}
```

**2. Build your project:**

```bash
./gradlew build
```

**3. Use in tests:**

```kotlin
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

## Get Started

Ready to eliminate test boilerplate?

- [Installation](introduction/installation.md) - Add Fakt to your project
- [Quick Start](introduction/quick-start.md) - Your first fake in 5 minutes
- [Features](introduction/features.md) - What Fakt supports

---

## Platform Support

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

## Requirements

- **Kotlin:** 2.2.20+
- **Gradle:** 8.0+
- **JVM:** 11+

---

## License

Fakt is licensed under the Apache License 2.0. See [LICENSE](https://github.com/rsicarelli/fakt/blob/main/LICENSE) for details.

```
Copyright (C) 2025 Rodrigo Sicarelli

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
```
