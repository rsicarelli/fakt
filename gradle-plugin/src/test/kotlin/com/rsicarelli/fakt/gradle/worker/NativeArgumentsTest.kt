// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pure unit coverage for the Kotlin/Native `K2Native` argument plan: [nativeArguments] (leaf vs
 * shared shape), [representativeKonanTarget], [hostKonanTarget] and the reflective
 * [applyNativeArguments]. Runs without a Kotlin/Native distribution.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NativeArgumentsTest {

    private val konanHome = File("/konan/home")
    private val scratch = File("/scratch")

    private fun input(
        shared: Boolean,
        konanTargets: List<String>,
        dependencies: List<File> = emptyList(),
        ownSources: List<File> = emptyList(),
        refinesPaths: List<File> = emptyList(),
        hostTarget: String = "linux_x64",
    ) =
        NativeArgumentsInput(
            shared = shared,
            konanTargets = konanTargets,
            hostTarget = hostTarget,
            konanHome = konanHome,
            dependencies = dependencies,
            ownSources = ownSources,
            refinesPaths = refinesPaths,
            scratchDir = scratch,
        )

    // region leaf plan

    @Test
    fun `GIVEN a leaf native compilation WHEN planning THEN produces a metadata klib for its target`() {
        val plan = nativeArguments(input(shared = false, konanTargets = listOf("linux_x64")))

        assertEquals("library", plan.produce)
        assertTrue(plan.metadataKlib)
        assertEquals("linux_x64", plan.target)
        assertEquals(File(scratch, "native-klib/fakt-analysis").absolutePath, plan.outputName)
    }

    @Test
    fun `GIVEN a leaf native compilation WHEN planning THEN keeps dependencies in order and default libs on`() {
        val deps = listOf(File("/deps/b.klib"), File("/deps/a.klib"))

        val plan =
            nativeArguments(
                input(shared = false, konanTargets = listOf("linux_x64"), dependencies = deps)
            )

        assertEquals(deps.map { it.absolutePath }, plan.libraries)
        assertFalse(plan.noDefaultLibs)
        assertFalse(plan.noStdlib)
    }

    @Test
    fun `GIVEN a leaf native compilation WHEN planning THEN leaves the common fragment to the shared builder`() {
        val plan =
            nativeArguments(
                input(
                    shared = false,
                    konanTargets = listOf("linux_x64"),
                    ownSources = listOf(File("/p/A.kt")),
                    refinesPaths = listOf(File("/r/common.klib")),
                )
            )

        assertTrue(plan.refinesPaths.isEmpty())
        assertNull(plan.commonSources)
        assertFalse(plan.multiPlatform)
    }

    // endregion

    // region shared plan

    @Test
    fun `GIVEN a shared native compilation WHEN planning THEN disables default libs and stdlib`() {
        val plan =
            nativeArguments(input(shared = true, konanTargets = listOf("linux_arm64", "linux_x64")))

        assertTrue(plan.noDefaultLibs)
        assertTrue(plan.noStdlib)
        assertTrue(plan.multiPlatform)
    }

    @Test
    fun `GIVEN a shared native compilation WHEN planning THEN puts the distribution stdlib before the dependencies`() {
        val deps = listOf(File("/deps/commonized.klib"), File("/deps/meta.klib"))

        val plan =
            nativeArguments(
                input(shared = true, konanTargets = listOf("linux_x64"), dependencies = deps)
            )

        val stdlib = File(konanHome, "klib/common/stdlib").absolutePath
        assertEquals(listOf(stdlib) + deps.map { it.absolutePath }, plan.libraries)
    }

    @Test
    fun `GIVEN dependencies already holding the stdlib under another spelling WHEN planning THEN lists it once`() {
        val stdlib = File(konanHome, "klib/common/stdlib")
        val respelled = File(konanHome, "klib/platform/../common/stdlib/")
        val other = File("/deps/meta.klib")

        val plan =
            nativeArguments(
                input(
                    shared = true,
                    konanTargets = listOf("linux_x64"),
                    dependencies = listOf(respelled, other),
                )
            )

        assertEquals(listOf(stdlib.absolutePath, other.absolutePath), plan.libraries)
    }

    @Test
    fun `GIVEN a shared native compilation WHEN planning THEN analyses own sources as common and refines ancestors`() {
        val own = listOf(File("/p/nativeMain/A.kt"), File("/p/nativeMain/B.kt"))
        val refines = listOf(File("/r/common.klib"))

        val plan =
            nativeArguments(
                input(
                    shared = true,
                    konanTargets = listOf("linux_x64"),
                    ownSources = own,
                    refinesPaths = refines,
                )
            )

        assertEquals(own.map { it.absolutePath }, plan.commonSources)
        assertEquals(refines.map { it.absolutePath }, plan.refinesPaths)
    }

    @Test
    fun `GIVEN a shared native compilation WHEN planning THEN targets the representative target`() {
        val plan =
            nativeArguments(
                input(
                    shared = true,
                    konanTargets = listOf("ios_arm64", "macos_arm64"),
                    hostTarget = "linux_x64",
                )
            )

        assertEquals("ios_arm64", plan.target)
    }

    // endregion

    // region representative and host targets

    @Test
    fun `GIVEN the host among the targets WHEN choosing the representative THEN picks the host`() {
        val targets = listOf("ios_x64", "ios_arm64", "macos_arm64")

        assertEquals("macos_arm64", representativeKonanTarget(targets, host = "macos_arm64"))
    }

    @Test
    fun `GIVEN the host outside the targets WHEN choosing the representative THEN picks the first sorted target for any order`() {
        val targets = listOf("ios_x64", "ios_arm64", "macos_arm64")

        assertEquals("ios_arm64", representativeKonanTarget(targets, host = "linux_x64"))
        assertEquals("ios_arm64", representativeKonanTarget(targets.reversed(), host = "linux_x64"))
    }

    @Test
    fun `GIVEN no targets WHEN choosing the representative THEN fails with a clear message`() {
        val failure =
            assertFailsWith<IllegalArgumentException> {
                representativeKonanTarget(emptyList(), host = "linux_x64")
            }

        assertTrue(failure.message.orEmpty().contains("konan target"))
    }

    @Test
    fun `GIVEN supported host platforms WHEN mapping to a konan target THEN returns the matching name`() {
        assertEquals("linux_x64", hostKonanTarget("Linux", "amd64"))
        assertEquals("linux_arm64", hostKonanTarget("Linux", "aarch64"))
        assertEquals("macos_arm64", hostKonanTarget("Mac OS X", "aarch64"))
        assertEquals("macos_x64", hostKonanTarget("Mac OS X", "x86_64"))
        assertEquals("mingw_x64", hostKonanTarget("Windows 11", "amd64"))
    }

    @Test
    fun `GIVEN an unsupported host WHEN mapping to a konan target THEN fails naming the host`() {
        val failure = assertFailsWith<IllegalStateException> { hostKonanTarget("Plan 9", "mips") }

        assertTrue(failure.message.orEmpty().contains("Plan 9"))
    }

    // endregion

    // region applyNativeArguments

    @Test
    fun `GIVEN a shared plan WHEN applying THEN sets every K2Native argument reflectively`() {
        val args = RecordingNativeArguments()
        val plan =
            nativeArguments(
                input(
                    shared = true,
                    konanTargets = listOf("linux_x64"),
                    ownSources = listOf(File("/p/A.kt")),
                    refinesPaths = listOf(File("/r/common.klib")),
                )
            )

        applyNativeArguments(K2CompilerBridge(javaClass.classLoader), args, plan)

        assertEquals("library", args.produce)
        assertTrue(args.metadataKlib)
        assertEquals("linux_x64", args.target)
        assertEquals(plan.outputName, args.outputName)
        assertEquals(plan.libraries, args.libraries?.toList())
        assertTrue(args.nodefaultlibs)
        assertTrue(args.nostdlib)
        assertEquals(plan.commonSources, args.commonSources?.toList())
        assertEquals(plan.refinesPaths, args.refinesPaths?.toList())
        assertTrue(args.multiPlatform)
    }

    @Test
    fun `GIVEN a leaf plan WHEN applying THEN leaves the common fragment arguments untouched`() {
        val args = RecordingNativeArguments()
        val plan = nativeArguments(input(shared = false, konanTargets = listOf("linux_x64")))

        applyNativeArguments(K2CompilerBridge(javaClass.classLoader), args, plan)

        assertNull(args.commonSources)
        assertNull(args.refinesPaths)
        assertFalse(args.multiPlatform)
        assertFalse(args.nodefaultlibs)
        assertFalse(args.nostdlib)
    }

    // endregion
}

/**
 * Hand-written stand-in for `K2NativeCompilerArguments` (which lives only in the Kotlin/Native
 * embeddable jar): the same setter names and signatures, recording what was set.
 */
private class RecordingNativeArguments {
    var produce: String? = null
    var metadataKlib = false
    var target: String? = null
    var outputName: String? = null
    var libraries: Array<String>? = null
    var nodefaultlibs = false
    var nostdlib = false
    var commonSources: Array<String>? = null
    var refinesPaths: Array<String>? = null
    var multiPlatform = false
}
