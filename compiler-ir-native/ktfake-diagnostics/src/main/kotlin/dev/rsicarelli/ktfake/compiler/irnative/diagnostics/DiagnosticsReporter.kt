// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.irnative.diagnostics

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

/**
 * Structured error handling and diagnostics reporting for IR-Native compiler plugin.
 *
 * This module provides:
 * - Structured error hierarchy with contextual information
 * - IDE-friendly error messages with suggested fixes
 * - Performance monitoring and compilation metrics
 * - Debug logging with configurable verbosity
 * - Integration with Kotlin compiler diagnostics system
 *
 * Key principle: Rich, actionable error messages that help developers fix issues quickly.
 */
interface DiagnosticsReporter {

    /**
     * Report an error during compilation.
     *
     * @param error Structured error information
     */
    fun reportError(error: KtFakeError)

    /**
     * Report a warning during compilation.
     *
     * @param warning Warning information
     */
    fun reportWarning(warning: KtFakeWarning)

    /**
     * Report informational message.
     *
     * @param info Information to report
     */
    fun reportInfo(info: String)

    /**
     * Report debug information (only when debug enabled).
     *
     * @param debug Debug message
     */
    fun reportDebug(debug: String)

    /**
     * Check if debug reporting is enabled.
     */
    val isDebugEnabled: Boolean
}

/**
 * Structured error hierarchy with contextual information and suggested fixes.
 */
sealed class KtFakeError(
    val message: String,
    val severity: CompilerMessageSeverity = CompilerMessageSeverity.ERROR,
    val sourceLocation: SourceLocation? = null,
    val suggestedFixes: List<String> = emptyList()
) {

    /**
     * @Fake annotation applied to objects (not allowed - thread safety).
     */
    data class FakeObjectNotAllowed(
        val className: String,
        val location: SourceLocation
    ) : KtFakeError(
        message = "@Fake annotation cannot be applied to objects. Use class or interface instead to ensure thread safety.",
        sourceLocation = location,
        suggestedFixes = listOf(
            "Convert '$className' from object to class",
            "Convert '$className' from object to interface",
            "Remove @Fake annotation from '$className'"
        )
    )

    /**
     * Circular dependency detected in @Fake dependencies.
     */
    data class CircularDependency(
        val cycle: List<String>,
        val location: SourceLocation
    ) : KtFakeError(
        message = "Circular dependency detected in @Fake dependencies: ${cycle.joinToString(" -> ")}",
        sourceLocation = location,
        suggestedFixes = listOf(
            "Remove one of the dependencies: ${cycle.joinToString(", ")}",
            "Restructure dependencies to avoid circular references"
        )
    )

    /**
     * Missing @Fake dependency - referenced type not available.
     */
    data class MissingFakeDependency(
        val typeName: String,
        val referencingInterface: String,
        val similarTypes: List<String>,
        val location: SourceLocation
    ) : KtFakeError(
        message = buildString {
            appendLine("No @Fake available for '$typeName' referenced by '$referencingInterface'")
            if (similarTypes.isNotEmpty()) {
                appendLine("Similar available fakes:")
                similarTypes.forEach { appendLine("  - $it") }
            }
        },
        sourceLocation = location,
        suggestedFixes = buildList {
            add("Add @Fake annotation to '$typeName'")
            if (similarTypes.isNotEmpty()) {
                add("Use one of the available fakes: ${similarTypes.joinToString(", ")}")
            }
            add("Remove '$typeName' from dependencies list")
        }
    )

    /**
     * Interface has no methods or properties - nothing to fake.
     */
    data class EmptyInterface(
        val interfaceName: String,
        val location: SourceLocation
    ) : KtFakeError(
        message = "Interface '$interfaceName' has no methods or properties to fake",
        sourceLocation = location,
        suggestedFixes = listOf(
            "Add methods to '$interfaceName'",
            "Remove @Fake annotation from empty interface"
        )
    )

    /**
     * Unsupported method signature (complex generics, etc.).
     */
    data class UnsupportedMethodSignature(
        val methodName: String,
        val interfaceName: String,
        val reason: String,
        val location: SourceLocation
    ) : KtFakeError(
        message = "Cannot generate fake for method '$methodName' in '$interfaceName': $reason",
        sourceLocation = location,
        suggestedFixes = listOf(
            "Simplify the method signature",
            "Exclude '$methodName' using @FakeConfig(exclude = [\"$methodName\"])",
            "Provide custom implementation for '$methodName'"
        )
    )
}

/**
 * Warning messages for non-critical issues.
 */
sealed class KtFakeWarning(
    val message: String,
    val sourceLocation: SourceLocation? = null
) {

    /**
     * Performance warning for large interfaces.
     */
    data class LargeInterface(
        val interfaceName: String,
        val methodCount: Int,
        val location: SourceLocation
    ) : KtFakeWarning(
        message = "Interface '$interfaceName' has $methodCount methods. Consider breaking it into smaller interfaces for better fake performance.",
        sourceLocation = location
    )

    /**
     * Unused @FakeConfig options.
     */
    data class UnusedConfigOption(
        val option: String,
        val interfaceName: String,
        val location: SourceLocation
    ) : KtFakeWarning(
        message = "Unused @FakeConfig option '$option' on interface '$interfaceName'",
        sourceLocation = location
    )
}

/**
 * Source location information for error reporting.
 */
data class SourceLocation(
    val file: String,
    val line: Int,
    val column: Int,
    val length: Int = 0
)

/**
 * Performance and compilation metrics.
 */
data class CompilationMetrics(
    val interfacesProcessed: Int,
    val fakesGenerated: Int,
    val generationTimeMs: Long,
    val irNodesCreated: Int,
    val errorsReported: Int,
    val warningsReported: Int
)

/**
 * Default implementation using Kotlin compiler MessageCollector.
 */
class DefaultDiagnosticsReporter(
    private val messageCollector: MessageCollector,
    override val isDebugEnabled: Boolean = false
) : DiagnosticsReporter {

    private val metrics = mutableMapOf<String, Any>()

    override fun reportError(error: KtFakeError) {
        val location = error.sourceLocation
        val fullMessage = buildString {
            appendLine(error.message)
            if (error.suggestedFixes.isNotEmpty()) {
                appendLine()
                appendLine("Suggested fixes:")
                error.suggestedFixes.forEach { fix ->
                    appendLine("  - $fix")
                }
            }
        }

        messageCollector.report(
            error.severity,
            fullMessage,
            location?.let {
                // TODO: Convert to proper Kotlin compiler location format
                null
            }
        )
    }

    override fun reportWarning(warning: KtFakeWarning) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            warning.message
        )
    }

    override fun reportInfo(info: String) {
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "KtFakes: $info"
        )
    }

    override fun reportDebug(debug: String) {
        if (isDebugEnabled) {
            messageCollector.report(
                CompilerMessageSeverity.INFO,
                "KtFakes [DEBUG]: $debug"
            )
        }
    }

    /**
     * Record metric for performance monitoring.
     */
    fun recordMetric(key: String, value: Any) {
        metrics[key] = value
    }

    /**
     * Get compilation metrics summary.
     */
    fun getMetrics(): Map<String, Any> = metrics.toMap()
}
