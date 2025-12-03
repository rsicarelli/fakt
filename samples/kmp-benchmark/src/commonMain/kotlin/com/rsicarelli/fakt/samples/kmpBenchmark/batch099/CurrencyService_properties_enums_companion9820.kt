// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch099

import com.rsicarelli.fakt.Fake

@Fake
interface CurrencyService_properties_enums_companion9820 {
    
    val defaultCurrency: CurrencyService_properties_enums_companion9820_1

    
    val preferredCurrency: CurrencyService_properties_enums_companion9820_1?

    
    fun getExchangeRate(
        from: CurrencyService_properties_enums_companion9820_1,
        to: CurrencyService_properties_enums_companion9820_1,
    ): Double

    
    fun convert(
        amount: Double,
        from: CurrencyService_properties_enums_companion9820_1,
        to: CurrencyService_properties_enums_companion9820_1,
    ): Double

    
    fun getSupportedCurrencies(): List<CurrencyService_properties_enums_companion9820_1>

    
    fun isSupported(currency: CurrencyService_properties_enums_companion9820_1): Boolean

    
    fun formatAmount(
        amount: Double,
        currency: CurrencyService_properties_enums_companion9820_1,
    ): String
}

enum class CurrencyService_properties_enums_companion9820_1(
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
        
        val DEFAULT = BRL

        
        val FIAT_CURRENCIES = listOf(BRL, USD, EUR, GBP, JPY)

        
        val CRYPTO_CURRENCIES = listOf(BTC)

        
        fun fromCode(code: String): CurrencyService_properties_enums_companion9820_1? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }

        
        fun fromSymbol(symbol: String): CurrencyService_properties_enums_companion9820_1? = entries.firstOrNull { it.symbol == symbol }

        
        fun isSupported(code: String): Boolean = fromCode(code) != null
    }

    
    fun format(amount: Double): String {
        
        val multiplier = when (decimals) {
            0 -> 1.0
            1 -> 10.0
            2 -> 100.0
            8 -> 100000000.0
            else -> 1.0
        }

        val rounded = kotlin.math.round(amount * multiplier) / multiplier

        
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