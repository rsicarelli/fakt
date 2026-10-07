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
 * Proves the explicit output map (`SourceSetContext.outputDirectories`) on the FIR path: each owned
 * source set writes to its own route, and a source set that is not owned is never emitted. A
 * declaration whose source set cannot be read from its path uses the default source set, so it
 * fails closed when that set is not owned.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktOutputDirectoriesTest {

    @Test
    fun `GIVEN an output map WHEN compiling commonMain and jvmMain fakes THEN each lands in its own route`() {
        val base = Files.createTempDirectory("fakt-routes").toFile()
        val commonDir = File(base, "out/commonTest")
        val jvmDir = File(base, "out/jvmTest")
        try {
            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures = listOf(common(base), jvm(base)),
                    emitPhase = EmitPhase.FIR,
                    outputDir = File(base, "out/fallback"),
                    defaultSourceSetName = "jvmMain",
                    outputDirectories =
                        mapOf(
                            "commonMain" to commonDir.absolutePath,
                            "jvmMain" to jvmDir.absolutePath,
                        ),
                )

            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            assertEquals(setOf("FakeCommonRepositoryImpl.kt"), generatedNames(commonDir))
            assertEquals(setOf("FakeJvmClockImpl.kt"), generatedNames(jvmDir))
            assertEquals(emptySet(), generatedNames(File(base, "out/fallback")))
        } finally {
            base.deleteRecursively()
        }
    }

    @Test
    fun `GIVEN an output map without commonMain WHEN compiling a commonMain fake THEN it is not emitted`() {
        val base = Files.createTempDirectory("fakt-routes-unowned").toFile()
        val jvmDir = File(base, "out/jvmTest")
        try {
            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures = listOf(common(base), jvm(base)),
                    emitPhase = EmitPhase.FIR,
                    outputDir = File(base, "out/fallback"),
                    defaultSourceSetName = "jvmMain",
                    outputDirectories = mapOf("jvmMain" to jvmDir.absolutePath),
                )

            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            assertEquals(setOf("FakeJvmClockImpl.kt"), generatedNames(jvmDir))
            assertEquals(emptySet(), generatedNames(File(base, "out/fallback")))
        } finally {
            base.deleteRecursively()
        }
    }

    @Test
    fun `GIVEN an unowned default source set WHEN the fake has no source set attribution THEN it is not emitted`() {
        val base = Files.createTempDirectory("fakt-routes-null-closed").toFile()
        val commonDir = File(base, "out/commonTest")
        try {
            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures = listOf(unattributed(base)),
                    emitPhase = EmitPhase.FIR,
                    outputDir = File(base, "out/fallback"),
                    defaultSourceSetName = "jvmMain",
                    outputDirectories = mapOf("commonMain" to commonDir.absolutePath),
                )

            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            assertEquals(emptySet(), generatedNames(commonDir))
            assertEquals(emptySet(), generatedNames(File(base, "out/fallback")))
        } finally {
            base.deleteRecursively()
        }
    }

    @Test
    fun `GIVEN an owned default source set WHEN the fake has no source set attribution THEN it goes to the default route`() {
        val base = Files.createTempDirectory("fakt-routes-null-open").toFile()
        val jvmDir = File(base, "out/jvmTest")
        try {
            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures = listOf(unattributed(base)),
                    emitPhase = EmitPhase.FIR,
                    outputDir = File(base, "out/fallback"),
                    defaultSourceSetName = "jvmMain",
                    outputDirectories = mapOf("jvmMain" to jvmDir.absolutePath),
                )

            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            assertEquals(setOf("FakeLooseImpl.kt"), generatedNames(jvmDir))
        } finally {
            base.deleteRecursively()
        }
    }

    @Test
    fun `GIVEN a source set Fakt does not know WHEN the fake is skipped THEN a warning names it`() {
        val base = Files.createTempDirectory("fakt-routes-warn-unknown").toFile()
        val jvmDir = File(base, "out/jvmTest")
        try {
            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures = listOf(customLayout(base)),
                    emitPhase = EmitPhase.FIR,
                    outputDir = File(base, "out/fallback"),
                    defaultSourceSetName = "jvmMain",
                    outputDirectories = mapOf("jvmMain" to jvmDir.absolutePath),
                    knownSourceSets = listOf("commonMain", "jvmMain"),
                )

            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            assertEquals(emptySet(), generatedNames(jvmDir))
            assertTrue(outcome.messages.contains("Odd"), outcome.messages)
            assertTrue(outcome.messages.contains("src/<sourceSet>/kotlin"), outcome.messages)
        } finally {
            base.deleteRecursively()
        }
    }

    @Test
    fun `GIVEN no source set attribution WHEN the default set is not owned THEN a warning is shown`() {
        val base = Files.createTempDirectory("fakt-routes-warn-null").toFile()
        val commonDir = File(base, "out/commonTest")
        try {
            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures = listOf(unattributed(base)),
                    emitPhase = EmitPhase.FIR,
                    outputDir = File(base, "out/fallback"),
                    defaultSourceSetName = "jvmMain",
                    outputDirectories = mapOf("commonMain" to commonDir.absolutePath),
                    knownSourceSets = listOf("commonMain", "jvmMain"),
                )

            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            assertEquals(emptySet(), generatedNames(commonDir))
            assertTrue(outcome.messages.contains("Loose"), outcome.messages)
        } finally {
            base.deleteRecursively()
        }
    }

    @Test
    fun `GIVEN a known source set this task does not own WHEN the fake is skipped THEN no warning`() {
        val base = Files.createTempDirectory("fakt-routes-quiet").toFile()
        val jvmDir = File(base, "out/jvmTest")
        try {
            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures = listOf(common(base), jvm(base)),
                    emitPhase = EmitPhase.FIR,
                    outputDir = File(base, "out/fallback"),
                    defaultSourceSetName = "jvmMain",
                    outputDirectories = mapOf("jvmMain" to jvmDir.absolutePath),
                    knownSourceSets = listOf("commonMain", "jvmMain"),
                )

            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            assertEquals(setOf("FakeJvmClockImpl.kt"), generatedNames(jvmDir))
            assertFalse(outcome.messages.contains("CommonRepository"), outcome.messages)
        } finally {
            base.deleteRecursively()
        }
    }

    /** A real source set folder name that no Gradle source set of the compilation uses. */
    private fun customLayout(base: File): SourceFile =
        fixture(File(base, "src/oddMain/kotlin/fixtures/Odd.kt"), "Odd", "fun run(): Int")

    private fun common(base: File): SourceFile =
        fixture(
            File(base, "src/commonMain/kotlin/fixtures/CommonRepository.kt"),
            "CommonRepository",
            "fun load(id: String): String",
        )

    private fun jvm(base: File): SourceFile =
        fixture(
            File(base, "src/jvmMain/kotlin/fixtures/JvmClock.kt"),
            "JvmClock",
            "fun now(): Long",
        )

    /** A path with no `src/<set>/kotlin` segment, so the source set cannot be read. */
    private fun unattributed(base: File): SourceFile =
        fixture(File(base, "loose/Loose.kt"), "Loose", "fun run(): Int")

    private fun fixture(file: File, name: String, member: String): SourceFile {
        file.parentFile.mkdirs()
        file.writeText(
            """
            package fixtures
            import com.rsicarelli.fakt.Fake

            @Fake
            interface $name {
                $member
            }
            """
                .trimIndent()
        )
        return SourceFile.fromPath(file)
    }

    private fun generatedNames(dir: File): Set<String> =
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.map { it.name }.toSet()
}
