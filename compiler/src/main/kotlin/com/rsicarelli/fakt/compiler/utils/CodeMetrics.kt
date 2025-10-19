// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.utils

/**
 * Calculates lines of code excluding blank lines and comments.
 */
internal fun calculateLOC(code: String): Int {
    return code.lines()
        .filter { line ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() &&
                !trimmed.startsWith("//") &&
                !trimmed.startsWith("/*") &&
                !trimmed.startsWith("*")
        }
        .count()
}

/**
 * Formats byte count into human-readable string.
 */
internal fun Long.formatBytes(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> "${this / 1024} KB"
        else -> "${this / (1024 * 1024)} MB"
    }
}
