# Multi-Module Support: Frequently Asked Questions

**Last Updated**: 2025-10-05

---

## General Questions

### Q: What is multi-module support in Fakt?

Multi-module support allows you to use fakes generated in one module (e.g., `:foundation`) in tests of dependent modules (e.g., `:domain`, `:features`, `:app`) without manually copying code or creating additional modules.

**Example**:
```kotlin
// :foundation module
@Fake interface Logger

// :domain module tests
import foundation.fakeLogger  // ✅ Works with multi-module support!

val logger = fakeLogger { info { message -> println(message) } }
```

---

### Q: Why not just use dedicated test modules (`:foundation-fakes`)?

**Short answer**: Repository pollution and build convention conflicts.

**Long answer**:
- Creates double the number of modules (10 modules → 20 modules)
- Modifies `settings.gradle.kts` automatically (Git pollution)
- Plugin must know company-specific build conventions
- Ownership confusion (plugin-generated but user-maintained?)

Custom Source Sets approach keeps everything in the original module with zero new directories.

**See**: [COMPARISON-MATRIX.md](./COMPARISON-MATRIX.md) for full comparison.

---

### Q: How does this work under the hood?

1. **Convention plugin** creates a `fakes` source set in your module
2. **Compiler plugin** generates fake implementations to `src/fakes/kotlin/`
3. **Gradle capabilities** expose fakes as a consumable variant
4. **Consumer modules** declare capability requirement to access fakes

**Gradle variant resolution** automatically selects the fakes variant when you require the capability.

**See**: [TECHNICAL-REFERENCE.md](./TECHNICAL-REFERENCE.md) for deep dive.

---

### Q: Is this production-ready?

**Current status**: Design phase complete, implementation starting.

**Timeline**:
- Week 1 (Days 1-5): Convention plugin core
- Week 2 (Days 6-10): Compiler integration
- Week 3 (Days 11-15): Auto-wiring and polish

**Target**: Production-ready in 3 weeks.

**See**: [IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md) for detailed timeline.

---

## Setup and Configuration

### Q: How do I enable multi-module support?

**Step 1**: Apply convention plugin to modules that produce fakes:

```kotlin
// foundation/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("fakt-convention")  // Add this line
}
```

**Step 2**: Consume fakes in dependent modules:

```kotlin
// domain/build.gradle.kts
dependencies {
    commonTestImplementation(project(":foundation")) {
        capabilities {
            requireCapability("com.example:foundation-fakes:1.0.0")
        }
    }
}
```

**Future (Week 3)**: Auto-wiring will eliminate Step 2 - fakes will be automatically available!

---

### Q: Do I need to create any new modules?

**No!** That's the whole point. Zero new modules are created. Everything lives in your existing module structure.

---

### Q: Where are fakes generated?

**Current (single-module)**: `build/generated/fakt/test/kotlin/`

**Future (multi-module)**: `src/fakes/kotlin/` (and `src/jvmFakes/kotlin/`, `src/jsFakes/kotlin/`, etc.)

This change enables IDE indexing and cross-module consumption.

---

### Q: Will this modify my `settings.gradle.kts`?

**No!** Unlike dedicated modules approach, custom source sets approach never touches `settings.gradle.kts`.

Only files modified:
- Individual module's `build.gradle.kts` (to apply convention plugin)

---

## Kotlin Multiplatform

### Q: Does this work with Kotlin Multiplatform?

**Yes!** Full KMP support is a primary design goal.

**Supported targets**:
- ✅ JVM
- ✅ JS (IR)
- ✅ Native (iOS, macOS, Linux, Windows)
- ✅ Wasm
- ✅ Android

Each target gets its own fakes variant (`jvmFakesElements`, `jsFakesElements`, etc.).

---

### Q: How does it work with `commonTest`?

Perfectly! The `fakes` source set is designed for `commonTest`:

```kotlin
// Source set hierarchy:
commonMain
    ↓
  fakes (accessible from commonTest)
    ↓
commonTest

// Usage in commonTest:
import foundation.fakeLogger
val logger = fakeLogger { /* ... */ }
```

