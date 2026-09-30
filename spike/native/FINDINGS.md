# Findings log (raw; the report on #152 is the summary)

## Distribution (2.4.10, linux-x86_64)
- `konan/lib/kotlin-native-compiler-embeddable.jar` (84 MB) is the only jar. It is
  self-contained: `K2Native` plus the full FIR frontend. No trove4j or other jars are needed.
- `klib/platform/` ships ios_*, macos_*, tvos_*, watchos_*, android_*, mingw_x64 and linux_* on
  the Linux host.
- There is no `klib/commonized`. KGP's commonizer produces it per build.

## KGP provisioning (2.4.10, from bytecode + a real build)
- Properties: `kotlin.native.home` (or `org.jetbrains.kotlin.native.home`), `konanDataDir`,
  `kotlin.native.version`, `kotlin.native.distribution.downloadFromMaven`,
  `kotlin.native.toolchain.enabled`.
- The distribution is the Maven artifact
  `org.jetbrains.kotlin:kotlin-native-prebuilt:<ver>` (tar.gz), resolved through the
  `kotlinNativeBundleConfiguration` configuration.
- `NativeVersionValueSource` (configuration time) unpacks it and writes `provisioned.ok`. It also
  runs as the task `:downloadKotlinNativeDistribution`, which KGP's native compile tasks depend
  on.
- `~/.konan/dependencies` was populated by the build (the LLVM toolchain for cinterop and
  linking). That comes from KGP's cinterop and link tasks, not from compilation to klib.

## C1: PASS (2.4.10)
- `bin/c1-leaf.sh 2.4.10 <dir>`: K2Native with `-produce library -Xmetadata-klib`, Fakt as
  `-Xplugin`, `emitPhase=FIR`, `emitSourceSets=[linuxX64Main]`.
- Arguments mirror KGP's `compileKotlinLinuxX64`, captured with
  `-Pkotlin.internal.compiler.arguments.log.level=warning`: `-library`,
  `-Xfragment-refines`, `-Xfragment-sources`, `-Xfragments`, `-Xmulti-platform`, `-target`.
- `FakePosixClockImpl` and `FakeCinteropPortImpl` are byte-identical (sha256) to the in-process
  output.
- 8.3 s wall time, 1g heap.

## C3 (leaf): PASS
`FakeCinteropPortImpl` resolves the user cinterop klib (`spike.cfixture.Point`) and the
forward-declared `cnames.structs.Opaque`.

## C5 (leaf): PASS
- Run with `~/.konan` moved away and `KONAN_DATA_DIR` set to an empty dir.
- strace (`%file`) shows 0 accesses to `dependencies/` and 0 to `~/.konan`.
- The only distribution files read are `konan/konan.properties`, the compiler jar,
  `klib/common/stdlib` and `klib/platform/linux_x64`.

## C2: PASS on Linux at klib level (2.4.10); macOS link/run pending (Layer 3)
- KGP compiles shared native with K2Native, not the metadata compiler:
  - `-produce library -Xmetadata-klib -no-default-libs -nostdlib`;
  - a representative `-target` (`linux_x64` for `nativeMain`, `ios_arm64` for `appleMain` and
    `iosMain`);
  - `-Xcommon-sources=<own sources>`;
  - each commonized platform klib as an explicit `-library` (6 for `nativeMain`, 134 for
    `appleMain`, 181 for `iosMain`) from
    `~/.konan/<dist>/klib/commonized/<ver>/(<targets>)/`, plus the parent source sets' metadata
    klibs.
- `bin/c2-shared.sh` replays those exact arguments, with KGP's in-process Fakt swapped for FIR
  emission and `emitSourceSets=[<sourceSet>]`.
- `FakeNativeMemoryImpl` (`nativeMain`, commonized `size_t`), `FakeAppleUrlsImpl` (`appleMain`,
  `NSURL`/`NSData`, suspend) and `FakeIosViewsImpl` (`iosMain`, `UIView`/`UIViewController`) are
  byte-identical to in-process. About 7 s each, 0 `dependencies/` accesses.
- `compileTestKotlin{IosArm64,IosSimulatorArm64,MacosArm64}` compile against those fakes on the
  Linux host.
- Commonization (`:commonizeNativeDistribution`) runs on Linux, Apple included.
- Commonized klibs are **inputs** that the producer receives through the metadata compilation's
  dependency files. The driver doesn't locate them itself.

## C7: PASS (2.2.0 and 2.4.10, same Fakt jar)
- The K/N 2.2.0 `konan/lib` has `kotlin-native-compiler-embeddable.jar` (self-contained) **and**
  `kotlin-native.jar`. Use the embeddable one only.
- Leaf, `nativeMain`, `appleMain` and `iosMain` through K2Native 2.2.0 with the 2.4.10-built
  Fakt jar produce byte-identical output to 2.2.0 in-process **and** to 2.4.10.
- Apple on a Linux host with KGP 2.2.0 needs `kotlin.native.enableKlibsCrossCompilation=true`,
  otherwise KGP SKIPs the tasks. On 2.4.10 it is the default.
- **Blocker found (pre-existing, not driver-related):** the published
  `com.rsicarelli.fakt:annotations-*` klibs are built with 2.4.10 (`abi_version=2.4.0`).
  Kotlin/Native 2.2.0 rejects them: "can consume libraries having ABI version <= 2.2.0". So
  Native, and presumably JS/Wasm, users on Kotlin < 2.4 cannot use Fakt 1.0.0-beta13 at all,
  in-process included. The `compat` CI samples are JVM-only, which is why this was missed. The
  spike works around it with `-Pspike.localAnnotations=true`, which compiles the annotation
  sources into the fixture.
  - Fix: build `:annotations` with a lower klib ABI and apiVersion (for example
    `-Xklib-abi-version` / `apiVersion = 2.2`), and add a KMP/Native compat cell. This belongs
    with #166.

## Side findings (not Native-specific)
- **Codegen bug:** an interface-level `@OptIn(X::class)` is copied into the fake without an
  import for `X`, so the compile fails with `Unresolved reference 'ExperimentalForeignApi'`. This
  happens on the in-process path too. The fixture works around it with module-level
  `languageSettings.optIn`.
- **Undeclared write:** `CompilerOptimizations` (legacy IR signature cache) creates
  `<outputDir>/../../cache/` on every driver, JVM worker included, outside the task's declared
  outputs. It is already on the #150 removal list (`CompilerOptimizations` / `SignatureBuilder`).
- **Fixture note:** the default `enableCallHistory=true` needs kotlinx-coroutines in the test
  classpath.
