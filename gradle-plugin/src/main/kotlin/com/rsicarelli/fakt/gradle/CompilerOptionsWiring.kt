// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder

/**
 * Hands the options of [compilation] to [task], so the Fakt worker analyses the code under the same
 * rules as `compileKotlin*`.
 *
 * What is wired:
 * - `compilerArguments`: language and API version, opt-ins, progressive mode, free args and the
 *   language features of the default source set. JVM flags (`-jvm-target`, `-jvm-default`,
 *   `-no-jdk`) are added only for JVM compilations (`-no-jdk` not for Android). The list is built
 *   in a `project.provider`, so it is read when the task is configured for execution and sees late
 *   edits of the options.
 * - `jdkVersion` and `jdkHome` (plain JVM compilations only, see [usesToolchainJdk]): the JDK of
 *   the project's Java toolchain. Gradle falls back to the JVM that runs the build when no
 *   toolchain is set.
 *
 * It uses `project.provider` and not `compileTaskProvider.map`: `map` would make the task depend on
 * `compileKotlin*`, and generation would run after compilation instead of before it.
 *
 * Known limit: a per-task override such as `kotlinJavaToolchain.toolchain.use(...)` on the compile
 * task is ignored. Only `java.toolchain` of the project is read.
 */
internal fun configureCompilerOptions(
    project: Project,
    task: FaktGenerateTask,
    compilation: KotlinCompilation<*>,
) {
    task.compilerArguments.set(
        project.provider { forwardedCompilerArguments(snapshotOf(compilation)) }
    )
    if (usesToolchainJdk(compilation.platformType)) {
        project.plugins.withType(JavaBasePlugin::class.java) { wireToolchainJdk(project, task) }
    }
}

/**
 * Only plain JVM compilations get the toolchain JDK wiring and `-no-jdk`. Android is left out on
 * purpose: `kotlin-android` sets `noJdk = true`, so forwarding it would leave the worker without a
 * JDK and, when the boot classpath lookup falls back, without `android.jar` too. Android keeps the
 * worker JDK plus `android.jar`, and its `jdkVersion` (just Gradle's JVM major) would only cause
 * cache misses between machines.
 */
internal fun usesToolchainJdk(platformType: KotlinPlatformType): Boolean =
    platformType == KotlinPlatformType.jvm

/** Reads the options now. Call it inside a provider, never while the build is configuring. */
private fun snapshotOf(compilation: KotlinCompilation<*>): CompilerOptionsSnapshot {
    val options: KotlinCommonCompilerOptions = compilation.compileTaskProvider.get().compilerOptions
    val settings = compilation.defaultSourceSet.languageSettings
    val common =
        CompilerOptionsSnapshot(
            languageVersion = options.languageVersion.orNull?.version,
            apiVersion = options.apiVersion.orNull?.version,
            optIn = options.optIn.get() + settings.optInAnnotationsInUse,
            progressiveMode = options.progressiveMode.get(),
            freeCompilerArgs = options.freeCompilerArgs.get(),
            languageFeatures = settings.languageFeatures(),
        )
    return if (options is KotlinJvmCompilerOptions)
        common.withJvm(options, usesToolchainJdk(compilation.platformType))
    else common
}

/**
 * `enabledLanguageFeatures` is deprecated at ERROR level in KGP 2.4 ("internal Kotlin compiler
 * argument"), so a direct call does not compile. Builds that still call `enableLanguageFeature`
 * fill it, and dropping them would make the worker reject code that `compileKotlin*` accepts, so
 * the getter is read by reflection. An absent getter means no features.
 */
private fun LanguageSettingsBuilder.languageFeatures(): List<String> =
    runCatching {
            val getter = javaClass.getMethod("getEnabledLanguageFeatures")
            (getter.invoke(this) as? Set<*>).orEmpty().map(Any?::toString)
        }
        .getOrDefault(emptyList())

private fun CompilerOptionsSnapshot.withJvm(
    options: KotlinJvmCompilerOptions,
    forwardNoJdk: Boolean,
) =
    copy(
        jvmTarget = options.jvmTarget.orNull?.target,
        jvmDefault = options.jvmDefault.orNull?.compilerArgument,
        noJdk = forwardNoJdk && options.noJdk.getOrElse(false),
    )

private fun wireToolchainJdk(project: Project, task: FaktGenerateTask) {
    val launcher =
        project.extensions
            .getByType(JavaToolchainService::class.java)
            .launcherFor(project.extensions.getByType(JavaPluginExtension::class.java).toolchain)
    task.jdkHome.set(project.layout.dir(launcher.map { it.metadata.installationPath.asFile }))
    task.jdkVersion.set(launcher.map { it.metadata.languageVersion.asInt() })
}
