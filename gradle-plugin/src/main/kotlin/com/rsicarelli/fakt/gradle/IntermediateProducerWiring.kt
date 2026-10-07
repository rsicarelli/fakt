// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.util.concurrent.Callable
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget

private const val MAIN_COMPILATION = "main"
private const val COMMON_MAIN_COMPILATION = "commonMain"

/**
 * Whether [compilation] is the metadata compilation KGP builds for a shared source set other than
 * `commonMain` (`webMain` for js + wasmJs). The legacy metadata `main` compilation is not one.
 */
internal fun isIntermediateMetadata(compilation: KotlinCompilation<*>): Boolean =
    compilation.target is KotlinMetadataTarget &&
        compilation.name != MAIN_COMPILATION &&
        compilation.name != COMMON_MAIN_COMPILATION

/**
 * Who owns the default source set of [compilation], when it is an intermediate metadata
 * compilation; `null` otherwise. The compilation being applied counts as built even when KGP has
 * not listed it on the metadata target yet, and its `dependsOn` edges are already visible here (KGP
 * creates it after the default hierarchy is applied).
 */
internal fun sharedSourceSetOwner(
    kmp: KotlinMultiplatformExtension?,
    compilation: KotlinCompilation<*>,
): SourceSetOwner? {
    if (kmp == null || !isIntermediateMetadata(compilation)) return null
    val sourceSet = compilation.defaultSourceSet.name
    val graph = readSourceSetGraph(kmp)
    val built = graph.copy(metadataCompilations = graph.metadataCompilations + sourceSet)
    return assignSourceSetOwners(built)[sourceSet]
}

/**
 * Registers the producer a [CompilationRoute] of `REGISTER_PRODUCER` asks for: the intermediate
 * metadata producer for a `webMain`-style compilation, the ordinary producer for everything else.
 */
internal fun registerProducerFor(
    project: Project,
    compilation: KotlinCompilation<*>,
    extension: FaktPluginExtension,
) {
    if (isIntermediateMetadata(compilation)) {
        registerIntermediateProducer(project, compilation, extension)
    } else {
        FaktGenerateTaskWiring.registerProducer(project, compilation, extension)
    }
}

/**
 * Registers `faktGenerateMetadata<Set>` for an intermediate metadata [compilation]. A task of that
 * name that Fakt did not register is a name clash and fails the build with a clear message instead
 * of being skipped; Fakt's own task makes a repeated call a no-op.
 */
private fun registerIntermediateProducer(
    project: Project,
    compilation: KotlinCompilation<*>,
    extension: FaktPluginExtension,
) {
    val shape = TaskShape.INTERMEDIATE_METADATA
    val taskName = taskLayoutFor(compilation, shape).taskName
    val existing = project.tasks.findByName(taskName)
    if (
        existing != null && !(existing is FaktGenerateTask && isFaktTestDirOwner(project, taskName))
    ) {
        throw GradleException(
            "Fakt needs the task name '$taskName' for the producer of the " +
                "'${compilation.defaultSourceSet.name}' source set, but a task with that name " +
                "already exists in project '${project.path}'. Rename that task."
        )
    }
    FaktGenerateTaskWiring.register(project, compilation, extension, shape)
}

/**
 * Feeds an intermediate producer: only the set's own sources are analysed and emitted. `commonMain`
 * (and every other ancestor) arrives as the compiled metadata klib on the classpath, and as the
 * `-Xrefines-paths` input [FaktGenerateTask.refinesKlibs]; handing the driver their sources as well
 * would redeclare them.
 */
internal fun configureIntermediateSources(
    task: FaktGenerateTask,
    compilation: KotlinCompilation<*>,
    enabled: Provider<Boolean>,
) {
    task.sources.from(enabled.gate(compilation.defaultSourceSet.kotlin))
    task.refinesKlibs.from(refinesOutputs(compilation))
}

/**
 * The metadata outputs of the source sets [compilation] refines: the same files KGP hands its own
 * metadata compiler as `-Xrefines-paths`. Resolved lazily, and built by the ancestors' compile
 * tasks.
 */
private fun refinesOutputs(compilation: KotlinCompilation<*>): FileCollection =
    compilation.project.files(
        Callable {
            val metadata = compilation.target as KotlinMetadataTarget
            val own = compilation.defaultSourceSet
            own.getAllParentSourceSets()
                .filter { it != own }
                .mapNotNull { ancestor ->
                    metadata.compilations.findByName(ancestor.name)?.output?.classesDirs
                }
        }
    )

/**
 * Tells [task] where every analysed source set keeps its sources, so the worker can attribute each
 * file to a source set when an intermediate level makes it pass fragments. Read when the input
 * resolves, because the `dependsOn` edges are not final while the compilation is being applied.
 */
internal fun configureSourceSetRoots(task: FaktGenerateTask, compilation: KotlinCompilation<*>) {
    task.sourceSetRoots.putAll(
        task.project.provider {
            compilation.allKotlinSourceSets.associate { set ->
                set.name to set.kotlin.srcDirs.map { it.path }
            }
        }
    )
}

/**
 * Hands the producer's output to the tests that see the owned set's fakes: the counterpart test
 * source set (`webTest`) when a test compilation compiles it, otherwise each leaf test source set
 * (see [testWiringFor]). The srcDir goes through the [taskProvider], so Gradle infers the task
 * dependency, and every wired set is recorded so [SourceSetConfigurator] adds no plain directory.
 * [owned] is the set whose fakes the task writes: the compilation's own default set for a metadata
 * producer, the intermediate set for a synthetic one that rides on a platform compilation.
 */
internal fun wireIntermediateTestDirs(
    project: Project,
    compilation: KotlinCompilation<*>,
    taskProvider: TaskProvider<FaktGenerateTask>,
    owned: String? = null,
) {
    val kmp = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
    val owners = testDirOwners(project)
    owners.tasks.add(taskProvider.name)
    val generated = taskProvider.flatMap { it.generatedKotlinDir }
    testSourceSetsFor(kmp, owned ?: compilation.defaultSourceSet.name).forEach { name ->
        kmp.sourceSets.findByName(name)?.let { testSet ->
            testSet.kotlin.srcDir(generated)
            owners.bySourceSet[name] = taskProvider.name
        }
    }
    wireAndroidLintOrdering(project, taskProvider)
}

private fun testSourceSetsFor(kmp: KotlinMultiplatformExtension, owned: String): Set<String> {
    val testCompilations =
        kmp.targets
            .filter { it !is KotlinMetadataTarget }
            .flatMap { it.compilations }
            .filter { it.isTestCompilation }
    val compiled =
        testCompilations.flatMapTo(linkedSetOf()) { c -> c.allKotlinSourceSets.map { it.name } }
    val leaves =
        testCompilations
            .filter { test ->
                test.associatedCompilations.any { main ->
                    main.allKotlinSourceSets.any { it.name == owned }
                }
            }
            .mapTo(linkedSetOf()) { it.defaultSourceSet.name }
    return testWiringFor(owned, compiled, leaves)
}
