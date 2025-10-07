// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD tests for SourceSetDiscovery - the integration point that builds SourceSetContext.
 *
 * **Purpose**: Combine CompilationClassifier + SourceSetGraphTraversal to build complete context.
 *
 * **Test Strategy**:
 * 1. Use real interfaces (not fakes) where possible for integration validation
 * 2. Test both test and main compilations
 * 3. Verify source set hierarchy is captured correctly
 * 4. Validate output directory generation
 */
class SourceSetDiscoveryTest {
    @Test
    fun `GIVEN simple JVM test compilation WHEN building context THEN should capture test metadata`() {
        // GIVEN: JVM test compilation with commonMain as parent
        val commonMain = FakeSourceSet(name = "commonMain")
        val jvmTest = FakeSourceSet(name = "jvmTest", parents = setOf(commonMain))

        val target = FakeTarget(name = "jvm", platformType = KotlinPlatformType.jvm)
        val compilation =
            FakeCompilation(
                name = "test",
                defaultSourceSet = jvmTest,
                target = target,
                isTest = true,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(compilation, "/project/build")

        // THEN
        assertEquals("test", context.compilationName)
        assertEquals("jvm", context.targetName)
        assertEquals("jvm", context.platformType)
        assertTrue(context.isTest, "Should be classified as test compilation")
        assertEquals("jvmTest", context.defaultSourceSet.name)
        assertEquals(2, context.allSourceSets.size, "Should include jvmTest and commonMain")
        assertTrue(context.outputDirectory.contains("/project/build/generated/fakt"))
        assertTrue(context.outputDirectory.contains("test"), "Test output should go to test directory")
    }

    @Test
    fun `GIVEN simple JVM main compilation WHEN building context THEN should capture main metadata`() {
        // GIVEN: JVM main compilation
        val commonMain = FakeSourceSet(name = "commonMain")
        val jvmMain = FakeSourceSet(name = "jvmMain", parents = setOf(commonMain))

        val target = FakeTarget(name = "jvm", platformType = KotlinPlatformType.jvm)
        val compilation =
            FakeCompilation(
                name = "main",
                defaultSourceSet = jvmMain,
                target = target,
                isTest = false,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(compilation, "/project/build")

        // THEN
        assertEquals("main", context.compilationName)
        assertEquals("jvm", context.targetName)
        assertEquals("jvm", context.platformType)
        assertFalse(context.isTest, "Should be classified as main compilation")
        assertEquals("jvmMain", context.defaultSourceSet.name)
        assertTrue(context.outputDirectory.contains("main"), "Main output should go to main directory")
    }

    @Test
    fun `GIVEN KMP iOS compilation WHEN building context THEN should capture full hierarchy`() {
        // GIVEN: iOS hierarchy - iosX64Main → iosMain → appleMain → nativeMain → commonMain
        val commonMain = FakeSourceSet(name = "commonMain")
        val nativeMain = FakeSourceSet(name = "nativeMain", parents = setOf(commonMain))
        val appleMain = FakeSourceSet(name = "appleMain", parents = setOf(nativeMain))
        val iosMain = FakeSourceSet(name = "iosMain", parents = setOf(appleMain))
        val iosX64Main = FakeSourceSet(name = "iosX64Main", parents = setOf(iosMain))

        val target = FakeTarget(name = "iosX64", platformType = KotlinPlatformType.native)
        val compilation =
            FakeCompilation(
                name = "main",
                defaultSourceSet = iosX64Main,
                target = target,
                isTest = false,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(compilation, "/project/build")

        // THEN
        assertEquals("iosX64", context.targetName)
        assertEquals("native", context.platformType)
        assertEquals("iosX64Main", context.defaultSourceSet.name)
        assertEquals(5, context.allSourceSets.size, "Should include all 5 source sets in hierarchy")

        // Verify hierarchy is preserved
        val sourceSetNames = context.allSourceSets.map { it.name }.toSet()
        assertTrue(sourceSetNames.contains("iosX64Main"))
        assertTrue(sourceSetNames.contains("iosMain"))
        assertTrue(sourceSetNames.contains("appleMain"))
        assertTrue(sourceSetNames.contains("nativeMain"))
        assertTrue(sourceSetNames.contains("commonMain"))

        // Verify parent relationships are captured
        val iosX64Info = context.allSourceSets.first { it.name == "iosX64Main" }
        assertTrue(iosX64Info.parents.contains("iosMain"))

        val iosMainInfo = context.allSourceSets.first { it.name == "iosMain" }
        assertTrue(iosMainInfo.parents.contains("appleMain"))
    }

    @Test
    fun `GIVEN custom test suite WHEN building context THEN should classify as test`() {
        // GIVEN: integrationTest custom compilation
        val commonMain = FakeSourceSet(name = "commonMain")
        val integrationTest = FakeSourceSet(name = "integrationTest", parents = setOf(commonMain))

        val target = FakeTarget(name = "jvm", platformType = KotlinPlatformType.jvm)
        val compilation =
            FakeCompilation(
                name = "integrationTest",
                defaultSourceSet = integrationTest,
                target = target,
                isTest = true, // Classified by name ending with "Test"
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(compilation, "/project/build")

        // THEN
        assertEquals("integrationTest", context.compilationName)
        assertTrue(context.isTest, "Custom test suite should be classified as test")
        assertTrue(context.outputDirectory.contains("test"))
    }

    @Test
    fun `GIVEN diamond dependency WHEN building context THEN should not duplicate common parent`() {
        // GIVEN: Diamond pattern
        //        commonMain
        //        /        \
        //   nativeMain   appleMain
        //        \        /
        //         iosMain
        val commonMain = FakeSourceSet(name = "commonMain")
        val nativeMain = FakeSourceSet(name = "nativeMain", parents = setOf(commonMain))
        val appleMain = FakeSourceSet(name = "appleMain", parents = setOf(commonMain))
        val iosMain = FakeSourceSet(name = "iosMain", parents = setOf(nativeMain, appleMain))

        val target = FakeTarget(name = "ios", platformType = KotlinPlatformType.native)
        val compilation =
            FakeCompilation(
                name = "main",
                defaultSourceSet = iosMain,
                target = target,
                isTest = false,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(compilation, "/project/build")

        // THEN
        assertEquals(4, context.allSourceSets.size, "Should not duplicate commonMain")

        // Verify commonMain appears exactly once
        val commonMainCount = context.allSourceSets.count { it.name == "commonMain" }
        assertEquals(1, commonMainCount, "commonMain should appear exactly once")
    }

    @Test
    fun `GIVEN JS platform WHEN building context THEN should capture JS platform type`() {
        // GIVEN: JS compilation
        val commonMain = FakeSourceSet(name = "commonMain")
        val jsMain = FakeSourceSet(name = "jsMain", parents = setOf(commonMain))

        val target = FakeTarget(name = "js", platformType = KotlinPlatformType.js)
        val compilation =
            FakeCompilation(
                name = "main",
                defaultSourceSet = jsMain,
                target = target,
                isTest = false,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(compilation, "/project/build")

        // THEN
        assertEquals("js", context.platformType)
        assertEquals("js", context.targetName)
    }

    @Test
    fun `GIVEN Android JVM target WHEN building context THEN should capture androidJvm platform`() {
        // GIVEN: Android JVM compilation
        val commonMain = FakeSourceSet(name = "commonMain")
        val androidMain = FakeSourceSet(name = "androidMain", parents = setOf(commonMain))

        val target = FakeTarget(name = "android", platformType = KotlinPlatformType.androidJvm)
        val compilation =
            FakeCompilation(
                name = "main",
                defaultSourceSet = androidMain,
                target = target,
                isTest = false,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(compilation, "/project/build")

        // THEN
        assertEquals("androidJvm", context.platformType)
        assertEquals("android", context.targetName)
    }

    @Test
    fun `GIVEN output directory path WHEN building context THEN should use conventional structure`() {
        // GIVEN
        val commonMain = FakeSourceSet(name = "commonMain")
        val jvmTest = FakeSourceSet(name = "jvmTest", parents = setOf(commonMain))

        val target = FakeTarget(name = "jvm", platformType = KotlinPlatformType.jvm)
        val compilation =
            FakeCompilation(
                name = "test",
                defaultSourceSet = jvmTest,
                target = target,
                isTest = true,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(compilation, "/Users/dev/project/build")

        // THEN
        // Should follow pattern: {buildDir}/generated/fakt/{test|main}/kotlin
        assertTrue(context.outputDirectory.startsWith("/Users/dev/project/build/generated/fakt"))
        assertTrue(context.outputDirectory.contains("/test/"))
        assertTrue(context.outputDirectory.endsWith("/kotlin"))
    }
}

/**
 * Fake KotlinSourceSet for testing discovery.
 */
@Suppress("DEPRECATION")
private class FakeSourceSet(
    private val name: String,
    private val parents: Set<KotlinSourceSet> = emptySet(),
) : KotlinSourceSet {
    override fun getName(): String = name

    override val dependsOn: Set<KotlinSourceSet> = parents

    override fun dependsOn(other: KotlinSourceSet) = error("Not used in discovery tests")

    override val kotlin get() = error("Not used")

    override fun kotlin(configure: org.gradle.api.file.SourceDirectorySet.() -> Unit) = error("Not used")

    override fun kotlin(configure: org.gradle.api.Action<org.gradle.api.file.SourceDirectorySet>) = error("Not used")

    override val resources get() = error("Not used")
    override val languageSettings get() = error("Not used")

    override fun languageSettings(configure: org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder.() -> Unit) = error("Not used")

    override fun languageSettings(configure: org.gradle.api.Action<org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder>) =
        error("Not used")

    override fun dependencies(configure: org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler.() -> Unit) = error("Not used")

    override fun dependencies(configure: org.gradle.api.Action<org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler>) =
        error("Not used")

    override val customSourceFilesExtensions get() = error("Not used")
    override val project get() = error("Not used")
    override val extras get() = error("Not used")
    override val apiConfigurationName get() = error("Not used")
    override val compileOnlyConfigurationName get() = error("Not used")
    override val implementationConfigurationName get() = error("Not used")
    override val runtimeOnlyConfigurationName get() = error("Not used")
    override val apiMetadataConfigurationName get() = error("Deprecated")
    override val implementationMetadataConfigurationName get() = error("Deprecated")
    override val compileOnlyMetadataConfigurationName get() = error("Deprecated")
    override val runtimeOnlyMetadataConfigurationName get() = error("Deprecated")
}

/**
 * Fake KotlinTarget for testing discovery.
 */
private class FakeTarget(
    private val name: String,
    override val platformType: KotlinPlatformType,
) : KotlinTarget {
    override fun getName(): String = name

    override val targetName: String = name
    override val disambiguationClassifier: String? = name

    override val compilations get() = error("Not used")
    override val publishable get() = error("Not used")
    override val sourceSets get() = error("Not used")
    override val artifactsTaskName get() = error("Not used")
    override val apiElementsConfigurationName get() = error("Not used")
    override val runtimeElementsConfigurationName get() = error("Not used")
    override val sourcesElementsConfigurationName get() = error("Not used")

    override fun mavenPublication(action: org.gradle.api.Action<org.gradle.api.publish.maven.MavenPublication>) = error("Not used")

    override fun withSourcesJar(publish: Boolean) = error("Not used")

    override fun getAttributes() = error("Not used")

    override val project get() = error("Not used")
    override val extras get() = error("Not used")
    override val components get() = error("Not used")
}

/**
 * Fake KotlinCompilation for testing discovery.
 * Simulates minimal compilation metadata needed for context building.
 */
@org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
@Suppress("DEPRECATION_ERROR")
private class FakeCompilation(
    name: String,
    override val defaultSourceSet: KotlinSourceSet,
    override val target: KotlinTarget,
    private val isTest: Boolean = false,
) : KotlinCompilation<Any> {
    override fun getName(): String = compilationName

    override val compilationName: String = name

    // For testing classification
    override val allAssociatedCompilations: Set<KotlinCompilation<*>> =
        if (isTest && name != "test") {
            setOf(FakeCompilation("main", defaultSourceSet, target, false))
        } else {
            emptySet()
        }

    override val associatedCompilations get() = error("Not used")
    override val kotlinSourceSets get() = error("Not used")
    override val allKotlinSourceSets get() = error("Not used")

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) = error("Not used")

    override fun defaultSourceSet(configure: org.gradle.api.Action<KotlinSourceSet>) = error("Not used")

    override val compileDependencyConfigurationName get() = error("Not used")
    override var compileDependencyFiles: org.gradle.api.file.FileCollection
        get() = error("Not used")
        set(_) = error("Not used")
    override val runtimeDependencyConfigurationName get() = error("Not used")
    override val runtimeDependencyFiles get() = error("Not used")
    override val output get() = error("Not used")
    override val compileKotlinTaskName get() = error("Not used")
    override val compileTaskProvider get() = error("Not used")
    override val compilerOptions get() = error("Not used")
    override val compileKotlinTask get() = error("Not used")
    override val compileKotlinTaskProvider get() = error("Not used")
    override val kotlinOptions get() = error("Not used")

    override fun kotlinOptions(configure: org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions.() -> Unit) = error("Not used")

    override fun kotlinOptions(configure: org.gradle.api.Action<org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions>) = error("Not used")

    override fun getAttributes() = error("Not used")

    override fun attributes(configure: org.gradle.api.attributes.AttributeContainer.() -> Unit) = error("Not used")

    override fun attributes(configure: org.gradle.api.Action<org.gradle.api.attributes.AttributeContainer>) = error("Not used")

    override val compileAllTaskName get() = error("Not used")

    override fun associateWith(other: KotlinCompilation<*>) = error("Not used")

    override fun source(sourceSet: KotlinSourceSet) = error("Not used")

    override val implementationConfigurationName get() = error("Not used")
    override val apiConfigurationName get() = error("Not used")
    override val compileOnlyConfigurationName get() = error("Not used")
    override val runtimeOnlyConfigurationName get() = error("Not used")

    override fun dependencies(configure: org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler.() -> Unit) = error("Not used")

    override fun dependencies(configure: org.gradle.api.Action<org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler>) =
        error("Not used")

    override val extras get() = error("Not used")
    override val project get() = error("Not used")
}
