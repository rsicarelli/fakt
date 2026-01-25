// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.LogLevel
import org.gradle.api.logging.Logger

public class GradleFaktLogger(
    private val logger: Logger,
    public val logLevel: LogLevel,
) {
    public fun info(message: String) {
        if (logLevel >= LogLevel.INFO) {
            logger.lifecycle("Fakt: $message")
        }
    }

    public fun debug(message: String) {
        if (logLevel >= LogLevel.DEBUG) {
            logger.info("Fakt: $message")
        }
    }

    public fun warn(message: String): Unit = logger.warn("Fakt: $message")

    public fun error(message: String): Unit = logger.error("Fakt: $message")
}
