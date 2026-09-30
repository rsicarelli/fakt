---
description: Bootstrap the next v1.0 (#150) item — red tests first, then sample + CI contract, then implementation
argument-hint: "[issue number, e.g. 164 — omit to take the next unchecked #150 item]"
---

Work the next item of the **v1.0 tracker #150** (issue **$ARGUMENTS**, or the first unchecked
"Up next" item whose dependencies are merged when no argument is given).

Work **one step at a time**. Finish each step and show its evidence (command plus trimmed output)
before you start the next. Never write production code before a failing test that demands it.

## Step 0: Orient (read only)

1. Read #150: the body **and** the latest comments. It is the single source of truth for order,
   dependencies and decisions (D1–D3).
2. Read `.claude/docs/implementation/v1-sunset-handover.md`, especially §3 (decisions), §5
   (removal ordering), §6 (local verification) and §7 (gotchas).
3. Read the item's issue and all its comments, and any linked spike or report.
   - For #152 Native, the spike report is a comment on #152, and the throwaway prototype is on
     branch `ccr-aa1d21c5-gqlrfs` (`spike/native/`). Reuse its findings, not its code.
4. Confirm that every dependency named in #150 is merged into `main`. If one isn't, stop and say
   which.
5. Create the branch:
   `git fetch origin main && git checkout -b claude/<issue#>-<slug> origin/main`.

## Step 1: Plan the contract (no code)

Write a short plan in the chat:

- **Behaviors:** a numbered list of the observable behaviors this item adds or changes. Each one
  is a sentence a test can assert.
- **Test for each behavior**, at the lowest level that proves it:

  | Level | Where | Use for |
  |---|---|---|
  | Pure function | `gradle-plugin/src/test` (plain JUnit) | Routing (`routeCompilation`), ownership maps, argument builders |
  | Gradle model | `ProjectBuilder` test | Task registration, inputs, `srcDir` wiring, `dependsOn` |
  | Worker end to end | TestKit test (template: `FaktGenerateJsConsumerTest`) | Driver arguments, generated files, `UP_TO_DATE`, cache key |
  | Compiler | `compiler/src/test` | FIR checkers, emission, output routing |
  | Codegen | `codegen-runtime/src/test` | Generated code shape |

- **Sample contract:** name the sample under `samples/` that proves the shape (extend an existing
  one before adding a new one), and the exact CI cells:
  - `test-samples` in `.github/workflows/development.yml`: the sample's tests run;
  - `test-cache-correctness`: `producers:` lists every `faktGenerate*` task this item makes
    cache-correct, and `cached-fakes:` lists every fake they own. **Never add new
    `presence-tasks:`/`presence-fakes:`**; items that close a legacy shape remove them;
  - `clean-rebuild-cache-check.sh` when the item touches the consumer or test wiring.

Wait for the user to confirm the plan if any behavior is ambiguous. Otherwise continue.

## Step 2: RED, one behavior at a time

For behavior 1 (then 2, 3, …):

1. Write the test.
   - Follow `.claude/docs/development/validation/testing-guidelines.md`: GIVEN-WHEN-THEN names,
     `@TestInstance(PER_CLASS)`, vanilla JUnit5 + kotlin-test, fakes not mocks, no `@BeforeEach`.
2. Run **only that test** and show it failing:
   `./gradlew :<module>:test --tests '<Class>'`
3. Check that it fails **for the right reason**: an assertion about the missing behavior, not a
   compile error in the test itself, and not a setup problem. Fix the test until it does.

Keep red tests local. Don't push a red commit.

## Step 3: RED sample contract

Before implementing, add or extend the sample and the CI cells from step 1, then run the contract
locally and show it failing for the expected reason:

```bash
./gradlew publishToMavenLocal
./gradlew -p samples/<sample> <tests> --continue
FAKT_CACHED_FAKES="FakeXImpl ..." ./.github/scripts/cache-correctness-check.sh samples/<sample> <producer...>
```

The expected failure is usually "task not found", "re-executed instead of FROM-CACHE", or a fake
missing from the cache restore.

## Step 4: GREEN, smallest change per behavior

For each red test, in order:
1. Write the minimal production code that turns it green.
2. Rerun that test, then the module's whole suite.
3. Tell the user which behavior is now green before moving to the next.

Put new generation behavior on the FIR/worker path (`fir/`, `gradle-plugin/.../worker/`,
`FaktGenerateTaskWiring`). Don't extend `compiler/.../ir/`.

## Step 5: GREEN sample contract

Rerun step 3's commands until the sample tests pass and every producer is FROM-CACHE with every
listed fake restored. When the item touches caching or wiring, also run
`clean-rebuild-cache-check.sh`, and a second, relocated checkout of the sample.

## Step 6: Refactor and verify

Refactor while everything stays green. Split files instead of suppressing detekt (stock
thresholds: LongMethod 60, TooManyFunctions 11, LongParameterList 6, ReturnCount 2).

Then run the full local verification from handover §6:

```bash
export ANDROID_HOME=/root/android-sdk   # cloud sandbox
./gradlew spotlessApply spotlessCheck :gradle-plugin:detekt apiCheck :gradle-plugin:test publishToMavenLocal
./gradlew :compiler:test :compiler-api:test   # when compiler/ or compiler-api/ changed
```

- Run `apiDump` only for API changes you intend, and say which.
- Maven 429s are rate limits: retry, don't "fix" anything.

## Step 7: Ship

1. Commit with `/commit` (Conventional Commits).
2. Open a draft PR with `/pr`: `Closes #<n>` and `Part of #150`. The PR body lists each behavior
   with its test, and the sample + CI cells added.
3. Subscribe to the PR and drive CI to green.
4. After the squash merge, tick the item in #150, update "Last updated", and record anything the
   next item needs in the #150 comments and in the handover doc.

## Item notes (what "red" means for the open items)

- **#164a** (item 1):
  - `logLevel` change keeps the cache key (TestKit: second run FROM-CACHE);
  - `enabled=false` → task NO-SOURCE and old outputs deleted;
  - kotlinter-style `lintKotlin` does not depend on `faktGenerate*`, but `lintAnalyzeDebug`
    does;
  - `CodeGenerator` routes correctly when the checkout path contains `/commonTest/`;
  - no producer writes `fir-metadata.json` (#173). Assert on the file, not on timing.
- **#165** (item 2): the worker receives the compilation's `-opt-in`, language/API version,
  `-Xcontext-parameters`, explicit API and other compiler plugins (serialization). The sample
  uses at least an opt-in and one other compiler plugin in an `@Fake` signature.
- **#160 / #162** (items 3–4): the ownership function is a pure-function table test first, then
  `ProjectBuilder` wiring, then a sample with JVM-only targets or with `webMain`.
- **#152** (item 7): follow the spike report.
  - Routing: shared-native producer and leaf consumer.
  - Arguments test: `-Xrefines-paths`, `-no-default-libs`, `-target`.
  - TestKit with a real distribution: skip when `fakt.test.konanHome` is unset.
  - `kmp-multi-target` moves from `presence-*` to `producers`/`cached-fakes`.
  - Add a macOS cell (#167).
