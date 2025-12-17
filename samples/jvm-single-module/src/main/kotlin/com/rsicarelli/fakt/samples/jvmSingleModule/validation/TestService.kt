// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.jvmSingleModule.validation

import com.rsicarelli.fakt.Fake

/**
 * Test interface for POC validation: Can we access compiled classes during test compilation?
 *
 * This interface will be compiled in main, then the plugin will attempt to reference it
 * during test compilation via IrPluginContext.referenceClass().
 */
@Fake
interface TestService {
    fun getValue(): String
    fun computeSum(a: Int, b: Int): Int
}
