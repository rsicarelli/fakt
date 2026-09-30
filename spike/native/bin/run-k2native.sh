#!/usr/bin/env bash
# Runs K2Native from a K/N distribution with Fakt as -Xplugin, emitting fakes at the FIR phase,
# exactly the way a NATIVE worker driver would (no Gradle).
#
# Env:
#   KN_HOME     distribution root (contains konan/lib/kotlin-native-compiler-embeddable.jar)
#   FAKT_JAR    Fakt compiler plugin jar (com.rsicarelli.fakt:compiler)
#   CONTEXT     SourceSetContext JSON (emitPhase must be FIR)
#   OUT         output dir for fakes; the klib goes to $OUT.klib-scratch
#   HEAP        optional, default 1g
# Args: everything after `--` is passed to K2Native verbatim (target, -library, sources, ...).
set -euo pipefail
: "${KN_HOME:?}" "${FAKT_JAR:?}" "${CONTEXT:?}" "${OUT:?}"
mkdir -p "$OUT" "$OUT.klib-scratch"
CTX_B64=$(printf '%s' "$CONTEXT" | base64 -w0)
P=plugin:com.rsicarelli.fakt
exec java -Xmx"${HEAP:-1g}" -XX:MaxMetaspaceSize=512m \
  -Dkonan.home="$KN_HOME" \
  -cp "$KN_HOME/konan/lib/kotlin-native-compiler-embeddable.jar" \
  org.jetbrains.kotlin.cli.bc.K2Native \
  -produce library -Xmetadata-klib \
  -module-name fakt-analysis \
  -output "$OUT.klib-scratch/fakt-analysis" \
  -Xplugin="$FAKT_JAR" \
  -P "$P:enabled=true" -P "$P:logLevel=${LOG_LEVEL:-INFO}" -P "$P:outputDir=$OUT" \
  -P "$P:sourceSetContext=$CTX_B64" \
  -P "$P:enableCallHistory=true" -P "$P:enableMutableFakes=false" \
  "$@"
