# compiler-interop

Proves that Fakt's generation step sees the same compiler settings as your normal build
(issue #165). Fakt generates fakes in its own worker. If that worker did not get your opt-ins,
`-X` flags and JDK, code that `compileKotlin` accepts would break fake generation.

## What each part proves

| Part | What it proves |
|------|----------------|
| `InteropMarker` (ERROR-level `@RequiresOptIn`), opted in for the whole module | The module-wide opt-in reaches the worker. Main code uses the marker with no `@OptIn`. In `:jvm` the `ReceiptStore` fake also has a marker type in its signature. |
| `-Xcontext-sensitive-resolution` and `when (tier) { FREE -> ... }` | A `-X` flag from `freeCompilerArgs` reaches the worker. The unqualified enum entries compile only with the flag. |
| `:kmp` sets the opt-in with `languageSettings.optIn` and the flag with `compilerOptions.freeCompilerArgs` | Both KMP ways of setting options are forwarded, to the common producer and to the jvm and js producers. |
| `:jvm` + local `:processor` (KSP) | A KSP-generated type (`GeneratedInvoice`) is visible to the worker. |
| `jvmToolchain(interopToolchain)` and `src/jdk17` | The toolchain JDK is forwarded. With 17, main code references `java.lang.Compiler`. Gradle runs on JDK 21, where that class does not exist. |
| `:serialization` (Kotlin serialization plugin, `Receipt.serializer()` in main code) | The worker does not stop on errors outside `@Fake` code. `compileKotlin` has the plugin, the worker does not, so it sees an unresolved `serializer`, tolerates it, and still writes the fakes. Its `@Fake` interfaces do not use `Receipt`. |
| `cache-correctness-check.sh` in CI | Forwarded arguments are relocatable: all four producers of `:jvm` and `:kmp` restore FROM-CACHE. |

## Why serialization is its own module

The worker now tolerates errors outside `@Fake` code and only prints them (`Fakt: tolerated
compiler error: ...`). That is what `:serialization` needs. But it also means `:jvm` and `:kmp`
would still pass if option forwarding broke: the worker would quietly tolerate the errors. So the
tolerance case lives in its own module, and the other two modules are guarded against it.

## The two guards

`cache-correctness-check.sh` reads the warm-up log (the forced run that really executes the
producers). Both guards need the module's `fakt { logLevel }` to be INFO or higher; all three
modules set it.

- `FAKT_FORBID_TOLERATED=1` (used for `:jvm` and `:kmp`): fail if the log has a tolerated
  error. These modules must be fully understood by the worker, so a tolerated error means an
  option was not forwarded.
- `FAKT_EXPECT_TOLERATED=1` (used for `:serialization`): fail if the log has no tolerated
  error. Then the sample would prove nothing.

## Known limits

- If the `@Fake` annotation itself cannot be resolved in the worker (a classpath bug), the worker
  finds no fake, treats that located error as tolerated, and succeeds. You only see
  `unresolved reference FakeXxx` later, when the test source set compiles.

## Run it

```bash
make publish-local
make test-compiler-interop                          # JDK 17 toolchain (what CI uses)
make test-compiler-interop INTEROP_TOOLCHAIN=21     # machine with only JDK 21
```

`interopToolchain` (default `17`, set in `gradle.properties`) picks the `:jvm` toolchain. The
`src/jdk17` source directory is added to `main` only when it is 17.

## What only CI can prove

- **The JDK toolchain.** CI installs JDK 17 next to JDK 21. A JDK 21-only machine runs with
  `-PinteropToolchain=21`, so it proves the options but not the JDK forwarding.
- **JS tests.** `jsNodeTest` needs npm. Locally only `faktGenerateJsMain` and the JS test
  compilation were run.

## JS yarn lock

The root build moves the Kotlin/JS `yarn.lock` to `vendor/kotlin-js-store`, and the lock is
committed, like in `samples/kmp-no-jvm`. With a committed lock, `kotlinRestoreYarnLock` runs before
`kotlinStoreYarnLock`, so Gradle 9.5+ does not fail on a missing input (#144). To refresh it, run
`./gradlew -p samples/compiler-interop kotlinUpgradeYarnLock` and commit the result.

## KSP

`kspKotlin` is a task dependency of `faktGenerateJvmMain`, because KSP registers its output
directory on `kotlin.srcDir` as a task output. No Fakt wiring is needed for the generated type.
KSP with AGP 9 built-in Kotlin is not supported, so this sample has no Android module.

Tests use JUnit 5 and `kotlin.test` with fakes only. The `:jvm` tests use
`@TestInstance(PER_CLASS)`. The `:kmp` tests live in `commonTest`, where that JUnit annotation is not
available.
