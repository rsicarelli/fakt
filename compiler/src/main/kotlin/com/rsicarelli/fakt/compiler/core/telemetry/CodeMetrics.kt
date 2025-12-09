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

/** Number of bytes in one kilobyte. */
private const val BYTES_PER_KB = 1024L

/** Number of bytes in one megabyte. */
private const val BYTES_PER_MB = 1024L * 1024L

/**
 * Formats byte count into human-readable string.
 */
internal fun Long.formatBytes(): String =
    when {
        this < BYTES_PER_KB -> "$this B"
        this < BYTES_PER_MB -> "${this / BYTES_PER_KB} KB"
        else -> "${this / BYTES_PER_MB} MB"
    }
