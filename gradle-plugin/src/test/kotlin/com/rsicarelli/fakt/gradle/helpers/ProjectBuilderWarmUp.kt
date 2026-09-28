// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.helpers

import org.gradle.testfixtures.ProjectBuilder
import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener

/**
 * Initialises Gradle's `ProjectBuilder` once, on a single thread, before any test runs.
 *
 * This suite runs test classes concurrently (JUnit parallel execution, see the build-logic test
 * conventions). `ProjectBuilderImpl`'s static initializer calls `Logging.getLogger` while SLF4J may
 * still be initialising on another thread, which hands back an `org.slf4j.helpers.SubstituteLogger`
 * and fails the initializer with a `ClassCastException`. A failed static initializer poisons the
 * class for the rest of the JVM, so every `ProjectBuilder` test then fails with
 * `NoClassDefFoundError: Could not initialize class ProjectBuilderImpl`. Warming it up here makes
 * that first initialisation happen before any concurrency starts.
 *
 * Registered through `META-INF/services/org.junit.platform.launcher.LauncherSessionListener`.
 */
class ProjectBuilderWarmUp : LauncherSessionListener {
    override fun launcherSessionOpened(session: LauncherSession) {
        ProjectBuilder.builder()
    }
}
