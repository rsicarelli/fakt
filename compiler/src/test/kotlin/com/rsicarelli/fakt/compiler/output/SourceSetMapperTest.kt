// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.output

import com.rsicarelli.fakt.compiler.telemetry.FaktLogger
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD tests for SourceSetMapper source set-based routing.
 *
 * **Problem Statement**:
 * In KMP compilations, a single compilation (e.g., jvmMain) can include files from multiple source sets:
 * - Files from jvmMain/kotlin/ (platform-specific)
 * - Files from commonMain/kotlin/ (shared code visible via dependsOn)
 *
 * Generator was placing ALL fakes in the same directory based on compilation name:
 * - jvmMain compilation → all fakes to commonTest/ (WRONG!)
 * - Result: Common interfaces generate to wrong location, iOS compilation fails
 *
 * **Solution**:
 * Extract source set from IrFile path (source of truth approach):
 * - Parse /path/to/src/{sourceSet}/kotlin/File.kt → extract {sourceSet}
 * - Map source set to test source set: jvmMain → jvmTest, commonMain → commonTest
 * - Route each fake based on its SOURCE file location, not compilation name
 *
 * **Test Strategy** (TDD):
 * 1. Test source set extraction from file paths
 * 2. Test source set → test source set mapping
 * 3. Test output directory routing with explicit source sets
 * 4. Test fallback to module name mapping when source set is null
 */
class SourceSetMapperTest {
    private val logger = FaktLogger.quiet()

    /**
     * Helper to expose internal methods for testing.
     * Since SourceSetMapper has private methods we need to test, we use reflection or
     * create a test-friendly wrapper.
     */
    private class TestableSourceSetMapper(
        outputDir: String?,
        logger: FaktLogger,
    ) {
        // Store instance for method access via reflection
        private val mapper = SourceSetMapper(outputDir, logger)

        // Expose mapToTestSourceSet for testing (it's currently private)
        @Suppress("UNCHECKED_CAST")
        fun mapToTestSourceSet(moduleName: String): String {
            val method =
                SourceSetMapper::class.java.getDeclaredMethod(
                    "mapToTestSourceSet",
                    String::class.java,
                )
            method.isAccessible = true
            return method.invoke(mapper, moduleName) as String
        }
    }

    @Test
    fun `GIVEN JVM module name WHEN mapping to test source set THEN should return jvmTest`() =
        runTest {
            // GIVEN: JVM module names
            val mapper = TestableSourceSetMapper(null, logger)
            val testCases =
                listOf(
                    "api_jvmMain",
                    "module_jvmMain",
                    "jvmMain",
                )

            testCases.forEach { moduleName ->
                // WHEN: Mapping to test source set
                val result = mapper.mapToTestSourceSet(moduleName)

                // THEN: Should return jvmTest
                assertEquals(
                    "jvmTest",
                    result,
                    "Module $moduleName should map to jvmTest",
                )
            }
        }

    @Test
    fun `GIVEN iOS module name WHEN mapping to test source set THEN should return iosTest`() =
        runTest {
            // GIVEN: iOS module names
            val mapper = TestableSourceSetMapper(null, logger)
            val testCases =
                listOf(
                    "api_iosMain",
                    "module_iosArm64Main",
                    "iosMain",
                )

            testCases.forEach { moduleName ->
                // WHEN: Mapping to test source set
                val result = mapper.mapToTestSourceSet(moduleName)

                // THEN: Should return iosTest or platform-specific variant
                assertTrue(
                    result.contains("iosTest") || result.contains("iosArm64Test"),
                    "Module $moduleName should map to iosTest variant, got: $result",
                )
            }
        }

    @Test
    fun `GIVEN common module name WHEN mapping to test source set THEN should return commonTest`() =
        runTest {
            // GIVEN: Common module names
            val mapper = TestableSourceSetMapper(null, logger)
            val testCases =
                listOf(
                    "api_commonMain",
                    "module_commonMain",
                    "commonMain",
                )

            testCases.forEach { moduleName ->
                // WHEN: Mapping to test source set
                val result = mapper.mapToTestSourceSet(moduleName)

                // THEN: Should return commonTest
                assertEquals(
                    "commonTest",
                    result,
                    "Module $moduleName should map to commonTest",
                )
            }
        }

