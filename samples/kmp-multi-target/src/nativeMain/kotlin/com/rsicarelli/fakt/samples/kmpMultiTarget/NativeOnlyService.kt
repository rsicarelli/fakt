// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake

/**
 * Native-only service for platform-specific operations.
 *
 * This interface is defined in nativeMain (shared by iOS, macOS, Linux, Windows)
 * and should generate a fake in nativeTest ONLY.
 * It should NOT be available in JVM, JS, or WASM tests.
 *
 * Tests: C interop, memory management (Native-specific abstractions).
 */
@Fake
interface NativeOnlyService {
    /**
     * Allocate native memory using malloc-like semantics.
     */
    fun allocateNativeMemory(size: Long): Long

    /**
     * Free native memory using free-like semantics.
     */
    fun freeNativeMemory(pointer: Long)

    /**
     * Call a C function via interop.
     */
    fun callCFunction(name: String): Int

    /**
     * Current platform architecture (e.g., "x64", "arm64").
     */
    val architecture: String
}
