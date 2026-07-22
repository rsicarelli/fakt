// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.androidtestfixtures.consumer

import com.rsicarelli.fakt.samples.androidtestfixtures.producer.UserRepository
import com.rsicarelli.fakt.samples.androidtestfixtures.producer.models.User

/** Production code in the consumer module that depends on the producer's [UserRepository]. */
class UserService(private val repository: UserRepository) {

    fun displayName(id: String): String = repository.findById(id)?.name ?: "Unknown"

    fun registeredCount(): Int = repository.users.size
}
