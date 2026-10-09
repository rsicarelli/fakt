// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntermediateTestWiringTest {
    private val owners: Map<String, SourceSetOwner> =
        mapOf(
            "commonMain" to SourceSetOwner.Metadata("commonMain"),
            "webMain" to SourceSetOwner.Metadata("webMain"),
            "jvmMain" to SourceSetOwner.Platform("jvm"),
            "sharedJvmMain" to SourceSetOwner.Platform("jvm"),
            "jsMain" to SourceSetOwner.Platform("js"),
            "desktopAndServerMain" to
                SourceSetOwner.Synthetic("desktopAndServerMain", "desktop", "desktopMain"),
            "otherMain" to SourceSetOwner.Synthetic("otherMain", "server", "serverMain"),
            "aaaMain" to SourceSetOwner.Synthetic("aaaMain", "desktop", "desktopMain"),
        )

    @Test
    fun `GIVEN a counterpart test set that is compiled WHEN wiring THEN only the counterpart receives the fakes`() {
        val wired =
            testWiringFor(
                owned = "webMain",
                compiledTestSourceSets = setOf("webTest", "jsTest", "commonTest"),
                leafTestSourceSets = setOf("jsTest", "wasmJsTest"),
            )

        assertEquals(setOf("webTest"), wired)
    }

    @Test
    fun `GIVEN no counterpart in any test compilation WHEN wiring THEN every leaf test set receives the fakes`() {
        val wired =
            testWiringFor(
                owned = "webMain",
                compiledTestSourceSets = setOf("jsTest", "wasmJsTest"),
                leafTestSourceSets = setOf("jsTest", "wasmJsTest"),
            )

        assertEquals(setOf("jsTest", "wasmJsTest"), wired)
    }

    @Test
    fun `GIVEN a synthetic intermediate with a compiled counterpart WHEN wiring THEN the counterpart is used`() {
        val wired =
            testWiringFor(
                "desktopAndServerMain",
                setOf("desktopAndServerTest", "desktopTest", "serverTest"),
                setOf("desktopTest", "serverTest"),
            )

        assertEquals(setOf("desktopAndServerTest"), wired)
    }

    @Test
    fun `GIVEN a synthetic intermediate without a counterpart WHEN wiring THEN desktopTest and serverTest are used`() {
        val wired =
            testWiringFor(
                "desktopAndServerMain",
                setOf("desktopTest", "serverTest"),
                setOf("desktopTest", "serverTest"),
            )

        assertEquals(setOf("desktopTest", "serverTest"), wired)
    }

    @Test
    fun `GIVEN a counterpart that is not compiled by a test compilation WHEN wiring THEN leaves are used`() {
        val wired = testWiringFor("webMain", setOf("jsTest"), setOf("jsTest"))

        assertEquals(setOf("jsTest"), wired)
    }

    @Test
    fun `GIVEN synthetic owners across targets WHEN asking for one target THEN only its synthetic sets come back sorted`() {
        val result = syntheticOwnersOf(owners, "desktop")

        assertEquals(listOf("aaaMain", "desktopAndServerMain"), result.map { it.sourceSet })
    }

    @Test
    fun `GIVEN a target with no synthetic sets WHEN asking for its synthetic owners THEN the result is empty`() {
        assertEquals(emptyList(), syntheticOwnersOf(owners, "jvm"))
    }

    @Test
    fun `GIVEN ancestors of jvmMain WHEN asking for platform owned ones THEN only sets owned by that target remain`() {
        val result =
            platformOwnedAncestors(
                owners,
                "jvm",
                linkedSetOf("commonMain", "sharedJvmMain", "webMain", "jsMain", "unknownMain"),
            )

        assertEquals(setOf("sharedJvmMain"), result)
    }

    @Test
    fun `GIVEN a target that owns no ancestor WHEN asking for platform owned ones THEN the result is empty`() {
        val result = platformOwnedAncestors(owners, "server", setOf("commonMain", "sharedJvmMain"))

        assertEquals(emptySet(), result)
    }
}
