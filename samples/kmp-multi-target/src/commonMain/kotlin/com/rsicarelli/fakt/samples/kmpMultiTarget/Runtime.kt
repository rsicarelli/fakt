// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

/** A storage slot address; a plain commonMain type that webMain fakes reference. */
data class StorageKey(val namespace: String, val name: String)

/** The runtime family of the current target ("jvm", "web", "native"). */
expect fun runtimeFamily(): String
