// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins the registry query that decides which `FaktGenerateTask` owns a test source set: an explicit
 * claim first, otherwise the task of the main compilation the test compilation is associated with.
 * The association is read on every call, so one made after the claim still counts.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestDirOwnersTest {

    private val byMain =
        mapOf(
            "jvm/main" to "faktGenerateJvmMain",
            "android/main" to "faktGenerateAndroidMain",
            "linuxX64/main" to "faktGenerateLinuxX64Main",
        )

    private val tests =
        listOf(
            TestCompilationNode("jvmTest", listOf("jvm/main")),
            TestCompilationNode("jvmIntegrationTest", listOf("jvm/main")),
            TestCompilationNode("androidHostTest", listOf("android/main")),
            TestCompilationNode("linuxX64Test", emptyList()),
            TestCompilationNode("commonTest", emptyList()),
        )

    // ---- B7: pure ---------------------------------------------------------------------------

    @Test
    fun `GIVEN jvmTest associated with jvm main WHEN resolving the owner THEN it is faktGenerateJvmMain`() {
        assertEquals("faktGenerateJvmMain", ownerByAssociation("jvmTest", tests, byMain))
    }

    @Test
    fun `GIVEN a custom jvmIntegrationTest associated with jvm main WHEN resolving the owner THEN it is faktGenerateJvmMain`() {
        assertEquals("faktGenerateJvmMain", ownerByAssociation("jvmIntegrationTest", tests, byMain))
    }

    @Test
    fun `GIVEN androidHostTest associated with android main WHEN resolving the owner THEN it is faktGenerateAndroidMain`() {
        assertEquals(
            "faktGenerateAndroidMain",
            ownerByAssociation("androidHostTest", tests, byMain),
        )
    }

    @Test
    fun `GIVEN linuxX64Test with no association WHEN resolving the owner THEN there is none`() {
        assertNull(ownerByAssociation("linuxX64Test", tests, byMain))
    }

    @Test
    fun `GIVEN commonTest and no explicit claim WHEN resolving the owner THEN there is none`() {
        assertNull(ownerByAssociation("commonTest", tests, byMain))
    }

    @Test
    fun `GIVEN a test compilation associated with an unclaimed main WHEN resolving the owner THEN there is none`() {
        val unclaimed = listOf(TestCompilationNode("iosTest", listOf("ios/main")))

        assertNull(ownerByAssociation("iosTest", unclaimed, byMain))
    }

    @Test
    fun `GIVEN a key built from target and compilation WHEN reading it THEN it joins them with a slash`() {
        assertEquals("jvm/main", compilationKey("jvm", "main"))
    }

    // ---- ProjectBuilder ---------------------------------------------------------------------

    private fun jvmProject(): Project =
        createKmpProject().also {
            it.getKotlinExtension().apply {
                jvm().compilations.create("integrationTest")
                linuxX64()
            }
            it.evaluate()
        }

    @Test
    fun `GIVEN an explicit claim for a test set WHEN querying the owner THEN the explicit claim wins`() {
        val project = jvmProject()
        testDirOwners(project).bySourceSet["jvmTest"] = "explicitTask"
        claimAssociatedTests(project, compilationKey("jvm", "main"), "faktGenerateJvmMain")

        assertEquals("explicitTask", testDirOwnerOf(project, "jvmTest"))
    }

    @Test
    fun `GIVEN a claim made before the association WHEN the test compilation is associated afterwards THEN the owner is seen`() {
        val project = jvmProject()
        val jvm = project.getKotlinExtension().targets.getByName("jvm")
        val main = jvm.compilations.getByName("main")
        claimAssociatedTests(project, compilationKey("jvm", "main"), "faktGenerateJvmMain")
        val integration = jvm.compilations.getByName("integrationTest")
        assertNull(testDirOwnerOf(project, "jvmIntegrationTest"), "not associated yet")

        integration.associateWith(main)

        assertEquals("faktGenerateJvmMain", testDirOwnerOf(project, "jvmIntegrationTest"))
        assertEquals("faktGenerateJvmMain", testDirOwnerOf(project, "jvmTest"))
        assertNull(testDirOwnerOf(project, "linuxX64Test"))
        assertEquals("faktGenerateMetadataCommonMain", testDirOwnerOf(project, "commonTest"))
    }

    @Test
    fun `GIVEN a claim WHEN reading the registry THEN the task is recorded as a Fakt owner`() {
        val project = jvmProject()

        claimAssociatedTests(project, compilationKey("jvm", "main"), "faktGenerateJvmMain")

        assertEquals(true, isFaktTestDirOwner(project, "faktGenerateJvmMain"))
        assertEquals(
            mapOf("jvm/main" to "faktGenerateJvmMain"),
            testDirOwners(project).byMainCompilation,
        )
    }

    @Test
    fun `GIVEN a project without the registry WHEN querying the owner THEN the property stays absent`() {
        val project = ProjectBuilder.builder().build()
        assertEquals(false, project.extensions.extraProperties.has(TEST_DIR_OWNERS_PROPERTY))

        val owner = testDirOwnerOf(project, "jvmTest")

        assertNull(owner)
        assertEquals(false, project.extensions.extraProperties.has(TEST_DIR_OWNERS_PROPERTY))
    }
}