---

### Q: Can I use platform-specific fakes?

**Yes!** Each platform gets its own fakes source set:

```kotlin
// Common fakes
src/fakes/kotlin/foundation/FakeLogger.kt

// Platform-specific override (if needed)
src/jvmFakes/kotlin/foundation/FakeLogger.jvm.kt
src/iosFakes/kotlin/foundation/FakeLogger.ios.kt
```

Platform fakes depend on common fakes, so you can override behavior per-platform.

---

## IDE Integration

### Q: Will my IDE recognize fakes from other modules?

**Yes!** Because fakes live in a proper source set (`src/fakes/kotlin/`), IntelliJ IDEA automatically indexes them.

**Expected IDE features**:
- ✅ Code completion
- ✅ "Go to Definition"
- ✅ Find Usages
- ✅ Refactoring (rename, extract, etc.)
- ✅ Debugging (set breakpoints in fakes)

---

### Q: What if IDE shows "Unresolved reference"?

**Try these steps**:

1. **Gradle → Reload All Gradle Projects**
2. **File → Invalidate Caches → Invalidate and Restart**
3. Verify source set exists:
   - **File → Project Structure → Modules**
   - Check for `fakes` and `jvmFakes` under your module

4. Manually add source root (if needed):
   ```kotlin
   // build.gradle.kts
   idea {
       module {
           sourceDirs.add(file("src/fakes/kotlin"))
       }
   }
   ```

---

## Gradle and Build Configuration

### Q: Does this work with Gradle configuration cache?

**Yes!** The convention plugin is designed to be configuration cache compatible.

**Best practices**:
- Use lazy configuration APIs (`configureEach` instead of `all`)
- Avoid project evaluation during configuration
- Use Provider API for task inputs

---

### Q: What Gradle version is required?

**Minimum**: Gradle 7.6 (for full GMM support)
**Recommended**: Gradle 9.0+ (latest features and performance)

Kotlin 2.2.20+ recommended for K2 compiler benefits.

---

### Q: Can I customize the fakes source set name?

**Future feature** (Week 3). Configuration DSL will allow:

```kotlin
fakt {
    fakesSourceSetName.set("testDoubles")  // Custom name
}
```

For now, it's fixed to `fakes`.

---

### Q: How do capabilities work?

**Capabilities** are Gradle's way of exposing multiple variants from one module.

**Format**: `${group}:${module-name}-fakes:${version}`

**Example**: `com.example:foundation-fakes:1.0.0`

When you declare `requireCapability()`, Gradle selects the variant with that capability instead of the default main variant.

