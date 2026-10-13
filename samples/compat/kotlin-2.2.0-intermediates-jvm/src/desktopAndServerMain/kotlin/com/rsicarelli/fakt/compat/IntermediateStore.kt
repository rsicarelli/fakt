// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.compat

import com.rsicarelli.fakt.Fake

/** Declared in the intermediate source set, not in commonMain or a platform main. */
@Fake
interface IntermediateStore {
    fun open(user: String): String
    fun close(token: String)
}
