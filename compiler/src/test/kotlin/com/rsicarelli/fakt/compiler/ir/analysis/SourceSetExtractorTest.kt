// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TDD tests for SourceSetExtractor - extracting source set from IrFile paths.
 *
 * **Problem Statement**:
 * The compiler receives IrClass instances from multiple source sets in a single compilation.
 * For example, jvmMain compilation includes:
 * - Files from jvmMain/kotlin/ (JVM-specific)
 * - Files from commonMain/kotlin/ (common code visible via dependsOn)
 *
 * We need to determine which source set each IrClass came from to route fakes correctly:
 * - JVM files → jvmTest
 * - Common files → commonTest
 *
 * **Solution**:
 * Extract source set from IrFile path by parsing the directory structure.
 *
 * **Test Strategy** (TDD - RED phase):
 * 1. Write tests for real file path patterns
 * 2. Tests will FAIL (SourceSetExtractor doesn't exist yet)
 * 3. Implement SourceSetExtractor (GREEN phase)
 * 4. Tests pass
 */
class SourceSetExtractorTest {
    @Test
    fun `GIVEN file path from jvmMain WHEN extracting source set THEN should return jvmMain`() =
        runTest {
            // GIVEN: Realistic file paths from jvmMain
            val testCases =
                listOf(
                    "/Users/dev/project/api/src/jvmMain/kotlin/Database.kt",
                    "/home/ci/workspace/api/src/jvmMain/kotlin/com/example/FileSystem.kt",
                    "C:\\Projects\\api\\src\\jvmMain\\kotlin\\Services.kt",
                )

            testCases.forEach { filePath ->
                // WHEN: Extracting source set from path
                val sourceSet = SourceSetExtractor.extractSourceSetFromPath(filePath)

                // THEN: Should return "jvmMain"
                assertEquals(
                    "jvmMain",
                    sourceSet,
                    "Path $filePath should extract jvmMain",
                )
            }
        }

    @Test
    fun `GIVEN file path from commonMain WHEN extracting source set THEN should return commonMain`() =
        runTest {
            // GIVEN: Realistic file paths from commonMain
            val testCases =
                listOf(
                    "/Users/dev/project/api/src/commonMain/kotlin/Network.kt",
                    "/home/ci/workspace/api/src/commonMain/kotlin/com/example/shared/Storage.kt",
                    "C:\\Projects\\api\\src\\commonMain\\kotlin\\Logger.kt",
                )

            testCases.forEach { filePath ->
                // WHEN: Extracting source set from path
                val sourceSet = SourceSetExtractor.extractSourceSetFromPath(filePath)

                // THEN: Should return "commonMain"
                assertEquals(
                    "commonMain",
                    sourceSet,
                    "Path $filePath should extract commonMain",
                )
            }
        }

    @Test
    fun `GIVEN file path from iosMain WHEN extracting source set THEN should return iosMain`() =
        runTest {
            // GIVEN: Realistic file paths from iosMain
            val testCases =
                listOf(
                    "/Users/dev/project/api/src/iosMain/kotlin/IOSService.kt",
                    "/home/ci/workspace/api/src/iosArm64Main/kotlin/PlatformService.kt",
                )

            testCases.forEach { filePath ->
                // WHEN: Extracting source set from path
                val sourceSet = SourceSetExtractor.extractSourceSetFromPath(filePath)

                // THEN: Should return the specific iOS source set
                assertEquals(
                    true,
                    sourceSet?.contains("ios") ?: false,
                    "Path $filePath should extract iOS-related source set, got: $sourceSet",
                )
            }
        }

    @Test
    fun `GIVEN file path without src directory WHEN extracting THEN should return null`() =
        runTest {
            // GIVEN: Paths without standard /src/ structure
            val testCases =
                listOf(
                    "/Users/dev/project/api/kotlin/Database.kt",
                    "/home/ci/build/classes/com/example/Service.kt",
                    "Database.kt",
                )

            testCases.forEach { filePath ->
                // WHEN: Extracting source set from path
                val sourceSet = SourceSetExtractor.extractSourceSetFromPath(filePath)

                // THEN: Should return null
                assertNull(
                    sourceSet,
                    "Path $filePath without /src/ should return null",
                )
            }
        }

    @Test
    fun `GIVEN various platform source sets WHEN extracting THEN should return correct source set`() =
        runTest {
            // GIVEN: Paths from different platforms
            val testCases =
                mapOf(
                    "/project/src/jsMain/kotlin/File.kt" to "jsMain",
                    "/project/src/nativeMain/kotlin/File.kt" to "nativeMain",
                    "/project/src/linuxX64Main/kotlin/File.kt" to "linuxX64Main",
                    "/project/src/macosArm64Main/kotlin/File.kt" to "macosArm64Main",
                    "/project/src/androidMain/kotlin/File.kt" to "androidMain",
                )

            testCases.forEach { (filePath, expectedSourceSet) ->
                // WHEN: Extracting source set from path
                val sourceSet = SourceSetExtractor.extractSourceSetFromPath(filePath)

                // THEN: Should return correct platform source set
                assertEquals(
                    expectedSourceSet,
                    sourceSet,
                    "Path $filePath should extract $expectedSourceSet",
                )
            }
        }

    @Test
    fun `GIVEN commonTest path WHEN extracting THEN should return commonTest`() =
        runTest {
            // GIVEN: Path from test source set
            val filePath = "/Users/dev/project/api/src/commonTest/kotlin/NetworkTest.kt"

            // WHEN: Extracting source set from path
            val sourceSet = SourceSetExtractor.extractSourceSetFromPath(filePath)

            // THEN: Should return "commonTest"
            assertEquals(
                "commonTest",
                sourceSet,
                "Test source set should be extracted correctly",
            )
        }

    @Test
    fun `GIVEN path with nested packages WHEN extracting THEN should ignore package structure`() =
        runTest {
            // GIVEN: Path with deep package structure (should NOT affect source set extraction)
            val testCases =
                mapOf(
                    "/project/src/jvmMain/kotlin/com/example/api/jvm/Database.kt" to "jvmMain",
                    "/project/src/commonMain/kotlin/com/shared/common/Network.kt" to "commonMain",
                )

            testCases.forEach { (filePath, expectedSourceSet) ->
                // WHEN: Extracting source set from path
                val sourceSet = SourceSetExtractor.extractSourceSetFromPath(filePath)

                // THEN: Should extract from directory, not package
                assertEquals(
                    expectedSourceSet,
                    sourceSet,
                    "Should extract from directory structure, not package names in path",
                )
            }
        }

    @Test
    fun `GIVEN path with custom source set name WHEN extracting THEN should return custom name`() =
        runTest {
            // GIVEN: Paths with custom source set names (e.g., integrationTest, e2eTest)
            val testCases =
                mapOf(
                    "/project/src/integrationTest/kotlin/IntegrationTest.kt" to "integrationTest",
                    "/project/src/e2eTest/kotlin/E2ETest.kt" to "e2eTest",
                    "/project/src/performanceTest/kotlin/PerfTest.kt" to "performanceTest",
                )

            testCases.forEach { (filePath, expectedSourceSet) ->
                // WHEN: Extracting source set from path
                val sourceSet = SourceSetExtractor.extractSourceSetFromPath(filePath)

                // THEN: Should return custom source set name
                assertEquals(
                    expectedSourceSet,
                    sourceSet,
                    "Custom source sets should be extracted correctly",
                )
            }
        }
}
