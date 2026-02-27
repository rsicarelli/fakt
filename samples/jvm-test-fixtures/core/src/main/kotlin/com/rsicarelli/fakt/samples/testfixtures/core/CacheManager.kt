// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.core

import com.rsicarelli.fakt.Fake

/**
 * Open class for cache management.
 *
 * Demonstrates @Fake on an open class with default implementations.
 * Generated fakes are placed in testFixtures for cross-module consumption.
 */
@Fake
public open class CacheManager {
    public open fun get(key: String): String? = null

    public open fun put(key: String, value: String) {}

    public open fun invalidate(key: String) {}

    public open fun clear() {}
}
