# Common Issues & Solutions - Fakt Development

> **Purpose**: Quick solutions for common issues when developing the Fakt compiler plugin
> **Architecture**: fakes are emitted at FIR by `FaktGenerateTask` (default) or by the legacy in-process plugin (see `CLAUDE.md`)
> **User-facing limitations**: `docs/user-guide/known-issues.md`

## 🚨 **Compilation Issues**

### **Issue: "Unresolved reference: Fake"**

**Solution:** add the annotations artifact to the source set that declares the `@Fake` types:
```kotlin
dependencies {
    implementation(libs.fakt.annotations) // com.rsicarelli.fakt:annotations
}
```
The Gradle plugin (`id("com.rsicarelli.fakt")`) supplies the compiler plugin; don't add the compiler as a dependency.

### **Issue: "My plugin change has no effect"**

**Solution:** samples resolve the plugin from Maven Local, so republish before rebuilding:
```bash
make publish-local
./gradlew -p samples/<sample> build
```

### **Issue: "Fake generation not happening"**

**Solution:**
1. The type is annotated with `@Fake` in a main source set (`main`/`commonMain`); fakes are generated into the matching test source set.
2. The `faktGenerate*` task ran: `./gradlew -p samples/<sample> tasks --all | grep faktGenerate`.
3. Look for output in `samples/<sample>/build/generated/fakt/<testSourceSet>/kotlin/`.
4. Turn on logs with `fakt { logLevel.set(LogLevel.DEBUG) }`, or run `make debug`.

**Root Cause**: usually a stale publish, a type outside a main source set, or a compilation routed to the legacy path (Native mains) where the in-process plugin generates instead.

### **Issue: "Generated code doesn't compile"**

**Solution:**
1. Read the generated file under `build/generated/fakt/` and compare it with the source type.
2. Find which path produced it: `faktGenerate*` task (FIR emission, `compiler/.../fir/generation/`) or legacy in-process (`UnifiedFaktIrGenerationExtension`). Re-run with `-Pfakt.useExperimentalGenerateTask=false` to compare.
3. Fix the generator in `codegen-runtime/` and add a GIVEN-WHEN-THEN test plus a sample scenario that reproduces it.

**Root Cause**: a code generation bug. Generics (class-level, method-level, bounded), `suspend`, overloads and properties are all supported, so a mismatch there is a bug, not a limitation.

## 🔧 **IDE Integration Issues**

### **Issue: "Generated fakes not visible in IDE"**

**Solution:**
1. Build once so the `faktGenerate*` tasks run, then sync the Gradle project.
2. If the IDE still shows red: **File > Invalidate Caches and Restart**.

**Root Cause**: the IDE only indexes generated sources after a build has produced them.

## 🔗 **Cross-Module Issues**

### **Issue: "Fakes not visible across modules"**

**Solution:** use a collector module (see `samples/kmp-multi-module`):
```kotlin
fakt {
    @OptIn(com.rsicarelli.fakt.gradle.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.features.notifications)
}
```
For JVM, Gradle test fixtures also work (`fakt { useGradleTestFixtures.set(true) }`, see `samples/jvm-test-fixtures`). User docs: `docs/user-guide/multi-module.md`.

## 📊 **Performance Issues**

### **Issue: "OutOfMemoryError during fake generation"**

**Solution:** check where it happened. The `faktGenerate*` worker runs in its own JVM with a fixed 1 GB heap (`WORKER_MAX_HEAP` in `FaktGenerateTask.kt`), so `GRADLE_OPTS` / `org.gradle.jvmargs` don't change it. For the legacy in-process path, raise `kotlin.daemon.jvmargs`.

## 🧱 **Build / Gradle Issues**

### **Issue: `kotlinWasmStoreYarnLock` (or `kotlinStoreYarnLock`) FAILED — "input file was expected but doesn't exist"**
```
> Task :kotlinWasmStoreYarnLock FAILED
  property 'inputFile' specifies file '.../build/wasm/yarn.lock'
  which doesn't exist. Reason: An input file was expected to be present but it doesn't exist.
```

