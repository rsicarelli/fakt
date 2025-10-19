// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.rsicarelli.fakt.samples.kmpmultimodule.features.profile

import app.cash.turbine.test
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthSession
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.UserInfo
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.fakeLogger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.fakeKeyValueStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for ProfileViewModel demonstrating:
 * - Turbine for StateFlow testing
 * - K2.2+ backing fields pattern
 * - OPTIMISTIC UPDATES pattern (update UI before API)
 * - Rollback on error
 * - Concurrency and thread safety
 * - Factory pattern with TestScope extension
 *
 * This serves as a production-ready example for the Fakt community.
 */
class ProfileViewModelTest {

    companion object {
        // Helper to create test session
        private fun createTestSession(userId: String = "user-123") =
            AuthSession(
                userId = userId,
                accessToken = "test-token",
                refreshToken = "refresh-token",
                expiresAt = 1735689600000L,
                userInfo =
                    UserInfo(
                        id = userId,
                        email = "test@example.com",
                        displayName = "Test User",
                    ),
            )

        // Helper to create test profile
        private fun createTestProfile(
            userId: String = "user-123",
            name: String = "Test User",
            email: String = "test@example.com",
        ) = UserProfile(
            userId = userId,
            displayName = name,
            email = email,
            avatarUrl = "https://example.com/avatar.jpg",
        )
    }

    // ============================================================================
    // LOAD PROFILE TESTS
    // ============================================================================

    @Test
    fun `GIVEN valid userId WHEN loading profile THEN should transition to Success`() =
        runTest {
            // Given
            val testProfile = createTestProfile()
            val profileUseCase = fakeProfileUseCase {
                getProfile { _, _, _, _ -> testProfile }
            }

            val viewModel = factoryProfileViewModel(profileUseCase = profileUseCase)

            // When
            viewModel.state.test {
                assertEquals(ProfileState.Idle, awaitItem())

                viewModel.loadProfile("user-123")
                advanceUntilIdle()

                assertEquals(ProfileState.Loading, awaitItem())
                val successState = awaitItem()
                assertTrue(successState is ProfileState.Success)
                assertEquals(testProfile, successState.profile)
            }
        }

