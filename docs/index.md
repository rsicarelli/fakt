# Fakt

**Compile-time type-safe test fakes for Kotlin Multiplatform**

[![Build](https://img.shields.io/github/actions/workflow/status/rsicarelli/fakt/ci.yml)](https://github.com/rsicarelli/fakt/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.rsicarelli.fakt/runtime)](https://search.maven.org/search?q=g:com.rsicarelli.fakt)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21%2B-blue)](https://kotlinlang.org)

Automate the fake-over-mock pattern. Fakt generates type-safe test doubles that eliminate boilerplate.

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

- **Manual fakes scale poorly** - Each type needs custom tracking, config, or cleanup code
- **Manual fakes rot over time** - Behavior diverges from real implementations without compile-time warnings
- **Mocks verify interactions, not behavior** - Testing how dependencies are called, not what code achieves
- **Mocks make refactoring painful** - Tests break even when behavior stays correct
- **[Google explicitly prefers fakes](https://developer.android.com/training/testing/fundamentals/test-doubles)** - Lightweight, framework-free, and resilient to refactoring
- **Compile-time generation solves both problems** - Automated fakes that never drift from interfaces

**[Read the full story →](get-started/why-fakt.md)**

---

## ✨ Features

- ✅ **Zero boilerplate** - Compiler generates type-safe fakes automatically at build time
- ✅ **Never drift from real code** - Interface changes cause compile errors, not silent bugs
- ✅ **Test what matters** - Verify outcomes (state) instead of implementation details (calls)
- ✅ **Works everywhere** - All KMP targets without reflection, zero production dependencies
- ✅ **Smart defaults** - Sensible behaviors for all types, configure only what you need
- ✅ **Real code, not magic** - Generated .kt files are readable, not bytecode magic

**[Complete feature reference →](get-started/features.md)**

---

## 🚀 Getting Started

**Quick Start:**

1. **[Getting Started](get-started/index.md)** - Install Fakt and create your first fake in 5 minutes
2. **[Usage Guide](user-guide/usage.md)** - Core patterns and techniques
3. **[Testing Patterns](user-guide/testing-patterns.md)** - Best practices for using fakes in tests

**Usage Topics:**

- **[Suspend Functions](user-guide/usage.md#suspend-functions)** - Working with coroutines and async code
- **[Generics](user-guide/usage.md#generics)** - Generic interfaces and type parameters
- **[Properties](user-guide/usage.md#properties)** - Faking val and var properties
- **[Call Tracking](user-guide/usage.md#call-tracking)** - StateFlow-based reactive counters

**Advanced Topics:**

- **[Multi-Module Setup](user-guide/multi-module.md)** - Cross-module fakes with collector modules
- **[Configuration](user-guide/plugin-configuration.md)** - Plugin configuration and log levels
- **[Performance](user-guide/performance.md)** - Build times, caching, and optimization
- **[Migration from Mocks](user-guide/migration-from-mocks.md)** - Migrating from MockK or Mockito

**Reference:**

- **[API Reference](user-guide/generated-code-reference.md)** - Complete generated API documentation
- **[Limitations](user-guide/known-issues.md)** - Known limitations and workarounds
- **[Compatibility](user-guide/platform-support.md)** - Platform and version requirements
- **[FAQ](help/faq.md)** - Frequently asked questions
- **[Troubleshooting](help/troubleshooting.md)** - Common issues and solutions

---

## 🌐 Platform Support

Fakt works on **all Kotlin Multiplatform targets** without reflection: JVM, Android, iOS, macOS, Linux, Windows, JavaScript, WebAssembly, watchOS, tvOS.

Single-platform projects (JVM-only, Android-only) are fully supported.

**[Full compatibility matrix →](user-guide/platform-support.md)**

---

## Requirements

- **Kotlin:** 2.2.21+
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
