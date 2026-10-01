// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInterop

/**
 * Only compiled on the JDK 17 toolchain. `java.lang.Compiler` was removed in JDK 21, so a Fakt
 * worker that analysed this module against Gradle's JDK 21 (instead of the toolchain) would fail
 * here with an unresolved reference.
 */
fun legacyCompilerName(): String = java.lang.Compiler::class.java.simpleName
