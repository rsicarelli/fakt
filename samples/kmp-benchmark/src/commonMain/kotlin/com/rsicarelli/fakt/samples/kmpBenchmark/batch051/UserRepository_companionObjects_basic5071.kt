// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch051

import com.rsicarelli.fakt.samples.kmpBenchmark.models.User
import com.rsicarelli.fakt.Fake

@Fake
interface UserRepository_companionObjects_basic5071 {
    fun getUser(id: String): Result<User>
}

