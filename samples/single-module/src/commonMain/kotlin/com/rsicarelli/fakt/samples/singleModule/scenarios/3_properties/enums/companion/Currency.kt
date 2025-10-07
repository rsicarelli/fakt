// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.`3_properties`.enums.companion

/**
 * Currency enum with companion object.
 * Tests enum with companion object properties and factory methods.
 */
enum class Currency(
    val code: String,
    val symbol: String,
    val decimals: Int,
) {
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
        val DEFAULT = USD

        /**
         * All supported fiat currencies.
         */
        val FIAT_CURRENCIES = listOf(USD, EUR, GBP, JPY)

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
