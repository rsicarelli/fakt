// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.FaktGradleSubplugin.CacheCorrectDecision
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins which routing decisions leave fake generation inside `compileKotlin*`, and therefore which
 * compile tasks must opt out of the Gradle build cache.
 *
 * Regression cover for issue #142: with a warm cache, `clean <consumerTask>` restored the producing
 * `compileKotlin*` FROM-CACHE, skipping the in-process generation that rides it. No `.kt` files
 * were written, the consuming compilation saw an empty source dir, and the build failed with
 * unresolved references to the fakes. Reproduced on three shapes — the
 * `useExperimentalGenerateTask=false` opt-out, an AGP 9 built-in-Kotlin module, and a single-target
 * multiplatform project — the last two on their default configuration, with no opt-out anywhere.
 *
 * The mapping below is the whole fix: get it wrong in the `true` direction and a compile task loses
 * cache hits for nothing; wrong in the `false` direction and #142 comes back.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktInProcessCachingTest {

    @Test
    fun `GIVEN the legacy decision WHEN asking whether fakes are generated in-process THEN it is true`() {
        assertTrue(
            generatesFakesInProcess(CacheCorrectDecision.LEGACY),
            "LEGACY runs the compiler plugin inside compileKotlin*, so the fakes are an undeclared " +
                "side effect and that task's cache entry is incomplete (issue #142).",
        )
    }

    @Test
    fun `GIVEN the legacy hybrid decision WHEN asking whether fakes are generated in-process THEN it is true`() {
        assertTrue(
            generatesFakesInProcess(CacheCorrectDecision.LEGACY_HYBRID),
            "LEGACY_HYBRID keeps the in-process plugin on for a non-drivable platform main so it " +
                "writes that platform's own fakes — undeclared, exactly like LEGACY.",
        )
    }

    @Test
    fun `GIVEN the producer decision WHEN asking whether fakes are generated in-process THEN it is false`() {
        assertFalse(
            generatesFakesInProcess(CacheCorrectDecision.REGISTER_PRODUCER),
            "A producer FaktGenerateTask declares generatedKotlinDir as an @OutputDirectory, so the " +
                "cache restores the fakes with the task and the compile task must stay cacheable.",
        )
    }

    @Test
    fun `GIVEN the consumer decision WHEN asking whether fakes are generated in-process THEN it is false`() {
        assertFalse(
            generatesFakesInProcess(CacheCorrectDecision.REGISTER_CONSUMER),
            "A consumer FaktGenerateTask declares its outputs the same way a producer does.",
        )
    }

    @Test
    fun `GIVEN the suppress decision WHEN asking whether fakes are generated in-process THEN it is false`() {
        assertFalse(
            generatesFakesInProcess(CacheCorrectDecision.SUPPRESS),
            "SUPPRESS generates nothing at all — another task owns this compilation's fakes — so " +
                "the compilation caches like any Fakt-free one.",
        )
    }

    @Test
    fun `GIVEN every routing decision WHEN mapping to in-process generation THEN exactly the two legacy decisions qualify`() {
        val inProcess = CacheCorrectDecision.entries.filter(::generatesFakesInProcess).toSet()
        assertTrue(
            inProcess == setOf(CacheCorrectDecision.LEGACY, CacheCorrectDecision.LEGACY_HYBRID),
            "A new routing decision must declare whether it generates in-process; defaulting it to " +
                "cacheable would silently reintroduce issue #142. Got: $inProcess",
        )
    }
}