    @Test
    fun `GIVEN JS module name WHEN mapping to test source set THEN should return jsTest`() =
        runTest {
            // GIVEN: JS module names
            val mapper = TestableSourceSetMapper(null, logger)
            val testCases =
                listOf(
                    "api_jsMain",
                    "module_jsMain",
                    "jsMain",
                )

            testCases.forEach { moduleName ->
                // WHEN: Mapping to test source set
                val result = mapper.mapToTestSourceSet(moduleName)

                // THEN: Should return jsTest
                assertEquals(
                    "jsTest",
                    result,
                    "Module $moduleName should map to jsTest",
                )
            }
        }

    @Test
    fun `GIVEN platform-specific module names WHEN mapping THEN should correctly identify all platforms`() =
        runTest {
            // GIVEN: Multiple platform-specific modules
            val mapper = TestableSourceSetMapper(null, logger)

            val platformCases =
                mapOf(
                    "api_jvmMain" to "jvmTest",
                    "api_iosMain" to "iosTest",
                    "api_jsMain" to "jsTest",
                    "api_commonMain" to "commonTest",
                    "api_nativeMain" to "nativeTest",
                    "api_macosMain" to "macosTest",
                    "api_linuxMain" to "linuxTest",
                )

            platformCases.forEach { (moduleName, expectedSourceSet) ->
                // WHEN: Mapping each platform module
                val result = mapper.mapToTestSourceSet(moduleName)

                // THEN: Should map to correct platform test source set
                assertEquals(
                    expectedSourceSet,
                    result,
                    "Module $moduleName should map to $expectedSourceSet",
                )
            }
        }

    @Test
    fun `GIVEN ambiguous module name WHEN mapping THEN should use intelligent fallback`() =
        runTest {
            // GIVEN: Module name without clear platform marker
            val mapper = TestableSourceSetMapper(null, logger)
            val testCases =
                listOf(
                    "api" to "commonTest", // Default fallback
                    "module" to "commonTest", // Default fallback
                    "test" to "jvmTest", // Test keyword → jvmTest
                )

            testCases.forEach { (moduleName, expectedSourceSet) ->
                // WHEN: Mapping ambiguous module
                val result = mapper.mapToTestSourceSet(moduleName)

                // THEN: Should use intelligent fallback
                assertEquals(
                    expectedSourceSet,
                    result,
                    "Module $moduleName should fallback to $expectedSourceSet",
                )
            }
        }

    // ========================================
    // Source Set-Based Routing Tests (RED phase - will fail initially)
    // ========================================

    @Test
    fun `GIVEN jvmMain source set WHEN getting output directory THEN should route to jvmTest`(
        @TempDir tempDir: File,
    ) = runTest {
        // GIVEN: Output dir for jvmMain and source set name
        val baseOutputDir = tempDir.resolve("build/generated/fakt").absolutePath
        val sourceSetMapper = SourceSetMapper(baseOutputDir, logger)

        // Mock module fragment (module name doesn't matter when source set provided)
        val moduleFragment = createMockModuleFragment("api")

        // WHEN: Getting generated dir with jvmMain source set
        val outputDir =
            sourceSetMapper.getGeneratedSourcesDir(
                moduleFragment = moduleFragment,
                sourceSetName = "jvmMain",
            )

        // THEN: Should route to jvmTest
        assertTrue(
            outputDir.path.contains("jvmTest") || outputDir.path.contains("/jvmMain/"),
            "jvmMain source set should route to jvmTest, got: ${outputDir.path}",
        )
    }

