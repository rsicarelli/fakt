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

### Found along the way (not Native-specific; filed)
- **#171: Kotlin < 2.4 cannot consume Fakt's published klibs.** `annotations-*` klibs are built with
  2.4.10 (`abi_version=2.4.0`); K/N 2.2.0 rejects them with "can consume libraries having ABI
  version <= 2.2.0". This breaks Native, and likely JS/Wasm, users on Kotlin 2.2 and 2.3 today,
  in-process too. The `compat` CI cells are JVM-only. Fix: build `:annotations` with a lower klib
  ABI/apiVersion and add a KMP compat cell. It relates to #166.
- **#173: the unread producer metadata cache is quadratic.**
  - In producer mode, `FakeInterfaceChecker` rewrites the whole `FirMetadataCache` after
    **every** interface.
  - On the task path nothing reads the file (#164a's unused consumer mode), yet every KMP
    `commonMain` producer pays for it on today's default path.
  - A/B on `faktGenerateMetadataCommonMain`, `kmp-benchmark` subsets, `--rerun`, warm daemon
    dependencies, same fake count in both columns:

    | fakes | with the file (today) | without it (`-Pfakt.spike.noFirMetadata=true`) | speedup |
    |---|---|---|---|
    | 2,400 | 2 min 0 s | 19.8 s | 6× |
    | 4,900 | 7 min 23 s | 26.9 s | 16× |
    | 10,000 | ≈30 min (first build) | 41.0 s | ~44× |

    Doubling the fakes roughly quadruples the time. #164a removes this cost.
- **#172: an interface-level `@OptIn(X::class)` is copied without importing `X`**, so the fake
  fails to compile. This happens in-process too.
- `CompilerOptimizations` creates `<outputDir>/../../cache/` (an undeclared write) on every
  driver. It is already on #150's removal list.

### Existing test suites on the spike branch (flag off)
- `:gradle-plugin:test` 252/252, `:compiler:test` 312/312, `:compiler-api:test` 27/27, all
  green. The default path is unaffected.
- `:gradle-plugin:apiCheck` fails **only** on the 7 new prototype properties of
  `FaktGenerateTask`.
- `:gradle-plugin:detekt` fails **only** on size thresholds in the prototype's additions
  (`TooManyFunctions`, `LongMethod` in `register`, `LongParameterList` in `routeCompilation`).
- Both are expected for throwaway code. Item 7 must split the code (for example a
  `NativeCompilerDriver` and a `NativeTaskWiring` file) and run `apiDump`.

### Not validated by the spike (for item 7)
- **Tests:** the prototype has no unit or integration tests. Item 7 needs:
  - `ProjectBuilder` routing tests (leaf → consumer, shared native → producer, the
    `SourceSetConfigurator` ownership of `nativeTest`/`iosTest`);
  - a TestKit worker test with a real distribution (skip when `fakt.test.konanHome` is unset);
  - a `CompilerDriverTest` case for `native`.
- **CI at scale:** the 10,000-fake runs were local only. CI covered the fixture and
  `kmp-multi-target` (Linux locally, macOS in CI).
- **Timing:**
  - no systematic cold-daemon vs warm-daemon matrix;
  - no in-process vs task-path wall-time comparison beyond the metadata-cache A/B;
  - no incremental measurement. `FaktGenerateTask` is not incremental on any driver, so any
    change reruns the whole source set.
- **Platforms and project shapes:**
  - Windows host (mingw) and Intel macOS host;
  - a single-target Native KMP project, which is routed `LEGACY` today and outside the spike;
  - multi-module builds and collector mode with Native producers (#168);
  - KGP 2.2.0 on a macOS host;
  - Kotlin 2.3.x.