**See**: [TECHNICAL-REFERENCE.md#capabilities-declaration](./TECHNICAL-REFERENCE.md#capabilities-declaration)

---

## Publishing and Distribution

### Q: Can I publish fakes to Maven Central?

**Yes!** Gradle Module Metadata automatically includes fakes variant when publishing.

```kotlin
publishing {
    publications {
        // KMP plugin auto-creates publications
        // Fakes included automatically!
    }
}
```

**Published files**:
- `foundation-1.0.0.jar` (main artifact)
- `foundation-1.0.0.module` (GMM with fakes variant)
- `foundation-1.0.0-sources.jar` (includes `src/fakes/`)

---

### Q: Do external consumers need special configuration?

**If using Gradle Module Metadata**: No! They declare capability requirement and Gradle resolves it.

**If using Maven POM (legacy)**: Fakes won't be available (GMM required for capabilities).

**Recommendation**: Publish with GMM enabled (default in Gradle 7.6+).

---

## Troubleshooting

### Q: Variant resolution fails with "No matching variant found"

**Cause**: Attribute mismatch between consumer and producer.

**Debug**:
```bash
# Check producer's variants
./gradlew :foundation:outgoingVariants --all

# Check consumer's requirements
./gradlew :domain:dependencies --configuration commonTestCompileClasspath --debug
```

**Fix**: Ensure fakes variant copies ALL attributes from main API variant.

**See**: [TECHNICAL-REFERENCE.md#debugging-guide](./TECHNICAL-REFERENCE.md#debugging-guide)

---

### Q: Fakes compile but aren't visible to dependent modules

**Likely cause**: Capability not declared or mismatched.

**Check**:
1. Producer declares capability in `outgoing.capability()`
2. Consumer requires EXACT capability string
3. Gradle selects correct variant (check with `dependencies` task)

**Format must match exactly**:
```kotlin
"${project.group}:${project.name}-fakes:${project.version}"
```

---

### Q: Build fails with "Duplicate class" error

**Cause**: Both main and fakes variants on classpath simultaneously.

**Fix**: Ensure you're using `testImplementation` (not `implementation`) for fakes dependency:

```kotlin
// ❌ Wrong: Adds to main classpath
dependencies {
    implementation(project(":foundation")) {
        capabilities { requireCapability("...") }
    }
}

// ✅ Correct: Test classpath only
dependencies {
    commonTestImplementation(project(":foundation")) {
        capabilities { requireCapability("...") }
    }
}
```

---

## Migration and Compatibility

### Q: I already use Fakt in single-module projects. Will this break?

**No!** Multi-module support is backward compatible.

**Single-module projects** continue to work without changes:
- Fakes generate to `build/generated/` (current behavior)
- No convention plugin required

**Multi-module projects** opt-in by applying `fakt-convention`.

---

### Q: Can I migrate from dedicated test modules?

**Yes!** Migration is straightforward:

**Before**:
```
:foundation
:foundation-fakes  # Delete this module
```

**After**:
```
:foundation  # Just apply fakt-convention
```

**Steps**:
1. Apply `id("fakt-convention")` to foundation module
2. Remove `:foundation-fakes` from `settings.gradle.kts`
3. Delete `foundation-fakes/` directory
4. Consumer modules work unchanged (with capability requirement)

---

### Q: Does this work with Android modules?

**Yes!** Android modules are supported:

```kotlin
// Android library with fakes
plugins {
    id("com.android.library")
    kotlin("android")
    id("fakt-convention")
}

android {
    // Standard Android config
}
```

**Note**: Android test source sets have different names (`androidTest` vs `commonTest`), but convention plugin handles this automatically.

---

## Advanced Usage

### Q: Can I exclude certain interfaces from fakes variant?

**Future feature**. For now, all `@Fake` interfaces in a module are included in the fakes variant.

**Workaround**: Use a separate module for interfaces you don't want to share.

---

### Q: Can fakes depend on other fakes?

**Yes!** Transitive fake dependencies work:

```kotlin
// foundation module
@Fake interface Logger

// domain module (depends on foundation)
@Fake interface UserRepository {
    fun save(user: User, logger: Logger)  // Uses foundation.Logger
}

// app module tests
import domain.fakeUserRepository
import foundation.fakeLogger

val logger = fakeLogger { /* ... */ }
val repo = fakeUserRepository {
    save { user, log -> /* log is foundation.Logger */ }
}
```

**Gradle handles transitive resolution** automatically via capabilities.

---

### Q: How does this affect build performance?

**Initial build**: Slightly slower (new source set compilation)
**Incremental builds**: Minimal impact (only changed fakes recompile)

**Benchmarks** (target):
- Incremental compilation: < 5 seconds
- Configuration cache: Fully compatible
- Build cache: Fully compatible

**See**: [IMPLEMENTATION-ROADMAP.md#success-metrics](./IMPLEMENTATION-ROADMAP.md#success-metrics)

---

## Comparison with Alternatives

### Q: Why not use MockK or Mockito?

**Fakt** is complementary, not a replacement:

- **MockK/Mockito**: Runtime mocking frameworks (verification, stubbing)
- **Fakt**: Compile-time fake generation (type-safe, no reflection)

**Use cases**:
- **Fakt**: Test doubles for interfaces you control
- **Mocks**: Verifying interactions, stubbing third-party libraries

---

### Q: How does this compare to test fixtures?

| Feature                  | Fakt Multi-Module | java-test-fixtures |
|--------------------------|-------------------|--------------------|
| KMP support              | ✅ Full           | ❌ JVM only        |
| Zero module proliferation| ✅ Yes            | ✅ Yes             |
| Code generation          | ✅ Automatic      | ❌ Manual          |
| Type-safe DSL            | ✅ Yes            | ❌ No              |
| IDE support              | ✅ Full           | ✅ Full            |

**Fakt** = test fixtures + code generation + KMP support

**See**: [COMPARISON-MATRIX.md](./COMPARISON-MATRIX.md) for full comparison.

---

## Contributing and Support

### Q: How can I contribute?

**During implementation**:
1. Test early builds and provide feedback
2. Report issues via GitHub Issues
3. Suggest improvements to docs

**After release**:
1. Write tutorials and blog posts
2. Create sample projects
3. Submit PRs for enhancements

---

### Q: Where can I get help?

**Documentation**:
- [ARCHITECTURE-DECISION.md](./ARCHITECTURE-DECISION.md) - Why this approach
- [TECHNICAL-REFERENCE.md](./TECHNICAL-REFERENCE.md) - How it works
- [IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md) - What's coming
- [CONVENTION-PLUGIN-BLUEPRINT.md](./CONVENTION-PLUGIN-BLUEPRINT.md) - Implementation details

**Community**:
- GitHub Discussions (Q&A)
- GitHub Issues (Bug reports)
- Slack/Discord (coming soon)

---

### Q: What's the release timeline?

**Week 1** (Days 1-5): Convention plugin core
- Source set creation
- Variant configuration
- Integration testing

**Week 2** (Days 6-10): Compiler integration
- Generate to `src/fakes/`
- Source set registration
- IDE validation

**Week 3** (Days 11-15): Auto-wiring and polish
- Automatic dependency wiring
- Configuration DSL
- Documentation and samples

**Target**: Production release in 3 weeks (end of Week 3)

**See**: [IMPLEMENTATION-ROADMAP.md](./IMPLEMENTATION-ROADMAP.md) for detailed timeline.

---

## Known Limitations

### Q: What features are NOT supported yet?

**Current limitations**:
- ❌ Auto-wiring (coming in Week 3)
- ❌ Custom source set names (coming in Week 3)
- ❌ Opt-out per interface (future enhancement)

**Design limitations**:
- ❌ Sealed interfaces (Kotlin language restriction)
- ⚠️ Inline functions (potential future support)

**See**: Project CLAUDE.md for full feature matrix.

---

### Q: Are there any platform-specific issues?

**All platforms supported equally**:
- ✅ JVM: Full support
- ✅ JS: Full support
- ✅ Native: Full support
- ✅ Wasm: Full support
- ✅ Android: Full support

No known platform-specific issues with multi-module approach.

---

## Feedback and Questions

### Q: I have a question not covered here. Where should I ask?

**GitHub Discussions** is the best place for questions.

**Before posting**:
1. Search existing discussions
2. Check this FAQ
3. Review technical documentation

**When posting**:
- Include Gradle version
- Include Kotlin version
- Provide minimal reproducible example
- Share relevant build scripts

---

### Q: How do I report a bug?

**GitHub Issues** for bug reports.

**Include**:
1. Fakt version
2. Gradle version (`./gradlew --version`)
3. Kotlin version
4. Project structure (module dependency graph)
5. Minimal reproducible example
6. Error messages and stack traces

**Template**:
```markdown
**Environment**:
- Fakt: 1.0.0
- Gradle: 9.0
- Kotlin: 2.2.20

**Project structure**:
:foundation → :domain → :app

**Expected behavior**: ...
**Actual behavior**: ...
**Reproduction**: ...
```

---

**Last Updated**: 2025-10-05
**Next Review**: After Week 1 implementation completion
