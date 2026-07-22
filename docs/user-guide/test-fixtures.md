# Test Fixtures (JVM & Android)

Share generated fakes across modules using Gradle test fixtures — no collector module needed. Two flavors are supported:

- **JVM** — the Gradle [`java-test-fixtures`](https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures) plugin.
- **Android** — AGP's native `android { testFixtures { enable = true } }` (the `java-test-fixtures` plugin cannot be applied to an Android module — it breaks Gradle sync).

!!! note "Single-platform only"
    Test fixtures apply to single-platform JVM and Android library modules. For KMP multi-module projects, use the [Collector Module](multi-module.md) approach instead. (A KMP `androidTarget()`'s fixtures are a different mechanism and are out of scope here — this covers `com.android.library` modules.)

---

## Test Fixtures vs Collector Module

| | Test Fixtures | Collector Module |
|---|---|---|
| **Platform** | JVM + Android (single-platform) | KMP + JVM + Android |
| **Extra module needed?** | No | Yes (`:module-fakes`) |
| **Setup complexity** | Minimal (2 lines) | Moderate (new module + config) |
| **Consumer dependency** | `testFixtures(projects.core)` | `implementation(projects.core.fakes)` |
| **Best for** | JVM multi-module projects | KMP projects, Maven publishing |

---

## Setup

### Step 1: Configure the Producer Module

Apply the `java-test-fixtures` plugin and enable `useGradleTestFixtures`:

```kotlin
// core/build.gradle.kts
plugins {
    kotlin("jvm")
    `java-test-fixtures`
    alias(libs.plugins.fakt)
}

fakt {
    useGradleTestFixtures.set(true)
}
```

Define your `@Fake` annotated types as usual:

```kotlin
// core/src/main/kotlin/com/example/core/UserRepository.kt
@Fake
interface UserRepository {
    fun findById(id: String): User?
    fun save(user: User): User
    fun delete(id: String): Boolean
}
```

Build the module to generate fakes:

```bash
./gradlew :core:build
```

Verify fakes are generated in the `testFixtures` directory:

```
core/build/generated/fakt/testFixtures/kotlin/
└── com/example/core/
    └── FakeUserRepositoryImpl.kt
```

### Step 2: Consume in Another Module

In the consumer module, add a `testFixtures()` dependency — no Fakt plugin needed:

```kotlin
// app/build.gradle.kts
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(projects.core)
    testImplementation(testFixtures(projects.core))
}
```

Use the generated fakes in tests:

```kotlin
// app/src/test/kotlin/com/example/app/UserServiceTest.kt
class UserServiceTest {
    @Test
    fun `GIVEN user exists WHEN getUser THEN returns user`() {
        val repo = fakeUserRepository {
            findById { id -> User(id, "Alice") }
        }
        val service = UserService(repo)

        val result = service.getUser("1")

        assertEquals("Alice", result?.name)
    }
}
```

---

## Android (AGP) modules

Android library modules can't apply `java-test-fixtures` (it breaks Gradle sync with
`Neither the source set nor the artifact property was populated by the Android Gradle plugin`).
Use AGP's own test fixtures instead — Fakt detects the Android plugin and routes generated fakes
into the AGP `testFixtures` source set.

### Producer (`com.android.library`)

```kotlin
// producer/build.gradle.kts
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.fakt)
}

android {
    testFixtures {
        enable = true
    }
}

fakt {
    useGradleTestFixtures.set(true) // works for both JVM and Android
}
```

!!! warning "AGP 8.x needs an experimental flag"
    Kotlin compilation of the Android `testFixtures` source set requires this in
    `gradle.properties` on **AGP 8.x**:

    ```properties
    android.experimental.enableTestFixturesKotlinSupport=true
    ```

    Without it, AGP leaves `testFixtures` Java-only, no `compileDebugTestFixturesKotlin` task is
    created, and the generated Kotlin fakes are silently dropped. On **AGP 9.0+** this is the
    default and the property is unnecessary. The minimum supported floor is **AGP 8.11** — AGP ≤ 8.7
    calls a Kotlin Gradle Plugin API (`KotlinJvmOptions.getUseK2()`) that Kotlin 2.3.20, Fakt's
    compiler version, removed, so it cannot compile Android test-fixtures Kotlin.

### Consumer (must be an Android module)

```kotlin
// consumer/build.gradle.kts
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

dependencies {
    implementation(projects.producer)
    testImplementation(testFixtures(projects.producer))
}
```

!!! note "Consumer must be Android"
    A plain JVM module cannot resolve an Android `testFixtures` variant. Consume Android test
    fixtures from another Android module.

---

## How It Works

1. Fakt detects `useGradleTestFixtures.set(true)` **and** a testFixtures source — either the
   `java-test-fixtures` plugin (JVM) or `com.android.library` / `com.android.application` (Android)
2. Generated fakes go to `build/generated/fakt/testFixtures/kotlin/` instead of `test/`
3. The `testFixtures` source set is wired to include the generated directory: the JVM Java source
   set for `java-test-fixtures`, and every per-variant `compile*TestFixturesKotlin` task for Android
4. The module's own tests can use the fakes (Gradle makes testFixtures visible to test automatically)
5. Other modules import fakes via `testFixtures(project(":producer"))` — standard Gradle mechanism

---

## Validation

If `useGradleTestFixtures` is enabled but no testFixtures source is available (neither the
`java-test-fixtures` plugin nor an Android plugin), Fakt emits a warning and falls back to the
default `test` source set:

```
Fakt: useGradleTestFixtures is enabled but no testFixtures source is available. Apply ONE of:
  • JVM: add the `java-test-fixtures` plugin
      plugins { `java-test-fixtures` }
  • Android: enable AGP test fixtures
      android { testFixtures { enable = true } }
Falling back to default 'test' source set.
```

On Android, if the experimental Kotlin-test-fixtures property is missing, Fakt additionally warns
that the generated Kotlin fakes won't be compiled into the testFixtures artifact (see the AGP 8.x
note above).

---

## Sample Project

See [`samples/jvm-test-fixtures/`](https://github.com/rsicarelli/fakt/tree/main/samples/jvm-test-fixtures) for a working JVM multi-module example with:

- `core/` — Producer with interface, abstract class, and open class
- `app/` — Consumer using fakes from core's testFixtures

See [`samples/android-test-fixtures/`](https://github.com/rsicarelli/fakt/tree/main/samples/android-test-fixtures) for the Android (AGP) equivalent:

- `producer/` — `com.android.library` with `testFixtures { enable = true }` and `@Fake` interfaces
- `consumer/` — Android library consuming the fakes via `testImplementation(testFixtures(...))`

---

## Next Steps

- **[Multi-Module (KMP)](multi-module.md)** — Collector module approach for KMP projects
- **[Plugin Configuration](plugin-configuration.md)** — All configuration options
- **[Samples](../examples/index.md)** — All sample projects
