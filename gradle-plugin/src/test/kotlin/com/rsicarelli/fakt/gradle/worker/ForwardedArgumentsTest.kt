// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import com.rsicarelli.fakt.gradle.FaktGradleSubplugin
import kotlin.test.assertContains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ForwardedArgumentsTest {

    @Test
    fun `GIVEN rejected arguments WHEN building the failure message THEN it names arguments problems and worker version`() {
        val message =
            forwardedArgumentsRejectedMessage(
                problems = listOf("Unknown language version: 9.9"),
                arguments = listOf("-language-version=9.9"),
            )

        assertContains(message, "forwarded compiler arguments")
        assertContains(message, "Unknown language version: 9.9")
        assertContains(message, "-language-version=9.9")
        assertContains(message, FaktGradleSubplugin.FAKT_KOTLIN_VERSION)
        assertContains(message, "faktWorker")
    }
}
