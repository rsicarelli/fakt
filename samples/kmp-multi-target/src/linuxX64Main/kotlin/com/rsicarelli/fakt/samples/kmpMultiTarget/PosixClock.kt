// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake
import kotlinx.cinterop.CPointer
import platform.posix.FILE
import platform.posix.timespec

/**
 * Clock and file access backed by platform.posix (linuxX64Main only).
 *
 * The signature uses platform.posix types the metadata compiler cannot see, so only the Kotlin/Native
 * driver can analyse it. The fake is generated into linuxX64Test.
 */
@Fake
interface PosixClock {
    fun now(): timespec?

    fun open(path: String): CPointer<FILE>?

    fun close(file: CPointer<FILE>?): Int
}
