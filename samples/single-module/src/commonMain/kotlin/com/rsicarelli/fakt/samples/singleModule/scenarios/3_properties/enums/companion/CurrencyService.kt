// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.`3_properties`.enums.companion

import com.rsicarelli.fakt.Fake

/**
 * Currency service using enum with companion object.
 *
 * Tests:
 * - Enum with companion object as property types
 * - Enum with companion object as method parameters
 * - Enum with companion object as method return types
 * - Accessing companion object methods from enum instances
 */
@Fake
interface CurrencyService {
    /**
     * Default currency for transactions.
     */
    val defaultCurrency: Currency

    /**
     * Preferred currency for the current user.
     */
    val preferredCurrency: Currency?

    /**
     * Get exchange rate between two currencies.
     */
    fun getExchangeRate(
        from: Currency,
        to: Currency,
    ): Double

    /**
     * Convert amount from one currency to another.
     */
    fun convert(
        amount: Double,
        from: Currency,
        to: Currency,
    ): Double

    /**
     * Get all supported currencies.
     */
    fun getSupportedCurrencies(): List<Currency>

    /**
     * Validate if currency is supported.
     */
    fun isSupported(currency: Currency): Boolean

    /**
     * Format amount with currency.
     */
    fun formatAmount(
        amount: Double,
        currency: Currency,
    ): String
}
