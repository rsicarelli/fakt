// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

private fun node(name: String, type: String, isAndroid: Boolean = false): TargetNode =
    TargetNode(name, type, isAndroid, listOf("${name}Main"))

private fun graphOf(
    vararg targets: TargetNode,
    extraParents: Map<String, Set<String>> = emptyMap(),
    metadataCompilations: Set<String> = emptySet(),
): SourceSetGraph {
    val base = targets.flatMap { it.mainSourceSets }.associateWith { setOf("commonMain") }
    return SourceSetGraph(targets.toList(), base + extraParents, metadataCompilations)
}

private val webParents =
    mapOf(
        "jsMain" to setOf("webMain"),
        "wasmJsMain" to setOf("webMain"),
        "webMain" to setOf("commonMain"),
    )

private val desktopAndServerParents =
    mapOf(
        "desktopMain" to setOf("desktopAndServerMain"),
        "serverMain" to setOf("desktopAndServerMain"),
        "desktopAndServerMain" to setOf("commonMain"),
    )

private val jvmAndAndroidParents =
    mapOf(
        "jvmMain" to setOf("jvmAndAndroidMain"),
        "androidMain" to setOf("jvmAndAndroidMain"),
        "jvmAndAndroidMain" to setOf("commonMain"),
    )

private val nativeParents =
    mapOf(
        "iosArm64Main" to setOf("iosMain"),
        "iosX64Main" to setOf("iosMain"),
        "iosMain" to setOf("nativeMain"),
        "nativeMain" to setOf("commonMain"),
    )

