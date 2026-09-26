// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleTarget

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

/**
 * Drives a JVM-only fake (platform output, jvmTest) together with a common fake (common output,
 * visible to jvmTest through commonTest) — both produced by the single `faktGenerateJvmMain` task.
 */
class SessionExporterTest {

    @Test
    fun `GIVEN a stored session WHEN exporting THEN writes it through the file store`() = runTest {
        // Given
        val session = Session(id = "s-7", userName = "linus")
        val directory = File("exports")
        val repository = fakeSessionRepository { load { session } }
        val store = fakeSessionFileStore { write { dir, s -> File(dir, "${s.id}.json") } }
        val exporter = SessionExporter(repository, store)

        // When
        val exported = exporter.export("s-7", directory)

        // Then
        assertEquals(File(directory, "s-7.json"), exported)
        assertEquals(listOf(session), store.writeCalls.value.map { it.session })
    }

    @Test
    fun `GIVEN no stored session WHEN exporting THEN nothing is written`() = runTest {
        // Given
        val store = fakeSessionFileStore()
        val exporter = SessionExporter(fakeSessionRepository(), store)

        // When
        val exported = exporter.export("missing", File("exports"))

        // Then
        assertNull(exported)
        assertEquals(0, store.writeCalls.value.size)
    }
}
