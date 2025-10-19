// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.propertiesAndMethods

import com.rsicarelli.fakt.Fake

/**
 * P2.4: Nested collections with method-level generics.
 *
 * Tests deeply nested collection types (Map<String, List<Set<Int>>>) combined with
 * method-level type parameters. Common in data processing pipelines where you need to
 * transform complex data structures while preserving type safety.
 */
@Fake
interface CollectionService {
    fun processStrings(items: List<String>): Set<String>

    fun processNumbers(items: Set<Int>): Map<Int, String>

    fun <K, V> transformMap(
        map: Map<K, V>,
        transformer: (K, V) -> String,
    ): Map<K, String>

    fun nestedCollections(data: Map<String, List<Set<Int>>>): List<Map<String, Int>>
}
