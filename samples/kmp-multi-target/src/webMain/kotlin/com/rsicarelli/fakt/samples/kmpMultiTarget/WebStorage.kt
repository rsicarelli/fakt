// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake

/**
 * Declared in `webMain`, the intermediate source set shared by js and wasmJs. It uses the
 * commonMain `expect class DeviceInfo` and the commonMain `StorageKey` data type.
 */
@Fake
interface WebStorage {
    /** The device the storage lives on (commonMain expect type). */
    val device: DeviceInfo

    /** Reads the value stored under [key]. */
    fun read(key: StorageKey): String?

    /** Stores [value] under [key]. */
    fun write(key: StorageKey, value: String)
}
