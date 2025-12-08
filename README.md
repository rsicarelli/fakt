<div align="center">
  <img src="FAKT_logo.png" alt="Fakt Logo" width="200">
</div>

# Fakt

**Compile-time type-safe test fakes for Kotlin Multiplatform**

[![Build](https://img.shields.io/github/actions/workflow/status/rsicarelli/fakt/ci.yml)](https://github.com/rsicarelli/fakt/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.rsicarelli.fakt/runtime)](https://search.maven.org/search?q=g:com.rsicarelli.fakt)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20%2B-blue)](https://kotlinlang.org)
[![Documentation](https://img.shields.io/badge/docs-mkdocs-blue)](https://rsicarelli.github.io/fakt/)

Fakt generates test fakes at compile-time. No runtime reflection. No production dependencies. Just type-safe fakes that break when your interfaces change.

```kotlin
@Fake
interface UserRepository {
    suspend fun getUser(id: String): User
}

val fake = fakeUserRepository {
    getUser { id -> User(id, "Alice") }
}
```

---

## ✅ Why Fakt?

Writing test fakes manually is tedious and error-prone:

- **Repetitive boilerplate** - 50+ lines per interface with manual call tracking
- **Silent breakage** - Refactoring interfaces doesn't break tests at compile-time
- **Thread-safety issues** - Manual counters are non-thread-safe
- **Maintenance burden** - Scales poorly across large codebases

**[Read the full story →](https://rsicarelli.github.io/fakt/introduction/why-fakt/)**

---

## 🎯 Key Features

- ✅ **Universal KMP support** - Works on all Kotlin targets without reflection
- ✅ **Zero production overhead** - Test-only code generation, no runtime dependencies
- ✅ **Thread-safe call tracking** - Built-in StateFlow-based reactive counters
- ✅ **Full language support** - Suspend functions, generics, properties, inheritance
- ✅ **Smart defaults** - Sensible behaviors for all types (identity functions, Result.success)
- ✅ **IR-level generation** - Direct compiler plugin, not KSP or annotation processing

**[Complete feature reference →](https://rsicarelli.github.io/fakt/introduction/features/)**

---

## ⚡ Quick Start

**1. Add plugin** (`build.gradle.kts`):
```kotlin
plugins {
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}
```

**2. Annotate interface:**
```kotlin
@Fake interface Analytics
```

**3. Use in tests:**
```kotlin
val fake = fakeAnalytics {
    track { event -> println(event) }
}
```

**[Full installation guide →](https://rsicarelli.github.io/fakt/introduction/installation/)**

---

## 📚 Documentation

| Topic | Guide |
|-------|-------|
| **Getting Started** | [Installation](https://rsicarelli.github.io/fakt/introduction/installation/) · [Quick Start](https://rsicarelli.github.io/fakt/introduction/quick-start/) |
| **Usage** | [Basic Usage](https://rsicarelli.github.io/fakt/usage/basic-usage/) · [Suspend Functions](https://rsicarelli.github.io/fakt/usage/suspend-functions/) · [Generics](https://rsicarelli.github.io/fakt/usage/generics/) |
| **Guides** | [Multi-Module Setup](https://rsicarelli.github.io/fakt/multi-module/) · [Testing Patterns](https://rsicarelli.github.io/fakt/guides/testing-patterns/) · [Performance](https://rsicarelli.github.io/fakt/guides/performance/) |
| **Reference** | [API Reference](https://rsicarelli.github.io/fakt/reference/api/) · [Limitations](https://rsicarelli.github.io/fakt/reference/limitations/) · [Compatibility](https://rsicarelli.github.io/fakt/reference/compatibility/) |

---

## 🌐 Platform Support

Works on **all Kotlin Multiplatform targets** without reflection: JVM, Android, iOS, macOS, Linux, Windows, JavaScript, WebAssembly, watchOS, tvOS.

Single-platform projects (JVM-only, Android-only) are fully supported.

**[Full compatibility matrix →](https://rsicarelli.github.io/fakt/reference/compatibility/)**

---

## Requirements

- **Kotlin:** 2.2.20+
- **Gradle:** 8.0+
- **JVM:** 11+

---

## Contributing

Contributions are welcome! Please follow the **[testing guidelines](https://github.com/rsicarelli/fakt/tree/main/.claude/docs/development/validation/testing-guidelines.md)** and run `make format` before committing.

Run `make help` for available development commands.

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
