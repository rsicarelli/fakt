// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

/**
 * Compile-only: instrumented tests are not executed here, but their compilation must see the
 * androidMain fakes of the variant they belong to.
 */
internal fun instrumentedFake(): AndroidOnly = fakeAndroidOnly { label { "instrumented" } }
