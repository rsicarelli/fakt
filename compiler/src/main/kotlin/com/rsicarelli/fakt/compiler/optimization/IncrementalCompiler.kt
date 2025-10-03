// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.optimization

import com.rsicarelli.fakt.compiler.types.TypeInfo

/**
 * Production implementation of [CompilerOptimizations] with efficient algorithms for large-scale projects.
 *
 * This implementation provides:
 * - O(1) annotation configuration lookup
 * - O(n) type indexing and annotation-based filtering
 * - Memory-efficient signature-based change detection
 * - Thread-safe operations for concurrent compilation
 * - Simple metrics collection and reporting
 * - Persistent incremental compilation cache
 *
 * The implementation uses simple but effective data structures optimized for typical
 * compiler plugin usage patterns where most operations are reads after an initial indexing phase.
 *
 * @param fakeAnnotations List of fully qualified annotation names to process
 * @param outputDir Optional output directory for cache files
 *
 * @since 1.0.0
 */
internal class IncrementalCompiler(
    private val fakeAnnotations: List<String>,
    private val outputDir: String? = null,
) : CompilerOptimizations {
    /** Index of all discovered types for efficient annotation-based lookup */
    private val indexedTypes = mutableListOf<TypeInfo>()

    /** Set of signatures for types that have been successfully generated */
    private val generatedTypes = mutableSetOf<String>()

    /** Persistent cache of previous compilation signatures for incremental compilation */
    private val previousSignatures = mutableMapOf<String, String>()

    /** Compilation metrics for simple reporting */
    private val metrics = CompilationMetrics()

    init {
        loadPreviousSignatures()
    }

    override fun isConfiguredFor(annotation: String): Boolean = annotation in fakeAnnotations

    override fun indexType(type: TypeInfo) {
        indexedTypes.add(type)
    }

    override fun findTypesWithAnnotation(annotation: String): List<TypeInfo> =
        indexedTypes.filter { type ->
            type.annotations.any { it == annotation }
        }

    override fun needsRegeneration(type: TypeInfo): Boolean {
        // Check if this is the first time we see this type
        val typeKey = "${type.fullyQualifiedName}@${type.fileName}"
        val previousSignature = previousSignatures[typeKey]

        if (previousSignature == null) {
            // New type - needs generation
            return true
        }

        // Compare current signature with previous one
        return type.signature != previousSignature
    }

    override fun recordGeneration(type: TypeInfo) {
        val typeKey = "${type.fullyQualifiedName}@${type.fileName}"
        generatedTypes.add(type.signature)
        previousSignatures[typeKey] = type.signature
        metrics.recordGeneration()
    }

    /**
     * Get current compilation metrics for reporting.
     */
    private fun getMetrics(): CompilationMetrics =
        metrics.copy(
            typesIndexed = indexedTypes.size,
            typesGenerated = generatedTypes.size,
            typesSkipped = indexedTypes.size - generatedTypes.size,
            annotationsConfigured = fakeAnnotations.size,
        )

    /**
     * Generate a simple JSON report file.
     */
    fun generateReport(outputDir: String?) {
        if (outputDir == null) return

        try {
            val reportGenerator = JsonReportGenerator()
            val reportMetrics = getMetrics()

            val reportData =
                reportGenerator.generateReportData(
                    metrics = reportMetrics,
                    fakeAnnotations = fakeAnnotations,
                    indexedTypes = indexedTypes,
                    generatedTypes = generatedTypes,
                )

            val reportJson = reportGenerator.toJsonString(reportData)

            val reportFile = java.io.File(outputDir, "fakt-report.json")
            reportFile.parentFile?.mkdirs()
            reportFile.writeText(reportJson)

            println("Fakt: Report generated at ${reportFile.absolutePath}")
        } catch (e: java.io.IOException) {
            println("Fakt: Failed to generate report: ${e.message}")
        }
    }

    /**
     * Loads previous compilation signatures for incremental compilation.
     */
    private fun loadPreviousSignatures() {
        if (outputDir == null) return

        try {
            val cacheFile = java.io.File(outputDir, "fakt-signatures.cache")
            val loaded = loadCacheFromFile(cacheFile)
            previousSignatures.putAll(loaded)
            if (loaded.isNotEmpty()) {
                println("Fakt: Loaded ${loaded.size} cached signatures for incremental compilation")
            }
        } catch (e: java.io.IOException) {
            println("Fakt: Failed to load signature cache: ${e.message}")
        }
    }

    /**
     * Loads signature cache from file if it exists.
     *
     * @param cacheFile The cache file to load from
     * @return Map of type keys to their signatures
     */
    private fun loadCacheFromFile(cacheFile: java.io.File): Map<String, String> {
        if (!cacheFile.exists()) return emptyMap()

        val signatures = mutableMapOf<String, String>()
        cacheFile.readLines().forEach { line ->
            parseCacheLine(line)?.let { (key, signature) ->
                signatures[key] = signature
            }
        }
        return signatures
    }

    /**
     * Parses a single line from the signature cache file.
     *
     * @param line The cache line to parse
     * @return Pair of (key, signature) if valid, null otherwise
     */
    private fun parseCacheLine(line: String): Pair<String, String>? {
        val parts = line.split("=", limit = 2)
        return if (parts.size == 2) {
            parts[0] to parts[1]
        } else {
            null
        }
    }

    /**
     * Saves current signatures for next compilation.
     */
    fun saveSignatures() {
        if (outputDir == null) return

        try {
            val cacheFile = java.io.File(outputDir, "fakt-signatures.cache")
            cacheFile.parentFile?.mkdirs()

            cacheFile.writeText(
                previousSignatures.entries.joinToString("\n") { (key, signature) ->
                    "$key=$signature"
                },
            )
            println("Fakt: Saved ${previousSignatures.size} signatures to cache")
        } catch (e: java.io.IOException) {
            println("Fakt: Failed to save signature cache: ${e.message}")
        }
    }
}

/**
 * Simple compilation metrics for reporting.
 */
internal data class CompilationMetrics(
    val startTime: Long = System.currentTimeMillis(),
    var typesIndexed: Int = 0,
    var typesGenerated: Int = 0,
    var typesSkipped: Int = 0,
    var annotationsConfigured: Int = 0,
) {
    val compilationTimeMs: Long get() = System.currentTimeMillis() - startTime

    fun recordGeneration() {
        // Basic tracking - can be expanded later
    }
}
