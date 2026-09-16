#!/usr/bin/env bash
#
# Issue #142 regression contract.
#
# Reported as "plugin outputs don't seem to be invalidated on clean": with a warm Gradle build cache,
# `./gradlew clean <consumerTask>` restored the producing `compileKotlin*` FROM-CACHE, which skipped
# the in-process fake generation that rides along as an undeclared side effect of that task. The
# generated `.kt` files were never written, the consuming compilation saw an empty source dir, and
# the build failed with unresolved references to the fakes. `--no-build-cache` always worked.
#
# Where #79's contract (cache-correctness-check.sh) proves the `faktGenerate*` PRODUCERS restore from
# cache, this one proves the end-to-end shape the user actually runs: warm the cache with a real
# downstream task, then `clean` and run it again in a single invocation. The producer is deliberately
# NOT named on the command line — the point is that whatever the build does to satisfy
# <consumerTask>, the fakes must be on disk when it compiles.
#
#   1. warm run: `<tasks>` must succeed and must leave `Fake*.kt` on disk,
#   2. `clean <tasks>` in one invocation — the reporter's exact command — must succeed,
#   3. `Fake*.kt` must be on disk again. A build that "succeeds" with an empty generated dir still
#      fails this: the fakes are what downstream code compiles against.
#
# Usage: clean-rebuild-cache-check.sh <project-path> <task...>
# Env:   FAKT_GRADLE      gradle command, including any `-p` / `-P` arguments
#                         (default: `./gradlew -p <project-path>`)
#        FAKT_GRADLE_DIR  directory to run FAKT_GRADLE from (default: the repo root). Samples that
#                         pin their own Gradle wrapper pass their own path here plus
#                         FAKT_GRADLE="./gradlew".
#
# <project-path> is always repo-root-relative and is only used to find the generated fakes on disk.

set -euo pipefail

PROJECT_PATH="${1:?usage: clean-rebuild-cache-check.sh <project-path> <task...>}"
shift
if [ "$#" -eq 0 ]; then
  echo "::error::No tasks given — this contract needs a downstream consumer task (a test or lint task)."
  exit 1
fi
TASKS=("$@")

# shellcheck disable=SC2206  # intentional word splitting: FAKT_GRADLE carries its own arguments
GRADLE=(${FAKT_GRADLE:-./gradlew -p "$PROJECT_PATH"})
GRADLE+=(--build-cache --no-configuration-cache --console=plain)
RUN_DIR="${FAKT_GRADLE_DIR:-.}"
FAKES_ROOT="$PROJECT_PATH"

count_fakes() {
  find "$FAKES_ROOT" -path '*build/generated*' -name 'Fake*.kt' 2>/dev/null | wc -l | tr -d ' '
}

echo "::group::Warm the cache — ${PROJECT_PATH}: ${TASKS[*]}"
(cd "$RUN_DIR" && "${GRADLE[@]}" "${TASKS[@]}")
echo "::endgroup::"

warm_count=$(count_fakes)
echo "Warm run generated ${warm_count} fake file(s)."
if [ "$warm_count" -eq 0 ]; then
  echo "::error::The warm run generated no fakes at all — the contract cannot be evaluated. This is a harness/setup problem, not a cache regression."
  exit 1
fi

# The reporter's command: clean and the downstream task in ONE invocation, against a warm cache.
echo "::group::clean + rebuild against a warm cache — ${PROJECT_PATH}: clean ${TASKS[*]}"
rebuild_log="$(mktemp)"
rebuild_status=0
(cd "$RUN_DIR" && "${GRADLE[@]}" clean "${TASKS[@]}") 2>&1 | tee "$rebuild_log" || rebuild_status=1
echo "::endgroup::"

restored=$(count_fakes)
echo "After clean + rebuild: ${restored} fake file(s) on disk."

fail=0
if [ "$rebuild_status" -ne 0 ]; then
  echo "::error::\`clean ${TASKS[*]}\` failed against a warm build cache — issue #142 regressed."
  grep -m5 -E "Unresolved reference|^e: " "$rebuild_log" | sed 's/^/  /' || true
  fail=1
fi
if [ "$restored" -eq 0 ]; then
  echo "::error::No fakes on disk after \`clean ${TASKS[*]}\` with a warm cache — generation was skipped by a cache hit (issue #142)."
  grep -E '^> Task .*(compile[A-Za-z]*Kotlin|faktGenerate).*(FROM-CACHE|NO-SOURCE)' "$rebuild_log" | sed 's/^/  /' || true
  fail=1
fi

if [ "$fail" -ne 0 ]; then
  exit 1
fi

echo "✅ Issue #142 contract holds for ${PROJECT_PATH}: ${restored} fake file(s) present after clean + rebuild on a warm cache."
