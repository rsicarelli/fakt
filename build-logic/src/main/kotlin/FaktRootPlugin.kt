// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyApiValidationConvention
import com.rsicarelli.fakt.conventions.applyLicenseReportConvention
import com.rsicarelli.fakt.conventions.applySpotlessPredeclare
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension

/**
 * Root convention plugin for Fakt project.
 *
 * Applies and configures:
 * - Binary Compatibility Validator (apiValidation)
 * - License Report (dependency license auditing)
 * - Spotless predeclareDeps
 * - Relocated npm/Wasm-npm lock file directories (vendor/kotlin-js-store)
 *
 * Note: This plugin should ONLY be applied to the root project.
 * Individual projects apply fakt-spotless, fakt-detekt as needed.
 */
class FaktRootPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "FaktRootPlugin can only be applied to the root project"
        }

        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")
            pluginManager.apply("com.diffplug.spotless")

            applyApiValidationConvention()
            applyLicenseReportConvention()
            applySpotlessPredeclare()
            relocateJsPackageManagerLockFiles()
        }
    }

    /**
     * GitHub's Dependency Graph indexes the committed kotlin-js-store lock files as
     * npm_and_yarn manifests, but there's no sibling package.json for Dependabot to resolve
     * (it's build output, correctly gitignored) - so every matching security advisory spawns a
     * Dependabot Security Update job that fails deterministically. GitHub's dependency graph
     * parser skips manifests under vendor-style directory names, so move the lock files there.
     */
    private fun Project.relocateJsPackageManagerLockFiles() {
        plugins.withType(NodeJsRootPlugin::class.java) {
            the<NpmExtension>().lockFileDirectory.set(rootDir.resolve("vendor/kotlin-js-store"))
        }
        plugins.withType(WasmNodeJsRootPlugin::class.java) {
            the<WasmNpmExtension>().lockFileDirectory
                .set(rootDir.resolve("vendor/kotlin-js-store/wasm"))
        }
    }
}
