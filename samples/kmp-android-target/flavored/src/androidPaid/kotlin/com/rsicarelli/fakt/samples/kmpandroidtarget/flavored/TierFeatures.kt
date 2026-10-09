// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.flavored

import com.rsicarelli.fakt.Fake

/** Same FQN as the androidFree declaration, different members: each flavor analyses its own. */
@Fake
interface TierFeatures {
    fun maxItems(): Int

    fun exportEnabled(): Boolean
}