**Solution:**
1. Un-ignore the store locks — the bare `yarn.lock` rule in `.gitignore` is too broad; keep it (it covers `build/**` locks) but add a negation so the committed store lock is tracked:
```gitignore
yarn.lock
!**/kotlin-js-store/**/yarn.lock
```
2. Generate the committed locks per KMP sample (needs node/npm + network):
```bash
./gradlew -p samples/<sample> kotlinUpgradeYarnLock kotlinWasmUpgradeYarnLock
# produces vendor/kotlin-js-store/yarn.lock and vendor/kotlin-js-store/wasm/yarn.lock
# (relocated from the KGP default kotlin-js-store/ by FaktSampleKmpPlugin - see below)
```
3. Commit `vendor/kotlin-js-store/**/yarn.lock`. Do this for every KMP sample, not just the one that failed.

**Verify:** `:kotlinWasmRestoreYarnLock` now RUNS (not SKIPPED) and `:kotlinWasmStoreYarnLock` is UP-TO-DATE.

**Root Cause**: With no committed `kotlin-js-store/**/yarn.lock`, `RestoreYarnLock` is SKIPPED, so the only producer of `build/**/yarn.lock` is `NpmInstall` — and `StoreYarnLock` races it. Large module graphs (e.g. `kmp-multi-module`, ~25 modules) lose the race; small samples win, so it looks intermittent. Gradle **9.5.1** escalated the "input file absent" validation from a deprecation *warning* (9.0.0) to a *hard failure*, which is why the wrapper bump surfaced it. Committing the store lock (the KGP-recommended practice) makes the build deterministic. Do **not** modify `gradle-wrapper` files to work around this.

**Fallbacks** (only if the committed-lock route is insufficient — report tradeoffs, don't silently switch): `YarnRootExtension` settings (`yarnLockMismatchReport` / `yarnLockAutoReplace` / `reportNewYarnLock`), or disabling the `*StoreYarnLock` / `*UpgradeYarnLock` tasks in `build-logic/src/main/kotlin/FaktSampleKmpPlugin.kt` (least clean — masks rather than fixes).

### **Issue: Dependabot "npm_and_yarn" Security Update jobs fail with `.../kotlin-js-store/package.json not found`**
```
Error during file fetching; aborting: /samples/kmp-multi-target/kotlin-js-store/package.json not found
```

**Root Cause**: GitHub's Dependency Graph indexes the committed `kotlin-js-store/yarn.lock` / `package-lock.json` files as `npm_and_yarn` manifests. There's no sibling `package.json` next to them — it's KGP build output, correctly gitignored, not a real npm project — so any Dependabot Security Update job triggered by a matching advisory fails deterministically, forever. `.gitattributes` (`linguist-generated`) and `.github/dependabot.yml` `ignore` rules do **not** stop this (verified: neither is documented to affect the dependency graph's manifest scanning or security-update path scoping).

**Solution**: relocate the lock file directories to `vendor/kotlin-js-store/` (done — see `FaktRootPlugin.kt` and `FaktSampleKmpPlugin.kt`). GitHub's dependency graph parser skips manifests under directories matching vendor-style naming (`vendor/`, `third-party/`, `external/` — see [Troubleshooting the dependency graph](https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/troubleshooting-the-dependency-graph)), so these lockfiles stop being indexed and no more Security Update jobs are spawned against them. This is configured via the Kotlin Gradle Plugin's documented `lockFileDirectory` property on `YarnRootExtension`/`WasmYarnRootExtension` (yarn-based sample builds) and `NpmExtension`/`WasmNpmExtension` (npm-based root build).

**Verify**: after merging to the default branch, check the repo's Insights → Dependency graph page — the `vendor/kotlin-js-store/**` manifests should no longer be listed, and no new `npm_and_yarn` jobs should appear for future advisories.

## 🚀 **Quick Diagnostic Commands**

```bash
# Unit tests for every module
./gradlew test

# End-to-end sample (republishes the plugin first)
make publish-local && make test-sample

# Fakt log lines from the KMP sample
make debug

# Every check CI runs
make validate
```

---

**For issues not covered here, reproduce them in a sample scenario or a GIVEN-WHEN-THEN test first (see the [testing guidelines](../development/validation/testing-guidelines.md)).**
