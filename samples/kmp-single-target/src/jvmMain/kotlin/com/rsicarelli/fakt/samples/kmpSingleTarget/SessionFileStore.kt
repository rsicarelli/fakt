// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleTarget

import com.rsicarelli.fakt.Fake
import java.io.File

/**
 * JVM-only store: its signature uses `java.io.File` (a platform type) and [Session] (a common
 * type). Its fake is written to the platform output and wired into jvmTest only — a commonTest
 * source could not compile against it.
 */
@Fake
interface SessionFileStore {
    /** Writes [session] under [directory]; returns the written file. */
    fun write(directory: File, session: Session): File
}

/** JVM logic that combines a platform fake with a common fake in jvmTest. */
class SessionExporter(
    private val repository: SessionRepository,
    private val store: SessionFileStore,
) {
    /** Exports session [id] to [directory], or returns null when it doesn't exist. */
    suspend fun export(id: String, directory: File): File? =
        repository.load(id)?.let { session -> store.write(directory, session) }
}
