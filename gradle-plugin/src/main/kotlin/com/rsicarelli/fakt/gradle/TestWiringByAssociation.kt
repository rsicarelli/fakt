// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.util.Locale
import java.util.concurrent.Callable
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

/** The kind of test-like Kotlin compile task a name denotes. */
internal enum class TestCompileKind {
    UNIT,
    ANDROID_TEST,
    FIXTURES,
    TEST,
}

/** A parsed test-like compile task: [variant] (`main` when the task has none) and its [kind]. */
internal data class TestCompileTask(val variant: String, val kind: TestCompileKind)

private val TEST_COMPILE_TASK =
    Regex("^compile(.*?)(UnitTest|AndroidTest|TestFixtures|Test)Kotlin$")

private const val MAIN_VARIANT = "main"

/**
 * Parses a Kotlin compile task name into its build variant and test kind, or `null` when the task
 * is not a test-like compile (`compileKotlin`, `compileDebugKotlin`). The variant is matched as a
 * whole, so `debugMinified` is never confused with `debug`.
 *
 * `compileDebugMinifiedUnitTestKotlin` -> (`debugMinified`, UNIT); `compileTestKotlin` -> (`main`,
 * TEST).
 */
internal fun testCompileVariant(taskName: String): TestCompileTask? =
    TEST_COMPILE_TASK.matchEntire(taskName)?.let { match ->
        val (rawVariant, rawKind) = match.destructured
        TestCompileTask(
            variant =
                rawVariant.replaceFirstChar { it.lowercase(Locale.ROOT) }.ifEmpty { MAIN_VARIANT },
            kind =
                when (rawKind) {
                    "UnitTest" -> TestCompileKind.UNIT
                    "AndroidTest" -> TestCompileKind.ANDROID_TEST
                    "TestFixtures" -> TestCompileKind.FIXTURES
                    else -> TestCompileKind.TEST
                },
        )
    }

/**
 * Decides whether a non-KMP producer's generated-fakes directory is sourced by a Kotlin compile
 * task.
 * - Fixtures on: only the `testFixtures` compile of the producer's own variant (a `main` producer
 *   feeds every fixtures compile).
 * - Fixtures off, [associatedWith] known: the task's compilation is associated with the producer's
 *   compilation, whatever the task is called.
 * - Fixtures off, no association data: exact-name fallback, the producer's own variant for a
 *   variant producer, any non-fixtures test compile for `main`.
 *
 * @param associatedWith names of the compilations the task's compilation is associated with, or
 *   `null` when it has no association data.
 */
internal fun wiresTestCompile(
    taskName: String,
    producer: String,
    associatedWith: Set<String>?,
    useTestFixtures: Boolean,
): Boolean {
    val parsed = testCompileVariant(taskName)
    val producesMain = producer.equals(MAIN_VARIANT, ignoreCase = true)
    val sameVariant = parsed?.variant.equals(producer, ignoreCase = true)
    return when {
        useTestFixtures -> parsed?.kind == TestCompileKind.FIXTURES && (producesMain || sameVariant)
        parsed?.kind == TestCompileKind.FIXTURES -> false
        associatedWith != null ->
            producer in associatedWith && !isProductionCompile(taskName, producer)
        parsed == null -> producesMain && isPlainTestTask(taskName)
        else -> producesMain || sameVariant
    }
}

/** Whether [taskName] is the production compile of [producer] itself, which fakes never reach. */
private fun isProductionCompile(taskName: String, producer: String): Boolean =
    taskName.equals(
        if (producer.equals(MAIN_VARIANT, ignoreCase = true)) "compileKotlin"
        else "compile${producer.replaceFirstChar { it.uppercase(Locale.ROOT) }}Kotlin",
        ignoreCase = true,
    )

/** Today's `main` rule for task names the parser does not know: a non-fixtures `*test*` task. */
private fun isPlainTestTask(taskName: String): Boolean =
    taskName.contains("test", ignoreCase = true) && !isTestFixturesCompileTask(taskName)

