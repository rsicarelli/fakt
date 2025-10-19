// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.properties.enums.companion

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
     * KMP-safe formatting without String.format()
     */
    fun format(amount: Double): String {
        // Round to correct number of decimals
        val multiplier = when (decimals) {
            0 -> 1.0
            1 -> 10.0
            2 -> 100.0
            8 -> 100000000.0
            else -> 1.0
        }

        val rounded = kotlin.math.round(amount * multiplier) / multiplier

        // Format with correct decimals
        return when (decimals) {
            0 -> "$symbol${rounded.toInt()}"
            else -> {
                val intPart = rounded.toInt()
                val fracPart = ((rounded - intPart) * multiplier).toInt()
                val fracStr = fracPart.toString().padStart(decimals, '0')
                "$symbol$intPart.$fracStr"
            }
        }
    }
}