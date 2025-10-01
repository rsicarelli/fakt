// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

/**
 * Gradle plugin for KtFakes performance reporting and optimization.
 *
 * This plugin adds performance monitoring capabilities to KtFakes compilation:
 * - Performance tracking and reporting
 * - Memory optimization configuration
 * - Incremental compilation settings
 * - Type analysis caching
 * - Automated benchmarking
 *
 * Apply in build.gradle.kts:
 * ```kotlin
 * plugins {
 *     id("dev.rsicarelli.ktfake.performance")
 * }
 * ```
 */
class FaktPerformancePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Register the performance extension
        val extension = project.extensions.create("ktfakePerformance", FaktPerformanceExtension::class.java)

        // Create performance report task
        val performanceReportTask = registerPerformanceReportTask(project, extension)

        // Create benchmark task
        val benchmarkTask = registerBenchmarkTask(project, extension)

        // Create memory profiling task
        val memoryProfilingTask = registerMemoryProfilingTask(project, extension)

        // Configure Kotlin compilation tasks
        configureKotlinCompilation(project, extension)

        // Set up task dependencies
        project.afterEvaluate {
            if (extension.benchmarkOnBuild.get()) {
                project.tasks.named("build") {
                    dependsOn(benchmarkTask)
                }
            }
        }
    }

    private fun registerPerformanceReportTask(
        project: Project,
        extension: FaktPerformanceExtension
    ): TaskProvider<FaktPerformanceReportTask> {
        return project.tasks.register<FaktPerformanceReportTask>("ktfakePerformanceReport") {
            group = "verification"
            description = "Generate KtFakes performance report"

            enabled.set(extension.enabled)
            outputDirectory.set(extension.outputDir)
            reportFormat.set(extension.reportFormat)
            compilationTimeThreshold.set(extension.compilationTimeWarningThreshold)
            memoryUsageThreshold.set(extension.memoryUsageWarningThreshold)
        }
    }

    private fun registerBenchmarkTask(
        project: Project,
        extension: FaktPerformanceExtension
    ): TaskProvider<KtFakeBenchmarkTask> {
        return project.tasks.register<KtFakeBenchmarkTask>("ktfakeBenchmark") {
            group = "verification"
            description = "Run KtFakes performance benchmarks"

            enabled.set(extension.enabled)
            outputDirectory.set(extension.outputDir)
            autoRun.set(extension.benchmarking.autoRun)
            smallThreshold.set(extension.benchmarking.smallProjectThreshold)
            mediumThreshold.set(extension.benchmarking.mediumProjectThreshold)
            largeThreshold.set(extension.benchmarking.largeProjectThreshold)
            includeWarmup.set(extension.benchmarking.includeWarmupRuns)
            includeIncremental.set(extension.benchmarking.includeIncrementalRuns)
        }
    }

    private fun registerMemoryProfilingTask(
        project: Project,
        extension: FaktPerformanceExtension
    ): TaskProvider<KtFakeMemoryProfilingTask> {
        return project.tasks.register<KtFakeMemoryProfilingTask>("ktfakeMemoryProfiling") {
            group = "verification"
            description = "Profile KtFakes memory usage during compilation"

            enabled.set(extension.memoryProfiling.enabled)
            outputDirectory.set(extension.outputDir)
            heapDumpOnOOM.set(extension.memoryProfiling.heapDumpOnOutOfMemory)
            gcLogging.set(extension.memoryProfiling.gcLogging)
            memoryThreshold.set(extension.memoryProfiling.memoryThresholdMB)
        }
    }

    private fun configureKotlinCompilation(
        project: Project,
        extension: FaktPerformanceExtension
    ) {
        project.tasks.withType(KotlinCompile::class.java) { kotlinTask ->
            kotlinTask.doFirst {
                if (extension.enabled.get()) {
                    // Configure compiler arguments for performance tracking
                    kotlinTask.compilerOptions.freeCompilerArgs.addAll(
                        "-P", "plugin:dev.rsicarelli.ktfake:performance-enabled=true",
                        "-P", "plugin:dev.rsicarelli.ktfake:output-dir=${extension.outputDir.get().asFile.absolutePath}"
                    )

                    // Memory profiling configuration
                    if (extension.memoryProfiling.enabled.get()) {
                        kotlinTask.compilerOptions.freeCompilerArgs.addAll(
                            "-P", "plugin:dev.rsicarelli.ktfake:memory-profiling=true",
                            "-P", "plugin:dev.rsicarelli.ktfake:memory-threshold=${extension.memoryProfiling.memoryThresholdMB.get()}"
                        )
                    }

                    // Type analysis caching configuration
                    if (extension.typeAnalysis.cacheEnabled.get()) {
                        kotlinTask.compilerOptions.freeCompilerArgs.addAll(
                            "-P", "plugin:dev.rsicarelli.ktfake:type-cache=true",
                            "-P", "plugin:dev.rsicarelli.ktfake:cache-size=${extension.typeAnalysis.maxCacheSize.get()}"
                        )
                    }

                    // Incremental compilation configuration
                    if (extension.incrementalCompilation.enabled.get()) {
                        kotlinTask.compilerOptions.freeCompilerArgs.addAll(
                            "-P", "plugin:dev.rsicarelli.ktfake:incremental=true",
                            "-P", "plugin:dev.rsicarelli.ktfake:cache-dir=${extension.incrementalCompilation.cacheDir.orNull?.asFile?.absolutePath ?: "${project.buildDir}/ktfake-cache"}"
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val PLUGIN_ID = "dev.rsicarelli.ktfake.performance"
    }
}