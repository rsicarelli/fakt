// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInterop

enum class Tier {
    FREE,
    PRO,
}

/**
 * Uses unqualified enum entries in a `when`: this compiles only with
 * `-Xcontext-sensitive-resolution`, which the build adds to `compileKotlin`.
 */
fun discountPercent(tier: Tier): Int =
    when (tier) {
        FREE -> 0
        PRO -> 20
    }

fun isPaid(tier: Tier): Boolean = tier == PRO
