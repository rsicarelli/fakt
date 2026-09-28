// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.android

import com.rsicarelli.fakt.gradle.FaktGenerateTask
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Pins [AndroidVariantSources], which feeds AGP 9 built-in Kotlin variants' source directories to
 * their producer `FaktGenerateTask`s (issue #154).
 *
 * AGP's `onVariants` callback and KGP's compilations (which register the producers) arrive in no
 * guaranteed order, so the [AndroidVariantSources.Registry] must pair a variant with its task
 * either way round. The real callback needs AGP itself; it is locked end-to-end by the
 * `compat-agp/agp-9.0` cell, which generates every fake from `faktGenerate*` on built-in Kotlin.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AndroidVariantSourcesTest {

    @Test
    fun `GIVEN the variant reported before its producer WHEN the producer registers THEN it reads the variant's sources`(
        @TempDir dir: File
    ) {
        // Given
        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        val registry = AndroidVariantSources.Registry()
        val task =
            project.tasks.register("faktGenerateAndroidjvmDebug", FaktGenerateTask::class.java)
        registry.onVariant("debug", project.sourceDirs("src/main/kotlin", "src/main/java"))

        // When
        registry.onTask("debug", task)

        // Then
        assertEquals(
            project.files("src/main/kotlin", "src/main/java").files,
            task.get().sources.files,
        )
    }

    @Test
    fun `GIVEN the producer registered before its variant WHEN the variant is reported THEN the producer reads its sources`(
        @TempDir dir: File
    ) {
        // Given
        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        val registry = AndroidVariantSources.Registry()
        val task =
            project.tasks.register("faktGenerateAndroidjvmDebug", FaktGenerateTask::class.java)
        registry.onTask("debug", task)

        // When
        registry.onVariant("debug", project.sourceDirs("src/main/kotlin"))

        // Then
        assertEquals(project.files("src/main/kotlin").files, task.get().sources.files)
    }

    @Test
    fun `GIVEN two variants WHEN each producer registers THEN it reads only its own variant's sources`(
        @TempDir dir: File
    ) {
        // Given
        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        val registry = AndroidVariantSources.Registry()
        val debug =
            project.tasks.register("faktGenerateAndroidjvmDebug", FaktGenerateTask::class.java)
        val release =
            project.tasks.register("faktGenerateAndroidjvmRelease", FaktGenerateTask::class.java)
        registry.onVariant("debug", project.sourceDirs("src/main/kotlin", "src/debug/kotlin"))
        registry.onVariant("release", project.sourceDirs("src/main/kotlin", "src/release/kotlin"))

        // When
        registry.onTask("debug", debug)
        registry.onTask("release", release)

        // Then
        assertTrue(project.file("src/debug/kotlin") in debug.get().sources.files)
        assertFalse(project.file("src/release/kotlin") in debug.get().sources.files)
        assertTrue(project.file("src/release/kotlin") in release.get().sources.files)
    }

    @Test
    fun `GIVEN a project without an Android plugin WHEN installing THEN it is neither built-in Kotlin nor hooked`() {
        // Given
        val project = ProjectBuilder.builder().build()

        // When
        AndroidVariantSources.install(project)

        // Then
        assertFalse(AndroidVariantSources.usesBuiltInKotlin(project))
        assertFalse(AndroidVariantSources.isAvailable(project))
    }

    private fun Project.sourceDirs(
        vararg paths: String
    ): List<Provider<out Collection<Directory>>> =
        listOf(provider { paths.map { layout.projectDirectory.dir(it) } })
}
