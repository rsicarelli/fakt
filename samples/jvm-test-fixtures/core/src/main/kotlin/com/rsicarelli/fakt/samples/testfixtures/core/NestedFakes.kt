// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.core

import com.rsicarelli.fakt.Fake

public object NestedFakes {
    @Fake
    public interface AlphaService {
        public fun alpha(): String
    }

    @Fake
    public interface BetaService {
        public fun beta(): Int
    }

    @Fake
    public interface GammaService {
        public fun gamma(): Boolean
    }

    @Fake
    public interface DeltaService {
        public fun delta(): Long
    }
}

public class NestedFakesContainer {
    @Fake
    public interface NestedInClassService {
        public fun ping(): String
    }
}
