// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Publishing convention plugin for all Fakt modules.
 *
 * Note: This plugin is intentionally empty. Publishing configuration is done
 * in the root build.gradle.kts using plugins.withId("com.vanniktech.maven.publish")
 * to avoid plugin application order issues.
 *
 * The vanniktech plugin automatically reads these properties from gradle.properties:
 * - GROUP, VERSION_NAME (from root gradle.properties)
 * - POM_ARTIFACT_ID, POM_NAME, POM_DESCRIPTION, POM_URL, etc.
 * - POM_LICENCE_*, POM_DEVELOPER_*, POM_SCM_*
 * - SONATYPE_*, RELEASE_SIGNING_ENABLED
 *
 * To publish a module, simply apply the plugin in the module's build.gradle.kts:
 * plugins {
 *   alias(libs.plugins.mavenPublish)
 * }
 */
class FaktPublishingPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // This plugin is a marker - actual configuration is in root build.gradle.kts
    // to avoid plugin lifecycle issues
  }
}