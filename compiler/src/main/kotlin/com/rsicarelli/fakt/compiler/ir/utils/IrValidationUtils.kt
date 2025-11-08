// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.utils

import com.rsicarelli.fakt.compiler.ir.analysis.GenericPatternAnalyzer
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.telemetry.FaktLogger
import org.jetbrains.kotlin.ir.declarations.IrClass

/**
 * Validates the analyzed generic pattern and logs warnings.
 *
 * Checks for pattern consistency and reports any warnings to help developers
 * understand potential issues with their interface design.
 *
 * @param interfaceAnalysis The analyzed interface structure
 * @param fakeInterface The IR class being processed
 * @param interfaceName Name of the interface for logging context
 * @param logger The FaktLogger instance for warning output
 */
internal fun validateAndLogGenericPattern(
    interfaceAnalysis: InterfaceAnalysis,
    fakeInterface: IrClass,
    interfaceName: String,
    logger: FaktLogger,
) {
    // Validate pattern for consistency using companion object methods
    val warnings =
        GenericPatternAnalyzer.validatePattern(
            interfaceAnalysis.genericPattern,
            fakeInterface,
        )

    // Log warnings if any
    if (warnings.isNotEmpty()) {
        warnings.forEach { warning ->
            logger.warn("$warning in $interfaceName")
        }
    }
}
