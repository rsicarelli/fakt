// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.inheritance

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: MultiLevelInheritance (3-level hierarchy)
 *
 * **Pattern**: GrandParent → Parent → Child inheritance chain
 * **Priority**: P1 (High - Deep inheritance is common in real codebases)
 *
 * **What it tests**:
 * - Three-level inheritance (BaseStorage → CloudStorage → S3Storage)
 * - Abstract methods inherited from grandparent
 * - Abstract methods inherited from parent
 * - Open methods inherited from parent
 * - Own open methods in child class
 * - Correct default behavior attribution across hierarchy
 *
 * **Expected behavior**:
 * ```kotlin
 * class FakeS3StorageImpl : S3Storage() {
 *     // From grandparent (BaseStorage) - abstract
 *     private var connectBehavior: () -> Boolean = { _ -> error("Configure connect behavior") }
 *
 *     // From parent (CloudStorage) - abstract
 *     private var uploadBehavior: (ByteArray) -> String = { _ -> error("Configure upload behavior") }
 *
 *     // From parent (CloudStorage) - open
 *     private var downloadBehavior: (String) -> ByteArray? = { id -> super.download(id) }
 *
 *     // From grandparent (BaseStorage) - open
 *     private var disconnectBehavior: () -> Unit = { super.disconnect() }
 *
 *     // Own method - open
 *     private var listBucketsBehavior: () -> List<String> = { super.listBuckets() }
 * }
 * ```
 *
 * **Real-world equivalent**:
 * ```kotlin
 * // Common pattern: Storage abstraction hierarchy
 * abstract class BaseStorage { abstract fun connect() }
 * abstract class CloudStorage : BaseStorage() { abstract fun upload() }
 * class S3Storage : CloudStorage() { ... }
 *
 * // Testing:
 * val storage: S3Storage = fakeS3Storage {
 *     connect { true }  // Must configure (inherited abstract)
 *     upload { data -> "s3://bucket/key" }  // Must configure (inherited abstract)
 *     download { id -> mockData }  // Optional (inherited open)
 *     listBuckets { listOf("bucket1") }  // Optional (own open)
 * }
 * ```
 */

// Level 1: GrandParent - Base abstract class
abstract class BaseStorage {
    /**
     * Connects to storage - abstract, must be implemented.
     */
    abstract fun connect(): Boolean

    /**
     * Disconnects from storage - has default implementation.
     */
    open fun disconnect() {
        // Default disconnect logic
    }
}

// Level 2: Parent - Extends BaseStorage, adds cloud-specific methods
abstract class CloudStorage : BaseStorage() {
    /**
     * Inherits abstract connect() from BaseStorage.
     * Must be overridden but still abstract at this level.
     */
    abstract override fun connect(): Boolean

    /**
     * Upload data to cloud - abstract, no default implementation.
     */
    abstract fun upload(data: ByteArray): String

    /**
     * Download data from cloud - has default implementation.
     */
    open fun download(id: String): ByteArray? = null
}

// Level 3: Child - Concrete implementation for S3
@Fake
open class S3Storage : CloudStorage() {
    /**
     * S3-specific connection implementation.
     */
    override fun connect(): Boolean = true

    /**
     * S3-specific upload implementation.
     */
    override fun upload(data: ByteArray): String = "upload-id"

    /**
     * Inherits default download from CloudStorage.
     */
    override fun download(id: String): ByteArray? = super.download(id)

    /**
     * S3-specific method - list S3 buckets.
     */
    open fun listBuckets(): List<String> = emptyList()
}
