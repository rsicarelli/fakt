// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.renderTo
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.core.context.ImportResolver
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.core.telemetry.calculateLOC
import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.packageFqName

/**
 * Groups the code generators used by CodeGenerator.
 *
 * @property implementation Generator for fake implementation classes
 * @property factory Generator for factory functions
 * @property configDsl Generator for configuration DSL
 */
internal data class CodeGenerators(
    val implementation: ImplementationGenerator,
    val factory: FactoryGenerator,
    val configDsl: ConfigurationDslGenerator,
)

/**
 * Contains all generated code pieces for a fake implementation.
 *
 * @property implementation The generated implementation class code
 * @property factory The generated factory function code
 * @property configDsl The generated configuration DSL code
 */
internal data class GeneratedCode(
    val implementation: String,
    val factory: String,
    val configDsl: String,
) {
    /**
     * Calculates total lines of code across all generated components.
     *
     * @return Total non-blank, non-comment lines of code
     */
    fun calculateTotalLOC(): Int {
        val combinedCode = "$implementation\n$factory\n$configDsl"
        return calculateLOC(combinedCode)
    }
}

/**
 * Contains metadata for writing generated code to a file.
 *
 * @property packageName The package name for the generated code
 * @property fakeClassName The name of the fake implementation class
 * @property interfaceName The original interface name
 */
internal data class WriteContext(
    val packageName: String,
    val fakeClassName: String,
    val interfaceName: String,
)

/**
 * Handles code generation for fake implementations.
 * Orchestrates the generation of implementation classes, factory functions, and configuration DSLs.
 *
 * @property importResolver Resolves import statements for generated code
 * @property sourceSetContext Context with compilation metadata from Gradle plugin
 * @property generators Code generation modules (implementation, factory, DSL)
 * @property logger Logger for compilation feedback
 */
