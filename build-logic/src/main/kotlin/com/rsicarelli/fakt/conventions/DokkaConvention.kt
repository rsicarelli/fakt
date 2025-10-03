// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.dokka.gradle.DokkaExtension

/**
 * Dokka documentation convention.
 *
 * Configures:
 * - Module name for generated documentation
 * - Output directory (docs/api)
 * - Includes from README.md
 */
fun Project.applyDokkaConvention() {
    configure<DokkaExtension> {
        // Dokka 2.x configuration is simplified
        moduleName.set("Fakt")
    }
}
