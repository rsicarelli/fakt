// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.android

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.rsicarelli.fakt.gradle.FaktGenerateTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * Sources for Android modules compiled by AGP 9's built-in Kotlin (issue #154).
 *
 * AGP 9 compiles Kotlin itself and rejects KGP's `org.jetbrains.kotlin.android`. The
 * `KotlinCompilation`s it still exposes (`debug`, `release`, …) carry `KotlinSourceSet`s whose
 * `kotlin.srcDirs` stay empty: the sources live in AGP's variant model. A producer
 * `FaktGenerateTask` reading only the Kotlin source sets would run NO-SOURCE and generate nothing.
 *
 * This reads each variant's `sources.kotlin` / `sources.java` static directories from AGP's variant
 * API (`androidComponents.onVariants`) and feeds them to that variant's producer task. Static only,
 * matching what KGP's Android source sets expose: generated source directories (KSP, BuildConfig,
 * …) are not analysed, as on the KGP path.
 *
 * `onVariants` must be registered before AGP finalises its variants, so [install] hooks the Android
 * plugin ids at apply time. KGP's compilations — and with them the producer tasks — may appear
 * before or after AGP runs the callback, so a per-project [Registry] pairs a variant with its task
 * in whichever order they arrive.
 */
internal object AndroidVariantSources {

    private const val REGISTRY: String = "fakt.androidVariantSources"
    private const val KOTLIN_ANDROID_PLUGIN_ID: String = "org.jetbrains.kotlin.android"
    private const val KOTLIN_MULTIPLATFORM_PLUGIN_ID: String = "org.jetbrains.kotlin.multiplatform"

    /**
     * Android plugins whose variants compile Kotlin through AGP's variant model. The KMP Android
     * library plugin (`com.android.kotlin.multiplatform.library`) is not one of them: it exposes
     * real Kotlin source sets.
     */
    private val ANDROID_PLUGIN_IDS: List<String> =
        listOf(
            "com.android.library",
            "com.android.application",
            "com.android.dynamic-feature",
            "com.android.test",
        )

    /**
     * Registers the `onVariants` callback as soon as an Android plugin is applied to [project].
     * Safe on every project: nothing happens without an Android plugin, and AGP classes are only
     * touched once one is applied.
     */
    fun install(project: Project) {
        ANDROID_PLUGIN_IDS.forEach { id ->
            project.pluginManager.withPlugin(id) { register(project) }
        }
    }

    /**
     * Whether [project] applies one of the variant-model Android plugins ([ANDROID_PLUGIN_IDS]).
     * Plugin ids only, so no AGP class is loaded.
     */
    fun hasAndroidPlugin(project: Project): Boolean =
        ANDROID_PLUGIN_IDS.any(project.plugins::hasPlugin)

    /** Whether KGP's `org.jetbrains.kotlin.android` compiles [project]'s Kotlin. */
    fun hasKotlinAndroidPlugin(project: Project): Boolean =
        project.plugins.hasPlugin(KOTLIN_ANDROID_PLUGIN_ID)

    /** Whether [project] applies the Kotlin Multiplatform plugin. */
    fun isMultiplatform(project: Project): Boolean =
        project.plugins.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID)

    /**
     * Whether [project] is an Android module on AGP's built-in Kotlin: a variant-model Android
     * plugin is applied, and neither KGP's `org.jetbrains.kotlin.android` nor Kotlin Multiplatform
     * compiles it. A multiplatform `androidTarget()` also lacks `org.jetbrains.kotlin.android`, but
     * its Kotlin is compiled by KGP, not by AGP. Plugin ids only, so no AGP class is loaded.
     */
    fun usesBuiltInKotlin(project: Project): Boolean =
        hasAndroidPlugin(project) && !hasKotlinAndroidPlugin(project) && !isMultiplatform(project)

    /**
     * Whether [install] could hook [project]'s variants: an Android plugin is applied, AGP's
     * variant API is visible to Fakt's classloader, and it still accepted callbacks. When it isn't,
     * a built-in Kotlin module stays on the in-process plugin, which reads sources from the compile
     * task itself.
     */
    fun isAvailable(project: Project): Boolean =
        (project.extensions.extraProperties.findPropertyOrNull(REGISTRY) as? Registry)?.hooked ==
            true

    /** Feeds variant [variantName]'s sources to its producer [task] once AGP reports them. */
    fun feed(project: Project, variantName: String, task: TaskProvider<FaktGenerateTask>) {
        (project.extensions.extraProperties.findPropertyOrNull(REGISTRY) as? Registry)?.onTask(
            variantName,
            task,
        )
    }

    private fun register(project: Project) {
        val extras = project.extensions.extraProperties
        if (extras.has(REGISTRY)) return
        val registry = Registry()
        extras.set(REGISTRY, registry)
        try {
            @Suppress("UNCHECKED_CAST")
            val components =
                project.extensions.findByType(AndroidComponentsExtension::class.java)
                    as AndroidComponentsExtension<*, *, Variant>? ?: return
            components.onVariants(components.selector().all()) { variant ->
                registry.onVariant(variant.name, staticSourceDirs(variant))
            }
            registry.hooked = true
        } catch (_: LinkageError) {
            // AGP's API is not visible to Fakt; isAvailable stays false and the module keeps the
            // in-process plugin.
        } catch (@Suppress("TooGenericExceptionCaught") tooLate: RuntimeException) {
            // AGP throws a plain RuntimeException ("too late to add actions") when Fakt is applied
            // after it ran its variant callbacks, e.g. from an `afterEvaluate`. Same fallback; the
            // routing warning names this cause.
            project.logger.info("Fakt: could not hook AGP's variant API: ${tooLate.message}")
        }
    }

    private fun staticSourceDirs(variant: Variant): List<Provider<out Collection<Directory>>> =
        listOfNotNull(variant.sources.kotlin?.static, variant.sources.java?.static)

    private fun org.gradle.api.plugins.ExtraPropertiesExtension.findPropertyOrNull(
        name: String
    ): Any? = if (has(name)) get(name) else null

    /** Pairs each variant's sources with its producer task, whichever is reported first. */
    internal class Registry {
        private companion object {
            val KOTLIN: org.gradle.api.Action<org.gradle.api.tasks.util.PatternFilterable> =
                org.gradle.api.Action { it.include("**/*.kt") }
        }

        var hooked: Boolean = false
        private val sources = mutableMapOf<String, List<Provider<out Collection<Directory>>>>()
        private val tasks = mutableMapOf<String, TaskProvider<FaktGenerateTask>>()

        fun onVariant(name: String, dirs: List<Provider<out Collection<Directory>>>) {
            sources[name] = dirs
            tasks[name]?.let { connect(it, dirs) }
        }

        fun onTask(name: String, task: TaskProvider<FaktGenerateTask>) {
            tasks[name] = task
            sources[name]?.let { connect(task, it) }
        }

        private fun connect(
            task: TaskProvider<FaktGenerateTask>,
            dirs: List<Provider<out Collection<Directory>>>,
        ) {
            // Kotlin files, not directory roots: the worker walks directory roots only a few levels
            // deep, and a variant's source roots sit above arbitrarily deep packages.
            task.configure { it.sources.from(it.project.files(dirs).asFileTree.matching(KOTLIN)) }
        }
    }
}
