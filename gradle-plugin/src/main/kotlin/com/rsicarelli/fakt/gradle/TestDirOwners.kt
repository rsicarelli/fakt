// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.Project

/**
 * Project extra property holding the [TestDirOwners] registry: which `FaktGenerateTask` feeds which
 * test source set, for the producers that are not tied to `commonTest` or a platform consumer name
 * (the intermediate producers). [SourceSetConfigurator] reads it so it never registers the plain
 * `build/generated/fakt/<testSet>/kotlin` directory next to the task output.
 */
internal const val TEST_DIR_OWNERS_PROPERTY: String = "fakt.testDirOwners"

/**
 * The test source sets a task owns ([bySourceSet], test set to task name) and every task that
 * registered here ([tasks]), so a repeated registration is told apart from a foreign task that
 * happens to have the same name.
 */
internal class TestDirOwners {
    val bySourceSet: MutableMap<String, String> = linkedMapOf()
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
