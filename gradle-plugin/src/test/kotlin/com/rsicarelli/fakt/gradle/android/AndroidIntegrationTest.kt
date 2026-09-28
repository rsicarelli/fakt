// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.android

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.dsl.SdkComponents
import com.android.build.api.instrumentation.manageddevice.ManagedDeviceRegistry
import com.android.build.api.variant.Aapt2
import com.android.build.api.variant.Aidl
import com.android.build.api.variant.AndroidComponents
import com.rsicarelli.fakt.gradle.FaktGenerateTask
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Pins [AndroidIntegration], Fakt's only use of AGP types (issue #158).
 *
 * AGP's API (`gradle-api`, the same floor version Fakt compiles against) is on the test classpath,
 * but no AGP implementation is: the `androidComponents` extension is a hand-written fake registered
 * on a `ProjectBuilder` project. The "AGP not visible to Fakt's classloader" case is reproduced by
 * a lookup that throws the [NoClassDefFoundError] the JVM would. The end-to-end path (real AGP,
 * `android.*` types in `@Fake` signatures) is locked by the Android samples: the `compat-agp` cells
 * from the 8.11 floor to the newest AGP, `android-single-module` and `kmp-android-lint`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AndroidIntegrationTest {

    @Test
    fun `GIVEN an androidComponents extension WHEN reading the boot classpath THEN returns the SDK's jars`(
        @TempDir dir: File
    ) {
        // Given
        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        val androidJar =
            project.layout.projectDirectory.file("sdk/platforms/android-35/android.jar")
        project.registerAndroidComponents(bootClasspath = listOf(androidJar))

        // When
        val lookup = AndroidIntegration.bootClasspath(project)

        // Then
        val found = assertIs<BootClasspathLookup.Found>(lookup)
        assertEquals(listOf(androidJar), found.files.get())
    }

    @Test
    fun `GIVEN an androidComponents extension WHEN adding the boot classpath THEN the task analyses against android jar`(
        @TempDir dir: File
    ) {
        // Given
        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        val androidJar =
            project.layout.projectDirectory.file("sdk/platforms/android-35/android.jar")
        project.registerAndroidComponents(bootClasspath = listOf(androidJar))
        val task = project.tasks.register("faktGenerate", FaktGenerateTask::class.java).get()

        // When
        AndroidIntegration.addBootClasspath(task, project)

        // Then
        assertEquals(setOf(androidJar.asFile), task.compileClasspath.files)
    }

    @Test
    fun `GIVEN a project without an androidComponents extension WHEN reading the boot classpath THEN reports it missing`() {
        // Given
        val project = ProjectBuilder.builder().build()

        // When
        val lookup = AndroidIntegration.bootClasspath(project)

        // Then
        assertEquals(BootClasspathLookup.NoAndroidComponents, lookup)
    }

    @Test
    fun `GIVEN AGP classes not loadable from Fakt's classloader WHEN reading the boot classpath THEN reports AGP not visible instead of failing`() {
        // Given
        val project = ProjectBuilder.builder().build()

        // When
        val lookup =
            AndroidIntegration.bootClasspath(project) {
                throw NoClassDefFoundError("com/android/build/api/variant/AndroidComponents")
            }

        // Then
        assertEquals(BootClasspathLookup.AgpNotVisible, lookup)
    }

    @Test
    fun `GIVEN AGP not visible WHEN building the warning THEN it tells the user to share Fakt's classpath`() {
        // When
        val warning =
            AndroidIntegration.bootClasspathWarning(":app", BootClasspathLookup.AgpNotVisible)

        // Then
        assertTrue("':app'" in warning, warning)
        assertTrue("android.jar" in warning, warning)
        assertTrue("same build classpath as Fakt" in warning, warning)
    }

    @Test
    fun `GIVEN no androidComponents extension WHEN building the warning THEN it tells the user to apply an Android plugin`() {
        // When
        val warning =
            AndroidIntegration.bootClasspathWarning(":app", BootClasspathLookup.NoAndroidComponents)

        // Then
        assertTrue("Apply an Android Gradle Plugin" in warning, warning)
        assertFalse("same build classpath as Fakt" in warning, warning)
    }

    @Test
    fun `GIVEN an androidJvm compilation WHEN deciding THEN needs the boot classpath without inspecting the target`() {
        // When
        val needed =
            AndroidIntegration.needsBootClasspath("androidjvm") {
                error("An androidJvm compilation must not need the target check")
            }

        // Then
        assertTrue(needed)
    }

    @Test
    fun `GIVEN a jvm compilation of the KMP Android library target WHEN deciding THEN needs the boot classpath`() {
        // When
        val needed = AndroidIntegration.needsBootClasspath("jvm") { true }

        // Then
        assertTrue(
            needed,
            "android.kmp.use.jvm.platform.type=true reports the KMP Android target as jvm",
        )
    }

    @Test
    fun `GIVEN a plain jvm compilation WHEN deciding THEN does not need the boot classpath`() {
        // When
        val needed = AndroidIntegration.needsBootClasspath("jvm") { false }

        // Then
        assertFalse(needed)
    }

    @Test
    fun `GIVEN non-JVM compilations WHEN deciding THEN never need the boot classpath nor inspect the target`() {
        listOf("common", "js", "wasm", "native").forEach { platform ->
            // When
            val needed =
                AndroidIntegration.needsBootClasspath(platform) {
                    error("$platform must not trigger the AGP target check")
                }

            // Then
            assertFalse(needed, platform)
        }
    }

    @Test
    fun `GIVEN a Kotlin JVM target WHEN checking for the KMP Android library target THEN returns false`() {
        // Given
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        val target = project.extensions.getByType(KotlinJvmProjectExtension::class.java).target

        // When
        val isKmpAndroid = AndroidIntegration.isKmpAndroidTarget(target)

        // Then
        assertFalse(isKmpAndroid)
    }

    private fun Project.registerAndroidComponents(bootClasspath: List<RegularFile>) {
        val sdk = FakeSdkComponents(bootClasspath = provider { bootClasspath })
        extensions.add(
            AndroidComponents::class.java,
            "androidComponents",
            FakeAndroidComponents(sdk),
        )
    }

    /** `androidComponents` as every Android plugin exposes it; only the SDK is read by Fakt. */
    private class FakeAndroidComponents(override val sdkComponents: SdkComponents) :
        AndroidComponents {
        override val pluginVersion: AndroidPluginVersion = AndroidPluginVersion(8, 11, 1)
        override val managedDeviceRegistry: ManagedDeviceRegistry
            get() = error("Fakt does not read the managed device registry")
    }

    private class FakeSdkComponents(override val bootClasspath: Provider<List<RegularFile>>) :
        SdkComponents {
        override val sdkDirectory: Provider<Directory>
            get() = error("Fakt only reads the boot classpath")

        override val ndkDirectory: Provider<Directory>
            get() = error("Fakt only reads the boot classpath")

        override val adb: Provider<RegularFile>
            get() = error("Fakt only reads the boot classpath")

        override val aidl: Provider<Aidl>
            get() = error("Fakt only reads the boot classpath")

        override val aapt2: Provider<Aapt2>
            get() = error("Fakt only reads the boot classpath")
    }
}
