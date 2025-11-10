// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsMethodLevel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MixedProcessorTest {
    @Test
    fun `GIVEN MixedProcessor with class and method generics WHEN generating fake THEN should preserve both type parameters`() {
        // Given - Interface with both class-level T and method-level R

        // When - Create fake with class-level type parameter
        val processor = fakeMixedProcessor<String>()

        // Then - Should work with class-level type
        val result: String = processor.process("test")
        assertEquals("test", result)
    }

    @Test
    fun `GIVEN mixed generics WHEN using method-level generic THEN should preserve type safety`() {
        // Given - MixedProcessor<String> with method-level <R>
        val processor = fakeMixedProcessor<String> {
            transform<Int> { item ->
                item.length // Transform String to Int
            }
        }

        // When - Use method with different type parameter
        val length: Int = processor.transform("hello")

        // Then - Type safety preserved
        assertEquals(5, length)
    }

    @Test
    fun `GIVEN non-generic method WHEN mixed with generics THEN should work normally`() {
        // Given - MixedProcessor with non-generic reset()
        var resetCalled = false
        val processor = fakeMixedProcessor<String> {
            reset { resetCalled = true }
        }

        // When - Call non-generic method
        processor.reset()

        // Then - Should execute
        assertEquals(true, resetCalled)
    }

    @Test
    fun `GIVEN MixedProcessor WHEN generated THEN fake should exist`() {
        // Given - MixedProcessor with class-level and method-level generics
        val processor = fakeMixedProcessor<String>()

        // Then - Should be successfully generated
        assertNotNull(processor, "Fake should be generated for MixedProcessor")
    }
}
