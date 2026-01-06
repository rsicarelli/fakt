// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake

/**
 * WebAssembly-only service for platform-specific operations.
 *
 * This interface is defined in wasmJsMain and should generate a fake
 * in wasmJsTest ONLY. It should NOT be available in other platform tests.
 *
 * Tests: WASM module interactions, memory management (WASM-specific).
 */
@Fake
interface WasmOnlyService {
    /**
     * Allocate memory in WASM linear memory.
     */
    fun allocateMemory(size: Int): Int

    /**
     * Free memory in WASM linear memory.
     */
    fun freeMemory(pointer: Int): Boolean

    /**
     * Call a WASM function by name.
     */
    fun callWasmFunction(name: String, args: List<Int>): Int

    /**
     * Current WASM memory size in pages.
     */
    val memoryPages: Int
}
