# Compatibility

Fakt is tested against multiple Kotlin and Android Gradle Plugin versions on every change. This page is the source of truth for version support.

---

## Compatibility Matrix

| Kotlin Version | Status | CI |
|---------------|--------|-----|
| 2.5.0-Beta1 | 🐤 Canary (newer than the worker): warns, builds | ✅ |
| **2.4.10** | ⭐ **Latest tested** | ✅ |
| 2.3.20 | ✅ Supported | ✅ |
| 2.3.10 | ✅ Supported | ✅ |
| 2.3.0 | ✅ Supported | ✅ |
| 2.2.21 | ✅ Supported | ✅ |
| 2.2.20 | ✅ Supported | ✅ |
| 2.2.10 | ✅ Supported | ✅ |
| 2.2.0 | ✅ Minimum supported | ✅ |
| 2.1.x | ⚠️ Not tested | — |
| 2.0.x | ⚠️ Not tested | — |
| 1.x | ❌ Not supported (K1) | — |

---

## Worker compiler version

Fakt generates your fakes in a separate step, called the worker. The worker runs its own Kotlin
compiler, and Fakt pins it to the Kotlin version Fakt was tested with (**2.4.10** today). Your
project can still use an older or newer Kotlin. Fakt compares your Kotlin Gradle plugin version with
the worker's version, looking only at the major and minor numbers:

| Your Kotlin | What happens |
|-------------|--------------|
| Same or older | Works. Fakt says nothing. |
| One minor newer (for example 2.5.x) | A warning. It usually works fine. |
| Two or more minors newer (for example 2.6.x) | The build stops with a message. The pinned compiler may not be able to read your Kotlin. |

A newer patch of the same minor (2.4.20 with a 2.4.10 worker) counts as the same. Pre-release
versions such as `2.5.0-Beta1` are compared by their `major.minor.patch` numbers.

### Build anyway

To skip the stop, add this to `gradle.properties`. The error becomes a warning:

```properties
fakt.allowNewerKotlin=true
```

### Use a different worker compiler

The worker's compiler comes from the `faktWorker` Gradle configuration. Fakt adds a fixed version
of `org.jetbrains.kotlin:kotlin-compiler-embeddable` to it. Because of that, Gradle's conflict
resolution lets you move **up**: declare a newer version and it wins.

```kotlin
dependencies {
    "faktWorker"("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.5.0")
}
```

Going **down** needs `resolutionStrategy.force(...)`, because Gradle keeps the higher version
unless you force the lower one:

```kotlin
configurations.named("faktWorker") {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.20")
}
```

!!! warning "At your own risk"
    Fakt's jar is built against the pinned compiler API. A different worker compiler can break
    with errors such as `NoSuchMethodError`. Fakt does not test this setup.

### Kotlin/Native

The Native worker does not use the pinned compiler. It runs on your own Kotlin/Native compiler, the
same version as your build. The version check still compares your Kotlin Gradle plugin version, and
it does so for every target, Native included.

### Why not use your Kotlin version for the worker?

It would mean Fakt's jar has to work with every supported compiler. The compiler API changes between
Kotlin versions, so Fakt would need a compatibility layer for each one. A single pinned worker keeps
the generated code the same for everyone, and the version check tells you when your Kotlin has moved
too far ahead.

---

## Android Gradle Plugin

Android projects are tested against these Android Gradle Plugin (AGP) versions on every change:

| AGP | Gradle | Kotlin | What it covers |
|-----|--------|--------|----------------|
| **9.4.1** | 9.6.1 | Kotlin Android plugin | ⭐ Newest tested, cache-correct path |
| 9.0.0 | 9.1.0 | AGP built-in Kotlin | Built-in Kotlin, cache-correct path |
| 8.12.3 | 9.0.0 | Kotlin Android plugin | Cache-correct path |
| 8.11.1 | 8.13 | Kotlin Android plugin | ✅ Minimum supported |

The KMP Android library plugin (`com.android.kotlin.multiplatform.library`) is tested on AGP 9.0.0.
AGP 8.7 and older can't compile Android test-fixtures Kotlin with the Kotlin version Fakt is built
with, so they aren't supported.

Fakt's Gradle plugin is compiled against the **minimum** AGP API, so it can only call APIs that
every supported AGP version has. The newest AGP runs the same code in CI. AGP is not bundled: your
build always uses its own AGP.

Both ways of compiling Android Kotlin use the cache-correct path. On AGP 9's built-in Kotlin, Fakt
reads each variant's sources from AGP's variant API. Modules that apply `org.jetbrains.kotlin.android`
(every AGP 8.x module, and AGP 9 with `android.builtInKotlin=false`) use Kotlin's own source sets.

---

## Our Compatibility Philosophy

Fakt maintains a range of supported Kotlin versions, verified by automated CI testing on every change. This means adopting Fakt doesn't force you to upgrade Kotlin — you can use whatever version fits your project within the supported range.

Every change to Fakt is tested against all versions in the matrix above. If a change breaks any supported version, the CI pipeline fails and the change is blocked. This makes backward compatibility a built-in constraint of the development process, not an afterthought.

When a new Kotlin version is released, we add it to the matrix and verify compatibility before updating the "Latest tested" entry.

---

## What the Status Labels Mean

- ⭐ **Latest tested** — The newest stable Kotlin version Fakt is compiled against, and the one its worker uses. Recommended for new projects.
- 🐤 **Canary** — A pre-release Kotlin that is newer than the worker. It checks that Fakt's version check works with a real newer Kotlin: Fakt prints a warning and the build passes.
- ✅ **Supported** — Verified working via CI on every commit. Safe to use.
- ⚠️ **Not tested** — May work, but not part of the CI matrix. If you encounter issues, [open a bug report](https://github.com/rsicarelli/fakt/issues/new?template=bug_report.yml) and we'll investigate adding it to the matrix.
- ❌ **Not supported** — Known to be incompatible (e.g., Kotlin 1.x uses the K1 compiler, which Fakt does not support).

---

## Adding a New Version

When Kotlin releases a new version, adding it to Fakt's support matrix requires:

1. A new compat sample directory (`samples/compat/kotlin-X.Y.Z/`)
2. CI verification that all tests pass
3. Updating this page

See the [contributing guide](https://github.com/rsicarelli/fakt/blob/main/CONTRIBUTING.md) for details.
