// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.SourceSetExtractor
import com.rsicarelli.fakt.compiler.output.SourceSetMapper
import com.rsicarelli.fakt.compiler.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.types.ImportResolver
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
        return com.rsicarelli.fakt.compiler.utils
            .calculateLOC(combinedCode)
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
 */
internal class CodeGenerator(
    private val importResolver: ImportResolver,
    private val sourceSetMapper: SourceSetMapper,
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
            // Collect imports to calculate count
            val requiredImports = importResolver.collectRequiredImports(analysis, packageName)

            val generatedCode =
                GeneratedCode(
                    implementation = generators.implementation.generateImplementation(
                        analysis,
                        fakeClassName
                    ),
                    factory = generators.factory.generateFactoryFunction(analysis, fakeClassName),
                    configDsl = generators.configDsl.generateConfigurationDsl(
                        analysis,
                        fakeClassName
                    ),
                    importCount = requiredImports.size,
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
            // Collect imports to calculate count
            val requiredImports =
                importResolver.collectRequiredImportsForClass(analysis, packageName)

            val generatedCode =
                GeneratedCode(
                    implementation = generators.implementation.generateClassFake(
                        analysis,
                        fakeClassName
                    ),
                    factory = generators.factory.generateFactoryFunction(analysis, fakeClassName),
                    configDsl = generators.configDsl.generateConfigurationDsl(
                        analysis,
                        fakeClassName
                    ),
                    importCount = requiredImports.size,
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

        // Extract source set from IrClass file path (source of truth approach)
        val sourceSetName = SourceSetExtractor.extractSourceSet(sourceInterface)
        val outputDir = sourceSetMapper.getGeneratedSourcesDir(moduleFragment, sourceSetName)

        // Create subdirectories matching the package structure
        val packagePath = packageName.replace('.', '/')
        val packageDir = outputDir.resolve(packagePath)
        packageDir.mkdirs()
        val outputFile = packageDir.resolve("$fakeClassName.kt")

        // Collect all required imports from the interface analysis
        val requiredImports = importResolver.collectRequiredImports(analysis, packageName)

        val fullCode =
            buildString {
                appendLine("// Generated by Fakt - NoGenerics Pattern")
                appendLine("// Interface: $interfaceName")
                appendLine("package ${escapePackageName(packageName)}")
                appendLine()

                // Add StateFlow imports for call tracking
                appendLine("import kotlinx.coroutines.flow.MutableStateFlow")
                appendLine("import kotlinx.coroutines.flow.StateFlow")
                appendLine("import kotlinx.coroutines.flow.update")
                appendLine()

                // Add required imports
                if (requiredImports.isNotEmpty()) {
                    requiredImports.sorted().forEach { import ->
                        appendLine("import $import")
                    }
                    appendLine()
                }

                // Add implementation class
                append(code.implementation)
                appendLine()

                // Add factory function
                append(code.factory)
                appendLine()

                // Add configuration DSL
                append(code.configDsl)
                appendLine()
            }

        outputFile.writeText(fullCode)
    }

    /**
     * Writes the generated code for a class fake to the appropriate output file.
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

        // Extract source set from IrClass file path (source of truth approach)
        val sourceSetName = SourceSetExtractor.extractSourceSet(sourceClass)
        val outputDir = sourceSetMapper.getGeneratedSourcesDir(moduleFragment, sourceSetName)

        // Create subdirectories matching the package structure
        val packagePath = packageName.replace('.', '/')
        val packageDir = outputDir.resolve(packagePath)
        packageDir.mkdirs()
        val outputFile = packageDir.resolve("$fakeClassName.kt")

        // Collect required imports from class analysis
        // Classes need imports for return types, parameter types, etc.
        val requiredImports = importResolver.collectRequiredImportsForClass(analysis, packageName)

        val fullCode =
            buildString {
                appendLine("// Generated by Fakt - Class Fake")
                appendLine("// Class: $className")
                appendLine("package ${escapePackageName(packageName)}")
                appendLine()

                // Add StateFlow imports for call tracking
                appendLine("import kotlinx.coroutines.flow.MutableStateFlow")
                appendLine("import kotlinx.coroutines.flow.StateFlow")
                appendLine("import kotlinx.coroutines.flow.update")
                appendLine()

                // Add required imports
                if (requiredImports.isNotEmpty()) {
                    requiredImports.sorted().forEach { import ->
                        appendLine("import $import")
                    }
                    appendLine()
                }

                // Add implementation class
                append(code.implementation)
                appendLine()

                // Add factory function
                append(code.factory)
                appendLine()

                // Add configuration DSL
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
