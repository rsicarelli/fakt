// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// The shared domain under test: plain Kotlin interfaces + models with ZERO dependencies on any
// mock or fake technology. Every non-Fakt competitor consumes exactly this one set of interfaces,
// so the mock side is provably testing identical contracts. The Fakt module keeps its own copy
// annotated with @Fake (a compiler plugin must see the annotation at the declaration site); a CI
// diff check keeps the two byte-identical apart from that annotation.
plugins {
    id("fakt-sample-jvm")
}
