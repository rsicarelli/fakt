// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake

/**
 * Common platform service shared across all platforms.
 *
 * This interface is defined in commonMain and should generate a fake
 * in commonTest that can be used by all platform tests.
 *
 * Tests: Logger-like functionality available on all platforms.
 */
@Fake
interface CommonPlatformService {
    /**
     * Log a message with a specific level.
     */
    fun log(level: String, message: String)

    /**
     * Get the current platform name (e.g., "JVM", "iOS", "JS").
     */
    fun getPlatformName(): String

    /**
     * Check if debug logging is enabled.
     */
    val isDebugEnabled: Boolean
}
