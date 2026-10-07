// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalFaktMultiModule::class)

package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.android.AndroidVariantSources
import java.util.Base64
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * The lone real (non-`metadata`) target's `KotlinPlatformType` name when this KMP project declares
 * exactly one target, `null` otherwise.
 *
 * Only a project with more than one real target gets a per-source-set `commonMain` metadata
 * compilation — the one the cache-correct path drives as the common producer. A **single-target**
 * project gets only KGP's legacy metadata `main` compilation, which carries `commonMain` as its
 * default source set but resolves an empty compile classpath, so `KotlinMetadataCompiler` cannot be
 * driven over it. Its lone platform main therefore owns the common fakes too (see
 * `routeCompilation`); treating it as a source-partitioned consumer instead would silently drop
 * every `@Fake` declared in `commonMain`.
 *
 * Counting targets rather than looking the compilation up is deliberate. KGP creates the metadata
 * target's per-source-set compilations *after* it has resolved subplugins for every platform main,
 * so `commonMain` is still absent while `jvmMain` is being decided and the lookup would report
 * single-target for every project. The target set, by contrast, is complete before the routing
 * decision runs — KGP resolves subplugins once the `kotlin { }` block has run.
 */
private fun singleTargetPlatformTypeName(
    kmp: org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
): String? =
    kmp.targets
        .filter { !it.platformType.name.equals("common", ignoreCase = true) }
        .singleOrNull()
        ?.platformType
        ?.name

/**
 * Why the cache-correct producer cannot read this project's sources, or `null` when it can.
 *
 * AGP 9 ships built-in Kotlin support and rejects KGP's `org.jetbrains.kotlin.android` plugin. In
 * that setup the `KotlinCompilation`s handed to [FaktGradleSubplugin.applyToCompilation] carry
 * `KotlinSourceSet`s whose `kotlin.srcDirs` stay **empty** — AGP keeps the sources in its own
 * variant model (measured on `samples/compat-agp/agp-9.0`: `srcDirs=[]` even at
 * `projectsEvaluated`). Their producers read the sources from AGP's variant API instead
 * ([AndroidVariantSources], issue #154).
 *
 * Keyed off the applied plugin ids rather than probing `srcDirs`, for the same reason
 * [singleTargetPlatformTypeName] counts targets: source sets are not reliably populated at the
 * point the routing decision runs, so an emptiness probe would send healthy KGP projects down the
 * legacy path too. Plugin ids are settled in the `plugins { }` block, long before subplugin
 * resolution.
 */
private fun unreadableSourcesReason(project: Project): String? =
    unreadableSourcesReason(
        hasAndroidPlugin = AndroidVariantSources.hasAndroidPlugin(project),
        hasKotlinAndroidPlugin = AndroidVariantSources.hasKotlinAndroidPlugin(project),
        isMultiplatform = AndroidVariantSources.isMultiplatform(project),
        canReadVariantSources = AndroidVariantSources.isAvailable(project),
    )

/**
 * Pure decision behind [unreadableSourcesReason], `Project`-free so the truth table is
 * unit-testable (mirrors [shouldEnableTestFixtures]). Only variant-model Android plugins
 * (`com.android.library`, `com.android.application`, …) without KGP's
 * `org.jetbrains.kotlin.android` can be unreadable:
 * - Kotlin Multiplatform with `androidTarget()`: KGP compiles one compilation per Android variant
 *   (`debug`, `release`), whose default source sets (`androidDebug`) exclude `androidMain`, so a
 *   per-compilation task would drop its fakes. Stays in-process until it has a per-variant design.
 * - AGP 9 built-in Kotlin: readable through AGP's variant API, unless Fakt could not hook it.
 *
 * @param hasAndroidPlugin whether a variant-model Android plugin is applied.
 * @param hasKotlinAndroidPlugin whether KGP's `org.jetbrains.kotlin.android` is applied.
 * @param isMultiplatform whether the Kotlin Multiplatform plugin is applied.
 * @param canReadVariantSources whether AGP's variant API is visible and hooked for this project.
 */
internal fun unreadableSourcesReason(
    hasAndroidPlugin: Boolean,
    hasKotlinAndroidPlugin: Boolean,
    isMultiplatform: Boolean,
    canReadVariantSources: Boolean,
): String? =
    when {
        !hasAndroidPlugin || hasKotlinAndroidPlugin -> null
        isMultiplatform ->
            "this multiplatform project declares androidTarget() through the com.android.library " +
                "or com.android.application plugin, whose per-variant compilations Fakt cannot " +
                "drive from a Gradle task yet; the com.android.kotlin.multiplatform.library " +
                "plugin is supported"
        canReadVariantSources -> null
        else ->
            "this Android module uses AGP's built-in Kotlin support and Fakt could not hook AGP's " +
                "variant API: put AGP on the same build classpath as Fakt, and apply Fakt in the " +
                "plugins { } block rather than after evaluation"
    }

