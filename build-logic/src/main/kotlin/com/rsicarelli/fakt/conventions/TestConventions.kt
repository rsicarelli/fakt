// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

/**
 * Test convention for Fakt modules.
 *
 * Provides:
 * - JUnit Platform (JUnit 5) configuration
 * - Parallel execution with optimal fork count
 * - Memory configuration and heap dumps on OOM
 * - JUnit 5 parallel execution settings
 * - Default timeouts
 */
internal fun Project.applyTestConventions() {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()

        // Parallel execution - maximize CPU usage
        maxParallelForks = Runtime.getRuntime().availableProcessors() * 2

        // Memory configuration
        jvmArgs(
            "-Xmx2g",
            "-XX:MaxMetaspaceSize=512m",
            "-XX:+HeapDumpOnOutOfMemoryError",
        )

        // JUnit 5 parallel execution
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")

        // Default timeout for all tests
        systemProperty("junit.jupiter.execution.timeout.default", "60s")
    }
}
