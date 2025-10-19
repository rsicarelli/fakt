// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.profile

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthSession
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage

// Profile feature - User profile management
data class UserProfile(val userId: String, val displayName: String, val email: String, val avatarUrl: String?)

@Fake
interface ProfileUseCase {
    suspend fun getProfile(userId: String, session: AuthSession, storage: KeyValueStorage, logger: Logger): UserProfile?

    suspend fun updateProfile(profile: UserProfile, storage: KeyValueStorage, logger: Logger): Boolean
}
