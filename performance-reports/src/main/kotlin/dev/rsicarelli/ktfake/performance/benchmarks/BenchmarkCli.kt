// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance.benchmarks

/**
 * Command-line interface for running KtFakes performance benchmarks.
 *
 * Usage:
 * - Run all benchmarks: kotlin BenchmarkCli
 * - Run specific size: kotlin BenchmarkCli --size large
 * - Save results: kotlin BenchmarkCli --output results.txt
 */
object BenchmarkCli {

    @JvmStatic
    fun main(args: Array<String>) {
        println("🚀 KtFakes Performance Benchmark CLI")
        println("=" * 40)

        val options = parseArgs(args)
        val benchmark = LargeProjectBenchmark()

        try {
            when (options.size) {
                "small" -> runSingleBenchmark(benchmark, LargeProjectBenchmark.SMALL_PROJECT, options)
                "medium" -> runSingleBenchmark(benchmark, LargeProjectBenchmark.MEDIUM_PROJECT, options)
                "large" -> runSingleBenchmark(benchmark, LargeProjectBenchmark.LARGE_PROJECT, options)
                "enterprise" -> runSingleBenchmark(benchmark, LargeProjectBenchmark.ENTERPRISE_PROJECT, options)
                "all" -> runAllBenchmarks(benchmark, options)
                else -> {
                    println("❌ Invalid size: ${options.size}")
                    printUsage()
                    return
                }
            }

            println("\n✅ Benchmark completed successfully!")

        } catch (e: Exception) {
            println("❌ Benchmark failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun runSingleBenchmark(
        benchmark: LargeProjectBenchmark,
        config: BenchmarkConfig,
        options: CliOptions
    ) {
        println("📊 Running ${config.name} project benchmark...")

        val result = benchmark.runProjectBenchmark(config)

        println("\n🎯 Results:")
        printBenchmarkResult(result)

        if (options.outputFile != null) {
            saveSingleResult(result, options.outputFile)
        }
    }

    private fun runAllBenchmarks(
        benchmark: LargeProjectBenchmark,
        options: CliOptions
    ) {
        println("📊 Running comprehensive benchmark suite...")

        val results = benchmark.runComprehensiveBenchmarks()
        val report = results.generateReport()

        println("\n")
        println(report)

        if (options.outputFile != null) {
            saveReport(report, options.outputFile)
        }
    }

    private fun printBenchmarkResult(result: BenchmarkResult) {
        println("  • Cold compilation: ${result.coldCompilationMs}ms")
        println("  • Warm compilation: ${result.warmCompilationMs}ms (${String.format("%.1fx", result.warmSpeedup)} speedup)")
        println("  • Incremental: ${result.incrementalMs}ms (${String.format("%.1fx", result.incrementalSpeedup)} speedup)")
        println("  • Cache hit rate: ${result.cacheHitRate}%")
        println("  • Memory usage: ${result.memoryUsageMB}MB")
        println("  • Interfaces processed: ${result.interfacesProcessed}")
        println("  • Methods generated: ${result.methodsGenerated}")
        println("  • Properties generated: ${result.propertiesGenerated}")
    }

    private fun saveSingleResult(result: BenchmarkResult, filename: String) {
        val content = buildString {
            appendLine("KtFakes Benchmark Result - ${result.config.name}")
            appendLine("=" * 40)
            appendLine("Timestamp: ${java.time.Instant.now()}")
            appendLine()
            appendLine("Configuration:")
            appendLine("  - Project size: ${result.config.name}")
            appendLine("  - Interface count: ${result.config.interfaceCount}")
            appendLine("  - Avg methods per interface: ${result.config.avgMethodsPerInterface}")
            appendLine("  - Avg properties per interface: ${result.config.avgPropertiesPerInterface}")
            appendLine()
            appendLine("Performance Results:")
            appendLine("  - Cold compilation: ${result.coldCompilationMs}ms")
            appendLine("  - Warm compilation: ${result.warmCompilationMs}ms")
            appendLine("  - Incremental compilation: ${result.incrementalMs}ms")
            appendLine("  - Warm speedup: ${String.format("%.1fx", result.warmSpeedup)}")
            appendLine("  - Incremental speedup: ${String.format("%.1fx", result.incrementalSpeedup)}")
            appendLine()
            appendLine("Resource Usage:")
            appendLine("  - Cache hit rate: ${result.cacheHitRate}%")
            appendLine("  - Peak memory usage: ${result.memoryUsageMB}MB")
            appendLine()
            appendLine("Generated Code:")
            appendLine("  - Interfaces processed: ${result.interfacesProcessed}")
            appendLine("  - Methods generated: ${result.methodsGenerated}")
            appendLine("  - Properties generated: ${result.propertiesGenerated}")
        }

        try {
            java.io.File(filename).writeText(content)
            println("📄 Results saved to: $filename")
        } catch (e: Exception) {
            println("❌ Failed to save results: ${e.message}")
        }
    }

    private fun saveReport(report: String, filename: String) {
        try {
            val fullReport = buildString {
                appendLine("Generated at: ${java.time.Instant.now()}")
                appendLine()
                append(report)
            }

            java.io.File(filename).writeText(fullReport)
            println("📄 Report saved to: $filename")
        } catch (e: Exception) {
            println("❌ Failed to save report: ${e.message}")
        }
    }

    private fun parseArgs(args: Array<String>): CliOptions {
        var size = "all"
        var outputFile: String? = null
        var verbose = false

        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--size", "-s" -> {
                    if (i + 1 < args.size) {
                        size = args[i + 1]
                        i++
                    }
                }
                "--output", "-o" -> {
                    if (i + 1 < args.size) {
                        outputFile = args[i + 1]
                        i++
                    }
                }
                "--verbose", "-v" -> {
                    verbose = true
                }
                "--help", "-h" -> {
                    printUsage()
                    kotlin.system.exitProcess(0)
                }
                else -> {
                    if (args[i].startsWith("-")) {
                        println("❌ Unknown option: ${args[i]}")
                        printUsage()
                        kotlin.system.exitProcess(1)
                    }
                }
            }
            i++
        }

        return CliOptions(size, outputFile, verbose)
    }

    private fun printUsage() {
        println("""
            Usage: kotlin BenchmarkCli [options]

            Options:
              --size, -s <size>     Project size to benchmark (small, medium, large, enterprise, all)
                                   Default: all
              --output, -o <file>   Save results to file
              --verbose, -v         Enable verbose output
              --help, -h           Show this help message

            Examples:
              kotlin BenchmarkCli                           # Run all benchmarks
              kotlin BenchmarkCli --size large              # Run large project benchmark only
              kotlin BenchmarkCli --output results.txt      # Save results to file
              kotlin BenchmarkCli -s enterprise -o report.txt -v  # Enterprise benchmark with file output
        """.trimIndent())
    }

    private data class CliOptions(
        val size: String,
        val outputFile: String?,
        val verbose: Boolean
    )

    private operator fun String.times(n: Int): String = repeat(n)
}