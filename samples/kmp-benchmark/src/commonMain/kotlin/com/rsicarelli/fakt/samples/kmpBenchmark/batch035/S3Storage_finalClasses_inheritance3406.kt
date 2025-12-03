// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch035

import com.rsicarelli.fakt.Fake

abstract class S3Storage_finalClasses_inheritance3406 {
    
    abstract fun connect(): Boolean

    
    open fun disconnect() {
        
    }
}

abstract class S3Storage_finalClasses_inheritance3406_1 : S3Storage_finalClasses_inheritance3406() {
    
    abstract override fun connect(): Boolean

    
    abstract fun upload(data: ByteArray): String

    
    open fun download(id: String): ByteArray? = null
}

@Fake
open class S3Storage_finalClasses_inheritance3406_2 : S3Storage_finalClasses_inheritance3406_1() {
    
    override fun connect(): Boolean = true

    
    override fun upload(data: ByteArray): String = "upload-id"

    
    override fun download(id: String): ByteArray? = super.download(id)

    
    open fun listBuckets(): List<String> = emptyList()
}
