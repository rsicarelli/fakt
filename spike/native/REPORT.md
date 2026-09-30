## #152 Native spike: results

**Verdict: approach A works.** All 8 exit criteria pass, on Linux and on macOS (arm64). `K2Native`, loaded from
the K/N distribution's `kotlin-native-compiler-embeddable.jar` with Fakt as `-Xplugin` and FIR
emission, produces fakes **byte-identical** to the in-process path for:
- leaf targets;
- `nativeMain`, `appleMain` and `iosMain` (UIKit/Foundation included);
- user cinterop klibs, including forward-declared structs.

It does this on Kotlin 2.2.0 and 2.4.10, never touches `~/.konan/dependencies`, and is
FROM-CACHE after relocation.

**D1** (the fallback if C2 or C4 fails) is not needed. Native can follow the task path in 1.0.

Branch: `ccr-aa1d21c5-gqlrfs`. It holds a throwaway prototype behind
`-Pfakt.spike.native=true`, the harness in `spike/native/`, the raw log in
`spike/native/FINDINGS.md`, and a temporary macOS workflow.

### Exit criteria

| # | Criterion | Result | Evidence |
|---|---|---|---|
| 1 | Leaf `linuxX64Main` fake over `platform.posix` byte-identical to in-process | ✅ PASS | `FakePosixClockImpl` sha256 equal. K2Native CLI with the args KGP uses for `compileKotlinLinuxX64` (`bin/c1-leaf.sh`) and on the Gradle task path |
| 2 | `nativeMain` / `appleMain` via `-Xmetadata-klib`, incl. iOS forward declarations | ✅ PASS (Linux + macOS) | `NativeMemory` (commonized `size_t`), `AppleUrls` (`NSURL`, `NSData`, suspend) and `IosViews` (`UIView`, `UIViewController`) are byte-identical. `iosArm64`, `iosSimulatorArm64` and `macosArm64` test klibs compile against them on Linux. On `macos-latest`, `iosSimulatorArm64Test` and `macosArm64Test` link and pass against task-path fakes, and all fakes are byte-identical to in-process there (runs 36702721411, 36703013116). `kmp-multi-target` iOS-simulator and macOS tests pass too |
| 3 | User cinterop klibs resolve | ✅ PASS | `CinteropPort` uses `spike.cfixture.Point` and forward-declared `cnames.structs.Opaque`; byte-identical |
| 4 | Distribution located without KGP internals, offline; when is it provisioned | ✅ PASS | Discovery uses `kotlin.native.home` → `konanDataDir`/`KONAN_DATA_DIR`/`~/.konan` + `kotlin-native-prebuilt-<host>-<ver>`; version from `kotlin.native.version` or `getKotlinPluginVersion()`. Clean `~/.konan` + `--offline` works on 2.2.0 and 2.4.10. Configuration cache is stored and then reused. Provisioning details below |
| 5 | `~/.konan/dependencies` never accessed | ✅ PASS | strace (`%file`) of every K2Native run: 0 accesses to `dependencies/`. The only distribution files read are the compiler jar, `konan.properties`, `klib/common/stdlib` and `klib/platform/<target>` |
| 6 | Relocatable, configuration-cache-safe cache key | ✅ PASS | `cache-correctness-check.sh`: 5/5 native + common producers FROM-CACHE. Relocated to another path **and** another distribution dir: 5/5 FROM-CACHE. `kmp-multi-target` with the native producers in the contract: 6/6 FROM-CACHE, 7 fakes restored |
| 7 | Kotlin 2.2.0 and 2.4.10 | ✅ PASS (with a separate blocker below) | The same 2.4.10-built Fakt jar inside K2Native 2.2.0 gives output byte-identical to 2.2.0 in-process **and** to 2.4.10 for all 5 shapes. The `K2NativeCompilerArguments` setters are the same on both versions |
| 8 | Heap and time | ✅ PASS | 10,000 `@Fake`s in one `nativeMain`: 512m 48.0 s / 842 MB RSS, 1g 46.1 s / 1,158 MB, 2g 45.0 s / 1,260 MB. No OOM |