/**
 * Pure decision for whether Fakt should route generated fakes into a `testFixtures` source set.
 *
 * Test-fixtures mode requires the opt-in flag AND a build that actually has a `testFixtures`
 * compilation to receive them — supplied either by the `java-test-fixtures` Gradle plugin (JVM) or
 * by the Android Gradle plugin's `android { testFixtures { enable = true } }`
 * (`com.android.library` / `com.android.application`). Extracted as a `Project`-free function so
 * the full truth table is unit-testable without a Gradle project (mirrors
 * [shouldWireGeneratedDir]).
 *
 * @param useGradleTestFixtures the resolved `fakt { useGradleTestFixtures }` value.
 * @param hasJavaTestFixtures whether the `java-test-fixtures` plugin is applied.
 * @param hasAndroidLibrary whether an Android library/application plugin is applied.
 */
internal fun shouldEnableTestFixtures(
    useGradleTestFixtures: Boolean,
    hasJavaTestFixtures: Boolean,
    hasAndroidLibrary: Boolean,
): Boolean = useGradleTestFixtures && (hasJavaTestFixtures || hasAndroidLibrary)

/**
 * Whether a routing decision leaves generation inside `compileKotlin*`, where the fakes are an
 * undeclared side effect and the task's cache entry is therefore incomplete (issue #142).
 *
 * Pure so the mapping is unit-testable without a Gradle project (mirrors [shouldWireGeneratedDir]).
 */
internal fun generatesFakesInProcess(decision: FaktGradleSubplugin.CacheCorrectDecision): Boolean =
    when (decision) {
        FaktGradleSubplugin.CacheCorrectDecision.LEGACY,
        FaktGradleSubplugin.CacheCorrectDecision.LEGACY_HYBRID -> true
        FaktGradleSubplugin.CacheCorrectDecision.REGISTER_PRODUCER,
        FaktGradleSubplugin.CacheCorrectDecision.REGISTER_CONSUMER,
        FaktGradleSubplugin.CacheCorrectDecision.REGISTER_SINGLE_TARGET,
        FaktGradleSubplugin.CacheCorrectDecision.SUPPRESS -> false
    }

/**
 * Warns once per project when an Android module routes fakes into `testFixtures` without the
 * experimental Gradle property that turns on Kotlin compilation for the Android `testFixtures`
 * source set. Without it AGP leaves that source set Java-only, no `*TestFixturesKotlin` task
 * materializes, and the generated Kotlin fakes are silently dropped.
 *
 * The property is needed whenever KGP's `org.jetbrains.kotlin.android` compiles the module — every
 * AGP 8.x module, and AGP 9 modules that opt out of built-in Kotlin
 * (`android.builtInKotlin=false`). Only AGP 9's built-in Kotlin compiles Kotlin test fixtures by
 * default, so without KGP applied this stays silent (see [shouldWarnAboutTestFixturesKotlinFlag]).
 * Read lazily via [org.gradle.api.provider.ProviderFactory.gradleProperty] to remain
 * configuration-cache safe.
 *
 * Top-level (not a member) so [FaktGradleSubplugin] stays under detekt's function-count threshold.
 */
private fun warnIfMissingAndroidTestFixturesKotlinFlag(project: Project) {
    val extras = project.extensions.extraProperties
    val property = FaktGradleSubplugin.ANDROID_TEST_FIXTURES_KOTLIN_PROPERTY
    val shouldWarn =
        shouldWarnAboutTestFixturesKotlinFlag(
            flagValue = project.providers.gradleProperty(property).orNull,
            hasKotlinAndroidPlugin = project.plugins.hasPlugin(KOTLIN_ANDROID_PLUGIN_ID),
        )
    if (shouldWarn && !extras.has(TEST_FIXTURES_FLAG_WARNED)) {
        extras.set(TEST_FIXTURES_FLAG_WARNED, true)
        project.logger.warn(
            "Fakt: Android test fixtures need Kotlin compilation for the testFixtures source set. " +
                "Add to gradle.properties:\n" +
                "  $property=true\n" +
                "(required whenever the Kotlin Android plugin compiles the module: every AGP 8.x " +
                "module, and AGP 9+ with android.builtInKotlin=false). Without it the generated " +
                "Kotlin fakes are not compiled into the testFixtures artifact."
        )
    }
}

/**
 * Whether [warnIfMissingAndroidTestFixturesKotlinFlag] should warn: the flag is not `true` and
 * KGP's Kotlin Android plugin compiles the module. AGP 9's built-in Kotlin (no KGP plugin) compiles
 * Kotlin test fixtures without the flag.
 */
