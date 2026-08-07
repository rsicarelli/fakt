# Snapshot Builds

Every merge to `main` publishes a `-SNAPSHOT` build to **Maven Central Snapshots**. Use it to try
fixes before they land in a tagged release.

!!! note
    Snapshots are **not** listed on the Maven Central *artifact* page (that page only shows released
    versions). Browse or verify them in the snapshots repository directly:
    [maven-metadata.xml](https://central.sonatype.com/repository/maven-snapshots/com/rsicarelli/fakt/com.rsicarelli.fakt.gradle.plugin/maven-metadata.xml).

The current snapshot version is **`1.0.0-beta12-SNAPSHOT`**.

---

## 1. Add the snapshots repository

The repo must be in **both** blocks: `pluginManagement` (so the `plugins {}` block finds the
plugin) **and** `dependencyResolutionManagement` (so the `annotations` dependency resolves).

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() } // optional: only look here for -SNAPSHOT versions
        }
        gradlePluginPortal()
        mavenCentral()
        google() // only needed for Android
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() }
        }
        mavenCentral()
        google() // only needed for Android
    }
}
```

## 2. Apply the plugin at the snapshot version

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.4.10" // or kotlin("multiplatform")
    id("com.rsicarelli.fakt") version "1.0.0-beta12-SNAPSHOT"
}

dependencies {
    // The @Fake annotation.
    implementation("com.rsicarelli.fakt:annotations:1.0.0-beta12-SNAPSHOT")

    // Generated fakes track call history via kotlinx-coroutines StateFlow, so it must be on the
    // classpath that compiles them (your test source set).
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(kotlin("test"))
}
```

## 3. Use it

```kotlin
// src/main — annotate an interface
@Fake
interface Greeter {
    fun greet(name: String): String
}

// src/test — the plugin generated fakeGreeter { } for you
class GreeterTest {
    @Test
    fun example() {
        val greeter = fakeGreeter { greet { name -> "Hi, $name!" } }
        assertEquals("Hi, Ada!", greeter.greet("Ada"))
    }
}
```

```bash
./gradlew test
```

---

## Troubleshooting

- **`Plugin ... was not found`** — the snapshots repo is missing from **`pluginManagement`** (step 1).
- **Stale snapshot** — Gradle caches snapshot metadata for 24h; force a refresh:
  ```bash
  ./gradlew test --refresh-dependencies
  ```
- **`unresolved reference: MutableStateFlow`** in a generated fake — add `kotlinx-coroutines-core`
  to the source set that compiles the fakes (step 2).

> A runnable end-to-end example lives in
> [`samples/snapshot-smoke`](https://github.com/rsicarelli/fakt/tree/main/samples/snapshot-smoke).
