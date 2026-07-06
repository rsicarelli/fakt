// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpNoJvm

import com.rsicarelli.fakt.Fake

/**
 * Common service in a project with NO JVM/Android target (js + linuxX64 only).
 *
 * The cache-correct producer (`faktGenerateMetadataCommonMain`) must generate
 * `FakeDeviceRegistryImpl` for commonTest even though no JVM classpath exists — the metadata
 * driver reads the compilation's own klib dependencies instead.
 */
@Fake
interface DeviceRegistry {
    /** Register a device by name; returns whether it was newly added. */
    fun register(name: String): Boolean

    /** Look up a registered device by numeric id, or null when absent. */
    fun find(id: Int): String?

    /** Devices currently registered. */
    val deviceCount: Int
}