internal fun shouldWarnAboutTestFixturesKotlinFlag(
    flagValue: String?,
    hasKotlinAndroidPlugin: Boolean,
): Boolean = hasKotlinAndroidPlugin && flagValue?.toBooleanStrictOrNull() != true

/** KGP's Android plugin; absent when AGP 9's built-in Kotlin compiles the module. */
private const val KOTLIN_ANDROID_PLUGIN_ID: String = "org.jetbrains.kotlin.android"

/** De-duplicates [warnIfMissingAndroidTestFixturesKotlinFlag]: it runs once per compilation. */
private const val TEST_FIXTURES_FLAG_WARNED: String = "fakt.testFixturesKotlinFlagWarned"

/** De-duplicates [warnNotCacheCorrect]: the decision is per compilation, the answer per module. */
private const val NOT_CACHE_CORRECT_WARNED: String = "fakt.notCacheCorrectWarned"

/**
 * Warns once per project that this module's fakes are not build-cache-correct, naming the reason.
 * Three of the four shapes that land here are not an opt-in, and the fallback used to be silent —
 * the first sign of trouble was an unresolved reference after a `clean` (issue #142).
 *
 * Top-level (not a member) so [FaktGradleSubplugin] stays under detekt's function-count threshold.
 */
private fun warnNotCacheCorrect(project: Project, reason: String) {
    if (!project.extensions.extraProperties.has(NOT_CACHE_CORRECT_WARNED)) {
        project.extensions.extraProperties.set(NOT_CACHE_CORRECT_WARNED, true)
        project.logger.warn(
            "Fakt: '${project.path}' generates fakes with the in-process compiler plugin " +
                "($reason), so they are not declared task outputs and this module's " +
                "compileKotlin* tasks opt out of the Gradle build cache. Generation is still " +
                "correct; only cache reuse is lost. See " +
                "https://rsicarelli.github.io/fakt/user-guide/plugin-configuration/#cache-correct-generation"
        )
    }
}

/**
 * Takes the compilation's own `compileKotlin*` out of the build cache: the in-process plugin writes
 * `Fake*Impl.kt` as a side effect no task declares, so after a `clean` a cache hit on unchanged
 * inputs skips generation and downstream compilation fails on missing fakes (issue #142).
 *
 * `cacheIf` leaves up-to-date checks alone, so only a `clean` — which deletes the fakes anyway —
 * re-runs the task. Nothing downstream needs the same treatment: `test` / `testFixtures`
 * compilations read the generated files as *source* and cache on their own fingerprints. Mirrors
 * [FakeCollectorTask]'s own `outputs.cacheIf` guard on the same path.
 *
 * Top-level (not a member) so [FaktGradleSubplugin] stays under detekt's function-count threshold.
 */
private fun refuseBuildCache(
    project: Project,
    kotlinCompilation: KotlinCompilation<*>,
    extension: FaktPluginExtension,
) {
    // A Provider, not a resolved Boolean: `applyToCompilation` can run before the `fakt { }` block
    // is evaluated, and the extension itself is not configuration-cache serializable.
    val faktEnabled: Provider<Boolean> = extension.enabled
    val compileTaskName = kotlinCompilation.compileKotlinTaskName
    project.tasks
        .matching { it.name == compileTaskName }
        .configureEach { task ->
            task.outputs.cacheIf(
                "Fakt generates fakes in-process during this task; they are not declared outputs"
            ) {
                // Fakt switched off writes nothing, so the task caches normally.
                !faktEnabled.get()
            }
        }
}

