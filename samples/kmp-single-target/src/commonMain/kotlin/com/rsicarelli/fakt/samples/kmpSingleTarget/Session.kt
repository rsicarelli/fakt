// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleTarget

import com.rsicarelli.fakt.Fake

/** A signed-in user session. Common type rendered by both the common and the JVM fakes. */
data class Session(val id: String, val userName: String)

/**
 * Common repository. In a single-target project there is no commonMain metadata compilation, so
 * `faktGenerateJvmMain` owns this fake and writes it to the `commonTest` output.
 */
@Fake
interface SessionRepository {
    /** Loads a session by id, or null when it doesn't exist. */
    suspend fun load(id: String): Session?

    /** Persists [session]; returns whether it was stored. */
    suspend fun save(session: Session): Boolean
}

/** Common analytics sink, faked for commonTest. */
@Fake
interface AnalyticsTracker {
    /** Records a named event. */
    fun track(event: String)
}

/** Resolved per platform; its `actual` lives in jvmMain. */
expect fun platformName(): String

/** Common business logic under test in commonTest. */
class SessionManager(
    private val repository: SessionRepository,
    private val tracker: AnalyticsTracker,
) {
    /** Resumes session [id], tracking whether it was found. */
    suspend fun resume(id: String): Session? {
        val session = repository.load(id)
        tracker.track(if (session != null) "session_resumed" else "session_missing")
        return session
    }

    /** Signs [userName] in on this platform and persists the new session. */
    suspend fun signIn(userName: String): Session {
        val session = Session(id = "${platformName()}-$userName", userName = userName)
        repository.save(session)
        tracker.track("signed_in")
        return session
    }
}
