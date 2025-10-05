// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.output.SourceSetMapper
import com.rsicarelli.fakt.compiler.types.ImportResolver
import com.rsicarelli.fakt.compiler.types.TypeResolver
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
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
)

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
 * @since 1.0.0
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class CodeGenerator(
    @Suppress("UnusedPrivateProperty") // Used by importResolver dependency injection
    private val typeResolver: TypeResolver,
    private val importResolver: ImportResolver,
    private val sourceSetMapper: SourceSetMapper,
    private val generators: CodeGenerators,
    private val messageCollector: MessageCollector?,
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
    ) {
        val interfaceName = analysis.interfaceName
        val fakeClassName = "Fake${interfaceName}Impl"
        val packageName = sourceInterface.packageFqName?.asString() ?: ""

        messageCollector?.reportInfo("Fakt: Generating fake for interface $interfaceName")

        try {
            val generatedCode =
                GeneratedCode(
                    implementation = generators.implementation.generateImplementation(analysis, fakeClassName),
                    factory = generators.factory.generateFactoryFunction(analysis, fakeClassName),
                    configDsl = generators.configDsl.generateConfigurationDsl(analysis, fakeClassName),
                )

            writeGeneratedCode(
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

            messageCollector?.reportInfo("Fakt: Successfully generated fake for $interfaceName -> $fakeClassName")
        } catch (e: Exception) {
            // Top-level error boundary: Catch all exceptions during code generation
            // This is a legitimate use of generic exception handling to provide context
            // We log the error with interface name for debugging, then re-throw to fail fast
            messageCollector?.reportError("Fakt: Failed to generate fake for $interfaceName: ${e.message}")
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
    ) {
        val className = analysis.className
        val fakeClassName = "Fake${className}Impl"
        val packageName = sourceClass.packageFqName?.asString() ?: ""

        messageCollector?.reportInfo("Fakt: Generating fake for class $className")

        try {
            val generatedCode =
                GeneratedCode(
                    implementation = generators.implementation.generateClassFake(analysis, fakeClassName),
                    factory = generators.factory.generateFactoryFunction(analysis, fakeClassName),
                    configDsl = generators.configDsl.generateConfigurationDsl(analysis, fakeClassName),
                )

            writeGeneratedCodeForClass(
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

            messageCollector?.reportInfo("Fakt: Successfully generated fake for class $className -> $fakeClassName")
        } catch (e: Exception) {
            messageCollector?.reportError("Fakt: Failed to generate fake for class $className: ${e.message}")
            throw e
        }
    }

    /**
     * Writes the generated code to the appropriate output file.
     */
    private fun writeGeneratedCode(
        moduleFragment: IrModuleFragment,
        context: WriteContext,
        analysis: InterfaceAnalysis,
        code: GeneratedCode,
    ) {
        val packageName = context.packageName
        val fakeClassName = context.fakeClassName
        val interfaceName = context.interfaceName
        val outputDir = sourceSetMapper.getGeneratedSourcesDir(moduleFragment)

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
                appendLine("package $packageName")
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
                appendLine()

                // Add factory function
                append(code.factory)
                appendLine()
                appendLine()

                // Add configuration DSL
                append(code.configDsl)
                appendLine()
            }

        outputFile.writeText(fullCode)

        messageCollector?.reportInfo("Fakt: Generated fake written to: ${outputFile.absolutePath}")
    }

    /**
     * Writes the generated code for a class fake to the appropriate output file.
     */
    private fun writeGeneratedCodeForClass(
        moduleFragment: IrModuleFragment,
        context: WriteContext,
        analysis: ClassAnalysis,
        code: GeneratedCode,
    ) {
        val packageName = context.packageName
        val fakeClassName = context.fakeClassName
        val className = context.interfaceName // Reused field name
        val outputDir = sourceSetMapper.getGeneratedSourcesDir(moduleFragment)

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
                appendLine("package $packageName")
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
                appendLine()

                // Add factory function
                append(code.factory)
                appendLine()
                appendLine()

                // Add configuration DSL
                append(code.configDsl)
                appendLine()
            }

        outputFile.writeText(fullCode)

        messageCollector?.reportInfo("Fakt: Generated class fake written to: ${outputFile.absolutePath}")
    }

    private fun MessageCollector.reportInfo(message: String) {
        this.report(
            org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO,
            message,
            null,
        )
    }

    private fun MessageCollector.reportError(message: String) {
        this.report(
            org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR,
            message,
            null,
        )
    }
}
