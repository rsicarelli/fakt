// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch008

import com.rsicarelli.fakt.Fake

abstract class FileRepository_finalClasses_inheritance752 {
    abstract fun findById(id: String): String?

    abstract fun save(entity: String)
}

@Fake
open class FileRepository_finalClasses_inheritance752_1 : FileRepository_finalClasses_inheritance752() {
    
    override fun findById(id: String): String? {
        
        return null
    }

    override fun save(entity: String) {
        
    }

    
    open fun findAll(): List<String> {
        
        return emptyList()
    }

    open fun clearCache() {
        
    }
}
