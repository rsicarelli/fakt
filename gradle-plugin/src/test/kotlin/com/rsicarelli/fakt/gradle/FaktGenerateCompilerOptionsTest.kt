// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * TestKit suite for the compiler options the Gradle plugin forwards to the Fakt worker
 * (`compilerArguments`, `jdkHome`). Uses the manual-task pattern of [FaktGenerateTaskTest]: the
 * task is registered by hand with explicit inputs, so these tests exercise the task and worker
 * contract without the wiring that derives the values from a Kotlin compilation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateCompilerOptionsTest {

    @Test
    fun `GIVEN main code using an ERROR-level opt-in marker WHEN no compiler argument is forwarded THEN the error is tolerated and logged`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, fixtures = OPT_IN_FIXTURES, taskConfig = INFO_LOG)

        val result = gradle(projectDir).build("faktGenerate")

        assertContains(result.output, "Fakt: tolerated compiler error", message = result.output)
        assertContains(result.output, "Marker", message = result.output)
        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
    }

    @Test
    fun `GIVEN main code using an ERROR-level opt-in marker WHEN -opt-in is forwarded THEN the task succeeds`(
        @TempDir projectDir: File
    ) {
        setupProject(
            projectDir,
            fixtures = OPT_IN_FIXTURES,
            taskConfig = INFO_LOG + """compilerArguments.set(listOf("-opt-in=fixture.Marker"))""",
        )

        val result = gradle(projectDir).build("faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse("Fakt: tolerated" in result.output, result.output)
    }

    @Test
    fun `GIVEN context-sensitive resolution outside any fake file WHEN no compiler argument is forwarded THEN the error is tolerated and logged`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, fixtures = CONTEXT_FIXTURES, taskConfig = INFO_LOG)

        val result = gradle(projectDir).build("faktGenerate")

        assertContains(result.output, "Fakt: tolerated compiler error", message = result.output)
        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
    }

    @Test
    fun `GIVEN context-sensitive resolution outside any fake file WHEN -Xcontext-sensitive-resolution is forwarded THEN the task succeeds`(
        @TempDir projectDir: File
    ) {
        setupProject(
            projectDir,
            fixtures = CONTEXT_FIXTURES,
            taskConfig =
                INFO_LOG + """compilerArguments.set(listOf("-Xcontext-sensitive-resolution"))""",
        )

        val result = gradle(projectDir).build("faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse("Fakt: tolerated" in result.output, result.output)
    }

    @Test
    fun `GIVEN task ran once WHEN the compiler arguments change THEN the task re-executes`(
        @TempDir projectDir: File
    ) {
        setupProject(
            projectDir,
            fixtures = OPT_IN_FIXTURES,
            taskConfig = """compilerArguments.set(listOf("-opt-in=fixture.Marker"))""",
        )
        val first = gradle(projectDir).build("faktGenerate")
        assertEquals(TaskOutcome.SUCCESS, first.task(":faktGenerate")?.outcome, first.output)
        val control = gradle(projectDir).build("faktGenerate")
        assertEquals(
            TaskOutcome.UP_TO_DATE,
            control.task(":faktGenerate")?.outcome,
            "Unchanged compilerArguments must be UP-TO-DATE:\n${control.output}",
        )
        projectDir
            .resolve("build.gradle.kts")
            .writeText(
                buildScript(
                    projectDir,
                    """compilerArguments.set(listOf("-opt-in=fixture.Marker", "-progressive"))""",
                )
            )

        val second = gradle(projectDir).build("faktGenerate")

        assertEquals(
            TaskOutcome.SUCCESS,
            second.task(":faktGenerate")?.outcome,
            "Changed compilerArguments must invalidate the task:\n${second.output}",
        )
    }

    @Test
    fun `GIVEN identical compiler arguments in two project dirs sharing a build cache WHEN both run THEN second reports FROM-CACHE`(
        @TempDir projectA: File,
        @TempDir projectB: File,
        @TempDir buildCacheDir: File,
    ) {
        val config = """compilerArguments.set(listOf("-opt-in=fixture.Marker"))"""
        setupProject(projectA, fixtures = OPT_IN_FIXTURES, taskConfig = config)
        configureLocalBuildCache(projectA, buildCacheDir)
        setupProject(projectB, fixtures = OPT_IN_FIXTURES, taskConfig = config)
        configureLocalBuildCache(projectB, buildCacheDir)

        val first = gradle(projectA).build("faktGenerate", "--build-cache")
        assertEquals(TaskOutcome.SUCCESS, first.task(":faktGenerate")?.outcome, first.output)
        val second = gradle(projectB).build("faktGenerate", "--build-cache")

        assertEquals(
            TaskOutcome.FROM_CACHE,
            second.task(":faktGenerate")?.outcome,
            "Forwarded arguments must be relocatable:\n${second.output}",
        )
    }

    @Test
    fun `GIVEN code using java io File WHEN jdkHome points at an empty directory THEN the worker fails`(
        @TempDir projectDir: File,
        @TempDir emptyJdk: File,
    ) {
        setupProject(
            projectDir,
            fixtures = mapOf("Fixture.kt" to FAKE_FIXTURE, "FileUse.kt" to FILE_USE_FIXTURE),
            taskConfig = """jdkHome.set(file("${emptyJdk.absolutePath.replace('\\', '/')}"))""",
        )

        val result = gradle(projectDir).buildAndFail("faktGenerate")

        assertEquals(TaskOutcome.FAILED, result.task(":faktGenerate")?.outcome, result.output)
    }

    @Test
    fun `GIVEN code using java io File WHEN no jdkHome is set THEN the task succeeds`(
        @TempDir projectDir: File
    ) {
        setupProject(
            projectDir,
            fixtures = mapOf("Fixture.kt" to FAKE_FIXTURE, "FileUse.kt" to FILE_USE_FIXTURE),
        )

        val result = gradle(projectDir).build("faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
    }

    @Test
    fun `GIVEN an unsupported language version WHEN it is forwarded THEN the task fails naming it`(
        @TempDir projectDir: File
    ) {
        setupProject(
            projectDir,
            fixtures = mapOf("Fixture.kt" to FAKE_FIXTURE),
            taskConfig = """compilerArguments.set(listOf("-language-version=9.9"))""",
        )

        val result = gradle(projectDir).buildAndFail("faktGenerate")

        assertNotEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome)
        assertContains(result.output, "9.9", message = result.output)
        assertContains(result.output, "forwarded compiler arguments", message = result.output)
    }

    @Test
    fun `GIVEN a flag missing its value WHEN it is forwarded THEN the task fails naming the rejected argument`(
        @TempDir projectDir: File
    ) {
        setupProject(
            projectDir,
            fixtures = mapOf("Fixture.kt" to FAKE_FIXTURE),
            taskConfig = """compilerArguments.set(listOf("-api-version"))""",
        )

        val result = gradle(projectDir).buildAndFail("faktGenerate")

        assertContains(result.output, "-api-version", message = result.output)
        assertContains(result.output, "forwarded compiler arguments", message = result.output)
    }

    private fun gradle(projectDir: File): GradleRunner =
        GradleRunner.create().withProjectDir(projectDir).forwardOutput()

    private fun GradleRunner.build(vararg arguments: String): BuildResult =
        withArguments(*arguments, "--stacktrace").build()

    private fun GradleRunner.buildAndFail(vararg arguments: String): BuildResult =
        withArguments(*arguments, "--stacktrace").buildAndFail()

    private fun setupProject(
        projectDir: File,
        fixtures: Map<String, String>,
        taskConfig: String = "",
    ) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-options-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        projectDir.resolve("src/main/kotlin/fixture").mkdirs()
        fixtures.forEach { (name, source) ->
            projectDir.resolve("src/main/kotlin/fixture/$name").writeText(source)
        }
        projectDir.resolve("build.gradle.kts").writeText(buildScript(projectDir, taskConfig))
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

    private fun buildScript(projectDir: File, taskConfig: String): String {
        val outputDir = projectDir.resolve("build/generated/fakt/jvm/jvmMain/kotlin")
        val contextJson = json.encodeToString(SourceSetContext.serializer(), STUB_CONTEXT)
        val cp = workerClasspath()
        val classpathLiteral =
            cp.joinToString(",\n        ") { jar ->
                """file("${jar.absolutePath.replace('\\', '/')}")"""
            }
        val compilerJarLiteral =
            cp.filter { it.name.startsWith("compiler-") && it.name.endsWith(".jar") }
                .joinToString(",\n        ") { jar ->
                    """file("${jar.absolutePath.replace('\\', '/')}")"""
                }
                .ifEmpty {
                    error("Fakt :compiler shadowJar not found; run :compiler:shadowJar first.")
                }
        return """
            buildscript {
                dependencies {
                    classpath(files(
                        $classpathLiteral
                    ))
                }
            }

            import com.rsicarelli.fakt.gradle.FaktGenerateTask
            import com.rsicarelli.fakt.compiler.api.LogLevel

            tasks.register<FaktGenerateTask>("faktGenerate") {
                sources.from(file("src/main/kotlin"))
                compileClasspath.from(
                    $classpathLiteral
                )
                faktWorkerClasspath.from(
                    $classpathLiteral
                )
                faktCompilerClasspath.from(
                    $compilerJarLiteral
                )
                sourceSetContextJson.set(${'"'}${'"'}${'"'}${contextJson}${'"'}${'"'}${'"'})
                faktVersion.set("test-1.0")
                logLevel.set(LogLevel.QUIET)
                imports.set(listOf<String>())
                generatedKotlinDir.set(file("${outputDir.absolutePath.replace('\\', '/')}"))
                scratchDir.set(layout.buildDirectory.dir("faktCaches/jvm/jvmMain"))
                $taskConfig
            }
            """
            .trimIndent()
    }

    /** Same worker classpath recipe as [FaktGenerateTaskTest]: drop kctfork and KGP. */
    private fun workerClasspath(): List<File> =
        System.getProperty("java.class.path").split(File.pathSeparator).map(::File).filter { entry
            ->
            entry.exists() &&
                "kctfork" !in entry.absolutePath &&
                !entry.name.startsWith("kotlin-gradle-plugin")
        }

    private val json = Json { prettyPrint = false }

    companion object {
        /** Later `logLevel.set` wins over the QUIET default of the generated build script. */
        private const val INFO_LOG = "logLevel.set(LogLevel.INFO)\n"

        private val STUB_CONTEXT =
            SourceSetContext(
                compilationName = "main",
                targetName = "jvm",
                platformType = "jvm",
                isTest = false,
                defaultSourceSet = SourceSetInfo("jvmMain", parents = listOf("commonMain")),
                allSourceSets =
                    listOf(
                        SourceSetInfo("jvmMain", parents = listOf("commonMain")),
                        SourceSetInfo("commonMain", parents = emptyList()),
                    ),
                outputDirectory = "/tmp/fakt-spike/jvmMain",
                commonTestOutputDirectory = "/tmp/fakt-spike/commonMain",
            )

        private val FAKE_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            @Fake
            interface UserService {
                fun greet(name: String): String
            }
            """
                .trimIndent()

        /** The marker is used from a top-level function, never inside a `@Fake` declaration. */
        private val OPT_IN_FIXTURES =
            mapOf(
                "Fixture.kt" to FAKE_FIXTURE,
                "Marker.kt" to
                    """
                    package fixture

                    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
                    annotation class Marker

                    @Marker
                    fun risky(): Int = 1
                    """
                        .trimIndent(),
                "Use.kt" to
                    """
                    package fixture

                    fun useRisky(): Int = risky()
                    """
                        .trimIndent(),
            )

        private val CONTEXT_FIXTURES =
            mapOf(
                "Fixture.kt" to FAKE_FIXTURE,
                "Context.kt" to
                    """
                    package fixture

                    enum class Color { Red, Green }

                    fun pick(color: Color): Int =
                        when (color) {
                            Red -> 1
                            Green -> 2
                        }
                    """
                        .trimIndent(),
            )

        private val FILE_USE_FIXTURE =
            """
            package fixture

            import java.io.File

            fun currentDir(): File = File(".")
            """
                .trimIndent()
    }
}
