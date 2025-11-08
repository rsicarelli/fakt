// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.inheritance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class S3StorageTest {
    @Test
    fun `GIVEN unconfigured connect WHEN called THEN delegates to super implementation`() {
        val storage: S3Storage = fakeS3Storage {}

        val result = storage.connect()

        // S3Storage.connect() returns true in actual implementation (line 98)
        assertTrue(result)
    }

    @Test
    fun `GIVEN configured connect WHEN called THEN uses custom behavior`() {
        var called = false

        val storage: S3Storage =
            fakeS3Storage {
                connect {
                    called = true
                    false
                }
            }

        val result = storage.connect()

        assertTrue(called)
        assertEquals(false, result)
    }

    @Test
    fun `GIVEN unconfigured upload WHEN called THEN delegates to super implementation`() {
        val storage: S3Storage = fakeS3Storage {}

        val result = storage.upload(byteArrayOf(1, 2, 3))

        // S3Storage.upload() returns "upload-id" in actual implementation (line 103)
        assertEquals("upload-id", result)
    }

    @Test
    fun `GIVEN configured upload WHEN called THEN uses custom behavior`() {
        val storage: S3Storage =
            fakeS3Storage {
                upload { data -> "s3://bucket/${data.size}" }
            }

        val result = storage.upload(byteArrayOf(1, 2, 3))

        assertEquals("s3://bucket/3", result)
    }

    @Test
    fun `GIVEN unconfigured download WHEN called THEN uses super implementation`() {
        val storage: S3Storage = fakeS3Storage {}

        val result = storage.download("key1")

        assertNull(result)
    }

    @Test
    fun `GIVEN configured download WHEN called THEN uses custom behavior`() {
        val mockData = byteArrayOf(10, 20, 30)

        val storage: S3Storage =
            fakeS3Storage {
                download { mockData }
            }

        val result = storage.download("key1")

        assertEquals(mockData.toList(), result?.toList())
    }

    @Test
    fun `GIVEN unconfigured listBuckets WHEN called THEN uses super implementation`() {
        val storage: S3Storage = fakeS3Storage {}

        val result = storage.listBuckets()

        assertEquals(emptyList(), result)
    }

    @Test
    fun `GIVEN configured listBuckets WHEN called THEN uses custom behavior`() {
        val storage: S3Storage =
            fakeS3Storage {
                listBuckets { listOf("bucket1", "bucket2") }
            }

        val result = storage.listBuckets()

        assertEquals(listOf("bucket1", "bucket2"), result)
    }

    @Test
    fun `GIVEN all methods configured WHEN called THEN all use custom behaviors`() {
        var connectCalled = false
        val uploadedData = mutableListOf<Int>()

        val storage: S3Storage =
            fakeS3Storage {
                connect {
                    connectCalled = true
                    true
                }
                upload { data ->
                    uploadedData.add(data.size)
                    "custom-id"
                }
                download { byteArrayOf(1, 2) }
                listBuckets { listOf("b1") }
            }

        val connected = storage.connect()
        val uploaded = storage.upload(byteArrayOf(10, 20, 30))
        val downloaded = storage.download("key")
        val buckets = storage.listBuckets()

        assertTrue(connectCalled)
        assertTrue(connected)
        assertEquals(listOf(3), uploadedData)
        assertEquals("custom-id", uploaded)
        assertEquals(byteArrayOf(1, 2).toList(), downloaded?.toList())
        assertEquals(listOf("b1"), buckets)
    }
}
