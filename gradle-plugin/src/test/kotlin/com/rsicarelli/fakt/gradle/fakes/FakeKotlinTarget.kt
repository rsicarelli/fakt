package com.rsicarelli.fakt.gradle.fakes

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

/**
 * Fake KotlinTarget for testing discovery.
 */
internal class FakeKotlinTarget(
    private val name: String,
    override val platformType: KotlinPlatformType,
) : KotlinTarget {
    override fun getName(): String = name

    override val targetName: String = name
    override val disambiguationClassifier: String? = name

    override val compilations get() = error("Not used")
    override val publishable get() = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override val sourceSets get() = error("Not used")
    override val artifactsTaskName get() = error("Not used")
    override val apiElementsConfigurationName get() = error("Not used")
    override val runtimeElementsConfigurationName get() = error("Not used")
    override val sourcesElementsConfigurationName get() = error("Not used")

    override fun mavenPublication(action: Action<org.gradle.api.publish.maven.MavenPublication>) =
        error("Not used")

    override fun withSourcesJar(publish: Boolean) = error("Not used")

    override fun getAttributes() = error("Not used")

    override val project get() = error("Not used")
    override val extras get() = error("Not used")
    override val components get() = error("Not used")
}