/**
 * Gradle plugin for Fakt compiler plugin integration.
 *
 * This is the main entry point that bridges Gradle build system with the Fakt compiler plugin. It
 * implements [KotlinCompilerPluginSupportPlugin] to hook into Kotlin's compilation lifecycle.
 *
 * ## Plugin Lifecycle
 *
 * ```
 * 1. apply(Project)
 *    └─> Creates `fakt { }` extension
 *    └─> Configures source sets (generator mode) OR registers tasks (collector mode)
 *    └─> Adds runtime dependencies to test configurations
 *
 * 2. isApplicable(KotlinCompilation)
 *    └─> Called for each compilation (main, test, jvmMain, etc.)
 *    └─> Returns true for main compilations only (where @Fake annotations exist)
 *    └─> Skips test compilations (generated code goes there, not analyzed)
 *
 * 3. applyToCompilation(KotlinCompilation)
 *    └─> Called for compilations where isApplicable returned true
 *    └─> Serializes configuration to compiler plugin options
 *    └─> Passes source set context (output directories, hierarchy, etc.)
 * ```
 *
 * ## Modes of Operation
 *
 * **Generator Mode (default):**
 *
 * ```kotlin
 * // build.gradle.kts
 * fakt {
 *     enabled.set(true)
 *     logLevel.set(LogLevel.INFO)
 * }
 * // Generates fakes from @Fake annotations in main source sets
 * ```
 *
 * **Collector Mode (experimental):**
 *
 * ```kotlin
 * // build.gradle.kts
 * fakt {
 *     collectFrom(project(":source-module"))
 * }
 * // Copies generated fakes from another module without compilation
 * ```
 *
 * ## Integration Points
 * - **Extension DSL**: [FaktPluginExtension] provides `fakt { }` block
 * - **Compiler Plugin**: Serializes options to Fakt compiler plugin
 * - **Source Sets**: [SourceSetConfigurator] adds generated directories to test source sets
 * - **Multi-Module**: [FakeCollectorTask] handles cross-module fake collection
 *
 * @see FaktPluginExtension
 * @see SourceSetDiscovery
 * @see FakeCollectorTask
 */
@Suppress("unused") // used by reflection
public class FaktGradleSubplugin : KotlinCompilerPluginSupportPlugin {
    public companion object {
        public const val PLUGIN_ID: String = "com.rsicarelli.fakt"
        public const val PLUGIN_ARTIFACT_NAME: String = "compiler"
        public const val PLUGIN_GROUP_ID: String = "com.rsicarelli.fakt"
        public const val PLUGIN_VERSION: String = "1.0.0-beta13"

        /**
         * `kotlin-compiler-embeddable` version pulled into the worker's isolated classpath. Pinned
         * to the Kotlin version Fakt was built and tested against. Users can override the
         * `faktWorker` configuration in their project if they need a different compiler version
         * (e.g. for a Kotlin EAP).
         */
        public const val FAKT_KOTLIN_VERSION: String = "2.4.10"

        internal const val WORKER_CONFIGURATION: String = "faktWorker"
        internal const val COMPILER_CLASSPATH_CONFIGURATION: String = "faktCompiler"
        internal const val GRADLE_PROPERTY_FLAG: String = "fakt.useExperimentalGenerateTask"

        /**
         * AGP experimental Gradle property that enables Kotlin compilation for the Android
         * `testFixtures` source set. Required whenever KGP's Kotlin Android plugin compiles the
         * module (AGP 8.x, or AGP 9+ with built-in Kotlin off); AGP 9's built-in Kotlin does not
         * need it.
         */
        internal const val ANDROID_TEST_FIXTURES_KOTLIN_PROPERTY: String =
            "android.experimental.enableTestFixturesKotlinSupport"

        /** KGP metadata compilation whose default source set is `commonMain`. */
        internal const val COMMON_MAIN_COMPILATION: String = "commonMain"

        /**
         * Sentinel substituted into [SourceSetContext.outputDirectory] /
         * [SourceSetContext.commonTestOutputDirectory] when the JSON is stored as a task `@Input` —
         * the worker overwrites these with absolute paths from file properties at execution time so
         * the cache key never carries machine-specific paths.
         */
        private const val OUTPUT_PLACEHOLDER: String = "fakt://generated"
    }

