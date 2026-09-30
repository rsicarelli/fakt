#!/usr/bin/env bash
# C2: replays KGP's shared-native metadata K2Native arguments (captured with
# -Pkotlin.internal.compiler.arguments.log.level=warning) with Fakt swapped to FIR emission.
# Usage: c2-shared.sh <kotlin-version> <args-file> <sourceSet> <parent> <out-dir>
set -uo pipefail
VER="$1"; ARGS_FILE="$2"; SS="$3"; PARENT="$4"; RUN="$5"
HERE="$(cd "$(dirname "$0")/.." && pwd)"
: "${SPIKE_HOME:?}"
export KN_HOME="$SPIKE_HOME/dist/kotlin-native-prebuilt-linux-x86_64-$VER"
export FAKT_JAR="${FAKT_JAR:-$HOME/.m2/repository/com/rsicarelli/fakt/compiler/1.0.0-beta13/compiler-1.0.0-beta13.jar}"
export OUT="$RUN/out"
rm -rf "$RUN"; mkdir -p "$RUN"
export CONTEXT='{"compilationName":"'"$SS"'","targetName":"metadata","platformType":"native","isTest":false,"defaultSourceSet":{"name":"'"$SS"'","parents":["'"$PARENT"'"]},"allSourceSets":[{"name":"'"$SS"'","parents":["'"$PARENT"'"]}],"outputDirectory":"'"$OUT"'","commonTestOutputDirectory":"'"$OUT"'","emitPhase":"FIR","emitSourceSets":["'"$SS"'"]}'
# Drop KGP's own -output / -Xplugin / -P (the in-process Fakt) and the fixed flags the harness sets.
mapfile -t RAW < <(sed -e 's/^\t//' -e 's/^"//' -e 's/"$//' "$ARGS_FILE")
ARGS=(); skip=0
for a in "${RAW[@]}"; do
  if (( skip )); then skip=0; continue; fi
  case "$a" in
    -output|-P|-module-name|-produce) skip=1 ;;
    -Xplugin=*|-Xmetadata-klib) ;;
    *) ARGS+=("$a") ;;
  esac
done
START=$(date +%s.%N)
strace -f -qq -e trace=%file -o "$RUN/strace.log" "$HERE/bin/run-k2native.sh" "${ARGS[@]}" > "$RUN/run.log" 2>&1
echo "exit=$? seconds=$(echo "$(date +%s.%N) - $START" | bc)"
echo "dependencies/ accesses: $(grep -c '/dependencies/' "$RUN/strace.log")"
find "$OUT" -name '*.kt' | sort
