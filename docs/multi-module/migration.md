# Migration Guide

Migrate from single-module to multi-module fake consumption.

---

## When to Migrate

### Decision Criteria

✅ **Migrate to Multi-Module When**:
- 3+ modules need the same fakes
- Publishing fake artifacts
- Strict module boundaries (DDD, Clean Architecture)
- Large teams (dedicated module ownership)

❌ **Stay Single-Module When**:
- 1-2 modules total
- Fakes only used locally
- Small team or prototype
- Prefer simplicity over reuse

### Cost/Benefit Analysis

| Aspect | Single-Module | Multi-Module |
|--------|---------------|--------------|
| **Setup Time** | 0 minutes | ~30-60 minutes |
| **Build Modules** | N modules | N + N collector modules |
| **Build Time** | Baseline | +5-10ms per collector |
| **Fake Reuse** | None | Full cross-module reuse |
| **Publishability** | No | Yes (Maven, etc.) |

---

## Migration Strategies

### Strategy 1: Big Bang (All at Once)

**Timeline**: 1-2 days

**Process**:
1. Create all collector modules
2. Update all consumer dependencies
3. Test entire project
4. Deploy together

**Best for**: Small projects (< 10 modules), dedicated migration time

---

### Strategy 2: Gradual (Module by Module)

**Timeline**: 1-4 weeks

**Process**:
1. **Week 1**: Core infrastructure (logger, analytics, etc.)
2. **Week 2**: Foundation features (auth, storage)
3. **Week 3**: Business features
4. **Week 4**: Polish, optimize, document

**Best for**: Large projects, continuous delivery, risk aversion

---

### Strategy 3: Hybrid (Mixed)

Keep some single-module, migrate others

**Process**:
- Migrate frequently-shared fakes (core/logger, core/analytics)
- Keep feature-specific fakes single-module

**Best for**: Mixed module ownership, gradual rollout

---

## Step-by-Step Migration

### Phase 1: Preparation (30 minutes)

#### 1. Enable Type-Safe Project Accessors (Optional)

```kotlin
// settings.gradle.kts
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
```

Sync Gradle to generate `projects.*` accessors.

#### 2. Identify Fake Candidates

```bash
# Find all @Fake interfaces
find . -name "*.kt" -exec grep -l "@Fake" {} \;

# Group by module
# - High reuse: core/logger (10+ consumers)
# - Medium reuse: core/auth (5+ consumers)
# - Low reuse: features/profile (1-2 consumers)
```

**Prioritize**: High reuse modules first

---

### Phase 2: Create Collectors (15 minutes per module)

#### 1. Create Collector Module Directory

```bash
# Example: core/analytics → core/analytics-fakes
mkdir -p core/analytics-fakes/src
```

#### 2. Create build.gradle.kts

```kotlin
// core/analytics-fakes/build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.20"
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

kotlin {
    // MUST match producer's targets
    jvm()
    iosArm64()
    // ... all other targets from producer

    sourceSets.commonMain.dependencies {
        // Use api() to expose types
        api(projects.core.analytics)
        
        // Add transitive dependencies
        implementation(libs.coroutines)
    }
}

fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

#### 3. Register in settings.gradle.kts

```kotlin
include(":core:analytics-fakes")
```

#### 4. Build and Verify

```bash
./gradlew :core:analytics-fakes:build

# Verify fakes collected
ls core/analytics-fakes/build/generated/collected-fakes/
```

---

### Phase 3: Update Consumers (5 minutes per module)

#### 1. Update Dependencies

```kotlin
// Before (single-module)
kotlin {
    sourceSets.commonTest.dependencies {
        // No dependency - fakes generated locally
    }
}

// After (multi-module)
kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.analyticsFakes)
    }
}
```

#### 2. Test Imports

```kotlin
// Before (single-module)
import com.example.core.analytics.fakeAnalytics  // Local generated

