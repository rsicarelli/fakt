// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/** Modules consumed on Gradle's buildscript classpath (kotlin-dsl). */
private val GRADLE_COMPAT_MODULES = setOf("gradle-plugin", "compiler-api")

/**
 * Gradle buildscript compatibility convention.
 *
 * Configures Kotlin 2.0 language/API version for modules consumed on
 * Gradle's buildscript classpath (`gradle-plugin`, `compiler-api`).
 *
 * Gradle 8.x embeds Kotlin 2.0.x in `kotlin-dsl`, which can only read
 * metadata up to version 2.1.0. Without this, consumers get:
 * "metadata version is 2.3.0, but compiler can read up to 2.1.0"
 *
 * Also:
 * - Removes `-Xcontext-parameters` (requires Kotlin 2.2+, unused in these modules)
 * - Prevents kotlin-stdlib from leaking into published POM (Gradle provides its own)
 */
fun Project.applyGradleCompatibility() {
    if (name !in GRADLE_COMPAT_MODULES) return

    // Prevent Kotlin Gradle Plugin from adding kotlin-stdlib as `implementation`.
    // Must be set before afterEvaluate where KGP reads this property.
    // Gradle's kotlin-dsl already provides stdlib via the embedded Kotlin compiler;
    // including stdlib 2.3.20 would win version resolution over embedded 2.0.21.
    extensions.extraProperties.set("kotlin.stdlib.default.dependency", "false")
    dependencies.add("compileOnly", "org.jetbrains.kotlin:kotlin-stdlib")

    kotlinJvmExtension {
        compilerOptions {
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
        }
    }

    // Override compiler flags set by KotlinCompilerConvention:
    // - Disable progressive mode (only meaningful for latest language version)
    // - Remove -Xcontext-parameters (requires Kotlin 2.2+, incompatible with
    //   languageVersion 2.0). Keep -Xjsr305=strict for JVM null-safety.
    tasks.withType(KotlinCompilationTask::class.java).configureEach {
        compilerOptions {
            progressiveMode.set(false)
            freeCompilerArgs.set(listOf("-Xjsr305=strict"))
        }
    }
}
