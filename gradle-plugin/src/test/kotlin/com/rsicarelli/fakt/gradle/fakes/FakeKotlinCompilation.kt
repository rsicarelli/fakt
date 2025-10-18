// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.fakes

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

/**
 * Fake KotlinCompilation for testing discovery.
 * Simulates minimal compilation metadata needed for context building.
 */
@ExperimentalKotlinGradlePluginApi
internal class FakeKotlinCompilation(
    name: String,
    override val defaultSourceSet: KotlinSourceSet,
    override val target: KotlinTarget,
    private val isTest: Boolean = false,
    associatedWith: Set<KotlinCompilation<*>> =
        if (isTest && name != "test") {
            setOf(FakeKotlinCompilation("main", defaultSourceSet, target, false))
        } else {
            emptySet()
        },
) : KotlinCompilation<Any> {
    override fun getName(): String = compilationName

    override val compilationName: String = name

    // For testing classification
    override val allAssociatedCompilations: Set<KotlinCompilation<*>> = associatedWith

    override val associatedCompilations get() = error("Not used")
    override val kotlinSourceSets get() = error("Not used")
    override val allKotlinSourceSets get() = error("Not used")

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) = error("Not used")

    override fun defaultSourceSet(configure: Action<KotlinSourceSet>) = error("Not used")

    override val compileDependencyConfigurationName get() = error("Not used")
    override var compileDependencyFiles: org.gradle.api.file.FileCollection
        get() = error("Not used")
        set(_) = error("Not used")
    override val runtimeDependencyConfigurationName get() = error("Not used")
    override val runtimeDependencyFiles get() = error("Not used")
    override val output get() = error("Not used")
    override val compileKotlinTaskName get() = error("Not used")
    override val compileTaskProvider get() = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override val compilerOptions get() = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override val compileKotlinTask get() = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override val compileKotlinTaskProvider get() = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override val kotlinOptions get() = error("Not used")

    override fun getAttributes() = error("Not used")

    override fun attributes(configure: org.gradle.api.attributes.AttributeContainer.() -> Unit) = error("Not used")

    override fun attributes(configure: Action<org.gradle.api.attributes.AttributeContainer>) = error("Not used")

    override val compileAllTaskName get() = error("Not used")

    override fun associateWith(other: KotlinCompilation<*>) = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override fun source(sourceSet: KotlinSourceSet) = error("Not used")

    override val implementationConfigurationName get() = error("Not used")
    override val apiConfigurationName get() = error("Not used")
    override val compileOnlyConfigurationName get() = error("Not used")
    override val runtimeOnlyConfigurationName get() = error("Not used")

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) = error("Not used")

    override fun dependencies(configure: Action<KotlinDependencyHandler>) = error("Not used")

    override val extras get() = error("Not used")
    override val project get() = error("Not used")
}
