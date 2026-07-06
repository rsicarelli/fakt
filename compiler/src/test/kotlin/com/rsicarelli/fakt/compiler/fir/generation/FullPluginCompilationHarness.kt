// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.generation

import com.rsicarelli.fakt.compiler.FaktCompilerPluginRegistrar
import com.rsicarelli.fakt.compiler.api.EmitPhase
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import com.rsicarelli.fakt.compiler.core.config.FaktCommandLineProcessor
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import java.io.File
import java.util.Base64
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * Drives the REAL Fakt plugin ([FaktCompilerPluginRegistrar] + [FaktCommandLineProcessor]) through
 * kotlin-compile-testing with a chosen [EmitPhase], writing generated fakes into [outputDir].
 *
 * Fixtures must declare the `@Fake` annotation in-fixture (`package com.rsicarelli.fakt; annotation
 * class Fake`) — the `:annotations` jar is not on the test classpath, and the FIR checker matches
 * by ClassId.
 */
@OptIn(ExperimentalCompilerApi::class)
internal object FullPluginCompilationHarness {

    /** In-fixture stand-in for the real annotation; prepended to every fixture compilation. */
    val FAKE_ANNOTATION: SourceFile =
        SourceFile.kotlin(
            "Fake.kt",
            """
            package com.rsicarelli.fakt

            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.SOURCE)
            annotation class Fake
            """
                .trimIndent(),
        )

    data class Outcome(
        val exitCode: KotlinCompilation.ExitCode,
        val messages: String,
        /** Generated files keyed by path relative to the output dir. */
        val generatedFiles: Map<String, String>,
    )

    fun compile(
        fixtures: List<SourceFile>,
        emitPhase: EmitPhase,
        outputDir: File,
        enableCallHistory: Boolean = true,
    ): Outcome {
        val context =
            SourceSetContext(
                compilationName = "main",
                targetName = "jvm",
                platformType = "jvm",
                isTest = false,
                defaultSourceSet = SourceSetInfo(name = "main", parents = emptyList()),
                allSourceSets = listOf(SourceSetInfo(name = "main", parents = emptyList())),
                outputDirectory = outputDir.absolutePath,
                commonTestOutputDirectory = outputDir.absolutePath,
                emitPhase = emitPhase,
            )
        val contextBase64 =
            Base64.getEncoder()
                .encodeToString(
                    Json.encodeToString(SourceSetContext.serializer(), context).toByteArray()
                )

        val result =
            KotlinCompilation()
                .apply {
                    sources = listOf(FAKE_ANNOTATION) + fixtures
                    compilerPluginRegistrars = listOf(FaktCompilerPluginRegistrar())
                    commandLineProcessors = listOf(FaktCommandLineProcessor())
                    pluginOptions =
                        listOf(
                            PluginOption("com.rsicarelli.fakt", "enabled", "true"),
                            PluginOption("com.rsicarelli.fakt", "logLevel", "QUIET"),
                            PluginOption(
                                "com.rsicarelli.fakt",
                                "outputDir",
                                outputDir.absolutePath,
                            ),
                            PluginOption(
                                "com.rsicarelli.fakt",
                                "enableCallHistory",
                                enableCallHistory.toString(),
                            ),
                            PluginOption("com.rsicarelli.fakt", "sourceSetContext", contextBase64),
                        )
                    inheritClassPath = true
                    messageOutputStream = System.out
                }
                .compile()

        val generated =
            outputDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .associate { file -> file.relativeTo(outputDir).path to file.readText() }

        return Outcome(
            exitCode = result.exitCode,
            messages = result.messages,
            generatedFiles = generated,
        )
    }
}
