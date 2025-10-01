// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains
import java.io.File
import java.nio.file.Files

class IncrementalCompilationManagerTest {

    @Test
    fun `GIVEN new interface WHEN checking regeneration THEN should generate new`() {
        // Given
        val tempDir = Files.createTempDirectory("ktfakes-test").toFile()
        val manager = IncrementalCompilationManager(tempDir)

        val interfaceInfo = InterfaceChangeInfo(
            fullyQualifiedName = "com.example.TestService",
            typeParameters = emptyList(),
            methods = listOf(
                MethodSignatureInfo("getUser", "fun getUser(): User")
            ),
            properties = emptyList(),
            dependencies = emptyList()
        )

        // When
        val decision = manager.needsRegeneration(interfaceInfo)

        // Then
        assertEquals(DecisionType.GENERATE_NEW, decision.type)
        assertContains(decision.reason, "not seen before")

        tempDir.deleteRecursively()
    }

    @Test
    fun `GIVEN unchanged interface WHEN checking regeneration THEN should skip`() {
        // Given
        val tempDir = Files.createTempDirectory("ktfakes-test").toFile()
        val manager = IncrementalCompilationManager(tempDir)

        val interfaceInfo = InterfaceChangeInfo(
            fullyQualifiedName = "com.example.TestService",
            typeParameters = emptyList(),
            methods = listOf(
                MethodSignatureInfo("getUser", "fun getUser(): User")
            ),
            properties = emptyList(),
            dependencies = emptyList()
        )

        // First generation
        manager.recordGeneration(
            interfaceInfo,
            listOf(File(tempDir, "FakeTestServiceImpl.kt").also { it.writeText("fake content") })
        )

        // When checking again without changes
        val decision = manager.needsRegeneration(interfaceInfo)

        // Then
        assertEquals(DecisionType.SKIP_UNCHANGED, decision.type)
        assertContains(decision.reason, "unchanged")

        tempDir.deleteRecursively()
    }

    @Test
    fun `GIVEN changed interface WHEN checking regeneration THEN should regenerate`() {
        // Given
        val tempDir = Files.createTempDirectory("ktfakes-test").toFile()
        val manager = IncrementalCompilationManager(tempDir)

        val originalInterface = InterfaceChangeInfo(
            fullyQualifiedName = "com.example.TestService",
            typeParameters = emptyList(),
            methods = listOf(
                MethodSignatureInfo("getUser", "fun getUser(): User")
            ),
            properties = emptyList(),
            dependencies = emptyList()
        )

        // Record original generation
        manager.recordGeneration(
            originalInterface,
            listOf(File(tempDir, "FakeTestServiceImpl.kt").also { it.writeText("fake content") })
        )

        // Create changed interface (added method)
        val changedInterface = originalInterface.copy(
            methods = originalInterface.methods + MethodSignatureInfo("updateUser", "fun updateUser(user: User)")
        )

        // When
        val decision = manager.needsRegeneration(changedInterface)

        // Then
        assertEquals(DecisionType.REGENERATE_CHANGED, decision.type)
        assertContains(decision.reason, "changed")

        tempDir.deleteRecursively()
    }

    @Test
    fun `GIVEN interface with dependencies WHEN finding dependents THEN should return affected interfaces`() {
        // Given
        val tempDir = Files.createTempDirectory("ktfakes-test").toFile()
        val manager = IncrementalCompilationManager(tempDir)

        // Interface A depends on nothing
        val interfaceA = InterfaceChangeInfo(
            fullyQualifiedName = "com.example.InterfaceA",
            typeParameters = emptyList(),
            methods = emptyList(),
            properties = emptyList(),
            dependencies = emptyList()
        )

        // Interface B depends on A
        val interfaceB = InterfaceChangeInfo(
            fullyQualifiedName = "com.example.InterfaceB",
            typeParameters = emptyList(),
            methods = emptyList(),
            properties = emptyList(),
            dependencies = listOf("com.example.InterfaceA")
        )

        // Interface C depends on B (transitive dependency on A)
        val interfaceC = InterfaceChangeInfo(
            fullyQualifiedName = "com.example.InterfaceC",
            typeParameters = emptyList(),
            methods = emptyList(),
            properties = emptyList(),
            dependencies = listOf("com.example.InterfaceB")
        )

        // Record all interfaces
        manager.recordGeneration(interfaceA, listOf(File(tempDir, "A.kt")))
        manager.recordGeneration(interfaceB, listOf(File(tempDir, "B.kt")))
        manager.recordGeneration(interfaceC, listOf(File(tempDir, "C.kt")))

        // When A changes
        val dependents = manager.findDependentInterfaces("com.example.InterfaceA")

        // Then both B and C should be affected (B directly, C transitively)
        assertEquals(2, dependents.size)
        assertTrue(dependents.contains("com.example.InterfaceB"))
        assertTrue(dependents.contains("com.example.InterfaceC"))

        tempDir.deleteRecursively()
    }

