// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.rsicarelli.fakt.samples.kmpmultimodule.features.settings

import app.cash.turbine.test
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

class SettingsViewModelTest {

    companion object {
        // Helper to create test settings
        private fun createTestSettings(
            theme: String = "light",
            language: String = "en",
            notificationsEnabled: Boolean = true,
            fontSize: Int = 14,
        ) = AppSettings(
            theme = theme,
            language = language,
            notificationsEnabled = notificationsEnabled,
            fontSize = fontSize,
        )
    }

    // ============================================================================
    // LOAD SETTINGS TESTS
    // ============================================================================

    @Test
    fun `GIVEN SettingsViewModel WHEN loading settings THEN should transition to Success`() =
        runTest {
            // Given
            val testSettings = createTestSettings()
            val settingsUseCase = fakeSettingsUseCase {
                getSettings { _, _ -> testSettings }
            }

            val viewModel = factorySettingsViewModel(settingsUseCase = settingsUseCase)

            // When
            viewModel.state.test {
                assertEquals(SettingsState.Idle, awaitItem())

                viewModel.loadSettings()
                advanceUntilIdle()

                assertEquals(SettingsState.Loading, awaitItem())
                val successState = awaitItem()
                assertTrue(successState is SettingsState.Success)
                assertEquals(testSettings, successState.settings)
            }
        }

