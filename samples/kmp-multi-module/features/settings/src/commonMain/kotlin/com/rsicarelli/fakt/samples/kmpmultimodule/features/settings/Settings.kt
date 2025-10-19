// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.settings

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage

// Settings feature - App settings and preferences
data class AppSettings(val theme: String, val language: String, val notificationsEnabled: Boolean, val fontSize: Int)

@Fake
interface SettingsUseCase {
    suspend fun getSettings(storage: KeyValueStorage, logger: Logger): AppSettings

    suspend fun updateSettings(settings: AppSettings, storage: KeyValueStorage, logger: Logger): Boolean

    suspend fun resetToDefaults(storage: KeyValueStorage, logger: Logger): AppSettings
}
