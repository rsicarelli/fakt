// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch033

import com.rsicarelli.fakt.samples.kmpBenchmark.models.User
import com.rsicarelli.fakt.Fake

@Fake
open class UserService_finalClasses_edgeCases3266 {
    open fun findById(id: String?): User? = null

    open fun update(user: User?): Boolean = false

    open fun merge(
        primary: User?,
        secondary: User?,
    ): User? = primary

    open val currentUser: User?
        get() = null

    open var cachedUser: User? = null
}

