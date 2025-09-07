// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import kotlinx.validation.ExperimentalBCVApi
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.dokka)
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.atomicfu) apply false
  alias(libs.plugins.spotless)
  alias(libs.plugins.binaryCompatibilityValidator)
}

apiValidation {
  ignoredProjects += listOf("compiler-unified", "compiler-tests")
  ignoredPackages += listOf("dev.rsicarelli.ktfake.internal")
  @OptIn(ExperimentalBCVApi::class)
  klib {
    enabled = true
  }
}

dokka {
  dokkaPublications.html {
    // NOTE: This path must be in sync with documentation structure
    outputDirectory.set(rootDir.resolve("docs/api"))
    includes.from(project.layout.projectDirectory.file("README.md"))
  }
}

val ktfmtVersion = libs.versions.ktfmt.get()

spotless { predeclareDeps() }

configure<SpotlessExtensionPredeclare> {
  kotlin { ktfmt(ktfmtVersion).googleStyle().configure { it.setRemoveUnusedImports(true) } }
  kotlinGradle { ktfmt(ktfmtVersion).googleStyle().configure { it.setRemoveUnusedImports(true) } }
  java { googleJavaFormat(libs.versions.gjf.get()).reorderImports(true).reflowLongStrings(true) }
}

// Configure spotless in subprojects
allprojects {
  apply(plugin = "com.diffplug.spotless")
  configure<SpotlessExtension> {
    format("misc") {
      target("*.gradle", "*.md", ".gitignore")
      trimTrailingWhitespace()
      leadingTabsToSpaces(2)
      endWithNewline()
    }
    java {
      target("src/**/*.java")
      trimTrailingWhitespace()
      endWithNewline()
      targetExclude("**/spotless.java")
      targetExclude("**/src/test/data/**")
      targetExclude("**/*Generated.java")
    }
    kotlin {
      target("src/**/*.kt")
      trimTrailingWhitespace()
      endWithNewline()
      targetExclude("**/spotless.kt")
      targetExclude("**/src/test/data/**")
    }
    kotlinGradle {
      target("*.kts")
      trimTrailingWhitespace()
      endWithNewline()
      licenseHeaderFile(
        rootProject.file("spotless/spotless.kt"),
        "(import|plugins|buildscript|dependencies|pluginManagement|dependencyResolutionManagement)",
      )
    }
    // Apply license formatting separately for kotlin files
    format("licenseKotlin") {
      licenseHeaderFile(rootProject.file("spotless/spotless.kt"), "(package|@file:)")
      target("src/**/*.kt")
      targetExclude("**/spotless.kt")
      targetExclude("**/src/test/data/**")
    }
  }
}

// Configure Kotlin compilation for all subprojects
subprojects {
  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    configure<KotlinProjectExtension> {
      jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
      }
    }

    tasks.withType<KotlinCompile>().configureEach {
      compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
        freeCompilerArgs.addAll(
          "-progressive",
          "-Xjsr305=strict",
          "-Xjvm-default=all",
          "-Xtype-enhancement-improvements-strict-mode",
          "-Xcontext-receivers",
        )
      }
    }

    // Configure test tasks
    tasks.withType<Test>().configureEach {
      useJUnitPlatform()
      jvmArgs("-XX:+HeapDumpOnOutOfMemoryError")
      systemProperty("junit.jupiter.execution.parallel.enabled", "true")
      systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    }
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    configure<KotlinProjectExtension> {
      jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
      }
    }

    tasks.withType<KotlinCompilationTask<*>>().configureEach {
      compilerOptions {
        freeCompilerArgs.addAll(
          "-progressive",
          "-Xcontext-receivers",
        )
      }
    }
  }

  // Configure publishing
  pluginManager.withPlugin("com.vanniktech.maven.publish.base") {
    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(automaticRelease = true)
      signAllPublications()
    }
  }
}