    @Test
    fun `GIVEN manager with cached data WHEN getting incremental stats THEN should return metrics`() {
        // Given
        val tempDir = Files.createTempDirectory("ktfakes-test").toFile()
        val manager = IncrementalCompilationManager(tempDir)

        // Record some interfaces
        repeat(5) { index ->
            val interfaceInfo = InterfaceChangeInfo(
                fullyQualifiedName = "com.example.Interface$index",
                typeParameters = emptyList(),
                methods = listOf(
                    MethodSignatureInfo("method$index", "fun method$index(): String")
                ),
                properties = emptyList(),
                dependencies = emptyList()
            )

            manager.recordGeneration(
                interfaceInfo,
                listOf(File(tempDir, "Interface$index.kt").also { it.writeText("content") })
            )
        }

        // When
        val stats = manager.getIncrementalStats()

        // Then
        assertEquals(5, stats.cachedInterfaces)
        assertEquals(5, stats.generatedFiles)
        assertTrue(stats.cacheHitPotential > 0)

        tempDir.deleteRecursively()
    }

    @Test
    fun `GIVEN manager WHEN saving and loading cache THEN should persist data across sessions`() {
        // Given
        val tempDir = Files.createTempDirectory("ktfakes-test").toFile()

        val interfaceInfo = InterfaceChangeInfo(
            fullyQualifiedName = "com.example.PersistentService",
            typeParameters = listOf("T"),
            methods = listOf(
                MethodSignatureInfo("process", "fun <T> process(data: T): T")
            ),
            properties = listOf(
                PropertySignatureInfo("isReady", "Boolean", false)
            ),
            dependencies = emptyList()
        )

        // First session
        val manager1 = IncrementalCompilationManager(tempDir)
        manager1.recordGeneration(
            interfaceInfo,
            listOf(File(tempDir, "FakePersistentServiceImpl.kt").also { it.writeText("fake") })
        )
        manager1.saveCaches()

        // Second session (new manager instance)
        val manager2 = IncrementalCompilationManager(tempDir)
        val decision = manager2.needsRegeneration(interfaceInfo)

        // Then data should be preserved
        assertEquals(DecisionType.SKIP_UNCHANGED, decision.type)

        tempDir.deleteRecursively()
    }

    @Test
    fun `GIVEN stale interfaces WHEN cleaning up THEN should remove obsolete files`() {
        // Given
        val tempDir = Files.createTempDirectory("ktfakes-test").toFile()
        val manager = IncrementalCompilationManager(tempDir)

        // Create files for two interfaces
        val file1 = File(tempDir, "FakeService1.kt").also { it.writeText("content1") }
        val file2 = File(tempDir, "FakeService2.kt").also { it.writeText("content2") }

        manager.recordGeneration(
            InterfaceChangeInfo("com.example.Service1", emptyList(), emptyList(), emptyList(), emptyList()),
            listOf(file1)
        )
        manager.recordGeneration(
            InterfaceChangeInfo("com.example.Service2", emptyList(), emptyList(), emptyList(), emptyList()),
            listOf(file2)
        )

        // When only Service1 is current (Service2 is stale)
        val deletedFiles = manager.cleanupStaleFiles(setOf("com.example.Service1"))

        // Then Service2's files should be cleaned up
        assertEquals(1, deletedFiles.size)
        assertTrue(deletedFiles.any { it.name == "FakeService2.kt" })
        assertTrue(file1.exists()) // Service1 file should remain
        assertTrue(!file2.exists()) // Service2 file should be deleted

        tempDir.deleteRecursively()
    }
}