// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.optimization

import com.rsicarelli.fakt.compiler.CompilationMetrics
import com.rsicarelli.fakt.compiler.CompilerOptimizations
import com.rsicarelli.fakt.compiler.TypeInfo

/**
 * Refactored implementation of [CompilerOptimizations] with clear separation of concerns.
 *
 * This implementation delegates to specialized components:
 * - [SignatureCache] for persistent signature storage
 * - [ChangeDetector] for signature comparison logic
 * - Local metrics collection for reporting
 *
 * Benefits of this architecture:
 * - Single Responsibility Principle: each class has one job
 * - Easy to test: components can be tested in isolation
 * - Easy to extend: new caching strategies or change detection logic
 * - Better maintainability: clear boundaries between concerns
 *
 * @param fakeAnnotations List of fully qualified annotation names to process
 * @param outputDir Optional output directory for cache files
 *
 * @since 1.0.0
 */
internal class IncrementalCompiler(
    private val fakeAnnotations: List<String>,
    outputDir: String? = null,
) : CompilerOptimizations {
    /** Persistent signature cache */
    private val signatureCache = SignatureCache(outputDir)

    /** Change detection logic */
    private val changeDetector = ChangeDetector()

    /** Index of all discovered types for efficient annotation-based lookup */
    private val indexedTypes = mutableListOf<TypeInfo>()

    /** Set of signatures for types that have been successfully generated in this session */
    private val generatedTypes = mutableSetOf<String>()

    /** Compilation metrics for simple reporting */
    private val metrics = CompilationMetrics()

    override fun isConfiguredFor(annotation: String): Boolean = annotation in fakeAnnotations

    override fun indexType(type: TypeInfo) {
        indexedTypes.add(type)
    }

    override fun findTypesWithAnnotation(annotation: String): List<TypeInfo> =
        indexedTypes.filter { type ->
            type.annotations.any { it == annotation }
        }

    override fun needsRegeneration(type: TypeInfo): Boolean {
        val cacheKey = changeDetector.generateCacheKey(type)
        val cachedSignature = signatureCache.getSignature(cacheKey)
        return changeDetector.needsRegeneration(type, cachedSignature)
    }

    override fun recordGeneration(type: TypeInfo) {
        val cacheKey = changeDetector.generateCacheKey(type)

        // Update both in-memory tracking and persistent cache
        generatedTypes.add(type.signature)
        signatureCache.putSignature(cacheKey, type.signature)

        // Update metrics
        metrics.recordGeneration()
    }

    /**
     * Get current compilation metrics for reporting.
     */
    fun getMetrics(): CompilationMetrics =
        metrics.copy(
            typesIndexed = indexedTypes.size,
            typesGenerated = generatedTypes.size,
            typesSkipped = indexedTypes.size - generatedTypes.size,
            annotationsConfigured = fakeAnnotations.size,
        )

    /**
     * Saves signatures to persistent cache for next compilation.
     */
    fun saveSignatures() {
        signatureCache.save()
    }

    /**
     * Generate a simple JSON report file.
     */
    fun generateReport(outputDir: String?) {
        if (outputDir == null) return

        try {
            val reportMetrics = getMetrics()
            val reportData =
                mapOf(
                    "timestamp" to System.currentTimeMillis(),
                    "date" to
                        java.time.LocalDateTime
                            .now()
                            .toString(),
                    "compilation" to
                        mapOf(
                            "typesIndexed" to reportMetrics.typesIndexed,
                            "typesGenerated" to reportMetrics.typesGenerated,
                            "typesSkipped" to reportMetrics.typesSkipped,
                            "compilationTimeMs" to reportMetrics.compilationTimeMs,
                            "annotationsConfigured" to reportMetrics.annotationsConfigured,
                        ),
                    "annotations" to
                        mapOf(
                            "configured" to fakeAnnotations,
                            "discovered" to
                                indexedTypes
                                    .groupBy { type ->
                                        type.annotations.firstOrNull { isConfiguredFor(it) } ?: "unknown"
                                    }.mapValues { it.value.size },
                        ),
                    "types" to
                        indexedTypes.map { type ->
                            mapOf(
                                "name" to type.name,
                                "package" to type.packageName,
                                "file" to type.fileName,
                                "annotations" to type.annotations,
                                "generated" to (type.signature in generatedTypes),
                            )
                        },
                )

            val reportJson =
                buildString {
                    append("{\n")
                    reportData.entries.forEachIndexed { index, (key, value) ->
                        append("  \"$key\": ")
                        append(formatJsonValue(value, 2))
                        if (index < reportData.size - 1) append(",")
                        append("\n")
                    }
                    append("}")
                }

            val reportFile = java.io.File(outputDir, "ktfakes-report.json")
            reportFile.parentFile?.mkdirs()
            reportFile.writeText(reportJson)

            println("KtFakes: Report generated at ${reportFile.absolutePath}")
        } catch (e: Exception) {
            println("KtFakes: Failed to generate report: ${e.message}")
        }
    }

    /**
     * Gets cache statistics for debugging.
     */
    fun getCacheStats(): Map<String, Any> =
        mapOf(
            "cacheSize" to signatureCache.size(),
            "indexedTypes" to indexedTypes.size,
            "generatedThisSession" to generatedTypes.size,
            "annotationsConfigured" to fakeAnnotations.size,
        )

    private fun formatJsonValue(
        value: Any?,
        indent: Int,
    ): String {
        val indentStr = " ".repeat(indent)
        return when (value) {
            is String -> "\"$value\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is List<*> -> {
                if (value.isEmpty()) {
                    "[]"
                } else {
                    "[\n${value.joinToString(",\n") { "$indentStr  ${formatJsonValue(it, indent + 2)}" }}\n$indentStr]"
                }
            }
            is Map<*, *> -> {
                if (value.isEmpty()) {
                    "{}"
                } else {
                    "{\n${value.entries.joinToString(",\n") { (k, v) ->
                        "$indentStr  \"$k\": ${formatJsonValue(v, indent + 2)}"
                    }}\n$indentStr}"
                }
            }
            null -> "null"
            else -> "\"$value\""
        }
    }
}
