// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

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
import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis

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
     * This is more reliable than the cache-based approach because it works regardless of
     * compilation order and metadata compilation being skipped.
     *
     * @param sourceSourceSet Source set name (e.g., "commonMain", "iosMain") or null
     * @return Absolute path to output directory
     */
    private fun selectOutputDirectory(sourceSourceSet: String?): String {
        // If we don't know the source set, use the default output directory
        if (sourceSourceSet == null) {
            return sourceSetContext.outputDirectory
        }

        // Map main source set to test source set
        // e.g., "commonMain" → "commonTest", "iosMain" → "iosTest"
        val testSourceSet =
            when {
                sourceSourceSet.equals("main", ignoreCase = true) -> "test"
                sourceSourceSet.endsWith("Main", ignoreCase = true) ->
                    sourceSourceSet.dropLast(MAIN_SUFFIX_LENGTH) + "Test"
                else -> sourceSourceSet + "Test"
            }

        // Derive output directory from commonTestOutputDirectory pattern
        // commonTestOutputDirectory = "$buildDir/generated/fakt/commonTest/kotlin"
        // We replace "commonTest" with our target test source set
        return sourceSetContext.commonTestOutputDirectory.replace(
            "/commonTest/",
            "/$testSourceSet/",
        )
    }

    /**
     * Generates complete fake implementation including class, factory, and configuration DSL.
     *
     * @param analysis The analyzed interface metadata (includes pre-stored packageName)
     * @param sourceSourceSet Source set name where interface was defined (e.g., "commonMain",
     *   "iosMain")
     */
    fun generateWorkingFakeImplementation(
        analysis: InterfaceAnalysis,
        sourceSourceSet: String? = null,
    ): GeneratedCode {
        val interfaceName = analysis.interfaceName
        val fakeClassName = "Fake${interfaceName}Impl"
        val packageName = analysis.packageName

        try {
            // Collect required imports for implementation
            val requiredImports = importResolver.collectRequiredImports(analysis, packageName)

            // Generate implementation + factory using DSL
            val generated =
                generators.implementation.generateImplementation(
                    analysis,
                    packageName,
                    requiredImports.toList(),
                )

            // Assemble final code using CodeFile throughout
            val generatedCode =
                GeneratedCode(
                    implementation = generated.implementationFile,
                    factory = generated.factoryFunction,
                    configDsl =
                        generators.configDsl.generateConfigurationDslCodeFile(
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
                sourceSourceSet = sourceSourceSet,
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
     * Generates complete fake implementation for a class including implementation, factory, and
     * configuration DSL.
     *
     * @param analysis The analyzed class metadata (includes pre-stored packageName)
     * @param sourceSourceSet Source set name where class was defined (e.g., "commonMain",
     *   "iosMain")
     */
    fun generateWorkingClassFake(
        analysis: ClassAnalysis,
        sourceSourceSet: String? = null,
    ): GeneratedCode {
        val className = analysis.className
        val fakeClassName = "Fake${className}Impl"
        val packageName = analysis.packageName

        try {
            // Collect required imports for implementation
            val requiredImports =
                importResolver.collectRequiredImportsForClass(analysis, packageName)

            // Generate implementation + factory using DSL
            val generated =
                generators.implementation.generateClassFake(
                    analysis,
                    packageName,
                    requiredImports.toList(),
                )

            // Assemble final code using CodeFile throughout
            val generatedCode =
                GeneratedCode(
                    implementation = generated.implementationFile,
                    factory = generated.factoryFunction,
                    configDsl =
                        generators.configDsl.generateConfigurationDslCodeFile(
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
     * Uses per-interface output directory selection based on source set for KMP isolation:
     * - CommonPlatformService (commonMain) → commonTest
     * - NativeOnlyService (nativeMain) → nativeTest
     * - IosOnlyService (iosMain) → iosTest
     *
     * @param context Write context with package and class names
     * @param code Generated code to write
     * @param sourceSourceSet Source set name where interface was defined
     */
    private fun writeGeneratedCode(
        context: WriteContext,
        code: GeneratedCode,
        sourceSourceSet: String?,
    ) {
        val packageName = context.packageName
        val fakeClassName = context.fakeClassName

        // Select output directory based on source set for KMP source set isolation
        val outputDir = java.io.File(selectOutputDirectory(sourceSourceSet))

        // Create subdirectories matching the package structure
        val packagePath = packageName.replace('.', '/')
        val packageDir = outputDir.resolve(packagePath)
        packageDir.mkdirs()
        val outputFile = packageDir.resolve("$fakeClassName.kt")

        // Render all CodeFiles to a single output string
        val fullCode = renderGeneratedCode(code)

        // Use buffered writer for better I/O performance
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

        // 1. Package + imports (from implementation CodeFile)
        code.implementation.renderHeaderOnly(builder)

        // 2. Factory function (API entry point — first thing user sees)
        code.factory.declarations.forEach { declaration -> declaration.renderTo(builder) }
        builder.appendLine()

        // 3. Config DSL (what the user configures)
        code.configDsl.declarations.forEach { declaration -> declaration.renderTo(builder) }
        builder.appendLine()

        // 4. Implementation class + call history (internals)
        code.implementation.renderDeclarationsOnly(builder)

        return builder.build()
    }

    /**
     * Writes the generated code for a class fake to the appropriate output file.
     *
     * Uses per-class output directory selection based on source set for KMP isolation:
     * - CommonService (commonMain) → commonTest
     * - NativeService (nativeMain) → nativeTest
     * - IosService (iosMain) → iosTest
     *
     * @param context Write context with package and class names
     * @param code Generated code to write
     * @param sourceSourceSet Source set name where class was defined
     */
    private fun writeGeneratedCodeForClass(
        context: WriteContext,
        code: GeneratedCode,
        sourceSourceSet: String?,
    ) {
        val packageName = context.packageName
        val fakeClassName = context.fakeClassName

        // Select output directory based on source set for KMP source set isolation
        val outputDir = java.io.File(selectOutputDirectory(sourceSourceSet))

        // Create subdirectories matching the package structure
        val packagePath = packageName.replace('.', '/')
        val packageDir = outputDir.resolve(packagePath)
        packageDir.mkdirs()
        val outputFile = packageDir.resolve("$fakeClassName.kt")

        // Render all CodeFiles to a single output string
        val fullCode = renderGeneratedCode(code)

        // Use buffered writer for better I/O performance
        outputFile.bufferedWriter().use { writer -> writer.write(fullCode) }
    }
}
