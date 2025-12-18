// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.cache

import com.rsicarelli.fakt.compiler.api.FirMetadataCache
import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.SerializableFakeInterface
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.fir.metadata.FirMetadataStorage
import com.rsicarelli.fakt.compiler.fir.metadata.FirSourceLocation
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
 * BDD tests for MetadataCacheManager.
 *
 * Tests verify the cache management logic for cross-compilation caching.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetadataCacheManagerTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var cacheFile: File
    private lateinit var sourceFile: File
    private val testLogger = FaktLogger(messageCollector = null, logLevel = LogLevel.INFO)

    @BeforeEach
    fun setup() {
        cacheFile = File(tempDir, "fakt-cache.json")
        sourceFile = File(tempDir, "TestInterface.kt")
        sourceFile.writeText("interface TestInterface { fun test(): String }")
    }

    // ========================================================================
    // Mode Detection Tests
    // ========================================================================

    @Test
    fun `GIVEN metadataOutputPath set WHEN isProducerMode THEN returns true`() =
        runTest {
            // GIVEN
            val manager =
                MetadataCacheManager(
                    metadataOutputPath = cacheFile.absolutePath,
                    metadataCachePath = null,
                    logger = testLogger,
                )

            // WHEN & THEN
            assertTrue(manager.isProducerMode, "Should be producer mode when output path set")
            assertFalse(manager.isConsumerMode, "Should not be consumer mode")
        }

    @Test
    fun `GIVEN metadataCachePath set WHEN isConsumerMode THEN returns true`() =
        runTest {
            // GIVEN
            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = cacheFile.absolutePath,
                    logger = testLogger,
                )

            // WHEN & THEN
            assertTrue(manager.isConsumerMode, "Should be consumer mode when cache path set")
            assertFalse(manager.isProducerMode, "Should not be producer mode")
        }

    @Test
    fun `GIVEN neither path set WHEN checking modes THEN both are false`() =
        runTest {
            // GIVEN
            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = null,
                    logger = testLogger,
                )

            // WHEN & THEN
            assertFalse(manager.isProducerMode, "Should not be producer mode")
            assertFalse(manager.isConsumerMode, "Should not be consumer mode")
        }

    // ========================================================================
    // Cache Loading Tests
    // ========================================================================

    @Test
    fun `GIVEN valid cache file WHEN tryLoadCache THEN populates storage`() =
        runTest {
            // GIVEN: Create cache file with one interface
            val sourceSignature = MetadataCacheSerializer.computeFileSignature(sourceFile.absolutePath)
            val cache =
                FirMetadataCache(
                    cacheSignature = "test-sig",
                    interfaces =
                        listOf(
                            SerializableFakeInterface(
                                classIdString = "com/example/TestInterface",
                                simpleName = "TestInterface",
                                packageName = "com.example",
                                typeParameters = emptyList(),
                                properties = emptyList(),
                                functions = emptyList(),
                                inheritedProperties = emptyList(),
                                inheritedFunctions = emptyList(),
                                sourceFilePath = sourceFile.absolutePath,
                                sourceFileSignature = sourceSignature,
                                validationTimeNanos = 0L,
                            ),
                        ),
                    classes = emptyList(),
                )
            MetadataCacheSerializer.serialize(cache, cacheFile.absolutePath)

            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = cacheFile.absolutePath,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // WHEN
            val loaded = manager.tryLoadCache(storage)

            // THEN
            assertTrue(loaded, "Should successfully load cache")
            assertEquals(1, storage.totalCount(), "Storage should have 1 interface")
            val interfaces = storage.getAllInterfaces()
            assertEquals(1, interfaces.size)
            assertEquals("TestInterface", interfaces.first().simpleName)
        }

    @Test
    fun `GIVEN missing cache file WHEN tryLoadCache THEN returns false`() =
        runTest {
            // GIVEN
            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = "/nonexistent/path/cache.json",
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // WHEN
            val loaded = manager.tryLoadCache(storage)

            // THEN
            assertFalse(loaded, "Should return false for missing cache")
            assertTrue(storage.isEmpty(), "Storage should be empty")
        }

    @Test
    fun `GIVEN corrupt cache file WHEN tryLoadCache THEN returns false`() =
        runTest {
            // GIVEN
            cacheFile.writeText("{ invalid json }")
            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = cacheFile.absolutePath,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // WHEN
            val loaded = manager.tryLoadCache(storage)

            // THEN
            assertFalse(loaded, "Should return false for corrupt cache")
            assertTrue(storage.isEmpty(), "Storage should be empty")
        }

    @Test
    fun `GIVEN not consumer mode WHEN tryLoadCache THEN returns false`() =
        runTest {
            // GIVEN: Producer mode (no cache path)
            val manager =
                MetadataCacheManager(
                    metadataOutputPath = cacheFile.absolutePath,
                    metadataCachePath = null,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // WHEN
            val loaded = manager.tryLoadCache(storage)

            // THEN
            assertFalse(loaded, "Should return false when not in consumer mode")
        }

    @Test
    fun `GIVEN cache already loaded WHEN tryLoadCache again THEN returns true without reloading`() =
        runTest {
            // GIVEN
            val sourceSignature = MetadataCacheSerializer.computeFileSignature(sourceFile.absolutePath)
            val cache =
                FirMetadataCache(
                    cacheSignature = "test-sig",
                    interfaces =
                        listOf(
                            SerializableFakeInterface(
                                classIdString = "com/example/Test",
                                simpleName = "Test",
                                packageName = "com.example",
                                typeParameters = emptyList(),
                                properties = emptyList(),
                                functions = emptyList(),
                                inheritedProperties = emptyList(),
                                inheritedFunctions = emptyList(),
                                sourceFilePath = sourceFile.absolutePath,
                                sourceFileSignature = sourceSignature,
                                validationTimeNanos = 0L,
                            ),
                        ),
                    classes = emptyList(),
                )
            MetadataCacheSerializer.serialize(cache, cacheFile.absolutePath)

            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = cacheFile.absolutePath,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // WHEN: Load twice
            val firstLoad = manager.tryLoadCache(storage)
            val secondLoad = manager.tryLoadCache(storage)

            // THEN
            assertTrue(firstLoad, "First load should succeed")
            assertTrue(secondLoad, "Second load should return true (cached)")
            assertEquals(1, storage.totalCount(), "Should still have 1 interface (no duplicate)")
        }

    // ========================================================================
    // Cache Invalidation Tests
    // ========================================================================

    @Test
    fun `GIVEN changed source file WHEN tryLoadCache THEN invalidates cache`() =
        runTest {
            // GIVEN: Cache with old signature
            val cache =
                FirMetadataCache(
                    cacheSignature = "old-sig",
                    interfaces =
                        listOf(
                            SerializableFakeInterface(
                                classIdString = "com/example/Test",
                                simpleName = "Test",
                                packageName = "com.example",
                                typeParameters = emptyList(),
                                properties = emptyList(),
                                functions = emptyList(),
                                inheritedProperties = emptyList(),
                                inheritedFunctions = emptyList(),
                                sourceFilePath = sourceFile.absolutePath,
                                sourceFileSignature = "old-file-signature-that-doesnt-match",
                                validationTimeNanos = 0L,
                            ),
                        ),
                    classes = emptyList(),
                )
            MetadataCacheSerializer.serialize(cache, cacheFile.absolutePath)

            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = cacheFile.absolutePath,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // WHEN
            val loaded = manager.tryLoadCache(storage)

            // THEN
            assertFalse(loaded, "Should return false when source file changed")
            assertTrue(storage.isEmpty(), "Storage should be empty")
        }

    @Test
    fun `GIVEN source file deleted WHEN tryLoadCache THEN invalidates cache`() =
        runTest {
            // GIVEN: Cache pointing to non-existent source
            val cache =
                FirMetadataCache(
                    cacheSignature = "test-sig",
                    interfaces =
                        listOf(
                            SerializableFakeInterface(
                                classIdString = "com/example/Deleted",
                                simpleName = "Deleted",
                                packageName = "com.example",
                                typeParameters = emptyList(),
                                properties = emptyList(),
                                functions = emptyList(),
                                inheritedProperties = emptyList(),
                                inheritedFunctions = emptyList(),
                                sourceFilePath = "/nonexistent/Deleted.kt",
                                sourceFileSignature = "some-sig",
                                validationTimeNanos = 0L,
                            ),
                        ),
                    classes = emptyList(),
                )
            MetadataCacheSerializer.serialize(cache, cacheFile.absolutePath)

            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = cacheFile.absolutePath,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // WHEN
            val loaded = manager.tryLoadCache(storage)

            // THEN
            assertFalse(loaded, "Should return false when source file missing")
        }

    @Test
    fun `GIVEN cache version mismatch WHEN tryLoadCache THEN invalidates cache`() =
        runTest {
            // GIVEN: Cache with old version
            val sourceSignature = MetadataCacheSerializer.computeFileSignature(sourceFile.absolutePath)
            val cache =
                FirMetadataCache(
                    version = 999, // Future version
                    cacheSignature = "test-sig",
                    interfaces =
                        listOf(
                            SerializableFakeInterface(
                                classIdString = "com/example/Test",
                                simpleName = "Test",
                                packageName = "com.example",
                                typeParameters = emptyList(),
                                properties = emptyList(),
                                functions = emptyList(),
                                inheritedProperties = emptyList(),
                                inheritedFunctions = emptyList(),
                                sourceFilePath = sourceFile.absolutePath,
                                sourceFileSignature = sourceSignature,
                                validationTimeNanos = 0L,
                            ),
                        ),
                    classes = emptyList(),
                )
            MetadataCacheSerializer.serialize(cache, cacheFile.absolutePath)

            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = cacheFile.absolutePath,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // WHEN
            val loaded = manager.tryLoadCache(storage)

            // THEN
            assertFalse(loaded, "Should return false for version mismatch")
        }

    @Test
    fun `GIVEN all signatures match WHEN tryLoadCache THEN cache is valid`() =
        runTest {
            // GIVEN: Valid cache with correct signatures
            val sourceSignature = MetadataCacheSerializer.computeFileSignature(sourceFile.absolutePath)
            val cache =
                FirMetadataCache(
                    version = FirMetadataCache.CURRENT_VERSION,
                    cacheSignature = "valid-sig",
                    interfaces =
                        listOf(
                            SerializableFakeInterface(
                                classIdString = "com/example/Valid",
                                simpleName = "Valid",
                                packageName = "com.example",
                                typeParameters = emptyList(),
                                properties = emptyList(),
                                functions = emptyList(),
                                inheritedProperties = emptyList(),
                                inheritedFunctions = emptyList(),
                                sourceFilePath = sourceFile.absolutePath,
                                sourceFileSignature = sourceSignature,
                                validationTimeNanos = 0L,
                            ),
                        ),
                    classes = emptyList(),
                )
            MetadataCacheSerializer.serialize(cache, cacheFile.absolutePath)

            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = cacheFile.absolutePath,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // WHEN
            val loaded = manager.tryLoadCache(storage)

            // THEN
            assertTrue(loaded, "Should load valid cache")
            assertEquals(1, storage.totalCount())
        }

    // ========================================================================
    // Cache Writing Tests
    // ========================================================================

    @Test
    fun `GIVEN storage with interfaces WHEN writeCache THEN serializes to file`() =
        runTest {
            // GIVEN
            val manager =
                MetadataCacheManager(
                    metadataOutputPath = cacheFile.absolutePath,
                    metadataCachePath = null,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // Add an interface to storage
            val classId = ClassId.topLevel(FqName("com.example.WriteTest"))
            storage.storeInterface(
                ValidatedFakeInterface(
                    classId = classId,
                    simpleName = "WriteTest",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceLocation = FirSourceLocation(sourceFile.absolutePath, 1, 0, 10, 1),
                    validationTimeNanos = 12345L,
                ),
            )

            // WHEN
            manager.writeCache(storage)

            // THEN
            assertTrue(cacheFile.exists(), "Cache file should be created")
            val loadedCache = MetadataCacheSerializer.deserialize(cacheFile.absolutePath)
            assertEquals(1, loadedCache?.interfaces?.size)
            assertEquals("WriteTest", loadedCache?.interfaces?.first()?.simpleName)
        }

    @Test
    fun `GIVEN not producer mode WHEN writeCache THEN does nothing`() =
        runTest {
            // GIVEN: Consumer mode
            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = cacheFile.absolutePath,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()
            val classId = ClassId.topLevel(FqName("com.example.Test"))
            storage.storeInterface(
                ValidatedFakeInterface(
                    classId = classId,
                    simpleName = "Test",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                ),
            )

            // WHEN
            manager.writeCache(storage)

            // THEN
            assertFalse(cacheFile.exists(), "Should not write cache in consumer mode")
        }

    @Test
    fun `GIVEN empty storage WHEN writeCache THEN does not create file`() =
        runTest {
            // GIVEN
            val manager =
                MetadataCacheManager(
                    metadataOutputPath = cacheFile.absolutePath,
                    metadataCachePath = null,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // WHEN
            manager.writeCache(storage)

            // THEN
            assertFalse(cacheFile.exists(), "Should not create cache for empty storage")
        }

    // ========================================================================
    // Multiple Interfaces Tests
    // ========================================================================

    @Test
    fun `GIVEN cache with multiple interfaces WHEN tryLoadCache THEN loads all`() =
        runTest {
            // GIVEN
            val sourceSignature = MetadataCacheSerializer.computeFileSignature(sourceFile.absolutePath)
            val cache =
                FirMetadataCache(
                    cacheSignature = "multi-sig",
                    interfaces =
                        listOf(
                            SerializableFakeInterface(
                                classIdString = "com/example/ServiceA",
                                simpleName = "ServiceA",
                                packageName = "com.example",
                                typeParameters = emptyList(),
                                properties = emptyList(),
                                functions = emptyList(),
                                inheritedProperties = emptyList(),
                                inheritedFunctions = emptyList(),
                                sourceFilePath = sourceFile.absolutePath,
                                sourceFileSignature = sourceSignature,
                                validationTimeNanos = 100L,
                            ),
                            SerializableFakeInterface(
                                classIdString = "com/example/ServiceB",
                                simpleName = "ServiceB",
                                packageName = "com.example",
                                typeParameters = emptyList(),
                                properties = emptyList(),
                                functions = emptyList(),
                                inheritedProperties = emptyList(),
                                inheritedFunctions = emptyList(),
                                sourceFilePath = sourceFile.absolutePath,
                                sourceFileSignature = sourceSignature,
                                validationTimeNanos = 200L,
                            ),
                        ),
                    classes = emptyList(),
                )
            MetadataCacheSerializer.serialize(cache, cacheFile.absolutePath)

            val manager =
                MetadataCacheManager(
                    metadataOutputPath = null,
                    metadataCachePath = cacheFile.absolutePath,
                    logger = testLogger,
                )
            val storage = FirMetadataStorage()

            // WHEN
            val loaded = manager.tryLoadCache(storage)

            // THEN
            assertTrue(loaded)
            assertEquals(2, storage.totalCount())
            val names = storage.getAllInterfaces().map { it.simpleName }.toSet()
            assertTrue(names.contains("ServiceA"))
            assertTrue(names.contains("ServiceB"))
        }
}
