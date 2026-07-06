// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.generation

import com.rsicarelli.fakt.compiler.core.context.FaktSharedContext
import com.rsicarelli.fakt.compiler.core.context.ImportResolver
import com.rsicarelli.fakt.compiler.core.generation.CodeGenerator
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeClass
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeInterface

/**
 * FIR-phase fake emitter: renders and writes a `@Fake` declaration's `.kt` right after the checker
 * stores its metadata — the generation path used when
 * [com.rsicarelli.fakt.compiler.api.EmitPhase.FIR] is active (the cache-correct worker; required
 * for the `KotlinMetadataCompiler`-driven producer, whose pipeline has no IR phase).
 *
 * Checker-driven emission fires only for **source** declarations, so cache-loaded (common)
 * interfaces never re-emit in consumer scenarios by construction.
 *
 * Collision policy mirrors the IR path's `reportAndDropOutputCollisions`: the first `(packageName,
 * FakeXxxImpl)` wins; later duplicates log an error and are skipped. The seen-set lives on
 * [FaktSharedContext] so both checkers share it within a compilation.
 */
internal class FirFakeEmitter(private val sharedContext: FaktSharedContext) {
    private val logger = sharedContext.logger

    private val codeGenerator: CodeGenerator? by lazy {
        sharedContext.options.sourceSetContext?.let { context ->
            CodeGenerator(
                importResolver = ImportResolver(),
                sourceSetContext = context,
                logger = logger,
            )
        }
    }

    /** Renders and writes the fake for a validated interface. */
    fun emit(metadata: ValidatedFakeInterface) {
        val generator = codeGenerator ?: return missingContext(metadata.simpleName)
        if (!claimOutput(metadata.packageName, metadata.simpleName, metadata.qualifiedSourceName)) {
            return
        }

        val decl =
            metadata.toFakeInterface(
                enableCallHistoryDefault = sharedContext.options.enableCallHistoryDefault,
                enableMutableFakesDefault = sharedContext.options.enableMutableFakesDefault,
            )
        generator.generateWorkingFakeImplementation(decl, metadata.sourceSourceSet)
    }

    /** Renders and writes the fake for a validated abstract/open class. */
    fun emit(metadata: ValidatedFakeClass) {
        val generator = codeGenerator ?: return missingContext(metadata.simpleName)
        if (!claimOutput(metadata.packageName, metadata.simpleName, metadata.qualifiedSourceName)) {
            return
        }

        val decl =
            metadata.toFakeClass(
                enableCallHistoryDefault = sharedContext.options.enableCallHistoryDefault,
                enableMutableFakesDefault = sharedContext.options.enableMutableFakesDefault,
            )
        generator.generateWorkingClassFake(decl, metadata.sourceSourceSet)
    }

    private fun claimOutput(
        packageName: String,
        simpleName: String,
        qualifiedSourceName: String,
    ): Boolean {
        val claimed = sharedContext.emittedOutputs.add("$packageName:Fake${simpleName}Impl")
        if (!claimed) {
            logger.error(
                "[FAKT] Multiple @Fake declarations would generate the same " +
                    "Fake${simpleName}Impl in package '$packageName': $qualifiedSourceName " +
                    "collides with an earlier declaration. " +
                    "Rename one of them to avoid the file collision."
            )
        }
        return claimed
    }

    private fun missingContext(simpleName: String) {
        logger.error(
            "[FAKT] Cannot emit fake for $simpleName at FIR: no SourceSetContext was supplied. " +
                "EmitPhase.FIR requires the Gradle plugin's serialized context."
        )
    }
}
