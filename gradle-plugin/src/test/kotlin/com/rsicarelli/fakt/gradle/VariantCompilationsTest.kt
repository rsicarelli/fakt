// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VariantCompilationsTest {
    @Test
    fun `GIVEN debug and release compilations WHEN building the node THEN defaults are mains and members are the extra sets`() {
        val node =
            targetNodeOf(
                name = "android",
                platformType = "androidjvm",
                isAndroid = true,
                compilations =
                    listOf(
                        CompilationFacts(
                            "androidDebug",
                            listOf("androidDebug", "androidMain"),
                            false,
                        ),
                        CompilationFacts(
                            "androidRelease",
                            listOf("androidRelease", "androidMain"),
                            false,
                        ),
                    ),
            )

        assertEquals(
            TargetNode(
                "android",
                "androidjvm",
                true,
                listOf("androidDebug", "androidRelease"),
                listOf("androidMain"),
            ),
            node,
        )
    }

    @Test
    fun `GIVEN test compilations WHEN building the node THEN they contribute nothing`() {
        val node =
            targetNodeOf(
                name = "android",
                platformType = "androidjvm",
                isAndroid = true,
                compilations =
                    listOf(
                        CompilationFacts(
                            "androidDebug",
                            listOf("androidDebug", "androidMain"),
                            false,
                        ),
                        CompilationFacts(
                            "androidUnitTestDebug",
                            listOf("androidUnitTestDebug", "androidUnitTest"),
                            true,
                        ),
                    ),
            )

        assertEquals(listOf("androidDebug"), node.mainSourceSets)
        assertEquals(listOf("androidMain"), node.memberSourceSets)
    }

    @Test
    fun `GIVEN flavored compilations WHEN building the node THEN members are deduplicated in first seen order`() {
        val node =
            targetNodeOf(
                name = "android",
                platformType = "androidjvm",
                isAndroid = true,
                compilations =
                    listOf(
                        CompilationFacts(
                            "androidFreeDebug",
                            listOf(
                                "androidFreeDebug",
                                "androidMain",
                                "androidFree",
                                "androidDebug",
                            ),
                            false,
                        ),
                        CompilationFacts(
                            "androidFreeRelease",
                            listOf(
                                "androidFreeRelease",
                                "androidMain",
                                "androidFree",
                                "androidRelease",
                            ),
                            false,
                        ),
                    ),
            )

        assertEquals(
            listOf("androidMain", "androidFree", "androidDebug", "androidRelease"),
            node.memberSourceSets,
        )
    }

    @Test
    fun `GIVEN a jvm compilation without extra sets WHEN building the node THEN members are empty`() {
        val node =
            targetNodeOf(
                name = "jvm",
                platformType = "jvm",
                isAndroid = false,
                compilations = listOf(CompilationFacts("jvmMain", listOf("jvmMain"), false)),
            )

        assertEquals(TargetNode("jvm", "jvm", false, listOf("jvmMain")), node)
    }

    @Test
    fun `GIVEN no main compilation WHEN building the node THEN the target main set is the fallback`() {
        val node = targetNodeOf("desktop", "jvm", false, emptyList())

        assertEquals(listOf("desktopMain"), node.mainSourceSets)
        assertEquals(emptyList(), node.memberSourceSets)
    }

    @Test
    fun `GIVEN only test compilations WHEN building the node THEN the target main set is the fallback`() {
        val node =
            targetNodeOf(
                "desktop",
                "jvm",
                false,
                listOf(CompilationFacts("desktopTest", listOf("desktopTest"), true)),
            )

        assertEquals(listOf("desktopMain"), node.mainSourceSets)
    }
}
