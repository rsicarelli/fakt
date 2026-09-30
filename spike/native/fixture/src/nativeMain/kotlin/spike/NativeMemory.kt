// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package spike

import com.rsicarelli.fakt.Fake
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import platform.posix.size_t

/** C2: shared nativeMain over commonized posix + cinterop runtime types. */
@Fake
interface NativeMemory {
    fun alloc(size: size_t): CPointer<ByteVar>?
    fun free(ptr: CPointer<ByteVar>?)
}
