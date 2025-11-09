// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.context

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.fir.metadata.FirMetadataStorage
import com.rsicarelli.fakt.compiler.core.config.FaktOptions
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [FaktSharedContext] following GIVEN-WHEN-THEN pattern.
 *
 * Testing strategy:
 * - Annotation configuration checking
 * - Shared context creation
 * - Metadata storage access
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktSharedContextTest {
    @Test
    fun `GIVEN default fake annotation WHEN checking if configured THEN returns true`() =
        runTest {
            // GIVEN
            val context =
                FaktSharedContext(
                    fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                    options = FaktOptions(enabled = true),
                    metadataStorage = FirMetadataStorage(),
                )

            // WHEN
            val result = context.isConfiguredAnnotation("com.rsicarelli.fakt.Fake")

            // THEN
            assertTrue(result)
        }

    @Test
    fun `GIVEN custom annotation WHEN checking if configured THEN returns false for non-configured`() =
        runTest {
            // GIVEN
            val context =
                FaktSharedContext(
                    fakeAnnotations = listOf("com.example.CustomFake"),
                    options = FaktOptions(enabled = true),
                    metadataStorage = FirMetadataStorage(),
                )

            // WHEN
            val resultConfigured = context.isConfiguredAnnotation("com.example.CustomFake")
            val resultNotConfigured = context.isConfiguredAnnotation("com.rsicarelli.fakt.Fake")

            // THEN
            assertTrue(resultConfigured)
            assertFalse(resultNotConfigured)
        }

    @Test
    fun `GIVEN multiple annotations WHEN checking if configured THEN matches any`() =
        runTest {
            // GIVEN
            val context =
                FaktSharedContext(
                    fakeAnnotations =
                        listOf(
                            "com.rsicarelli.fakt.Fake",
                            "com.example.CustomFake",
                            "com.test.TestFake",
                        ),
                    options = FaktOptions(enabled = true),
                    metadataStorage = FirMetadataStorage(),
                )

            // WHEN & THEN
            assertTrue(context.isConfiguredAnnotation("com.rsicarelli.fakt.Fake"))
            assertTrue(context.isConfiguredAnnotation("com.example.CustomFake"))
            assertTrue(context.isConfiguredAnnotation("com.test.TestFake"))
            assertFalse(context.isConfiguredAnnotation("com.other.NotConfigured"))
        }

    @Test
    fun `GIVEN shared context WHEN accessing metadata storage THEN returns same instance`() =
        runTest {
            // GIVEN
            val storage = FirMetadataStorage()
            val context =
                FaktSharedContext(
                    fakeAnnotations = FaktSharedContext.DEFAULT_FAKE_ANNOTATIONS,
                    options = FaktOptions(enabled = true),
                    metadataStorage = storage,
                )

            // WHEN
            val retrievedStorage = context.metadataStorage

            // THEN
            assertTrue(retrievedStorage === storage) // Same instance
        }

    @Test
    fun `GIVEN shared context WHEN accessing options THEN returns configured options`() =
        runTest {
            // GIVEN
            val options =
                FaktOptions(
                    enabled = true,
                    logLevel = LogLevel.DEBUG,
                    outputDir = "/tmp/fakes",
                )
            val context =
                FaktSharedContext(
                    fakeAnnotations = FaktSharedContext.DEFAULT_FAKE_ANNOTATIONS,
                    options = options,
                    metadataStorage = FirMetadataStorage(),
                )

            // WHEN
            val retrievedOptions = context.options

            // THEN
            assertEquals(LogLevel.DEBUG, retrievedOptions.logLevel)
            assertEquals("/tmp/fakes", retrievedOptions.outputDir)
        }

    @Test
    fun `GIVEN default fake annotations constant WHEN accessed THEN contains official annotation`() =
        runTest {
            // GIVEN & WHEN
            val defaults = FaktSharedContext.DEFAULT_FAKE_ANNOTATIONS

            // THEN
            assertEquals(1, defaults.size)
            assertEquals("com.rsicarelli.fakt.Fake", defaults[0])
        }
}
