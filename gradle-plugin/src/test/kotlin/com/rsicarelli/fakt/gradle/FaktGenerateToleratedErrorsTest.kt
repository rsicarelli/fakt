// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * TestKit suite for the worker accepting compiler errors outside `@Fake` code (#165). Uses the
 * manual-task pattern of [FaktGenerateTaskTest].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateToleratedErrorsTest {

    @Test
    fun `GIVEN an unresolved reference outside any fake WHEN the task runs THEN it succeeds and generates the fake`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, fixtures = TOLERATED_FIXTURES, logLevel = "INFO")

        val result = gradle(projectDir).build("faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertTrue(
            generatedFiles(projectDir).any { it.name.contains("UserService") },
            result.output,
        )
        assertContains(
            result.output,
            "Fakt: tolerated compiler error(s) outside @Fake code: 1 " +
                "(set fakt logLevel to DEBUG to list each one)",
            message = result.output,
        )
        assertFalse("Other.kt" in result.output, "INFO must not list each error\n${result.output}")
        assertNoRawCompilerError(result)
    }

    @Test
    fun `GIVEN an unresolved reference outside any fake WHEN the log level is DEBUG THEN the located error is listed`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, fixtures = TOLERATED_FIXTURES, logLevel = "DEBUG")

        val result = gradle(projectDir).build("faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        val line = result.output.lines().single { it.startsWith("Fakt: tolerated compiler error:") }
        assertTrue("Other.kt:3:" in line, line)
        assertTrue("nresolved reference" in line, line)
        assertNoRawCompilerError(result)
    }

    @Test
    fun `GIVEN an unresolved reference outside any fake WHEN the log level is QUIET THEN nothing is logged about it`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, fixtures = TOLERATED_FIXTURES, logLevel = "QUIET")

        val result = gradle(projectDir).build("faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse("Fakt: tolerated" in result.output, result.output)
        assertNoRawCompilerError(result)
        assertTrue(generatedFiles(projectDir).any { it.name.contains("UserService") })
    }

    @Test
    fun `GIVEN clean code WHEN the task runs at INFO THEN no tolerated line is printed`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, fixtures = mapOf("Fixture.kt" to FAKE_FIXTURE), logLevel = "INFO")

        val result = gradle(projectDir).build("faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse("Fakt: tolerated" in result.output, result.output)
    }

    @Test
    fun `GIVEN an error without a source location WHEN the task runs THEN it stays fatal`(
        @TempDir projectDir: File
    ) {
        setupProject(
            projectDir,
            fixtures = mapOf("Fixture.kt" to FAKE_FIXTURE),
            taskConfig = """compilerArguments.set(listOf("-language-version=9.9"))""",
        )

        val result = gradle(projectDir).buildAndFail("faktGenerate")

        assertEquals(TaskOutcome.FAILED, result.task(":faktGenerate")?.outcome, result.output)
    }

    @Test
    fun `GIVEN a fake whose signature uses an unresolved type WHEN the task runs THEN it fails with FAKT and generates nothing`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, fixtures = UNRESOLVED_IN_FAKE_FIXTURES, logLevel = "INFO")

        val result = gradle(projectDir).buildAndFail("faktGenerate")

        assertEquals(TaskOutcome.FAILED, result.task(":faktGenerate")?.outcome, result.output)
        assertContains(result.output, "[FAKT]", message = result.output)
        assertTrue(generatedFiles(projectDir).none { it.name.contains("Broken") }, result.output)
    }

    @Test
    fun `GIVEN a failing run with an unrelated located error WHEN the task fails THEN every error is printed`(
        @TempDir projectDir: File
    ) {
        setupProject(
            projectDir,
            fixtures =
                UNRESOLVED_IN_FAKE_FIXTURES + TOLERATED_FIXTURES.filterKeys { it == "Other.kt" },
        )

        val result = gradle(projectDir).buildAndFail("faktGenerate")

        assertContains(result.output, "[FAKT]", message = result.output)
        assertContains(result.output, "Other.kt", message = result.output)
        assertContains(result.output, "Other.kt:3", message = result.output)
        assertContains(result.output, "unresolved reference", message = result.output)
    }

    /** The printing collector renders `<path>:<line>:<col>: error: <message>`. */
    private fun assertNoRawCompilerError(result: BuildResult) {
        val raw =
            result.output
                .lines()
                .filterNot { it.startsWith("Fakt: tolerated") }
                .filter {
                    it.contains("error:") || it.contains("unresolved reference", ignoreCase = true)
                }
        assertTrue(raw.isEmpty(), "worker printed raw compiler errors: $raw\n${result.output}")
    }

    private fun generatedFiles(projectDir: File): List<File> =
        projectDir
            .resolve("build/generated/fakt/jvm/jvmMain/kotlin")
            .walkTopDown()
            .filter { it.isFile }
            .toList()

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
        logLevel: String = "QUIET",
    ) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-tolerated-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        projectDir.resolve("src/main/kotlin/fixture").mkdirs()
        fixtures.forEach { (name, source) ->
            projectDir.resolve("src/main/kotlin/fixture/$name").writeText(source)
        }
        projectDir
            .resolve("build.gradle.kts")
            .writeText(buildScript(projectDir, taskConfig, logLevel))
    }

    private fun buildScript(projectDir: File, taskConfig: String, logLevel: String): String {
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
                logLevel.set(LogLevel.$logLevel)
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

        /** `Unresolved.serializer()` stands in for code that needs another compiler plugin. */
        private val TOLERATED_FIXTURES =
            mapOf(
                "Fixture.kt" to FAKE_FIXTURE,
                "Other.kt" to
                    """
                    package fixture

                    fun codec() = Unresolved.serializer()
                    """
                        .trimIndent(),
            )

        private val UNRESOLVED_IN_FAKE_FIXTURES =
            mapOf(
                "Broken.kt" to
                    """
                    package fixture

                    import com.rsicarelli.fakt.Fake

                    @Fake
                    interface BrokenService {
                        fun load(): Unresolved
                    }
                    """
                        .trimIndent()
            )
    }
}
