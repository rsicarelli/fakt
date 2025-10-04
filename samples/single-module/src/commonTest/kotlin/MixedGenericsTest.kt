// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Phase 3 RED TEST: Mixed generics (class-level + method-level) fake generation.
 *
 * ❌ This test will FAIL until we implement mixed generics support.
 *
 * Currently, MixedProcessor<T> is filtered out by GenericPatternAnalyzer
 * as MixedGenerics pattern.
 *
 * After implementation, fakeMixedProcessor should be generated with:
 * ```kotlin
 * class FakeMixedProcessor<T> : MixedProcessor<T> {
 *     override fun process(item: T): T = processBehavior(item)
 *     override fun <R> transform(item: T): R = transformBehavior(item)
 *     override fun reset() = resetBehavior()
 * }
 *
 * inline fun <reified T> fakeMixedProcessor(
 *     configure: FakeMixedProcessorConfig<T>.() -> Unit = {}
 * ): MixedProcessor<T>
 * ```
 */
class MixedGenericsTest {
    @Test
    fun `GIVEN MixedProcessor with class and method generics WHEN generating fake THEN should preserve both type parameters`() {
        // Given - Interface with both class-level T and method-level R
        // ❌ RED: fakeMixedProcessor doesn't exist yet (filtered out)

        // When - Create fake with class-level type parameter
        val processor = fakeMixedProcessor<String>()

        // Then - Should work with class-level type
        val result: String = processor.process("test")
        assertEquals("test", result)
    }

    @Test
    fun `GIVEN mixed generics WHEN using method-level generic THEN should preserve type safety`() {
        // Given - MixedProcessor<String> with method-level <R>
        val processor =
            fakeMixedProcessor<String> {
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
        val processor =
            fakeMixedProcessor<String> {
                reset { resetCalled = true }
            }

        // When - Call non-generic method
        processor.reset()

        // Then - Should execute
        assertEquals(true, resetCalled)
    }

    @Test
    fun `GIVEN MixedProcessor WHEN generated THEN fake should exist`() {
        // This is the most basic test - just check the fake was generated
        // ❌ RED: This will fail because fakeMixedProcessor doesn't exist
        val processor = fakeMixedProcessor<String>()

        assertNotNull(processor, "Fake should be generated for MixedProcessor")
    }
}
