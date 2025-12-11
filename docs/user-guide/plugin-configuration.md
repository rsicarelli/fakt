# Configuration Guide

Complete guide to configuring the Fakt Gradle plugin.

---

## Log Level Configuration

Control compilation output verbosity:

```kotlin
import com.rsicarelli.fakt.compiler.api.LogLevel

fakt {
    logLevel.set(LogLevel.INFO)  // Default
}
```

### Available Levels

**`LogLevel.QUIET`** - Zero output

- Use in CI/CD pipelines
- Minimal noise in build logs
- Recommended for production builds

```kotlin
fakt {
    logLevel.set(LogLevel.QUIET)
}
```

**`LogLevel.INFO`** - Concise summary (default)

- Shows generated fake count
- Cache hit rates
- Compilation time
- Ideal for local development

```kotlin
fakt {
    logLevel.set(LogLevel.INFO)
}
```

**Example output:**
```
GENERATION: 121 fakes (avg 333µs/fake, 3,867 LOC)
CACHE: 100% hit rate (121/121 from cache)
TOTAL: 40ms
```

**`LogLevel.DEBUG`** - Detailed breakdown

- FIR phase details
- IR generation per file
- Platform detection reasoning
- Performance metrics

```kotlin
fakt {
    logLevel.set(LogLevel.DEBUG)
}
```

Use for troubleshooting or understanding plugin behavior.

---

## Multi-Module Configuration

Enable cross-module fake collection:

```kotlin
fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

### Opt-In Requirement

Multi-module support requires explicit opt-in:

```kotlin
import com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule

fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

!!! info "Experimental API"
    Multi-module support is marked `@ExperimentalFaktMultiModule`. The API is production-ready but may evolve based on feedback.

### Complete Multi-Module Setup

**Producer module** (`core/analytics/build.gradle.kts`):
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

kotlin {
    jvm()
    iosArm64()

    sourceSets.commonMain.dependencies {
        implementation(libs.fakt.runtime)
    }
}

// Fakes generated automatically in commonTest/
```

**Collector module** (`core/analytics-fakes/build.gradle.kts`):
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

kotlin {
    // MUST have ALL producer's targets
    jvm()
    iosArm64()

    sourceSets.commonMain.dependencies {
        api(projects.core.analytics)
    }
}

fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

**Consumer module** (`app/build.gradle.kts`):
```kotlin
kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.analyticsFakes)
    }
}
```

See **[Multi-Module Guide](../multi-module/index.md)** for architecture details.

---

## IDE Integration

### IntelliJ IDEA / Android Studio

Fakt-generated code appears in `build/generated/fakt/` and is automatically indexed.

**Enable K2 Mode for Better Autocomplete:**

1. **Settings** → **Languages & Frameworks** → **Kotlin**
2. Enable **K2 mode**
3. Restart IDE

K2 mode improves factory function autocomplete and type inference.

### Generated Sources Location

| Source Set | Generated Output |
|-----------|------------------|
| `commonTest/` | `build/generated/fakt/commonTest/kotlin/` |
| `jvmTest/` | `build/generated/fakt/jvmTest/kotlin/` |
| `iosTest/` | `build/generated/fakt/iosTest/kotlin/` |
| `androidUnitTest/` | `build/generated/fakt/androidUnitTest/kotlin/` |

---

## CI/CD Best Practices

### Use QUIET Mode in Pipelines

```kotlin
// build.gradle.kts
fakt {
    logLevel.set(
        if (System.getenv("CI") == "true") {
            LogLevel.QUIET
        } else {
            LogLevel.INFO
        }
    )
}
```

### Gradle Configuration Cache

Fakt 1.0.0-SNAPSHOT+ supports Gradle configuration cache:

```kotlin
// gradle.properties
org.gradle.configuration-cache=true
```

### Verify Cache Hit Rates

Monitor cache effectiveness in CI:

```kotlin
fakt {
    logLevel.set(LogLevel.INFO)
}
```

Check logs for:
```
CACHE: 100% hit rate (121/121 from cache)
```

Low hit rates indicate unnecessary recompilation.

---

## Version Catalog Integration

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
    sourceSets.commonMain.dependencies {
        implementation(libs.fakt.runtime)
    }
}
```

---

## Performance Tuning

### Check Build Impact

Use `LogLevel.INFO` to measure overhead:

```
DISCOVERY: 1ms (100 interfaces, 21 classes)
GENERATION: 39ms (121 new fakes, avg 333µs/fake)
TOTAL: 40ms
```

### Optimize Large Projects

For projects with 100+ fakes:

1. **Enable incremental compilation** (on by default)
2. **Use configuration cache**
3. **Monitor cache hit rates**
4. **Use QUIET mode in CI**

See **[Performance Guide](performance.md)** for detailed benchmarks.

---

## Troubleshooting Configuration

### Verify Plugin Applied

```bash
./gradlew :module:tasks --all | grep fakt
```

Should show Fakt-related tasks.

### Check Effective Configuration

```bash
./gradlew :module:build --info | grep -i fakt
```

Shows actual plugin settings used during compilation.

### Common Issues

**"Cannot resolve fakt { }"**

- Ensure plugin is applied in module's `build.gradle.kts`
- Check version catalog references are correct

**"ExperimentalFaktMultiModule not found"**

- Add import: `import com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule`
- Verify Fakt version is 1.0.0-SNAPSHOT+

---

## Next Steps

- **[Multi-Module Setup](../multi-module/index.md)** - Cross-module fakes
- **[Performance Guide](performance.md)** - Build time optimization
- **[Troubleshooting](../troubleshooting.md)** - Common configuration issues
- **[API Reference](../reference/api.md)** - Generated API details
