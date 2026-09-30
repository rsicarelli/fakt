// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package spike

import com.rsicarelli.fakt.Fake
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import spike.cfixture.Point
import cnames.structs.Opaque

/** C3: user cinterop klib types, including a forward-declared struct. */
@OptIn(ExperimentalForeignApi::class)
@Fake
interface CinteropPort {
    fun newOpaque(): CPointer<Opaque>?
    fun sum(p: CValue<Point>): Int
}
