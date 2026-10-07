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
 * Gradle TestKit suite for the **single-target** shape of [FaktGenerateTask] (issue #153): a KMP
 * project with one target (`kotlin { jvm() }`) has no per-source-set `commonMain` compilation, so
 * its lone platform main owns both halves in one compiler run.
 *
 * `jvmMain` is fed as `sources` and `commonMain` as `commonSources`: the worker marks the latter
 * `-Xcommon-sources` (so `actual`s pair with their `expect`s) and routes their fakes to
 * `commonGeneratedKotlinDir`, while `jvmMain`'s fakes go to `generatedKotlinDir`. The Gradle plugin
 * wires those into `commonTest` and `jvmTest` — a common test must never see a platform fake, and a
 * platform fake in `commonTest` would not compile.
 *
 * Worker classpath is built from the test JVM's own classpath with KGP and kctfork filtered out,
 * for the same dangling-compiler-reference reasons documented in [FaktGenerateTaskTest].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateSingleTargetTest {

    @Test
    fun `GIVEN common and platform fakes with expect-actual WHEN running THEN each half lands in its own output`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, jvmSource = JVM_FIXTURE)

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertEquals(
            setOf("FakeCommonAuditServiceImpl.kt"),
            fakeNames(projectDir.resolve(COMMON_OUTPUT)),
            "commonMain fakes belong in the common output (wired into commonTest)",
        )
        assertEquals(
            setOf("FakeJvmDeviceServiceImpl.kt"),
            fakeNames(projectDir.resolve(PLATFORM_OUTPUT)),
            "jvmMain fakes belong in the platform output (wired into jvmTest)",
        )
    }

    @Test
    fun `GIVEN every fake in commonMain and no jvmMain sources WHEN running THEN the task still runs and emits them`(
        @TempDir projectDir: File
    ) {
        // The compat-sample shape: `kotlin { jvm() }` with everything in commonMain. `sources`
        // (jvmMain) is empty, but `commonSources` is @SkipWhenEmpty too, so the task must run.
        setupProject(projectDir, jvmSource = null, commonSource = COMMON_ONLY_FIXTURE)

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertEquals(
            setOf("FakeCommonAuditServiceImpl.kt"),
            fakeNames(projectDir.resolve(COMMON_OUTPUT)),
        )
        assertTrue(
            fakeNames(projectDir.resolve(PLATFORM_OUTPUT)).isEmpty(),
            "There is no platform fake to emit",
        )
    }

    @Test
    fun `GIVEN a stale in-process copy in the common output WHEN running THEN it is removed`(
        @TempDir projectDir: File
    ) {
        // Before #153 the in-process plugin wrote single-target common fakes to the same canonical
        // directory. A leftover copy must not survive next to the task's own output.
        setupProject(projectDir, jvmSource = JVM_FIXTURE)
        val stale = projectDir.resolve("$COMMON_OUTPUT/fixture/FakeRemovedServiceImpl.kt")
        stale.parentFile.mkdirs()
        stale.writeText("package fixture\n\nclass FakeRemovedServiceImpl\n")

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse(stale.exists(), "The task owns the common output and must clear stale files")
        assertEquals(
            setOf("FakeCommonAuditServiceImpl.kt"),
            fakeNames(projectDir.resolve(COMMON_OUTPUT)),
        )
    }

    @Test
    fun `GIVEN unchanged inputs WHEN rerunning THEN task is UP-TO-DATE`(@TempDir projectDir: File) {
        setupProject(projectDir, jvmSource = JVM_FIXTURE)
        runTask(projectDir, "faktGenerate")

        val second = runTask(projectDir, "faktGenerate")

        assertEquals(
            TaskOutcome.UP_TO_DATE,
            second.task(":faktGenerate")?.outcome,
            "Both outputs are declared, so the task must be incremental:\n${second.output}",
        )
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

    private fun setupProject(
        projectDir: File,
        jvmSource: String?,
        commonSource: String = COMMON_FIXTURE,
    ) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-single-target-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        projectDir.resolve("src/commonMain/kotlin/fixture").mkdirs()
        projectDir.resolve("src/commonMain/kotlin/fixture/Common.kt").writeText(commonSource)
        projectDir.resolve("src/jvmMain/kotlin/fixture").mkdirs()
        if (jvmSource != null) {
            projectDir.resolve("src/jvmMain/kotlin/fixture/Jvm.kt").writeText(jvmSource)
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
                sources.from(file("src/jvmMain/kotlin"))
                commonSources.from(file("src/commonMain/kotlin"))
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
                generatedKotlinDir.set(file("${projectDir.resolve(PLATFORM_OUTPUT).slashPath()}"))
                commonGeneratedKotlinDir.set(file("${projectDir.resolve(COMMON_OUTPUT).slashPath()}"))
                scratchDir.set(layout.buildDirectory.dir("faktCaches/jvm/main"))
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
        private const val PLATFORM_OUTPUT = "build/generated/fakt/jvm/main/kotlin"
        private const val COMMON_OUTPUT = "build/generated/fakt/commonTest/kotlin"

        private val CONTEXT =
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
                outputDirectory = "fakt://generated",
                commonTestOutputDirectory = "fakt://generated",
                outputDirectories =
                    mapOf("jvmMain" to "fakt://generated", "commonMain" to "fakt://common"),
            )

        private val COMMON_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            expect fun platformName(): String

            data class DeviceRecord(val id: String)

            @Fake
            interface CommonAuditService {
                fun record(event: String): Boolean
            }
            """
                .trimIndent()

        private val COMMON_ONLY_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            @Fake
            interface CommonAuditService {
                fun record(event: String): Boolean
            }
            """
                .trimIndent()

        private val JVM_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            actual fun platformName(): String = "JVM"

            @Fake
            interface JvmDeviceService {
                fun current(): DeviceRecord
                fun label(record: DeviceRecord): String
            }
            """
                .trimIndent()
    }
}
