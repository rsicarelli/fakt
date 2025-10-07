// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD tests for SourceSetResolver - resolves source set hierarchies from compiler context.
 *
 * **Architecture**:
 * ```
 * CommandLineProcessor (deserializes) → SourceSetContext → SourceSetResolver (resolves hierarchy)
 * ```
 *
 * **Test Strategy**:
 * 1. Test simple JVM source set resolution
 * 2. Test complex KMP hierarchies
 * 3. Test parent traversal
 * 4. Test source set not found
 * 5. Test default source set retrieval
 */
class SourceSetResolverTest {
    @Test
    fun `GIVEN simple JVM context WHEN resolving source set THEN should return correct info`() =
        runTest {
            // GIVEN: Simple JVM test context
            val context =
                SourceSetContext(
                    compilationName = "test",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = true,
                    defaultSourceSet = SourceSetInfo("jvmTest", listOf("commonMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo("jvmTest", listOf("commonMain")),
                            SourceSetInfo("commonMain", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/test/kotlin",
                )

            // WHEN: Resolving source set
            val resolver = SourceSetResolver(context)
            val sourceSetInfo = resolver.resolveSourceSet("jvmTest")

            // THEN: Should return correct source set info
            assertNotNull(sourceSetInfo, "Source set should be found")
            assertEquals("jvmTest", sourceSetInfo.name)
            assertEquals(listOf("commonMain"), sourceSetInfo.parents)
        }

    @Test
    fun `GIVEN complex KMP context WHEN resolving source set THEN should preserve full hierarchy`() =
        runTest {
            // GIVEN: iOS hierarchy - iosX64Main → iosMain → appleMain → nativeMain → commonMain
            val context =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "iosX64",
                    platformType = "native",
                    isTest = false,
                    defaultSourceSet = SourceSetInfo("iosX64Main", listOf("iosMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo("iosX64Main", listOf("iosMain")),
                            SourceSetInfo("iosMain", listOf("appleMain")),
                            SourceSetInfo("appleMain", listOf("nativeMain")),
                            SourceSetInfo("nativeMain", listOf("commonMain")),
                            SourceSetInfo("commonMain", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/main/kotlin",
                )

            // WHEN: Resolving each source set
            val resolver = SourceSetResolver(context)

            // THEN: Each level should have correct parent relationships
            val iosX64Info = resolver.resolveSourceSet("iosX64Main")
            assertNotNull(iosX64Info)
            assertEquals("iosX64Main", iosX64Info.name)
            assertEquals(listOf("iosMain"), iosX64Info.parents)

            val iosInfo = resolver.resolveSourceSet("iosMain")
            assertNotNull(iosInfo)
            assertEquals("iosMain", iosInfo.name)
            assertEquals(listOf("appleMain"), iosInfo.parents)

            val appleInfo = resolver.resolveSourceSet("appleMain")
            assertNotNull(appleInfo)
            assertEquals("appleMain", appleInfo.name)
            assertEquals(listOf("nativeMain"), appleInfo.parents)

            val nativeInfo = resolver.resolveSourceSet("nativeMain")
            assertNotNull(nativeInfo)
            assertEquals("nativeMain", nativeInfo.name)
            assertEquals(listOf("commonMain"), nativeInfo.parents)

            val commonInfo = resolver.resolveSourceSet("commonMain")
            assertNotNull(commonInfo)
            assertEquals("commonMain", commonInfo.name)
            assertTrue(commonInfo.parents.isEmpty(), "commonMain should have no parents")
        }

    @Test
    fun `GIVEN context WHEN resolving non-existent source set THEN should return null`() =
        runTest {
            // GIVEN: Simple context
            val context =
                SourceSetContext(
                    compilationName = "test",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = true,
                    defaultSourceSet = SourceSetInfo("jvmTest", listOf("commonMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo("jvmTest", listOf("commonMain")),
                            SourceSetInfo("commonMain", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/test/kotlin",
                )

            // WHEN: Resolving non-existent source set
            val resolver = SourceSetResolver(context)
            val sourceSetInfo = resolver.resolveSourceSet("nonExistent")

            // THEN: Should return null
            assertNull(sourceSetInfo, "Non-existent source set should return null")
        }

    @Test
    fun `GIVEN context WHEN getting default source set THEN should return default`() =
        runTest {
            // GIVEN: Context with default source set
            val defaultSourceSet = SourceSetInfo("jvmTest", listOf("commonMain"))
            val context =
                SourceSetContext(
                    compilationName = "test",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = true,
                    defaultSourceSet = defaultSourceSet,
                    allSourceSets =
                        listOf(
                            defaultSourceSet,
                            SourceSetInfo("commonMain", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/test/kotlin",
                )

            // WHEN: Getting default source set
            val resolver = SourceSetResolver(context)
            val result = resolver.getDefaultSourceSet()

            // THEN: Should return default source set
            assertEquals(defaultSourceSet, result)
            assertEquals("jvmTest", result.name)
        }

    @Test
    fun `GIVEN KMP context WHEN getting all parent source sets THEN should traverse full hierarchy`() =
        runTest {
            // GIVEN: iOS hierarchy
            val context =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "iosX64",
                    platformType = "native",
                    isTest = false,
                    defaultSourceSet = SourceSetInfo("iosX64Main", listOf("iosMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo("iosX64Main", listOf("iosMain")),
                            SourceSetInfo("iosMain", listOf("appleMain")),
                            SourceSetInfo("appleMain", listOf("nativeMain")),
                            SourceSetInfo("nativeMain", listOf("commonMain")),
                            SourceSetInfo("commonMain", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/main/kotlin",
                )

            // WHEN: Getting all parents for iosX64Main
            val resolver = SourceSetResolver(context)
            val allParents = resolver.getAllParentSourceSets("iosX64Main")

            // THEN: Should include full hierarchy (excluding self)
            assertEquals(4, allParents.size, "Should have 4 parents")
            assertTrue(allParents.any { it.name == "iosMain" })
            assertTrue(allParents.any { it.name == "appleMain" })
            assertTrue(allParents.any { it.name == "nativeMain" })
            assertTrue(allParents.any { it.name == "commonMain" })
        }

    @Test
    fun `GIVEN context WHEN getting all source sets THEN should return all from context`() =
        runTest {
            // GIVEN: Context with multiple source sets
            val context =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "iosX64",
                    platformType = "native",
                    isTest = false,
                    defaultSourceSet = SourceSetInfo("iosX64Main", listOf("iosMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo("iosX64Main", listOf("iosMain")),
                            SourceSetInfo("iosMain", listOf("appleMain")),
                            SourceSetInfo("appleMain", listOf("nativeMain")),
                            SourceSetInfo("nativeMain", listOf("commonMain")),
                            SourceSetInfo("commonMain", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/main/kotlin",
                )

            // WHEN: Getting all source sets
            val resolver = SourceSetResolver(context)
            val allSourceSets = resolver.getAllSourceSets()

            // THEN: Should return all 5 source sets
            assertEquals(5, allSourceSets.size)
            assertTrue(allSourceSets.any { it.name == "iosX64Main" })
            assertTrue(allSourceSets.any { it.name == "iosMain" })
            assertTrue(allSourceSets.any { it.name == "appleMain" })
            assertTrue(allSourceSets.any { it.name == "nativeMain" })
            assertTrue(allSourceSets.any { it.name == "commonMain" })
        }

    @Test
    fun `GIVEN context WHEN checking if source set is test THEN should use context isTest flag`() =
        runTest {
            // GIVEN: Test context
            val testContext =
                SourceSetContext(
                    compilationName = "test",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = true,
                    defaultSourceSet = SourceSetInfo("jvmTest", listOf("commonMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo("jvmTest", listOf("commonMain")),
                            SourceSetInfo("commonMain", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/test/kotlin",
                )

            // WHEN: Checking if compilation is test
            val resolver = SourceSetResolver(testContext)
            val isTest = resolver.isTestSourceSet()

            // THEN: Should return true
            assertTrue(isTest, "Test source set should be identified")
        }

    @Test
    fun `GIVEN multi-parent source set WHEN getting parents THEN should return all immediate parents`() =
        runTest {
            // GIVEN: Source set with multiple parents (e.g., appleTest → iosTest, macosTest)
            val context =
                SourceSetContext(
                    compilationName = "test",
                    targetName = "apple",
                    platformType = "native",
                    isTest = true,
                    defaultSourceSet = SourceSetInfo("appleTest", listOf("iosTest", "macosTest")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo("appleTest", listOf("iosTest", "macosTest")),
                            SourceSetInfo("iosTest", listOf("commonTest")),
                            SourceSetInfo("macosTest", listOf("commonTest")),
                            SourceSetInfo("commonTest", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/test/kotlin",
                )

            // WHEN: Resolving source set with multiple parents
            val resolver = SourceSetResolver(context)
            val sourceSetInfo = resolver.resolveSourceSet("appleTest")

            // THEN: Should return both immediate parents
            assertNotNull(sourceSetInfo)
            assertEquals(2, sourceSetInfo.parents.size)
            assertTrue(sourceSetInfo.parents.contains("iosTest"))
            assertTrue(sourceSetInfo.parents.contains("macosTest"))
        }
}