    @Test
    fun `GIVEN settings load failure WHEN loading THEN should show Error with defaults`() =
        runTest {
            // Given
            val settingsUseCase =
                fakeSettingsUseCase {
                    getSettings { _, _ ->
                        throw RuntimeException("Storage error")
                    }
                }

            val viewModel = factorySettingsViewModel(settingsUseCase = settingsUseCase)

            // When
            viewModel.loadSettings()
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is SettingsState.Error)
                assertEquals("Storage error", state.message)
            }
        }

    // ============================================================================
    // OPTIMISTIC UPDATE TESTS
    // ============================================================================

    @Test
    fun `GIVEN Success state WHEN updating settings THEN should show Updating then Success`() =
        runTest {
            // Given
            val originalSettings = createTestSettings(theme = "light")
            val updatedSettings = originalSettings.copy(theme = "dark")

            val settingsUseCase =
                fakeSettingsUseCase {
                    getSettings { _, _ -> originalSettings }
                    updateSettings { _, _, _ -> true }
                }

            val viewModel = factorySettingsViewModel(settingsUseCase = settingsUseCase)

            // Load initial settings
            viewModel.loadSettings()
            advanceUntilIdle()

            // When - Update settings
            viewModel.state.test {
                assertTrue(awaitItem() is SettingsState.Success)

                viewModel.updateSettings(updatedSettings)
                advanceUntilIdle()

                // Then - Optimistic update shows Updating state immediately
                val updatingState = awaitItem()
                assertTrue(updatingState is SettingsState.Updating)
                assertEquals("dark", updatingState.settings.theme)

                // Then - Success after storage update
                val successState = awaitItem()
                assertTrue(successState is SettingsState.Success)
                assertEquals("dark", successState.settings.theme)
            }
        }

    @Test
    fun `GIVEN update failure WHEN updating settings THEN should rollback to previous state`() =
        runTest {
            // Given
            val originalSettings = createTestSettings(theme = "light")
            val updatedSettings = originalSettings.copy(theme = "dark")

            val settingsUseCase =
                fakeSettingsUseCase {
                    getSettings { _, _ -> originalSettings }
                    updateSettings { _, _, _ -> false } // Update fails
                }

            val viewModel = factorySettingsViewModel(settingsUseCase = settingsUseCase)

            // Load initial settings
            viewModel.loadSettings()
            advanceUntilIdle()

            // When - Update settings (will fail)
            viewModel.state.test {
                assertTrue(awaitItem() is SettingsState.Success)

                viewModel.updateSettings(updatedSettings)
                advanceUntilIdle()

                // Optimistic update
                val updatingState = awaitItem()
                assertTrue(updatingState is SettingsState.Updating)
                assertEquals("dark", updatingState.settings.theme)

                // Then - Rollback to original after failure
                val rolledBackState = awaitItem()
                assertTrue(rolledBackState is SettingsState.Success)
                assertEquals("light", rolledBackState.settings.theme)
            }
        }

    // ============================================================================
    // RESET TO DEFAULTS TESTS
    // ============================================================================

    @Test
    fun `GIVEN modified settings WHEN resetting to defaults THEN should restore default values`() =
        runTest {
            // Given
            val modifiedSettings = createTestSettings(theme = "dark", fontSize = 18)
            val defaultSettings = createTestSettings(theme = "light", fontSize = 14)

            val settingsUseCase =
                fakeSettingsUseCase {
                    getSettings { _, _ -> modifiedSettings }
                    resetToDefaults { _, _ -> defaultSettings }
                }

            val viewModel = factorySettingsViewModel(settingsUseCase = settingsUseCase)

            // Load modified settings first
            viewModel.loadSettings()
            advanceUntilIdle()

            // When - Reset to defaults
            viewModel.state.test {
                val currentState = awaitItem()
                assertTrue(currentState is SettingsState.Success)
                assertEquals("dark", currentState.settings.theme)

                viewModel.resetToDefaults()
                advanceUntilIdle()

                assertEquals(SettingsState.Loading, awaitItem())
                val successState = awaitItem()
                assertTrue(successState is SettingsState.Success)
                assertEquals("light", successState.settings.theme)
                assertEquals(14, successState.settings.fontSize)
            }
        }

    // ============================================================================
    // INDIVIDUAL SETTING UPDATE TESTS
    // ============================================================================

    @Test
    fun `GIVEN Success state WHEN updating theme THEN should update only theme`() =
        runTest {
            // Given
            val originalSettings = createTestSettings(theme = "light", fontSize = 14)

            val settingsUseCase =
                fakeSettingsUseCase {
                    getSettings { _, _ -> originalSettings }
                    updateSettings { _, _, _ -> true }
                }

            val viewModel = factorySettingsViewModel(settingsUseCase = settingsUseCase)

            // Load initial settings
            viewModel.loadSettings()
            advanceUntilIdle()

            // When - Update only theme
            viewModel.state.test {
                assertTrue(awaitItem() is SettingsState.Success)

                viewModel.updateTheme("dark")
                advanceUntilIdle()

                // Optimistic update
                val updatingState = awaitItem()
                assertTrue(updatingState is SettingsState.Updating)
                assertEquals("dark", updatingState.settings.theme)
                assertEquals(14, updatingState.settings.fontSize) // Other settings preserved

                // Final success
                val successState = awaitItem()
                assertTrue(successState is SettingsState.Success)
                assertEquals("dark", successState.settings.theme)
            }
        }

    // ============================================================================
    // CONCURRENCY TESTS
    // ============================================================================

    @Test
    fun `GIVEN SettingsViewModel WHEN 10 concurrent updates THEN should be thread safe`() =
        runTest {
            // Given
            val originalSettings = createTestSettings()
            val settingsUseCase =
                fakeSettingsUseCase {
                    getSettings { _, _ -> originalSettings }
                    updateSettings { _, _, _ ->
                        delay(10)
                        true
                    }
                }

            val viewModel = factorySettingsViewModel(settingsUseCase = settingsUseCase)

            // Load initial settings
            viewModel.loadSettings()
            advanceUntilIdle()

            // When - 10 concurrent updates
            repeat(10) { index ->
                launch {
                    viewModel.updateSettings(originalSettings.copy(theme = "theme$index"))
                }
            }
            advanceUntilIdle()

            // Then - Fakt tracks all 10 update calls!
            settingsUseCase.updateSettingsCallCount.test {
                assertEquals(10, awaitItem())
            }
        }

    // ============================================================================
    // FAKT CALL COUNT TESTS
    // ============================================================================

    @Test
    fun `GIVEN SettingsViewModel WHEN loading settings THEN should track getSettings call count`() =
        runTest {
            // Given
            val settingsUseCase =
                fakeSettingsUseCase {
                    getSettings { _, _ -> createTestSettings() }
                }

            val viewModel = factorySettingsViewModel(settingsUseCase = settingsUseCase)

            // When
            viewModel.loadSettings()
            advanceUntilIdle()

            // Then - Fakt tracks the call automatically!
            settingsUseCase.getSettingsCallCount.test {
                assertEquals(1, awaitItem())
            }
        }

    @Test
    fun `GIVEN SettingsViewModel WHEN updating settings THEN should track updateSettings call count`() =
        runTest {
            // Given
            val originalSettings = createTestSettings()
            val settingsUseCase = fakeSettingsUseCase {
                getSettings { _, _ -> originalSettings }
                updateSettings { _, _, _ -> true }
            }

            val viewModel = factorySettingsViewModel(settingsUseCase = settingsUseCase)

            // Load initial
            viewModel.loadSettings()
            advanceUntilIdle()

            // When - Update
            viewModel.updateSettings(originalSettings.copy(theme = "dark"))
            advanceUntilIdle()

            // Then - Fakt tracks it!
            settingsUseCase.updateSettingsCallCount.test {
                assertEquals(1, awaitItem())
            }
        }

    // ============================================================================
    // LOGGER VALIDATION TESTS
    // ============================================================================

    @Test
    fun `GIVEN SettingsViewModel WHEN executing operations THEN should log messages`() =
        runTest {
            // Given
            val loggedMessages = mutableListOf<String>()
            val logger =
                fakeLogger {
                    info { message, _ ->
                        loggedMessages.add(message)
                    }
                }

            val settingsUseCase =
                fakeSettingsUseCase {
                    getSettings { _, _ -> createTestSettings() }
                }

            val viewModel =
                factorySettingsViewModel(settingsUseCase = settingsUseCase, logger = logger)

            // When
            viewModel.loadSettings()
            advanceUntilIdle()

            // Then
            assertTrue(loggedMessages.any { it.contains("Loading app settings") })
            assertTrue(loggedMessages.any { it.contains("Settings loaded successfully") })
        }

    // ============================================================================
    // HELPER FACTORY
    // ============================================================================

    private fun TestScope.factorySettingsViewModel(
        settingsUseCase: SettingsUseCase = fakeSettingsUseCase(),
        storage: KeyValueStorage = fakeKeyValueStorage(),
        logger: Logger = fakeLogger(),
    ) = SettingsViewModel(
        settingsUseCase = settingsUseCase,
        storage = storage,
        logger = logger,
        scope = this,
    )
}
