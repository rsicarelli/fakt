// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpNativeOnly.allNative

import com.rsicarelli.fakt.Fake
import platform.posix.size_t

/**
 * A native buffer described with the commonized platform.posix `size_t` (shared by the linux and
 * ios targets). The module is all-native, so commonMain is a shared-native compilation and the
 * Kotlin/Native driver generates this fake.
 */
@Fake
interface NativeBuffer {
    fun capacity(): size_t

    fun resize(size: size_t): Boolean

    val label: String
}

/** Locks the common fragment: commonMain only compiles with its actuals in view. */
expect fun platformFamily(): String
