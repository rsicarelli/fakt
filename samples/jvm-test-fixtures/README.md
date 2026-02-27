# jvm-test-fixtures

Multi-module JVM sample demonstrating Fakt integration with [Gradle test fixtures](https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures).

## What this demonstrates

Fakt generates fakes into the `testFixtures` source set instead of `test`, enabling cross-module fake sharing without a dedicated `-fakes` collector module.

- **`core/`** — Defines `@Fake` annotated types (interface, abstract class, open class) and generates fakes into `testFixtures`
- **`app/`** — Consumes fakes from `core` via `testFixtures(projects.core)` dependency

## Project structure

```
jvm-test-fixtures/
├── core/
│   ├── src/main/kotlin/          # @Fake annotated types
│   │   ├── UserRepository.kt    # Interface
│   │   ├── OrderProcessor.kt    # Abstract class (suspend functions)
│   │   └── CacheManager.kt      # Open class
│   └── src/test/kotlin/          # Tests using fakes locally
├── app/
│   ├── src/main/kotlin/          # Service depending on core interfaces
│   └── src/test/kotlin/          # Tests using fakes from core's testFixtures
└── README.md
```

## Key configuration

### Producer module (`core/build.gradle.kts`)

```kotlin
plugins {
    `java-test-fixtures`
    alias(libs.plugins.fakt)
}

fakt {
    useGradleTestFixtures.set(true)
}
```

### Consumer module (`app/build.gradle.kts`)

```kotlin
dependencies {
    implementation(projects.core)
    testImplementation(testFixtures(projects.core))
}
```

## How it works

1. Fakt detects `java-test-fixtures` plugin and `useGradleTestFixtures.set(true)`
2. Generated fakes go to `build/generated/fakt/testFixtures/kotlin/` instead of `test/`
3. The `testFixtures` source set is configured to include the generated directory
4. `core`'s own tests can use the fakes (Gradle automatically makes testFixtures visible to test)
5. `app` imports fakes via `testFixtures(projects.core)` — no Fakt plugin needed in `app`

## Running

```bash
# Build everything
../../gradlew build

# Build only core (generates fakes to testFixtures)
../../gradlew :core:build

# Build only app (consumes fakes from core's testFixtures)
../../gradlew :app:build

# Run tests
../../gradlew test
```
