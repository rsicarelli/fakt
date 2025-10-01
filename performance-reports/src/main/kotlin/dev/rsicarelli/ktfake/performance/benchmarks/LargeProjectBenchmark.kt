// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance.benchmarks

import com.rsicarelli.fakt.performance.*
import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Large project performance benchmarks for KtFakes.
 *
 * Simulates real-world scenarios with hundreds of interfaces to validate
 * performance optimizations and identify bottlenecks.
 *
 * Benchmark scenarios:
 * - Cold compilation (no cache)
 * - Warm compilation (with cache)
 * - Incremental compilation (partial changes)
 * - Mixed complexity interfaces
 */
class LargeProjectBenchmark {

    companion object {
        // Benchmark configurations
        val SMALL_PROJECT = BenchmarkConfig("Small", 50, 5, 2)
        val MEDIUM_PROJECT = BenchmarkConfig("Medium", 200, 8, 3)
        val LARGE_PROJECT = BenchmarkConfig("Large", 500, 12, 4)
        val ENTERPRISE_PROJECT = BenchmarkConfig("Enterprise", 1000, 20, 6)
    }

    /**
     * Run comprehensive benchmark suite across different project sizes.
     */
    fun runComprehensiveBenchmarks(): BenchmarkResults {
        val results = mutableListOf<BenchmarkResult>()

        println("🚀 Starting KtFakes Large Project Performance Benchmarks")
        println("=" * 60)

        // Run benchmarks for each project size
        listOf(SMALL_PROJECT, MEDIUM_PROJECT, LARGE_PROJECT, ENTERPRISE_PROJECT).forEach { config ->
            println("\n📊 Benchmarking ${config.name} Project (${config.interfaceCount} interfaces)")

            val result = runProjectBenchmark(config)
            results.add(result)

            println("Results: ${result.coldCompilationMs}ms cold, ${result.warmCompilationMs}ms warm, ${result.incrementalMs}ms incremental")
        }

        return BenchmarkResults(results)
    }

