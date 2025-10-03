// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package kmp.test.native

import com.rsicarelli.fakt.Fake

/**
 * RULE TEST 3: nativeMain → nativeTest
 * These interfaces should generate fakes in nativeTest source set
 * FALLBACK TEST: If nativeTest doesn't exist, should fallback to commonTest
 */

@Fake
interface NativeMemoryService {
    val totalMemory: Long
    val freeMemory: Long

    fun allocate(size: Long): Boolean

    fun deallocate(pointer: Long)

    suspend fun gc(): Boolean

    fun <T> withMemoryPool(action: () -> T): T
}

@Fake
interface NativeFileSystemService {
    val separator: String
    val homeDirectory: String

    fun exists(path: String): Boolean

    fun createDirectory(path: String): Boolean

    suspend fun listFiles(path: String): List<String>

    fun <T> withPermissions(
        path: String,
        permissions: Int,
        action: () -> T,
    ): T
}

@Fake
interface NativeSystemService<TConfig> {
    val platform: String
    val architecture: String

    fun getEnvironmentVariable(name: String): String?

    fun setEnvironmentVariable(
        name: String,
        value: String,
    ): Boolean

    suspend fun executeCommand(command: String): String

    fun configure(config: TConfig)

    fun <R> withSystemLock(action: () -> R): R
}

// Native-specific data classes
data class NativeSystemInfo(
    val platform: String,
    val architecture: String,
    val version: String,
    val capabilities: Set<String>,
)

data class NativeFileInfo(
    val path: String,
    val size: Long,
    val permissions: Int,
    val lastModified: Long,
)
