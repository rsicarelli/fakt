// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch012

import com.rsicarelli.fakt.Fake

@Fake
interface ContainerOperations_operators_collections1169<K, V> {
    
    operator fun get(key: K): V?

    
    operator fun set(key: K, value: V)

    
    operator fun contains(key: K): Boolean

    
    operator fun iterator(): Iterator<Pair<K, V>>
}

data class ContainerOperations_operators_collections1169_3(val value: Int)
data class ContainerOperations_operators_collections1169_4(val start: Int, val endInclusive: Int)

@Fake
interface ContainerOperations_operators_collections1169_1 {
    
    operator fun ContainerOperations_operators_collections1169_3.rangeTo(other: ContainerOperations_operators_collections1169_3): ContainerOperations_operators_collections1169_4

    
    operator fun ContainerOperations_operators_collections1169_3.rangeUntil(other: ContainerOperations_operators_collections1169_3): ContainerOperations_operators_collections1169_4
}

@Fake
interface ContainerOperations_operators_collections1169_2<T : Comparable<T>> {
    
    operator fun compareTo(other: T): Int
}
