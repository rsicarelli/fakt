# Test Fixtures (JVM)

Share generated fakes across JVM modules using Gradle's built-in [test fixtures](https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures) plugin — no collector module needed.

!!! note "JVM Only"
    This feature requires the `java-test-fixtures` Gradle plugin, which is only available for JVM and Android projects. For KMP multi-module projects, use the [Collector Module](multi-module.md) approach instead.

---

## Test Fixtures vs Collector Module

| | Test Fixtures | Collector Module |
|---|---|---|
| **Platform** | JVM / Android only | KMP + JVM + Android |
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

## How It Works

1. Fakt detects `java-test-fixtures` plugin and `useGradleTestFixtures.set(true)`
2. Generated fakes go to `build/generated/fakt/testFixtures/kotlin/` instead of `test/`
3. The `testFixtures` source set is configured to include the generated directory
4. The module's own tests can use the fakes (Gradle makes testFixtures visible to test automatically)
5. Other modules import fakes via `testFixtures(project(":core"))` — standard Gradle mechanism

---

## Validation

If `useGradleTestFixtures` is enabled but the `java-test-fixtures` plugin is not applied, Fakt emits a warning and falls back to the default `test` source set:

```
Fakt: useGradleTestFixtures is enabled but the 'java-test-fixtures' plugin
is not applied. Add `java-test-fixtures` to your plugins block:
  plugins {
      `java-test-fixtures`
  }
Falling back to default 'test' source set.
```

---

## Sample Project

See [`samples/jvm-test-fixtures/`](https://github.com/rsicarelli/fakt/tree/main/samples/jvm-test-fixtures) for a working multi-module example with:

- `core/` — Producer with interface, abstract class, and open class
- `app/` — Consumer using fakes from core's testFixtures

---

## Next Steps

- **[Multi-Module (KMP)](multi-module.md)** — Collector module approach for KMP projects
- **[Plugin Configuration](plugin-configuration.md)** — All configuration options
- **[Samples](../examples/index.md)** — All sample projects
