// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake

/**
 * JavaScript-only service for platform-specific operations.
 *
 * This interface is defined in jsMain and should generate a fake
 * in jsTest ONLY. It should NOT be available in other platform tests.
 *
 * Tests: DOM manipulation, localStorage (JS-specific abstractions).
 */
@Fake
interface JsOnlyService {
    /**
     * Get an element by ID from the DOM (browser-specific).
     */
    fun getElementById(id: String): String?

    /**
     * Set local storage item (browser-specific).
     */
    fun setLocalStorage(key: String, value: String)

    /**
     * Get local storage item (browser-specific).
     */
    fun getLocalStorage(key: String): String?

    /**
     * Current window URL.
     */
    val currentUrl: String
}