// After (multi-module)
import com.example.core.analytics.fakeAnalytics  // From collector dependency
// ↑ Import path unchanged! Fakes use same package as original interface
```

#### 3. Verify Tests Pass

```bash
./gradlew :app:test
```

---

### Phase 4: Cleanup (Optional)

#### 1. Remove Unused Test Code

If producer module no longer needs its own fakes:

```kotlin
// core/analytics/build.gradle.kts
// Can remove Fakt plugin if only used for generation
// Keep if producer's own tests use fakes
```

#### 2. Update Documentation

Update README, wiki, or docs to reflect multi-module setup.

---

## Migration Examples

### Example 1: Core Logger (High Reuse)

**Before**:
```
core/logger/
└── src/
    ├── commonMain/kotlin/Logger.kt (@Fake)
    └── commonTest/kotlin/  (fakes generated here, not shared)

features/login/
└── src/commonTest/kotlin/  (no access to logger fakes)

features/checkout/
└── src/commonTest/kotlin/  (no access to logger fakes)
```

**After**:
```
core/logger/
└── src/commonMain/kotlin/Logger.kt (@Fake)

core/logger-fakes/
└── build/generated/collected-fakes/  (fakes collected here)

features/login/
└── build.gradle.kts: implementation(projects.core.loggerFakes)

features/checkout/
└── build.gradle.kts: implementation(projects.core.loggerFakes)
```

**Benefits**: 2+ feature modules now share logger fakes

---

### Example 2: Feature Module (Low Reuse)

**Decision**: Keep single-module (not worth migration overhead)

```
features/profile/
└── src/
    ├── commonMain/kotlin/ProfileService.kt (@Fake)
    └── commonTest/kotlin/  (fakes used only here)

# No collector module created
# Fakes stay local
```

---

## Rollback Procedure

If migration causes issues:

### 1. Revert Consumer Dependencies

```kotlin
// Remove collector dependency
kotlin {
    sourceSets.commonTest.dependencies {
        // implementation(projects.core.analyticsFakes)  // ← Comment out
    }
}
```

### 2. Re-enable Local Fake Generation

Producer module already generates fakes locally. Just use them:

```kotlin
// core/analytics/build.gradle.kts
// Plugin already present - fakes still generated locally
```

### 3. Rebuild

```bash
./gradlew clean build
```

### 4. Remove Collector Modules (Optional)

```bash
rm -rf core/analytics-fakes/
```

```kotlin
// settings.gradle.kts
// include(":core:analytics-fakes")  // ← Comment out
```

---

## Gradual Migration Timeline

### 4-Week Plan (Large Project)

**Week 1: Core Infrastructure**
- [ ] Create collectors for: logger, analytics, network, storage
- [ ] Update 3-5 high-priority consumers
- [ ] Test integration
- [ ] Document patterns

**Week 2: Foundation Features**
- [ ] Create collectors for: auth, config, database
- [ ] Update remaining core consumers
- [ ] Test cross-module dependencies

**Week 3: Business Features**
- [ ] Create collectors for: login, checkout, profile
- [ ] Update all feature module tests
- [ ] Remove local fake duplicates

**Week 4: Polish & Optimize**
- [ ] Tune build cache
- [ ] Optimize task dependencies
- [ ] Update CI/CD pipelines
- [ ] Team training & documentation

---

## Common Migration Patterns

### Pattern 1: Version Catalog Updates

```toml
# gradle/libs.versions.toml

# Before
[libraries]
core-analytics = { module = "com.example:core-analytics", version = "1.0.0" }

# After (add fake modules)
[libraries]
core-analytics = { module = "com.example:core-analytics", version = "1.0.0" }
core-analytics-fakes = { module = "com.example:core-analytics-fakes", version = "1.0.0" }
```

### Pattern 2: Convention Plugin

Create plugin for collector boilerplate:

```kotlin
// buildSrc/src/main/kotlin/fakt-collector.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

kotlin {
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()
}

// Apply to collectors
// core/analytics-fakes/build.gradle.kts
plugins {
    id("fakt-collector")
}

fakt {
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

---

## Next Steps

- [Getting Started](getting-started.md) - Multi-module setup guide
- [Troubleshooting](troubleshooting.md) - Common migration issues
- [Advanced Topics](advanced.md) - Performance optimization
