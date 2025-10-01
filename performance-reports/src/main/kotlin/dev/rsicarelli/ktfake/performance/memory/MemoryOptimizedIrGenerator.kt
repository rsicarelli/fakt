// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance.memory

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.lang.ref.WeakReference
import java.lang.ref.SoftReference

/**
 * Memory-optimized IR generation utilities for KtFakes.
 *
 * Implements several memory optimization strategies:
 * - Object pooling for frequently created objects
 * - Lazy generation to reduce peak memory usage
 * - Weak references for cached IR nodes
 * - String interning for common type names
 * - Batch processing to control memory growth
 *
 * Based on Kotlin compiler's own memory optimization patterns.
 */
class MemoryOptimizedIrGenerator {

    private val objectPools = ObjectPoolManager()
    private val stringInternPool = StringInternPool()
    private val irNodeCache = IrNodeCache()
    private val batchProcessor = BatchProcessor()

    /**
     * Object pool manager for frequently allocated objects.
     */
    class ObjectPoolManager {
        // StringBuilder pool for code generation
        private val stringBuilderPool = ObjectPool { StringBuilder(1024) }

        // List pools for different sizes
        private val smallListPool = ObjectPool<MutableList<String>> { mutableListOf() }
        private val mediumListPool = ObjectPool<MutableList<String>> { ArrayList(50) }
        private val largeListPool = ObjectPool<MutableList<String>> { ArrayList(200) }

        // Map pool for metadata
        private val mapPool = ObjectPool<MutableMap<String, Any>> { mutableMapOf() }

        fun <T> withPooledStringBuilder(block: (StringBuilder) -> T): T {
            val sb = stringBuilderPool.acquire()
            try {
                sb.clear()
                return block(sb)
            } finally {
                stringBuilderPool.release(sb)
            }
        }

        fun <T> withPooledList(expectedSize: Int = 10, block: (MutableList<String>) -> T): T {
            val pool = when {
                expectedSize <= 10 -> smallListPool
                expectedSize <= 50 -> mediumListPool
                else -> largeListPool
            }

            val list = pool.acquire()
            try {
                list.clear()
                return block(list)
            } finally {
                pool.release(list)
            }
        }

        fun <T> withPooledMap(block: (MutableMap<String, Any>) -> T): T {
            val map = mapPool.acquire()
            try {
                map.clear()
                return block(map)
            } finally {
                mapPool.release(map)
            }
        }

        fun getPoolStats(): PoolStats {
            return PoolStats(
                stringBuilderPoolSize = stringBuilderPool.size(),
                smallListPoolSize = smallListPool.size(),
                mediumListPoolSize = mediumListPool.size(),
                largeListPoolSize = largeListPool.size(),
                mapPoolSize = mapPool.size()
            )
        }
    }

    /**
     * String interning pool for common type names and method names.
     */
    class StringInternPool {
        private val internedStrings = ConcurrentHashMap<String, WeakReference<String>>()

        // Pre-populate with common Kotlin types
        init {
            prePopulateCommonStrings()
        }

        fun intern(string: String): String {
            val existing = internedStrings[string]?.get()
            if (existing != null) {
                return existing
            }

            // Use Java's string interning for the actual interning
            val interned = string.intern()
            internedStrings[string] = WeakReference(interned)
            return interned
        }

        private fun prePopulateCommonStrings() {
            val commonTypes = listOf(
                "String", "Int", "Boolean", "Unit", "Long", "Double", "Float",
                "List", "Set", "Map", "Collection", "MutableList", "MutableSet", "MutableMap",
                "Result", "Optional", "Pair", "Triple",
                "suspend", "fun", "val", "var", "override", "internal", "private", "public",
                "kotlin.String", "kotlin.Int", "kotlin.Boolean", "kotlin.Unit",
                "kotlin.collections.List", "kotlin.collections.Set", "kotlin.collections.Map"
            )

            commonTypes.forEach { intern(it) }
        }

