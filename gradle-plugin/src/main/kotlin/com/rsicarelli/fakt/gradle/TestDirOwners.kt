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

/** Whether a Fakt task feeds [testSourceSet] with its generated directory. */
internal fun isTestDirOwned(project: Project, testSourceSet: String): Boolean =
    project.extensions.extraProperties.has(TEST_DIR_OWNERS_PROPERTY) &&
        testSourceSet in testDirOwners(project).bySourceSet

/** Whether [taskName] is a task Fakt itself registered through the registry. */
internal fun isFaktTestDirOwner(project: Project, taskName: String): Boolean =
    project.extensions.extraProperties.has(TEST_DIR_OWNERS_PROPERTY) &&
        taskName in testDirOwners(project).tasks

/**
 * A non-metadata compilation as the owner query sees it: the name of its default source set and the
 * [compilationKey] of every compilation it is associated with.
 */
internal data class TestCompilationNode(
    val defaultSourceSet: String,
    val associatedMainKeys: List<String>,
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
 * that default source set is associated with. Associations are read on every call.
 */
internal fun testDirOwnerOf(project: Project, testSet: String): String? {
    val owners = testDirOwners(project)
    return owners.bySourceSet[testSet]
        ?: ownerByAssociation(testSet, associationNodes(project), owners.byMainCompilation)
}

/** Pure part of [testDirOwnerOf]: the association rule over already read [tests]. */
internal fun ownerByAssociation(
    testSet: String,
    tests: List<TestCompilationNode>,
    byMain: Map<String, String>,
): String? =
    tests
        .filter { it.defaultSourceSet == testSet }
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
                    associatedMainKeys =
                        compilation.associatedCompilations.map {
                            compilationKey(target.name, it.name)
                        },
                )
            }
        }
