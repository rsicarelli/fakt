// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

actual class DeviceInfo(actual val name: String, actual val isEmulator: Boolean)

actual fun currentDevice(): DeviceInfo = DeviceInfo(name = "JS", isEmulator = false)
