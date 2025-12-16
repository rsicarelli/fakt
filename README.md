<div id="user-content-toc">
  <ul style="list-style: none;">
    <summary>
      <img src="FAKT_logo.png" alt="Fakt Logo" width="120" align="left" hspace="20"><h1>Fakt</h1>
      <p>
      <a href="https://github.com/rsicarelli/fakt/actions/workflows/continuous-deploy.yml"><img src="https://img.shields.io/github/actions/workflow/status/rsicarelli/fakt/continuous-deploy.yml" alt="Build"></a>
      <a href="https://search.maven.org/search?q=g:com.rsicarelli.fakt"><img src="https://img.shields.io/maven-central/v/com.rsicarelli.fakt/annotations" alt="Maven Central"></a>
      <a href="https://opensource.org/licenses/Apache-2.0"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
      <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.2.21%2B-blue" alt="Kotlin"></a>
      <a href="https://rsicarelli.github.io/fakt/"><img src="https://img.shields.io/badge/docs-mkdocs-blue" alt="Documentation"></a>
    </summary>
  </ul>
</div>

<br clear="left"/>

Automate the fake-over-mock pattern. Fakt generates type-safe test doubles that eliminate boilerplate.

```kotlin
@Fake
interface UserRepository {
    suspend fun getUser(id: String): User
}

// Use in tests
val fake = fakeUserRepository {
    getUser { id -> User(id, "Alice") }
}
```

## 🤔 Why Fakt?

- **Manual fakes scale poorly** - Each type needs custom tracking, config, or cleanup code
- **Manual fakes rot over time** - Behavior diverges from real implementations without compile-time warnings
- **Mocks verify interactions, not behavior** - Testing how dependencies are called, not what code achieves
- **Mocks make refactoring painful** - Tests break even when behavior stays correct
- **[Google explicitly prefers fakes](https://developer.android.com/training/testing/fundamentals/test-doubles)** - Lightweight, framework-free, and resilient to refactoring
- **Compile-time generation solves both problems** - Automated fakes that never drift from interfaces


## ✨ Key Features

- **Zero boilerplate** - Compiler generates type-safe fakes automatically at build time
- **Never drift from real code** - Interface changes cause compile errors, not silent bugs
- **Test what matters** - Verify outcomes (state) instead of implementation details (calls)
- **Works everywhere** - All KMP targets without reflection, zero production dependencies
- **Smart defaults** - Sensible behaviors for all types, configure only what you need
- **Real code, not magic** - Generated .kt files are readable, not bytecode magic

## ⚡ Quick Start

**1. Add plugin and dependency** (`build.gradle.kts`):
```kotlin
plugins {
    kotlin("multiplatform") version "2.2.21" // JVM or Android also works
    id("com.rsicarelli.fakt") version "x.y.z"
}

kotlin {
    // ... your targets

    sourceSets {
        commonMain {
            dependencies {
                implementation("com.rsicarelli.fakt:annotations:x.y.z")
            }
        }
    }
}
```

**2. Annotate interface:**
```kotlin
import com.rsicarelli.fakt.Fake

@Fake
interface Analytics {
    suspend fun track(event: String)
}
```

**3. Build the project:**
```kotlin
./gradlew build
```

**4. Use in tests:**
```kotlin
val events = mutableListOf<String>()
val fake = fakeAnalytics {
    track { event -> events.add(event) }
}

fake.track("user_signup")

assertEquals(listOf("user_signup"), events)
assertEquals(1, fake.trackCallCount.value)
```

## 🤝 Contributing

- **Report Bugs:** [Bug Report](https://github.com/rsicarelli/fakt/issues/new?assignees=&labels=bug%2Cneeds-triage&template=bug_report.yml)
- **Suggest Features:** [Feature Request](https://github.com/rsicarelli/fakt/issues/new?assignees=&labels=enhancement%2Cneeds-triage&template=feature_request.yml) - Your ideas shape our roadmap!
- **Documentation:** [User Guide](https://rsicarelli.github.io/fakt/)
- **Contributing:** See [CONTRIBUTING.md](CONTRIBUTING.md)


## 🐜 Our Mascot

Meet our mascot, the [Giant Anteater](https://en.wikipedia.org/wiki/Giant_anteater)! Just like they eat bugs in nature, Fakt catches drift bugs at compile-time before they reach production.

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