        fun getStats(): StringPoolStats {
            // Clean up dead references
            val deadRefs = internedStrings.values.count { it.get() == null }
            internedStrings.values.removeIf { it.get() == null }

            return StringPoolStats(
                totalStrings = internedStrings.size,
                deadReferencesCleared = deadRefs
            )
        }
    }

    /**
     * Soft-reference cache for IR nodes to allow GC when memory is needed.
     */
    class IrNodeCache {
        private val cache = ConcurrentHashMap<String, SoftReference<CachedIrNode>>()
        private var hits = 0L
        private var misses = 0L

        fun getCachedNode(key: String, generator: () -> CachedIrNode): CachedIrNode {
            val cached = cache[key]?.get()
            if (cached != null) {
                hits++
                return cached
            }

            misses++
            val newNode = generator()
            cache[key] = SoftReference(newNode)
            return newNode
        }

        fun clearCache() {
            cache.clear()
            hits = 0L
            misses = 0L
        }

        fun getStats(): CacheStats {
            // Clean up empty soft references
            val deadRefs = cache.values.count { it.get() == null }
            cache.values.removeIf { it.get() == null }

            val total = hits + misses
            val hitRate = if (total > 0) (hits * 100 / total).toInt() else 0

            return CacheStats(
                totalRequests = total,
                hits = hits,
                misses = misses,
                hitRate = hitRate,
                cachedNodes = cache.size,
                deadReferencesCleared = deadRefs
            )
        }
    }

    /**
     * Batch processor to control memory usage during bulk operations.
     */
    class BatchProcessor {
        companion object {
            const val DEFAULT_BATCH_SIZE = 100
            const val MEMORY_THRESHOLD_MB = 512 // Force GC if memory usage exceeds this
        }

        fun <T, R> processBatches(
            items: List<T>,
            batchSize: Int = DEFAULT_BATCH_SIZE,
            processor: (List<T>) -> List<R>
        ): List<R> {
            val results = mutableListOf<R>()

            items.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                // Process batch
                val batchResults = processor(batch)
                results.addAll(batchResults)

                // Check memory usage and suggest GC if needed
                if (batchIndex % 5 == 0) { // Check every 5 batches
                    checkMemoryAndSuggestGC()
                }
            }

            return results
        }

