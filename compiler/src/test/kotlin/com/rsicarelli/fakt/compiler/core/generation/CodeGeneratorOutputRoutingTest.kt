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

    private fun context(
        outputDirectory: File,
        emitPhase: EmitPhase,
        outputDirectories: Map<String, String> = emptyMap(),
    ): SourceSetContext =
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
            outputDirectories = outputDirectories,
        )

    private fun declaration(name: String = "UserService"): FakeDeclaration.Interface =
        FakeDeclaration.Interface(
            simpleName = name,
            qualifiedSourceName = name,
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

    @Test
    fun `GIVEN FIR emission and an output map WHEN generating common and platform fakes THEN each lands in its mapped directory`(
        @TempDir root: File
    ) {
        val commonDir = root.resolve("commonTest")
        val jvmDir = root.resolve("jvmTest")
        val generator =
            CodeGenerator(
                importResolver = ImportResolver(),
                sourceSetContext =
                    context(
                        root.resolve("fallback"),
                        EmitPhase.FIR,
                        mapOf(
                            "commonMain" to commonDir.absolutePath,
                            "jvmMain" to jvmDir.absolutePath,
                        ),
                    ),
                logger = FaktLogger.quiet(),
            )

        generator.generateWorkingFakeImplementation(declaration(), sourceSourceSet = "commonMain")
        generator.generateWorkingFakeImplementation(
            declaration("OrderService"),
            sourceSourceSet = "jvmMain",
        )

        assertTrue(commonDir.resolve("com/example/FakeUserServiceImpl.kt").isFile)
        assertTrue(jvmDir.resolve("com/example/FakeOrderServiceImpl.kt").isFile)
        assertTrue(!jvmDir.resolve("com/example/FakeUserServiceImpl.kt").exists())
    }

    @Test
    fun `GIVEN FIR emission and an output map WHEN the source set is unknown THEN the default source set route is used`(
        @TempDir root: File
    ) {
        val jvmDir = root.resolve("jvmTest")
        val generator =
            CodeGenerator(
                importResolver = ImportResolver(),
                sourceSetContext =
                    context(
                        root.resolve("fallback"),
                        EmitPhase.FIR,
                        mapOf("jvmMain" to jvmDir.absolutePath),
                    ),
                logger = FaktLogger.quiet(),
            )

        generator.generateWorkingFakeImplementation(declaration(), sourceSourceSet = null)

        assertTrue(jvmDir.resolve("com/example/FakeUserServiceImpl.kt").isFile)
    }

    @Test
    fun `GIVEN IR emission and no output map WHEN generating THEN the test counterpart directory is kept`(
        @TempDir root: File
    ) {
        val commonTest = root.resolve("build/commonTest/kotlin")
        val generator =
            CodeGenerator(
                importResolver = ImportResolver(),
                sourceSetContext =
                    context(root.resolve("out"), EmitPhase.IR)
                        .copy(commonTestOutputDirectory = commonTest.absolutePath),
                logger = FaktLogger.quiet(),
            )

        generator.generateWorkingFakeImplementation(declaration(), sourceSourceSet = "jvmMain")

        assertTrue(
            root.resolve("build/jvmTest/kotlin/com/example/FakeUserServiceImpl.kt").isFile,
            "found: " + root.walkTopDown().filter { it.isFile }.toList(),
        )
    }
}
