// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.ir.analysis.GenericPatternAnalyzer
import com.rsicarelli.fakt.compiler.ir.transform.IrClassGenerationMetadata
import com.rsicarelli.fakt.compiler.ir.transform.IrGenerationMetadata

/**
 * Validates the analyzed generic pattern for an interface and logs warnings.
 *
 * Operates directly on [IrGenerationMetadata] (3.1.d.2: lifted from InterfaceAnalysis level so
 * callers no longer need to pass the `sourceInterface: IrClass` explicitly).
 *
 * @param metadata The IR generation metadata containing genericPattern and sourceInterface
 * @param logger The FaktLogger instance for warning output
 */
internal fun validateAndLogGenericPattern(metadata: IrGenerationMetadata, logger: FaktLogger) {
    val warnings =
        GenericPatternAnalyzer.validatePattern(metadata.genericPattern, metadata.sourceInterface)
    if (warnings.isNotEmpty()) {
        warnings.forEach { warning -> logger.warn("$warning in ${metadata.interfaceName}") }
    }
}

/**
 * Validates the analyzed generic pattern for a class and logs warnings.
 *
 * Operates directly on [IrClassGenerationMetadata] (3.1.d.2 parity with interface variant).
 *
 * @param metadata The IR class generation metadata containing genericPattern and sourceClass
 * @param logger The FaktLogger instance for warning output
 */
internal fun validateAndLogClassGenericPattern(
    metadata: IrClassGenerationMetadata,
    logger: FaktLogger,
) {
    val warnings =
        GenericPatternAnalyzer.validatePattern(metadata.genericPattern, metadata.sourceClass)
    if (warnings.isNotEmpty()) {
        warnings.forEach { warning -> logger.warn("$warning in ${metadata.className}") }
    }
}
