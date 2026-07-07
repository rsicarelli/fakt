// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.generation

import com.rsicarelli.fakt.compiler.api.EmitPhase
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * End-to-end proof of the issue #79 blocker-2 self-heal: when the experimental flag is flipped on
 * over a warm legacy build, a stale per-platform copy of a common fake must be removed during the
 * (still-legacy) platform compilation so the platform test compile does not see it twice.
 *
 * Reproduced at the compiler level by giving the plugin a platform `outputDirectory` distinct from
 * the `commonTest` producer dir, pre-seeding the stale platform copy plus the canonical commonTest
 * copy, then compiling the common `@Fake` under [EmitPhase.IR] (the legacy in-process path).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlagMigrationSelfHealTest {

    private val fakeRelativePath = "migration/FakeMigratedServiceImpl.kt"

    @Test
    fun `GIVEN a stale platform copy of a common fake WHEN the platform compiles THEN the stale copy is removed`() {
        val platformDir = Files.createTempDirectory("migration-platform").toFile()
        val commonTestDir = Files.createTempDirectory("migration-common").toFile()
        try {
            // The commonMain producer already wrote the canonical copy into commonTest...
            val commonCopy = seed(commonTestDir, "// producer output\n")
            // ...and a prior legacy cache-miss left a stale copy in this platform's own test dir.
            val staleCopy = seed(platformDir, "// stale legacy copy\n")

            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures =
                        listOf(
                            SourceFile.kotlin(
                                "MigratedService.kt",
                                """
                                package migration
                                import com.rsicarelli.fakt.Fake

                                @Fake
                                interface MigratedService {
                                    fun run(): Int
                                }
                                """
                                    .trimIndent(),
                            )
                        ),
                    emitPhase = EmitPhase.IR,
                    outputDir = platformDir,
                    commonTestOutputDir = commonTestDir,
                )

            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            assertFalse(
                staleCopy.exists(),
                "The stale per-platform copy must be removed during the migration compile.",
            )
            assertTrue(
                commonCopy.exists(),
                "The canonical commonTest copy owned by the producer must survive.",
            )
        } finally {
            platformDir.deleteRecursively()
            commonTestDir.deleteRecursively()
        }
    }

    private fun seed(dir: File, content: String): File =
        File(dir, fakeRelativePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }
}
