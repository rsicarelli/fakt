# Installation

Add Fakt to your Kotlin project in minutes. Works with both multiplatform and single-platform projects.

---

## Gradle Plugin (Recommended)

### Version Catalog

**`gradle/libs.versions.toml`:**

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

### Root Build File

**Root `build.gradle.kts`:**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.fakt) apply false
}
```

### Module Build File

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

## Single-Platform Projects

Fakt works with single-platform Kotlin projects too:

### JVM-Only

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

dependencies {
    implementation(libs.fakt.runtime)
}
```

### Android-Only

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

## Requirements

| Requirement      | Version    |
|------------------|------------|
| **Kotlin**       | 2.2.10+    |
| **Gradle**       | 8.0+       |
| **JVM**          | 11+        |

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
| 1.0.0-SNAPSHOT   | 2.2.10 - 2.2.30        |

Fakt follows forward compatibility on a best-effort basis (usually N+.2 minor versions).

---

## Next Steps

- [Quick Start](quick-start.md) - Your first fake in 5 minutes
- [Features](features.md) - What Fakt supports
- [Configuration](../reference/configuration.md) - Plugin options
