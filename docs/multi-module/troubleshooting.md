# Troubleshooting

Common issues and solutions for multi-module setups.

---

## Diagnosis Tools

### Enable Debug Logging

```kotlin
fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.DEBUG)
}
```

Rebuild to see detailed output:

```bash
./gradlew :core:analytics-fakes:clean :core:analytics-fakes:build --info
```

### Check Task Execution

View task dependency graph:

```bash
./gradlew :core:analytics-fakes:build --dry-run
```

### Use Build Scans

```bash
./gradlew build --scan
```

---

## Common Issues

### Issue 1: "No fakes found in source module"

**Error**:
```
No fakes found in source module 'analytics'.
Verify that source module has @Fake annotated interfaces.
```

**Causes**:
1. Source module has no `@Fake` interfaces
2. Fakes not generated (compilation failed)
3. Wrong module path in `collectFakesFrom()`

**Diagnosis**:

```bash
# Check for @Fake annotations
grep -r "@Fake" core/analytics/src/

# Verify fakes were generated  
ls core/analytics/build/generated/fakt/

# Check for compilation errors
./gradlew :core:analytics:build
```

**Solutions**:

```kotlin
// ✅ CORRECT: Collect from producer (has @Fake interfaces)
fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)  
}

// ❌ WRONG: Collecting from collector (no @Fake interfaces)
fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analyticsFakes)  // Wrong!
}
```

---

### Issue 2: "Unresolved reference: fakeXxx"

**Error**:
```kotlin
// In test file
val fake = fakeAnalytics { }  // ← Unresolved reference
```

**Causes**:
1. Consumer doesn't depend on collector module
2. Collector module not built
3. IDE not synced
4. Wrong import

**Diagnosis**:

```bash
# Check dependency in consumer
grep "analyticsFakes" app/build.gradle.kts

# Verify collector was built
ls core/analytics-fakes/build/generated/collected-fakes/

# Check for factory function
grep -r "fun fakeAnalytics" core/analytics-fakes/build/
```

**Solutions**:

```kotlin
// 1. Add dependency to consumer
kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.analyticsFakes)
    }
}

// 2. Sync Gradle and rebuild
./gradlew --refresh-dependencies build

// 3. Invalidate IDE caches
// File → Invalidate Caches → Invalidate and Restart
```

---

### Issue 3: Targets Mismatch

**Error**:
```
Cannot find source set 'iosMain' for target 'iosX64'
```

**Cause**: Collector has different targets than producer

**Diagnosis**:

```kotlin
// Check producer targets
// core/analytics/build.gradle.kts
kotlin {
    jvm()
    iosArm64()  // ← Producer has this
}

// Check collector targets
// core/analytics-fakes/build.gradle.kts
kotlin {
    jvm()
    // Missing: iosArm64()  ← Collector doesn't!
}
```

**Solution**: Collector MUST have ALL producer's targets

```kotlin
// core/analytics-fakes/build.gradle.kts
kotlin {
    jvm()
    iosArm64()  // ✅ Added
    iosX64()
    iosSimulatorArm64()
}
```

---

### Issue 4: Wrong Platform Placement

**Symptom**: Fake ends up in wrong source set (e.g., `commonMain` instead of `jvmMain`)

**Cause**: Package doesn't contain platform identifier

**Diagnosis**:

```kotlin
// Check generated fake's package
// core/analytics/build/generated/fakt/jvmTest/kotlin/DatabaseFake.kt
package com.example.database  // ← No "jvm" segment!
```

**Solution**: Use platform identifier in package name

```kotlin
// ✅ CORRECT: Platform in package
package com.example.jvm.database  // → jvmMain/
package com.example.ios.camera    // → iosMain/

// ❌ WRONG: No platform identifier
package com.example.database  // → commonMain/ (fallback)
```

---

### Issue 5: Circular Dependencies

**Error**:
```
Circular dependency between:
:features:payment
:features:user
```

**Cause**: Feature A fakes need Feature B, Feature B fakes need Feature A

**Solution**: Extract shared interfaces to core modules

```
Before (circular):
features/payment → features/user
features/user → features/payment

After (fixed):
core/payment-api @Fake interface PaymentProvider
core/user-api @Fake interface UserProvider

features/payment → core/payment-api, core/user-api
features/user → core/user-api, core/payment-api
```

---

### Issue 6: Missing Transitive Dependencies

