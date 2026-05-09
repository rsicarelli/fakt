// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.core.edgecases

import com.rsicarelli.fakt.Fake

// ---- Edge case 1: multi-level nesting (object inside object) ----
public object L1 {
    public object L2 {
        @Fake
        public interface DeepService {
            public fun deep(): String
        }
    }
}

// ---- Edge case 2: companion object ----
public class CompanionContainer {
    public companion object {
        @Fake
        public interface CompanionService {
            public fun companionFn(): Int
        }
    }
}

// ---- Edge case 3: nested in interface (one level) ----
public interface OuterInterface {
    @Fake
    public interface InnerService {
        public fun inner(): String
    }
}

// ---- Edge case 4: generic interface nested in object ----
public object GenericHolder {
    @Fake
    public interface GenericRepo<T> {
        public fun get(id: String): T?

        public fun put(id: String, value: T)
    }
}

// ---- Edge case 5: abstract class nested in object ----
public object AbstractHolder {
    @Fake
    public abstract class NestedAbstractRepo {
        public abstract fun fetch(): String

        public open fun cached(): String = "default"
    }
}

// ---- Edge case 6: interface nested with same simple name as enclosing object ----
public object NameSame {
    @Fake
    public interface NameSame {
        public fun work(): String
    }
}
