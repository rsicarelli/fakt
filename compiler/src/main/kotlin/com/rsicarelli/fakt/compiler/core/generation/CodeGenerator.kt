// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.generation

import com.rsicarelli.fakt.codegen.FaktCodegen
import com.rsicarelli.fakt.codegen.RenderedFakeFile
import com.rsicarelli.fakt.codegen.analysis.FakeDeclaration
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.core.context.ImportResolver
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.core.telemetry.calculateLOC

/**
 * Result of generating a fake: the rendered file plus a pre-computed LOC count for telemetry.
 *
 * @property file The rendered file ready for disk write.
 * @property linesOfCode Non-blank, non-comment lines in [RenderedFakeFile.content].
 */
internal data class GeneratedCode(val file: RenderedFakeFile, val linesOfCode: Int) {
    fun calculateTotalLOC(): Int = linesOfCode
}

/**
 * Compiler-side orchestrator: resolves imports, picks the KMP-aware output directory, delegates
 * rendering to [FaktCodegen], and writes the result to disk.
 *
 * @property importResolver Filters and remaps the FQN set into the generated file's import list.
 * @property sourceSetContext Compilation metadata supplied by the Gradle plugin.
 * @property logger Logger for compilation feedback.
 */
internal class CodeGenerator(
    private val importResolver: ImportResolver,
    private val sourceSetContext: SourceSetContext,
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
     * Renders the fake for an interface and writes it to the source-set-appropriate test directory.
     *
     * @param sourceSourceSet Source set where the interface was defined (e.g. `"commonMain"`,
     *   `"iosMain"`); null when the source set is unknown (then the default output dir is used).
     */
    fun generateWorkingFakeImplementation(
        decl: FakeDeclaration.Interface,
        sourceSourceSet: String? = null,
    ): GeneratedCode = renderAndWrite(decl, decl.simpleName, sourceSourceSet)

    /** Renders the fake for an abstract or open class. See [generateWorkingFakeImplementation]. */
    fun generateWorkingClassFake(
        decl: FakeDeclaration.Class,
        sourceSourceSet: String? = null,
    ): GeneratedCode = renderAndWrite(decl, decl.simpleName, sourceSourceSet)

    private fun renderAndWrite(
        decl: FakeDeclaration,
        sourceName: String,
        sourceSourceSet: String?,
    ): GeneratedCode =
        try {
            val imports = importResolver.resolveImports(decl.requiredImports, decl.packageName)
            val rendered = FaktCodegen.render(decl, imports.toList())
            writeToDisk(rendered, sourceSourceSet)
            GeneratedCode(file = rendered, linesOfCode = calculateLOC(rendered.content))
        } catch (e: Exception) {
            logger.error("Failed to generate fake for $sourceName: ${e.message}")
            throw e
        }

    private fun writeToDisk(rendered: RenderedFakeFile, sourceSourceSet: String?) {
        val outputDir = java.io.File(selectOutputDirectory(sourceSourceSet))
        val packageDir = outputDir.resolve(rendered.packageName.replace('.', '/'))
        packageDir.mkdirs()
        packageDir.resolve(rendered.fileName).bufferedWriter().use { writer ->
            writer.write(rendered.content)
        }
    }
}
