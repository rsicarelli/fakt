# Compatibility

Fakt is tested against multiple Kotlin and Android Gradle Plugin versions on every change. This page is the source of truth for version support.

---

## Compatibility Matrix

| Kotlin Version | Status | CI |
|---------------|--------|-----|
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

## Android Gradle Plugin

Android projects are tested against these Android Gradle Plugin (AGP) versions on every change:

| AGP | Gradle | Kotlin | What it covers |
|-----|--------|--------|----------------|
| **9.4.1** | 9.6.1 | Kotlin Android plugin | ⭐ Newest tested, cache-correct path |
| 9.0.0 | 9.1.0 | AGP built-in Kotlin | Built-in Kotlin ([in-process path](user-guide/plugin-configuration.md#cache-correct-generation)) |
| 8.12.3 | 9.0.0 | Kotlin Android plugin | Cache-correct path |
| 8.11.1 | 8.13 | Kotlin Android plugin | ✅ Minimum supported |

The KMP Android library plugin (`com.android.kotlin.multiplatform.library`) is tested on AGP 9.0.0.
AGP 8.7 and older can't compile Android test-fixtures Kotlin with the Kotlin version Fakt is built
with, so they aren't supported.

Fakt's Gradle plugin is compiled against the **minimum** AGP API, so it can only call APIs that
every supported AGP version has. The newest AGP runs the same code in CI. AGP is not bundled: your
build always uses its own AGP.

Android modules on AGP 9's built-in Kotlin still generate fakes, but inside `compileKotlin*`,
without the Gradle build cache
([#154](https://github.com/rsicarelli/fakt/issues/154)). Modules that apply
`org.jetbrains.kotlin.android` use the cache-correct path. That means every AGP 8.x module, and AGP 9
with `android.builtInKotlin=false`.

---

## Our Compatibility Philosophy

Fakt maintains a range of supported Kotlin versions, verified by automated CI testing on every change. This means adopting Fakt doesn't force you to upgrade Kotlin — you can use whatever version fits your project within the supported range.

Every change to Fakt is tested against all versions in the matrix above. If a change breaks any supported version, the CI pipeline fails and the change is blocked. This makes backward compatibility a built-in constraint of the development process, not an afterthought.

When a new Kotlin version is released, we add it to the matrix and verify compatibility before updating the "Latest tested" entry.

---

## What the Status Labels Mean

- ⭐ **Latest tested** — The Kotlin version Fakt is compiled against and recommended for new projects.
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
