// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.publisher.api

import com.rsicarelli.fakt.Fake

/**
 * Abstract repository demonstrating fake generation for abstract classes.
 * This tests abstract class handling with properties and suspend functions.
 */
@Fake
abstract class Repository {
    /**
     * The name of this repository.
     */
    abstract val name: String

    /**
     * Fetches all items from the repository.
     *
     * @return List of item identifiers
     */
    abstract suspend fun fetchAll(): List<String>

    /**
     * Saves an item to the repository.
     *
     * @param item The item to save
     * @return True if saved successfully
     */
    abstract suspend fun save(item: String): Boolean
}
