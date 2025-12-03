// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch008

import com.rsicarelli.fakt.Fake

@Fake
open class Logger_finalClasses_edgeCases761 {
    open fun log(vararg messages: String) {
        
    }

    open fun logWithLevel(
        level: String,
        vararg messages: String,
    ) {
        
    }

    open fun format(
        template: String,
        vararg args: Any?,
    ): String = template

    open fun combine(
        prefix: String,
        vararg parts: String,
        suffix: String,
    ): String = "$prefix${parts.joinToString()}$suffix"
}
