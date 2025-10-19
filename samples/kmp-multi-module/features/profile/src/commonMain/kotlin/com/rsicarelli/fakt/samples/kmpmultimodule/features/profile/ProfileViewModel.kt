// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.profile

import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthSession
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Profile screen state.
 */
sealed class ProfileState {
    data object Idle : ProfileState()
    data object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Updating(val profile: UserProfile) : ProfileState() // Optimistic update
    data class Error(val message: String) : ProfileState()
}

/**
 * Vanilla ViewModel for Profile feature (no Android dependencies).
 *
 * Demonstrates production-ready patterns:
 * - StateFlow for reactive state management
 * - K2.2+ backing fields pattern (get() = _field)
 * - Thread-safe state updates with .update { }
 * - Optimistic updates pattern (update UI before API call)
 * - Update rollback on error
 * - Coroutine scope for async operations
 *
 * This serves as a real-world example for testing with Fakt + Turbine.
 *
 * NOTE: Call counts are automatically tracked by Fakt fakes!
 * Use `profileUseCase.getProfileCallCount` and `updateProfileCallCount` in tests.
 */
class ProfileViewModel(
    private val profileUseCase: ProfileUseCase,
    private val session: AuthSession,
    private val storage: KeyValueStorage,
    private val logger: Logger,
    private val scope: CoroutineScope,
) {
    // State - K2.2+ backing field pattern
    private val _state = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val state: StateFlow<ProfileState>
        get() = _state

    /**
     * Load user profile.
     * Transitions: Idle -> Loading -> Success/Error
     */
    fun loadProfile(userId: String) {
        scope.launch {
            try {
                _state.update { ProfileState.Loading }

                logger.info("Loading profile for user: $userId")
                val profile = profileUseCase.getProfile(userId, session, storage, logger)

                if (profile != null) {
                    _state.update { ProfileState.Success(profile) }
                    logger.info("Profile loaded successfully: ${profile.displayName}")
                } else {
                    _state.update { ProfileState.Error("Profile not found") }
                    logger.warn("Profile not found for user: $userId")
                }
            } catch (e: Exception) {
                _state.update { ProfileState.Error(e.message ?: "Unknown error") }
                logger.error("Failed to load profile", e)
            }
        }
    }

    /**
     * Update user profile with OPTIMISTIC UPDATE pattern.
     *
     * Flow:
     * 1. Immediately update UI with new data (Optimistic)
     * 2. Call API to persist changes
     * 3. On success: keep the optimistic update
     * 4. On failure: rollback to previous state
     */
    fun updateProfile(updatedProfile: UserProfile) {
        scope.launch {
            // Save current state for potential rollback
            val currentState = _state.value
            val previousProfile =
                when (currentState) {
                    is ProfileState.Success -> currentState.profile
                    else -> null
                }

            try {
                // OPTIMISTIC UPDATE - Update UI immediately
                _state.update { ProfileState.Updating(updatedProfile) }
                logger.info("Optimistically updating profile: ${updatedProfile.displayName}")

                // Persist to backend/storage
                val success = profileUseCase.updateProfile(updatedProfile, storage, logger)

                if (success) {
                    _state.update { ProfileState.Success(updatedProfile) }
                    logger.info("Profile updated successfully")
                } else {
                    // Rollback on failure
                    if (previousProfile != null) {
                        _state.update { ProfileState.Success(previousProfile) }
                        logger.warn("Profile update failed, rolled back to previous state")
                    } else {
                        _state.update { ProfileState.Error("Update failed") }
                    }
                }
            } catch (e: Exception) {
                // Rollback on exception
                if (previousProfile != null) {
                    _state.update { ProfileState.Success(previousProfile) }
                    logger.error("Profile update failed with exception, rolled back", e)
                } else {
                    _state.update { ProfileState.Error(e.message ?: "Unknown error") }
                    logger.error("Profile update failed with exception", e)
                }
            }
        }
    }

    /**
     * Delete user account.
     * Resets state to Idle after deletion.
     */
    fun deleteAccount() {
        scope.launch {
            try {
                logger.warn("Deleting account for user: ${session.userId}")
                _state.update { ProfileState.Loading }

                // In real app, would call API to delete account
                // For now, just reset to Idle
                _state.update { ProfileState.Idle }
                logger.info("Account deleted successfully")
            } catch (e: Exception) {
                _state.update { ProfileState.Error(e.message ?: "Unknown error") }
                logger.error("Failed to delete account", e)
            }
        }
    }

    /**
     * Retry loading profile after error.
     */
    fun retry(userId: String) {
        scope.launch {
            val currentState = _state.value
            if (currentState is ProfileState.Error) {
                logger.info("Retrying profile load")
                loadProfile(userId)
            }
        }
    }
}
