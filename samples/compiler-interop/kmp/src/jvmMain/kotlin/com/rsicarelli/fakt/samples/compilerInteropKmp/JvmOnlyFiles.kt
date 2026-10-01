// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInteropKmp

import com.rsicarelli.fakt.Fake

/** JVM-only fake: owned by the jvm producer, which needs the same options and toolchain JDK. */
@Fake
interface JvmOnlyFiles {
    fun exists(path: String): Boolean
}

/** Uses the opt-in marker and an unqualified enum entry, like commonMain does. */
fun jvmChannelLabel(channel: Channel, files: JvmOnlyFiles): String =
    when (channel) {
        EMAIL -> "${interopStamp()}:email:${files.exists("outbox")}"
        SMS -> "${interopStamp()}:sms:${files.exists("outbox")}"
    }
