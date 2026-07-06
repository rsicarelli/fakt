// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pure unit coverage for the worker's `-P` plugin-option payload ([FaktPluginOptions.payload]).
 *
 * Guards the issue #112 fix: the cache-correct worker path must forward the
 * `enableCallHistory` / `enableMutableFakes` extension defaults alongside the original four options,
 * so it can't silently fall back to the compiler's own defaults the way it did before. This runs
 * without the forked worker or `kotlin-compiler-embeddable`, so a regression in the forwarded keys
 * or values fails fast instead of only surfacing in the slower TestKit suite.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktPluginOptionsTest {

    @Test
    fun `GIVEN option keys WHEN read THEN each carries the compiler plugin prefix`() {
        val keys =
            listOf(
                FaktPluginOptions.ENABLED,
                FaktPluginOptions.LOG_LEVEL,
                FaktPluginOptions.OUTPUT_DIR,
                FaktPluginOptions.SOURCE_SET_CONTEXT,
                FaktPluginOptions.ENABLE_CALL_HISTORY,
                FaktPluginOptions.ENABLE_MUTABLE_FAKES,
            )

        keys.forEach { key ->
            assertTrue(
                key.startsWith("plugin:com.rsicarelli.fakt:"),
                "Option key must carry the Fakt compiler-plugin prefix: $key",
            )
        }
        assertEquals(
            "plugin:com.rsicarelli.fakt:enableCallHistory",
            FaktPluginOptions.ENABLE_CALL_HISTORY,
        )
        assertEquals(
            "plugin:com.rsicarelli.fakt:enableMutableFakes",
            FaktPluginOptions.ENABLE_MUTABLE_FAKES,
        )
    }

    @Test
    fun `GIVEN codegen inputs WHEN building the payload THEN it forwards all six options in order`() {
        val payload =
            FaktPluginOptions.payload(
                logLevel = "DEBUG",
                outputDir = "/out/generated",
                sourceSetContextBase64 = "BASE64==",
                enableCallHistory = true,
                enableMutableFakes = false,
            )

        assertEquals(
            listOf(
                "plugin:com.rsicarelli.fakt:enabled=true",
                "plugin:com.rsicarelli.fakt:logLevel=DEBUG",
                "plugin:com.rsicarelli.fakt:outputDir=/out/generated",
                "plugin:com.rsicarelli.fakt:sourceSetContext=BASE64==",
                "plugin:com.rsicarelli.fakt:enableCallHistory=true",
                "plugin:com.rsicarelli.fakt:enableMutableFakes=false",
            ),
            payload,
        )
    }

    @Test
    fun `GIVEN call history disabled WHEN building the payload THEN the flag is forwarded as false`() {
        val payload =
            FaktPluginOptions.payload(
                logLevel = "QUIET",
                outputDir = "/out",
                sourceSetContextBase64 = "ctx",
                enableCallHistory = false,
                enableMutableFakes = false,
            )

        assertTrue(
            "plugin:com.rsicarelli.fakt:enableCallHistory=false" in payload,
            "enableCallHistory=false must reach the compiler, not fall back to the true default; got: $payload",
        )
    }

    @Test
    fun `GIVEN mutable fakes enabled WHEN building the payload THEN the flag is forwarded as true`() {
        val payload =
            FaktPluginOptions.payload(
                logLevel = "INFO",
                outputDir = "/out",
                sourceSetContextBase64 = "ctx",
                enableCallHistory = true,
                enableMutableFakes = true,
            )

        assertTrue(
            "plugin:com.rsicarelli.fakt:enableMutableFakes=true" in payload,
            "enableMutableFakes=true must reach the compiler, not fall back to the false default; got: $payload",
        )
    }
}
