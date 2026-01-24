// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.jvmSingleModule.scenarios.visibility

import com.rsicarelli.fakt.Fake

@Fake
public interface PublicService {
    public fun doWork(): String
    public val status: Boolean
}
