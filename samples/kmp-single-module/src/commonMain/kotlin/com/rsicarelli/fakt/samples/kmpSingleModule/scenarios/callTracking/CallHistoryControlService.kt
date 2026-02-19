// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.callTracking

import com.rsicarelli.fakt.CallHistoryMode
import com.rsicarelli.fakt.Fake

/**
 * Interface with call history explicitly DISABLED.
 *
 * When `callHistory = CallHistoryMode.DISABLED`, the generated fake will:
 * - NOT have `xxxCalls.value.size` properties
 * - NOT have `_xxxCalls` backing fields
 * - NOT have `verifyXxx` extension functions
 * - Have smaller generated code footprint (lightweight fakes)
 *
 * Use case: Performance-critical tests where call tracking is not needed.
 */
@Fake(callHistory = CallHistoryMode.DISABLED)
interface LightweightService {
    /**
     * Simple method - no call tracking will be generated.
     */
    fun process(value: String): String

    /**
     * Suspend method - no call tracking will be generated.
     */
    suspend fun asyncProcess(items: List<String>): List<String>

    /**
     * Property getter - no call tracking will be generated.
     */
    val status: String
}

/**
 * Interface with call history explicitly ENABLED.
 *
 * When `callHistory = CallHistoryMode.ENABLED`, the generated fake will:
 * - ALWAYS have `xxxCalls.value.size` properties
 * - ALWAYS have `_xxxCalls` backing fields
 * - ALWAYS have `verifyXxx` extension functions
 *
 * Use case: Override plugin-level defaults for specific interfaces that need tracking.
 */
@Fake(callHistory = CallHistoryMode.ENABLED)
interface FullyTrackedService {
    /**
     * Simple method - call tracking will be generated.
     */
    fun execute(command: String): Boolean

    /**
     * Suspend method - call tracking will be generated.
     */
    suspend fun asyncExecute(commands: List<String>): List<Boolean>

    /**
     * Property getter - call tracking will be generated.
     */
    val isReady: Boolean
}

/**
 * Interface with DEFAULT call history mode.
 *
 * When `callHistory = CallHistoryMode.DEFAULT` (or not specified),
 * the generated fake follows the plugin-level default from:
 * `fakt { enableCallHistory.set(true/false) }`
 *
 * By default, call history is ENABLED unless configured otherwise.
 */
@Fake(callHistory = CallHistoryMode.DEFAULT)
interface DefaultTrackedService {
    /**
     * Method follows plugin default.
     */
    fun doWork(input: Int): String

    /**
     * Property follows plugin default.
     */
    val workCount: Int
}
