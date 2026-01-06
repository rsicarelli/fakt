// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake

/**
 * JVM-only service for platform-specific operations.
 *
 * This interface is defined in jvmMain and should generate a fake
 * in jvmTest ONLY. It should NOT be available in other platform tests.
 *
 * Tests: File I/O and system properties (JVM-specific).
 */
@Fake
interface JvmOnlyService {
    /**
     * Read a file from the filesystem (JVM-specific).
     */
    fun readFile(path: String): String

    /**
     * Write content to a file (JVM-specific).
     */
    fun writeFile(path: String, content: String): Boolean

    /**
     * Get a system property (JVM-specific).
     */
    fun getSystemProperty(key: String): String?

    /**
     * Current working directory.
     */
    val workingDirectory: String
}
