// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins B18 of #152: a Native task forks a worker that holds only the distribution's embeddable
 * compiler, with `konan.home` set. Every other task keeps the Fakt worker classpath.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkerForkPlanTest {

    private val konanHome =
        File("distribution").resolve("kotlin-native-prebuilt-linux-x86_64-2.4.10")
    private val faktWorker = listOf(File("libs").resolve("kotlin-compiler-embeddable.jar"))

    @Test
    fun `GIVEN a konan home WHEN planning the fork THEN the classpath is only the embeddable compiler`() {
        val plan = workerForkPlan(konanHome, faktWorker)

        assertEquals(
            listOf(
                konanHome
                    .resolve("konan")
                    .resolve("lib")
                    .resolve("kotlin-native-compiler-embeddable.jar")
            ),
            plan.classpath,
        )
    }

    @Test
    fun `GIVEN a konan home WHEN planning the fork THEN nothing from the Fakt worker classpath is mixed in`() {
        val plan = workerForkPlan(konanHome, faktWorker)

        assertTrue(plan.classpath.none { it in faktWorker })
    }

    @Test
    fun `GIVEN a konan home WHEN planning the fork THEN konan home is its absolute path`() {
        val plan = workerForkPlan(konanHome, faktWorker)

        assertEquals(mapOf("konan.home" to konanHome.absolutePath), plan.systemProperties)
    }

    @Test
    fun `GIVEN no konan home WHEN planning the fork THEN the classpath is the Fakt worker classpath`() {
        val plan = workerForkPlan(konanHome = null, faktWorkerClasspath = faktWorker)

        assertEquals(faktWorker, plan.classpath)
    }

    @Test
    fun `GIVEN no konan home WHEN planning the fork THEN no system properties are set`() {
        val plan = workerForkPlan(konanHome = null, faktWorkerClasspath = faktWorker)

        assertTrue(plan.systemProperties.isEmpty())
    }
}
