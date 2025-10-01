// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance.gradle

import org.gradle.api.provider.Property
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.Action
import javax.inject.Inject

/**
 * Gradle DSL extension for configuring KtFakes performance reporting.
 *
 * Usage in build.gradle.kts:
 * ```kotlin
 * ktfakePerformance {
 *     enabled.set(true)
 *     outputDir.set(layout.buildDirectory.dir("reports/ktfake-performance"))
 *     benchmarkOnBuild.set(true)
 *     reportFormat.set("html")
 *     memoryProfiling {
 *         enabled.set(true)
 *         heapDumpOnOutOfMemory.set(true)
 *     }
 * }
 * ```
 */
abstract class FaktPerformanceExtension @Inject constructor() {

    /**
     * Enable or disable performance reporting.
     * Default: false (to avoid overhead in development builds)
     */
    abstract val enabled: Property<Boolean>

    /**
     * Output directory for performance reports.
     * Default: build/reports/ktfake-performance
     */
    abstract val outputDir: DirectoryProperty

    /**
     * Whether to run performance benchmarks automatically on each build.
     * Default: false (only on explicit request)
     */
    abstract val benchmarkOnBuild: Property<Boolean>

    /**
     * Report format: "html", "json", "text", or "all"
     * Default: "html"
     */
    abstract val reportFormat: Property<String>

    /**
     * Compilation time threshold in milliseconds above which to generate warnings.
     * Default: 5000ms (5 seconds)
     */
    abstract val compilationTimeWarningThreshold: Property<Long>

    /**
     * Memory usage threshold in MB above which to generate warnings.
     * Default: 1000MB (1GB)
     */
    abstract val memoryUsageWarningThreshold: Property<Long>

    /**
     * Memory profiling configuration.
     */
    val memoryProfiling: MemoryProfilingConfig = MemoryProfilingConfig()

    /**
     * Incremental compilation configuration.
     */
    val incrementalCompilation: IncrementalConfig = IncrementalConfig()

    /**
     * Type analysis caching configuration.
     */
    val typeAnalysis: TypeAnalysisConfig = TypeAnalysisConfig()

    /**
     * Benchmarking configuration.
     */
    val benchmarking: BenchmarkingConfig = BenchmarkingConfig()

    fun memoryProfiling(action: Action<MemoryProfilingConfig>) {
        action.execute(memoryProfiling)
    }

    fun incrementalCompilation(action: Action<IncrementalConfig>) {
        action.execute(incrementalCompilation)
    }

    fun typeAnalysis(action: Action<TypeAnalysisConfig>) {
        action.execute(typeAnalysis)
    }

    fun benchmarking(action: Action<BenchmarkingConfig>) {
        action.execute(benchmarking)
    }

    init {
        // Set sensible defaults
        enabled.convention(false)
        benchmarkOnBuild.convention(false)
        reportFormat.convention("html")
        compilationTimeWarningThreshold.convention(5000L)
        memoryUsageWarningThreshold.convention(1000L)
    }
}

/**
 * Memory profiling configuration section.
 */
abstract class MemoryProfilingConfig {
    abstract val enabled: Property<Boolean>
    abstract val heapDumpOnOutOfMemory: Property<Boolean>
    abstract val gcLogging: Property<Boolean>
    abstract val memoryThresholdMB: Property<Long>

    init {
        enabled.convention(true)
        heapDumpOnOutOfMemory.convention(false)
        gcLogging.convention(false)
        memoryThresholdMB.convention(512L)
    }
}

/**
 * Incremental compilation configuration section.
 */
abstract class IncrementalConfig {
    abstract val enabled: Property<Boolean>
    abstract val cacheDir: DirectoryProperty
    abstract val dependencyTracking: Property<Boolean>
    abstract val signatureHashing: Property<Boolean>

    init {
        enabled.convention(true)
        dependencyTracking.convention(true)
        signatureHashing.convention(true)
    }
}

/**
 * Type analysis caching configuration section.
 */
abstract class TypeAnalysisConfig {
    abstract val cacheEnabled: Property<Boolean>
    abstract val maxCacheSize: Property<Int>
    abstract val cacheExpiry: Property<Long>
    abstract val preWarmCommonTypes: Property<Boolean>

    init {
        cacheEnabled.convention(true)
        maxCacheSize.convention(10000)
        cacheExpiry.convention(3600000L) // 1 hour
        preWarmCommonTypes.convention(true)
    }
}

/**
 * Benchmarking configuration section.
 */
abstract class BenchmarkingConfig {
    abstract val autoRun: Property<Boolean>
    abstract val smallProjectThreshold: Property<Int>
    abstract val mediumProjectThreshold: Property<Int>
    abstract val largeProjectThreshold: Property<Int>
    abstract val includeWarmupRuns: Property<Boolean>
    abstract val includeIncrementalRuns: Property<Boolean>

    init {
        autoRun.convention(false)
        smallProjectThreshold.convention(50)
        mediumProjectThreshold.convention(200)
        largeProjectThreshold.convention(500)
        includeWarmupRuns.convention(true)
        includeIncrementalRuns.convention(true)
    }
}