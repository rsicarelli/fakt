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
        public const val PLUGIN_VERSION: String = "1.0.0-beta08"

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
                } else {
                    val configurator = SourceSetConfigurator(target, useTestFixtures)
                    configurator.configureSourceSets()
                }
            }
        }

        target.logger.info("Fakt: Applied Gradle plugin to project ${target.name}")
    }

    /**
     * Reads the experimental flag from (in priority order) the `fakt { }` extension and the Gradle
     * property `fakt.useExperimentalGenerateTask`. Property gives end-users an opt-in switch
     * without having to touch the build script — useful for trying the cache-correct path on a
     * single CI run.
     */
    private fun resolveExperimentalGenerateTaskFlag(
        project: Project,
        extension: FaktPluginExtension,
    ): Boolean {
        if (extension.useExperimentalGenerateTask.get()) return true
        return project.providers
            .gradleProperty(GRADLE_PROPERTY_FLAG)
            .orNull
            ?.toBooleanStrictOrNull() ?: false
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
     * Returns `true` only when both conditions are met:
     * 1. `useGradleTestFixtures` is set to `true` in the extension
     * 2. The `java-test-fixtures` Gradle plugin is applied to the project
     *
     * If the option is enabled but the plugin is missing, emits a warning and returns `false`.
     */
    private fun resolveTestFixturesMode(project: Project, extension: FaktPluginExtension): Boolean {
        if (!extension.useGradleTestFixtures.get()) return false

        val hasTestFixturesPlugin = project.plugins.hasPlugin("java-test-fixtures")
        if (!hasTestFixturesPlugin) {
            project.logger.warn(
                "Fakt: useGradleTestFixtures is enabled but the 'java-test-fixtures' plugin " +
                    "is not applied. Add `java-test-fixtures` to your plugins block:\n" +
                    "  plugins {\n" +
                    "      `java-test-fixtures`\n" +
                    "  }\n" +
                    "Falling back to default 'test' source set."
            )
        } else {
            project.logger.info(
                "Fakt: Test fixtures mode enabled - generating fakes to testFixtures source set"
            )
        }

        return hasTestFixturesPlugin
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

        // REGISTER_PRODUCER drives generation from a FaktGenerateTask and disables the in-process
        // plugin. DISABLE_IN_PROCESS (KMP platform mains) also disables it — the common producer
        // already owns those fakes, and generating them again would surface as duplicate `.kt` /
        // Redeclaration in shared test sets. Both return `enabled=false`; an empty list isn't
        // enough
        // because FaktCompilerPluginRegistrar defaults `enabled` to true and would explode when the
        // sourceSetContext option is missing.
        if (decision == CacheCorrectDecision.REGISTER_PRODUCER) {
            FaktGenerateTaskWiring.register(project, kotlinCompilation, extension)
        }
        if (decision != CacheCorrectDecision.LEGACY) {
            return project.provider { listOf(SubpluginOption(key = "enabled", value = "false")) }
        }

        return project.provider { legacyInProcessOptions(project, kotlinCompilation, extension) }
    }

    /** How [applyToCompilation] should treat a compilation under the cache-correct flag. */
    private enum class CacheCorrectDecision {
        /** Drive `K2JVMCompiler` from a `FaktGenerateTask` for this compilation. */
        REGISTER_PRODUCER,
        /** Suppress the in-process plugin; another task already owns this compilation's fakes. */
        DISABLE_IN_PROCESS,
        /** Cache-correct path can't own this compilation; use the in-process plugin. */
        LEGACY,
    }

    /**
     * The cache-correct worker drives `K2JVMCompiler` reflectively. For a single-platform JVM
     * project it owns the `main` compilation outright. For a KMP project it drives the JVM compiler
     * once over `commonMain` (multiplatform mode) to produce platform-agnostic common fakes; every
     * target's test compilation then consumes that one generated directory as ordinary source, so
     * the Native/JS compilers never run Fakt. Platform `*Main` compilations therefore only need the
     * in-process plugin suppressed — their common fakes already exist.
     *
     * Native/JS/Wasm cannot be driven in-process (`K2NativeCompiler` is not on the embeddable
     * classpath), so a `@Fake` declared directly in a platform main source set is not yet handled
     * on this path; that case stays a documented limitation.
     */
    private fun cacheCorrectDecision(
        kotlinCompilation: KotlinCompilation<*>
    ): CacheCorrectDecision {
        val kmp =
            kotlinCompilation.project.extensions.findByType(
                org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java
            )

        // The metadata target exposes two compilations over the same `commonMain` sources: the
        // per-source-set `commonMain` compilation and a legacy `main` compilation. Both report
        // `defaultSourceSet == commonMain`, so match on the compilation name to register exactly
        // one
        // producer — registering both would wire duplicate generated `.kt` into `commonTest` and
        // fail with `Redeclaration`. The common producer drives K2JVMCompiler over `commonMain`, so
        // it needs a JVM-resolvable classpath; without a JVM/Android target the whole project stays
        // on legacy. Every other KMP compilation (legacy metadata `main`, platform `*Main`) only
        // needs the in-process plugin suppressed — the common producer already owns those fakes.
        return when {
            kmp == null ->
                if (isDrivablePlatform(kotlinCompilation.target.platformType.name)) {
                    CacheCorrectDecision.REGISTER_PRODUCER
                } else {
                    CacheCorrectDecision.LEGACY
                }
            kmp.targets.none { isDrivablePlatform(it.platformType.name) } ->
                CacheCorrectDecision.LEGACY
            kotlinCompilation.name == COMMON_MAIN_COMPILATION ->
                CacheCorrectDecision.REGISTER_PRODUCER
            else -> CacheCorrectDecision.DISABLE_IN_PROCESS
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
