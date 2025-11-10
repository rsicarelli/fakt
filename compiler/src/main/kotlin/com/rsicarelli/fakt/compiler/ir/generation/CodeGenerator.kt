// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.renderTo
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.core.telemetry.calculateLOC
import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.core.context.ImportResolver
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
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
 * @property importCount The number of imports required for this fake
 */
internal data class GeneratedCode(
    val implementation: String,
    val factory: String,
    val configDsl: String,
    val importCount: Int,
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

    /**
     * Calculates total file size in bytes.
     *
     * @return Total size of all generated code
     */
    fun calculateTotalBytes(): Int = implementation.length + factory.length + configDsl.length
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
    /**
     * Generates complete fake implementation including class, factory, and configuration DSL.
     *
     * @param sourceInterface The interface to generate a fake for
     * @param analysis The analyzed interface metadata
     * @param moduleFragment The module context for output directory resolution
     */
    fun generateWorkingFakeImplementation(
        sourceInterface: IrClass,
        analysis: InterfaceAnalysis,
        moduleFragment: IrModuleFragment,
    ): GeneratedCode {
        val interfaceName = analysis.interfaceName
        val fakeClassName = "Fake${interfaceName}Impl"
        val packageName = sourceInterface.packageFqName?.asString() ?: ""

        try {
            // Collect required imports for implementation
            val requiredImports = importResolver.collectRequiredImports(analysis, packageName)

            // Generate implementation + factory
            val generated = generators.implementation.generateImplementation(
                analysis,
                packageName,
                requiredImports.toList()
            )

            // Render CodeFile to string
            val builder = CodeBuilder()
            generated.implementationFile.renderTo(builder)
            val implementationCode = builder.build()

            // Assemble final code
            val generatedCode =
                GeneratedCode(
                    implementation = implementationCode, // Complete file with package + imports
                    factory = generated.factoryFunction,
                    configDsl = generators.configDsl.generateConfigurationDsl(
                        analysis,
                        fakeClassName
                    ),
                    importCount = 0, // TODO: Remove (imports now in implementationCode)
                )

            writeGeneratedCode(
                sourceInterface = sourceInterface,
                moduleFragment = moduleFragment,
                context =
                    WriteContext(
                        packageName = packageName,
                        fakeClassName = fakeClassName,
                        interfaceName = interfaceName,
                    ),
                analysis = analysis,
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
     * @param moduleFragment The module context for output directory resolution
     */
    fun generateWorkingClassFake(
        sourceClass: IrClass,
        analysis: ClassAnalysis,
        moduleFragment: IrModuleFragment,
    ): GeneratedCode {
        val className = analysis.className
        val fakeClassName = "Fake${className}Impl"
        val packageName = sourceClass.packageFqName?.asString() ?: ""

        try {
            // Collect required imports for implementation
            val requiredImports = importResolver.collectRequiredImportsForClass(analysis, packageName)

            // Generate implementation + factory
            val generated = generators.implementation.generateClassFake(
                analysis,
                packageName,
                requiredImports.toList()
            )

            // Render CodeFile to string
            val builder = CodeBuilder()
            generated.implementationFile.renderTo(builder)
            val implementationCode = builder.build()

            // Assemble final code
            val generatedCode =
                GeneratedCode(
                    implementation = implementationCode, // Complete file with package + imports
                    factory = generated.factoryFunction,
                    configDsl = generators.configDsl.generateConfigurationDsl(
                        analysis,
                        fakeClassName
                    ),
                    importCount = 0, // TODO: Remove (imports now in implementationCode)
                )

            writeGeneratedCodeForClass(
                sourceClass = sourceClass,
                moduleFragment = moduleFragment,
                context =
                    WriteContext(
                        packageName = packageName,
                        fakeClassName = fakeClassName,
                        interfaceName = className, // Reuse interfaceName field for class name
                    ),
                analysis = analysis,
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
        sourceInterface: IrClass,
        moduleFragment: IrModuleFragment,
        context: WriteContext,
        analysis: InterfaceAnalysis,
        code: GeneratedCode,
    ) {
        val packageName = context.packageName
        val fakeClassName = context.fakeClassName
        val interfaceName = context.interfaceName

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

        outputFile.writeText(fullCode)
    }

    /**
     * Writes the generated code for a class fake to the appropriate output file.
     *
     * Uses Gradle-provided output directory from SourceSetContext (no pattern matching needed).
     */
    private fun writeGeneratedCodeForClass(
        sourceClass: IrClass,
        moduleFragment: IrModuleFragment,
        context: WriteContext,
        analysis: ClassAnalysis,
        code: GeneratedCode,
    ) {
        val packageName = context.packageName
        val fakeClassName = context.fakeClassName
        val className = context.interfaceName // Reused field name

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

        outputFile.writeText(fullCode)
    }

    /**
     * Escapes package name segments that need backticks in Kotlin.
     *
     * Package segments need backticks if they:
     * - Start with a digit (e.g., "3_properties")
     * - Are Kotlin keywords
     * - Contain special characters
     *
     * @param packageName The fully-qualified package name
     * @return The package name with properly escaped segments
     */
    private fun escapePackageName(packageName: String): String {
        if (packageName.isEmpty()) return packageName

        return packageName
            .split('.')
            .joinToString(".") { segment ->
                when {
                    // Check if segment starts with a digit
                    segment.firstOrNull()?.isDigit() == true -> "`$segment`"
                    // Check if segment is a Kotlin keyword
                    segment in KOTLIN_KEYWORDS -> "`$segment`"
                    // Otherwise, no escaping needed
                    else -> segment
                }
            }
    }

    companion object {
        /**
         * List of Kotlin keywords that need backtick escaping in package names.
         */
        private val KOTLIN_KEYWORDS =
            setOf(
                "as",
                "break",
                "class",
                "continue",
                "do",
                "else",
                "false",
                "for",
                "fun",
                "if",
                "in",
                "interface",
                "is",
                "null",
                "object",
                "package",
                "return",
                "super",
                "this",
                "throw",
                "true",
                "try",
                "typealias",
                "typeof",
                "val",
                "var",
                "when",
                "while",
            )
    }
}
