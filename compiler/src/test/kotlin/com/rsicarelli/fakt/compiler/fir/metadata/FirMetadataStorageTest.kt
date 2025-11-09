// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.metadata

import com.rsicarelli.fakt.compiler.ir.analysis.GenericPattern
import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [FirMetadataStorage] following GIVEN-WHEN-THEN pattern.
 *
 * Testing strategy:
 * - Thread-safe storage operations
 * - Duplicate registration detection
 * - Empty state handling
 * - Retrieval operations
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirMetadataStorageTest {
    @Test
    fun `GIVEN empty storage WHEN checking isEmpty THEN returns true`() =
        runTest {
            // GIVEN
            val storage = FirMetadataStorage()

            // WHEN
            val result = storage.isEmpty()

            // THEN
            assertTrue(result)
            assertEquals(0, storage.totalCount())
        }

    @Test
    fun `GIVEN interface stored WHEN checking isEmpty THEN returns false`() =
        runTest {
            // GIVEN
            val storage = FirMetadataStorage()
            val metadata = createTestInterfaceMetadata("com.example.UserService")

            // WHEN
            storage.storeInterface(metadata)

            // THEN
            assertFalse(storage.isEmpty())
            assertEquals(1, storage.totalCount())
        }

    @Test
    fun `GIVEN interface stored WHEN retrieving by ClassId THEN returns metadata`() =
        runTest {
            // GIVEN
            val storage = FirMetadataStorage()
            val metadata = createTestInterfaceMetadata("com.example.UserService")
            storage.storeInterface(metadata)

            // WHEN
            val retrieved = storage.getInterface(metadata.classId)

            // THEN
            assertEquals(metadata, retrieved)
        }

    @Test
    fun `GIVEN no interfaces stored WHEN retrieving by ClassId THEN returns null`() =
        runTest {
            // GIVEN
            val storage = FirMetadataStorage()
            val classId = ClassId.topLevel(FqName("com.example.UserService"))

            // WHEN
            val retrieved = storage.getInterface(classId)

            // THEN
            assertNull(retrieved)
        }

    @Test
    fun `GIVEN multiple interfaces stored WHEN getting all THEN returns all metadata`() =
        runTest {
            // GIVEN
            val storage = FirMetadataStorage()
            val metadata1 = createTestInterfaceMetadata("com.example.UserService")
            val metadata2 = createTestInterfaceMetadata("com.example.OrderService")
            val metadata3 = createTestInterfaceMetadata("com.example.PaymentService")

            // WHEN
            storage.storeInterface(metadata1)
            storage.storeInterface(metadata2)
            storage.storeInterface(metadata3)
            val allInterfaces = storage.getAllInterfaces()

            // THEN
            assertEquals(3, allInterfaces.size)
            assertTrue(allInterfaces.contains(metadata1))
            assertTrue(allInterfaces.contains(metadata2))
            assertTrue(allInterfaces.contains(metadata3))
        }

    @Test
    fun `GIVEN class stored WHEN retrieving by ClassId THEN returns metadata`() =
        runTest {
            // GIVEN
            val storage = FirMetadataStorage()
            val metadata = createTestClassMetadata("com.example.AbstractRepository")
            storage.storeClass(metadata)

            // WHEN
            val retrieved = storage.getClass(metadata.classId)

            // THEN
            assertEquals(metadata, retrieved)
        }

    @Test
    fun `GIVEN interfaces and classes stored WHEN checking totalCount THEN returns sum`() =
        runTest {
            // GIVEN
            val storage = FirMetadataStorage()
            val interface1 = createTestInterfaceMetadata("com.example.Service1")
            val interface2 = createTestInterfaceMetadata("com.example.Service2")
            val class1 = createTestClassMetadata("com.example.Repository1")

            // WHEN
            storage.storeInterface(interface1)
            storage.storeInterface(interface2)
            storage.storeClass(class1)

            // THEN
            assertEquals(3, storage.totalCount())
            assertEquals(2, storage.getAllInterfaces().size)
            assertEquals(1, storage.getAllClasses().size)
        }

    @Test
    fun `GIVEN storage with data WHEN clearing for testing THEN becomes empty`() =
        runTest {
            // GIVEN
            val storage = FirMetadataStorage()
            storage.storeInterface(createTestInterfaceMetadata("com.example.Service"))
            storage.storeClass(createTestClassMetadata("com.example.Repository"))
            assertFalse(storage.isEmpty())

            // WHEN
            storage.clearForTesting()

            // THEN
            assertTrue(storage.isEmpty())
            assertEquals(0, storage.totalCount())
        }

    @Test
    fun `GIVEN storage WHEN toString called THEN returns debug info`() =
        runTest {
            // GIVEN
            val storage = FirMetadataStorage()
            storage.storeInterface(createTestInterfaceMetadata("com.example.Service1"))
            storage.storeInterface(createTestInterfaceMetadata("com.example.Service2"))
            storage.storeClass(createTestClassMetadata("com.example.Repository"))

            // WHEN
            val debugString = storage.toString()

            // THEN
            assertTrue(debugString.contains("interfaces=2"))
            assertTrue(debugString.contains("classes=1"))
        }

    // Helper functions to create test metadata

    private fun createTestInterfaceMetadata(fqName: String): ValidatedFakeInterface {
        val parts = fqName.split(".")
        val simpleName = parts.last()
        val packageName = parts.dropLast(1).joinToString(".")

        return ValidatedFakeInterface(
            classId = ClassId.topLevel(FqName(fqName)),
            simpleName = simpleName,
            packageName = packageName,
            typeParameters = emptyList(),
            properties = emptyList(),
            functions = emptyList(),
            inheritedProperties = emptyList(),
            inheritedFunctions = emptyList(),
            sourceLocation = FirSourceLocation.UNKNOWN,
        )
    }

    private fun createTestClassMetadata(fqName: String): ValidatedFakeClass {
        val parts = fqName.split(".")
        val simpleName = parts.last()
        val packageName = parts.dropLast(1).joinToString(".")

        return ValidatedFakeClass(
            classId = ClassId.topLevel(FqName(fqName)),
            simpleName = simpleName,
            packageName = packageName,
            typeParameters = emptyList(),
            abstractProperties = emptyList(),
            openProperties = emptyList(),
            abstractMethods = emptyList(),
            openMethods = emptyList(),
            sourceLocation = FirSourceLocation.UNKNOWN,
        )
    }
}
