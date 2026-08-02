// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyCommonConventions
import com.rsicarelli.fakt.conventions.applyPublishingConvention
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for Kotlin Multiplatform modules (annotations).
 *
 * Applies:
 * - Publishing convention (group/version from gradle.properties)
 * - kotlin-multiplatform plugin
 * - Common conventions (toolchain, compiler, tests)
 *
 * Note:
 * - maven-publish plugin should be explicitly applied in module build file
 * - Ktlint is applied via FaktRootPlugin to all projects
 */
class FaktMultiplatformPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Retro-compatibility (see docs/compatibility.md): the published `annotations` artifact is
        // consumed by projects on Kotlin 2.2.0 through 2.4.10. Building it with the latest toolchain
        // otherwise pins a transitive kotlin-stdlib of the build version into the published POM,
        // which wins version resolution and forces that stdlib onto older consumers — a Kotlin 2.2.x
        // compiler cannot read a 2.4.10 stdlib's metadata. Stop the Kotlin Gradle Plugin from
        // exporting a stdlib dependency; every Kotlin consumer already supplies its own stdlib at its
        // own compiler version. Must be set before the plugin is applied, as KGP reads it eagerly.
        target.extensions.extraProperties.set("kotlin.stdlib.default.dependency", "false")

        // Apply Kotlin Multiplatform plugin
        target.pluginManager.apply("org.jetbrains.kotlin.multiplatform")

        // Apply common conventions (toolchain, compiler, tests)
        target.applyCommonConventions()
    }
}
