// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import java.io.File

/**
 * Self-heals a build directory when `fakt.useExperimentalGenerateTask` is flipped on over a warm
 * legacy build (issue #79, blocker 2).
 *
 * Under the legacy in-process path a KMP platform compilation that cache-missed wrote *common*
 * fakes into its own per-platform test dir (`build/generated/fakt/<target>Test/kotlin`). Once the
 * flag is on, the `commonMain` producer owns those fakes and writes the canonical copy into
 * `build/generated/fakt/commonTest/kotlin`. Nothing deletes the stale per-platform copy, so the
 * platform test compile sees the fake twice (its own dir + `commonTest` via KMP propagation) and
 * fails with `Redeclaration`.
 *
 * This cleaner runs from the still-legacy (in-process) platform *main* compilation — ordered after
 * the producer and before the platform *test* compilation — and removes the stale copy just in
 * time, so the migration build succeeds without a manual `clean`.
 */
internal object StaleFakeCleaner {

    /**
     * Deletes a stale legacy copy of a fake from [platformTestDir] when the same fake is now owned
     * by [commonTestDir] (the producer's output). The presence of the `commonTest` copy is the
     * ownership signal: the fake is a *common* fake, so any same-named file in the platform dir is
     * a leftover duplicate. A platform-specific fake (absent from `commonTest`) is never touched —
     * this is an ownership check, not a blanket wipe.
     *
     * Safe to call for every fake on every compilation: a no-op when this compilation *is* the
     * common producer ([platformTestDir] == [commonTestDir]), when no `commonTest` copy exists, or
     * when the platform copy is absent.
     *
     * @return the deleted [File], or `null` if nothing was removed.
     */
    fun pruneStaleCommonDuplicate(
        platformTestDir: File,
        commonTestDir: File,
        packagePath: String,
        fakeFileName: String,
    ): File? {
        // This compilation owns commonTest — there is no separate platform copy to prune.
        if (platformTestDir.canonicalFile == commonTestDir.canonicalFile) return null

        val commonCopy = commonTestDir.resolve(packagePath).resolve(fakeFileName)
        val staleCopy = platformTestDir.resolve(packagePath).resolve(fakeFileName)
        // Delete only a genuine duplicate: the fake is common-owned (present in commonTest) and a
        // distinct same-named file exists in the platform dir. Absent from commonTest ⇒ it is a
        // platform-specific fake and must be kept.
        val isStaleDuplicate =
            commonCopy.exists() &&
                staleCopy.exists() &&
                staleCopy.canonicalFile != commonCopy.canonicalFile
        return if (isStaleDuplicate && staleCopy.delete()) staleCopy else null
    }
}
