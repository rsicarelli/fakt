// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.inheritance

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: ClassImplementingMultipleInterfaces
 *
 * **Pattern**: Class extends abstract class AND implements interface
 * **Priority**: P1 (High - Very common pattern in service layers)
 *
 * **What it tests**:
 * - Class extending abstract class
 * - Class implementing interface simultaneously
 * - Abstract methods from abstract class → error defaults
 * - Abstract methods from interface → error defaults
 * - Open methods from abstract class → super defaults
 * - Own open methods → super defaults
 *
 * **Expected behavior**:
 * ```kotlin
 * class FakeLoggingServiceImpl : LoggingService() {
 *     // From BaseService (abstract class) - abstract
 *     private var startBehavior: () -> Boolean = { _ -> error("Configure start behavior") }
 *
 *     // From Loggable (interface) - abstract (all interface methods are abstract)
 *     private var logBehavior: (String) -> Unit = { _ -> error("Configure log behavior") }
 *
 *     // From BaseService (abstract class) - open
 *     private var stopBehavior: () -> Unit = { super.stop() }
 *
 *     // Own method - open
 *     private var getLogLevelBehavior: () -> String = { super.getLogLevel() }
 * }
 * ```
 *
 * **Real-world equivalent**:
 * ```kotlin
 * // Common pattern: Service with lifecycle + capabilities
 * abstract class BaseService { abstract fun start(); open fun stop() {} }
 * interface Loggable { fun log(message: String) }
 * class LoggingService : BaseService(), Loggable { ... }
 *
 * // Testing:
 * val service: LoggingService = fakeLoggingService {
 *     start { true }  // Must configure (abstract from BaseService)
 *     log { msg -> println(msg) }  // Must configure (abstract from Loggable)
 *     stop { }  // Optional (open from BaseService)
 *     getLogLevel { "DEBUG" }  // Optional (own open method)
 * }
 * ```
 */

/**
 * Interface defining logging capability.
 */
interface Loggable {
    /**
     * Logs a message - abstract (all interface methods are implicitly abstract).
     */
    fun log(message: String)
}

/**
 * Abstract base class for services with lifecycle.
 */
abstract class BaseService {
    /**
     * Starts the service - abstract, must be implemented.
     */
    abstract fun start(): Boolean

    /**
     * Stops the service - has default implementation.
     */
    open fun stop() {
        // Default stop logic
    }
}

/**
 * Service that extends BaseService AND implements Loggable.
 * Demonstrates class implementing both abstract class and interface.
 */
@Fake
open class LoggingService :
    BaseService(),
    Loggable {
    /**
     * Implements abstract start() from BaseService.
     */
    override fun start(): Boolean = true

    /**
     * Implements abstract log() from Loggable interface.
     */
    override fun log(message: String) {
        // Default logging implementation
    }

    /**
     * Own open method - gets the current log level.
     */
    open fun getLogLevel(): String = "INFO"
}
