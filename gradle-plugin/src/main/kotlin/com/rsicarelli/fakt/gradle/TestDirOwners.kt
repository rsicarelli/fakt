// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget

/**
 * Project extra property holding the [TestDirOwners] registry: which `FaktGenerateTask` feeds which
 * test source set, for the producers that are not tied to `commonTest` or a platform consumer name
 * (the intermediate producers). [SourceSetConfigurator] reads it so it never registers the plain
 * `build/generated/fakt/<testSet>/kotlin` directory next to the task output.
 */
internal const val TEST_DIR_OWNERS_PROPERTY: String = "fakt.testDirOwners"

/**
 * The test source sets a task owns ([bySourceSet], test set to task name), the main compilations
 * whose task owns every test compilation associated with them ([byMainCompilation],
 * `<target>/<compilation>` key to task name), and every task that registered here ([tasks]), so a
 * repeated registration is told apart from a foreign task that happens to have the same name.
 */
internal class TestDirOwners {
    val bySourceSet: MutableMap<String, String> = linkedMapOf()
    val byMainCompilation: MutableMap<String, String> = linkedMapOf()
    val tasks: MutableSet<String> = linkedSetOf()
}

/** The registry of [project], created on first use. */
internal fun testDirOwners(project: Project): TestDirOwners {
    val extra = project.extensions.extraProperties
    if (!extra.has(TEST_DIR_OWNERS_PROPERTY)) extra.set(TEST_DIR_OWNERS_PROPERTY, TestDirOwners())
    return extra.get(TEST_DIR_OWNERS_PROPERTY) as TestDirOwners
}

/** The registry of [project] when a producer created it, else `null`; never creates it. */
private fun existingTestDirOwners(project: Project): TestDirOwners? =
    project.extensions.extraProperties
        .takeIf { it.has(TEST_DIR_OWNERS_PROPERTY) }
        ?.get(TEST_DIR_OWNERS_PROPERTY) as? TestDirOwners

/** Whether [taskName] is a task Fakt itself registered through the registry. */
internal fun isFaktTestDirOwner(project: Project, taskName: String): Boolean =
    existingTestDirOwners(project)?.tasks?.contains(taskName) == true

/**
 * A non-metadata compilation as the owner query sees it: the name of its default source set, the
 * [compilationKey] of every compilation it is associated with, and the other source sets that are
 * members of it ([memberSourceSets], e.g. `androidUnitTest` in AGP's `debugUnitTest`).
 */
internal data class TestCompilationNode(
    val defaultSourceSet: String,
    val associatedMainKeys: List<String>,
    val memberSourceSets: List<String> = emptyList(),
)

/** The registry key of a compilation: `<target>/<compilation>`. */
internal fun compilationKey(target: String, compilation: String): String = "$target/$compilation"

/**
 * Records that [task] owns every test compilation associated with the main compilation [mainKey].
 * The association itself is read later, by [testDirOwnerOf], so it can be made after this call.
 */
internal fun claimAssociatedTests(project: Project, mainKey: String, task: String) {
    val owners = testDirOwners(project)
    owners.byMainCompilation[mainKey] = task
    owners.tasks += task
}

/**
 * Records that [task] owns the test source set [testSet] outright (`commonTest` for the common
 * producers and the single-target task, the intermediate producers' test sets).
 */
internal fun claimTestSourceSet(project: Project, testSet: String, task: String) {
    val owners = testDirOwners(project)
    owners.bySourceSet[testSet] = task
    owners.tasks += task
}

/**
 * The task that owns [testSet], or `null` when none does: an explicit [TestDirOwners.bySourceSet]
 * claim first, otherwise the task of the first main compilation a non-metadata compilation with
 * that default source set, or that has it as a member, is associated with. Associations are read on
 * every call. Read only: a project without a registry has no owner and the registry is not created.
 */
internal fun testDirOwnerOf(project: Project, testSet: String): String? =
    existingTestDirOwners(project)?.let { owners ->
        owners.bySourceSet[testSet]
            ?: ownerByAssociation(testSet, associationNodes(project), owners.byMainCompilation)
    }

/**
 * Pure part of [testDirOwnerOf]: the association rule over already read [tests]. A test set shared
 * by several compilations (Android's `androidUnitTest` is a member of both `debugUnitTest` and
 * `releaseUnitTest`) is fed by several variant tasks, so the answer there only means "owned": it is
 * the task of the first claimed association, in [tests] order.
 */
internal fun ownerByAssociation(
    testSet: String,
    tests: List<TestCompilationNode>,
    byMain: Map<String, String>,
): String? =
    tests
        .filter { it.defaultSourceSet == testSet || testSet in it.memberSourceSets }
        .flatMap { it.associatedMainKeys }
        .firstNotNullOfOrNull { byMain[it] }

private fun associationNodes(project: Project): List<TestCompilationNode> =
    project.extensions
        .findByType(KotlinMultiplatformExtension::class.java)
        ?.targets
        .orEmpty()
        .filter { it !is KotlinMetadataTarget }
        .flatMap { target ->
            target.compilations.map { compilation ->
                TestCompilationNode(
                    defaultSourceSet = compilation.defaultSourceSet.name,
                    memberSourceSets =
                        compilation.kotlinSourceSets
                            .map { it.name }
                            .filter { it != compilation.defaultSourceSet.name },
                    associatedMainKeys =
                        compilation.associatedCompilations.map {
                            compilationKey(target.name, it.name)
                        },
                )
            }
        }
