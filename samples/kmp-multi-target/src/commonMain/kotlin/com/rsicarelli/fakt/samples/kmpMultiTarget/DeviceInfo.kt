// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

/**
 * Expect type used inside `@Fake` signatures ([DeviceSessionService]).
 *
 * From the commonMain producer's point of view these declarations are unpaired (the producer
 * compiles commonMain sources only, never the platform `actual`s) — the issue #79 blocker shape.
 * The metadata-driver producer has no IR actualizer, so it must generate the fake regardless,
 * rendering `DeviceInfo` like any other type.
 */
expect class DeviceInfo {
    val name: String
    val isEmulator: Boolean
}

/** Unpaired-from-the-producer's-view expect function. */
expect fun currentDevice(): DeviceInfo
