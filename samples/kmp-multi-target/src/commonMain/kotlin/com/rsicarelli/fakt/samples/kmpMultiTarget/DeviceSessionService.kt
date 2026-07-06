// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake

/**
 * Exercises an expect type ([DeviceInfo]) at every signature depth the generator renders:
 * property, parameter, return, nullable return, generic argument, and suspend variant.
 *
 * The commonTest suite drives the generated `fakeDeviceSessionService {}` on every target,
 * CI-locking the metadata-driver producer against the issue #79 expect/actual reproducer.
 */
@Fake
interface DeviceSessionService {
    /** Expect type as a property. */
    val device: DeviceInfo

    /** Expect type as a parameter. */
    fun bind(device: DeviceInfo): Boolean

    /** Expect type as a return type. */
    fun activeDevice(): DeviceInfo

    /** Expect type as a nullable return type. */
    fun lastKnownDevice(): DeviceInfo?

    /** Expect type as a generic argument. */
    fun history(): List<DeviceInfo>

    /** Expect type on a suspend function. */
    suspend fun refresh(device: DeviceInfo): DeviceInfo
}