internal class CodeGenerator(
    private val importResolver: ImportResolver,
    private val sourceSetContext: SourceSetContext,
    private val generators: CodeGenerators,
    private val logger: FaktLogger,
) {
    private companion object {
        /** Base overhead for generated code (package, imports, class header). */
        const val CODE_SIZE_BASE_OVERHEAD = 500

        /** Estimated characters per method (call tracking + behavior + override + config). */
        const val CODE_SIZE_PER_METHOD = 200

        /** Estimated characters per property (call tracking + behavior + override + config). */
        const val CODE_SIZE_PER_PROPERTY = 100

        /** Estimated characters per import statement. */
        const val CODE_SIZE_PER_IMPORT = 30
    }

    /**
     * Generates complete fake implementation including class, factory, and configuration DSL.
     *
     * @param sourceInterface The interface to generate a fake for
     * @param analysis The analyzed interface metadata
     */
    fun generateWorkingFakeImplementation(
        sourceInterface: IrClass,
        analysis: InterfaceAnalysis,
    ): GeneratedCode {
        val interfaceName = analysis.interfaceName
        val fakeClassName = "Fake${interfaceName}Impl"
        val packageName = sourceInterface.packageFqName?.asString() ?: ""

        try {
            // Collect required imports for implementation
            val requiredImports = importResolver.collectRequiredImports(analysis, packageName)

            // Generate implementation + factory
            val generated =
                generators.implementation.generateImplementation(
                    analysis,
                    packageName,
                    requiredImports.toList(),
                )

            // Render CodeFile to string with capacity estimation for performance
            val estimatedCapacity =
                estimateCodeSize(
                    methodCount = analysis.functions.size,
                    propertyCount = analysis.properties.size,
                    importCount = requiredImports.size,
                )
            val builder = CodeBuilder(builder = StringBuilder(estimatedCapacity))
            generated.implementationFile.renderTo(builder)
            val implementationCode = builder.build()

            // Assemble final code
            val generatedCode =
                GeneratedCode(
                    implementation = implementationCode, // Complete file with package + imports
                    factory = generated.factoryFunction,
                    configDsl =
                        generators.configDsl.generateConfigurationDsl(
                            analysis,
                            fakeClassName,
                        ),
                )

            writeGeneratedCode(
                context =
                    WriteContext(
                        packageName = packageName,
                        fakeClassName = fakeClassName,
                        interfaceName = interfaceName,
                    ),
                code = generatedCode,
            )

            return generatedCode
        } catch (e: Exception) {
            // Top-level error boundary: Catch all exceptions during code generation
            // This is a legitimate use of generic exception handling to provide context
            // We log the error with interface name for debugging, then re-throw to fail fast
            logger.error("Failed to generate fake for $interfaceName: ${e.message}")
            throw e
        }
    }

    /**
     * Generates complete fake implementation for a class including implementation, factory, and configuration DSL.
     *
     * @param sourceClass The class to generate a fake for
     * @param analysis The analyzed class metadata
     */
    fun generateWorkingClassFake(
        sourceClass: IrClass,
        analysis: ClassAnalysis,
    ): GeneratedCode {
        val className = analysis.className
        val fakeClassName = "Fake${className}Impl"
        val packageName = sourceClass.packageFqName?.asString() ?: ""

        try {
            // Collect required imports for implementation
            val requiredImports =
                importResolver.collectRequiredImportsForClass(analysis, packageName)

            // Generate implementation + factory
            val generated =
                generators.implementation.generateClassFake(
                    analysis,
                    packageName,
                    requiredImports.toList(),
                )

            // Render CodeFile to string with capacity estimation for performance
            val totalMethods = analysis.abstractMethods.size + analysis.openMethods.size
            val totalProperties = analysis.abstractProperties.size + analysis.openProperties.size
            val estimatedCapacity =
                estimateCodeSize(
                    methodCount = totalMethods,
                    propertyCount = totalProperties,
                    importCount = requiredImports.size,
                )
            val builder = CodeBuilder(builder = StringBuilder(estimatedCapacity))
            generated.implementationFile.renderTo(builder)
            val implementationCode = builder.build()

            // Assemble final code
            val generatedCode =
                GeneratedCode(
                    implementation = implementationCode, // Complete file with package + imports
                    factory = generated.factoryFunction,
                    configDsl =
                        generators.configDsl.generateConfigurationDsl(
                            analysis,
                            fakeClassName,
                        ),
                )

            writeGeneratedCodeForClass(
                context =
                    WriteContext(
                        packageName = packageName,
                        fakeClassName = fakeClassName,
                        interfaceName = className, // Reuse interfaceName field for class name
                    ),
                code = generatedCode,
            )

            return generatedCode
        } catch (e: Exception) {
            logger.error("Failed to generate fake for class $className: ${e.message}")
            throw e
        }
    }

    /**
     * Writes the generated code to the appropriate output file.
     *
     * Uses Gradle-provided output directory from SourceSetContext (no pattern matching needed).
     */
    private fun writeGeneratedCode(
        context: WriteContext,
        code: GeneratedCode,
    ) {
        val packageName = context.packageName
        val fakeClassName = context.fakeClassName

        // Use Gradle-provided output directory (authoritative source from build configuration)
        val outputDir = java.io.File(sourceSetContext.outputDirectory)

        // Create subdirectories matching the package structure
        val packagePath = packageName.replace('.', '/')
        val packageDir = outputDir.resolve(packagePath)
        packageDir.mkdirs()
        val outputFile = packageDir.resolve("$fakeClassName.kt")

        // Implementation now includes package + imports via DSL
        // Factory and configDSL are still old generators (no package/imports)
        val fullCode =
            buildString {
                // Implementation already has: package, imports, class (from DSL)
                append(code.implementation)
                appendLine()

                // Add factory function (no package/imports)
                append(code.factory)
                appendLine()

                // Add configuration DSL (no package/imports)
                append(code.configDsl)
                appendLine()
            }

        // Use buffered writer for better I/O performance
        outputFile.bufferedWriter().use { writer ->
            writer.write(fullCode)
        }
    }

    /**
     * Writes the generated code for a class fake to the appropriate output file.
     *
     * Uses Gradle-provided output directory from SourceSetContext (no pattern matching needed).
     */
    private fun writeGeneratedCodeForClass(
        context: WriteContext,
        code: GeneratedCode,
    ) {
        val packageName = context.packageName
        val fakeClassName = context.fakeClassName

        // Use Gradle-provided output directory (authoritative source from build configuration)
        val outputDir = java.io.File(sourceSetContext.outputDirectory)

        // Create subdirectories matching the package structure
        val packagePath = packageName.replace('.', '/')
        val packageDir = outputDir.resolve(packagePath)
        packageDir.mkdirs()
        val outputFile = packageDir.resolve("$fakeClassName.kt")

        // Implementation now includes package + imports via DSL
        // Factory and configDSL are still old generators (no package/imports)
        val fullCode =
            buildString {
                // Implementation already has: package, imports, class (from DSL)
                append(code.implementation)
                appendLine()

                // Add factory function (no package/imports)
                append(code.factory)
                appendLine()

                // Add configuration DSL (no package/imports)
                append(code.configDsl)
                appendLine()
            }

        // Use buffered writer for better I/O performance
        outputFile.bufferedWriter().use { writer ->
            writer.write(fullCode)
        }
    }

    /**
     * Estimates the size of generated code for StringBuilder capacity hint.
     *
     * This avoids StringBuilder reallocation during code generation,
     * improving performance by 2-3%.
     *
     * Heuristic based on typical fake structure:
     * - Base overhead: 500 chars (package, imports, class header)
     * - Per method: ~200 chars (call tracking + behavior property + override + config)
     * - Per property: ~100 chars (call tracking + behavior + override + config)
     * - Per import: ~30 chars average
     *
     * @param methodCount Number of methods in the interface
     * @param propertyCount Number of properties in the interface
     * @param importCount Number of import statements
     * @return Estimated capacity in characters
     */
    private fun estimateCodeSize(
        methodCount: Int,
        propertyCount: Int,
        importCount: Int,
    ): Int =
        CODE_SIZE_BASE_OVERHEAD +
            (methodCount * CODE_SIZE_PER_METHOD) +
            (propertyCount * CODE_SIZE_PER_PROPERTY) +
            (importCount * CODE_SIZE_PER_IMPORT)
}
