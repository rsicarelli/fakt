// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.models

/**
 * Custom annotation for testing custom annotation support.
 * Alternative to @Fake for organizational-specific naming preferences.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CompanyTestDouble
