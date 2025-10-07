// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.`3_properties`.sealed

/**
 * Network result sealed class for testing sealed hierarchies.
 * Similar to enums but allows associated data.
 */
sealed class NetworkResult<out T> {
    data class Success<T>(
        val data: T,
    ) : NetworkResult<T>()

    data class Error(
        val message: String,
        val code: Int,
    ) : NetworkResult<Nothing>()

    data object Loading : NetworkResult<Nothing>()

    data object Idle : NetworkResult<Nothing>()
}
