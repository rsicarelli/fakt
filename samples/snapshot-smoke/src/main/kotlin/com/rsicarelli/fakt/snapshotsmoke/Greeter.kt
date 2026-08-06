// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.snapshotsmoke

import com.rsicarelli.fakt.Fake

/** Annotate an interface with @Fake — the SNAPSHOT plugin generates `fakeGreeter { }` for tests. */
@Fake
interface Greeter {
    fun greet(name: String): String
}
