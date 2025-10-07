package com.rsicarelli.fakt.gradle.fakes

import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder


/**
 * Fake KotlinSourceSet for testing discovery.
 */
internal class FakeKotlinSourceSet(
    private val name: String,
    parents: Set<KotlinSourceSet> = emptySet(),
) : KotlinSourceSet {
    override fun getName(): String = name

    override val dependsOn: Set<KotlinSourceSet> = parents

    override fun dependsOn(other: KotlinSourceSet) = error("Not used in discovery tests")

    override val kotlin get() = error("Not used")
    override fun kotlin(configure: SourceDirectorySet.() -> Unit) = error("Not used")
    override fun kotlin(configure: Action<SourceDirectorySet>) = error("Not used")

    override val resources get() = error("Not used")
    override val languageSettings get() = error("Not used")

    override fun languageSettings(configure: LanguageSettingsBuilder.() -> Unit) = error("Not used")
    override fun languageSettings(configure: Action<LanguageSettingsBuilder>) = error("Not used")
    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) = error("Not used")
    override fun dependencies(configure: Action<KotlinDependencyHandler>) = error("Not used")

    override val customSourceFilesExtensions get() = error("Not used")
    override val project get() = error("Not used")
    override val extras get() = error("Not used")
    override val apiConfigurationName get() = error("Not used")
    override val compileOnlyConfigurationName get() = error("Not used")
    override val implementationConfigurationName get() = error("Not used")
    override val runtimeOnlyConfigurationName get() = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override val apiMetadataConfigurationName get() = error("Deprecated")

    @Suppress("OVERRIDE_DEPRECATION")
    override val implementationMetadataConfigurationName get() = error("Deprecated")

    @Suppress("OVERRIDE_DEPRECATION")
    override val compileOnlyMetadataConfigurationName get() = error("Deprecated")

    @Suppress("OVERRIDE_DEPRECATION")
    override val runtimeOnlyMetadataConfigurationName get() = error("Deprecated")
}
