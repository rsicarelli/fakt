#!/usr/bin/env bash
#
# Issue #79 regression contract.
#
# Fakt's generated `Fake*Impl.kt` used to be an undeclared side effect of `compileKotlin*`, so a
# warm Gradle build cache restored the compilation without the generated sources — leaving downstream
# test compilation with empty/missing fakes. The `FaktGenerateTask` producer makes those files real,
# cacheable task outputs. This script proves it stays that way:
#
#   1. warm the build cache for the producer tasks,
#   2. `clean` (which deletes the generated fakes), then
#   3. rebuild the producer tasks from cache and assert every producer is restored FROM-CACHE
#      (never re-executed — a re-execution means an unstable, non-relocatable cache key) and that the
#      generated `.kt` files physically reappear.
#
# The producer tasks are targeted directly rather than `collectFakes` or a test compilation: when a
# downstream task is itself FROM-CACHE it short-circuits and never schedules its producers, which
# would make the assertion vacuous.
#
# Usage: cache-correctness-check.sh <project-path> [producer-task...]
#        (producer task defaults to `faktGenerateMetadataCommonMain`)

set -euo pipefail

PROJECT_PATH="${1:?usage: cache-correctness-check.sh <project-path> [producer-task...]}"
shift || true
PRODUCER_TASKS=("$@")
if [ "${#PRODUCER_TASKS[@]}" -eq 0 ]; then
  PRODUCER_TASKS=("faktGenerateMetadataCommonMain")
fi

FLAG="-Pfakt.useExperimentalGenerateTask=true"
GRADLE=(./gradlew -p "$PROJECT_PATH" "$FLAG" --build-cache --no-configuration-cache --console=plain)

count_fakes() {
  find "$PROJECT_PATH" -path '*build/generated*' -name 'Fake*.kt' 2>/dev/null | wc -l | tr -d ' '
}

echo "::group::Warm cache — ${PROJECT_PATH}: ${PRODUCER_TASKS[*]}"
"${GRADLE[@]}" "${PRODUCER_TASKS[@]}"
echo "::endgroup::"
warm_count=$(count_fakes)
echo "Warm-up generated ${warm_count} fake file(s)."
if [ "$warm_count" -eq 0 ]; then
  echo "::error::No fakes were generated during warm-up — the producer task generated nothing."
  exit 1
fi

echo "::group::Clean"
"${GRADLE[@]}" clean
echo "::endgroup::"
after_clean=$(count_fakes)
if [ "$after_clean" -ne 0 ]; then
  echo "::error::clean left ${after_clean} generated fake file(s) behind."
  exit 1
fi

echo "::group::Rebuild from cache"
rebuild_log="$(mktemp)"
"${GRADLE[@]}" "${PRODUCER_TASKS[@]}" | tee "$rebuild_log"
echo "::endgroup::"

total=$(grep -cE '^> Task .*faktGenerate' "$rebuild_log" || true)
from_cache=$(grep -cE '^> Task .*faktGenerate.* FROM-CACHE' "$rebuild_log" || true)
reexecuted=$((total - from_cache))
restored=$(count_fakes)

echo "Producers: total=${total}, FROM-CACHE=${from_cache}, re-executed=${reexecuted}; fakes restored=${restored}"

fail=0
if [ "$total" -eq 0 ]; then
  echo "::error::No faktGenerate producer ran during the rebuild — the contract could not be evaluated."
  fail=1
fi
if [ "$from_cache" -eq 0 ]; then
  echo "::error::No faktGenerate producer was restored FROM-CACHE — issue #79 cacheability regressed."
  fail=1
fi
if [ "$reexecuted" -gt 0 ]; then
  echo "::error::${reexecuted} faktGenerate producer(s) re-executed instead of restoring FROM-CACHE — the cache key is unstable (likely an absolute path leaked into an input)."
  fail=1
fi
if [ "$restored" -eq 0 ]; then
  echo "::error::The build cache restored no fakes after clean — issue #79 regressed."
  fail=1
fi

if [ "$fail" -ne 0 ]; then
  exit 1
fi

echo "✅ Issue #79 contract holds for ${PROJECT_PATH}: ${restored} fake file(s) restored from cache, all ${total} producer(s) FROM-CACHE."
