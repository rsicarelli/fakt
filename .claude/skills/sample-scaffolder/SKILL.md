---
name: sample-scaffolder
description: Scaffolds new Fakt sample projects with correct structure, build scripts, and CI integration. Supports JVM, KMP, Android, multi-module, test-fixtures, and fake-publishing types. Use when creating a new sample project, adding a demo for a new feature, setting up a new test project, or scaffolding a demonstration project. Make sure to use this skill whenever a new sample directory needs to be created — it handles build scripts, CI integration, and settings.gradle.kts registration that are easy to miss manually.
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, AskUserQuestion
---

# Sample Project Scaffolder

Creates new sample projects under `samples/` following confirmed patterns from existing samples.

## Instructions

### 1. Determine Sample Type

Use `AskUserQuestion` to select type:
- **JVM single-module** — simplest, `src/main` + `src/test`
- **KMP single-module** — `commonMain`/`commonTest`, all targets
- **KMP multi-module** — multiple submodules with `-fakes` collectors
- **Android single-module** — Android library with Fakt
- **JVM test-fixtures** — Gradle `java-test-fixtures` plugin
- **Fake publishing** — publisher + consumer via Maven coordinates

Ask for: sample name (kebab-case), purpose/feature being demonstrated.

### 2. Create Directory Structure

**JVM single-module**:
```
samples/{name}/
├── build.gradle.kts
├── settings.gradle.kts
└── src/
    ├── main/kotlin/com/rsicarelli/fakt/samples/{camelName}/
    │   ├── models/
    │   └── scenarios/
    └── test/kotlin/com/rsicarelli/fakt/samples/{camelName}/
```

**KMP single-module**:
```
samples/{name}/
├── build.gradle.kts
├── settings.gradle.kts
└── src/
    ├── commonMain/kotlin/com/rsicarelli/fakt/samples/{camelName}/
    │   ├── models/
    │   └── scenarios/
    └── commonTest/kotlin/com/rsicarelli/fakt/samples/{camelName}/
```

**KMP multi-module**:
```
samples/{name}/
├── build.gradle.kts          (root: apply plugins false)
├── settings.gradle.kts       (include submodules)
├── core/{module}/
│   ├── build.gradle.kts
│   └── src/commonMain/...
└── core/{module}-fakes/
    ├── build.gradle.kts      (collectFakesFrom)
    └── src/commonMain/...
```

### 3. Generate `settings.gradle.kts`

All samples use this template (adjust relative paths for nesting depth):

```kotlin
rootProject.name = "{name}"

pluginManagement {
    includeBuild("../../build-logic")
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
```

Rules:
- `mavenLocal()` FIRST in both repository blocks
- `includeBuild` points to `build-logic` (adjust depth: `../../` for top-level, `../../../` for nested)
- Version catalog always references shared `libs.versions.toml`

> **Note**: JVM samples may use a simpler repository setup without Central Portal Snapshots. Always check an existing sample of the same type for the exact template. `rootProject.name` placement (top vs bottom) and `@Suppress("UnstableApiUsage")` usage also vary between samples.

### 4. Generate `build.gradle.kts`

**JVM single-module**:
```kotlin
import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    id("fakt-sample-jvm")
    alias(libs.plugins.fakt)
}

dependencies {
    implementation(libs.fakt.annotations)
    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}

fakt {
    logLevel.set(LogLevel.DEBUG)
}
```

**KMP single-module**:
```kotlin
import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    id("fakt-sample-kmp")
    alias(libs.plugins.fakt)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.fakt.annotations)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
    }
}

fakt {
    logLevel.set(LogLevel.DEBUG)
}
```

**Android single-module**: use `fakt-sample-android` convention plugin.

**JVM test-fixtures**: add `java-test-fixtures` plugin, `fakt { useGradleTestFixtures.set(true) }`.

**KMP multi-module root**: apply plugins `false`, submodules apply independently.

**Collector module** (multi-module `-fakes`):
```kotlin
plugins {
    id("fakt-sample-kmp")
    id("com.rsicarelli.fakt")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.{module})
                implementation(libs.coroutines)
            }
        }
    }
}

fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.DEBUG)
    @OptIn(com.rsicarelli.fakt.gradle.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.{module})
}
```

### 5. Create Example Interface

```kotlin
// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.{camelName}.scenarios

import com.rsicarelli.fakt.Fake

@Fake
interface {ExampleInterface} {
    fun getData(): String
    suspend fun fetchItems(): List<String>
}
```

### 6. Add to CI

Edit `.github/actions/test-samples/action.yml`:

**JVM/Android samples** — add `build` step:
```yaml
- name: "Test {name} sample"
  run: ./gradlew -p samples/{name} build
```

**KMP samples** — add `allTests` + `check`:
```yaml
- name: "Test {name} sample"
  run: ./gradlew -p samples/{name} allTests --continue

- name: "Check {name} sample"
  run: ./gradlew -p samples/{name} check
```

### 7. Verify

```bash
make publish-local
./gradlew -p samples/{name} build  # or allTests for KMP
```

Convention plugins available: `fakt-sample-jvm`, `fakt-sample-kmp`, `fakt-sample-android`.

## Related Skills

- `workflow` — full development cycle including sample creation
- `compilation` — diagnose build failures in new samples
- `feature-option` — new features often need a sample to demonstrate
