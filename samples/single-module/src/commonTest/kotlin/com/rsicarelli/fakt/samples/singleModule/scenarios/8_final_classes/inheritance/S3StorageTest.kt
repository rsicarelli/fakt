// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.inheritance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Tests for P1 Scenario: MultiLevelInheritance
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 *
 * Validates deep inheritance hierarchy (3 levels) with mixed abstract/open methods.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3StorageTest {

    @Test
    fun `GIVEN S3Storage fake WHEN calling connect THEN should use inherited abstract behavior with error default`() {
        // Given - connect() is abstract in BaseStorage (grandparent)
        val storage: S3Storage = fakeS3Storage {}

        // When/Then - Should error because it's inherited abstract
        assertFailsWith<IllegalStateException> {
            storage.connect()
        }
    }

    @Test
    fun `GIVEN S3Storage fake WHEN calling upload THEN should use inherited abstract behavior with error default`() {
        // Given - upload() is abstract in CloudStorage (parent)
        val storage: S3Storage = fakeS3Storage {}

        // When/Then - Should error because it's inherited abstract from parent
        assertFailsWith<IllegalStateException> {
            storage.upload(byteArrayOf(1, 2, 3))
        }
    }

    @Test
    fun `GIVEN S3Storage fake WHEN calling download THEN should use parent open default with super call`() {
        // Given - download() is open in CloudStorage (parent)
        val storage: S3Storage = fakeS3Storage {}

        // When
        val result = storage.download("test-id")

        // Then - Should use super.download() default (returns null)
        assertNull(result)
    }

    @Test
    fun `GIVEN S3Storage fake WHEN calling disconnect THEN should use grandparent open default`() {
        // Given - disconnect() is open in BaseStorage (grandparent)
        var disconnectCalled = false

        val storage: S3Storage = fakeS3Storage {
            disconnect {
                disconnectCalled = true
            }
        }

        // When
        storage.disconnect()

        // Then - Custom behavior executed (overriding grandparent default)
        assertTrue(disconnectCalled)
    }

    @Test
    fun `GIVEN configured all methods WHEN calling THEN should use custom behaviors from any level`() {
        // Given - Configure methods from all 3 levels
        var connectCalled = false
        var uploadResult: String? = null
        var downloadResult: ByteArray? = null
        var listBucketsResult: List<String>? = null

        val storage: S3Storage = fakeS3Storage {
            // Grandparent abstract
            connect {
                connectCalled = true
                true
            }

            // Parent abstract
            upload { data ->
                val id = "s3://bucket/${data.size}"
                uploadResult = id
                id
            }

            // Parent open
            download { id ->
                val mockData = byteArrayOf(1, 2, 3, 4, 5)
                downloadResult = mockData
                mockData
            }

            // Own open
            listBuckets {
                val buckets = listOf("bucket1", "bucket2")
                listBucketsResult = buckets
                buckets
            }
        }

        // When
        val connected = storage.connect()
        val uploaded = storage.upload(byteArrayOf(10, 20, 30))
        val downloaded = storage.download("key1")
        val buckets = storage.listBuckets()

        // Then - All custom behaviors work across 3-level hierarchy
        assertTrue(connectCalled)
        assertTrue(connected)
        assertEquals("s3://bucket/3", uploadResult)
        assertEquals("s3://bucket/3", uploaded)
        assertEquals(byteArrayOf(1, 2, 3, 4, 5).toList(), downloadResult?.toList())
        assertEquals(byteArrayOf(1, 2, 3, 4, 5).toList(), downloaded?.toList())
        assertEquals(listOf("bucket1", "bucket2"), listBucketsResult)
        assertEquals(listOf("bucket1", "bucket2"), buckets)
    }
}
