// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpNoJvm

import com.rsicarelli.fakt.Fake

/**
 * JS-only service. With no drivable target in the project, this platform `@Fake` rides the
 * in-process plugin (LEGACY_HYBRID): `FakeJsOnlyStorageImpl` is generated into jsTest — not
 * cache-correct, but never dropped.
 */
@Fake
interface JsOnlyStorage {
    /** Persist a value under a key. */
    fun put(key: String, value: String)

    /** Read a value, or null when the key is absent. */
    fun get(key: String): String?
}
