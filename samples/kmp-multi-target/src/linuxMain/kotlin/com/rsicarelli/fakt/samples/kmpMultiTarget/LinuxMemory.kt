// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import platform.posix.size_t

/**
 * Memory access through the commonized platform.posix (linuxMain, shared by linuxX64 and
 * linuxArm64). It is not in nativeMain because that set also holds mingwX64.
 */
@Fake
interface LinuxMemory {
    fun allocate(size: size_t): CPointer<ByteVar>?

    fun release(pointer: CPointer<ByteVar>?)
}
