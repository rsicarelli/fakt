// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.irnative.codegen

import dev.rsicarelli.ktfake.compiler.irnative.analysis.InterfaceAnalysis

/**
 * Abstract code generation engine for creating fake implementations.
 *
 * This module provides:
 * - Generic code generation framework
 * - Extensible output format support (IR, string, AST)
 * - Shared generation logic across different backends
 * - Template system for code patterns
 * - Plugin system for custom generators
 *
 * Key principle: Backend-agnostic code generation with pluggable output formats.
 */
interface CodeGenerator<T> {

    /**
     * Generate complete fake implementation for an interface.
     *
     * @param analysis Interface analysis results
     * @return Generated code in format T
     */
    fun generateFakeImplementation(analysis: InterfaceAnalysis): FakeImplementation<T>

    /**
     * Generate factory function for creating fake instances.
     *
     * @param analysis Interface analysis results
     * @return Generated factory in format T
     */
    fun generateFactory(analysis: InterfaceAnalysis): T

    /**
     * Generate configuration DSL for behavior setup.
     *
     * @param analysis Interface analysis results
     * @return Generated configuration DSL in format T
     */
    fun generateConfigurationDsl(analysis: InterfaceAnalysis): T

    /**
     * Generate call tracking implementation if enabled.
     *
     * @param analysis Interface analysis results
     * @return Generated call tracking code in format T
     */
    fun generateCallTracking(analysis: InterfaceAnalysis): T?

    /**
     * Get the output format for this generator.
     */
    val outputFormat: OutputFormat<T>
}

/**
 * Complete fake implementation including all generated components.
 */
data class FakeImplementation<T>(
    val implementationClass: T,
    val factoryFunction: T,
    val configurationClass: T,
    val callTrackingClasses: List<T> = emptyList()
)

/**
 * Output format specification for different code generation backends.
 */
interface OutputFormat<T> {
    /**
     * Combine multiple generated components into final output.
     */
    fun combine(components: List<T>): T

    /**
     * Render the generated code to string for debugging/inspection.
     */
    fun renderToString(code: T): String

    /**
     * Validate that generated code is syntactically correct.
     */
    fun validate(code: T): ValidationResult
}

/**
 * Generation context with shared state and utilities.
 */
interface GenerationContext {
    /**
     * Generate unique names to avoid conflicts.
     */
    fun generateUniqueName(baseName: String): String

    /**
     * Get import statements needed for generated code.
     */
    fun getRequiredImports(): List<String>

    /**
     * Add import statement to the context.
     */
    fun addImport(importPath: String)

    /**
     * Get package name for generated code.
     */
    fun getTargetPackage(): String
}

/**
 * Template system for generating repetitive code patterns.
 */
interface CodeTemplate<T> {
    /**
     * Apply template with provided parameters.
     */
    fun apply(parameters: Map<String, Any>): T

    /**
     * Get template parameter requirements.
     */
    fun getRequiredParameters(): List<String>
}

/**
 * Built-in templates for common patterns.
 */
object BuiltinTemplates {
    const val FACTORY_FUNCTION = "factory_function"
    const val IMPLEMENTATION_CLASS = "implementation_class"
    const val CONFIGURATION_DSL = "configuration_dsl"
    const val METHOD_OVERRIDE = "method_override"
    const val PROPERTY_OVERRIDE = "property_override"
    const val CALL_TRACKING = "call_tracking"
}

/**
 * Plugin system for extending code generation capabilities.
 */
interface CodeGeneratorPlugin<T> {
    /**
     * Plugin identifier.
     */
    val id: String

    /**
     * Generate additional code for specific scenarios.
     */
    fun generate(analysis: InterfaceAnalysis, context: GenerationContext): List<T>

    /**
     * Check if plugin should be applied to this interface.
     */
    fun shouldApply(analysis: InterfaceAnalysis): Boolean
}

/**
 * Validation results for generated code.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
}
