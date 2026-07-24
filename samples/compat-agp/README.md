<!--
  Copyright (C) 2025 Rodrigo Sicarelli
  SPDX-License-Identifier: Apache-2.0
-->
# AGP compatibility matrix

Validates that Fakt's Android test-fixtures support works across Android Gradle Plugin versions —
the AGP analogue of the Kotlin-version matrix in [`../compat/`](../compat).

Each `agp-<version>/` directory is a **self-contained** single-module Android library: a `@Fake`
interface in `src/main`, AGP test fixtures enabled, and the module's own unit test
(`testDebugUnitTest`) consuming the generated `fakeUserRepository`. Because different AGP versions
require different Gradle versions, each cell ships its **own** Gradle wrapper (unlike the Kotlin
compat cells, which share the repo-root wrapper).

| Cell | AGP | Gradle | compileSdk | `enableTestFixturesKotlinSupport` |
|------|-----|--------|------------|-----------------------------------|
| `agp-8.11` | 8.11.1 | 8.13 | 35 | required (set in `gradle.properties`) |
| `agp-8.12` | 8.12.3 | 9.0.0 | 35 | required |
| `agp-9.0` | 9.0.0 | 9.1.0 | 35 | **not set** — validates the flag-free 9.0 default |

> The floor is AGP **8.11.1** rather than an older 8.x: AGP ≤ 8.7 calls the `KotlinJvmOptions.getUseK2()` API that Kotlin 2.3.20 (Fakt's compiler version) removed, so those versions cannot compile Android test-fixtures Kotlin at all.

## Run

```bash
make publish-local
make test-compat-agp-all          # all cells
make test-compat-agp-8.11         # a single cell
```

CI runs these via the `test-compat-agp` matrix job in `.github/workflows/development.yml`.