**Error**:
```kotlin
// In test
val fake = fakeAnalytics { }  // Compiles

// But runtime error:
NoClassDefFoundError: kotlinx/coroutines/CoroutineScope
```

**Cause**: Collector didn't declare coroutines dependency

**Solution**:

```kotlin
// core/analytics-fakes/build.gradle.kts
kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.analytics)
        implementation(libs.coroutines)  // ✅ Add this
    }
}
```

**How to identify needed dependencies**:

```bash
# Inspect generated code
cat core/analytics-fakes/build/generated/collected-fakes/commonMain/kotlin/FakeAnalyticsImpl.kt

# Look for imports:
import kotlinx.coroutines.*  # → Need coroutines
import kotlinx.serialization.*  # → Need serialization
```

---

### Issue 7: IDE Not Finding Fakes

**Symptom**: Autocomplete doesn't suggest `fakeXxx()`, but code compiles

**Causes**:
1. IDE not synced with Gradle
2. Generated sources not indexed
3. Stale IDE caches

**Solutions**:

```bash
# 1. Reload Gradle projects
# File → Reload All Gradle Projects

# 2. Invalidate caches
# File → Invalidate Caches → Invalidate and Restart

# 3. Rebuild project
./gradlew clean build

# 4. Check generated sources are registered
ls core/analytics-fakes/build/generated/collected-fakes/
```

---

### Issue 8: Configuration Cache Failures

**Error**:
```
Configuration cache problems found:
- field 'project' from type 'FaktGradleSubplugin'
```

**Cause**: Using configuration cache with older Fakt version

**Solution**: Update to Fakt 1.0.0-SNAPSHOT+ (configuration cache compatible)

```kotlin
// gradle.properties
org.gradle.configuration-cache=true
```

---

## Error Messages Reference

### "Source project not found"

```
Source project ':core:analytics' not found.
Verify module exists and is included in settings.gradle.kts.
```

**Fix**: Add module to `settings.gradle.kts`

```kotlin
include(":core:analytics")
```

### "Collector and producer targets mismatch"

```
Collector has targets [jvm, js] but producer has [jvm, ios].
All producer targets must be present in collector.
```

**Fix**: Add missing targets to collector

```kotlin
kotlin {
    jvm()
    js()
    iosArm64()  // ✅ Add this
}
```

### "@OptIn annotation missing"

```
Multi-module APIs require opt-in with @OptIn(ExperimentalFaktMultiModule::class)
```

**Fix**: Add opt-in annotation

```kotlin
fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

---

## Advanced Debugging

### Enable TRACE Logging

```kotlin
fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.TRACE)
}
```

Shows:
- File-by-file collection
- Platform detection reasoning
- IR generation details
- Task execution timing

### Task Dependency Visualization

```bash
# Show task graph
./gradlew :app:test --dry-run

# Execution timeline
./gradlew :app:test --scan
# → View timeline in build scan
```

### Inspect Generated Code

```bash
# View collected fake
cat core/analytics-fakes/build/generated/collected-fakes/commonMain/kotlin/com/example/FakeAnalyticsImpl.kt

# Compare with original
cat core/analytics/build/generated/fakt/commonTest/kotlin/com/example/FakeAnalyticsImpl.kt
```

---

## Verification Checklist

Before reporting issues, verify:

- [ ] Producer module has `@Fake` annotated interfaces
- [ ] Producer builds successfully (`./gradlew :core:analytics:build`)
- [ ] Fakes generated in producer (`ls core/analytics/build/generated/fakt/`)
- [ ] Collector depends on producer with correct path
- [ ] Collector has ALL producer's KMP targets
- [ ] Collector declares transitive dependencies
- [ ] Consumer depends on collector module
- [ ] Gradle synced in IDE
- [ ] Using Fakt 1.0.0-SNAPSHOT+
- [ ] Kotlin 2.2.10+

---

## Getting Help

If issues persist:

1. **Enable DEBUG logging** and capture output
2. **Create minimal reproduction** (single producer + collector + consumer)
3. **Report on GitHub**: [github.com/rsicarelli/fakt/issues](https://github.com/rsicarelli/fakt/issues)

**Include in report**:
- Fakt version
- Kotlin version
- Gradle version
- KMP targets
- Full error message
- DEBUG/TRACE log output
- Minimal reproduction repository

---

## Next Steps

- [Migration Guide](migration.md) - Migrate from single-module
- [Technical Reference](reference.md) - FakeCollectorTask internals
- [Getting Started](getting-started.md) - Setup guide
