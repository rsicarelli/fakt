// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.generation

import com.rsicarelli.fakt.codegen.analysis.FakeDeclaration
import com.rsicarelli.fakt.codegen.analysis.PureGenericPattern
import com.rsicarelli.fakt.compiler.api.EmitPhase
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import com.rsicarelli.fakt.compiler.core.context.ImportResolver
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Pins where [CodeGenerator] writes on the FIR (task) path: the explicit
 * [SourceSetContext.outputDirectory], never a path rewritten by string replacement. The legacy
 * rewrite of `/commonTest/` corrupted any checkout whose absolute path contained that segment.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeGeneratorOutputRoutingTest {

    private val jvmMain = SourceSetInfo(name = "jvmMain", parents = listOf("commonMain"))

    private fun context(outputDirectory: File, emitPhase: EmitPhase): SourceSetContext =
        SourceSetContext(
            compilationName = "main",
            targetName = "jvm",
            platformType = "jvm",
            isTest = false,
            defaultSourceSet = jvmMain,
            allSourceSets =
                listOf(jvmMain, SourceSetInfo(name = "commonMain", parents = emptyList())),
            outputDirectory = outputDirectory.absolutePath,
            // The worker sets both to the task's declared output.
            commonTestOutputDirectory = outputDirectory.absolutePath,
            emitPhase = emitPhase,
        )

    private fun declaration(): FakeDeclaration.Interface =
        FakeDeclaration.Interface(
            simpleName = "UserService",
            qualifiedSourceName = "UserService",
            packageName = "com.example",
            typeParameters = emptyList(),
            visibility = FirVisibility.PUBLIC,
            annotations = emptyList(),
            requiredImports = emptySet(),
            generateCallHistory = true,
            generateMutableBehaviors = false,
            genericPattern = PureGenericPattern.NoGenerics,
            properties = emptyList(),
            functions = emptyList(),
        )

    @Test
    fun `GIVEN FIR emission and a checkout path containing commonTest WHEN generating THEN the fake lands in outputDirectory`(
        @TempDir root: File
    ) {
        val outputDir = root.resolve("commonTest/proj/build/generated/fakt/jvm/main/kotlin")
        val generator =
            CodeGenerator(
                importResolver = ImportResolver(),
                sourceSetContext = context(outputDir, EmitPhase.FIR),
                logger = FaktLogger.quiet(),
            )

        generator.generateWorkingFakeImplementation(declaration(), sourceSourceSet = "jvmMain")

        val expected = outputDir.resolve("com/example/FakeUserServiceImpl.kt")
        assertTrue(
            expected.isFile,
            "The task's declared output must receive the fake; found: " +
                root.walkTopDown().filter { it.isFile }.toList(),
        )
    }
}
