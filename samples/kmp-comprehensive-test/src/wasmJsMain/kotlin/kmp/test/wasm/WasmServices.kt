// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package kmp.test.wasm

import com.rsicarelli.fakt.Fake

/**
 * RULE TEST 7: wasmJsMain → wasmJsTest
 * These interfaces should generate fakes in wasmJsTest source set
 * FALLBACK TEST: If wasmJsTest doesn't exist, should fallback to commonTest
 */

@Fake
interface WasmMemoryService {
    val memorySize: Int
    val maxMemorySize: Int

    fun allocateMemory(size: Int): Int
    fun deallocateMemory(pointer: Int)
    suspend fun growMemory(pages: Int): Boolean
    fun <T> withMemoryView(action: () -> T): T
}

@Fake
interface WasmModuleService<TExport> {
    val exports: Map<String, TExport>
    val imports: Map<String, Any>

    suspend fun loadModule(wasmBytes: ByteArray): Boolean
    fun <T> callExport(name: String, args: Array<Any>): T?
    fun setImport(name: String, value: Any)
    fun <R> withModule(action: () -> R): R
}

@Fake
interface WasmInteropService {
    val jsContext: Any?
    val wasmContext: Any?

    suspend fun callJs(functionName: String, args: Array<Any>): Any?
    suspend fun callWasm(functionName: String, args: Array<Any>): Any?
    fun <T> bridge(jsValue: Any): T?
    fun <R> withInterop(action: () -> R): R
}

// WASM-specific data classes
data class WasmModule(
    val name: String,
    val size: Int,
    val exports: List<String>,
    val imports: List<String>
)

data class WasmMemoryInfo(
    val currentPages: Int,
    val maxPages: Int?,
    val pageSize: Int,
    val shared: Boolean
)
