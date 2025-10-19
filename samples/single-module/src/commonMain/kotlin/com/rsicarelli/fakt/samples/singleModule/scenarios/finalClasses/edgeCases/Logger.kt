// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.finalClasses.edgeCases

import com.rsicarelli.fakt.Fake

/**
 * Scenario: VarargsSupport
 * Tests varargs parameter handling in fake generation
 */
@Fake
open class Logger {
    open fun log(vararg messages: String) {
        // Default implementation
    }

    open fun logWithLevel(
        level: String,
        vararg messages: String,
    ) {
        // Default implementation
    }

    open fun format(
        template: String,
        vararg args: Any?,
    ): String = template

    open fun combine(
        prefix: String,
        vararg parts: String,
        suffix: String,
    ): String = "$prefix${parts.joinToString()}$suffix"
}
