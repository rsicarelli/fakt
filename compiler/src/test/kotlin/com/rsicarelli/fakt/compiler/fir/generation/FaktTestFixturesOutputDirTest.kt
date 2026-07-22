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
 * Proves the compiler honors a `testFixtures` output directory end-to-end. The Gradle plugin routes
 * fakes to `build/generated/fakt/testFixtures/kotlin` whenever `useGradleTestFixtures` resolves
 * active (JVM `java-test-fixtures` OR Android `com.android.library`) — `SourceSetDiscovery` derives
 * that path from the same flag. This test drives the REAL plugin with such a
 * [com.rsicarelli.fakt.compiler.api.SourceSetContext] output directory and asserts the generated
 * fakes physically land there, without needing Gradle or the Android SDK.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktTestFixturesOutputDirTest {

    @Test
    fun `GIVEN testFixtures output dir WHEN plugin generates THEN fakes are written under it`() {
        // Given — the exact relative shape the Gradle plugin computes for test-fixtures mode.
        val base = Files.createTempDirectory("fakt-fixtures").toFile()
        val outputDir = File(base, "generated/fakt/testFixtures/kotlin")
        try {
            // When
            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures =
                        listOf(
                            SourceFile.kotlin(
                                "Fixture.kt",
                                """
                                package fixtures
                                import com.rsicarelli.fakt.Fake

                                @Fake
                                interface UserRepository {
                                    fun findById(id: Long): String
                                }
                                """
                                    .trimIndent(),
                            )
                        ),
                    emitPhase = EmitPhase.IR,
                    outputDir = outputDir,
                )

            // Then — the fake compiled and its file exists under the testFixtures directory.
            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            val generatedPath =
                outcome.generatedFiles.keys.singleOrNull {
                    it.endsWith("FakeUserRepositoryImpl.kt")
                }
            assertTrue(
                generatedPath != null,
                "expected FakeUserRepositoryImpl.kt in ${outcome.generatedFiles.keys}",
            )
            assertTrue(
                File(outputDir, generatedPath).isFile,
                "generated fake must physically exist under the testFixtures/kotlin output dir",
            )
            assertTrue(
                outputDir.path.replace(File.separatorChar, '/').endsWith("testFixtures/kotlin"),
                "precondition: the driven output dir is the testFixtures path",
            )
        } finally {
            base.deleteRecursively()
        }
    }
}
