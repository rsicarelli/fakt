// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.optimization

import com.rsicarelli.fakt.compiler.types.TypeInfo

/**
 * Generates JSON reports for compilation metrics.
 * Extracted from IncrementalCompiler to reduce function count and improve separation of concerns.
 *
 * @since 1.0.0
 */
internal class JsonReportGenerator {
    /**
     * Generates report data structure for compilation metrics.
     *
     * @param metrics The compilation metrics to include in the report
     * @param fakeAnnotations List of configured fake annotations
     * @param indexedTypes All indexed types from compilation
     * @param generatedTypes Set of signatures for generated types
     * @return Map representing the JSON report structure
     */
    fun generateReportData(
        metrics: CompilationMetrics,
        fakeAnnotations: List<String>,
        indexedTypes: List<TypeInfo>,
        generatedTypes: Set<String>,
    ): Map<String, Any?> =
        mapOf(
            "timestamp" to System.currentTimeMillis(),
            "date" to
                java.time.LocalDateTime
                    .now()
                    .toString(),
            "compilation" to
                mapOf(
                    "typesIndexed" to metrics.typesIndexed,
                    "typesGenerated" to metrics.typesGenerated,
                    "typesSkipped" to metrics.typesSkipped,
                    "compilationTimeMs" to metrics.compilationTimeMs,
                    "annotationsConfigured" to metrics.annotationsConfigured,
                ),
            "annotations" to
                mapOf(
                    "configured" to fakeAnnotations,
                    "discovered" to
                        indexedTypes
                            .groupBy { type ->
                                type.annotations.firstOrNull { it in fakeAnnotations } ?: "unknown"
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

    /**
     * Formats a value as JSON string with proper indentation.
     *
     * @param value The value to format (String, Number, Boolean, List, Map, or null)
     * @param indent Current indentation level (number of spaces)
     * @return JSON-formatted string representation
     */
    fun formatJsonValue(
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

    /**
     * Converts report data to formatted JSON string.
     *
     * @param reportData The report data structure
     * @return Formatted JSON string
     */
    fun toJsonString(reportData: Map<String, Any?>): String =
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
}
