<!--
  Copyright (C) 2025 Rodrigo Sicarelli
  SPDX-License-Identifier: Apache-2.0
-->
# android-single-module

An Android library with `@Fake` interfaces and plain unit tests in `src/test/kotlin`.

## Build types

Besides `debug` and `release`, the module declares two build types whose names contain another
variant's name:

```kotlin
android {
    buildTypes {
        create("debugMinified") { initWith(getByName("debug")) }
        create("preRelease") { initWith(getByName("release")) }
    }
}
```

Fakt generates one producer per variant (`faktGenerateAndroidjvmDebug`,
`faktGenerateAndroidjvmDebugMinified`, `faktGenerateAndroidjvmRelease`,
`faktGenerateAndroidjvmPreRelease`). Each variant's fakes reach only that variant's unit tests, so
`testDebugMinifiedUnitTest` and `testPreReleaseUnitTest` compile without redeclaring the `debug` or
`release` fakes (#164b).

## Run

Needs an Android SDK (`ANDROID_HOME`).

```bash
./gradlew -p samples/android-single-module build
```
