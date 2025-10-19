// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.settings

import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Settings screen state.
 */
sealed class SettingsState {
    data object Idle : SettingsState()
    data object Loading : SettingsState()
    data class Success(val settings: AppSettings) : SettingsState()
    data class Updating(val settings: AppSettings) : SettingsState() // Optimistic update
    data class Error(val message: String) : SettingsState()
}

/**
 * Vanilla ViewModel for Settings feature (no Android dependencies).
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
 * Use `settingsUseCase.getSettingsCallCount` and `updateSettingsCallCount` in tests.
 */
class SettingsViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val storage: KeyValueStorage,
    private val logger: Logger,
    private val scope: CoroutineScope,
) {
    // State - K2.2+ backing field pattern
    private val _state = MutableStateFlow<SettingsState>(SettingsState.Idle)
    val state: StateFlow<SettingsState>
        get() = _state

    /**
     * Load app settings.
     * Transitions: Idle -> Loading -> Success/Error
     */
    fun loadSettings() {
        scope.launch {
            try {
                _state.update { SettingsState.Loading }

                logger.info("Loading app settings")
                val settings = settingsUseCase.getSettings(storage, logger)

                _state.update { SettingsState.Success(settings) }
                logger.info("Settings loaded successfully: $settings")
            } catch (e: Exception) {
                _state.update { SettingsState.Error(e.message ?: "Unknown error") }
                logger.error("Failed to load settings", e)
            }
        }
    }

    /**
     * Update app settings with OPTIMISTIC UPDATE pattern.
     *
     * Flow:
     * 1. Immediately update UI with new data (Optimistic)
     * 2. Call API/storage to persist changes
     * 3. On success: keep the optimistic update
     * 4. On failure: rollback to previous state
     */
    fun updateSettings(updatedSettings: AppSettings) {
        scope.launch {
            // Save current state for potential rollback
            val currentState = _state.value
            val previousSettings =
                when (currentState) {
                    is SettingsState.Success -> currentState.settings
                    else -> null
                }

            try {
                // OPTIMISTIC UPDATE - Update UI immediately
                _state.update { SettingsState.Updating(updatedSettings) }
                logger.info("Optimistically updating settings: $updatedSettings")

                // Persist to storage/backend
                val success = settingsUseCase.updateSettings(updatedSettings, storage, logger)

                if (success) {
                    _state.update { SettingsState.Success(updatedSettings) }
                    logger.info("Settings updated successfully")
                } else {
                    // Rollback on failure
                    if (previousSettings != null) {
                        _state.update { SettingsState.Success(previousSettings) }
                        logger.warn("Settings update failed, rolled back to previous state")
                    } else {
                        _state.update { SettingsState.Error("Update failed") }
                    }
                }
            } catch (e: Exception) {
                // Rollback on exception
                if (previousSettings != null) {
                    _state.update { SettingsState.Success(previousSettings) }
                    logger.error("Settings update failed with exception, rolled back", e)
                } else {
                    _state.update { SettingsState.Error(e.message ?: "Unknown error") }
                    logger.error("Settings update failed with exception", e)
                }
            }
        }
    }

    /**
     * Reset settings to default values.
     */
    fun resetToDefaults() {
        scope.launch {
            try {
                _state.update { SettingsState.Loading }

                logger.warn("Resetting settings to defaults")
                val defaultSettings = settingsUseCase.resetToDefaults(storage, logger)

                _state.update { SettingsState.Success(defaultSettings) }
                logger.info("Settings reset to defaults: $defaultSettings")
            } catch (e: Exception) {
                _state.update { SettingsState.Error(e.message ?: "Unknown error") }
                logger.error("Failed to reset settings", e)
            }
        }
    }

    /**
     * Update a single setting (convenience method).
     * Uses optimistic update pattern internally.
     */
    fun updateTheme(theme: String) {
        val currentState = _state.value
        if (currentState is SettingsState.Success) {
            val updated = currentState.settings.copy(theme = theme)
            updateSettings(updated)
        }
    }
}
