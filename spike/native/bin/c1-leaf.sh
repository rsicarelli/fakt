#!/usr/bin/env bash
# C1 + C5: leaf linuxX64Main fakes via K2Native (FIR emission), with ~/.konan hidden and every
# file access traced. Usage: c1-leaf.sh <kotlin-version> <out-dir>
set -uo pipefail
VER="$1"; RUN="$2"
HERE="$(cd "$(dirname "$0")/.." && pwd)"; F="$HERE/fixture"
: "${SPIKE_HOME:?}"
export KN_HOME="$SPIKE_HOME/dist/kotlin-native-prebuilt-linux-x86_64-$VER"
export FAKT_JAR="${FAKT_JAR:-$HOME/.m2/repository/com/rsicarelli/fakt/compiler/1.0.0-beta13/compiler-1.0.0-beta13.jar}"
export OUT="$RUN/out" KONAN_DATA_DIR="$RUN/empty-konan-data"
rm -rf "$RUN"; mkdir -p "$RUN" "$KONAN_DATA_DIR"
ANN=$(ls "$HOME"/.gradle/caches/modules-2/files-2.1/com.rsicarelli.fakt/annotations-linuxx64/1.0.0-beta13/*/annotations-linuxX64Main-1.0.0-beta13.klib)
CINTEROP="$F/build/classes/kotlin/linuxX64/main/cinterop/native-spike-fixture-cinterop-fixture"
export CONTEXT='{"compilationName":"main","targetName":"linuxX64","platformType":"native","isTest":false,"defaultSourceSet":{"name":"linuxX64Main","parents":["linuxMain"]},"allSourceSets":[{"name":"linuxX64Main","parents":["linuxMain"]},{"name":"linuxMain","parents":["nativeMain"]},{"name":"nativeMain","parents":["commonMain"]},{"name":"commonMain","parents":[]}],"outputDirectory":"'"$OUT"'","commonTestOutputDirectory":"'"$OUT"'","emitPhase":"FIR","emitSourceSets":["linuxX64Main"]}'
S1="$F/src/linuxX64Main/kotlin/spike/CinteropPort.kt"; S2="$F/src/linuxX64Main/kotlin/spike/PosixClock.kt"
S3="$F/src/nativeMain/kotlin/spike/NativeMemory.kt"
[[ -d "$HOME/.konan" ]] && mv "$HOME/.konan" "$HOME/.konan.off"
trap '[[ -d "$HOME/.konan.off" ]] && mv "$HOME/.konan.off" "$HOME/.konan"' EXIT
START=$(date +%s.%N)
strace -f -qq -e trace=%file -o "$RUN/strace.log" "$HERE/bin/run-k2native.sh" \
  -nostdlib -no-endorsed-libs -library "$KN_HOME/klib/common/stdlib" -library "$ANN" -library "$CINTEROP" \
  -target linux_x64 -Xmulti-platform -opt-in kotlinx.cinterop.ExperimentalForeignApi \
  -Xfragment-refines=linuxX64Main:linuxMain,linuxMain:nativeMain,nativeMain:commonMain \
  -Xfragment-sources=linuxX64Main:$S1,linuxX64Main:$S2,nativeMain:$S3 \
  -Xfragments=linuxX64Main,linuxMain,nativeMain,commonMain \
  "$S1" "$S2" "$S3" > "$RUN/run.log" 2>&1
echo "exit=$? seconds=$(echo "$(date +%s.%N) - $START" | bc)"
echo "dependencies/ accesses: $(grep -c '/dependencies' "$RUN/strace.log")"
echo "~/.konan accesses: $(grep -c "$HOME/.konan" "$RUN/strace.log")"
find "$OUT" -name '*.kt' | sort
