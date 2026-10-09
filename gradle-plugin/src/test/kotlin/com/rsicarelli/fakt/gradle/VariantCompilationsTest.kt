// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    private fun info(name: String, vararg parents: String) = SourceSetInfo(name, parents.toList())

    @Test
    fun `GIVEN every extra set already listed WHEN appending THEN the infos are unchanged`() {
        val infos = listOf(info("jvmMain", "commonMain"), info("commonMain"))

        val result = appendMissingSourceSets(infos, listOf(info("commonMain"), info("jvmMain")))

        assertEquals(infos, result)
    }

    @Test
    fun `GIVEN missing sets WHEN appending THEN they follow the existing infos sorted by name`() {
        val infos = listOf(info("androidDebug", "commonMain"), info("commonMain"))
        val extra =
            listOf(
                info("androidMain", "commonMain"),
                info("androidFree", "commonMain"),
                info("commonMain"),
            )

        val result = appendMissingSourceSets(infos, extra)

        assertEquals(
            listOf(
                info("androidDebug", "commonMain"),
                info("commonMain"),
                info("androidFree", "commonMain"),
                info("androidMain", "commonMain"),
            ),
            result,
        )
    }

    @Test
    fun `GIVEN a duplicated missing name WHEN appending THEN it is appended once`() {
        val result = appendMissingSourceSets(listOf(info("a")), listOf(info("b"), info("b")))

        assertEquals(listOf(info("a"), info("b")), result)
    }

    @Test
    fun `GIVEN no infos and no extras WHEN appending THEN the result is empty`() {
        assertEquals(emptyList(), appendMissingSourceSets(emptyList(), emptyList()))
    }

    private fun androidNode(vararg mains: String) =
        TargetNode("android", "androidjvm", true, mains.toList(), listOf("androidMain"))

    @Test
    fun `GIVEN a single androidjvm target WHEN predicting the owner THEN commonMain is synthetic on the debug variant`() {
        val owner =
            predictSyntheticCommonMainOwner(listOf(androidNode("androidDebug", "androidRelease")))

        assertEquals(SourceSetOwner.Synthetic("commonMain", "android", "androidDebug"), owner)
    }

    @Test
    fun `GIVEN a single androidjvm target with flavors WHEN predicting the owner THEN the debug flavor represents`() {
        val owner =
            predictSyntheticCommonMainOwner(
                listOf(androidNode("androidFreeDebug", "androidFreeRelease", "androidPaidDebug"))
            )

        assertEquals("androidFreeDebug", owner?.compilationSourceSet)
    }

    @Test
    fun `GIVEN androidjvm next to jvm WHEN predicting the owner THEN there is none because metadata owns commonMain`() {
        val jvm = TargetNode("jvm", "jvm", false, listOf("jvmMain"))

        assertNull(predictSyntheticCommonMainOwner(listOf(androidNode("androidDebug"), jvm)))
    }

    @Test
    fun `GIVEN two jvm targets WHEN predicting the owner THEN the representative desktop owns commonMain`() {
        val owner =
            predictSyntheticCommonMainOwner(
                listOf(
                    TargetNode("server", "jvm", false, listOf("serverMain")),
                    TargetNode("desktop", "jvm", false, listOf("desktopMain")),
                )
            )

        assertEquals("desktop", owner?.target)
    }

    private val androidOwner = SourceSetOwner.Synthetic("commonMain", "android", "androidDebug")
    private val jvmOwner = SourceSetOwner.Synthetic("commonMain", "desktop", "desktopMain")

    @Test
    fun `GIVEN an androidjvm owner WHEN checking compilations THEN only the one whose default is the owner set represents`() {
        fun represents(compilation: String, default: String) =
            isSyntheticCommonMainRepresentative(androidOwner, compilation, default, "androidjvm")

        assertTrue(represents("debug", "androidDebug"))
        assertFalse(represents("release", "androidRelease"))
        assertFalse(represents("debugUnitTest", "androidUnitTestDebug"))
        assertFalse(represents("main", "androidMain"))
    }

    @Test
    fun `GIVEN a jvm owner WHEN checking compilations THEN only main represents whatever its default set`() {
        fun represents(compilation: String, default: String) =
            isSyntheticCommonMainRepresentative(jvmOwner, compilation, default, "jvm")

        assertTrue(represents("main", "desktopMain"))
        assertFalse(represents("test", "desktopTest"))
        assertFalse(represents("debug", "desktopMain"))
    }

    @Test
    fun `GIVEN no owner WHEN checking a compilation THEN it never represents`() {
        assertFalse(isSyntheticCommonMainRepresentative(null, "main", "jvmMain", "jvm"))
        assertFalse(
            isSyntheticCommonMainRepresentative(null, "debug", "androidDebug", "androidjvm")
        )
    }

    @Test
    fun `GIVEN an owner and a non jvm type WHEN checking a compilation THEN it does not represent`() {
        assertFalse(isSyntheticCommonMainRepresentative(androidOwner, "main", "jsMain", "js"))
    }

    @Test
    fun `GIVEN test shaped names WHEN deciding test likeness THEN they are tests`() {
        val names =
            listOf(
                "test",
                "integrationTest",
                "debugUnitTest",
                "freeReleaseUnitTest",
                "debugAndroidTest",
                "debugTestFixtures",
                "testFixtures",
                "hostTest",
            )

        val tests = names.filter { isTestLikeCompilation(it, associated = false) }

        assertEquals(names, tests)
    }

    @Test
    fun `GIVEN variant names that only contain test WHEN deciding test likeness THEN they stay main`() {
        val names =
            listOf("latestDebug", "contestRelease", "attestDebug", "testingDebug", "main", "debug")

        val tests = names.filter { isTestLikeCompilation(it, associated = false) }

        assertEquals(emptyList(), tests)
    }

    @Test
    fun `GIVEN an associated compilation WHEN deciding test likeness THEN it is a test whatever its name`() {
        assertTrue(isTestLikeCompilation("benchmark", associated = true))
    }
}
