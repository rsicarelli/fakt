// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch081

import com.rsicarelli.fakt.Fake

@Fake
interface ContainerOperations_operators_collections8026<K, V> {
    
    operator fun get(key: K): V?

    
    operator fun set(key: K, value: V)

    
    operator fun contains(key: K): Boolean

    
    operator fun iterator(): Iterator<Pair<K, V>>
}

data class ContainerOperations_operators_collections8026_3(val value: Int)
data class ContainerOperations_operators_collections8026_4(val start: Int, val endInclusive: Int)

@Fake
interface ContainerOperations_operators_collections8026_1 {
    
    operator fun ContainerOperations_operators_collections8026_3.rangeTo(other: ContainerOperations_operators_collections8026_3): ContainerOperations_operators_collections8026_4

    
    operator fun ContainerOperations_operators_collections8026_3.rangeUntil(other: ContainerOperations_operators_collections8026_3): ContainerOperations_operators_collections8026_4
}

@Fake
interface ContainerOperations_operators_collections8026_2<T : Comparable<T>> {
    
    operator fun compareTo(other: T): Int
}
