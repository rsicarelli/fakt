// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInterop.processor

/** Marks a class that gets a `Generated<Name>` companion type written by the KSP processor. */
@Target(AnnotationTarget.CLASS) @Retention(AnnotationRetention.SOURCE) annotation class Generate
