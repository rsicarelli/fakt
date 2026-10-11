// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Pins B13 to B17 of #152: where the Kotlin/Native distribution lives, and when a directory is not
 * one. The lookup mirrors KGP 2.4.10's `NativePropertiesLoader`, so Fakt reads the directory KGP
 * provisioned.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NativeDistributionTest {

    private val home = File("home").resolve("dev").path
    private val dirName = "kotlin-native-prebuilt-linux-x86_64-2.4.10"

    private fun query(
        nativeHome: String? = null,
        dataDirProperty: String? = null,
        dataDirEnv: String? = null,
        version: String = "2.4.10",
    ) =
        NativeDistributionQuery(
            nativeHome = nativeHome,
            konanDataDirProperty = dataDirProperty,
            konanDataDirEnv = dataDirEnv,
            userHome = home,
            version = version,
            osName = "Linux",
            osArch = "amd64",
        )

    private fun distribution(root: File, jar: String) {
        val lib = root.resolve("konan").resolve("lib")
        lib.mkdirs()
        lib.resolve(jar).writeText("")
    }

    @Test
    fun `GIVEN kotlin native home WHEN resolving THEN it is used as is, ignoring env and user home`() {
        val result = resolveKonanHome(query(nativeHome = "custom-k2n", dataDirEnv = "env-dir"))

        assertEquals(File("custom-k2n"), result)
    }

    @Test
    fun `GIVEN only a data dir property WHEN resolving THEN the host distribution is inside it`() {
        val result = resolveKonanHome(query(dataDirProperty = "prop-dir"))

        assertEquals(File("prop-dir").resolve(dirName), result)
    }

    @Test
    fun `GIVEN a data dir property and kotlin native home WHEN resolving THEN the property wins as in KGP`() {
        val result =
            resolveKonanHome(query(nativeHome = "custom-k2n", dataDirProperty = "prop-dir"))

        assertEquals(File("prop-dir").resolve(dirName), result)
    }

    @Test
    fun `GIVEN a property and an env data dir WHEN resolving THEN the property wins`() {
        val result = resolveKonanHome(query(dataDirProperty = "prop-dir", dataDirEnv = "env-dir"))

        assertEquals(File("prop-dir").resolve(dirName), result)
    }

    @Test
    fun `GIVEN only an env data dir WHEN resolving THEN the distribution is inside it`() {
        val result = resolveKonanHome(query(dataDirEnv = "env-dir"))

        assertEquals(File("env-dir").resolve(dirName), result)
    }

    @Test
    fun `GIVEN no overrides WHEN resolving THEN the distribution is under dot konan in user home`() {
        val result = resolveKonanHome(query())

        assertEquals(File(home).resolve(".konan").resolve(dirName), result)
    }

    @Test
    fun `GIVEN a version WHEN resolving THEN the directory name carries it`() {
        val result = resolveKonanHome(query(version = "2.2.20"))

        assertEquals("kotlin-native-prebuilt-linux-x86_64-2.2.20", result.name)
    }

    @Test
    fun `GIVEN every supported host WHEN naming the suffix THEN it matches the distribution archives`() {
        val suffixes =
            listOf(
                    "Linux" to "amd64",
                    "Linux" to "aarch64",
                    "Mac OS X" to "aarch64",
                    "Mac OS X" to "x86_64",
                    "Windows 11" to "amd64",
                )
                .map { (os, arch) -> hostDistributionSuffix(os, arch) }

        assertEquals(
            listOf(
                "linux-x86_64",
                "linux-aarch64",
                "macos-aarch64",
                "macos-x86_64",
                "windows-x86_64",
            ),
            suffixes,
        )
    }

    @Test
    fun `GIVEN a distribution with the embeddable compiler WHEN checking it THEN there is no problem`(
        @TempDir root: File
    ) {
        distribution(root, "kotlin-native-compiler-embeddable.jar")

        assertNull(nativeDistributionProblem(root))
    }

    @Test
    fun `GIVEN a missing directory WHEN checking it THEN the message names the path and both settings`() {
        val missing = File("does-not-exist").resolve("k2n")

        val message = assertNotNull(nativeDistributionProblem(missing))

        assertTrue(missing.path in message, message)
        assertTrue("kotlin.native.home" in message, message)
        assertTrue("konan.data.dir" in message, message)
    }

    @Test
    fun `GIVEN only kotlin-native jar WHEN checking it THEN it is rejected`(@TempDir root: File) {
        distribution(root, "kotlin-native.jar")

        assertNotNull(nativeDistributionProblem(root))
    }

    @Test
    fun `GIVEN no konan targets WHEN planning a task THEN there is no problem even without a home`() {
        assertNull(nativeTaskProblem(emptyList(), konanHome = null))
    }

    @Test
    fun `GIVEN konan targets without a home WHEN planning a task THEN the message names konanHome`() {
        val message = assertNotNull(nativeTaskProblem(listOf("linux_x64"), konanHome = null))

        assertTrue("konanHome" in message, message)
    }

    @Test
    fun `GIVEN konan targets and a missing distribution WHEN planning a task THEN it is reported`() {
        val missing = File("does-not-exist").resolve("k2n")

        val message = assertNotNull(nativeTaskProblem(listOf("linux_x64"), missing))

        assertEquals(nativeDistributionProblem(missing), message)
    }
}