/** Whether [taskName] is a `testFixtures` Kotlin compile task (legacy fixtures route). */
internal fun isTestFixturesCompileTask(taskName: String): Boolean =
    taskName.contains("testfixtures", ignoreCase = true)

/**
 * Adds [taskProvider]'s generated directory to the tests of [compilation], decided by association
 * and never by name. Lazy via the task provider, so Gradle infers `builtBy` and the test compile
 * waits for the generator with no explicit `dependsOn`.
 * - KMP `commonMain`: feeds `commonTest`.
 * - KMP platform compilation: feeds the default source set of every compilation associated with it
 *   ([wireKmpAssociatedTests]); the KMP Android library target's `androidMain` is tested by
 *   `androidHostTest` / `androidDeviceTest`, not by a `<target>Test` name.
 * - Non-KMP: feeds the compile tasks [wiresTestCompile] accepts ([wireCompileTasksByAssociation]).
 */
internal fun wireTestSrcDirByAssociation(
    project: Project,
    compilation: KotlinCompilation<*>,
    taskProvider: TaskProvider<FaktGenerateTask>,
    useTestFixtures: Boolean,
) {
    val kmp = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
    val generatedDir = taskProvider.flatMap { it.generatedKotlinDir }
    when {
        kmp == null ->
            wireCompileTasksByAssociation(project, compilation, generatedDir, useTestFixtures)
        compilation.defaultSourceSet.name == "commonMain" -> {
            claimTestSourceSet(project, "commonTest", taskProvider.name)
            kmp.sourceSets.findByName("commonTest")?.kotlin?.srcDir(generatedDir)
        }
        else -> wireKmpAssociatedTests(project, compilation, taskProvider.name, generatedDir)
    }
}

/**
 * Claims every test compilation associated with [main] for [taskName] in the registry, and gives
 * each one's default source set the generated directory. The association is read inside a
 * [Callable], when Gradle resolves the files or visits the dependencies, so one made after this
 * call still counts; a [Callable] (never `project.provider {}`) keeps the task dependency.
 */
private fun wireKmpAssociatedTests(
    project: Project,
    main: KotlinCompilation<*>,
    taskName: String,
    generatedDir: Provider<Directory>,
) {
    claimAssociatedTests(project, compilationKey(main.target.name, main.name), taskName)
    main.target.compilations.configureEach { candidate ->
        if (candidate != main) {
            candidate.defaultSourceSet.kotlin.srcDir(
                project.files(
                    Callable {
                        if (main in candidate.associatedCompilations) generatedDir
                        else emptyList<Any>()
                    }
                )
            )
        }
    }
}

/**
 * Sources [generatedDir] into every Kotlin compile task [wiresTestCompile] accepts for the producer
 * [compilation]. A non-KMP target may register several producers (one per Android variant); each
 * feeds only its own variant's tests, or the `testFixtures` compile in fixtures mode. The
 * association of the task's compilation is read lazily, inside the [Callable].
 */
private fun wireCompileTasksByAssociation(
    project: Project,
    compilation: KotlinCompilation<*>,
    generatedDir: Provider<Directory>,
    useTestFixtures: Boolean,
) {
    project.tasks.withType(AbstractKotlinCompile::class.java).configureEach { compileTask ->
        compileTask.source(
            project.files(
                Callable {
                    val wired =
                        wiresTestCompile(
                            compileTask.name,
                            compilation.name,
                            associationsOf(compilation, compileTask.name),
                            useTestFixtures,
                        )
                    if (wired) generatedDir else emptyList<Any>()
                }
            )
        )
    }
}

/** Names the compilation behind [taskName] is associated with, `null` when it has none. */
private fun associationsOf(compilation: KotlinCompilation<*>, taskName: String): Set<String>? =
    compilation.target.compilations
        .firstOrNull { it.compileKotlinTaskName == taskName }
        ?.associatedCompilations
        ?.map { it.name }
        ?.toSet()
        ?.takeIf { it.isNotEmpty() }
