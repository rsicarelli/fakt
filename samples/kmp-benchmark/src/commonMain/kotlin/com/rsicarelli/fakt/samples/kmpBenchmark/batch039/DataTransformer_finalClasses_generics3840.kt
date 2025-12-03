// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch039

import com.rsicarelli.fakt.Fake

@Fake
open class DataTransformer_finalClasses_generics3840<In, Out> {
    
    open fun transform(input: In): Out {
        error("Not implemented")
    }

    
    open fun transformBatch(inputs: List<In>): List<Out> = emptyList()

    
    open fun canTransform(input: In): Boolean = false
}
