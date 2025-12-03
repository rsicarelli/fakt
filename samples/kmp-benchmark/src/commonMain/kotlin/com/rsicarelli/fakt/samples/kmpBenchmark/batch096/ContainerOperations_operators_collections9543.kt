// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch096

import com.rsicarelli.fakt.Fake

@Fake
interface ContainerOperations_operators_collections9543<K, V> {
    
    operator fun get(key: K): V?

    
    operator fun set(key: K, value: V)

    
    operator fun contains(key: K): Boolean

    
    operator fun iterator(): Iterator<Pair<K, V>>
}

data class ContainerOperations_operators_collections9543_3(val value: Int)
data class ContainerOperations_operators_collections9543_4(val start: Int, val endInclusive: Int)

@Fake
interface ContainerOperations_operators_collections9543_1 {
    
    operator fun ContainerOperations_operators_collections9543_3.rangeTo(other: ContainerOperations_operators_collections9543_3): ContainerOperations_operators_collections9543_4

    
    operator fun ContainerOperations_operators_collections9543_3.rangeUntil(other: ContainerOperations_operators_collections9543_3): ContainerOperations_operators_collections9543_4
}

@Fake
interface ContainerOperations_operators_collections9543_2<T : Comparable<T>> {
    
    operator fun compareTo(other: T): Int
}
