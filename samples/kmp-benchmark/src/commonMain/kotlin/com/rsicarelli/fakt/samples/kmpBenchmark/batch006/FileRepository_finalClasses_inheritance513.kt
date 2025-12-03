// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch006

import com.rsicarelli.fakt.Fake

abstract class FileRepository_finalClasses_inheritance513 {
    abstract fun findById(id: String): String?

    abstract fun save(entity: String)
}

@Fake
open class FileRepository_finalClasses_inheritance513_1 : FileRepository_finalClasses_inheritance513() {
    
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
