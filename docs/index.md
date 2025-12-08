# Fakt

**Compile-time type-safe test fakes for Kotlin Multiplatform**

[![Build](https://img.shields.io/github/actions/workflow/status/rsicarelli/fakt/ci.yml)](https://github.com/rsicarelli/fakt/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.rsicarelli.fakt/runtime)](https://search.maven.org/search?q=g:com.rsicarelli.fakt)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20%2B-blue)](https://kotlinlang.org)

Fakt generates type-safe test fakes at compile-time. No reflection. No production dependencies. Just clean fakes that break when your interfaces change.

```kotlin
@Fake
interface Analytics {
    fun track(event: String)
}

val fake = fakeAnalytics {
    track { event -> println("Tracked: $event") }
}
```

---

## Why Fakt?

Writing test fakes manually is tedious and error-prone. You write 50+ lines of boilerplate for every interface, manage non-thread-safe call counters, and refactoring interfaces won't break tests at compile-time. Runtime mocking frameworks like MockK and Mockito avoid the boilerplate but introduce severe performance penalties and don't work on Native/WASM targets.

Fakt solves both problems with compile-time code generation that works everywhere Kotlin compiles.

**[Read the full story →](introduction/why-fakt.md)**

---

## ✨ Features

- ✅ **Universal KMP support** - Works on all Kotlin targets without reflection
- ✅ **Zero production overhead** - Test-only code generation, no runtime dependencies
- ✅ **Thread-safe call tracking** - Built-in StateFlow-based reactive counters
- ✅ **Full language support** - Suspend functions, generics, properties, inheritance
- ✅ **Smart defaults** - Identity functions for generics, Result.success for Results
- ✅ **IR-level generation** - Direct compiler plugin for performance and compatibility

**[Complete feature reference →](introduction/features.md)**

---

## 🚀 Getting Started

**Quick Start:**

1. **[Getting Started](introduction/getting-started.md)** - Install Fakt and create your first fake in 5 minutes
2. **[Basic Usage](usage/basic-usage.md)** - Core patterns and techniques
3. **[Testing Patterns](guides/testing-patterns.md)** - Best practices for using fakes in tests

**Usage Guides:**

- **[Suspend Functions](usage/suspend-functions.md)** - Working with coroutines and async code
- **[Generics](usage/generics.md)** - Generic interfaces and type parameters
- **[Properties](usage/properties.md)** - Faking val and var properties
- **[Call Tracking](usage/call-tracking.md)** - StateFlow-based reactive counters

**Advanced Topics:**

- **[Multi-Module Setup](multi-module/index.md)** - Cross-module fakes with collector modules
- **[Configuration](guides/configuration.md)** - Plugin configuration and log levels
- **[Performance](guides/performance.md)** - Build times, caching, and optimization
- **[Migration from Mocks](guides/migration-from-mocks.md)** - Migrating from MockK or Mockito

**Reference:**

- **[API Reference](reference/api.md)** - Complete generated API documentation
- **[Limitations](reference/limitations.md)** - Known limitations and workarounds
- **[Compatibility](reference/compatibility.md)** - Platform and version requirements
- **[FAQ](faq.md)** - Frequently asked questions
- **[Troubleshooting](troubleshooting.md)** - Common issues and solutions

---

## 🌐 Platform Support

Fakt works on **all Kotlin Multiplatform targets** without reflection: JVM, Android, iOS, macOS, Linux, Windows, JavaScript, WebAssembly, watchOS, tvOS.

Single-platform projects (JVM-only, Android-only) are fully supported.

**[Full compatibility matrix →](reference/compatibility.md)**

---

## Requirements

- **Kotlin:** 2.2.20+
- **Gradle:** 8.0+
- **JVM:** 11+

---

## License

Fakt is licensed under the Apache License 2.0.

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
