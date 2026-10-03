// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.checkers

import com.rsicarelli.fakt.compiler.api.EmitPhase
import com.rsicarelli.fakt.compiler.fir.generation.FullPluginCompilationHarness
import com.tschuchort.compiletesting.SourceFile
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * A `@Fake` whose signature touches an unresolved type must fail loudly with a `[FAKT]` error and
 * emit no fake, while unresolved code OUTSIDE any `@Fake` must not stop emission.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeUnresolvedTypeTest {

    @Test
    fun `GIVEN unresolved return type WHEN emitting at FIR THEN FAKT error and no fake`() {
        val outcome = compileAtFir("@Fake interface Api { fun load(): Missing }")

        assertRejected(outcome, "Api", "Missing")
    }

    @Test
    fun `GIVEN unresolved parameter type WHEN emitting at FIR THEN FAKT error and no fake`() {
        val outcome = compileAtFir("@Fake interface Api { fun save(item: Missing) }")

        assertRejected(outcome, "Api", "Missing")
    }

    @Test
    fun `GIVEN unresolved supertype WHEN emitting at FIR THEN FAKT error and no fake`() {
        val outcome = compileAtFir("@Fake interface Api : Missing { fun ping(): Int }")

        assertRejected(outcome, "Api", "Missing")
    }

    @Test
    fun `GIVEN unresolved type parameter bound WHEN emitting at FIR THEN FAKT error and no fake`() {
        val outcome = compileAtFir("@Fake interface Api<T : Missing> { fun ping(): Int }")

        assertRejected(outcome, "Api", "Missing")
    }

    @Test
    fun `GIVEN unresolved type nested in generic WHEN emitting at FIR THEN FAKT error`() {
        val outcome = compileAtFir("@Fake interface Api { fun all(): List<Missing> }")

        assertRejected(outcome, "Api", "Missing")
    }

    @Test
    fun `GIVEN unresolved type in inherited member WHEN emitting at FIR THEN FAKT error`() {
        val outcome =
            compileAtFir(
                """
                interface Base { fun load(): Missing }
                @Fake interface Api : Base { fun ping(): Int }
                """
            )

        assertRejected(outcome, "Api", "Missing")
    }

    @Test
    fun `GIVEN unresolved type in abstract class WHEN emitting at FIR THEN FAKT error`() {
        val outcome = compileAtFir("@Fake abstract class Api { abstract fun load(): Missing }")

        assertRejected(outcome, "Api", "Missing")
    }

    @Test
    fun `GIVEN fully resolved fake WHEN emitting at FIR THEN fake is generated`() {
        val outcome = compileAtFir("@Fake interface Api { fun load(): String }")

        assertTrue("[FAKT]" !in outcome.messages, outcome.messages)
        assertEquals(1, outcome.generatedFiles.size)
    }

    @Test
    fun `GIVEN unresolved type in non-fake declaration WHEN emitting at FIR THEN fake generates`() {
        val outcome =
            compileAtFir(
                """
                @Fake interface Api { fun load(): String }
                class Unrelated { fun broken(): Missing = TODO() }
                """
            )

        assertTrue("[FAKT]" !in outcome.messages, outcome.messages)
        assertEquals(1, outcome.generatedFiles.size)
    }

    @Test
    fun `GIVEN unresolved reference in another file WHEN emitting at FIR THEN fake generates`() {
        val outcome =
            compileFiles(
                listOf(
                    fixture("Fixture.kt", "@Fake interface Api { fun load(): String }"),
                    fixture("Other.kt", "fun use() = Unresolved.serializer()"),
                )
            )

        assertTrue("Unresolved" in outcome.messages, "other file error expected")
        assertTrue("[FAKT]" !in outcome.messages, outcome.messages)
        assertEquals(1, outcome.generatedFiles.size)
    }

    private fun assertRejected(
        outcome: FullPluginCompilationHarness.Outcome,
        fakeName: String,
        typeName: String,
    ) {
        assertTrue(
            "[FAKT] @Fake $fakeName references unresolved type(s) $typeName; no fake generated" in
                outcome.messages,
            "FAKT error missing:\n${outcome.messages}",
        )
        assertTrue(outcome.generatedFiles.isEmpty(), "no fake expected: ${outcome.generatedFiles}")
    }

    private fun fixture(name: String, body: String): SourceFile =
        SourceFile.kotlin(
            name,
            "package emitter\nimport com.rsicarelli.fakt.Fake\n\n" + body.trimIndent(),
        )

    private fun compileAtFir(body: String): FullPluginCompilationHarness.Outcome =
        compileFiles(listOf(fixture("Fixture.kt", body)))

    private fun compileFiles(fixtures: List<SourceFile>): FullPluginCompilationHarness.Outcome {
        val dir = Files.createTempDirectory("unresolved-fir").toFile()
        return try {
            FullPluginCompilationHarness.compile(
                fixtures = fixtures,
                emitPhase = EmitPhase.FIR,
                outputDir = dir,
            )
        } finally {
            dir.deleteRecursively()
        }
    }
}
