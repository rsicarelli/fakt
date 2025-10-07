// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.fakes.FakeKotlinCompilation
import com.rsicarelli.fakt.gradle.fakes.FakeKotlinSourceSet
import com.rsicarelli.fakt.gradle.fakes.FakeKotlinTarget
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
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
@OptIn(ExperimentalKotlinGradlePluginApi::class)
class SourceSetDiscoveryTest {
    @Test
    fun `GIVEN simple JVM test compilation WHEN building context THEN should capture test metadata`() {
        // GIVEN: JVM test compilation with commonMain as parent
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val jvmTest = FakeKotlinSourceSet(
            name = "jvmTest",
            parents = setOf(commonMain)
        )

        val target = FakeKotlinTarget(
            name = "jvm",
            platformType = KotlinPlatformType.jvm
        )
        val compilation =
            FakeKotlinCompilation(
                name = "test",
                defaultSourceSet = jvmTest,
                target = target,
                isTest = true,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(
            compilation = compilation,
            buildDir = "/project/build"
        )

        // THEN
        assertEquals("test", context.compilationName)
        assertEquals("jvm", context.targetName)
        assertEquals("jvm", context.platformType)
        assertTrue(context.isTest, "Should be classified as test compilation")
        assertEquals("jvmTest", context.defaultSourceSet.name)
        assertEquals(2, context.allSourceSets.size, "Should include jvmTest and commonMain")
        assertTrue(context.outputDirectory.contains("/project/build/generated/fakt"))
        assertTrue(
            context.outputDirectory.contains("test"),
            "Test output should go to test directory"
        )
    }

    @Test
    fun `GIVEN simple JVM main compilation WHEN building context THEN should capture main metadata`() {
        // GIVEN: JVM main compilation
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val jvmMain = FakeKotlinSourceSet(
            name = "jvmMain",
            parents = setOf(commonMain)
        )

        val target = FakeKotlinTarget(
            name = "jvm",
            platformType = KotlinPlatformType.jvm
        )
        val compilation =
            FakeKotlinCompilation(
                name = "main",
                defaultSourceSet = jvmMain,
                target = target,
                isTest = false,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(
            compilation = compilation,
            buildDir = "/project/build"
        )

        // THEN
        assertEquals("main", context.compilationName)
        assertEquals("jvm", context.targetName)
        assertEquals("jvm", context.platformType)
        assertFalse(context.isTest, "Should be classified as main compilation")
        assertEquals("jvmMain", context.defaultSourceSet.name)
        assertTrue(
            context.outputDirectory.contains("main"),
            "Main output should go to main directory"
        )
    }

    @Test
    fun `GIVEN KMP iOS compilation WHEN building context THEN should capture full hierarchy`() {
        // GIVEN: iOS hierarchy - iosX64Main → iosMain → appleMain → nativeMain → commonMain
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val nativeMain = FakeKotlinSourceSet(
            name = "nativeMain",
            parents = setOf(commonMain)
        )
        val appleMain = FakeKotlinSourceSet(
            name = "appleMain",
            parents = setOf(nativeMain)
        )
        val iosMain = FakeKotlinSourceSet(
            name = "iosMain",
            parents = setOf(appleMain)
        )
        val iosX64Main = FakeKotlinSourceSet(
            name = "iosX64Main",
            parents = setOf(iosMain)
        )

        val target = FakeKotlinTarget(
            name = "iosX64",
            platformType = KotlinPlatformType.native
        )
        val compilation =
            FakeKotlinCompilation(
                name = "main",
                defaultSourceSet = iosX64Main,
                target = target,
                isTest = false,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(
            compilation = compilation,
            buildDir = "/project/build"
        )

        // THEN
        assertEquals("iosX64", context.targetName)
        assertEquals("native", context.platformType)
        assertEquals("iosX64Main", context.defaultSourceSet.name)
        assertEquals(
            expected = 5,
            actual = context.allSourceSets.size,
            message = "Should include all 5 source sets in hierarchy"
        )

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
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val integrationTest = FakeKotlinSourceSet(
            name = "integrationTest",
            parents = setOf(commonMain)
        )

        val target = FakeKotlinTarget(
            name = "jvm",
            platformType = KotlinPlatformType.jvm
        )
        val compilation =
            FakeKotlinCompilation(
                name = "integrationTest",
                defaultSourceSet = integrationTest,
                target = target,
                isTest = true, // Classified by name ending with "Test"
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(
            compilation = compilation,
            buildDir = "/project/build"
        )

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
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val nativeMain = FakeKotlinSourceSet(
            name = "nativeMain",
            parents = setOf(commonMain)
        )
        val appleMain = FakeKotlinSourceSet(
            name = "appleMain",
            parents = setOf(commonMain)
        )
        val iosMain = FakeKotlinSourceSet(
            name = "iosMain",
            parents = setOf(nativeMain, appleMain)
        )

        val target = FakeKotlinTarget(
            name = "ios",
            platformType = KotlinPlatformType.native
        )
        val compilation =
            FakeKotlinCompilation(
                name = "main",
                defaultSourceSet = iosMain,
                target = target,
                isTest = false,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(
            compilation = compilation,
            buildDir = "/project/build"
        )

        // THEN
        assertEquals(
            expected = 4,
            actual = context.allSourceSets.size,
            message = "Should not duplicate commonMain"
        )

        // Verify commonMain appears exactly once
        val commonMainCount = context.allSourceSets.count { it.name == "commonMain" }
        assertEquals(
            expected = 1,
            actual = commonMainCount,
            message = "commonMain should appear exactly once"
        )
    }

    @Test
    fun `GIVEN JS platform WHEN building context THEN should capture JS platform type`() {
        // GIVEN: JS compilation
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val jsMain = FakeKotlinSourceSet(
            name = "jsMain",
            parents = setOf(commonMain)
        )

        val target = FakeKotlinTarget(
            name = "js",
            platformType = KotlinPlatformType.js
        )
        val compilation =
            FakeKotlinCompilation(
                name = "main",
                defaultSourceSet = jsMain,
                target = target,
                isTest = false,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(
            compilation = compilation,
            buildDir = "/project/build"
        )

        // THEN
        assertEquals("js", context.platformType)
        assertEquals("js", context.targetName)
    }

    @Test
    fun `GIVEN Android JVM target WHEN building context THEN should capture androidJvm platform`() {
        // GIVEN: Android JVM compilation
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val androidMain = FakeKotlinSourceSet(name = "androidMain", parents = setOf(commonMain))

        val target = FakeKotlinTarget(
            name = "android",
            platformType = KotlinPlatformType.androidJvm
        )
        val compilation =
            FakeKotlinCompilation(
                name = "main",
                defaultSourceSet = androidMain,
                target = target,
                isTest = false,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(
            compilation = compilation,
            buildDir = "/project/build"
        )

        // THEN
        assertEquals("androidJvm", context.platformType)
        assertEquals("android", context.targetName)
    }

    @Test
    fun `GIVEN output directory path WHEN building context THEN should use conventional structure`() {
        // GIVEN
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val jvmTest = FakeKotlinSourceSet(
            name = "jvmTest",
            parents = setOf(commonMain)
        )

        val target = FakeKotlinTarget(
            name = "jvm",
            platformType = KotlinPlatformType.jvm
        )
        val compilation =
            FakeKotlinCompilation(
                name = "test",
                defaultSourceSet = jvmTest,
                target = target,
                isTest = true,
            )

        // WHEN
        val context = SourceSetDiscovery.buildContext(
            compilation = compilation,
            buildDir = "/Users/dev/project/build"
        )

        // THEN
        // Should follow pattern: {buildDir}/generated/fakt/{test|main}/kotlin
        assertTrue(context.outputDirectory.startsWith("/Users/dev/project/build/generated/fakt"))
        assertTrue(context.outputDirectory.contains("/test/"))
        assertTrue(context.outputDirectory.endsWith("/kotlin"))
    }
}

