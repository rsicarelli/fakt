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

/** Heap ceiling for the forked codegen worker JVM. Generous enough for K2 over a large module. */
private const val WORKER_MAX_HEAP: String = "1g"

/** Metaspace ceiling for the forked worker JVM — `kotlin-compiler-embeddable` is class-heavy. */
private const val WORKER_METASPACE_ARG: String = "-XX:MaxMetaspaceSize=512m"

/**
 * Generates Fakt's `Fake<X>Impl.kt` files into a declared `@OutputDirectory` from outside
 * `compileKotlin*`. Solves issue #79: when Gradle's build cache restores `compileKotlin*`, the
 * generated `.kt` files come back too because they're now real task outputs rather than side-effect
 * writes.
 *
 * The task hosts `kotlin-compiler-embeddable` in an isolated [WorkerExecutor.processIsolation]
 * worker so the daemon doesn't carry the compiler classpath, so static state inside the FIR
 * pipeline can't leak across invocations, and so the compiler's metaspace footprint stays in a
 * forked JVM that many concurrent producer tasks don't share. The reference architecture is KSP2's
 * `KspAATask`.
 *
 * Caching semantics (path sensitivity, classpath normalization, output split, local state) are
 * documented on each annotated property. For KMP, the task runs in producer mode (writes
 * [firMetadataFile]) or consumer mode (reads [commonFirMetadata]) — see those properties.
 */
@CacheableTask
public abstract class FaktGenerateTask @Inject constructor(private val workers: WorkerExecutor) :
    DefaultTask() {

    /**
     * Kotlin source files (or directories) that may carry `@Fake`-annotated declarations.
     *
     * `@PathSensitive(RELATIVE)` because Kotlin file paths encode the package; relative sensitivity
     * keeps cache keys stable across machines and checkout locations.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    public abstract val sources: ConfigurableFileCollection

    /**
     * Compile classpath the FIR + IR pipeline analyses against. `@CompileClasspath` normalization
     * means ABI changes in dependencies invalidate cached fakes while implementation-only changes
     * don't.
     */
    @get:CompileClasspath public abstract val compileClasspath: ConfigurableFileCollection

    /**
     * Ancestor sources (commonMain and intermediate source sets) fed to a source-partitioned
     * consumer for **analysis only**: they let the K2JVM frontend pair `actual` declarations with
     * their `expect`s (via `-Xcommon-sources`) and resolve common types referenced from platform
     * `@Fake` signatures. Their own `@Fake` declarations never emit here — the common producer owns
     * those (`SourceSetContext.emitSourceSets` restricts the FIR emitter). Empty for producers.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    public abstract val analysisOnlySources: ConfigurableFileCollection

    /**
     * Metadata-klib dependencies for common (`commonMain`) producers — the compilation's own
     * `compileDependencyFiles` (stdlib and library klibs the `KotlinMetadataCompiler` driver
     * reads).
     *
     * Deliberately `@Classpath`, not `@CompileClasspath`: the compile-classpath normalizer
     * fingerprints `.class` entries and can treat a klib archive (which has none) as effectively
     * empty, so klib content changes would never invalidate the producer. `@Classpath` hashes the
     * actual content, closing that missed-invalidation hole. Empty for non-common compilations.
     */
    @get:Classpath public abstract val commonKlibClasspath: ConfigurableFileCollection

    /**
     * Classpath for the Fakt worker — `kotlin-compiler-embeddable` (the `K2JVMCompiler` driver).
     * Held isolated from the Gradle daemon to avoid clashes with KGP's bundled compiler.
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

    @get:Input public abstract val logLevel: Property<LogLevel>

    /**
     * Extension-level default for call-history generation (`fakt { enableCallHistory }`). Forwarded
     * to the compiler plugin so the worker path produces the same fakes as the legacy in-process
     * path. `@Input` so a change to the default invalidates cached outputs; `@Optional` because the
     * compiler falls back to its own default (`true`) when the option is absent.
     */
    @get:Input @get:Optional public abstract val enableCallHistory: Property<Boolean>

    /**
     * Extension-level default for mutable-fake generation (`fakt { enableMutableFakes }`).
     * Forwarded to the compiler plugin so the worker path produces the same fakes as the legacy
     * in-process path. `@Input` so a change to the default invalidates cached outputs; `@Optional`
     * because the compiler falls back to its own default (`false`) when the option is absent.
     */
    @get:Input @get:Optional public abstract val enableMutableFakes: Property<Boolean>

    /** Imports forced into every generated file (e.g. user-extension imports). */
    @get:Input public abstract val imports: ListProperty<String>

    /**
     * KMP consumer-mode input: serialized `FirMetadataCache` produced by the metadata compilation's
     * [firMetadataFile]. Its presence is what switches the worker to consumer mode — common `@Fake`
     * declarations are read from the cache instead of being re-validated.
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
     * to avoid Gradle's overlapping-outputs rule.
     */
    @get:OutputDirectory public abstract val generatedKotlinDir: DirectoryProperty

    /**
     * KMP producer-mode output: serialized `FirMetadataCache` written when this task represents the
     * metadata (`commonMain`) compilation — [FaktGenerateTaskWiring] sets it for metadata-like
     * compilations only. Platform tasks consume it through [commonFirMetadata]. A single file (not
     * a directory) so consumers can declare it `@InputFile` without overlapping outputs.
     */
    @get:OutputFile @get:Optional public abstract val firMetadataFile: RegularFileProperty

    /** Internal scratch state — cleared on cache restore instead of being replayed. */
    @get:LocalState public abstract val scratchDir: DirectoryProperty

    @TaskAction
    public fun generate() {
        // Process isolation forks a pooled worker JVM that hosts `kotlin-compiler-embeddable` in
        // its
        // own metaspace, instead of loading the compiler into the Gradle daemon. On a multi-module
        // KMP build many producer tasks run concurrently; under classloader isolation their
        // parallel
        // K2 invocations share the daemon's metaspace and exhaust it (`OutOfMemoryError:
        // Metaspace`).
        // The fork is reused across tasks with identical options, so the JVM-startup cost is paid
        // once per pooled worker, not per task.
        val queue =
            workers.processIsolation { spec ->
                spec.classpath.from(faktWorkerClasspath)
                spec.forkOptions { options ->
                    options.maxHeapSize = WORKER_MAX_HEAP
                    options.jvmArgs(WORKER_METASPACE_ARG)
                }
            }
        queue.submit(FaktCodegenWorkAction::class.java) { params ->
            params.sources.from(sources)
            params.analysisOnlySources.from(analysisOnlySources)
            params.compileClasspath.from(compileClasspath)
            params.commonKlibClasspath.from(commonKlibClasspath)
            params.faktCompilerClasspath.from(faktCompilerClasspath)
            params.sourceSetContextJson.set(sourceSetContextJson)
            params.faktVersion.set(faktVersion)
            params.logLevel.set(logLevel)
            params.enableCallHistory.set(enableCallHistory)
            params.enableMutableFakes.set(enableMutableFakes)
            params.imports.set(imports)
            params.commonFirMetadata.set(commonFirMetadata)
            params.generatedKotlinDir.set(generatedKotlinDir)
            params.firMetadataFile.set(firMetadataFile)
            params.scratchDir.set(scratchDir)
        }
    }
}
