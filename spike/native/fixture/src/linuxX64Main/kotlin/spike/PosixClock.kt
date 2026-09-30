// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package spike

import com.rsicarelli.fakt.Fake
import kotlinx.cinterop.CPointer
import platform.posix.FILE
import platform.posix.timespec

/** C1: a leaf linuxX64 fake over platform.posix types. */
@Fake
interface PosixClock {
    fun now(): timespec?
    fun open(path: String): CPointer<FILE>?
    val clockId: Int
}
