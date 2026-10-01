// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInterop

/**
 * ERROR-level opt-in marker. Using anything annotated with it without an opt-in is a compile
 * error, so main code only builds because `kotlin.compilerOptions.optIn` lists this marker for
 * the whole module. Fakt's worker has to receive the same opt-in.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Interop API: opt in module-wide.")
@Retention(AnnotationRetention.BINARY)
annotation class InteropMarker

/** Marker-gated helper used by main code (not by any @Fake declaration). */
@InteropMarker fun interopStamp(): String = "interop"

/** Marker-gated type used inside a @Fake signature. */
@InteropMarker data class Receipt(val total: Int)
