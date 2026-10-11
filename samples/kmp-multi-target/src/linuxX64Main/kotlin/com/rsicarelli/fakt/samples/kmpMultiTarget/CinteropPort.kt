// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import cnames.structs.Opaque
import com.rsicarelli.fakt.Fake
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import sampleport.Point

/**
 * Port over the `sampleport` cinterop library (linuxX64Main only).
 *
 * `Opaque` is a forward-declared C struct (cnames.structs.Opaque): the Kotlin/Native frontend
 * resolves it, the metadata compiler does not.
 */
@Fake
interface CinteropPort {
    fun open(): CPointer<Opaque>?

    fun origin(): CValue<Point>?

    fun translate(point: CValue<Point>?, dx: Int): CValue<Point>?
}
