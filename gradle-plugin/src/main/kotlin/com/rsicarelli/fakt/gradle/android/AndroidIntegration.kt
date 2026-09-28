// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.android

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.AndroidComponents
import com.rsicarelli.fakt.gradle.FaktGenerateTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

/**
 * The single place where Fakt touches Android Gradle Plugin (AGP) types.
 *
 * AGP is a `compileOnly` dependency of Fakt's Gradle plugin, compiled against the **oldest**
 * supported version (`agp-api-floor` in the version catalog, matching the `compat-agp/agp-8.11`
 * sample) so that only API every supported AGP has can be called. At runtime the user's own AGP
 * provides these classes — or doesn't: a project without an Android plugin, or one whose AGP lives
 * in a classloader Fakt can't see, must still configure. Every entry point therefore catches
 * [LinkageError] and degrades, and callers outside this package never reference AGP types, so no
 * AGP class is loaded unless an Android compilation is actually being wired.
 */
internal object AndroidIntegration {

    /**
     * De-duplicates [addBootClasspath]'s warning: the lookup is per task, the answer per module.
     */
    private const val BOOT_CLASSPATH_WARNED: String = "fakt.androidBootClasspathWarned"

    /**
     * Adds [project]'s Android SDK boot classpath (`android.jar`) to an Android compilation's
     * generation [task] (issue #158).
     *
     * KGP's `KotlinCompilation.compileDependencyFiles` does not contain the boot classpath for
     * Android compilations — the real `compileKotlin*` task receives it separately — so without
     * this the worker cannot resolve `android.*` types (`Context`, `Uri`, …) in `@Fake` signatures
     * and fails with "unresolved reference 'android'". When it cannot be read, generation still
     * runs and one warning per project says why.
     */
    fun addBootClasspath(task: FaktGenerateTask, project: Project) {
        when (val lookup = bootClasspath(project)) {
            is BootClasspathLookup.Found -> task.compileClasspath.from(lookup.files)
            BootClasspathLookup.NoAndroidComponents,
            BootClasspathLookup.AgpNotVisible -> warnOnce(project, lookup)
        }
    }

    /**
     * Reads the boot classpath from AGP's `AndroidComponents.sdkComponents` — the base of every
     * Android plugin's `androidComponents` extension (`com.android.library`,
     * `com.android.application`, `com.android.kotlin.multiplatform.library`, …). The provider is
     * returned unresolved; the SDK is only located when the task's inputs are fingerprinted.
     *
     * [findComponents] exists for tests: the default looks the extension up by type.
     */
    fun bootClasspath(
        project: Project,
        findComponents: (Project) -> AndroidComponents? = ::findAndroidComponents,
    ): BootClasspathLookup =
        try {
            findComponents(project)?.let {
                BootClasspathLookup.Found(it.sdkComponents.bootClasspath)
            } ?: BootClasspathLookup.NoAndroidComponents
        } catch (_: LinkageError) {
            BootClasspathLookup.AgpNotVisible
        }

    /**
     * Whether [target] is the KMP Android library target
     * (`com.android.kotlin.multiplatform.library`).
     *
     * Its platform type is normally `androidJvm`, but AGP's experimental
     * `android.kmp.use.jvm.platform.type=true` reports it as `jvm`, so the platform type alone
     * would silently skip the boot classpath. `false` when AGP's API is not visible.
     */
    fun isKmpAndroidTarget(target: KotlinTarget): Boolean =
        try {
            target is KotlinMultiplatformAndroidLibraryTarget
        } catch (_: LinkageError) {
            false
        }

    /**
     * Whether a compilation of [platformTypeName] (KGP's `KotlinPlatformType.name`, lowercased)
     * needs the SDK boot classpath: every `androidjvm` compilation, plus a `jvm` one whose target
     * is the KMP Android library target (see [isKmpAndroidTarget]). Lazy, so a plain JVM target in
     * a non-Android build never touches AGP classes.
     */
    fun needsBootClasspath(platformTypeName: String, isKmpAndroidTarget: () -> Boolean): Boolean =
        platformTypeName == "androidjvm" || (platformTypeName == "jvm" && isKmpAndroidTarget())

    /** The actionable warning for a boot classpath that could not be read. */
    fun bootClasspathWarning(projectPath: String, lookup: BootClasspathLookup): String {
        val cause =
            when (lookup) {
                BootClasspathLookup.AgpNotVisible ->
                    "the Android Gradle Plugin API is not visible to Fakt's classloader. Put AGP " +
                        "on the same build classpath as Fakt: declare both in the root build's " +
                        "`plugins { … apply false }` block, or add both to your build-logic " +
                        "(buildSrc) dependencies"
                else ->
                    "the project has no `androidComponents` extension. Apply an Android Gradle " +
                        "Plugin (com.android.library, com.android.application, " +
                        "com.android.kotlin.multiplatform.library, …) to this project"
            }
        return "Fakt: '$projectPath' has an Android compilation, but the Android SDK boot " +
            "classpath (android.jar) could not be read, so Android framework types in @Fake " +
            "signatures will not resolve: $cause."
    }

    private fun warnOnce(project: Project, lookup: BootClasspathLookup) {
        val extras = project.extensions.extraProperties
        if (!extras.has(BOOT_CLASSPATH_WARNED)) {
            extras.set(BOOT_CLASSPATH_WARNED, true)
            project.logger.warn(bootClasspathWarning(project.path, lookup))
        }
    }

    private fun findAndroidComponents(project: Project): AndroidComponents? =
        project.extensions.findByType(AndroidComponents::class.java)
}

/** Outcome of [AndroidIntegration.bootClasspath]. */
internal sealed interface BootClasspathLookup {
    /** The SDK boot classpath, still unresolved. */
    data class Found(val files: Provider<List<RegularFile>>) : BootClasspathLookup

    /** AGP's API loaded, but the project has no `androidComponents` extension. */
    data object NoAndroidComponents : BootClasspathLookup

    /** AGP's API is not on Fakt's classloader. */
    data object AgpNotVisible : BootClasspathLookup
}
