// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch092

import com.rsicarelli.fakt.Fake

interface LoggingService_finalClasses_inheritance9101 {
    
    fun log(message: String)
}

abstract class LoggingService_finalClasses_inheritance9101_1 {
    
    abstract fun start(): Boolean

    
    open fun stop() {
        
    }
}

@Fake
open class LoggingService_finalClasses_inheritance9101_2 :
    LoggingService_finalClasses_inheritance9101_1(),
    LoggingService_finalClasses_inheritance9101 {
    
    override fun start(): Boolean = true

    
    override fun log(message: String) {
        
    }

    
    open fun getLogLevel(): String = "INFO"
}
