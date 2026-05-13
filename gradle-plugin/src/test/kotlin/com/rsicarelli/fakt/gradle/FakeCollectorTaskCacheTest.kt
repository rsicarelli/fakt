// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Gradle TestKit suite for [FakeCollectorTask]'s cache-correctness contract (PR 4 of the issue #79
 * roadmap). Tests register the collector against a stand-in `Sync` task that mimics the
 * `FaktGenerateTask.generatedKotlinDir` output — this keeps the test free of the K2 worker
 * classpath setup the producer suite needs, while still proving:
 * 1. The collector's `sourceFakeRoots` `@InputFiles` produces a stable build-cache key, so a clean
 *    rebuild against a populated `--build-cache` reports `FROM-CACHE`. This is the canonical
 *    issue #79 regression at collector level.
 * 2. Wiring `sourceFakeRoots.from(taskProvider.map { it.outputDir })` carries the implicit
 *    `builtBy` chain, so requesting `:collectFakes` alone implicitly runs the producer.
 *
 * Legacy in-process path is intentionally not exercised here — see [FakeCollectorTaskTest] for the
 * action-level coverage of that fallback.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeCollectorTaskCacheTest {

    @Test
    fun `GIVEN populated cache WHEN running clean rebuild with --build-cache THEN collector reports FROM-CACHE`(
        @TempDir projectDir: File,
        @TempDir buildCacheDir: File,
    ) {
        setupProject(projectDir)
        configureLocalBuildCache(projectDir, buildCacheDir)

        val first = runTask(projectDir, "collectFakes", "--build-cache")
        assertEquals(
            TaskOutcome.SUCCESS,
            first.task(":collectFakes")?.outcome,
            "First run failed:\n${first.output}",
        )
        assertTrue(
            projectDir
                .resolve("build/collected/jvmMain/kotlin/api/jvm/FakeJvmServiceImpl.kt")
                .exists(),
            "First run must produce the routed fake.",
        )

        runTask(projectDir, "clean")

        val second = runTask(projectDir, "collectFakes", "--build-cache")
        assertEquals(
            TaskOutcome.FROM_CACHE,
            second.task(":collectFakes")?.outcome,
            "Expected FROM-CACHE on second run — collector is not cache-correct.\n" +
                "first run:\n${first.output}\nsecond run:\n${second.output}",
        )
        assertTrue(
            projectDir
                .resolve("build/collected/jvmMain/kotlin/api/jvm/FakeJvmServiceImpl.kt")
                .exists(),
            "Cache restore must replace the collected output (issue #79 regression at collector).",
        )
    }

    @Test
    fun `GIVEN collector wired via TaskProvider chain WHEN running collector alone THEN producer is triggered implicitly`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir)

        val result = runTask(projectDir, "collectFakes")

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":fakeProducer")?.outcome,
            "Producer must run before collector via the builtBy chain on sourceFakeRoots.\n${result.output}",
        )
        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":collectFakes")?.outcome,
            "Collector itself failed:\n${result.output}",
        )
    }

    private fun runTask(projectDir: File, vararg arguments: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments(*arguments, "--stacktrace")
            .build()

    private fun setupProject(projectDir: File) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-collector-test"""")
        // TestKit daemons share state across tests; cap heap explicitly to avoid Metaspace bloat
        // when the suite runs alongside FaktGenerateTaskTest in the same fork.
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        // Pre-staged fixture mimicking what FaktGenerateTask would write under
        // build/generated/fakt/<target>/<compilation>/kotlin
        val fixture = projectDir.resolve("fixture/api/jvm/FakeJvmServiceImpl.kt")
        fixture.parentFile.mkdirs()
        fixture.writeText("package api.jvm\nclass FakeJvmServiceImpl")
        projectDir.resolve("build.gradle.kts").writeText(buildScript())
    }

    private fun configureLocalBuildCache(projectDir: File, buildCacheDir: File) {
        projectDir
            .resolve("settings.gradle.kts")
            .appendText(
                """

                buildCache {
                    local {
                        directory = file("${buildCacheDir.absolutePath.replace('\\', '/')}")
                        isPush = true
                    }
                }
                """
                    .trimIndent()
            )
    }

    private fun buildScript(): String {
        val cp = workerClasspath()
        val classpathLiteral =
            cp.joinToString(",\n        ") { jar ->
                """file("${jar.absolutePath.replace('\\', '/')}")"""
            }
        return """
            @file:OptIn(com.rsicarelli.fakt.gradle.ExperimentalFaktMultiModule::class)

            buildscript {
                dependencies {
                    classpath(files(
                        $classpathLiteral
                    ))
                }
            }

            import com.rsicarelli.fakt.gradle.FakeCollectorTask
            import com.rsicarelli.fakt.compiler.api.LogLevel

            // Stand-in for FaktGenerateTask. Output mirrors generatedKotlinDir's layout: a package-
            // structured Kotlin source root.
            val fakeProducer = tasks.register<Sync>("fakeProducer") {
                from("fixture")
                into(layout.buildDirectory.dir("generated/fakt/jvm/main/kotlin"))
            }

            tasks.register<FakeCollectorTask>("collectFakes") {
                sourceProjectPath.set(":fixture")
                sourceFakeRoots.from(fakeProducer.map { it.destinationDir })
                destinationDir.set(layout.buildDirectory.dir("collected"))
                availableSourceSets.set(setOf("commonMain", "jvmMain"))
                logLevel.set(LogLevel.QUIET)
            }

            tasks.register("clean") { doLast { delete(layout.buildDirectory) } }
            """
            .trimIndent()
    }

    /**
     * Plugin-under-test classpath, minus kctfork and kotlin-gradle-plugin — both pull stale
     * compiler internals and would crash the TestKit daemon's classloader. Mirrors the filter on
     * [FaktGenerateTaskTest.workerClasspath].
     */
    private fun workerClasspath(): List<File> =
        System.getProperty("java.class.path").split(File.pathSeparator).map(::File).filter { entry
            ->
            entry.exists() &&
                "kctfork" !in entry.absolutePath &&
                !entry.name.startsWith("kotlin-gradle-plugin")
        }
}
