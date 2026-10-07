// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import java.io.File
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
 * Gradle TestKit suite for the **synthetic common producer** shape of [FaktGenerateTask]: a KMP
 * project whose targets are all JVM (`jvm("desktop")` + `jvm("server")`) has no `commonMain`
 * metadata compilation, so one task owns `commonMain`.
 *
 * `commonMain` is fed as `commonSources` (emitted). The representative platform (`desktopMain`) is
 * fed as `platformAnalysisOnlySources`: it is analysed so its `actual`s pair with the common
 * `expect`s, but its own fakes are not emitted because the route map only owns `commonMain`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateSyntheticShapeTest {

    @Test
    fun `GIVEN a platform actual for a common expect WHEN running THEN it succeeds and emits only the common fake`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, withSources = true)

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse(result.output.contains("NO_ACTUAL_FOR_EXPECT"), result.output)
        assertEquals(setOf("FakeCommonAuditServiceImpl.kt"), fakeNames(projectDir.resolve(OUTPUT)))
    }

    @Test
    fun `GIVEN no common and no platform sources WHEN running THEN the task is NO-SOURCE`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, withSources = false)

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.NO_SOURCE, result.task(":faktGenerate")?.outcome, result.output)
        assertTrue(fakeNames(projectDir.resolve(OUTPUT)).isEmpty())
    }

    private fun runTask(projectDir: File, vararg arguments: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments(*arguments, "--stacktrace")
            .build()

    private fun fakeNames(dir: File): Set<String> =
        dir.walkTopDown()
            .filter { it.isFile && it.name.startsWith("Fake") && it.extension == "kt" }
            .map { it.name }
            .toSet()

    private fun setupProject(projectDir: File, withSources: Boolean) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-synthetic-shape-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        projectDir.resolve("src/commonMain/kotlin/fixture").mkdirs()
        projectDir.resolve("src/desktopMain/kotlin/fixture").mkdirs()
        if (withSources) {
            projectDir.resolve("src/commonMain/kotlin/fixture/Common.kt").writeText(COMMON_FIXTURE)
            projectDir
                .resolve("src/desktopMain/kotlin/fixture/Desktop.kt")
                .writeText(DESKTOP_FIXTURE)
        }
        projectDir.resolve("build.gradle.kts").writeText(buildScript(projectDir))
    }

    private fun buildScript(projectDir: File): String {
        val sourceSetContextJson = json.encodeToString(SourceSetContext.serializer(), CONTEXT)
        val cp = workerClasspath()
        val classpathLiteral = cp.joinToString(",\n        ") { fileLiteral(it) }
        val compilerJarLiteral =
            cp.filter { it.name.startsWith("compiler-") && it.name.endsWith(".jar") }
                .joinToString(",\n        ") { fileLiteral(it) }
                .ifEmpty {
                    error(
                        "Fakt :compiler shadowJar not found on test classpath. Ensure :compiler:shadowJar ran before tests."
                    )
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
                commonSources.from(file("src/commonMain/kotlin"))
                platformAnalysisOnlySources.from(file("src/desktopMain/kotlin"))
                compileClasspath.from(
                    $classpathLiteral
                )
                faktWorkerClasspath.from(
                    $classpathLiteral
                )
                faktCompilerClasspath.from(
                    $compilerJarLiteral
                )
                sourceSetContextJson.set(${'"'}${'"'}${'"'}${sourceSetContextJson}${'"'}${'"'}${'"'})
                faktVersion.set("test-1.0")
                logLevel.set(LogLevel.QUIET)
                imports.set(listOf<String>())
                generatedKotlinDir.set(file("${projectDir.resolve(OUTPUT).slashPath()}"))
                scratchDir.set(layout.buildDirectory.dir("faktCaches/desktop/main"))
            }
            """
            .trimIndent()
    }

    private fun fileLiteral(file: File): String = """file("${file.slashPath()}")"""

    private fun File.slashPath(): String = absolutePath.replace('\\', '/')

    private fun workerClasspath(): List<File> =
        System.getProperty("java.class.path").split(File.pathSeparator).map(::File).filter { entry
            ->
            entry.exists() &&
                "kctfork" !in entry.absolutePath &&
                !entry.name.startsWith("kotlin-gradle-plugin")
        }

    private val json = Json { prettyPrint = false }

    companion object {
        private const val OUTPUT = "build/generated/fakt/commonTest/kotlin"

        private val CONTEXT =
            SourceSetContext(
                compilationName = "main",
                targetName = "desktop",
                platformType = "jvm",
                isTest = false,
                defaultSourceSet = SourceSetInfo("desktopMain", parents = listOf("commonMain")),
                allSourceSets =
                    listOf(
                        SourceSetInfo("desktopMain", parents = listOf("commonMain")),
                        SourceSetInfo("commonMain", parents = emptyList()),
                    ),
                outputDirectory = "fakt://generated",
                commonTestOutputDirectory = "fakt://generated",
                outputDirectories = mapOf("commonMain" to "fakt://generated"),
            )

        private val COMMON_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            expect fun platformName(): String

            @Fake
            interface CommonAuditService {
                fun record(event: String): Boolean
            }
            """
                .trimIndent()

        private val DESKTOP_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            actual fun platformName(): String = "Desktop"

            @Fake
            interface DesktopDeviceService {
                fun label(): String
            }
            """
                .trimIndent()
    }
}
