// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

/*
 * KMP module with only `jvm()` and `js { nodejs() }` (no Native). commonMain code needs the same
 * two options as the :jvm module, set the way KMP builds usually set them:
 * - an ERROR-level opt-in marker listed in `languageSettings.optIn`,
 * - -Xcontext-sensitive-resolution through `compilerOptions.freeCompilerArgs`.
 * The commonMain producer (metadata) and the jvm and js producers must all receive them.
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

kotlin {
    jvm()
    js { nodejs() }

    compilerOptions { freeCompilerArgs.add("-Xcontext-sensitive-resolution") }

    sourceSets {
        // Kotlin requires every dependent source set to repeat its dependency's opt-ins.
        configureEach {
            languageSettings.optIn("com.rsicarelli.fakt.samples.compilerInteropKmp.InteropMarker")
        }

        commonMain {
            dependencies {
                implementation(libs.fakt.annotations)
                implementation(libs.coroutines)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
            }
        }
    }
}

fakt {
    logLevel.set(LogLevel.DEBUG)
}
