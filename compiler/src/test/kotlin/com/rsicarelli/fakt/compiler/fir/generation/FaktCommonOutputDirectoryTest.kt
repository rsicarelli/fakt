// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.generation

import com.rsicarelli.fakt.compiler.api.EmitPhase
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Proves the explicit output routing behind a single-target KMP project (issue #153): when
 * [com.rsicarelli.fakt.compiler.api.SourceSetContext.commonOutputDirectory] is set, fakes declared
 * in the compilation's default source set (`jvmMain`) land in `outputDirectory`, while fakes from
 * every other source set (the common fragment, `commonMain`) land in `commonOutputDirectory`. The
 * Gradle plugin wires those two directories into `jvmTest` and `commonTest` respectively.
 *
 * Fixtures are written under real `src/<sourceSet>/kotlin/` paths because the emitter derives a
 * declaration's source set from its file path.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktCommonOutputDirectoryTest {

    @Test
    fun `GIVEN commonOutputDirectory WHEN compiling common and platform fakes THEN each lands in its own directory`() {
        // Given
        val base = Files.createTempDirectory("fakt-common-output").toFile()
        val platformDir = File(base, "out/jvmTest/kotlin")
        val commonDir = File(base, "out/commonTest/kotlin")
        try {
            val commonFixture =
                fixture(
                    base,
                    "commonMain",
                    "CommonRepository.kt",
                    """
                    package fixtures
                    import com.rsicarelli.fakt.Fake

                    @Fake
                    interface CommonRepository {
                        fun load(id: String): String
                    }
                    """,
                )
            val platformFixture =
                fixture(
                    base,
                    "jvmMain",
                    "JvmClock.kt",
                    """
                    package fixtures
                    import com.rsicarelli.fakt.Fake

                    @Fake
                    interface JvmClock {
                        fun now(): Long
                    }
                    """,
                )

            // When
            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures = listOf(commonFixture, platformFixture),
                    emitPhase = EmitPhase.FIR,
                    outputDir = platformDir,
                    defaultSourceSetName = "jvmMain",
                    commonOutputDir = commonDir,
                )

            // Then
            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            assertTrue(
                generatedNames(platformDir) == setOf("FakeJvmClockImpl.kt"),
                "the default source set's fake belongs in outputDirectory; got " +
                    generatedNames(platformDir),
            )
            assertTrue(
                generatedNames(commonDir) == setOf("FakeCommonRepositoryImpl.kt"),
                "common-fragment fakes belong in commonOutputDirectory; got " +
                    generatedNames(commonDir),
            )
        } finally {
            base.deleteRecursively()
        }
    }

    @Test
    fun `GIVEN no commonOutputDirectory WHEN compiling a default source set fake THEN it lands in outputDirectory`() {
        // Given
        val base = Files.createTempDirectory("fakt-common-output-default").toFile()
        val outputDir = File(base, "out/jvm/main/kotlin")
        try {
            val platformFixture =
                fixture(
                    base,
                    "jvmMain",
                    "JvmClock.kt",
                    """
                    package fixtures
                    import com.rsicarelli.fakt.Fake

                    @Fake
                    interface JvmClock {
                        fun now(): Long
                    }
                    """,
                )

            // When
            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures = listOf(platformFixture),
                    emitPhase = EmitPhase.FIR,
                    outputDir = outputDir,
                    defaultSourceSetName = "jvmMain",
                )

            // Then — unchanged routing: without a commonOutputDirectory nothing is split.
            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            assertEquals(setOf("FakeJvmClockImpl.kt"), generatedNames(outputDir))
        } finally {
            base.deleteRecursively()
        }
    }

    private fun fixture(base: File, sourceSet: String, name: String, code: String): SourceFile {
        val file = File(base, "src/$sourceSet/kotlin/fixtures/$name")
        file.parentFile.mkdirs()
        file.writeText(code.trimIndent())
        return SourceFile.fromPath(file)
    }

    private fun generatedNames(dir: File): Set<String> =
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.map { it.name }.toSet()
}