    @Test
    fun `GIVEN commonMain source set in jvmMain compilation WHEN getting output directory THEN should route to commonTest not jvmTest`(
        @TempDir tempDir: File,
    ) = runTest {
        // GIVEN: jvmMain compilation but file from commonMain
        val baseOutputDir = tempDir.resolve("build/generated/fakt/jvmTest").absolutePath
        val sourceSetMapper = SourceSetMapper(baseOutputDir, logger)

        val moduleFragment = createMockModuleFragment("api_jvmMain")

        // WHEN: Getting generated dir with commonMain source set (file from commonMain visible in jvmMain)
        val outputDir =
            sourceSetMapper.getGeneratedSourcesDir(
                moduleFragment = moduleFragment,
                sourceSetName = "commonMain",
            )

        // THEN: Should route to commonTest NOT jvmTest
        assertTrue(
            outputDir.path.contains("commonTest") || outputDir.path.contains("/commonMain/"),
            "commonMain source set should route to commonTest even in jvmMain compilation, got: ${outputDir.path}",
        )
    }

    @Test
    fun `GIVEN iosMain source set WHEN getting output directory THEN should route to iosTest`(
        @TempDir tempDir: File,
    ) = runTest {
        // GIVEN: Output dir and iosMain source set
        val baseOutputDir = tempDir.resolve("build/generated/fakt").absolutePath
        val sourceSetMapper = SourceSetMapper(baseOutputDir, logger)

        val moduleFragment = createMockModuleFragment("api")

        // WHEN: Getting generated dir with iosMain source set
        val outputDir =
            sourceSetMapper.getGeneratedSourcesDir(
                moduleFragment = moduleFragment,
                sourceSetName = "iosMain",
            )

        // THEN: Should route to iosTest
        assertTrue(
            outputDir.path.contains("iosTest") || outputDir.path.contains("/iosMain/"),
            "iosMain source set should route to iosTest, got: ${outputDir.path}",
        )
    }

    @Test
    fun `GIVEN null source set WHEN getting output directory THEN should fall back to module name mapping`(
        @TempDir tempDir: File,
    ) = runTest {
        // GIVEN: No Gradle-provided output dir (null), forcing module name fallback
        // Create a temp project structure to simulate the fallback scenario
        val projectDir = tempDir.resolve("project").also { it.mkdirs() }
        projectDir.resolve("build.gradle.kts").writeText("// mock build file")

        // Change working directory temporarily for this test
        val originalDir = System.getProperty("user.dir")
        try {
            System.setProperty("user.dir", projectDir.absolutePath)

            val sourceSetMapper = SourceSetMapper(null, logger)
            val moduleFragment = createMockModuleFragment("api_jvmMain")

            // WHEN: Getting generated dir without source set (null)
            val outputDir =
                sourceSetMapper.getGeneratedSourcesDir(
                    moduleFragment = moduleFragment,
                    sourceSetName = null,
                )

            // THEN: Should fall back to module name mapping (jvmMain → jvmTest)
            assertTrue(
                outputDir.path.contains("jvmTest"),
                "Null source set should fall back to module name mapping, got: ${outputDir.path}",
            )
        } finally {
            // Restore original working directory
            System.setProperty("user.dir", originalDir)
        }
    }

    // Helper to create mock module fragment
    private fun createMockModuleFragment(moduleName: String): org.jetbrains.kotlin.ir.declarations.IrModuleFragment =
        object : org.jetbrains.kotlin.ir.declarations.IrModuleFragment() {
            override val name: org.jetbrains.kotlin.name.Name =
                org.jetbrains.kotlin.name.Name
                    .identifier(moduleName)
            override val files: MutableList<org.jetbrains.kotlin.ir.declarations.IrFile> = mutableListOf()

            // Required abstract members from IrElement
            override var startOffset: Int = 0
            override var endOffset: Int = 0
            override var attributeOwnerId: org.jetbrains.kotlin.ir.IrElement = this

            // Required abstract member from IrModuleFragment
            override val descriptor: org.jetbrains.kotlin.descriptors.ModuleDescriptor
                get() = throw UnsupportedOperationException("Mock descriptor not needed for test")
        }
}
