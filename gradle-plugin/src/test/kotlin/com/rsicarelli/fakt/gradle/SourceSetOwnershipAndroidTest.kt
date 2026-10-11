// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.TestInstance

private fun androidTarget(
    mains: List<String>,
    members: List<String> = emptyList(),
    type: String = "androidjvm",
): TargetNode =
    TargetNode(
        name = "android",
        platformType = type,
        isAndroid = true,
        mainSourceSets = mains,
        memberSourceSets = members,
    )

private fun jvmTarget(): TargetNode = TargetNode("jvm", "jvm", false, listOf("jvmMain"))

/** Every set that a compilation reaches (default or member) has `commonMain` as direct parent. */
private fun graphOf(vararg targets: TargetNode): SourceSetGraph =
    SourceSetGraph(
        targets = targets.toList(),
        parents =
            targets
                .flatMap { it.mainSourceSets + it.memberSourceSets }
                .associateWith { setOf("commonMain") },
    )

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SourceSetOwnershipAndroidTest {
    @Test
    fun `GIVEN jvm and androidTarget with members WHEN assigning owners THEN androidMain and variants belong to android`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    androidTarget(
                        mains = listOf("androidDebug", "androidRelease"),
                        members = listOf("androidMain"),
                    ),
                    jvmTarget(),
                )
            )

        assertEquals(SourceSetOwner.Platform("android"), owners["androidMain"])
        assertEquals(SourceSetOwner.Platform("android"), owners["androidDebug"])
        assertEquals(SourceSetOwner.Platform("android"), owners["androidRelease"])
        assertEquals(SourceSetOwner.Platform("jvm"), owners["jvmMain"])
        assertEquals(SourceSetOwner.Metadata("commonMain"), owners["commonMain"])
    }

    @Test
    fun `GIVEN the same graph without members WHEN assigning owners THEN androidMain is absent`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    androidTarget(mains = listOf("androidDebug", "androidRelease")),
                    jvmTarget(),
                )
            )

        assertFalse("androidMain" in owners)
    }

    @Test
    fun `GIVEN a single androidTarget WHEN assigning owners THEN commonMain is synthetic on androidDebug`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    androidTarget(
                        mains = listOf("androidDebug", "androidRelease"),
                        members = listOf("androidMain"),
                    )
                )
            )

        assertEquals(
            SourceSetOwner.Synthetic("commonMain", "android", "androidDebug"),
            owners["commonMain"],
        )
        assertEquals(SourceSetOwner.Platform("android"), owners["androidMain"])
        assertEquals(SourceSetOwner.Platform("android"), owners["androidDebug"])
        assertEquals(SourceSetOwner.Platform("android"), owners["androidRelease"])
    }

    @Test
    fun `GIVEN a flavored single androidTarget WHEN assigning owners THEN the representative is the free debug variant`() {
        val owners =
            assignSourceSetOwners(
                graphOf(
                    androidTarget(
                        mains =
                            listOf("androidPaidRelease", "androidFreeDebug", "androidFreeRelease"),
                        members = listOf("androidMain", "androidFree", "androidDebug"),
                    )
                )
            )

        assertEquals(
            SourceSetOwner.Synthetic("commonMain", "android", "androidFreeDebug"),
            owners["commonMain"],
        )
    }

    @Test
    fun `GIVEN variants with a custom build type WHEN choosing the representative THEN debug wins`() {
        assertEquals(
            "androidDebug",
            representativeVariantSourceSet(listOf("androidBeta", "androidRelease", "androidDebug")),
        )
    }

    @Test
    fun `GIVEN variants without a debug one WHEN choosing the representative THEN the first by name wins`() {
        assertEquals(
            "androidBeta",
            representativeVariantSourceSet(listOf("androidRelease", "androidBeta")),
        )
    }

    @Test
    fun `GIVEN a single main set WHEN choosing the representative THEN it is that set`() {
        assertEquals("androidMain", representativeVariantSourceSet(listOf("androidMain")))
    }

    @Test
    fun `GIVEN an androidTarget with the jvm platform type alone WHEN assigning owners THEN commonMain stays with the platform`() {
        val owners =
            assignSourceSetOwners(
                graphOf(androidTarget(mains = listOf("androidMain"), type = "jvm"))
            )

        assertEquals(SourceSetOwner.Platform("android"), owners["commonMain"])
        assertEquals(SourceSetOwner.Platform("android"), owners["androidMain"])
    }

    @Test
    fun `GIVEN a single androidjvm library target WHEN assigning owners THEN commonMain is synthetic on androidMain`() {
        val owners = assignSourceSetOwners(graphOf(androidTarget(mains = listOf("androidMain"))))

        assertEquals(
            SourceSetOwner.Synthetic("commonMain", "android", "androidMain"),
            owners["commonMain"],
        )
    }
}
