// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch001

import com.rsicarelli.fakt.Fake

@Fake
open class BaseFragment_finalClasses_visibility34 {
    
    open fun onCreate() {
        
        onInit()
        println("onCreate called, initialized=$isInitialized")
    }

    
    protected open fun onInit() {
        
    }

    
    protected open val isInitialized: Boolean
        get() = true

    
    open fun checkInitialized(): Boolean = isInitialized
}
