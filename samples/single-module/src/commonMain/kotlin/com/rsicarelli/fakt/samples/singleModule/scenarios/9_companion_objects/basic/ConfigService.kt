// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.companionObjects.basic

import com.rsicarelli.fakt.Fake

/**
 * Scenario 2: Interface with companion property
 *
 * Demonstrates companion objects with properties (not just constants).
 */
@Fake
interface ConfigService {
    fun getConfig(key: String): String

    companion object {
        val defaultEnvironment: String
            get() = "production"
    }
}
