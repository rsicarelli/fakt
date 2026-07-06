// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Gradle TestKit suite for the source-partitioned **consumer** shape of [FaktGenerateTask]: a
 * drivable platform main (`jvmMain`) whose `sources` carry only its own source set while ancestor
 * sources (`commonMain`) ride along in `analysisOnlySources`.
 *
 * The worker marks the ancestors `-Xcommon-sources` under `-Xmulti-platform`, so platform `actual`
 * declarations pair with their commonMain `expect`s — real KMP platform source sets almost always
 * contain actuals, and without the pairing the frontend rejects the `actual` keyword outright.
 * Emission stays restricted to the consumer's own source set (`SourceSetContext.emitSourceSets`):
 * the common producer owns the ancestors' fakes, so nothing is generated twice.
 *
 * Worker classpath is built from the test JVM's own classpath with KGP and kctfork filtered out,
 * for the same dangling-compiler-reference reasons documented in [FaktGenerateTaskTest].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateConsumerTest {

    @Test
    fun `GIVEN jvmMain with actual declarations WHEN running consumer THEN task succeeds and pairs the expects`(
        @TempDir projectDir: File
    ) {
        setupConsumerProject(projectDir)

        val result = runTask(projectDir, "faktGenerate")

        // Platform actuals used to kill the consumer: without -Xmulti-platform the frontend
        // rejects the `actual` keyword, and ACTUAL_WITHOUT_EXPECT is an error even with it unless
        // the expect side is fed as -Xcommon-sources.
        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":faktGenerate")?.outcome,
            "actual declarations in jvmMain must not fail the consumer; output:\n${result.output}",
        )
    }

    @Test
    fun `GIVEN jvm fake referencing a common type WHEN running consumer THEN only the platform fake is emitted`(
        @TempDir projectDir: File
    ) {
        setupConsumerProject(projectDir)

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        val names = generatedFakes(projectDir).map { it.name }
        assertTrue(
            "FakeJvmDeviceServiceImpl.kt" in names,
            "The consumer must emit its own platform fake; got $names\n${result.output}",
        )
        assertTrue(
            names.none { "CommonAuditService" in it },
            "commonMain @Fake declarations are analysis-only for consumers — the common " +
                "producer owns their fakes; got $names",
        )
        val jvmFake = generatedFakes(projectDir).single { it.name == "FakeJvmDeviceServiceImpl.kt" }
        assertTrue(
            "DeviceRecord" in jvmFake.readText(),
            "The platform fake must render the commonMain type:\n${jvmFake.readText()}",
        )
    }

    private fun runTask(projectDir: File, vararg arguments: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments(*arguments, "--stacktrace")
            .build()

    private fun generatedFakes(projectDir: File): List<File> =
        projectDir
            .resolve("build/generated/fakt")
            .walkTopDown()
            .filter { it.isFile && it.name.startsWith("Fake") && it.extension == "kt" }
            .toList()

    private fun setupConsumerProject(projectDir: File) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-consumer-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        projectDir.resolve("src/commonMain/kotlin/fixture").mkdirs()
        projectDir.resolve("src/commonMain/kotlin/fixture/Common.kt").writeText(COMMON_FIXTURE)
        projectDir.resolve("src/jvmMain/kotlin/fixture").mkdirs()
        projectDir.resolve("src/jvmMain/kotlin/fixture/Jvm.kt").writeText(JVM_FIXTURE)
        projectDir.resolve("build.gradle.kts").writeText(buildScriptForTask(projectDir))
    }

    private fun buildScriptForTask(projectDir: File): String {
        val outputDir = projectDir.resolve("build/generated/fakt/jvm/main/kotlin")
        val sourceSetContextJson =
            json.encodeToString(SourceSetContext.serializer(), CONSUMER_CONTEXT)
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
                analysisOnlySources.from(file("src/commonMain/kotlin"))
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
                generatedKotlinDir.set(file("${outputDir.absolutePath.replace('\\', '/')}"))
                scratchDir.set(layout.buildDirectory.dir("faktCaches/jvm/main"))
            }

            tasks.register("clean") { doLast { delete(layout.buildDirectory) } }
            """
            .trimIndent()
    }

    private fun workerClasspath(): List<File> =
        System.getProperty("java.class.path").split(File.pathSeparator).map(::File).filter { entry
            ->
            entry.exists() &&
                "kctfork" !in entry.absolutePath &&
                !entry.name.startsWith("kotlin-gradle-plugin")
        }

    private val json = Json { prettyPrint = false }

    companion object {
        private val CONSUMER_CONTEXT =
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
                commonTestOutputDirectory = "/tmp/fakt-spike/commonTest",
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
