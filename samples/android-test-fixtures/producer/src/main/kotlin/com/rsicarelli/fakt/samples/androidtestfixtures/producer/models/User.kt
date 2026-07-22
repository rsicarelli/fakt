// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.androidtestfixtures.producer.models

/** Simple domain model shared across the producer and its consumers. */
data class User(
    val id: String,
    val name: String,
    val email: String,
)
