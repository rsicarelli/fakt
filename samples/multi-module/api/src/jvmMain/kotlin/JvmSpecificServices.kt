// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package api.jvm

import com.rsicarelli.fakt.Fake

// ============================================================================
// JVM-SPECIFIC INTERFACES - Java platform specific features
// Note: Using simple types that work with current compiler
// ============================================================================

@Fake
interface FileSystemService {
    fun readFile(path: String): String

    fun writeFile(
        path: String,
        content: String,
    ): Boolean

    fun createDirectory(path: String): Boolean

    fun deleteFile(path: String): Boolean

    val currentDirectory: String
}

@Fake
interface DatabaseService {
    suspend fun connect(
        url: String,
        username: String,
        password: String,
    ): Boolean

    suspend fun execute(sql: String): Int

    suspend fun close()

    val isConnected: Boolean
}

@Fake
interface JvmSystemService {
    val javaVersion: String
    val osName: String
    val availableProcessors: Int
    val maxMemory: Long

    fun getCurrentTime(): String

    fun getSystemProperty(key: String): String?

    fun getEnvironmentVariable(key: String): String?
}
