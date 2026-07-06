// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

// nativeMain sits above all native leaves in the default hierarchy (ios/macos/linux/mingw),
// so one actual pair covers every native target.
actual class DeviceInfo(actual val name: String, actual val isEmulator: Boolean)

actual fun currentDevice(): DeviceInfo = DeviceInfo(name = "Native", isEmulator = false)
