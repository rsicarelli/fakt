// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pure unit coverage for [buildFragmentArgs], the builder of the `-Xfragments` shape the worker
 * uses when the analysed source sets have an intermediate level (`commonMain` -> `webMain` ->
 * `jsMain`). `null` means "keep today's `-Xcommon-sources` arguments".
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FragmentArgumentsTest {

    private val webChain =
        listOf(
            SourceSetInfo("jsMain", listOf("webMain")),
            SourceSetInfo("webMain", listOf("commonMain")),
            SourceSetInfo("commonMain", emptyList()),
        )

    private val webRoots =
        mapOf(
            "jsMain" to listOf("/p/src/jsMain/kotlin"),
            "webMain" to listOf("/p/src/webMain/kotlin"),
            "commonMain" to listOf("/p/src/commonMain/kotlin"),
        )

    private val webFiles =
        listOf(
            File("/p/src/jsMain/kotlin/A.kt"),
            File("/p/src/webMain/kotlin/B.kt"),
            File("/p/src/commonMain/kotlin/C.kt"),
        )

    @Test
    fun `GIVEN a three level chain WHEN building THEN fragments list ancestors before descendants`() {
        val args = assertNotNull(buildFragmentArgs(webChain, webFiles, webRoots))

        assertEquals(listOf("commonMain", "webMain", "jsMain"), args.fragments)
    }

    @Test
    fun `GIVEN files in each source set WHEN building THEN sources use name colon path syntax in fragment order`() {
        val files =
            listOf(
                File("/p/src/jsMain/kotlin/A.kt"),
                File("/p/src/webMain/kotlin/sub/B.kt"),
                File("/p/src/commonMain/kotlin/C.kt"),
            )

        val args = assertNotNull(buildFragmentArgs(webChain, files, webRoots))

        assertEquals(
            listOf(
                "commonMain:/p/src/commonMain/kotlin/C.kt",
                "webMain:/p/src/webMain/kotlin/sub/B.kt",
                "jsMain:/p/src/jsMain/kotlin/A.kt",
            ),
            args.sources,
        )
    }

    @Test
    fun `GIVEN a three level chain WHEN building THEN refines link each fragment to its parent`() {
        val args = assertNotNull(buildFragmentArgs(webChain, webFiles, webRoots))

        assertEquals(listOf("webMain:commonMain", "jsMain:webMain"), args.refines)
    }

    @Test
    fun `GIVEN a file under no source set root WHEN building THEN returns null`() {
        val files = webFiles + File("/p/build/generated/Stray.kt")

        assertNull(buildFragmentArgs(webChain, files, webRoots))
    }

    @Test
    fun `GIVEN only commonMain above the default set WHEN building THEN returns null`() {
        val sourceSets =
            listOf(
                SourceSetInfo("jsMain", listOf("commonMain")),
                SourceSetInfo("commonMain", emptyList()),
            )
        val files = listOf(File("/p/src/jsMain/kotlin/A.kt"), File("/p/src/commonMain/kotlin/C.kt"))

        assertNull(buildFragmentArgs(sourceSets, files, webRoots))
    }

    @Test
    fun `GIVEN an intermediate without any file WHEN building THEN returns null`() {
        val files = listOf(File("/p/src/jsMain/kotlin/A.kt"), File("/p/src/commonMain/kotlin/C.kt"))

        assertNull(buildFragmentArgs(webChain, files, webRoots))
    }

    @Test
    fun `GIVEN no known roots WHEN building THEN returns null`() {
        assertNull(buildFragmentArgs(webChain, webFiles, emptyMap()))
    }

    @Test
    fun `GIVEN nested roots WHEN attributing a file THEN the deepest root wins`() {
        val roots = webRoots + ("webMain" to listOf("/p/src/commonMain/kotlin/web"))
        val files =
            listOf(
                File("/p/src/jsMain/kotlin/A.kt"),
                File("/p/src/commonMain/kotlin/web/B.kt"),
                File("/p/src/commonMain/kotlin/C.kt"),
            )

        val args = assertNotNull(buildFragmentArgs(webChain, files, roots))

        assertEquals(
            listOf(
                "commonMain:/p/src/commonMain/kotlin/C.kt",
                "webMain:/p/src/commonMain/kotlin/web/B.kt",
                "jsMain:/p/src/jsMain/kotlin/A.kt",
            ),
            args.sources,
        )
    }

    @Test
    fun `GIVEN a diamond of intermediates WHEN building THEN every fragment follows all its parents`() {
        val sourceSets =
            listOf(
                SourceSetInfo("appMain", listOf("leftMain", "rightMain")),
                SourceSetInfo("leftMain", listOf("commonMain")),
                SourceSetInfo("rightMain", listOf("commonMain")),
                SourceSetInfo("commonMain", emptyList()),
            )
        val roots =
            listOf("appMain", "leftMain", "rightMain", "commonMain").associateWith {
                listOf("/p/src/$it/kotlin")
            }
        val files = roots.values.flatten().map { File("$it/X.kt") }

        val args = assertNotNull(buildFragmentArgs(sourceSets, files, roots))

        assertEquals(listOf("commonMain", "leftMain", "rightMain", "appMain"), args.fragments)
        assertEquals(
            listOf(
                "leftMain:commonMain",
                "rightMain:commonMain",
                "appMain:leftMain",
                "appMain:rightMain",
            ),
            args.refines,
        )
    }
}
