// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake

/**
 * iOS-only service for platform-specific operations.
 *
 * This interface is defined in iosMain and should generate a fake
 * in iosTest ONLY. It should NOT be available in other platform tests.
 *
 * Tests: UserDefaults, device info (iOS-specific abstractions).
 */
@Fake
interface IosOnlyService {
    /**
     * Save a preference to UserDefaults (iOS-specific).
     */
    fun savePreference(key: String, value: String)

    /**
     * Load a preference from UserDefaults (iOS-specific).
     */
    fun loadPreference(key: String): String?

    /**
     * Get device model name (e.g., "iPhone 14 Pro").
     */
    fun getDeviceModel(): String

    /**
     * Check if running on a simulator.
     */
    val isSimulator: Boolean
}
