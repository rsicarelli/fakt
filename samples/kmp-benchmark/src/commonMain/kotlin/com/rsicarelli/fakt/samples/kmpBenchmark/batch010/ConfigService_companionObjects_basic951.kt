// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch010

import com.rsicarelli.fakt.Fake

@Fake
interface ConfigService_companionObjects_basic951 {
    fun getConfig(key: String): String

    companion object {
        val defaultEnvironment: String
            get() = "production"
    }
}