    /**
     * Creates the `fakt { }` extension and, after evaluation, routes the project: collector mode
     * registers [FakeCollectorTask]s for [FaktPluginExtension.collectFrom]; generator mode either
     * prepares the worker configurations for the default cache-correct path or, when it is opted
     * out of, wires the legacy source sets ([SourceSetConfigurator]) — per-compilation
     * [FaktGenerateTask] registration happens later in [applyToCompilation].
     */
    @OptIn(ExperimentalFaktMultiModule::class)
    override fun apply(target: Project) {
        // Create the fakt extension for configuration
        val extension = target.extensions.create("fakt", FaktPluginExtension::class.java)
        // AGP 9 built-in Kotlin keeps sources in its variant model; `onVariants` has to be hooked
        // before AGP finalises its variants, so this cannot wait for `afterEvaluate`.
        AndroidVariantSources.install(target)
        // JVM-only intermediate source sets: the late pass sees the final `dependsOn` edges.
        SyntheticIntermediateWiring.installLatePass(target, extension)

        // Determine mode after project evaluation
        target.afterEvaluate {
            val isCollectorMode = extension.collectFrom.isPresent

            if (isCollectorMode) {
                // COLLECTOR MODE: Collect fakes from another project
                val sourceProject = extension.collectFrom.get()
                target.logger.info(
                    "Fakt: Collector mode enabled - collecting fakes from ${sourceProject.name}"
                )

                // Register collector tasks (handles KMP automatically)
                FakeCollectorTask.registerForKmpProject(target, extension)
            } else {
                // GENERATOR MODE: Generate fakes from @Fake annotations
                target.logger.info("Fakt: Generator mode enabled - generating fakes")

                val useTestFixtures = resolveTestFixturesMode(target, extension)
                // `unreadableSourcesReason` gates the whole cache-correct branch, not just the
                // per-compilation routing below: a project without a readable Kotlin source-set
                // model falls back to the in-process plugin, and that path needs the legacy source
                // set wiring to compile the fakes it writes.
                if (
                    resolveExperimentalGenerateTaskFlag(target, extension) &&
                        unreadableSourcesReason(target) == null
                ) {
                    target.logger.info(
                        "Fakt: cache-correct generation enabled (the default) — registering " +
                            "FaktGenerateTask per compilation; the in-process compiler-plugin " +
                            "path stays disabled. Opt out with " +
                            "-Pfakt.useExperimentalGenerateTask=false."
                    )
                    ensureFaktConfigurations(target)
                    // Non-drivable KMP platform mains (Native) keep generating their own
                    // fakes via the in-process plugin; wire the platform *Test source sets so those
                    // generated fakes compile (the producer/consumer tasks wire their own dirs).
                    SourceSetConfigurator(target, useTestFixtures).configureKmpTestSourceSetDirs()
                } else {
                    val configurator = SourceSetConfigurator(target, useTestFixtures)
                    configurator.configureSourceSets()
                }
            }
        }

        target.logger.info("Fakt: Applied Gradle plugin to project ${target.name}")
    }

    /**
     * Resolves the cache-correct generate-task flag, with the Gradle property
     * `fakt.useExperimentalGenerateTask` taking precedence over the `fakt { }` extension. When the
     * property is set to a strict boolean it wins outright — so
     * `-Pfakt.useExperimentalGenerateTask=false` can turn the path off even when the build script
     * sets the extension to `true`. Otherwise the extension convention decides, and that convention
     * is `true`: the cache-correct path is the default and the legacy in-process path is the
     * opt-out.
     */
    private fun resolveExperimentalGenerateTaskFlag(
        project: Project,
        extension: FaktPluginExtension,
    ): Boolean {
        val property =
            project.providers.gradleProperty(GRADLE_PROPERTY_FLAG).orNull?.toBooleanStrictOrNull()
        return property ?: extension.useExperimentalGenerateTask.get()
    }

    /**
     * Idempotently creates the resolvable configurations that feed the worker classloader. Safe to
     * call from both `apply` / `afterEvaluate` and `applyToCompilation` because `maybeCreate(...)`
     * is no-op on the second call. Required at the call site that runs earliest:
     * `applyToCompilation` fires before `afterEvaluate`, so the configurations have to exist before
     * the task is registered.
     */
    internal fun ensureFaktConfigurations(target: Project) {
        target.configurations.maybeCreate(WORKER_CONFIGURATION).apply {
            isCanBeResolved = true
            isCanBeConsumed = false
            description = "Runtime classpath for Fakt's code-generation worker (K2JVMCompiler)."
        }
        target.configurations.maybeCreate(COMPILER_CLASSPATH_CONFIGURATION).apply {
            isCanBeResolved = true
            isCanBeConsumed = false
            description = "Fakt's :compiler shadowJar classpath, attached to K2 via -Xplugin."
        }
        target.dependencies.add(
            WORKER_CONFIGURATION,
            "org.jetbrains.kotlin:kotlin-compiler-embeddable:$FAKT_KOTLIN_VERSION",
        )
        target.dependencies.add(
            COMPILER_CLASSPATH_CONFIGURATION,
            "$PLUGIN_GROUP_ID:$PLUGIN_ARTIFACT_NAME:$PLUGIN_VERSION",
        )
    }

