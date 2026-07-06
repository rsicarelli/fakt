// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.generation

import com.rsicarelli.fakt.compiler.api.EmitPhase
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Behavior tests for [FirFakeEmitter] — the FIR-phase emission path used by the metadata-driver
 * producer. Each test compiles a fixture with the real plugin under [EmitPhase.FIR] and asserts on
 * the files written by the checker hook (byte parity with IR emission is locked separately by
 * [FirIrEmissionParityTest]).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirFakeEmitterTest {

    @Test
    fun `GIVEN fake interface WHEN emitting at FIR THEN impl factory and config are written`() {
        // GIVEN / WHEN
        val outcome =
            compileAtFir(
                """
                package emitter
                import com.rsicarelli.fakt.Fake

                @Fake
                interface GreetingService {
                    fun greet(name: String): String
                }
                """
            )

        // THEN
        assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
        val file = outcome.generatedFiles.entries.single()
        assertTrue(file.key.endsWith("FakeGreetingServiceImpl.kt"), "unexpected path: ${file.key}")
        assertTrue("class FakeGreetingServiceImpl" in file.value)
        assertTrue("fun fakeGreetingService(" in file.value)
        assertTrue("class FakeGreetingServiceConfig" in file.value)
    }

    @Test
    fun `GIVEN operator function WHEN emitting at FIR THEN override carries operator modifier`() {
        // GIVEN / WHEN
        val outcome =
            compileAtFir(
                """
                package emitter
                import com.rsicarelli.fakt.Fake

                @Fake
                interface Adder {
                    operator fun plus(other: Int): Int
                }
                """
            )

        // THEN: isOperator now comes from the FIR side-channel, not the IR node
        assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
        val content = outcome.generatedFiles.values.single()
        assertTrue(
            "operator override fun plus" in content,
            "operator modifier missing from FIR-emitted override:\n$content",
        )
    }

    @Test
    fun `GIVEN extension receiver function WHEN emitting at FIR THEN receiver type is preserved`() {
        // GIVEN / WHEN
        val outcome =
            compileAtFir(
                """
                package emitter
                import com.rsicarelli.fakt.Fake

                @Fake
                interface Formatter {
                    fun String.decorate(): String
                }
                """
            )

        // THEN: extension receiver now comes from the FIR side-channel
        assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
        val content = outcome.generatedFiles.values.single()
        assertTrue(
            "String.decorate()" in content,
            "extension receiver missing from FIR-emitted override:\n$content",
        )
    }

    @Test
    fun `GIVEN abstract class with constructor params WHEN emitting at FIR THEN params are forwarded`() {
        // GIVEN / WHEN
        val outcome =
            compileAtFir(
                """
                package emitter
                import com.rsicarelli.fakt.Fake

                @Fake
                abstract class BaseClient(val endpoint: String, val retries: Int = 3) {
                    abstract fun call(payload: String): String
                }
                """
            )

        // THEN: primary-constructor params now come from the FIR extraction — params without a
        // default are forwarded in the super(...) call; defaulted params (retries) are omitted
        assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
        val content = outcome.generatedFiles.values.single()
        assertTrue(
            ": BaseClient(endpoint = " in content,
            "super-call ctor forwarding missing:\n$content",
        )
    }

    @Test
    fun `GIVEN colliding fake output names WHEN emitting at FIR THEN first wins and error is logged`() {
        // GIVEN: two nested @Fake interfaces mapping to the same FakeSharedImpl.kt
        val outcome =
            compileAtFir(
                """
                package emitter
                import com.rsicarelli.fakt.Fake

                object FirstHost {
                    @Fake
                    interface Shared {
                        fun first(): Int
                    }
                }

                object SecondHost {
                    @Fake
                    interface Shared {
                        fun second(): Int
                    }
                }
                """
            )

        // THEN: exactly one file, and the collision was reported
        val sharedFiles = outcome.generatedFiles.keys.filter { it.endsWith("FakeSharedImpl.kt") }
        assertEquals(1, sharedFiles.size, "collision must yield exactly one FakeSharedImpl.kt")
        assertTrue(
            "Multiple @Fake declarations" in outcome.messages,
            "collision error missing from compiler output:\n${outcome.messages}",
        )
    }

    @Test
    fun `GIVEN call history disabled WHEN emitting at FIR THEN no call history members are generated`() {
        // GIVEN / WHEN
        val outcome =
            compileAtFir(
                """
                package emitter
                import com.rsicarelli.fakt.Fake

                @Fake
                interface PingService {
                    fun ping(): Boolean
                }
                """,
                enableCallHistory = false,
            )

        // THEN: plugin-level default resolved by the FIR emitter exactly like the IR path
        assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
        val content = outcome.generatedFiles.values.single()
        assertTrue(
            "callHistory" !in content,
            "callHistory members must not be generated when disabled:\n$content",
        )
    }

    @Test
    fun `GIVEN IR emit phase WHEN compiling THEN checker hook does not double-emit`() {
        // GIVEN: default IR phase — emission must come from the IR extension exactly once
        val dir = Files.createTempDirectory("emitter-ir").toFile()
        try {
            val outcome =
                FullPluginCompilationHarness.compile(
                    fixtures =
                        listOf(
                            SourceFile.kotlin(
                                "Fixture.kt",
                                """
                                package emitter
                                import com.rsicarelli.fakt.Fake

                                @Fake
                                interface SingleService {
                                    fun once(): Int
                                }
                                """
                                    .trimIndent(),
                            )
                        ),
                    emitPhase = EmitPhase.IR,
                    outputDir = dir,
                )

            // THEN: one file, and no FIR-emitter collision error (which double emission would log)
            assertEquals(KotlinCompilation.ExitCode.OK, outcome.exitCode, outcome.messages)
            assertEquals(1, outcome.generatedFiles.size)
            assertTrue("Multiple @Fake declarations" !in outcome.messages)
        } finally {
            dir.deleteRecursively()
        }
    }

    // -------------------------------------------------------------------------
    // Harness
    // -------------------------------------------------------------------------

    private fun compileAtFir(
        source: String,
        enableCallHistory: Boolean = true,
    ): FullPluginCompilationHarness.Outcome {
        val dir = Files.createTempDirectory("emitter-fir").toFile()
        return try {
            FullPluginCompilationHarness.compile(
                fixtures = listOf(SourceFile.kotlin("Fixture.kt", source.trimIndent())),
                emitPhase = EmitPhase.FIR,
                outputDir = dir,
                enableCallHistory = enableCallHistory,
            )
        } finally {
            dir.deleteRecursively()
        }
    }
}
