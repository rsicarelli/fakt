# KMP Native-Only Sample

> **Purpose**: Locks the two project shapes `kmp-multi-target` cannot have, because it also declares
> JVM, JS and Wasm targets. Both are Kotlin/Native fakes on the `FaktGenerateTask` path (#152).

## Modules

| Module | Targets | What it proves |
|--------|---------|----------------|
| `:all-native` | `linuxX64`, `linuxArm64`, `iosArm64`, `iosSimulatorArm64` | `commonMain` is a shared-native compilation. `faktGenerateMetadataCommonMain` runs the Kotlin/Native driver (the metadata driver cannot see commonized `platform.posix`). `FakeNativeBufferImpl` reaches `commonTest`, which runs on Linux through `linuxX64Test`. |
| `:single` | `linuxX64` | No `commonMain` compilation: the lone `faktGenerateLinuxX64Main` owns `FakeGreeterImpl` (into `commonTest`) and `FakeStatImpl` (posix, into `linuxX64Test`). |

The `expect fun` in each module has its `actual` in `linuxMain`/`iosMain` (`:all-native`) or
`linuxX64Main` (`:single`), so the common fragment has to compile together with its actuals.

## Commands

```bash
./gradlew -p samples/kmp-native-only allTests --continue
FAKT_CACHED_FAKES="FakeNativeBufferImpl FakeGreeterImpl FakeStatImpl" FAKT_FORBID_TOLERATED=1 \
  ./.github/scripts/cache-correctness-check.sh samples/kmp-native-only \
  :all-native:faktGenerateMetadataCommonMain :single:faktGenerateLinuxX64Main
```

On Linux the iOS targets are compile-only (`compileTestKotlinIosSimulatorArm64`).