    /**
     * Resolves whether test fixtures mode should be active.
     *
     * Returns `true` only when `useGradleTestFixtures` is set AND the project has a source of a
     * `testFixtures` compilation:
     * - the `java-test-fixtures` Gradle plugin (JVM modules), or
     * - the Android Gradle plugin (`com.android.library` / `com.android.application`), where the
     *   `testFixtures` source set comes from `android { testFixtures { enable = true } }`.
     *
     * If the option is enabled but neither is present, emits a warning and returns `false`. When
     * the Android path is taken, additionally warns if the Kotlin-test-fixtures experimental Gradle
     * property is missing while KGP compiles the module (see
     * [warnIfMissingAndroidTestFixturesKotlinFlag]).
     */
    internal fun resolveTestFixturesMode(
        project: Project,
        extension: FaktPluginExtension,
    ): Boolean {
        val useGradleTestFixtures = extension.useGradleTestFixtures.get()
        val hasJavaTestFixtures = project.plugins.hasPlugin("java-test-fixtures")
        val hasAndroidLibrary =
            project.plugins.hasPlugin("com.android.library") ||
                project.plugins.hasPlugin("com.android.application")

        val enabled =
            shouldEnableTestFixtures(
                useGradleTestFixtures = useGradleTestFixtures,
                hasJavaTestFixtures = hasJavaTestFixtures,
                hasAndroidLibrary = hasAndroidLibrary,
            )

        if (!enabled) {
            // Warn only when the user opted in but no testFixtures source exists; the common
            // default (`useGradleTestFixtures = false`) stays silent.
            if (useGradleTestFixtures) {
                project.logger.warn(
                    "Fakt: useGradleTestFixtures is enabled but no testFixtures source is " +
                        "available. Apply ONE of:\n" +
                        "  • JVM: add the `java-test-fixtures` plugin\n" +
                        "      plugins { `java-test-fixtures` }\n" +
                        "  • Android: enable AGP test fixtures\n" +
                        "      android { testFixtures { enable = true } }\n" +
                        "Falling back to default 'test' source set."
                )
            }
            return false
        }

        project.logger.info(
            "Fakt: Test fixtures mode enabled - generating fakes to testFixtures source set"
        )
        if (hasAndroidLibrary) {
            warnIfMissingAndroidTestFixturesKotlinFlag(project)
        }
        return true
    }

    /**
     * Determines if Fakt compiler plugin should be applied to a specific compilation.
     *
     * This is called by Gradle for EVERY Kotlin compilation in the project (main, test, jvmMain,
     * jvmTest, commonMain, commonTest, etc.). We only want to analyze main compilations where
     * `@Fake` annotations are defined, NOT test compilations where generated fakes are used.
     *
     * ## Decision Logic
     *
     * **Skip if collector mode** (no compilation needed, just copy tasks):
     * ```kotlin
     * fakt { collectFrom(project(":source")) } → returns false
     * ```
     *
     * **Apply to main compilations only:**
     *
     * ```
     * Single-platform JVM:
     *   "main" → true  ✅
     *   "test" → false ❌
     *
     * KMP:
     *   "metadata" → true ✅ (commonMain representation)
     *   "commonMain" → true ✅
     *   "jvmMain" → true ✅
     *   "iosMain" → true ✅
     *   "commonTest" → false ❌
     *   "jvmTest" → false ❌
     * ```
     *
     * ## Why Skip Test Compilations?
     *
     * Test compilations don't contain `@Fake` annotations to process. They only USE the generated
     * fakes that were created from main source sets. Applying the plugin to test compilations
     * would:
     * - Waste compilation time
     * - Generate duplicate/empty output
     * - Cause circular dependencies
     *
     * @param kotlinCompilation The Kotlin compilation to check
     * @return `true` if plugin should be applied, `false` to skip this compilation
     * @see applyToCompilation
     */
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.project
        val extension = project.extensions.findByType(FaktPluginExtension::class.java)

        if (extension == null) return false

        if (extension.collectFrom.isPresent) {
            project.logger.info(
                "Fakt: Skipping compiler plugin for '${kotlinCompilation.name}' (collector mode)"
            )
            return false
        }

        // A module's testFixtures compilation (AGP: "debugTestFixtures"/"releaseTestFixtures",
        // JVM: "testFixtures") is NOT flagged `isTestCompilation`, yet it holds no @Fake sources to
        // analyze — it CONSUMES the generated fakes we route into it. Applying the plugin there
        // would run generation over the fixtures sources and derive a self-referential output dir
        // from that compilation's own default source set. Exclude it explicitly.
        if (kotlinCompilation.name.contains("testFixtures", ignoreCase = true)) {
            project.logger.info(
                "Fakt: Skipping compiler plugin for '${kotlinCompilation.name}' " +
                    "(testFixtures compilation consumes generated fakes, it does not produce them)"
            )
            return false
        }

