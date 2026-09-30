# #152 Native spike (throwaway, never merged)

This spike checks approach A for Native: a `NATIVE` worker driver that runs
`org.jetbrains.kotlin.cli.bc.K2Native` from the K/N distribution, with Fakt as `-Xplugin`, and
emits fakes at the FIR phase. The exit criteria are listed in #152. The findings are reported on
#152.

## Layout

| Path | What |
|---|---|
| `bin/provision.sh <ver>` | Downloads `kotlin-native-prebuilt-<ver>-linux-x86_64` from Maven Central into `$SPIKE_HOME/dist` |
| `bin/run-k2native.sh` | Runs K2Native with Fakt as a plugin, the way a worker driver would. No Gradle. |
| `fixture/` | Standalone KMP project with `linuxX64`, `iosArm64`, `iosSimulatorArm64` and `macosArm64`, plus one `@Fake` per criterion |

## Fixture fakes to criteria

| Source set | Fake | Criterion |
|---|---|---|
| `linuxX64Main` | `PosixClock` (`platform.posix.timespec`, `FILE`) | C1: leaf, byte-identical to in-process |
| `linuxX64Main` | `CinteropPort` (user cinterop `Point`, forward-declared `cnames.structs.Opaque`) | C3 |
| `nativeMain` | `NativeMemory` (commonized `size_t`, `CPointer<ByteVar>`) | C2: shared native |
| `appleMain` | `AppleUrls` (`NSURL`, `NSData`, suspend) | C2: Apple shared |
| `iosMain` | `IosViews` (`UIView`, `UIViewController`) | C2: iOS forward declarations |

## Rerun

```bash
export SPIKE_HOME=/tmp/native-spike
make publish-local                        # Fakt 1.0.0-beta13 in ~/.m2

# Reference: the legacy in-process output, and KGP's own K2Native arguments
./gradlew -p spike/native/fixture compileTestKotlinLinuxX64 \
  -Pfakt.useExperimentalGenerateTask=false \
  -Pkotlin.internal.compiler.arguments.log.level=warning
```

The per-criterion commands and their results are recorded in the report on #152.
