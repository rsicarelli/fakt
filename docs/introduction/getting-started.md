# Getting Started

Get Fakt up and running in your project and create your first fake in 5 minutes.

---

## Prerequisites

| Requirement      | Version    |
|------------------|------------|
| **Kotlin**       | 2.2.20+    |
| **Gradle**       | 8.0+       |
| **JVM**          | 11+        |

---

## Installation

### Multiplatform Projects

**Version Catalog (`gradle/libs.versions.toml`):**

```toml
[versions]
fakt = "1.0.0-SNAPSHOT"
kotlin = "2.2.20"

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
fakt = { id = "com.rsicarelli.fakt", version.ref = "fakt" }

[libraries]
fakt-runtime = { module = "com.rsicarelli.fakt:runtime", version.ref = "fakt" }
```

**Root `build.gradle.kts`:**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.fakt) apply false
}
```

**Module `build.gradle.kts`:**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
            implementation(libs.fakt.runtime)
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
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

dependencies {
    implementation(libs.fakt.runtime)
}
```

**Android-Only:**

```kotlin
plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

dependencies {
    implementation(libs.fakt.runtime)
}
```

---

## Your First Fake (5 Minutes)

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

Fakt generates code in test source sets only:

| Source Set          | Generated Output                                |
|---------------------|-------------------------------------------------|
| `commonTest/`       | `build/generated/fakt/commonTest/kotlin/`       |
| `jvmTest/`          | `build/generated/fakt/jvmTest/kotlin/`          |
| `iosTest/`          | `build/generated/fakt/iosTest/kotlin/`          |
| `androidUnitTest/`  | `build/generated/fakt/androidUnitTest/kotlin/`  |

!!! note
    Generated code **never** appears in production builds. Fakt is test-only.

---

## More Complex Example

Here's a realistic interface with suspend functions and generics:

```kotlin
@Fake
interface UserRepository {
    suspend fun getUser(id: String): Result<User>
    suspend fun saveUser(user: User): Result<Unit>
    val currentUser: User?
}
```

**Using it in tests:**

```kotlin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class UserRepositoryTest {
    @Test
    fun `GIVEN fake repository WHEN saving user THEN returns success`() = runTest {
        val savedUsers = mutableListOf<User>()

        val fake = fakeUserRepository {
            saveUser { user ->
                savedUsers.add(user)
                Result.success(Unit)
            }
            getUser { id ->
                Result.success(User(id, "Alice"))
            }
            currentUser { User("123", "Bob") }
        }

        val result = fake.saveUser(User("456", "Charlie"))

        assertTrue(result.isSuccess)
        assertEquals(1, savedUsers.size)
        assertEquals("Charlie", savedUsers.first().name)
        assertEquals(1, fake.saveUserCallCount.value)
    }
}
```

---

## IDE Support

Fakt-generated code appears in `build/generated/fakt/` and is automatically indexed by IntelliJ IDEA and Android Studio.

!!! tip "K2 IDE Mode"
    Enable K2 mode for better autocomplete of generated factories:

    **Settings → Languages & Frameworks → Kotlin → Enable K2 mode**

---

## Kotlin Version Compatibility

| Fakt Version     | Kotlin Version Support |
|------------------|------------------------|
| 1.0.0-SNAPSHOT   | 2.2.20 - 2.2.30        |

Fakt follows forward compatibility on a best-effort basis (usually N+.2 minor versions).

---

## Next Steps

- **[Features](features.md)** - Complete feature reference
- **[Basic Usage](../usage/basic-usage.md)** - Common patterns and examples
- **[Suspend Functions](../usage/suspend-functions.md)** - Async/coroutine support
- **[Call Tracking](../usage/call-tracking.md)** - StateFlow-based reactive counters
- **[Testing Patterns](../guides/testing-patterns.md)** - Best practices
- **[Configuration](../guides/configuration.md)** - Plugin options (coming soon)
