// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.profile

import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthSession
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.UserInfo
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.fakeLogger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.fakeKeyValueStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProfileUseCaseTest {
    @Test
    fun `GIVEN ProfileUseCase WHEN getting profile THEN should return user profile`() =
        runTest {
            val storage = fakeKeyValueStorage {}
            val logger = fakeLogger {}
            val session = AuthSession("user-123", "token", "refresh", 0L, UserInfo("user-123", "user@example.com", "John"))
            val useCase =
                fakeProfileUseCase {
                    getProfile { _, _, _, _ -> UserProfile("user-123", "John", "user@example.com", null) }
                }
            val profile = useCase.getProfile("user-123", session, storage, logger)
            assertNotNull(profile)
        }
}