private fun ancestorsOf(set: String, parents: Map<String, Set<String>>): Set<String> {
    val seen = linkedSetOf<String>()
    val pending = ArrayDeque(parents[set].orEmpty())
    while (pending.isNotEmpty()) {
        val next = pending.removeFirst()
        if (seen.add(next)) pending.addAll(parents[next].orEmpty())
    }
    return seen
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SourceSetOwnershipIntermediateTest {
    @Test
    fun `GIVEN a set KGP built a metadata compilation for WHEN assigning owners THEN it is metadata even if the prediction says synthetic`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    node("js", "js"),
                    node("wasmJs", "wasm"),
                    extraParents = webParents,
                    metadataCompilations = setOf("webMain"),
                )
            )

        assertEquals(SourceSetOwner.Metadata("webMain"), owners["webMain"])
        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
    }

    @Test
    fun `GIVEN commonMain with a metadata compilation under two jvm targets WHEN assigning owners THEN it is metadata`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    node("desktop", "jvm"),
                    node("server", "jvm"),
                    metadataCompilations = setOf("commonMain"),
                )
            )

        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
    }

    @Test
    fun `GIVEN a native only set with a metadata compilation WHEN assigning owners THEN it is native shared`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    node("a", "native"),
                    node("b", "native"),
                    extraParents =
                        mapOf("aMain" to setOf("nativeMain"), "bMain" to setOf("nativeMain")),
                    metadataCompilations = setOf("nativeMain"),
                )
            )

        assertEquals(SourceSetOwner.NativeShared("nativeMain"), owners["nativeMain"])
    }

    @Test
    fun `GIVEN a set that is not listed as built WHEN assigning owners THEN the KGP prediction decides`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    node("desktop", "jvm"),
                    node("server", "jvm"),
                    extraParents = desktopAndServerParents,
                )
            )

        assertEquals(
            SourceSetOwner.Synthetic("desktopAndServerMain", "desktop", "desktopMain"),
            owners["desktopAndServerMain"],
        )
    }

    @Test
    fun `GIVEN a listed set whose parent is not listed WHEN assigning owners THEN the parent is metadata too`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    node("js", "js"),
                    node("wasmJs", "wasm"),
                    extraParents =
                        mapOf(
                            "jsMain" to setOf("leafMain"),
                            "wasmJsMain" to setOf("leafMain"),
                            "leafMain" to setOf("midMain"),
                            "midMain" to setOf("commonMain"),
                        ),
                    metadataCompilations = setOf("leafMain"),
                )
            )

        assertEquals(SourceSetOwner.Metadata("leafMain"), owners["leafMain"])
        assertEquals(SourceSetOwner.Metadata("midMain"), owners["midMain"])
    }

    @Test
    fun `GIVEN many shapes WHEN assigning owners THEN every ancestor of a metadata set is metadata or native shared`() {
        val js = node("js", "js")
        val wasm = node("wasmJs", "wasm")
        val jvm = node("jvm", "jvm")
        val shapes =
            listOf(
                graphOf(js, wasm, jvm, extraParents = webParents),
                graphOf(
                    js,
                    wasm,
                    extraParents = webParents,
                    metadataCompilations = setOf("webMain"),
                ),
                graphOf(
                    node("desktop", "jvm"),
                    node("server", "jvm"),
                    js,
                    extraParents = desktopAndServerParents,
                ),
                graphOf(
                    jvm,
                    node("android", "androidjvm", true),
                    js,
                    extraParents = jvmAndAndroidParents,
                ),
                graphOf(
                    jvm,
                    node("iosArm64", "native"),
                    node("iosX64", "native"),
                    extraParents = nativeParents,
                ),
            )

        shapes.forEach { shape ->
            val owners = assignSourceSetOwners(shape)
            owners
                .filterValues { it is SourceSetOwner.Metadata }
                .keys
                .forEach { metadataSet ->
                    ancestorsOf(metadataSet, shape.parents).forEach { ancestor ->
                        assertTrue(
                            owners[ancestor] is SourceSetOwner.Metadata ||
                                owners[ancestor] is SourceSetOwner.NativeShared,
                            "$ancestor above $metadataSet must be metadata or native shared: $owners",
                        )
                    }
                }
        }
    }

    @Test
    fun `GIVEN a manually declared webMain WHEN assigning owners THEN it is metadata with or without the built list`() {
        listOf(emptySet(), setOf("webMain")).forEach { built ->
            val owners =
                assignSourceSetOwners(
                    graphOf(
                        node("js", "js"),
                        node("wasmJs", "wasm"),
                        extraParents = webParents,
                        metadataCompilations = built,
                    )
                )

            assertEquals(SourceSetOwner.Metadata("webMain"), owners["webMain"], "built=$built")
        }
    }

    @Test
    fun `GIVEN desktop server and js WHEN assigning owners THEN desktopAndServerMain is synthetic while commonMain is metadata`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    node("desktop", "jvm"),
                    node("server", "jvm"),
                    node("js", "js"),
                    extraParents = desktopAndServerParents,
                )
            )

        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
        assertEquals(
            SourceSetOwner.Synthetic("desktopAndServerMain", "desktop", "desktopMain"),
            owners["desktopAndServerMain"],
        )
    }

    @Test
    fun `GIVEN desktop server and cli jvm targets WHEN assigning owners THEN both shared sets are synthetic on their own representative`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    node("desktop", "jvm"),
                    node("server", "jvm"),
                    node("cli", "jvm"),
                    extraParents = desktopAndServerParents,
                )
            )

        assertEquals(SourceSetOwner.Synthetic("commonMain", "cli", "cliMain"), owners["commonMain"])
        assertEquals(
            SourceSetOwner.Synthetic("desktopAndServerMain", "desktop", "desktopMain"),
            owners["desktopAndServerMain"],
        )
    }

    @Test
    fun `GIVEN jvm android and js WHEN assigning owners THEN jvmAndAndroidMain is synthetic on jvm while commonMain is metadata`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    node("jvm", "jvm"),
                    node("android", "androidjvm", true),
                    node("js", "js"),
                    extraParents = jvmAndAndroidParents,
                )
            )

        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
        assertEquals(
            SourceSetOwner.Synthetic("jvmAndAndroidMain", "jvm", "jvmMain"),
            owners["jvmAndAndroidMain"],
        )
    }

    @Test
    fun `GIVEN only jvm and android WHEN assigning owners THEN commonMain keeps its metadata rule and jvmAndAndroidMain is synthetic`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    node("jvm", "jvm"),
                    node("android", "androidjvm", true),
                    extraParents = jvmAndAndroidParents,
                )
            )

        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
        assertEquals(
            SourceSetOwner.Synthetic("jvmAndAndroidMain", "jvm", "jvmMain"),
            owners["jvmAndAndroidMain"],
        )
    }

    @Test
    fun `GIVEN KGP lists a jvm and android only set as built WHEN assigning owners THEN it is still synthetic because KGP disables its compile`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    node("jvm", "jvm"),
                    node("android", "androidjvm", true),
                    node("js", "js"),
                    extraParents = jvmAndAndroidParents,
                    metadataCompilations = setOf("commonMain", "jvmAndAndroidMain"),
                )
            )

        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
        assertEquals(
            SourceSetOwner.Synthetic("jvmAndAndroidMain", "jvm", "jvmMain"),
            owners["jvmAndAndroidMain"],
        )
    }
}
