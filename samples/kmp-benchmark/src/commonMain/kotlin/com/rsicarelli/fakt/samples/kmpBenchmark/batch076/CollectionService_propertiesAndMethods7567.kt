// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch076

import com.rsicarelli.fakt.Fake

@Fake
interface CollectionService_propertiesAndMethods7567 {
    fun processStrings(items: List<String>): Set<String>

    fun processNumbers(items: Set<Int>): Map<Int, String>

    fun <K, V> transformMap(
        map: Map<K, V>,
        transformer: (K, V) -> String,
    ): Map<K, String>

    fun nestedCollections(data: Map<String, List<Set<Int>>>): List<Map<String, Int>>
}
