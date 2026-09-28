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

    /** Android plugins that compile Kotlin with AGP 9's built-in Kotlin support. */
    private val ANDROID_PLUGIN_IDS: List<String> =
        listOf("com.android.library", "com.android.application")

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
     * Whether [project] is an Android module on AGP's built-in Kotlin: an Android plugin is applied
     * and KGP's `org.jetbrains.kotlin.android` is not. Plugin ids only, so no AGP class is loaded.
     */
    fun usesBuiltInKotlin(project: Project): Boolean =
        ANDROID_PLUGIN_IDS.any(project.plugins::hasPlugin) &&
            !project.plugins.hasPlugin(KOTLIN_ANDROID_PLUGIN_ID)

    /**
     * Whether [install] could hook [project]'s variants: an Android plugin is applied and AGP's
     * variant API is visible to Fakt's classloader. When it isn't, a built-in Kotlin module stays
     * on the in-process plugin, which reads sources from the compile task itself.
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
        }
    }

    private fun staticSourceDirs(variant: Variant): List<Provider<out Collection<Directory>>> =
        listOfNotNull(variant.sources.kotlin?.static, variant.sources.java?.static)

    private fun org.gradle.api.plugins.ExtraPropertiesExtension.findPropertyOrNull(
        name: String
    ): Any? = if (has(name)) get(name) else null

    /** Pairs each variant's sources with its producer task, whichever is reported first. */
    internal class Registry {
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
            task.configure { it.sources.from(dirs) }
        }
    }
}
