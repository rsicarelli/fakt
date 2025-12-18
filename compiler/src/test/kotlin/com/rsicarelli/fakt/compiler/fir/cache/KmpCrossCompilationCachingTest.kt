// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.cache

import com.rsicarelli.fakt.compiler.api.FirMetadataCache
import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.SerializableFakeClass
import com.rsicarelli.fakt.compiler.api.SerializableFakeInterface
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.fir.metadata.FirMetadataStorage
import com.rsicarelli.fakt.compiler.fir.metadata.FirSourceLocation
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeClass
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeInterface
import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for KMP cross-compilation caching workflow.
 *
 * These tests verify the complete producer-consumer flow:
 * 1. Producer (metadata compilation) performs FIR analysis and writes cache
 * 2. Consumer (platform compilations) reads cache and skips FIR analysis
 *
 * This simulates the real-world KMP scenario:
 * - compileCommonMainKotlinMetadata → PRODUCER (writes cache)
 * - compileKotlinJvm/IosX64/Js → CONSUMER (reads cache)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KmpCrossCompilationCachingTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var cacheFile: File
    private lateinit var sourceFile1: File
    private lateinit var sourceFile2: File
    private val testLogger = FaktLogger(messageCollector = null, logLevel = LogLevel.INFO)

    @BeforeEach
    fun setup() {
        cacheFile = File(tempDir, "fakt-cache.json")

        // Create mock source files for signature validation
        sourceFile1 = File(tempDir, "UserService.kt")
        sourceFile1.writeText("interface UserService { fun getUser(): User }")

        sourceFile2 = File(tempDir, "OrderService.kt")
        sourceFile2.writeText("interface OrderService { fun getOrder(): Order }")
    }

    // ========================================================================
    // Producer Mode Tests (Metadata Compilation)
    // ========================================================================

    @Test
    fun `GIVEN producer mode WHEN FIR analysis completes THEN writes cache file`() = runTest {
        // GIVEN: Producer mode configuration
        val producer = MetadataCacheManager(
            metadataOutputPath = cacheFile.absolutePath,
            metadataCachePath = null,
            logger = testLogger,
        )
        val storage = FirMetadataStorage()

        // Simulate FIR analysis results
        storage.storeInterface(createValidatedInterface("UserService", sourceFile1))
        storage.storeInterface(createValidatedInterface("OrderService", sourceFile2))

        // WHEN: Producer writes cache
        producer.writeCache(storage)

        // THEN: Cache file is created with correct data
        assertTrue(cacheFile.exists(), "Cache file should be created")

        val cache = MetadataCacheSerializer.deserialize(cacheFile.absolutePath)
        assertEquals(2, cache?.interfaces?.size, "Cache should contain 2 interfaces")
        val names = cache?.interfaces?.map { it.simpleName }?.toSet()
        assertTrue(names?.contains("UserService") == true, "Cache should contain UserService")
        assertTrue(names?.contains("OrderService") == true, "Cache should contain OrderService")
    }

    @Test
    fun `GIVEN producer mode with classes WHEN FIR analysis completes THEN writes both interfaces and classes`() = runTest {
        // GIVEN: Producer mode with mixed types
        val producer = MetadataCacheManager(
            metadataOutputPath = cacheFile.absolutePath,
            metadataCachePath = null,
            logger = testLogger,
        )
        val storage = FirMetadataStorage()

        // Simulate FIR analysis results with both interfaces and classes
        storage.storeInterface(createValidatedInterface("UserService", sourceFile1))
        storage.storeClass(createValidatedClass("AbstractRepository", sourceFile2))

        // WHEN: Producer writes cache
        producer.writeCache(storage)

        // THEN: Cache contains both interfaces and classes
        val cache = MetadataCacheSerializer.deserialize(cacheFile.absolutePath)
        assertEquals(1, cache?.interfaces?.size)
        assertEquals(1, cache?.classes?.size)
        assertEquals("UserService", cache?.interfaces?.first()?.simpleName)
        assertEquals("AbstractRepository", cache?.classes?.first()?.simpleName)
    }

    // ========================================================================
    // Consumer Mode Tests (Platform Compilations)
    // ========================================================================

    @Test
    fun `GIVEN valid cache WHEN consumer loads THEN storage is populated`() = runTest {
        // GIVEN: Producer has written cache
        setupCacheWithInterfaces("UserService", "OrderService")

        val consumer = MetadataCacheManager(
            metadataOutputPath = null,
            metadataCachePath = cacheFile.absolutePath,
            logger = testLogger,
        )
        val storage = FirMetadataStorage()

        // WHEN: Consumer loads cache
        val loaded = consumer.tryLoadCache(storage)

        // THEN: Storage is populated from cache
        assertTrue(loaded, "Cache should load successfully")
        assertEquals(2, storage.totalCount(), "Storage should have 2 items")

        val interfaces = storage.getAllInterfaces()
        assertEquals(2, interfaces.size)
        assertTrue(interfaces.any { it.simpleName == "UserService" })
        assertTrue(interfaces.any { it.simpleName == "OrderService" })
    }

    @Test
    fun `GIVEN consumer mode WHEN cache loaded THEN subsequent calls return true without reloading`() = runTest {
        // GIVEN: Valid cache exists
        setupCacheWithInterfaces("UserService")

        val consumer = MetadataCacheManager(
            metadataOutputPath = null,
            metadataCachePath = cacheFile.absolutePath,
            logger = testLogger,
        )
        val storage = FirMetadataStorage()

        // WHEN: Load multiple times
        val firstLoad = consumer.tryLoadCache(storage)
        val secondLoad = consumer.tryLoadCache(storage)
        val thirdLoad = consumer.tryLoadCache(storage)

        // THEN: All return true, storage has only 1 interface (not duplicated)
        assertTrue(firstLoad)
        assertTrue(secondLoad)
        assertTrue(thirdLoad)
        assertEquals(1, storage.totalCount(), "Should not duplicate entries on multiple loads")
    }

    // ========================================================================
    // Cache Invalidation Tests
    // ========================================================================

    @Test
    fun `GIVEN source file modified WHEN consumer loads THEN cache is invalidated`() = runTest {
        // GIVEN: Cache with signature for original source
        setupCacheWithInterfaces("UserService")

        // Modify source file AFTER cache was created
        sourceFile1.writeText("interface UserService { fun getUser(): User; fun newMethod(): String }")

        val consumer = MetadataCacheManager(
            metadataOutputPath = null,
            metadataCachePath = cacheFile.absolutePath,
            logger = testLogger,
        )
        val storage = FirMetadataStorage()

        // WHEN: Consumer tries to load cache
        val loaded = consumer.tryLoadCache(storage)

        // THEN: Cache is invalid (signature mismatch)
        assertFalse(loaded, "Cache should be invalid due to source change")
        assertTrue(storage.isEmpty(), "Storage should be empty when cache invalid")
    }

    @Test
    fun `GIVEN source file deleted WHEN consumer loads THEN cache is invalidated`() = runTest {
        // GIVEN: Cache with signature for source file
        setupCacheWithInterfaces("UserService")

        // Delete source file
        sourceFile1.delete()

        val consumer = MetadataCacheManager(
            metadataOutputPath = null,
            metadataCachePath = cacheFile.absolutePath,
            logger = testLogger,
        )
        val storage = FirMetadataStorage()

        // WHEN: Consumer tries to load cache
        val loaded = consumer.tryLoadCache(storage)

        // THEN: Cache is invalid (source file missing)
        assertFalse(loaded)
    }

    @Test
    fun `GIVEN cache version mismatch WHEN consumer loads THEN cache is invalidated`() = runTest {
        // GIVEN: Cache with old version
        val oldVersionCache = FirMetadataCache(
            version = 9999, // Future version
            cacheSignature = "test",
            interfaces = listOf(createSerializableInterface("UserService", sourceFile1)),
            classes = emptyList(),
        )
        MetadataCacheSerializer.serialize(oldVersionCache, cacheFile.absolutePath)

        val consumer = MetadataCacheManager(
            metadataOutputPath = null,
            metadataCachePath = cacheFile.absolutePath,
            logger = testLogger,
        )
        val storage = FirMetadataStorage()

        // WHEN: Consumer tries to load cache
        val loaded = consumer.tryLoadCache(storage)

        // THEN: Cache is invalid (version mismatch)
        assertFalse(loaded)
    }

    // ========================================================================
    // Full Producer-Consumer Workflow Test
    // ========================================================================

    @Test
    fun `GIVEN KMP project WHEN full producer-consumer workflow THEN platforms skip FIR analysis`() = runTest {
        // PHASE 1: PRODUCER (metadata compilation)
        val producer = MetadataCacheManager(
            metadataOutputPath = cacheFile.absolutePath,
            metadataCachePath = null,
            logger = testLogger,
        )
        val producerStorage = FirMetadataStorage()

        // Simulate FIR analysis
        producerStorage.storeInterface(createValidatedInterface("UserService", sourceFile1))
        producerStorage.storeInterface(createValidatedInterface("OrderService", sourceFile2))

        assertTrue(producer.isProducerMode, "Should be producer mode")
        assertFalse(producer.isConsumerMode, "Should not be consumer mode")

        // Write cache
        producer.writeCache(producerStorage)
        assertTrue(cacheFile.exists(), "Cache should exist after producer write")

        // PHASE 2: CONSUMER 1 (JVM compilation)
        val jvmConsumer = MetadataCacheManager(
            metadataOutputPath = null,
            metadataCachePath = cacheFile.absolutePath,
            logger = testLogger,
        )
        val jvmStorage = FirMetadataStorage()

        assertFalse(jvmConsumer.isProducerMode)
        assertTrue(jvmConsumer.isConsumerMode)

        val jvmLoaded = jvmConsumer.tryLoadCache(jvmStorage)
        assertTrue(jvmLoaded, "JVM consumer should load cache")
        assertEquals(2, jvmStorage.totalCount())

        // PHASE 3: CONSUMER 2 (iOS compilation) - should also work
        val iosConsumer = MetadataCacheManager(
            metadataOutputPath = null,
            metadataCachePath = cacheFile.absolutePath,
            logger = testLogger,
        )
        val iosStorage = FirMetadataStorage()

        val iosLoaded = iosConsumer.tryLoadCache(iosStorage)
        assertTrue(iosLoaded, "iOS consumer should load cache")
        assertEquals(2, iosStorage.totalCount())

        // Verify data integrity across consumers
        assertEquals(
            jvmStorage.getAllInterfaces().map { it.simpleName }.sorted(),
            iosStorage.getAllInterfaces().map { it.simpleName }.sorted(),
            "Both platforms should have identical metadata",
        )
    }

    @Test
    fun `GIVEN cache miss WHEN rebuild THEN producer creates new cache`() = runTest {
        // PHASE 1: Initial build (producer writes cache)
        val producer1 = MetadataCacheManager(
            metadataOutputPath = cacheFile.absolutePath,
            metadataCachePath = null,
            logger = testLogger,
        )
        val storage1 = FirMetadataStorage()
        storage1.storeInterface(createValidatedInterface("UserService", sourceFile1))
        producer1.writeCache(storage1)

        // PHASE 2: Source file changes
        sourceFile1.writeText("interface UserService { fun getUser(): User; fun newMethod(): String }")

        // PHASE 3: Consumer tries to load (cache miss due to signature change)
        val consumer = MetadataCacheManager(
            metadataOutputPath = null,
            metadataCachePath = cacheFile.absolutePath,
            logger = testLogger,
        )
        val consumerStorage = FirMetadataStorage()
        val loaded = consumer.tryLoadCache(consumerStorage)
        assertFalse(loaded, "Cache should miss due to source change")

        // PHASE 4: Producer rebuilds with new cache
        val producer2 = MetadataCacheManager(
            metadataOutputPath = cacheFile.absolutePath,
            metadataCachePath = null,
            logger = testLogger,
        )
        val storage2 = FirMetadataStorage()
        storage2.storeInterface(createValidatedInterface("UserService", sourceFile1))
        producer2.writeCache(storage2)

        // PHASE 5: Consumer loads new cache
        val consumer2 = MetadataCacheManager(
            metadataOutputPath = null,
            metadataCachePath = cacheFile.absolutePath,
            logger = testLogger,
        )
        val storage3 = FirMetadataStorage()
        val loadedNew = consumer2.tryLoadCache(storage3)
        assertTrue(loadedNew, "Cache should load after rebuild")
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private fun createValidatedInterface(name: String, sourceFile: File): ValidatedFakeInterface {
        val classId = ClassId.topLevel(FqName("com.example.$name"))
        return ValidatedFakeInterface(
            classId = classId,
            simpleName = name,
            packageName = "com.example",
            typeParameters = emptyList(),
            properties = emptyList(),
            functions = emptyList(),
            inheritedProperties = emptyList(),
            inheritedFunctions = emptyList(),
            sourceLocation = FirSourceLocation(
                filePath = sourceFile.absolutePath,
                startLine = 1,
                startColumn = 0,
                endLine = 1,
                endColumn = sourceFile.readText().length,
            ),
            validationTimeNanos = 100_000L,
        )
    }

    private fun createValidatedClass(name: String, sourceFile: File): ValidatedFakeClass {
        val classId = ClassId.topLevel(FqName("com.example.$name"))
        return ValidatedFakeClass(
            classId = classId,
            simpleName = name,
            packageName = "com.example",
            typeParameters = emptyList(),
            abstractProperties = emptyList(),
            openProperties = emptyList(),
            abstractMethods = emptyList(),
            openMethods = emptyList(),
            sourceLocation = FirSourceLocation(
                filePath = sourceFile.absolutePath,
                startLine = 1,
                startColumn = 0,
                endLine = 1,
                endColumn = sourceFile.readText().length,
            ),
            validationTimeNanos = 100_000L,
        )
    }

    private fun createSerializableInterface(name: String, sourceFile: File): SerializableFakeInterface {
        val signature = MetadataCacheSerializer.computeFileSignature(sourceFile.absolutePath)
        return SerializableFakeInterface(
            classIdString = "com/example/$name",
            simpleName = name,
            packageName = "com.example",
            typeParameters = emptyList(),
            properties = emptyList(),
            functions = emptyList(),
            inheritedProperties = emptyList(),
            inheritedFunctions = emptyList(),
            sourceFilePath = sourceFile.absolutePath,
            sourceFileSignature = signature,
            validationTimeNanos = 100_000L,
        )
    }

    private fun setupCacheWithInterfaces(vararg names: String) {
        val interfaces = names.mapIndexed { index, name ->
            val sourceFile = if (index == 0) sourceFile1 else sourceFile2
            createSerializableInterface(name, sourceFile)
        }
        val signatures = interfaces.map { it.sourceFileSignature }
        val cache = FirMetadataCache(
            cacheSignature = MetadataCacheSerializer.computeCombinedSignature(signatures),
            interfaces = interfaces,
            classes = emptyList(),
        )
        MetadataCacheSerializer.serialize(cache, cacheFile.absolutePath)
    }
}