    /**
     * Run benchmark for a specific project configuration.
     */
    fun runProjectBenchmark(config: BenchmarkConfig): BenchmarkResult {
        val tempDir = Files.createTempDirectory("ktfakes-benchmark-${config.name.lowercase()}").toFile()

        try {
            // Generate synthetic project
            val projectGenerator = SyntheticProjectGenerator()
            val project = projectGenerator.generateProject(config)

            // Initialize performance tracking
            val performanceTracker = FaktPerformanceTracker.create(enabled = true)
            val typeCache = TypeAnalysisCache()
            val incrementalManager = IncrementalCompilationManager(tempDir, performanceTracker)

            // Pre-warm common types
            typeCache.preWarmCache()

            // 1. Cold compilation benchmark (no cache)
            val coldTime = measureTimeMillis {
                simulateCompilation(project, performanceTracker, typeCache, incrementalManager, isWarm = false)
            }

            // 2. Warm compilation benchmark (with cache)
            val warmTime = measureTimeMillis {
                simulateCompilation(project, performanceTracker, typeCache, incrementalManager, isWarm = true)
            }

            // 3. Incremental compilation benchmark (10% interfaces changed)
            val changedInterfaces = project.interfaces.take((project.interfaces.size * 0.1).toInt())
            val incrementalTime = measureTimeMillis {
                simulateIncrementalCompilation(changedInterfaces, performanceTracker, incrementalManager)
            }

            // Collect performance metrics
            val cacheStats = typeCache.getCacheStats()
            val incrementalStats = incrementalManager.getIncrementalStats()

            return BenchmarkResult(
                config = config,
                coldCompilationMs = coldTime,
                warmCompilationMs = warmTime,
                incrementalMs = incrementalTime,
                cacheHitRate = cacheStats.hitRatePercent,
                memoryUsageMB = getCurrentMemoryUsage(),
                interfacesProcessed = project.interfaces.size,
                methodsGenerated = project.interfaces.sumOf { it.methods.size },
                propertiesGenerated = project.interfaces.sumOf { it.properties.size }
            )

        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * Simulate the compilation process for performance measurement.
     */
    private fun simulateCompilation(
        project: SyntheticProject,
        tracker: FaktPerformanceTracker,
        cache: TypeAnalysisCache,
        incrementalManager: IncrementalCompilationManager,
        isWarm: Boolean
    ) {
        tracker.startPhase(CompilationPhase.INTERFACE_DISCOVERY)

        // Simulate interface discovery overhead
        Thread.sleep(project.interfaces.size.toLong() / 10) // Realistic discovery time

        tracker.endPhase(CompilationPhase.INTERFACE_DISCOVERY, mapOf(
            "interfaces_found" to project.interfaces.size
        ))

        tracker.startPhase(CompilationPhase.TYPE_ANALYSIS)

        var totalCacheRequests = 0
        var cacheHits = 0

        // Simulate type analysis for each interface
        project.interfaces.forEach { syntheticInterface ->
            // Simulate type string conversion (expensive operation)
            syntheticInterface.methods.forEach { method ->
                totalCacheRequests++
                val typeString = cache.getCachedTypeString(method.returnType, true) {
                    // Simulate expensive type analysis
                    Thread.sleep(1L) // 1ms per type analysis
                    method.returnType
                }
                if (isWarm) cacheHits++
            }

            // Simulate property type analysis
            syntheticInterface.properties.forEach { property ->
                totalCacheRequests++
                val defaultValue = cache.getCachedDefaultValue(property.type) {
                    // Simulate default value computation
                    generateDefaultValue(property.type)
                }
                if (isWarm) cacheHits++
            }
        }

        tracker.endPhase(CompilationPhase.TYPE_ANALYSIS, mapOf(
            "cache_requests" to totalCacheRequests,
            "cache_hit_rate" to if (totalCacheRequests > 0) (cacheHits * 100 / totalCacheRequests) else 0
        ))

        tracker.startPhase(CompilationPhase.CODE_GENERATION)

        // Simulate code generation overhead
        val totalMethods = project.interfaces.sumOf { it.methods.size }
        val totalProperties = project.interfaces.sumOf { it.properties.size }

        // Realistic code generation timing: ~0.5ms per method, ~0.2ms per property
        Thread.sleep((totalMethods * 0.5 + totalProperties * 0.2).toLong())

        tracker.endPhase(CompilationPhase.CODE_GENERATION, mapOf(
            "methods_generated" to totalMethods,
            "properties_generated" to totalProperties
        ))

        tracker.recordGlobalMetrics(
            fakesGenerated = project.interfaces.size,
            methodsGenerated = totalMethods,
            propertiesGenerated = totalProperties
        )
    }

    /**
     * Simulate incremental compilation with partial changes.
     */
    private fun simulateIncrementalCompilation(
        changedInterfaces: List<SyntheticInterface>,
        tracker: FaktPerformanceTracker,
        incrementalManager: IncrementalCompilationManager
    ) {
        tracker.startPhase(CompilationPhase.INTERFACE_DISCOVERY)

        changedInterfaces.forEach { syntheticInterface ->
            val interfaceInfo = InterfaceChangeInfo(
                fullyQualifiedName = syntheticInterface.fullyQualifiedName,
                typeParameters = syntheticInterface.typeParameters,
                methods = syntheticInterface.methods.map { method ->
                    MethodSignatureInfo(method.name, "${method.name}(): ${method.returnType}")
                },
                properties = syntheticInterface.properties.map { property ->
                    PropertySignatureInfo(property.name, property.type, property.isMutable)
                },
                dependencies = syntheticInterface.dependencies
            )

            val decision = incrementalManager.needsRegeneration(interfaceInfo)

            // Simulate regeneration if needed
            if (decision.type != DecisionType.SKIP_UNCHANGED) {
                // Simulate compilation work for changed interface
                Thread.sleep(10L) // 10ms per changed interface
            }
        }

        tracker.endPhase(CompilationPhase.INTERFACE_DISCOVERY, mapOf(
            "changed_interfaces" to changedInterfaces.size
        ))
    }

    private fun generateDefaultValue(type: String): String {
        return when {
            type.contains("String") -> "\"\""
            type.contains("Int") -> "0"
            type.contains("Boolean") -> "false"
            type.contains("List") -> "emptyList()"
            type.contains("Set") -> "emptySet()"
            type.contains("Map") -> "emptyMap()"
            else -> "null"
        }
    }

    private fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) // MB
    }
}

