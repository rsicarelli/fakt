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
