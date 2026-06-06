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

        /**
         * Sentinel substituted into [SourceSetContext.outputDirectory] /
         * [SourceSetContext.commonTestOutputDirectory] when the JSON is stored as a task `@Input` —
         * the worker overwrites these with absolute paths from file properties at execution time so
         * the cache key never carries machine-specific paths.
         */
        private const val OUTPUT_PLACEHOLDER: String = "fakt://generated"
    }

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

                val isKmp =
                    target.extensions.findByType(
                        org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java
                    ) != null
                val useTestFixtures = resolveTestFixturesMode(target, extension)
                if (resolveExperimentalGenerateTaskFlag(target, extension) && !isKmp) {
                    target.logger.info(
                        "Fakt: useExperimentalGenerateTask=true — registering FaktGenerateTask " +
                            "per compilation; the in-process compiler-plugin path stays disabled."
                    )
                    ensureFaktConfigurations(target)
                } else {
                    // Legacy in-process path. Also taken when the flag is on but the project is
                    // KMP — the cache-correct worker only drives K2JVMCompiler, which can't
                    // compile metadata/native, so KMP falls through to the legacy wiring
                    // unchanged.
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

        if (
            resolveExperimentalGenerateTaskFlag(project, extension) &&
                isCacheCorrectPathSupported(kotlinCompilation)
        ) {
            // Side-effect: register FaktGenerateTask + wire its @OutputDirectory into the
            // matching test source set. Returns `enabled=false` so the in-process compiler
            // plugin (still loaded by KGP into compileKotlin*) skips registration — generation
            // happens in our task instead. Empty list isn't enough: FaktCompilerPluginRegistrar
            // defaults `enabled` to true and would explode when sourceSetContext is missing.
            FaktGenerateTaskWiring.register(project, kotlinCompilation, extension)
            return project.provider { listOf(SubpluginOption(key = "enabled", value = "false")) }
        }

        return project.provider { legacyInProcessOptions(project, kotlinCompilation, extension) }
    }

    /**
     * The cache-correct worker drives `K2JVMCompiler` reflectively, so it can only analyse JVM
     * bytecode-producing compilations. Two further constraints narrow the scope:
     * 1. **Platform.** KMP metadata, Native, JS and Wasm compilations need their own K2 drivers
     *    (`K2MetadataCompiler` et al.) plus matching common/native stdlib classpaths, which the
     *    worker does not provide.
     * 2. **KMP source-set inheritance.** Even on a JVM target inside a KMP project, the `jvmMain`
     *    compilation's analysis source set is `jvmMain + commonMain` (KGP attaches parent source
     *    sets). The task would generate fakes for `commonMain` `@Fake` interfaces too, conflicting
     *    with the legacy in-process path that handles `compileKotlinMetadata`. Disabling the legacy
     *    path entirely under the flag isn't safe yet (no `K2MetadataCompiler` producer) — so for
     *    now KMP projects stay on legacy across the board.
     *
     * Pure JVM Gradle projects (no `KotlinMultiplatformExtension`) take the cache-correct path.
     */
    private fun isCacheCorrectPathSupported(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val platform = kotlinCompilation.target.platformType.name.lowercase()
        if (platform != "jvm" && platform != "androidjvm") return false
        return kotlinCompilation.project.extensions.findByType(
            org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java
        ) == null
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
