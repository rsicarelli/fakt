// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalFaktMultiModule::class)

package com.rsicarelli.fakt.gradle

import java.util.Base64
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Platforms the cache-correct worker can drive in-process. `K2JVMCompiler` ships in
 * `kotlin-compiler-embeddable`; `K2NativeCompiler` does not, so only JVM and Android JVM targets
 * are drivable today.
 */
private fun isDrivablePlatform(platformTypeName: String): Boolean =
    when (platformTypeName.lowercase()) {
        "jvm",
        "androidjvm" -> true
        else -> false
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
 * Warns when an Android module routes fakes into `testFixtures` without the experimental Gradle
 * property that turns on Kotlin compilation for the Android `testFixtures` source set. Without it
 * AGP 8.x leaves that source set Java-only, no `*TestFixturesKotlin` task materializes, and the
 * generated Kotlin fakes are silently dropped. The property is unnecessary on AGP 9.0 (Kotlin test
 * fixtures are on by default), so this stays a warning, not an error. Read lazily via
 * [org.gradle.api.provider.ProviderFactory.gradleProperty] to remain configuration-cache safe.
 *
 * Top-level (not a member) so [FaktGradleSubplugin] stays under detekt's function-count threshold.
 */
private fun warnIfMissingAndroidTestFixturesKotlinFlag(project: Project) {
    val property = FaktGradleSubplugin.ANDROID_TEST_FIXTURES_KOTLIN_PROPERTY
    val flag = project.providers.gradleProperty(property).orNull
    if (flag?.toBooleanStrictOrNull() != true) {
        project.logger.warn(
            "Fakt: Android test fixtures need Kotlin compilation for the testFixtures source set. " +
                "Add to gradle.properties:\n" +
                "  $property=true\n" +
                "(required on AGP 8.x; unnecessary on AGP 9.0+, where it is the default). Without " +
                "it the generated Kotlin fakes are not compiled into the testFixtures artifact."
        )
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
        public const val PLUGIN_VERSION: String = "1.0.0-beta09"

        /**
         * `kotlin-compiler-embeddable` version pulled into the worker's isolated classpath. Pinned
         * to the Kotlin version Fakt was built and tested against. Users can override the
         * `faktWorker` configuration in their project if they need a different compiler version
         * (e.g. for a Kotlin EAP).
         */
        public const val FAKT_KOTLIN_VERSION: String = "2.3.20"

        internal const val WORKER_CONFIGURATION: String = "faktWorker"
        internal const val COMPILER_CLASSPATH_CONFIGURATION: String = "faktCompiler"
        internal const val GRADLE_PROPERTY_FLAG: String = "fakt.useExperimentalGenerateTask"

        /**
         * AGP experimental Gradle property that enables Kotlin compilation for the Android
         * `testFixtures` source set. Required on AGP 8.x; the default on AGP 9.0+.
         */
        internal const val ANDROID_TEST_FIXTURES_KOTLIN_PROPERTY: String =
            "android.experimental.enableTestFixturesKotlinSupport"

        /** KGP metadata compilation whose default source set is `commonMain`. */
        private const val COMMON_MAIN_COMPILATION: String = "commonMain"

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
     * wires legacy source sets ([SourceSetConfigurator]) or, with the experimental flag on,
     * prepares the worker configurations — per-compilation [FaktGenerateTask] registration happens
     * later in [applyToCompilation].
     */
    @OptIn(ExperimentalFaktMultiModule::class)
    override fun apply(target: Project) {
        // Create the fakt extension for configuration
        val extension = target.extensions.create("fakt", FaktPluginExtension::class.java)

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
                if (resolveExperimentalGenerateTaskFlag(target, extension)) {
                    target.logger.info(
                        "Fakt: useExperimentalGenerateTask=true — registering FaktGenerateTask " +
                            "per compilation; the in-process compiler-plugin path stays disabled."
                    )
                    ensureFaktConfigurations(target)
                    // Non-drivable KMP platform mains (Native/JS/Wasm) keep generating their own
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
     * Resolves the experimental flag, with the Gradle property `fakt.useExperimentalGenerateTask`
     * taking precedence over the `fakt { }` extension. When the property is set to a strict boolean
     * it wins outright — so `-Pfakt.useExperimentalGenerateTask=false` can turn the path off even
     * when the build script sets the extension to `true`. Otherwise the extension convention
     * decides.
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
     * property is missing (required on AGP 8.x; unnecessary on AGP 9.0).
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
                CacheCorrectDecision.LEGACY
            }

        // REGISTER_PRODUCER / REGISTER_CONSUMER drive generation from a FaktGenerateTask and
        // disable the in-process plugin. SUPPRESS (the legacy metadata `main` and shared-source-set
        // metadata compilations, which have no IR phase) also disables it. LEGACY_HYBRID keeps the
        // in-process plugin ON for a non-drivable platform main so it generates that platform's own
        // fakes the legacy way, but orders it after the common producer so it dedup-skips common
        // fakes instead of regenerating them. The first three return `enabled=false`; an empty list
        // isn't enough because FaktCompilerPluginRegistrar defaults `enabled` to true and would
        // explode when the sourceSetContext option is missing.
        when (decision) {
            CacheCorrectDecision.REGISTER_PRODUCER ->
                FaktGenerateTaskWiring.registerProducer(project, kotlinCompilation, extension)
            CacheCorrectDecision.REGISTER_CONSUMER ->
                FaktGenerateTaskWiring.registerConsumer(project, kotlinCompilation, extension)
            CacheCorrectDecision.LEGACY_HYBRID ->
                FaktGenerateTaskWiring.wireLegacyHybridOrdering(project, kotlinCompilation)
            CacheCorrectDecision.SUPPRESS,
            CacheCorrectDecision.LEGACY -> Unit
        }

        return when (decision) {
            CacheCorrectDecision.REGISTER_PRODUCER,
            CacheCorrectDecision.REGISTER_CONSUMER,
            CacheCorrectDecision.SUPPRESS ->
                project.provider { listOf(SubpluginOption(key = "enabled", value = "false")) }
            CacheCorrectDecision.LEGACY_HYBRID,
            CacheCorrectDecision.LEGACY ->
                project.provider { legacyInProcessOptions(project, kotlinCompilation, extension) }
        }
    }

    /** How [applyToCompilation] should treat a compilation under the cache-correct flag. */
    private enum class CacheCorrectDecision {
        /**
         * Drive a producer compilation from a `FaktGenerateTask`: `KotlinMetadataCompiler` over KMP
         * `commonMain` (+ ancestors), or `K2JVMCompiler` over a single-platform JVM `main`.
         */
        REGISTER_PRODUCER,
        /**
         * Drive `K2JVMCompiler` over a drivable platform main's own sources from a
         * `FaktGenerateTask` (source-partitioned consumer); `commonMain` arrives as a classpath
         * dependency.
         */
        REGISTER_CONSUMER,
        /**
         * Keep the in-process plugin ON for a non-drivable platform main (Native/JS/Wasm) so it
         * generates that platform's fakes the legacy way, ordered after the common producer.
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
     * compilation reuses; a drivable platform main (`jvmMain` / `androidMain`) gets its own
     * source-partitioned consumer task. The remaining `common`-platform metadata compilations are
     * suppressed.
     *
     * A non-drivable platform main (`iosMain` / `nativeMain` / `jsMain` / `wasmJsMain`) cannot be
     * driven in-process (`K2NativeCompiler` is not on the embeddable classpath), so it stays on the
     * in-process plugin ([CacheCorrectDecision.LEGACY_HYBRID]): its platform-specific fakes are
     * generated (not cache-correct), while the common producer still owns the cache-correct common
     * fakes.
     */
    private fun cacheCorrectDecision(
        kotlinCompilation: KotlinCompilation<*>
    ): CacheCorrectDecision {
        val kmp =
            kotlinCompilation.project.extensions.findByType(
                org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java
            )
        val platformType = kotlinCompilation.target.platformType.name

        // `commonMain` is checked before the `common` platform-type guard because the metadata
        // target exposes both the per-source-set `commonMain` compilation (the producer) and a
        // legacy `main` compilation plus shared-source-set metadata compilations (all platformType
        // `common`, no IR phase) — those must be suppressed, not turned into a second producer.
        return when {
            kmp == null ->
                if (isDrivablePlatform(platformType)) CacheCorrectDecision.REGISTER_PRODUCER
                else CacheCorrectDecision.LEGACY
            kotlinCompilation.name == COMMON_MAIN_COMPILATION ->
                CacheCorrectDecision.REGISTER_PRODUCER
            platformType.equals("common", ignoreCase = true) -> CacheCorrectDecision.SUPPRESS
            isDrivablePlatform(platformType) -> CacheCorrectDecision.REGISTER_CONSUMER
            else -> CacheCorrectDecision.LEGACY_HYBRID
        }
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
