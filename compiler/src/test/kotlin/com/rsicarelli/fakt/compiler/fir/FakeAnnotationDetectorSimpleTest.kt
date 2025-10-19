// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Essential tests for FakeAnnotationDetector focusing on core functionality.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeAnnotationDetectorSimpleTest {
    @Test
    fun `GIVEN detector instance WHEN creating THEN should initialize successfully`() =
        runTest {
            // Given - detector creation
            val detector = FakeAnnotationDetector()

            // When - checking instance
            // Then - should create valid detector instance
            assertNotNull(detector, "Detector should be created successfully")
        }

    @Test
    fun `GIVEN default parameters WHEN creating instance THEN should have correct default values`() =
        runTest {
            // Given - default parameters instance
            val params = FakeAnnotationParameters()

            // When - checking default values
            // Then - should have sensible defaults
            assertFalse(params.trackCalls, "Should have call tracking disabled by default")
            assertFalse(params.builder, "Should have builder pattern disabled by default")
            assertEquals(
                emptyList(),
                params.dependencies,
                "Should have empty dependencies by default"
            )
            assertTrue(params.concurrent, "Should be concurrent/thread-safe by default")
            assertEquals("test", params.scope, "Should have test scope by default")
        }

    @Test
    fun `GIVEN custom parameters WHEN creating instance THEN should preserve custom values`() =
        runTest {
            // Given - custom parameters configuration
            val customDeps =
                listOf(
                    ClassId.topLevel(FqName("com.example.Dependency1")),
                    ClassId.topLevel(FqName("com.example.Dependency2")),
                )
            val params =
                FakeAnnotationParameters(
                    trackCalls = true,
                    builder = true,
                    dependencies = customDeps,
                    concurrent = false,
                    scope = "integration",
                )

            // When - checking custom values
            // Then - should preserve all custom settings
            assertTrue(params.trackCalls, "Should preserve call tracking setting")
            assertTrue(params.builder, "Should preserve builder pattern setting")
            assertEquals(customDeps, params.dependencies, "Should preserve dependencies")
            assertFalse(params.concurrent, "Should preserve concurrent setting")
            assertEquals("integration", params.scope, "Should preserve scope setting")
        }

    @Test
    fun `GIVEN FqName patterns WHEN creating ClassId THEN should handle various package structures`() =
        runTest {
            // Given - various FqName patterns for testing
            val testFqNames =
                listOf(
                    "com.rsicarelli.fakt.Fake",
                    "com.rsicarelli.fakt.FakeConfig",
                    "com.example.CustomAnnotation",
                    "org.test.nested.package.Annotation",
                )

            // When - creating ClassId from FqNames
            // Then - should create valid ClassIds for all patterns
            testFqNames.forEach { fqNameString ->
                val classId = ClassId.topLevel(FqName(fqNameString))
                assertNotNull(classId, "Should create valid ClassId for $fqNameString")
                assertEquals(
                    fqNameString,
                    classId.asSingleFqName().asString(),
                    "ClassId should preserve FqName correctly",
                )
            }
        }

    @Test
    fun `GIVEN annotation detection methods WHEN checking method existence THEN should have expected methods`() =
        runTest {
            // Given
            val detector = FakeAnnotationDetector()

            // When & Then
            assertTrue(
                detector::hasFakeAnnotation.name == "hasFakeAnnotation",
                "Should have hasFakeAnnotation method",
            )
            assertTrue(
                detector::hasFakeConfigAnnotation.name == "hasFakeConfigAnnotation",
                "Should have hasFakeConfigAnnotation method",
            )
            assertTrue(
                detector::extractFakeParameters.name == "extractFakeParameters",
                "Should have extractFakeParameters method",
            )
        }

    @Test
    fun `GIVEN default parameters WHEN creating instance THEN should have expected default values`() =
        runTest {
            // Given & When
            val defaultParams = FakeAnnotationParameters()

            // Then
            assertFalse(defaultParams.trackCalls, "Should not track calls by default")
            assertFalse(defaultParams.builder, "Should not generate builders by default")
            assertEquals(
                emptyList(),
                defaultParams.dependencies,
                "Should have no dependencies by default"
            )
            assertTrue(defaultParams.concurrent, "Should be thread-safe by default")
            assertEquals("test", defaultParams.scope, "Should use test scope by default")
        }

    @Test
    fun `GIVEN complex dependency lists WHEN creating parameters THEN should handle empty and populated lists`() =
        runTest {
            // Given - various dependency list scenarios
            val emptyDeps = FakeAnnotationParameters(dependencies = emptyList())
            val singleDep =
                FakeAnnotationParameters(
                    dependencies =
                        listOf(
                            ClassId.topLevel(FqName("com.example.SingleDep")),
                        ),
                )
            val manyDeps =
                FakeAnnotationParameters(
                    dependencies =
                        (1..5).map {
                            ClassId.topLevel(FqName("com.example.Dep$it"))
                        },
                )

            // When - handling different dependency scenarios
            // Then - should handle all list sizes correctly
            assertEquals(0, emptyDeps.dependencies.size, "Should handle empty dependencies")
            assertEquals(1, singleDep.dependencies.size, "Should handle single dependency")
            assertEquals(5, manyDeps.dependencies.size, "Should handle many dependencies")

            // Verify dependency content preservation
            assertEquals(
                "com.example.SingleDep",
                singleDep.dependencies
                    .first()
                    .asSingleFqName()
                    .asString(),
                "Should preserve single dependency correctly",
            )
            assertTrue(
                manyDeps.dependencies.any {
                    it.asSingleFqName().asString() == "com.example.Dep3"
                },
                "Should preserve all dependencies in large list",
            )
        }
}
