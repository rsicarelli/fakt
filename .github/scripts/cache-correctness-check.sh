#!/usr/bin/env bash
#
# Issue #79 regression contract.
#
# Fakt's generated `Fake*Impl.kt` used to be an undeclared side effect of `compileKotlin*`, so a
# warm Gradle build cache restored the compilation without the generated sources — leaving downstream
# test compilation with empty/missing fakes. The `FaktGenerateTask` producer makes those files real,
# cacheable task outputs. This script proves it stays that way:
#
#   1. warm the build cache by force-executing the producer tasks (`--rerun-tasks`), so they run and
#      store their outputs even if a prior local build left them UP-TO-DATE,
#   2. `clean` (which deletes the generated fakes), then
#   3. rebuild the producer tasks from cache and assert every producer is restored FROM-CACHE
#      (never re-executed — a re-execution means an unstable, non-relocatable cache key) and that the
#      generated `.kt` files physically reappear.
#
# The producer tasks are targeted directly rather than `collectFakes` or a test compilation: when a
# downstream task is itself FROM-CACHE it short-circuits and never schedules its producers, which
# would make the assertion vacuous.
#
# Non-drivable platform mains (Native/JS/Wasm) generate their platform-specific fakes via the
# in-process plugin, which is not cacheable by construction. Those fakes must still never be dropped,
# so when FAKT_PRESENCE_TASKS is set this script additionally runs those compile tasks and asserts
# every fake named in FAKT_PRESENCE_FAKES physically exists — present, not cached.
#
# Usage: cache-correctness-check.sh <project-path> [producer-task...]
#        (producer task defaults to `faktGenerateMetadataCommonMain`)
# Env:   FAKT_PRESENCE_TASKS  space-separated compile tasks that generate non-cacheable platform fakes
#        FAKT_PRESENCE_FAKES  space-separated fake class basenames asserted present after those tasks

set -euo pipefail

PROJECT_PATH="${1:?usage: cache-correctness-check.sh <project-path> [producer-task...]}"
shift || true
PRODUCER_TASKS=("$@")
if [ "${#PRODUCER_TASKS[@]}" -eq 0 ]; then
  PRODUCER_TASKS=("faktGenerateMetadataCommonMain")
fi

# No -Pfakt.useExperimentalGenerateTask here on purpose: the cache-correct path is Fakt's default,
# so this contract must hold for the configuration users actually get.
GRADLE=(./gradlew -p "$PROJECT_PATH" --build-cache --no-configuration-cache --console=plain)

count_fakes() {
  find "$PROJECT_PATH" -path '*build/generated*' -name 'Fake*.kt' 2>/dev/null | wc -l | tr -d ' '
}

# Force execution so the producers actually run and populate the build cache. Without --rerun-tasks a
# prior local build can leave them UP-TO-DATE (samples that set org.gradle.caching=false never seed
# the cache): the warm run would then store nothing, and the rebuild below would cache-miss and be
# misreported as an unstable cache key. See issue #79 P8 gap 4.
WARM_GRADLE=("${GRADLE[@]}" --rerun-tasks)

echo "::group::Warm cache (force-execute to seed) — ${PROJECT_PATH}: ${PRODUCER_TASKS[*]}"
warm_log="$(mktemp)"
"${WARM_GRADLE[@]}" "${PRODUCER_TASKS[@]}" | tee "$warm_log"
echo "::endgroup::"

# The warm run must actually EXECUTE the producers (only an executed @CacheableTask is stored). If it
# did not, the cache was never seeded, so the FROM-CACHE assertion below cannot be evaluated — report
# that as a harness/setup problem, distinct from an unstable cache key.
warm_total=$(grep -cE '^> Task .*faktGenerate' "$warm_log" || true)
warm_cached=$(grep -cE '^> Task .*faktGenerate.* (UP-TO-DATE|FROM-CACHE)' "$warm_log" || true)
warm_executed=$((warm_total - warm_cached))
if [ "$warm_executed" -le 0 ]; then
  echo "::error::Warm-up did not execute any faktGenerate producer (all UP-TO-DATE/FROM-CACHE despite --rerun-tasks) — the build cache was not seeded, so the FROM-CACHE contract cannot be evaluated. This is a harness/setup problem, not a cache-key regression."
  exit 1
fi

warm_count=$(count_fakes)
echo "Warm-up executed ${warm_executed} producer(s), generated ${warm_count} fake file(s)."
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

# Non-cacheable platform fakes (Native/JS/Wasm via the in-process plugin) must still be generated.
if [ -n "${FAKT_PRESENCE_TASKS:-}" ]; then
  echo "::group::Generate non-cacheable platform fakes — ${FAKT_PRESENCE_TASKS}"
  # shellcheck disable=SC2086
  "${GRADLE[@]}" $FAKT_PRESENCE_TASKS
  echo "::endgroup::"
  presence_fail=0
  for fake in ${FAKT_PRESENCE_FAKES:-}; do
    if [ -z "$(find "$PROJECT_PATH" -path '*build/generated*' -name "${fake}.kt" 2>/dev/null | head -n1)" ]; then
      echo "::error::Platform fake ${fake}.kt was not generated — a platform-specific @Fake was dropped."
      presence_fail=1
    else
      echo "Present (not cached): ${fake}.kt"
    fi
  done
  if [ "$presence_fail" -ne 0 ]; then
    exit 1
  fi
  echo "✅ Platform fakes present for ${PROJECT_PATH}: ${FAKT_PRESENCE_FAKES:-}."
fi
