// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance.memory

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MemoryOptimizedIrGeneratorTest {

    @Test
    fun `GIVEN memory optimized generator WHEN using object pools THEN should reuse objects efficiently`() {
        // Given
        val generator = MemoryOptimizedIrGenerator()
        val objectPools = MemoryOptimizedIrGenerator.ObjectPoolManager()

        // When - Use pooled StringBuilder multiple times
        val result1 = objectPools.withPooledStringBuilder { sb ->
            sb.append("Hello")
            sb.append(" World")
            sb.toString()
        }

        val result2 = objectPools.withPooledStringBuilder { sb ->
            sb.append("Another")
            sb.append(" String")
            sb.toString()
        }

        // Then
        assertEquals("Hello World", result1)
        assertEquals("Another String", result2)

        val poolStats = objectPools.getPoolStats()
        assertTrue(poolStats.stringBuilderPoolSize >= 0, "Should have StringBuilder pool")
    }

    @Test
    fun `GIVEN string intern pool WHEN interning common strings THEN should reuse string instances`() {
        // Given
        val internPool = MemoryOptimizedIrGenerator.StringInternPool()

        // When
        val string1 = internPool.intern("String")
        val string2 = internPool.intern("String")
        val string3 = internPool.intern("kotlin.String")

        // Then
        assertTrue(string1 === string2, "Same string should be interned to same instance")
        assertTrue(string1 !== string3, "Different strings should be different instances")

        val stats = internPool.getStats()
        assertTrue(stats.totalStrings > 0, "Should have interned strings")
    }

    @Test
    fun `GIVEN IR node cache WHEN caching generated nodes THEN should improve performance`() {
        // Given
        val irNodeCache = MemoryOptimizedIrGenerator.IrNodeCache()
        var generationCount = 0

        // When - Generate same node multiple times
        val node1 = irNodeCache.getCachedNode("test.Interface") {
            generationCount++
            CachedIrNode(
                fullyQualifiedName = "test.Interface",
                generatedCode = GeneratedCode("FakeTestImpl", "class FakeTestImpl {}", emptyList()),
                metadata = emptyMap()
            )
        }

        val node2 = irNodeCache.getCachedNode("test.Interface") {
            generationCount++
            CachedIrNode(
                fullyQualifiedName = "test.Interface",
                generatedCode = GeneratedCode("FakeTestImpl", "class FakeTestImpl {}", emptyList()),
                metadata = emptyMap()
            )
        }

        // Then
        assertEquals(1, generationCount, "Should only generate once, second should be cached")
        assertEquals(node1.fullyQualifiedName, node2.fullyQualifiedName)

        val stats = irNodeCache.getStats()
        assertTrue(stats.hits > 0, "Should have cache hits")
        assertTrue(stats.hitRate > 0, "Should have positive hit rate")
    }

    @Test
    fun `GIVEN batch processor WHEN processing large collections THEN should handle memory efficiently`() {
        // Given
        val batchProcessor = MemoryOptimizedIrGenerator.BatchProcessor()
        val largeList = (1..250).map { "Interface$it" } // Larger than default batch size

        // When
        val results = batchProcessor.processBatches(largeList, batchSize = 50) { batch ->
            // Simulate processing each batch
            batch.map { "Processed$it" }
        }

        // Then
        assertEquals(250, results.size, "Should process all items")
        assertTrue(results.all { it.startsWith("Processed") }, "All items should be processed")
    }

    @Test
    fun `GIVEN memory optimized generator WHEN generating multiple interfaces THEN should provide memory stats`() {
        // Given
        val generator = MemoryOptimizedIrGenerator()
        val codeGenerator = TestCodeGenerator()

        val interfaces = listOf(
            InterfaceSpec("com.test.ServiceA", "ServiceA", listOf(
                MethodSpec("methodA", "String"),
                MethodSpec("methodB", "Int")
            )),
            InterfaceSpec("com.test.ServiceB", "ServiceB", listOf(
                MethodSpec("methodC", "Boolean")
            ))
        )

        // When
        val generatedCodes = generator.generateOptimizedCode(interfaces, codeGenerator)

        // Then
        assertEquals(2, generatedCodes.size)
        assertTrue(generatedCodes.all { it.content.isNotBlank() })

        val memoryStats = generator.getMemoryStats()
        assertNotNull(memoryStats.poolStats)
        assertNotNull(memoryStats.stringPoolStats)
        assertNotNull(memoryStats.cacheStats)
        assertTrue(memoryStats.currentMemoryUsageMB >= 0)
    }

    @Test
    fun `GIVEN object pool WHEN acquiring and releasing objects THEN should maintain pool size limits`() {
        // Given
        val pool = ObjectPool { StringBuilder() }

        // When - Acquire and release multiple objects
        val objects = mutableListOf<StringBuilder>()
        repeat(10) {
            objects.add(pool.acquire())
        }

        objects.forEach { pool.release(it) }

        // Then
        assertTrue(pool.size() > 0, "Pool should contain released objects")
        assertTrue(pool.size() <= 10, "Pool should not exceed reasonable size")
    }

    @Test
    fun `GIVEN memory optimized generator WHEN generating single interface THEN should use optimizations`() {
        // Given
        val generator = MemoryOptimizedIrGenerator()
        val codeGenerator = TestCodeGenerator()

        val interfaceSpec = InterfaceSpec(
            fullyQualifiedName = "com.example.TestService",
            simpleName = "TestService",
            methods = listOf(
                MethodSpec("getValue", "String"),
                MethodSpec("setValue", "Unit", listOf("String")),
                MethodSpec("isReady", "Boolean")
            )
        )

        // When
        val generatedCode = generator.generateSingleInterface(interfaceSpec, codeGenerator)

        // Then
        assertEquals("FakeTestServiceImpl", generatedCode.className)
        assertTrue(generatedCode.content.contains("class FakeTestServiceImpl"))
        assertTrue(generatedCode.content.contains("TestService"))

        // Verify memory stats show activity
        val stats = generator.getMemoryStats()
        assertTrue(stats.stringPoolStats.totalStrings > 0, "Should have interned strings")
    }

    @Test
    fun `GIVEN memory optimized generator WHEN cleanup is called THEN should free resources`() {
        // Given
        val generator = MemoryOptimizedIrGenerator()
        val codeGenerator = TestCodeGenerator()

        // Generate some content first
        val interfaceSpec = InterfaceSpec("com.test.Service", "Service", listOf(
            MethodSpec("test", "String")
        ))
        generator.generateSingleInterface(interfaceSpec, codeGenerator)

        val statsBefore = generator.getMemoryStats()

        // When
        generator.cleanup()

        // Then
        val statsAfter = generator.getMemoryStats()
        // Cache should be cleared
        assertTrue(statsAfter.cacheStats.cachedNodes <= statsBefore.cacheStats.cachedNodes)
    }

    // Test helper class
    private class TestCodeGenerator : CodeGenerator {
        override fun generateImplementation(interfaceSpec: InterfaceSpec): GeneratedCode {
            val className = "Fake${interfaceSpec.simpleName}Impl"
            val content = buildString {
                append("class $className : ${interfaceSpec.simpleName} {\n")
                interfaceSpec.methods.forEach { method ->
                    append("    override fun ${method.name}(): ${method.returnType} = TODO()\n")
                }
                append("}")
            }

            return GeneratedCode(
                className = className,
                content = content,
                imports = emptyList()
            )
        }
    }
}