        // Apply to all non-test compilations (covers JVM, KMP, and Android)
        // This handles:
        // - JVM: "main"
        // - KMP: "commonMain", "jvmMain", "iosMain", "metadata", etc.
        // - Android: "debug", "release", etc.
        return !kotlinCompilation.isTestCompilation
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = PLUGIN_GROUP_ID,
            artifactId = PLUGIN_ARTIFACT_NAME,
            version = PLUGIN_VERSION,
        )

    /**
     * Applies Fakt compiler plugin to a specific Kotlin compilation.
     *
     * This is called by Gradle for each compilation where [isApplicable] returned true. It
     * serializes all configuration and metadata into compiler plugin options that are passed to the
     * Fakt compiler plugin via command-line arguments.
     *
     * ## Serialization Strategy
     * 1. **Configuration Options**: Direct string/boolean values
     *     - `enabled`: true/false
     *     - `logLevel`: INFO/DEBUG/TRACE/QUIET
     * 2. **Source Set Context**: Base64-encoded JSON
     *     - Contains: compilation metadata, source set hierarchy, output directories
     *     - Serialized with kotlinx.serialization
     *     - Encoded to avoid special character issues in command-line arguments
     *
     * ## Example Compiler Options
     *
     * ```
     * -P plugin:com.rsicarelli.fakt:enabled=true
     * -P plugin:com.rsicarelli.fakt:logLevel=INFO
     * -P plugin:com.rsicarelli.fakt:sourceSetContext={hash}
     * -P plugin:com.rsicarelli.fakt:outputDir=/path/to/build/generated/fakt/test/kotlin
     * ```
     *
     * @param kotlinCompilation The Kotlin compilation to configure (e.g., jvmMain, commonMain)
     * @return A [Provider] of compiler plugin options, evaluated lazily at configuration time
     * @see SourceSetDiscovery.buildContext
     * @see FaktPluginExtension
     */
    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.project
        val extension = project.extensions.getByType(FaktPluginExtension::class.java)

        project.logger.info(
            "Fakt: Applying compiler plugin to compilation ${kotlinCompilation.name}"
        )

        val decision =
            if (resolveExperimentalGenerateTaskFlag(project, extension)) {
                cacheCorrectDecision(kotlinCompilation)
            } else {
                warnNotCacheCorrect(project, "$GRADLE_PROPERTY_FLAG is set to false")
                CacheCorrectDecision.LEGACY
            }

        // REGISTER_PRODUCER / REGISTER_CONSUMER / REGISTER_SINGLE_TARGET drive generation from a
        // FaktGenerateTask and
        // disable the in-process plugin. SUPPRESS (the legacy metadata `main` and shared-source-set
        // metadata compilations, which have no IR phase) also disables it. LEGACY_HYBRID keeps the
        // in-process plugin ON for a non-drivable platform main so it generates that platform's own
        // fakes the legacy way, but orders it after the common producer so it dedup-skips common
        // fakes instead of regenerating them. The first four return `enabled=false`; an empty list
        // isn't enough because FaktCompilerPluginRegistrar defaults `enabled` to true and would
        // explode when the sourceSetContext option is missing.
        when (decision) {
            CacheCorrectDecision.REGISTER_PRODUCER ->
                registerProducerFor(project, kotlinCompilation, extension)
            CacheCorrectDecision.REGISTER_CONSUMER -> {
                FaktGenerateTaskWiring.registerConsumer(project, kotlinCompilation, extension)
                SyntheticProducerWiring.registerIfRepresentative(
                    project,
                    kotlinCompilation,
                    extension,
                )
                SyntheticIntermediateWiring.registerEager(project, kotlinCompilation, extension)
            }
            CacheCorrectDecision.REGISTER_SINGLE_TARGET ->
                FaktGenerateTaskWiring.registerSingleTarget(project, kotlinCompilation, extension)
            CacheCorrectDecision.LEGACY_HYBRID ->
                FaktGenerateTaskWiring.wireLegacyHybridOrdering(project, kotlinCompilation)
            CacheCorrectDecision.SUPPRESS,
            CacheCorrectDecision.LEGACY -> Unit
        }

        // Issue #142: in-process generation is an undeclared side effect of the compile task, so
        // that task must not be stored in or restored from the build cache.
        if (generatesFakesInProcess(decision)) {
            refuseBuildCache(project, kotlinCompilation, extension)
        }

        return when (decision) {
            CacheCorrectDecision.REGISTER_PRODUCER,
            CacheCorrectDecision.REGISTER_CONSUMER,
            CacheCorrectDecision.REGISTER_SINGLE_TARGET,
            CacheCorrectDecision.SUPPRESS ->
                project.provider { listOf(SubpluginOption(key = "enabled", value = "false")) }
            CacheCorrectDecision.LEGACY_HYBRID,
            CacheCorrectDecision.LEGACY ->
                project.provider { legacyInProcessOptions(project, kotlinCompilation, extension) }
        }
    }

    /** How [applyToCompilation] should treat a compilation under the cache-correct flag. */
    internal enum class CacheCorrectDecision {
        /**
         * Drive a producer compilation from a `FaktGenerateTask`: `KotlinMetadataCompiler` over KMP
         * `commonMain` (+ ancestors), or `K2JVMCompiler` over a single-platform JVM `main`.
         */
        REGISTER_PRODUCER,
        /**
         * Drive a platform compiler (`K2JVMCompiler` for JVM/Android, `K2JSCompiler` for JS/Wasm)
         * over a drivable platform main's own sources from a `FaktGenerateTask` (source-partitioned
         * consumer); ancestor sources ride along for analysis only.
         */
        REGISTER_CONSUMER,
        /**
         * Drive the lone platform main of a single-target KMP project (JVM, JS or Wasm) from one
         * `FaktGenerateTask` that owns both halves: its own source set's fakes go to the platform
         * test source set and the common fragment's fakes to `commonTest` (issue #153).
         */
        REGISTER_SINGLE_TARGET,
        /**
         * Keep the in-process plugin ON for a non-drivable platform main (Native) so it generates
         * that platform's fakes the legacy way, ordered after the common producer.
         */
        LEGACY_HYBRID,
        /** Suppress the in-process plugin; another task already owns this compilation's fakes. */
        SUPPRESS,
        /** Cache-correct path can't own this compilation; use the in-process plugin. */
        LEGACY,
    }

    /**
     * The cache-correct worker drives a compiler front door reflectively. A single-platform JVM
     * project owns its `main` compilation outright (producer, `K2JVMCompiler`). A KMP project —
     * with or without a JVM/Android target, since `KotlinMetadataCompiler` needs only the
     * compilation's own metadata-klib dependencies — drives the metadata compiler once over
     * `commonMain` to produce platform-agnostic common fakes (producer), which every target's test
     * compilation reuses; a drivable platform main (`jvmMain` / `androidMain` / `jsMain` /
     * `wasmJsMain`) gets its own source-partitioned consumer task. The remaining `common`-platform
     * metadata compilations are suppressed.
     *
     * A Native platform main (`iosMain` / `nativeMain` / `linuxX64Main`) cannot be driven from a
     * task yet (`K2NativeCompiler` is not on the embeddable classpath, issue #152), so it stays on
     * the in-process plugin ([CacheCorrectDecision.LEGACY_HYBRID]): its platform-specific fakes are
     * generated (not cache-correct), while the common producer still owns the cache-correct common
     * fakes.
     *
     * A single-target KMP project has no per-source-set `commonMain` compilation at all (see
     * [singleTargetPlatformTypeName]): a JVM/JS/Wasm lone target owns both the common and the
     * platform fakes from one task ([CacheCorrectDecision.REGISTER_SINGLE_TARGET]), while an
     * Android or Native lone target stays on the in-process plugin. An Android project on AGP's
     * built-in Kotlin exposes empty Kotlin source sets; its producers read AGP's variant API
     * instead, and only when that API is not visible to Fakt does it stay on the in-process plugin
     * (see [unreadableSourcesReason]).
     */
    private fun cacheCorrectDecision(
        kotlinCompilation: KotlinCompilation<*>
    ): CacheCorrectDecision {
        val project = kotlinCompilation.project
        val kmp =
            project.extensions.findByType(
                org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java
            )
        val route =
            routeCompilation(
                unreadableSourcesReason = unreadableSourcesReason(project),
                isMultiplatform = kmp != null,
                singleTargetPlatformTypeName = kmp?.let(::singleTargetPlatformTypeName),
                compilation =
                    RoutedCompilation(
                        name = kotlinCompilation.name,
                        platformTypeName = kotlinCompilation.target.platformType.name,
                        sharedSourceSetOwner = sharedSourceSetOwner(kmp, kotlinCompilation),
                    ),
            )
        route.notCacheCorrectReason?.let { reason -> warnNotCacheCorrect(project, reason) }
        return route.decision
    }

    /** Original in-process subplugin option payload, untouched. */
    private fun legacyInProcessOptions(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
    ): List<SubpluginOption> = buildList {
        add(SubpluginOption(key = "enabled", value = extension.enabled.get().toString()))
        add(SubpluginOption(key = "logLevel", value = extension.logLevel.get().name))
        add(
            SubpluginOption(
                key = "enableCallHistory",
                value = extension.enableCallHistory.get().toString(),
            )
        )
        add(
            SubpluginOption(
                key = "enableMutableFakes",
                value = extension.enableMutableFakes.get().toString(),
            )
        )

        val buildDir = project.layout.buildDirectory.get().asFile.absolutePath
        val useTestFixtures = resolveTestFixturesMode(project, extension)
        val context = SourceSetDiscovery.buildContext(kotlinCompilation, buildDir, useTestFixtures)

        val json = Json { prettyPrint = false }
        val jsonString = json.encodeToString(context)
        val base64Encoded = Base64.getEncoder().encodeToString(jsonString.toByteArray())
        add(SubpluginOption(key = "sourceSetContext", value = base64Encoded))
        add(SubpluginOption(key = "outputDir", value = context.outputDirectory))
    }
}
