// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Base convention plugin for all Fakt modules.
 *
 * Provides:
 * - JVM toolchain configuration (Java 21)
 * - Kotlin compiler options (progressive mode, JVM target)
 * - Test task configuration
 *
 * Note: Spotless formatting is configured in root build.gradle.kts via allprojects
 */
class FaktBasePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // Configure Java toolchain
    target.pluginManager.withPlugin("java") {
      target.extensions.configure<JavaPluginExtension> {
        toolchain {
          languageVersion.set(JavaLanguageVersion.of(21))
        }
      }
    }

    // Configure Kotlin compilation
    target.plugins.withType<KotlinBasePlugin> {
      target.tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
          progressiveMode.set(true)

          // Configure JVM target if applicable
          if (this is org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions) {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.addAll("-Xjvm-default=all")
          }
        }
      }

      // Enable explicit API mode only for runtime (public API)
      // Compiler and gradle-plugin are internal implementations
      if (target.name == "runtime") {
        target.extensions.configure<KotlinProjectExtension> {
          explicitApi()
        }
      }
    }

    // Configure test tasks
    target.tasks.withType<Test>().configureEach {
      useJUnitPlatform()

      // Parallel execution
      maxParallelForks = Runtime.getRuntime().availableProcessors() * 2

      // Memory configuration
      jvmArgs("-Xmx2g", "-XX:MaxMetaspaceSize=512m")

      // Default timeout
      systemProperty("junit.jupiter.execution.timeout.default", "60s")
    }
  }
}