### What item 7 (the implementation) must include
The prototype found these; the fixture alone didn't.
1. **Shared native is a producer that owns its source set**:
   - `KotlinSharedNativeCompilation` → K2Native with `-produce library -Xmetadata-klib
     -no-default-libs -nostdlib`, the distribution's stdlib, and the compilation's dependency
     files (which already contain the commonized klibs);
   - `-Xcommon-sources=<own>`;
   - **`-Xrefines-paths=<ancestor metadata klibs>`**. Without it, `nativeMain` `actual`s have no
     `expect` (this broke `kmp-multi-target`);
   - `-target`: the host target if covered, else the first. This matches KGP.
   - Today these compilations are `SUPPRESS`ed, so a leaf-only driver would silently drop every
     `nativeMain`/`iosMain` fake.
2. **Leaf native is a consumer** (`emitSourceSets=[leaf]`, ancestors as `-Xcommon-sources`),
   routed like JS/Wasm.
3. **#165 is a hard prerequisite for Native.** Without the compilation's `-opt-in` values, every
   cinterop signature fails analysis ("needs opt-in to ExperimentalForeignApi"). The prototype
   forwards `languageSettings.optInAnnotationsInUse` as a minimal slice.
4. **Worker:**
   - a separate fork whose classpath is the distribution's embeddable jar (self-contained in both
     versions; ignore 2.2.0's extra `kotlin-native.jar`);
   - `-Dkonan.home`;
   - `konanHome` `@Internal`, and `kotlinNativeVersion`/`konanTarget` `@Input`.
5. **Provisioning:**
   - KGP 2.4.10 provisions in a configuration-time `ValueSource` **and** in
     `:downloadKotlinNativeDistribution`;
   - KGP 2.2.0 has only the `ValueSource`;
   - both finish before any task runs.

   Depending on `downloadKotlinNativeDistribution` also pulls the LLVM toolchain into
   `~/.konan/dependencies`, which Fakt never reads. Recommendation: no task dependency. Instead,
   either rely on the `ValueSource`, or, for zero KGP coupling, resolve
   `org.jetbrains.kotlin:kotlin-native-prebuilt:<ver>` through a Fakt configuration plus an
   artifact transform that extracts only `konan/lib` and `klib/`.

### Found along the way (not Native-specific; separate issues suggested)
- **Kotlin < 2.4 cannot consume Fakt's published klibs.** `annotations-*` klibs are built with
  2.4.10 (`abi_version=2.4.0`); K/N 2.2.0 rejects them with "can consume libraries having ABI
  version <= 2.2.0". This breaks Native, and likely JS/Wasm, users on Kotlin 2.2 and 2.3 today,
  in-process too. The `compat` CI cells are JVM-only. Fix: build `:annotations` with a lower klib
  ABI/apiVersion and add a KMP compat cell. It relates to #166.
- **The unread producer metadata cache is quadratic.** In producer mode `FakeInterfaceChecker`
  rewrites the whole `FirMetadataCache` after **every** interface. On the 10,000-fake benchmark
  the `commonMain` producer takes about **30 min** on today's default path, against **46 s** for the same 10,000 fakes on the native producer, which doesn't write the file. The A/B run with only the file removed (`-Pfakt.spike.noFirMetadata=true`) has not been done yet.
  #164a (removing the consumer mode and `firMetadataFile`) therefore removes a large, measurable
  cost.
- **Codegen: an interface-level `@OptIn(X::class)` is copied without importing `X`**, so the fake
  fails to compile. This happens in-process too.
- `CompilerOptimizations` creates `<outputDir>/../../cache/` (an undeclared write) on every
  driver. It is already on #150's removal list.
