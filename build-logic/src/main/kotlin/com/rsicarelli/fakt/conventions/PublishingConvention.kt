// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import org.gradle.api.Project

/**
 * Publishing convention for Fakt modules.
 *
 * Applies project coordinates (group, version) from gradle.properties.
 * These are required by maven-publish plugin to publish artifacts correctly.
 *
 * **gradle.properties values:**
 * - GROUP=com.rsicarelli.fakt
 * - VERSION_NAME=1.0.0-SNAPSHOT
 *
 * **Note:** The vanniktech/gradle-maven-publish-plugin is configured via
 * gradle.properties and automatically picks up POM metadata.
 *
 * See: https://vanniktech.github.io/gradle-maven-publish-plugin/
 */
fun Project.applyPublishingConvention() {
    // Read from gradle.properties (inherited from root project)
    val groupId = findProperty("GROUP") as String? ?: "com.rsicarelli.fakt"
    val versionName = findProperty("VERSION_NAME") as String? ?: "1.0.0-SNAPSHOT"

    // Apply to project
    group = groupId
    version = versionName

    logger.info("Fakt: Applied publishing convention - group=$group, version=$version")
}
