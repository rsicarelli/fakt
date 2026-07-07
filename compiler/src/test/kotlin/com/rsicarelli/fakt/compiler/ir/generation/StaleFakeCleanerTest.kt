// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Pins the ownership contract of [StaleFakeCleaner], the flag-migration self-heal for issue #79
 * blocker 2: a common fake now owned by `commonTest` must have its stale per-platform legacy copy
 * removed, while a platform-specific fake (absent from `commonTest`) must be left untouched.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StaleFakeCleanerTest {

    private fun File.seedFake(relativeDir: String, packagePath: String, fileName: String): File =
        resolve(relativeDir).resolve(packagePath).resolve(fileName).apply {
            parentFile.mkdirs()
            writeText("// generated\n")
        }

    @Test
    fun `GIVEN a common fake present in both commonTest and a platform dir WHEN pruning THEN the platform copy is deleted`(
        @TempDir buildDir: File
    ) {
        val commonTestDir = buildDir.resolve("commonTest/kotlin")
        val platformDir = buildDir.resolve("iosSimulatorArm64Test/kotlin")
        val commonCopy = buildDir.seedFake("commonTest/kotlin", "com/foo", "FakeBarImpl.kt")
        val staleCopy =
            buildDir.seedFake("iosSimulatorArm64Test/kotlin", "com/foo", "FakeBarImpl.kt")

        val deleted =
            StaleFakeCleaner.pruneStaleCommonDuplicate(
                platformTestDir = platformDir,
                commonTestDir = commonTestDir,
                packagePath = "com/foo",
                fakeFileName = "FakeBarImpl.kt",
            )

        assertTrue(deleted == staleCopy, "The stale platform copy should be the deleted file.")
        assertFalse(staleCopy.exists(), "The stale platform copy must be removed.")
        assertTrue(commonCopy.exists(), "The canonical commonTest copy must survive.")
    }

    @Test
    fun `GIVEN a platform-specific fake absent from commonTest WHEN pruning THEN it is kept`(
        @TempDir buildDir: File
    ) {
        val commonTestDir = buildDir.resolve("commonTest/kotlin").apply { mkdirs() }
        val platformDir = buildDir.resolve("nativeTest/kotlin")
        // Only in the platform dir — this is a legitimately platform-owned fake.
        val platformOnly =
            buildDir.seedFake("nativeTest/kotlin", "com/foo", "FakeNativeOnlyServiceImpl.kt")

        val deleted =
            StaleFakeCleaner.pruneStaleCommonDuplicate(
                platformTestDir = platformDir,
                commonTestDir = commonTestDir,
                packagePath = "com/foo",
                fakeFileName = "FakeNativeOnlyServiceImpl.kt",
            )

        assertNull(deleted, "A platform-specific fake must not be deleted.")
        assertTrue(platformOnly.exists(), "The platform-specific fake must survive.")
    }

    @Test
    fun `GIVEN the platform dir equals the commonTest dir WHEN pruning THEN nothing is deleted`(
        @TempDir buildDir: File
    ) {
        val commonTestDir = buildDir.resolve("commonTest/kotlin")
        val copy = buildDir.seedFake("commonTest/kotlin", "com/foo", "FakeBarImpl.kt")

        val deleted =
            StaleFakeCleaner.pruneStaleCommonDuplicate(
                platformTestDir = commonTestDir,
                commonTestDir = commonTestDir,
                packagePath = "com/foo",
                fakeFileName = "FakeBarImpl.kt",
            )

        assertNull(deleted, "The producer's own commonTest output must never be pruned.")
        assertTrue(copy.exists(), "The commonTest copy must survive.")
    }

    @Test
    fun `GIVEN a common fake with no stale platform copy WHEN pruning THEN it is a no-op`(
        @TempDir buildDir: File
    ) {
        val commonTestDir = buildDir.resolve("commonTest/kotlin")
        val platformDir = buildDir.resolve("iosTest/kotlin").apply { mkdirs() }
        buildDir.seedFake("commonTest/kotlin", "com/foo", "FakeBarImpl.kt")

        val deleted =
            StaleFakeCleaner.pruneStaleCommonDuplicate(
                platformTestDir = platformDir,
                commonTestDir = commonTestDir,
                packagePath = "com/foo",
                fakeFileName = "FakeBarImpl.kt",
            )

        assertNull(deleted, "With no platform copy present there is nothing to prune.")
    }

    @Test
    fun `GIVEN a nested package path WHEN pruning THEN the correct nested platform copy is deleted`(
        @TempDir buildDir: File
    ) {
        val commonTestDir = buildDir.resolve("commonTest/kotlin")
        val platformDir = buildDir.resolve("linuxX64Test/kotlin")
        buildDir.seedFake("commonTest/kotlin", "com/rsicarelli/fakt/deep/pkg", "FakeDeepImpl.kt")
        val staleCopy =
            buildDir.seedFake(
                "linuxX64Test/kotlin",
                "com/rsicarelli/fakt/deep/pkg",
                "FakeDeepImpl.kt",
            )

        val deleted =
            StaleFakeCleaner.pruneStaleCommonDuplicate(
                platformTestDir = platformDir,
                commonTestDir = commonTestDir,
                packagePath = "com/rsicarelli/fakt/deep/pkg",
                fakeFileName = "FakeDeepImpl.kt",
            )

        assertTrue(deleted == staleCopy, "The nested stale copy should be deleted.")
        assertFalse(staleCopy.exists(), "The nested stale platform copy must be removed.")
    }
}
