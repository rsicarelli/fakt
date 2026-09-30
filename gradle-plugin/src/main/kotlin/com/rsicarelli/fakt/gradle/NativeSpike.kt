// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation

/**
 * #152 spike (throwaway, never merged). Opt-in with `-Pfakt.spike.native=true`: Native leaf mains
 * become `NATIVE` consumers and shared-native metadata compilations (`nativeMain`, `appleMain`,
 * `iosMain`, …) become `NATIVE` producers that own their source set, instead of `LEGACY_HYBRID` /
 * `SUPPRESS`.
 */
internal object NativeSpike {
    const val PROPERTY: String = "fakt.spike.native"
    private const val HEAP_PROPERTY: String = "fakt.spike.workerHeap"
    private const val PROVISIONING_TASK: String = "downloadKotlinNativeDistribution"

    fun isEnabled(project: Project): Boolean =
        project.providers.gradleProperty(PROPERTY).map { it.toBoolean() }.getOrElse(false)

    fun isSharedNative(compilation: KotlinCompilation<*>): Boolean =
        compilation is KotlinSharedNativeCompilation

    /**
     * C4: locate the distribution without KGP internals, from public inputs only, in KGP's own
     * precedence: `kotlin.native.home`, then `konanDataDir` / `KONAN_DATA_DIR` / `~/.konan` +
     * `kotlin-native-prebuilt-<host>-<version>`. The version is `kotlin.native.version` or the
     * applied KGP's version.
     */
    fun kotlinNativeVersion(project: Project): Provider<String> =
        project.providers
            .gradleProperty("kotlin.native.version")
            .orElse(project.provider { project.getKotlinPluginVersion() })

    fun konanHome(project: Project): Provider<Directory> {
        val providers = project.providers
        val explicit = providers.gradleProperty("kotlin.native.home")
        val dataDir =
            providers
                .gradleProperty("konanDataDir")
                .orElse(providers.environmentVariable("KONAN_DATA_DIR"))
                .orElse(providers.systemProperty("user.home").map { "$it/.konan" })
        val derived =
            dataDir.zip(kotlinNativeVersion(project)) { dir, version ->
                "$dir/kotlin-native-prebuilt-${hostSuffix()}-$version"
            }
        return project.layout.projectDirectory.dir(explicit.orElse(derived).map { File(it).path })
    }

    fun configure(
        project: Project,
        compilation: KotlinCompilation<*>,
        taskProvider: TaskProvider<FaktGenerateTask>,
    ) {
        val shared = isSharedNative(compilation)
        val target =
            when (compilation) {
                is KotlinNativeCompilation -> compilation.konanTarget.name
                is KotlinSharedNativeCompilation ->
                    representativeTarget(compilation.konanTargets.map { it.name })
                else -> error("Not a native compilation: ${compilation.name}")
            }
        val heap = project.providers.gradleProperty(HEAP_PROPERTY)
        taskProvider.configure { task ->
            task.konanHome.set(konanHome(project))
            task.kotlinNativeVersion.set(kotlinNativeVersion(project))
            task.konanTarget.set(target)
            task.sharedNative.set(shared)
            task.workerMaxHeap.set(heap)
            if (shared) task.refinesKlibs.from(refinedMetadataKlibs(compilation))
            // Minimal #165 slice: without the opt-ins every cinterop signature fails analysis.
            task.optIns.set(
                project.provider {
                    compilation.allKotlinSourceSets
                        .flatMap { it.languageSettings.optInAnnotationsInUse }
                        .distinct()
                        .sorted()
                }
            )
            // C4: KGP provisions the distribution in this task (and in a configuration-time
            // ValueSource). Depend on it by name so a clean `~/.konan` is populated first.
            task.dependsOn(project.tasks.matching { it.name == PROVISIONING_TASK })
        }
    }

    /**
     * The metadata compilation outputs of every source set [compilation]'s default source set
     * depends on (transitively), mirroring KGP's `-Xrefines-paths`. `classesDirs` carries the
     * producing task, so Gradle orders the Fakt task after `compile<Ancestor>KotlinMetadata`.
     */
    private fun refinedMetadataKlibs(compilation: KotlinCompilation<*>): List<Any> {
        val metadataCompilations = compilation.target.compilations
        return (compilation.allKotlinSourceSets - compilation.defaultSourceSet).mapNotNull {
            metadataCompilations.findByName(it.name)?.output?.classesDirs
        }
    }

    /** KGP compiles `nativeMain` for the host target when it can, else the first target. */
    internal fun representativeTarget(targets: List<String>): String =
        targets.firstOrNull { it == hostTarget() } ?: targets.first()

    private fun isMac() = System.getProperty("os.name").lowercase().contains("mac")

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("windows")

    private fun isArm() = System.getProperty("os.arch").let { it == "aarch64" || it == "arm64" }

    private fun hostSuffix(): String =
        when {
            isMac() -> if (isArm()) "macos-aarch64" else "macos-x86_64"
            isWindows() -> "windows-x86_64"
            else -> if (isArm()) "linux-aarch64" else "linux-x86_64"
        }

    private fun hostTarget(): String =
        when {
            isMac() -> if (isArm()) "macos_arm64" else "macos_x64"
            isWindows() -> "mingw_x64"
            else -> if (isArm()) "linux_arm64" else "linux_x64"
        }
}
