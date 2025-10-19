// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.models

/**
 * User model used across various test scenarios.
 */
data class User(
    val id: String,
    val name: String,
    val email: String = "",
    val age: Int = 0,
)
