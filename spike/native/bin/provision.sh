#!/usr/bin/env bash
# Downloads and unpacks kotlin-native-prebuilt-<ver>-linux-x86_64 into $SPIKE_HOME/dist.
# Usage: provision.sh <kotlin-version>
set -euo pipefail
VER="$1"
SPIKE_HOME="${SPIKE_HOME:?set SPIKE_HOME to a scratch dir}"
DEST="$SPIKE_HOME/dist"
DIR="$DEST/kotlin-native-prebuilt-linux-x86_64-$VER"
[[ -d "$DIR" ]] && { echo "$DIR"; exit 0; }
mkdir -p "$DEST"
URL="https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-native-prebuilt/$VER/kotlin-native-prebuilt-$VER-linux-x86_64.tar.gz"
for i in 1 2 3 4 5; do
  curl -sSfL -o "$DEST/kn-$VER.tgz" "$URL" && break
  sleep $((i * 5))
done
tar xzf "$DEST/kn-$VER.tgz" -C "$DEST"
rm -f "$DEST/kn-$VER.tgz"
echo "$DIR"
