# Advanced Topics

Deep technical details for power users and complex multi-module setups.

---

## Platform Detection

Fakt's `FakeCollectorTask` automatically detects target platforms by analyzing package structures.

### How It Works

**Algorithm**:
1. Extract package declaration from generated fake file
2. Split package into segments (e.g., `com.example.ios.auth` → `["com", "example", "ios", "auth"]`)
3. Match segments against available KMP source sets
4. Prioritize shortest match (most general)
5. Fallback to `commonMain` if no match

**Examples**:

```kotlin
// Input: package com.example.jvm.database
// Available: [commonMain, jvmMain, iosMain]
// Matches: jvmMain (from "jvm" segment)
// Output: jvmMain/kotlin/com/example/jvm/database/FakeDatabaseImpl.kt

// Input: package com.example.ios.camera  
// Available: [commonMain, iosMain, iosArm64Main, iosX64Main]
// Matches: iosMain (7 chars), iosArm64Main (13 chars), iosX64Main (10 chars)
// Selected: iosMain (shortest = most general)
// Output: iosMain/kotlin/com/example/ios/camera/FakeCameraImpl.kt

// Input: package com.example.shared.network
// Available: [commonMain, jvmMain, jsMain]
// Matches: (none - "shared" doesn't match any source set)
// Output: commonMain/kotlin/com/example/shared/network/FakeNetworkImpl.kt (fallback)
```

### Package Naming Conventions

Use platform identifiers in package names for automatic detection:

| Platform     | Package Segment | Example |
|--------------|-----------------|---------|
| **JVM**      | `jvm.*`         | `com.example.jvm.database` |
| **Android**  | `android.*`     | `com.example.android.storage` |
| **iOS**      | `ios.*`         | `com.example.ios.camera` |
| **JS**       | `js.*`          | `com.example.js.browser` |
| **Native**   | `native.*`      | `com.example.native.file` |
| **WASM**     | `wasm.*`        | `com.example.wasm.api` |
| **Common**   | `shared.*`, `common.*` | `com.example.shared.logger` (fallback) |

---

## Build System Integration

### Task Dependencies

FakeCollectorTask automatically wires dependencies:

```
Producer Compilation
:core:analytics:compileKotlinMetadata
:core:analytics:compileKotlinJvm
:core:analytics:compileKotlinIos
    ↓ dependsOn
Collector Collection
:core:analytics-fakes:collectFakes
    ↓ dependsOn
Collector Compilation
:core:analytics-fakes:compileKotlinJvm
:core:analytics-fakes:compileKotlinIos
    ↓ testImplementation dependency
Consumer Compilation
:app:compileTestKotlinJvm
:app:compileTestKotlinIos
```

### Incremental Compilation

Fakt supports Gradle incremental compilation:

- **First build**: Full fake generation + collection (~40ms for 100 interfaces)
- **No changes**: Skip generation and collection (cached)
- **Producer changed**: Regenerate fakes + recollect (~40ms)
- **Collector config changed**: Recollect only (~10ms)
- **Consumer changed**: No regeneration/recollection (uses compiled artifacts)

### Configuration Cache Compatibility

Fakt is fully compatible with Gradle configuration cache:

```bash
./gradlew build --configuration-cache
```

FakeCollectorTask uses:
- `Property<T>` for all configuration
- File collections instead of file paths
- No direct `Project` references at execution time

---

## Performance Optimization

### Build Time Impact

**Typical overhead per fake module:**

| Scenario               | Time per Module | Total (10 modules) |
|------------------------|-----------------|---------------------|
| Clean build (first)    | ~40-50ms        | ~400-500ms          |
| Incremental (changed)  | ~10-15ms        | ~100-150ms          |
| Incremental (cached)   | ~1-2ms          | ~10-20ms            |

### Optimization Strategies

#### 1. Enable Build Cache

```kotlin
// gradle.properties
org.gradle.caching=true
org.gradle.configuration-cache=true
```

```bash
./gradlew build --build-cache
```

#### 2. Parallel Compilation

```kotlin
// gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=8
```

#### 3. Increase Heap Size

```kotlin
// gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

#### 4. Use LogLevel.QUIET in CI

```kotlin
fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.QUIET)
}
```

### Analyzing Build Performance

```bash
# Generate build scan
./gradlew build --scan

# Profile build
./gradlew build --profile

