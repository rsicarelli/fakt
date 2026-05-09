// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.gradle.worker.FaktCodegenWorkAction
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor

/**
 * Generates Fakt's `Fake<X>Impl.kt` files into a declared `@OutputDirectory` from outside
 * `compileKotlin*`. Solves issue #79: when Gradle's build cache restores `compileKotlin*`, the
 * generated `.kt` files come back too because they're now real task outputs rather than side-effect
 * writes.
 *
 * The task hosts `kotlin-compiler-embeddable` in an isolated [WorkerExecutor.classLoaderIsolation]
 * worker so the daemon doesn't carry the compiler classpath and so static state inside the FIR
 * pipeline can't leak across invocations. The reference architecture is KSP2's `KspAATask`
 * (research artifact 1, §"KSP2 / `KspAATask`").
 *
 * Caching contract:
 * - Sources: [sources] is `@PathSensitive(RELATIVE)` — Kotlin file paths encode package, so
 *   relative is required for cross-machine cache hits (research artifact 2, §2).
 * - Classpaths use `@Classpath` / `@CompileClasspath` so ABI changes invalidate but trivial
 *   manifest edits don't.
 * - Outputs are split: [generatedKotlinDir] holds the `.kt` files (downstream `compileKotlin*`
 *   consumes via `kotlin.srcDir(taskProvider)` from PR 3 onward); [firMetadataFile] is the
 *   producer-mode KMP metadata cache, declared as a single `@OutputFile` rather than a directory so
 *   it can't overlap with platform-task outputs (research artifact 2, §6).
 * - [scratchDir] is `@LocalState` so a build-cache restore wipes it instead of replaying stale
 *   contents (KSP issue #2042 lesson).
 *
 * Wiring this task into `compileKotlin*` is deliberately deferred to PR 3; this PR just defines the
 * task and exercises it through Gradle TestKit.
 */
@CacheableTask
public abstract class FaktGenerateTask @Inject constructor(private val workers: WorkerExecutor) :
    DefaultTask() {

    /** Kotlin source files (or directories) that may carry `@Fake`-annotated declarations. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    public abstract val sources: ConfigurableFileCollection

    /** Compile classpath the FIR + IR pipeline analyses against. */
    @get:CompileClasspath public abstract val compileClasspath: ConfigurableFileCollection

    /**
     * Classpath for the Fakt worker — `kotlin-compiler-embeddable` (the `K2JVMCompiler` driver).
     * Held isolated from the Gradle daemon to avoid clashes with KGP's bundled compiler (research
     * artifact 1, R2).
     */
    @get:Classpath public abstract val faktWorkerClasspath: ConfigurableFileCollection

    /**
     * Fakt's published `:compiler` plugin jar(s). Loaded into the K2 invocation via `-Xplugin` so
     * the existing `FaktCompilerPluginRegistrar` runs unchanged — same FIR + IR pipeline KGP uses
     * today, just hosted by this task instead of `compileKotlin*`. Declared as a separate input so
     * a Fakt version bump invalidates cached outputs even if user sources don't change.
     */
    @get:Classpath public abstract val faktCompilerClasspath: ConfigurableFileCollection

    /**
     * Source-set descriptor as the Gradle plugin produces it. Serialized to JSON because
     * `SourceSetContext` doesn't carry Gradle `@Input` annotations on its individual fields, so
     * `@Nested` would reject it; JSON gives Gradle a deterministic string to hash for cache
     * equality.
     */
    @get:Input public abstract val sourceSetContextJson: Property<String>

    /**
     * Fakt version baked into the cache key. Bumping Fakt itself invalidates cached outputs even
     * when source inputs are unchanged.
     */
    @get:Input public abstract val faktVersion: Property<String>

    /** Worker log verbosity. */
    @get:Input public abstract val logLevel: Property<LogLevel>

    /** Imports forced into every generated file (e.g. user-extension imports). */
    @get:Input public abstract val imports: ListProperty<String>

    /**
     * KMP consumer-mode input: serialized `FirMetadataCache` produced by an upstream `commonMain`
     * task. Wired in PR 3 once per-target tasks are registered.
     *
     * `PathSensitivity.NONE` because only the file's contents matter — its absolute path on the
     * producer host is irrelevant for cache equality.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    public abstract val commonFirMetadata: RegularFileProperty

    /**
     * Generated `Fake<X>Impl.kt` files. Convention is
     * `build/generated/fakt/<target>/<sourceSet>/kotlin` — disjoint from `compileKotlin*`'s outputs
     * to avoid Gradle's overlapping-outputs rule (research artifact 2, §6).
     */
    @get:OutputDirectory public abstract val generatedKotlinDir: DirectoryProperty

    /**
     * KMP producer-mode output: serialized `FirMetadataCache` written when this task represents the
     * metadata compilation. A single file (not a directory) so consumer tasks can read it via
     * `@InputFile` without overlapping outputs.
     */
    @get:OutputFile @get:Optional public abstract val firMetadataFile: RegularFileProperty

    /** Internal scratch state — cleared on cache restore (KSP #2042 lesson). */
    @get:LocalState public abstract val scratchDir: DirectoryProperty

    @TaskAction
    public fun generate() {
        val queue =
            workers.classLoaderIsolation { spec -> spec.classpath.from(faktWorkerClasspath) }
        queue.submit(FaktCodegenWorkAction::class.java) { params ->
            params.sources.from(sources)
            params.compileClasspath.from(compileClasspath)
            params.faktCompilerClasspath.from(faktCompilerClasspath)
            params.sourceSetContextJson.set(sourceSetContextJson)
            params.faktVersion.set(faktVersion)
            params.logLevel.set(logLevel)
            params.imports.set(imports)
            params.commonFirMetadata.set(commonFirMetadata)
            params.generatedKotlinDir.set(generatedKotlinDir)
            params.firMetadataFile.set(firMetadataFile)
            params.scratchDir.set(scratchDir)
        }
    }
}
