// Manual investigation of default parameters in Kotlin overrides

import kotlin.test.Test
import kotlin.test.assertEquals

interface PaymentService {
    fun processPayment(amount: Double, currency: String = "USD"): String
    fun complexDefaults(items: List<String> = emptyList(), config: Map<String, Any> = emptyMap()): Int
}

// CORRECT Kotlin approach: NO defaults in override
// Defaults are automatically inherited from the interface!
class ManualFake : PaymentService {
    var lastCurrency: String? = null
    var lastItems: List<String>? = null
    var lastConfig: Map<String, Any>? = null

    override fun processPayment(amount: Double, currency: String): String {
        lastCurrency = currency
        return "Fake payment: $amount $currency"
    }

    override fun complexDefaults(items: List<String>, config: Map<String, Any>): Int {
        lastItems = items
        lastConfig = config
        return items.size + config.size
    }
}

class ManualFakeTest {
    @Test
    fun `GIVEN interface with default parameters WHEN calling without arguments THEN defaults from interface are used`() {
        // GIVEN
        val fake = ManualFake()

        // WHEN - Call without providing optional parameters
        fake.processPayment(100.0) // Should use "USD" from interface
        fake.complexDefaults() // Should use emptyList() and emptyMap() from interface

        // THEN - Defaults from interface were used
        assertEquals("USD", fake.lastCurrency, "Default currency should be 'USD' from interface")
        assertEquals(emptyList(), fake.lastItems, "Default items should be emptyList() from interface")
        assertEquals(emptyMap(), fake.lastConfig, "Default config should be emptyMap() from interface")
    }

    @Test
    fun `GIVEN interface with default parameters WHEN calling with custom arguments THEN custom values are used`() {
        // GIVEN
        val fake = ManualFake()

        // WHEN - Call with custom parameters
        fake.processPayment(100.0, "EUR")
        fake.complexDefaults(listOf("item1"), mapOf("key" to "value"))

        // THEN - Custom values were used
        assertEquals("EUR", fake.lastCurrency)
        assertEquals(listOf("item1"), fake.lastItems)
        assertEquals(mapOf("key" to "value"), fake.lastConfig)
    }
}
