// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

private fun target(
    name: String,
    type: String,
    isAndroid: Boolean = false,
    main: List<String> = listOf("${name}Main"),
): TargetNode = TargetNode(name, type, isAndroid, main)

/** Builds a graph where every platform main set has `commonMain` as a direct parent. */
private fun graph(
    vararg targets: TargetNode,
    extraParents: Map<String, Set<String>> = emptyMap(),
): SourceSetGraph {
    val base = targets.flatMap { it.mainSourceSets }.associateWith { setOf("commonMain") }
    return SourceSetGraph(targets.toList(), base + extraParents)
}

private fun iosGraph(): SourceSetGraph =
    graph(
        target("jvm", "jvm"),
        target("iosArm64", "native"),
        target("iosX64", "native"),
        extraParents =
            mapOf(
                "iosArm64Main" to setOf("iosMain"),
                "iosX64Main" to setOf("iosMain"),
                "iosMain" to setOf("appleMain"),
                "appleMain" to setOf("nativeMain"),
                "nativeMain" to setOf("commonMain"),
            ),
    )

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SourceSetOwnershipTest {
    @Test
    fun `GIVEN a single jvm target WHEN assigning owners THEN commonMain and jvmMain belong to the platform`() {
        val owners = assignSourceSetOwners(graph(target("jvm", "jvm")))

        assertEquals(
            mapOf(
                "commonMain" to SourceSetOwner.Platform("jvm"),
                "jvmMain" to SourceSetOwner.Platform("jvm"),
            ),
            owners,
        )
    }

    @Test
    fun `GIVEN jvm and js WHEN assigning owners THEN commonMain is a metadata owner`() {
        val owners = assignSourceSetOwners(graph(target("jvm", "jvm"), target("js", "js")))

        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
        assertEquals(SourceSetOwner.Platform("jvm"), owners["jvmMain"])
        assertEquals(SourceSetOwner.Platform("js"), owners["jsMain"])
    }

    @Test
    fun `GIVEN two jvm targets WHEN assigning owners THEN commonMain is synthetic on desktop`() {
        val owners = assignSourceSetOwners(graph(target("server", "jvm"), target("desktop", "jvm")))

        assertEquals(
            SourceSetOwner.Synthetic("commonMain", "desktop", "desktopMain"),
            owners["commonMain"],
        )
        assertEquals(SourceSetOwner.Platform("desktop"), owners["desktopMain"])
        assertEquals(SourceSetOwner.Platform("server"), owners["serverMain"])
    }

    @Test
    fun `GIVEN kmp android with the jvm flag and jvm WHEN assigning owners THEN the non android target is chosen`() {
        val owners =
            assignSourceSetOwners(
                graph(target("android", "jvm", isAndroid = true), target("jvm", "jvm"))
            )

        assertEquals(SourceSetOwner.Synthetic("commonMain", "jvm", "jvmMain"), owners["commonMain"])
        assertEquals(SourceSetOwner.Platform("android"), owners["androidMain"])
        assertEquals(SourceSetOwner.Platform("jvm"), owners["jvmMain"])
    }

    @Test
    fun `GIVEN kmp android without the jvm flag and jvm WHEN assigning owners THEN commonMain is metadata`() {
        val owners =
            assignSourceSetOwners(
                graph(target("android", "androidjvm", isAndroid = true), target("jvm", "jvm"))
            )

        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
        assertEquals(SourceSetOwner.Platform("android"), owners["androidMain"])
        assertEquals(SourceSetOwner.Platform("jvm"), owners["jvmMain"])
    }

    @Test
    fun `GIVEN js and wasmJs sharing webMain WHEN assigning owners THEN commonMain and webMain are metadata`() {
        val owners =
            assignSourceSetOwners(
                graph(
                    target("js", "js"),
                    target("wasmJs", "wasm"),
                    extraParents =
                        mapOf(
                            "jsMain" to setOf("webMain"),
                            "wasmJsMain" to setOf("webMain"),
                            "webMain" to setOf("commonMain"),
                        ),
                )
            )

        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
        assertEquals(SourceSetOwner.Metadata("webMain"), owners["webMain"])
    }

    @Test
    fun `GIVEN desktop server and js with a custom shared set WHEN assigning owners THEN the shared set is synthetic`() {
        val owners =
            assignSourceSetOwners(
                graph(
                    target("desktop", "jvm"),
                    target("server", "jvm"),
                    target("js", "js"),
                    extraParents =
                        mapOf(
                            "desktopMain" to setOf("desktopAndServerMain"),
                            "serverMain" to setOf("desktopAndServerMain"),
                            "desktopAndServerMain" to setOf("commonMain"),
                        ),
                )
            )

        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
        assertEquals(
            SourceSetOwner.Synthetic("desktopAndServerMain", "desktop", "desktopMain"),
            owners["desktopAndServerMain"],
        )
    }

    @Test
    fun `GIVEN jvm and two ios targets WHEN assigning owners THEN native shared sets are native shared`() {
        val owners = assignSourceSetOwners(iosGraph())

        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
        listOf("iosMain", "appleMain", "nativeMain").forEach {
            assertEquals(SourceSetOwner.NativeShared(it), owners[it])
        }
        assertEquals(SourceSetOwner.Platform("iosArm64"), owners["iosArm64Main"])
        assertEquals(SourceSetOwner.Platform("iosX64"), owners["iosX64Main"])
        assertEquals(SourceSetOwner.Platform("jvm"), owners["jvmMain"])
    }

    @Test
    fun `GIVEN two js targets WHEN assigning owners THEN commonMain is synthetic on a js representative`() {
        val owners = assignSourceSetOwners(graph(target("b", "js"), target("a", "js")))

        assertEquals(SourceSetOwner.Synthetic("commonMain", "a", "aMain"), owners["commonMain"])
    }

    @Test
    fun `GIVEN wasmJs and wasmWasi WHEN assigning owners THEN commonMain is synthetic on a wasm representative`() {
        val owners =
            assignSourceSetOwners(graph(target("wasmWasi", "wasm"), target("wasmJs", "wasm")))

        assertEquals(
            SourceSetOwner.Synthetic("commonMain", "wasmJs", "wasmJsMain"),
            owners["commonMain"],
        )
    }

    @Test
    fun `GIVEN a source set no main compilation reaches WHEN assigning owners THEN it is absent`() {
        val g =
            graph(target("jvm", "jvm"), extraParents = mapOf("orphanMain" to setOf("commonMain")))

        assertNull(assignSourceSetOwners(g)["orphanMain"])
    }

    @Test
    fun `GIVEN every truth table shape WHEN assigning owners THEN each present source set has exactly one owner`() {
        val shapes =
            listOf(
                graph(target("jvm", "jvm")),
                graph(target("jvm", "jvm"), target("js", "js")),
                graph(target("desktop", "jvm"), target("server", "jvm")),
                graph(target("android", "jvm", isAndroid = true), target("jvm", "jvm")),
                graph(target("android", "androidjvm", isAndroid = true), target("jvm", "jvm")),
                graph(target("a", "js"), target("b", "js")),
                iosGraph(),
            )

        shapes.forEach { shape ->
            val owners = assignSourceSetOwners(shape)
            val present = shape.parents.keys + shape.parents.values.flatten()
            assertEquals(present, owners.keys, "every reachable source set owned once in $owners")
        }
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SourceSetOwnershipKgpRuleTest {
    @Test
    fun `GIVEN two jvm targets WHEN asking about a metadata compilation THEN it is not created`() {
        assertFalse(kgpCreatesMetadataCompilation(setOf("jvm"), 2))
    }

    @Test
    fun `GIVEN jvm and android jvm WHEN asking about a metadata compilation THEN it is created`() {
        assertTrue(kgpCreatesMetadataCompilation(setOf("jvm", "androidjvm"), 2))
    }

    @Test
    fun `GIVEN two native targets WHEN asking about a metadata compilation THEN it is created`() {
        assertTrue(kgpCreatesMetadataCompilation(setOf("native"), 2))
    }

    @Test
    fun `GIVEN one native target WHEN asking about a metadata compilation THEN it is not created`() {
        assertFalse(kgpCreatesMetadataCompilation(setOf("native"), 1))
    }

    @Test
    fun `GIVEN jvm and js WHEN asking about a metadata compilation THEN it is created`() {
        assertTrue(kgpCreatesMetadataCompilation(setOf("jvm", "js"), 2))
    }

    @Test
    fun `GIVEN a single target of any type WHEN asking about a metadata compilation THEN it is not created`() {
        assertFalse(kgpCreatesMetadataCompilation(setOf("jvm"), 1))
        assertFalse(kgpCreatesMetadataCompilation(setOf("js"), 1))
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SourceSetOwnershipRepresentativeTest {
    @Test
    fun `GIVEN an android and a jvm target WHEN choosing a representative THEN the non android one wins`() {
        val android = target("aaa", "jvm", isAndroid = true)
        val jvm = target("zzz", "jvm")

        assertEquals(jvm, chooseRepresentative(listOf(android, jvm)))
    }

    @Test
    fun `GIVEN targets of the same kind WHEN choosing a representative THEN the first by name wins`() {
        val desktop = target("desktop", "jvm")
        val server = target("server", "jvm")

        assertEquals(desktop, chooseRepresentative(listOf(server, desktop)))
    }

    @Test
    fun `GIVEN any input order WHEN choosing a representative THEN the result is the same`() {
        val all =
            listOf(
                target("zeta", "jvm"),
                target("alpha", "jvm", isAndroid = true),
                target("beta", "jvm"),
            )

        val results =
            listOf(all, all.reversed(), all.shuffled(Random(7))).map { chooseRepresentative(it) }

        assertEquals(setOf(target("beta", "jvm")), results.toSet())
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SourceSetOwnershipCounterpartTest {
    private val tests = setOf("commonTest", "desktopTest", "test")

    @Test
    fun `GIVEN desktopMain and a desktopTest WHEN finding the counterpart THEN desktopTest is returned`() {
        assertEquals("desktopTest", counterpartTestSourceSet("desktopMain", tests))
    }

    @Test
    fun `GIVEN commonMain WHEN finding the counterpart THEN commonTest is returned`() {
        assertEquals("commonTest", counterpartTestSourceSet("commonMain", tests))
    }

    @Test
    fun `GIVEN a main set without a test set WHEN finding the counterpart THEN null is returned`() {
        assertNull(counterpartTestSourceSet("serverMain", tests))
    }

    @Test
    fun `GIVEN a plain main source set WHEN finding the counterpart THEN test is returned`() {
        assertEquals("test", counterpartTestSourceSet("main", tests))
    }
}