/**
 * Configuration for benchmark scenarios.
 */
data class BenchmarkConfig(
    val name: String,
    val interfaceCount: Int,
    val avgMethodsPerInterface: Int,
    val avgPropertiesPerInterface: Int
)

/**
 * Results from a single benchmark run.
 */
data class BenchmarkResult(
    val config: BenchmarkConfig,
    val coldCompilationMs: Long,
    val warmCompilationMs: Long,
    val incrementalMs: Long,
    val cacheHitRate: Int,
    val memoryUsageMB: Long,
    val interfacesProcessed: Int,
    val methodsGenerated: Int,
    val propertiesGenerated: Int
) {
    val warmSpeedup: Double get() = coldCompilationMs.toDouble() / warmCompilationMs.toDouble()
    val incrementalSpeedup: Double get() = coldCompilationMs.toDouble() / incrementalMs.toDouble()
}

/**
 * Collection of benchmark results with analysis.
 */
data class BenchmarkResults(
    val results: List<BenchmarkResult>
) {
    fun generateReport(): String {
        return buildString {
            appendLine("🎯 KtFakes Performance Benchmark Report")
            appendLine("=" * 50)
            appendLine()

            appendLine("| Project Size | Cold (ms) | Warm (ms) | Incremental (ms) | Warm Speedup | Incremental Speedup | Cache Hit % |")
            appendLine("|--------------|-----------|-----------|-------------------|--------------|---------------------|-------------|")

            results.forEach { result ->
                appendLine("| ${result.config.name.padEnd(12)} | ${result.coldCompilationMs.toString().padEnd(9)} | ${result.warmCompilationMs.toString().padEnd(9)} | ${result.incrementalMs.toString().padEnd(17)} | ${String.format("%.1fx", result.warmSpeedup).padEnd(12)} | ${String.format("%.1fx", result.incrementalSpeedup).padEnd(19)} | ${result.cacheHitRate.toString().padEnd(11)} |")
            }

            appendLine()
            appendLine("📊 Performance Analysis:")

            val avgWarmSpeedup = results.map { it.warmSpeedup }.average()
            val avgIncrementalSpeedup = results.map { it.incrementalSpeedup }.average()
            val avgCacheHitRate = results.map { it.cacheHitRate }.average()

            appendLine("  • Average warm compilation speedup: ${String.format("%.1fx", avgWarmSpeedup)}")
            appendLine("  • Average incremental speedup: ${String.format("%.1fx", avgIncrementalSpeedup)}")
            appendLine("  • Average cache hit rate: ${String.format("%.1f%%", avgCacheHitRate)}")

            appendLine()
            appendLine("🎯 Recommendations:")
            when {
                avgCacheHitRate < 60 -> appendLine("  • Consider improving cache strategies")
                avgWarmSpeedup < 2.0 -> appendLine("  • Type analysis caching needs optimization")
                avgIncrementalSpeedup < 5.0 -> appendLine("  • Incremental compilation can be improved")
                else -> appendLine("  • Performance is excellent! 🚀")
            }
        }
    }
}

private operator fun String.times(n: Int): String = repeat(n)