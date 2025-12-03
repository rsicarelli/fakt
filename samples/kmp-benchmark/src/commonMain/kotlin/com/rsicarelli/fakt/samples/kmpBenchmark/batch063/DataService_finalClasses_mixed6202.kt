// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch063

import com.rsicarelli.fakt.Fake

@Fake
abstract class DataService_finalClasses_mixed6202 {
    
    abstract fun fetchData(): String

    abstract fun validate(data: String): Boolean

    
    open fun transform(data: String): String = data.uppercase()

    open fun log(message: String) {
        println("[LOG] $message")
    }
}
