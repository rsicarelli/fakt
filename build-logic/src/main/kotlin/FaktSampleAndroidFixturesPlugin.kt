// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for a Fakt Android sample module that PRODUCES test fixtures.
 *
 * Applies [FaktSampleAndroidPlugin] (com.android.library + Kotlin Android + namespace/SDK config)
 * and then turns on AGP's native test fixtures via `android { testFixtures { enable = true } }`.
 * Fakt routes its generated fakes into that `testFixtures` source set (enabled by
 * `fakt { useGradleTestFixtures.set(true) }` in the module build script).
 *
 * Kotlin compilation of the Android `testFixtures` source set additionally requires the Gradle
 * property `android.experimental.enableTestFixturesKotlinSupport=true` on AGP 8.x (default on AGP
 * 9.0+); the sample sets it in `gradle.properties`.
 */
class FaktSampleAndroidFixturesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("fakt-sample-android")

            extensions.configure<LibraryExtension> { testFixtures { enable = true } }
        }
    }
}