        private fun checkMemoryAndSuggestGC() {
            val runtime = Runtime.getRuntime()
            val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

            if (usedMemoryMB > MEMORY_THRESHOLD_MB) {
                // Suggest GC to free up memory
                System.gc()

                // Small pause to allow GC to complete
                Thread.sleep(10)
            }
        }
    }

    /**
     * Generate code with memory optimizations.
     */
    fun generateOptimizedCode(
        interfaces: List<InterfaceSpec>,
        generator: CodeGenerator
    ): List<GeneratedCode> {
        return batchProcessor.processBatches(interfaces) { batch ->
            batch.map { interfaceSpec ->
                generateSingleInterface(interfaceSpec, generator)
            }
        }
    }

    /**
     * Generate code for a single interface with full memory optimization.
     */
    fun generateSingleInterface(
        interfaceSpec: InterfaceSpec,
        generator: CodeGenerator
    ): GeneratedCode {
        val cacheKey = interfaceSpec.fullyQualifiedName

        return irNodeCache.getCachedNode(cacheKey) {
            CachedIrNode(
                fullyQualifiedName = stringInternPool.intern(interfaceSpec.fullyQualifiedName),
                generatedCode = objectPools.withPooledStringBuilder { sb ->
                    objectPools.withPooledList(interfaceSpec.methods.size) { methodList ->
                        objectPools.withPooledMap { metadata ->
                            // Generate code using pooled resources
                            generateCodeWithPools(interfaceSpec, sb, methodList, metadata, generator)
                        }
                    }
                },
                metadata = emptyMap() // Simplified for example
            )
        }.generatedCode
    }

    private fun generateCodeWithPools(
        interfaceSpec: InterfaceSpec,
        sb: StringBuilder,
        methodList: MutableList<String>,
        metadata: MutableMap<String, Any>,
        generator: CodeGenerator
    ): GeneratedCode {
        // Use interned strings for common elements
        val className = stringInternPool.intern("Fake${interfaceSpec.simpleName}Impl")
        val interfaceName = stringInternPool.intern(interfaceSpec.simpleName)

        // Generate class declaration
        sb.append("class ").append(className).append(" : ").append(interfaceName).append(" {\n")

        // Generate methods using pooled list
        interfaceSpec.methods.forEach { method ->
            val methodCode = generateMethodCode(method)
            methodList.add(methodCode)
            sb.append("    ").append(methodCode).append("\n")
        }

        sb.append("}")

        return GeneratedCode(
            className = className,
            content = sb.toString(),
            imports = emptyList() // Simplified
        )
    }

    private fun generateMethodCode(method: MethodSpec): String {
        return objectPools.withPooledStringBuilder { sb ->
            val returnType = stringInternPool.intern(method.returnType)
            val methodName = stringInternPool.intern(method.name)

            sb.append("override fun ").append(methodName).append("(): ").append(returnType)
            sb.append(" = TODO(\"Generated method\")")
            sb.toString()
        }
    }

    /**
     * Get comprehensive memory optimization statistics.
     */
    fun getMemoryStats(): MemoryOptimizationStats {
        return MemoryOptimizationStats(
            poolStats = objectPools.getPoolStats(),
            stringPoolStats = stringInternPool.getStats(),
            cacheStats = irNodeCache.getStats(),
            currentMemoryUsageMB = getCurrentMemoryUsage()
        )
    }

    /**
     * Clean up all caches and pools to free memory.
     */
    fun cleanup() {
        irNodeCache.clearCache()
        // Object pools will be cleaned up by GC when references are lost
        System.gc()
    }

    private fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }
}

/**
 * Generic object pool for reusing expensive objects.
 */
class ObjectPool<T>(private val factory: () -> T) {
    private val pool = ConcurrentLinkedQueue<T>()
    private val maxSize = 100 // Prevent unlimited growth

    fun acquire(): T {
        return pool.poll() ?: factory()
    }

    fun release(item: T) {
        if (pool.size < maxSize) {
            pool.offer(item)
        }
        // If pool is full, let the object be GC'd
    }

    fun size(): Int = pool.size
}

// Data classes for specifications and results
data class InterfaceSpec(
    val fullyQualifiedName: String,
    val simpleName: String,
    val methods: List<MethodSpec>
)

data class MethodSpec(
    val name: String,
    val returnType: String,
    val parameters: List<String> = emptyList()
)

data class GeneratedCode(
    val className: String,
    val content: String,
    val imports: List<String>
)

data class CachedIrNode(
    val fullyQualifiedName: String,
    val generatedCode: GeneratedCode,
    val metadata: Map<String, Any>
)

// Interface for code generators
interface CodeGenerator {
    fun generateImplementation(interfaceSpec: InterfaceSpec): GeneratedCode
}

// Statistics classes
data class PoolStats(
    val stringBuilderPoolSize: Int,
    val smallListPoolSize: Int,
    val mediumListPoolSize: Int,
    val largeListPoolSize: Int,
    val mapPoolSize: Int
)

data class StringPoolStats(
    val totalStrings: Int,
    val deadReferencesCleared: Int
)

data class CacheStats(
    val totalRequests: Long,
    val hits: Long,
    val misses: Long,
    val hitRate: Int,
    val cachedNodes: Int,
    val deadReferencesCleared: Int
)

data class MemoryOptimizationStats(
    val poolStats: PoolStats,
    val stringPoolStats: StringPoolStats,
    val cacheStats: CacheStats,
    val currentMemoryUsageMB: Long
)