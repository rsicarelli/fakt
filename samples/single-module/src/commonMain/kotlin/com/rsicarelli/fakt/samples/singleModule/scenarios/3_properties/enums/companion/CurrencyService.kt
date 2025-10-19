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

/**
 * Currency enum with companion object.
 * Tests enum with companion object properties and factory methods.
 */
enum class Currency(
    val code: String,
    val symbol: String,
    val decimals: Int,
) {
    BRL("BRL", "R$", 2),
    USD("USD", "$", 2),
    EUR("EUR", "€", 2),
    GBP("GBP", "£", 2),
    JPY("JPY", "¥", 0),
    BTC("BTC", "₿", 8),
    ;

    companion object {
        /**
         * Default currency for the application.
         */
        val DEFAULT = BRL

        /**
         * All supported fiat currencies.
         */
        val FIAT_CURRENCIES = listOf(BRL, USD, EUR, GBP, JPY)

        /**
         * All supported cryptocurrencies.
         */
        val CRYPTO_CURRENCIES = listOf(BTC)

        /**
         * Find currency by code.
         */
        fun fromCode(code: String): Currency? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }

        /**
         * Find currency by symbol.
         */
        fun fromSymbol(symbol: String): Currency? = entries.firstOrNull { it.symbol == symbol }

        /**
         * Check if currency code is supported.
         */
        fun isSupported(code: String): Boolean = fromCode(code) != null
    }

    /**
     * Format amount with currency symbol.
     */
    fun format(amount: Double): String = "$symbol%.${decimals}f".format(amount)
}