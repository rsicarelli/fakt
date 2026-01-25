// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

/**
 * Calculates lines of code excluding blank lines and comments.
 */
internal fun calculateLOC(code: String): Int =
    code.lines().count { line ->
        val trimmed = line.trim()
        trimmed.isNotEmpty() &&
            !trimmed.startsWith("//") &&
            !trimmed.startsWith("/*") &&
            !trimmed.startsWith("*")
    }

