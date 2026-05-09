// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.runner

import com.rsicarelli.fakt.codegen.analysis.FakeDeclaration
import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.core.config.FaktOptions
import com.rsicarelli.fakt.compiler.core.context.FaktSharedContext
import com.rsicarelli.fakt.compiler.core.optimization.CompilerOptimizations
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.fir.FaktFirExtensionRegistrar
import com.rsicarelli.fakt.compiler.fir.cache.MetadataCacheManager
import com.rsicarelli.fakt.compiler.fir.metadata.FirMetadataStorage
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

/**
 * Headless entry point that turns Fakt's FIR + IR analysis pipeline into something callable from
 * outside `compileKotlin*`. Returns the [CompilerPluginRegistrar] a caller plugs into their K2
 * driver (kotlin-compile-testing in this module's tests; a Gradle Worker hosting
 * `kotlin-compiler-embeddable` once PR 2 lands) along with an accumulator that exposes the
 * [FakeDeclaration]s discovered during compilation.
 *
 * The runner deliberately does *not* invoke K2 itself — it only wires the FIR registrar and the
 * capture IR extension into a shared context. Decoupling K2 invocation from translation lets tests
 * use the in-process driver from `kotlin-compile-testing` while production code (PR 2 onward) can
 * drive K2 through whichever worker-isolated path the cache-correctness fix lands on.
 */
public object FaktAnalysisRunner {

    /**
     * Builds an analysis session for one compilation. The returned [RunnerSession] holds a
     * configured [CompilerPluginRegistrar]; pass it to a K2 driver and read
     * [RunnerSession.captured] after compilation completes.
     *
     * @param sourceSetContext Source-set descriptor as the Gradle plugin would produce it. Required
     *   even though the capture extension never writes files — Fakt's FIR machinery propagates this
     *   through [FaktSharedContext] for telemetry and (in production) cache-mode selection.
     * @param logLevel Log verbosity for the embedded [FaktLogger]. Defaults to QUIET so spikes and
     *   tests don't pollute stdout.
     */
    public fun newSession(
        sourceSetContext: SourceSetContext,
        logLevel: LogLevel = LogLevel.QUIET,
    ): RunnerSession {
        val captured = mutableListOf<FakeDeclaration>()
        val logger = FaktLogger(messageCollector = null, logLevel = logLevel)
        val options =
            FaktOptions(
                enabled = true,
                logLevel = logLevel,
                outputDir = null,
                sourceSetContext = sourceSetContext,
                enableCallHistoryDefault = true,
                enableMutableFakesDefault = false,
            )
        val sharedContext =
            FaktSharedContext(
                fakeAnnotations = FaktSharedContext.DEFAULT_FAKE_ANNOTATIONS,
                options = options,
                metadataStorage = FirMetadataStorage(),
                logger = logger,
                optimizations =
                    CompilerOptimizations(
                        fakeAnnotations = FaktSharedContext.DEFAULT_FAKE_ANNOTATIONS,
                        outputDir = null,
                        logger = logger,
                    ),
                cacheManager =
                    MetadataCacheManager(metadataOutputPath = null, metadataCachePath = null),
            )
        val captureExtension =
            CaptureIrGenerationExtension(sharedContext) { decl -> captured.add(decl) }
        return RunnerSession(
            pluginRegistrar = FaktRunnerPluginRegistrar(sharedContext, captureExtension),
            capturedRef = captured,
        )
    }
}

/**
 * Output of [FaktAnalysisRunner.newSession]. The [pluginRegistrar] is registered with a K2 driver;
 * after compilation, [captured] returns the [FakeDeclaration]s the IR phase translated.
 *
 * Sessions are not reusable — call [FaktAnalysisRunner.newSession] per compilation so each one gets
 * a fresh [FirMetadataStorage] and accumulator.
 */
public class RunnerSession
internal constructor(
    public val pluginRegistrar: CompilerPluginRegistrar,
    private val capturedRef: List<FakeDeclaration>,
) {
    /** Snapshot of declarations discovered so far. */
    public fun captured(): List<FakeDeclaration> = capturedRef.toList()
}

@OptIn(ExperimentalCompilerApi::class)
internal class FaktRunnerPluginRegistrar(
    private val sharedContext: FaktSharedContext,
    private val captureExtension: CaptureIrGenerationExtension,
) : CompilerPluginRegistrar() {
    override val pluginId: String = "com.rsicarelli.fakt.runner"
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(FaktFirExtensionRegistrar(sharedContext))
        IrGenerationExtension.registerExtension(captureExtension)
    }
}