    @Test
    fun `GIVEN profile not found WHEN loading THEN should show Error state`() =
        runTest {
            // Given
            val profileUseCase = fakeProfileUseCase {
                getProfile { _, _, _, _ -> null } // Profile not found
            }

            val viewModel = factoryProfileViewModel(profileUseCase = profileUseCase)

            // When
            viewModel.loadProfile("non-existent-user")
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is ProfileState.Error)
                assertTrue(state.message.contains("not found"))
            }
        }

    @Test
    fun `GIVEN API failure WHEN loading profile THEN should show Error state`() =
        runTest {
            // Given
            val profileUseCase = fakeProfileUseCase {
                getProfile { _, _, _, _ ->
                    throw RuntimeException("Network error")
                }
            }

            val viewModel = factoryProfileViewModel(profileUseCase = profileUseCase)

            // When
            viewModel.loadProfile("user-123")
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is ProfileState.Error)
                assertEquals("Network error", state.message)
            }
        }

    // ============================================================================
    // OPTIMISTIC UPDATE TESTS
    // ============================================================================

    @Test
    fun `GIVEN Success state WHEN updating profile THEN should show Updating then Success`() =
        runTest {
            // Given
            val originalProfile = createTestProfile(name = "Original Name")
            val updatedProfile = originalProfile.copy(displayName = "Updated Name")

            val profileUseCase = fakeProfileUseCase {
                getProfile { _, _, _, _ -> originalProfile }
                updateProfile { _, _, _ -> true }
            }

            val viewModel = factoryProfileViewModel(profileUseCase = profileUseCase)

            // Load initial profile
            viewModel.loadProfile("user-123")
            advanceUntilIdle()

            // When - Update profile
            viewModel.state.test {
                assertTrue(awaitItem() is ProfileState.Success)

                viewModel.updateProfile(updatedProfile)
                advanceUntilIdle()

                // Then - Optimistic update shows Updating state immediately
                val updatingState = awaitItem()
                assertTrue(updatingState is ProfileState.Updating)
                assertEquals("Updated Name", updatingState.profile.displayName)

                // Then - API success transitions to Success
                val successState = awaitItem()
                assertTrue(successState is ProfileState.Success)
                assertEquals("Updated Name", successState.profile.displayName)
            }
        }

    @Test
    fun `GIVEN update failure WHEN updating profile THEN should rollback to previous state`() =
        runTest {
            // Given
            val originalProfile = createTestProfile(name = "Original Name")
            val updatedProfile = originalProfile.copy(displayName = "Updated Name")

            val profileUseCase = fakeProfileUseCase {
                getProfile { _, _, _, _ -> originalProfile }
                updateProfile { _, _, _ -> false } // Update fails
            }

            val viewModel = factoryProfileViewModel(profileUseCase = profileUseCase)

            // Load initial profile
            viewModel.loadProfile("user-123")
            advanceUntilIdle()

            // When - Update profile (will fail)
            viewModel.state.test {
                assertTrue(awaitItem() is ProfileState.Success)

                viewModel.updateProfile(updatedProfile)
                advanceUntilIdle()

                // Optimistic update
                val updatingState = awaitItem()
                assertTrue(updatingState is ProfileState.Updating)
                assertEquals("Updated Name", updatingState.profile.displayName)

                // Then - Rollback to original after failure
                val rolledBackState = awaitItem()
                assertTrue(rolledBackState is ProfileState.Success)
                assertEquals("Original Name", rolledBackState.profile.displayName)
            }
        }

    @Test
    fun `GIVEN update exception WHEN updating profile THEN should rollback`() =
        runTest {
            // Given
            val originalProfile = createTestProfile(name = "Original Name")
            val updatedProfile = originalProfile.copy(displayName = "Updated Name")

            val profileUseCase = fakeProfileUseCase {
                getProfile { _, _, _, _ -> originalProfile }
                updateProfile { _, _, _ ->
                    throw RuntimeException("API error")
                }
            }

            val viewModel = factoryProfileViewModel(profileUseCase = profileUseCase)

            // Load initial profile
            viewModel.loadProfile("user-123")
            advanceUntilIdle()

            // When - Update profile (will throw)
            viewModel.state.test {
                assertTrue(awaitItem() is ProfileState.Success)

                viewModel.updateProfile(updatedProfile)
                advanceUntilIdle()

                // Optimistic update
                assertTrue(awaitItem() is ProfileState.Updating)

                // Then - Rollback to original after exception
                val rolledBackState = awaitItem()
                assertTrue(rolledBackState is ProfileState.Success)
                assertEquals("Original Name", rolledBackState.profile.displayName)
            }
        }

    // ============================================================================
    // CONCURRENCY TESTS
    // ============================================================================

    @Test
    fun `GIVEN ProfileViewModel WHEN 10 concurrent updates THEN should be thread safe`() =
        runTest {
            // Given
            val originalProfile = createTestProfile(name = "Original")
            val profileUseCase =
                fakeProfileUseCase {
                    getProfile { _, _, _, _ -> originalProfile }
                    updateProfile { _, _, _ ->
                        delay(10)
                        true
                    }
                }

            val viewModel = factoryProfileViewModel(profileUseCase = profileUseCase)

            // Load initial profile
            viewModel.loadProfile("user-123")
            advanceUntilIdle()

            // When - 10 concurrent updates
            repeat(10) { index ->
                launch {
                    viewModel.updateProfile(originalProfile.copy(displayName = "Update $index"))
                }
            }
            advanceUntilIdle()

            // Then - Fakt tracks all 10 update calls!
            profileUseCase.updateProfileCallCount.test {
                assertEquals(10, awaitItem())
            }
        }

    // ============================================================================
    // DELETE ACCOUNT TESTS
    // ============================================================================

    @Test
    fun `GIVEN ProfileViewModel WHEN deleting account THEN should transition to Idle`() =
        runTest {
            // Given
            val viewModel = factoryProfileViewModel()

            // When
            viewModel.state.test {
                assertEquals(ProfileState.Idle, awaitItem())

                viewModel.deleteAccount()
                advanceUntilIdle()

                assertEquals(ProfileState.Loading, awaitItem())
                assertEquals(ProfileState.Idle, awaitItem())
            }
        }

    // ============================================================================
    // RETRY LOGIC TESTS
    // ============================================================================

    @Test
    fun `GIVEN Error state WHEN retrying THEN should reload profile`() =
        runTest {
            // Given
            var shouldFail = true
            val testProfile = createTestProfile()

            val profileUseCase =
                fakeProfileUseCase {
                    getProfile { _, _, _, _ ->
                        if (shouldFail) {
                            shouldFail = false
                            throw RuntimeException("First attempt failed")
                        } else {
                            testProfile
                        }
                    }
                }

            val viewModel = factoryProfileViewModel(profileUseCase = profileUseCase)

            // When - First load fails
            viewModel.loadProfile("user-123")
            advanceUntilIdle()

            viewModel.state.test {
                assertTrue(awaitItem() is ProfileState.Error)

                // Retry
                viewModel.retry("user-123")
                advanceUntilIdle()

                // Then - Should reload and succeed
                assertEquals(ProfileState.Loading, awaitItem())
                val successState = awaitItem()
                assertTrue(successState is ProfileState.Success)
                assertEquals(testProfile.displayName, successState.profile.displayName)
            }
        }

    // ============================================================================
    // FAKT CALL COUNT TESTS
    // ============================================================================

    @Test
    fun `GIVEN ProfileViewModel WHEN loading profile THEN should track getProfile call count`() =
        runTest {
            // Given
            val profileUseCase =
                fakeProfileUseCase {
                    getProfile { _, _, _, _ -> createTestProfile() }
                }

            val viewModel = factoryProfileViewModel(profileUseCase = profileUseCase)

            // When
            viewModel.loadProfile("user-123")
            advanceUntilIdle()

            // Then - Fakt tracks the call automatically!
            profileUseCase.getProfileCallCount.test {
                assertEquals(1, awaitItem())
            }
        }

    @Test
    fun `GIVEN ProfileViewModel WHEN updating profile THEN should track updateProfile call count`() =
        runTest {
            // Given
            val originalProfile = createTestProfile()
            val profileUseCase = fakeProfileUseCase {
                getProfile { _, _, _, _ -> originalProfile }
                updateProfile { _, _, _ -> true }
            }

            val viewModel = factoryProfileViewModel(profileUseCase = profileUseCase)

            // Load initial
            viewModel.loadProfile("user-123")
            advanceUntilIdle()

            // When - Update
            viewModel.updateProfile(originalProfile.copy(displayName = "New Name"))
            advanceUntilIdle()

            // Then - Fakt tracks it!
            profileUseCase.updateProfileCallCount.test {
                assertEquals(1, awaitItem())
            }
        }

    // ============================================================================
    // LOGGER VALIDATION TESTS
    // ============================================================================

    @Test
    fun `GIVEN ProfileViewModel WHEN executing operations THEN should log messages`() =
        runTest {
            // Given
            val loggedMessages = mutableListOf<String>()
            val logger = fakeLogger {
                info { message, _ ->
                    loggedMessages.add(message)
                }
            }

            val profileUseCase = fakeProfileUseCase {
                getProfile { _, _, _, _ -> createTestProfile() }
            }

            val viewModel = factoryProfileViewModel(
                profileUseCase = profileUseCase,
                logger = logger
            )

            // When
            viewModel.loadProfile("user-123")
            advanceUntilIdle()

            // Then
            assertTrue(loggedMessages.any { it.contains("Loading profile") })
            assertTrue(loggedMessages.any { it.contains("Profile loaded successfully") })
        }

    // ============================================================================
    // HELPER FACTORY
    // ============================================================================

    private fun TestScope.factoryProfileViewModel(
        profileUseCase: ProfileUseCase = fakeProfileUseCase(),
        session: AuthSession = createTestSession(),
        storage: KeyValueStorage = fakeKeyValueStorage(),
        logger: Logger = fakeLogger(),
    ) = ProfileViewModel(
        profileUseCase = profileUseCase,
        session = session,
        storage = storage,
        logger = logger,
        scope = this,
    )
}
