// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.`3_properties`.enums.companion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for CurrencyService fake with enum companion object.
 *
 * Validates that enums with companion objects are properly handled
 * by Fakt code generation, including accessing companion properties
 * and methods from enum instances.
 */
class CurrencyServiceTest {

    @Test
    fun `GIVEN CurrencyService fake WHEN configuring defaultCurrency THEN should return configured currency`() {
        // Given
        val currencyService = fakeCurrencyService {
            defaultCurrency { Currency.EUR }
        }

        // When
        val currency = currencyService.defaultCurrency

        // Then
        assertEquals(Currency.EUR, currency)
        assertEquals("EUR", currency.code)
        assertEquals("€", currency.symbol)
    }

    @Test
    fun `GIVEN CurrencyService fake WHEN configuring preferredCurrency as null THEN should return null`() {
        // Given
        val currencyService = fakeCurrencyService {
            preferredCurrency { null }
        }

        // When
        val currency = currencyService.preferredCurrency

        // Then
        assertNull(currency)
    }

    @Test
    fun `GIVEN CurrencyService fake WHEN configuring preferredCurrency THEN should return configured currency`() {
        // Given
        val currencyService = fakeCurrencyService {
            preferredCurrency { Currency.BTC }
        }

        // When
        val currency = currencyService.preferredCurrency

        // Then
        assertEquals(Currency.BTC, currency)
        assertEquals(8, currency?.decimals)
    }

    @Test
    fun `GIVEN CurrencyService fake WHEN configuring getExchangeRate THEN should return exchange rate`() {
        // Given
        val currencyService = fakeCurrencyService {
            getExchangeRate { from, to ->
                when {
                    from == to -> 1.0
                    from == Currency.USD && to == Currency.EUR -> 0.85
                    from == Currency.EUR && to == Currency.USD -> 1.18
                    from == Currency.USD && to == Currency.GBP -> 0.73
                    else -> 1.0
                }
            }
        }

        // When & Then
        assertEquals(1.0, currencyService.getExchangeRate(Currency.USD, Currency.USD))
        assertEquals(0.85, currencyService.getExchangeRate(Currency.USD, Currency.EUR))
        assertEquals(1.18, currencyService.getExchangeRate(Currency.EUR, Currency.USD))
        assertEquals(0.73, currencyService.getExchangeRate(Currency.USD, Currency.GBP))
    }

    @Test
    fun `GIVEN CurrencyService fake WHEN configuring convert THEN should convert amount between currencies`() {
        // Given
        val rates = mapOf(
            Pair(Currency.USD, Currency.EUR) to 0.85,
            Pair(Currency.EUR, Currency.USD) to 1.18
        )

        val currencyService = fakeCurrencyService {
            convert { amount, from, to ->
                if (from == to) {
                    amount
                } else {
                    val rate = rates[Pair(from, to)] ?: 1.0
                    amount * rate
                }
            }
        }

        // When
        val converted = currencyService.convert(100.0, Currency.USD, Currency.EUR)

        // Then
        assertEquals(85.0, converted)
    }

    @Test
    fun `GIVEN CurrencyService fake WHEN configuring getSupportedCurrencies THEN should return all currencies`() {
        // Given
        val currencyService = fakeCurrencyService {
            getSupportedCurrencies { Currency.entries }
        }

        // When
        val currencies = currencyService.getSupportedCurrencies()

        // Then
        assertEquals(5, currencies.size)
        assertTrue(currencies.contains(Currency.USD))
        assertTrue(currencies.contains(Currency.EUR))
        assertTrue(currencies.contains(Currency.BTC))
    }

    @Test
    fun `GIVEN CurrencyService fake WHEN configuring isSupported THEN should validate currency support`() {
        // Given
        val currencyService = fakeCurrencyService {
            isSupported { currency ->
                currency in Currency.FIAT_CURRENCIES
            }
        }

        // When & Then
        assertTrue(currencyService.isSupported(Currency.USD))
        assertTrue(currencyService.isSupported(Currency.EUR))
        assertFalse(currencyService.isSupported(Currency.BTC))
    }

    @Test
    fun `GIVEN CurrencyService fake WHEN configuring formatAmount THEN should format with currency symbol`() {
        // Given
        val currencyService = fakeCurrencyService {
            formatAmount { amount, currency ->
                currency.format(amount)
            }
        }

        // When & Then
        assertEquals("$100.00", currencyService.formatAmount(100.0, Currency.USD))
        assertEquals("€85.00", currencyService.formatAmount(85.0, Currency.EUR))
        assertEquals("¥1000", currencyService.formatAmount(1000.0, Currency.JPY))
        assertEquals("₿0.00123456", currencyService.formatAmount(0.00123456, Currency.BTC))
    }

    @Test
    fun `GIVEN Currency companion object WHEN accessing static properties THEN should access companion values`() {
        // Then
        assertEquals(Currency.USD, Currency.DEFAULT)
        assertEquals(4, Currency.FIAT_CURRENCIES.size)
        assertEquals(1, Currency.CRYPTO_CURRENCIES.size)
    }

    @Test
    fun `GIVEN Currency companion object WHEN using factory methods THEN should find currency by code`() {
        // When & Then
        assertEquals(Currency.USD, Currency.fromCode("USD"))
        assertEquals(Currency.EUR, Currency.fromCode("eur"))
        assertNull(Currency.fromCode("INVALID"))
    }

    @Test
    fun `GIVEN Currency companion object WHEN using factory methods THEN should find currency by symbol`() {
        // When & Then
        assertEquals(Currency.USD, Currency.fromSymbol("$"))
        assertEquals(Currency.EUR, Currency.fromSymbol("€"))
        assertNull(Currency.fromSymbol("@"))
    }

    @Test
    fun `GIVEN Currency companion object WHEN checking support THEN should validate currency code`() {
        // When & Then
        assertTrue(Currency.isSupported("USD"))
        assertTrue(Currency.isSupported("btc"))
        assertFalse(Currency.isSupported("INVALID"))
    }

    @Test
    fun `GIVEN Currency enum WHEN using instance methods THEN should format amount correctly`() {
        // When & Then
        assertEquals("$100.50", Currency.USD.format(100.50))
        assertEquals("€75.25", Currency.EUR.format(75.25))
        assertEquals("¥1000", Currency.JPY.format(1000.0))
    }
}
