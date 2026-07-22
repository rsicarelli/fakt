<!--
  Copyright (C) 2025 Rodrigo Sicarelli
  SPDX-License-Identifier: Apache-2.0
-->
# Android test fixtures sample

Demonstrates Fakt generating fakes into an **Android library's native `testFixtures`** source set
(AGP `android { testFixtures { enable = true } }`) and another Android module consuming them —
the AGP analogue of the JVM `java-test-fixtures` flow.

Covers discussions
[#87](https://github.com/rsicarelli/fakt/discussions/87) and
[#109](https://github.com/rsicarelli/fakt/discussions/109).

## Layout

| Module | Role | Key config |
|--------|------|------------|
| `:producer` | Android library that declares `@Fake` interfaces | `id("fakt-sample-android-fixtures")` (enables `testFixtures`), `fakt { useGradleTestFixtures.set(true) }` |
| `:consumer` | Android library that USES the fakes | `testImplementation(testFixtures(projects.producer))`, **no** Fakt plugin |

Fakt runs on `:producer`'s `main` compilation and writes the generated fakes to
`producer/build/generated/fakt/testFixtures/kotlin/`. AGP compiles them per variant
(`compileDebugTestFixturesKotlin` / `compileReleaseTestFixturesKotlin`) and publishes them in the
producer's `testFixtures` artifact. `:consumer`'s unit tests then instantiate
`fakeUserRepository { }` without applying the Fakt plugin themselves.

## Requirements

- **AGP 8.x** needs `android.experimental.enableTestFixturesKotlinSupport=true` in
  `gradle.properties` so the `testFixtures` source set compiles Kotlin (already set here). On
  **AGP 9.0+** this is the default and the property is unnecessary.
- The consumer must be an **Android** module: a plain JVM module cannot resolve an Android
  `testFixtures` variant.

## Run

```bash
make publish-local
./gradlew -p samples/android-test-fixtures build
```
