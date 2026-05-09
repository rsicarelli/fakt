// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.codegen.analysis.FakeDeclaration
import com.rsicarelli.fakt.codegen.generator.ConfigurationDslGenerator
import com.rsicarelli.fakt.codegen.generator.ImplementationGenerator
import com.rsicarelli.fakt.codegen.model.CodeFile
import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.renderDeclarationsOnly
import com.rsicarelli.fakt.codegen.renderer.renderHeaderOnly
import com.rsicarelli.fakt.codegen.renderer.renderTo
import com.rsicarelli.fakt.codegen.renderer.renderToString
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.core.context.ImportResolver
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.core.telemetry.calculateLOC

/**
 * Groups the code generators used by CodeGenerator.
 *
 * @property implementation Generator for fake implementation classes
 * @property configDsl Generator for configuration DSL
 */
internal data class CodeGenerators(
    val implementation: ImplementationGenerator,
    val configDsl: ConfigurationDslGenerator,
)

/**
 * Contains all generated code pieces for a fake implementation.
 *
 * Uses CodeFile for type-safe, composable code generation throughout the pipeline.
 *
 * @property implementation The generated implementation class (includes call history components)
 * @property factory The generated factory function
 * @property configDsl The generated configuration DSL class
 */
internal data class GeneratedCode(
    val implementation: CodeFile,
    val factory: CodeFile,
    val configDsl: CodeFile,
) {
    /**
     * Calculates total lines of code across all generated components.
     *
     * @return Total non-blank, non-comment lines of code
     */
    fun calculateTotalLOC(): Int {
        val combinedCode = buildString {
            append(implementation.renderToString())
            appendLine()
            append(factory.renderToString())
            appendLine()
            append(configDsl.renderToString())
        }
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
 * Handles code generation for fake implementations. Orchestrates the generation of implementation
 * classes, factory functions, and configuration DSLs.
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
        /** Length of "Main" suffix for source set name transformation. */
        private const val MAIN_SUFFIX_LENGTH = 4
    }

    /**
     * Selects the appropriate output directory based on the source set name.
     *
     * In KMP projects, fakes should be generated to the test counterpart of their source set:
     * - CommonPlatformService (commonMain) → commonTest
     * - NativeOnlyService (nativeMain) → nativeTest
     * - JvmOnlyService (jvmMain) → jvmTest
     * - IosOnlyService (iosMain) → iosTest
     *
     * @param sourceSourceSet Source set name (e.g., "commonMain", "iosMain") or null
     * @return Absolute path to output directory
     */
    private fun selectOutputDirectory(sourceSourceSet: String?): String {
        if (sourceSourceSet == null) {
            return sourceSetContext.outputDirectory
        }

        val testSourceSet =
            when {
                sourceSourceSet.equals("main", ignoreCase = true) -> "test"
                sourceSourceSet.endsWith("Main", ignoreCase = true) ->
                    sourceSourceSet.dropLast(MAIN_SUFFIX_LENGTH) + "Test"
                else -> sourceSourceSet + "Test"
            }

        return sourceSetContext.commonTestOutputDirectory.replace(
            "/commonTest/",
            "/$testSourceSet/",
        )
    }

    /**
     * Generates complete fake implementation including class, factory, and configuration DSL.
     *
     * @param decl Pure interface declaration (types pre-rendered, no IR coupling)
     * @param sourceSourceSet Source set name where interface was defined (e.g., "commonMain",
     *   "iosMain")
     */
    fun generateWorkingFakeImplementation(
        decl: FakeDeclaration.Interface,
        sourceSourceSet: String? = null,
    ): GeneratedCode {
        val interfaceName = decl.simpleName
        val fakeClassName = "Fake${interfaceName}Impl"
        val packageName = decl.packageName

        try {
            val requiredImports = importResolver.resolveImports(decl.requiredImports, packageName)

            val generated =
                generators.implementation.generateImplementation(
                    decl,
                    packageName,
                    requiredImports.toList(),
                )

            val generatedCode =
                GeneratedCode(
                    implementation = generated.implementationFile,
                    factory = generated.factoryFunction,
                    configDsl =
                        generators.configDsl.generateConfigurationDslCodeFile(decl, fakeClassName),
                )

            writeGeneratedCode(
                context =
                    WriteContext(
                        packageName = packageName,
                        fakeClassName = fakeClassName,
                        interfaceName = interfaceName,
                    ),
                code = generatedCode,
                sourceSourceSet = sourceSourceSet,
            )

            return generatedCode
        } catch (e: Exception) {
            logger.error("Failed to generate fake for $interfaceName: ${e.message}")
            throw e
        }
    }

    /**
     * Generates complete fake implementation for an abstract or open class.
     *
     * @param decl Pure class declaration (types pre-rendered, no IR coupling)
     * @param sourceSourceSet Source set name where class was defined (e.g., "commonMain",
     *   "iosMain")
     */
    fun generateWorkingClassFake(
        decl: FakeDeclaration.Class,
        sourceSourceSet: String? = null,
    ): GeneratedCode {
        val className = decl.simpleName
        val fakeClassName = "Fake${className}Impl"
        val packageName = decl.packageName

        try {
            val requiredImports = importResolver.resolveImports(decl.requiredImports, packageName)

            val generated =
                generators.implementation.generateClassFake(
                    decl,
                    packageName,
                    requiredImports.toList(),
                )

            val generatedCode =
                GeneratedCode(
                    implementation = generated.implementationFile,
                    factory = generated.factoryFunction,
                    configDsl =
                        generators.configDsl.generateConfigurationDslCodeFile(decl, fakeClassName),
                )

            writeGeneratedCode(
                context =
                    WriteContext(
                        packageName = packageName,
                        fakeClassName = fakeClassName,
                        interfaceName = className,
                    ),
                code = generatedCode,
                sourceSourceSet = sourceSourceSet,
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
     * Uses per-declaration output directory selection based on source set for KMP isolation:
     * - CommonPlatformService (commonMain) → commonTest
     * - NativeOnlyService (nativeMain) → nativeTest
     * - IosOnlyService (iosMain) → iosTest
     */
    private fun writeGeneratedCode(
        context: WriteContext,
        code: GeneratedCode,
        sourceSourceSet: String?,
    ) {
        val packageName = context.packageName
        val fakeClassName = context.fakeClassName

        val outputDir = java.io.File(selectOutputDirectory(sourceSourceSet))

        val packagePath = packageName.replace('.', '/')
        val packageDir = outputDir.resolve(packagePath)
        packageDir.mkdirs()
        val outputFile = packageDir.resolve("$fakeClassName.kt")

        val fullCode = renderGeneratedCode(code)

        outputFile.bufferedWriter().use { writer -> writer.write(fullCode) }
    }

    /**
     * Renders GeneratedCode (all CodeFile parts) to a single string.
     *
     * Order: package/imports → factory (API entry point) → config DSL → implementation class. This
     * puts user-facing API first and internal implementation details last.
     */
    private fun renderGeneratedCode(code: GeneratedCode): String {
        val builder = CodeBuilder()

        code.implementation.renderHeaderOnly(builder)

        code.factory.declarations.forEach { declaration -> declaration.renderTo(builder) }
        builder.appendLine()

        code.configDsl.declarations.forEach { declaration -> declaration.renderTo(builder) }
        builder.appendLine()

        code.implementation.renderDeclarationsOnly(builder)

        return builder.build()
    }
}