# Check task times
./gradlew build --info | grep "Execution time"
```

---

## Publishing Fake Modules

Collector modules are standard Gradle modules and can be published to repositories.

### Publishing to Maven Local

```bash
./gradlew :core:analytics-fakes:publishToMavenLocal
```

### Publishing to Maven Central

Add to collector's `build.gradle.kts`:

```kotlin
plugins {
    `maven-publish`
    signing
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])

            groupId = "com.example"
            artifactId = "analytics-fakes"
            version = "1.0.0"

            pom {
                name.set("Analytics Fakes")
                description.set("Test fakes for Analytics interface")
                url.set("https://github.com/example/project")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("example")
                        name.set("Example Developer")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/example/project.git")
                    url.set("https://github.com/example/project")
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrhUsername") as String?
                password = project.findProperty("ossrhPassword") as String?
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
```

### Using Published Fakes

```kotlin
// Consumer module
dependencies {
    commonTestImplementation("com.example:analytics-fakes:1.0.0")
}
```

---

## IDE Integration

### IntelliJ IDEA / Android Studio

Fakt-generated fakes are automatically indexed by IDEs.

**Setup**:
1. Build project: `./gradlew build`
2. Sync Gradle: **File → Reload All Gradle Projects**
3. Verify: Type `fake` and check autocomplete suggestions

**Troubleshooting**:
- **Fakes not appearing**: File → Invalidate Caches → Invalidate and Restart
- **Wrong platform fakes**: Check package naming conventions
- **Compilation errors**: Verify collector targets match producer

### Debugging Generated Code

Navigate to generated fakes:

```
core/analytics-fakes/build/generated/collected-fakes/
├── commonMain/kotlin/  # IDE can navigate here
├── jvmMain/kotlin/
└── iosMain/kotlin/
```

Add breakpoints in generated code for debugging.

---

## Multi-Repository Setups

For projects spanning multiple Git repositories:

### Repository A (Shared Infrastructure)

```kotlin
// repo-a/core/analytics/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = "com.company.shared"
            artifactId = "analytics"
            version = "2.0.0"
        }
    }
}

// repo-a/core/analytics-fakes/build.gradle.kts
fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = "com.company.shared"
            artifactId = "analytics-fakes"
            version = "2.0.0"
        }
    }
}
```

### Repository B (Consumer)

```kotlin
// repo-b/app/build.gradle.kts
dependencies {
    implementation("com.company.shared:analytics:2.0.0")
    commonTestImplementation("com.company.shared:analytics-fakes:2.0.0")
}
```

---

## Custom Collector Configurations

### Collecting Multiple Producers

One collector can aggregate multiple producers:

```kotlin
// Not currently supported - create separate collectors instead
// Future enhancement: fakt.collectFakesFrom(projects.core.analytics, projects.core.logger)

// Current pattern: One collector per producer
// :core:analytics-fakes → collects :core:analytics
// :core:logger-fakes → collects :core:logger
```

### Excluding Specific Fakes

Generated fakes are all-or-nothing from a producer. To exclude specific fakes:

**Option 1**: Don't add `@Fake` annotation
**Option 2**: Create separate producer modules for different fake groups

---

## Advanced Dependency Management

### Transitive Dependencies

Collector modules must declare ALL dependencies used by generated fakes:

```kotlin
// If generated fakes use:
// - suspend functions → kotlinx-coroutines
// - Flow → kotlinx-coroutines
// - Result<T> → kotlin-stdlib (already included)
// - Custom types from other modules → add those modules

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.analytics)  // Original interfaces
        implementation(libs.coroutines)  // Used by suspend fakes
        implementation(projects.core.logger)  // Used by fakes
    }
}
```

### Avoiding Circular Dependencies

**Problem**: Feature A fakes need Feature B, Feature B fakes need Feature A

**Solution**: Extract shared interfaces to core module

```
Before (circular):
features/payment → depends on features/user
features/user → depends on features/payment

After (hierarchical):
core/payment-api @Fake interface PaymentProvider
core/user-api @Fake interface UserProvider
features/payment → depends on core/payment-api, core/user-api
features/user → depends on core/user-api, core/payment-api
```

---

## Experimental Features

### Platform-Specific Collector Targets

Limit collector to specific platforms:

```kotlin
kotlin {
    // Only JVM and iOS (skip JS, Native, etc.)
    jvm()
    iosArm64()

    // Collector will only generate for these targets
}
```

Use when producer supports many platforms but you only test on a subset.

---

## Next Steps

- [Troubleshooting](troubleshooting.md) - Debug common issues
- [Technical Reference](reference.md) - FakeCollectorTask deep dive
- [Migration Guide](migration.md) - Single-module → Multi-module
