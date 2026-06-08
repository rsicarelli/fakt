// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import com.rsicarelli.fakt.compiler.fir.cache.MetadataCacheSerializer
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Gradle TestKit suite for the KMP common-producer path of [FaktGenerateTask]. Tests register the
 * task with `platformType = "common"` so the worker drives `K2JVMCompiler` over `commonMain`
 * sources in multiplatform mode, writing platform-agnostic fakes into the task's
 * `@OutputDirectory`. This is how Fakt generates `commonMain` fakes cache-correctly: every target's
 * test compilation later consumes that one declared directory as ordinary source.
 *
 * Worker classpath is built from the test JVM's own classpath with KGP and kctfork filtered out,
 * for the same dangling-compiler-reference reasons documented in [FaktGenerateTaskTest].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateCommonProducerTest {

    @Test
    fun `GIVEN commonMain fixture with one fake interface WHEN running in common mode THEN a platform-agnostic fake kt is written`(
        @TempDir projectDir: File
    ) {
        setupCommonProject(projectDir, commonSource = SINGLE_INTERFACE_FIXTURE)

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        val generated = generatedFakes(projectDir)
        assertTrue(
            generated.isNotEmpty(),
            "Expected a generated Fake*.kt under build/generated/fakt; output:\n${result.output}",
        )
        val leaked = generated.filter(::leaksJvmOnlyImports)
        assertTrue(
            leaked.isEmpty(),
            "Generated common fakes leaked JVM-only imports (would not compile on Native/JS): " +
                "${leaked.map { it.name }}\n${leaked.joinToString("\n---\n") { it.readText() }}",
        )
    }

    @Test
    fun `GIVEN producer mode over commonMain WHEN running THEN firMetadataFile deserialises with the validated interface`(
        @TempDir projectDir: File
    ) {
        val cacheFile = projectDir.resolve("fir-metadata.json")
        setupCommonProject(
            projectDir,
            commonSource = SINGLE_INTERFACE_FIXTURE,
            firMetadataFile = cacheFile,
        )

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertTrue(
            cacheFile.exists(),
            "Producer firMetadataFile not written: ${cacheFile.absolutePath}",
        )
        val cache = MetadataCacheSerializer.deserialize(cacheFile.absolutePath)
        assertNotNull(cache, "Cache failed to deserialise:\n${cacheFile.readText()}")
        assertEquals(
            "SessionService",
            cache.interfaces.single().simpleName,
            "Producer cache must include the validated @Fake interface; got ${cache.interfaces.map { it.simpleName }}",
        )
    }

    @Test
    fun `GIVEN commonMain with an expect declaration and jvmMain actuals WHEN running THEN task succeeds and the common fake is generated`(
        @TempDir projectDir: File
    ) {
        setupCommonProject(
            projectDir,
            commonSource = EXPECT_PLUS_FIXTURE,
            jvmActualSource = JVM_ACTUALS,
        )

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":faktGenerate")?.outcome,
            "Driving K2JVM over commonMain+jvmMain with actuals present must succeed; output:\n${result.output}",
        )
        assertTrue(
            generatedFakes(projectDir).any { it.name.contains("SessionService") },
            "Expected FakeSessionService*.kt; output:\n${result.output}",
        )
    }

    @Test
    fun `GIVEN commonMain with an expect declaration but no actual WHEN running THEN task fails with a clear missing-actual error`(
        @TempDir projectDir: File
    ) {
        setupCommonProject(projectDir, commonSource = EXPECT_PLUS_FIXTURE)

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .forwardOutput()
                .withArguments("faktGenerate", "--stacktrace")
                .buildAndFail()

        // Driving a JVM platform compile over common-only sources cannot satisfy `expect`
        // declarations that have no `actual`. This is an inherent boundary, locked here so a future
        // change that silently swallows it is caught. The robust path (commonMain + platform
        // actuals)
        // is covered by the test above.
        assertTrue(
            "no actual declaration" in result.output || "NO_ACTUAL_FOR_EXPECT" in result.output,
            "Expected a missing-actual error; output:\n${result.output}",
        )
    }

    @Test
    fun `GIVEN task ran once WHEN rerunning with unchanged inputs THEN second invocation is UP-TO-DATE`(
        @TempDir projectDir: File
    ) {
        setupCommonProject(projectDir, commonSource = SINGLE_INTERFACE_FIXTURE)

        val first = runTask(projectDir, "faktGenerate")
        assertEquals(TaskOutcome.SUCCESS, first.task(":faktGenerate")?.outcome, first.output)

        val second = runTask(projectDir, "faktGenerate")
        assertEquals(
            TaskOutcome.UP_TO_DATE,
            second.task(":faktGenerate")?.outcome,
            "Expected UP-TO-DATE on rerun; got:\n${second.output}",
        )
    }

    @Test
    fun `GIVEN identical common inputs in two project dirs sharing a build cache WHEN both run THEN second reports FROM-CACHE`(
        @TempDir projectA: File,
        @TempDir projectB: File,
        @TempDir buildCacheDir: File,
    ) {
        setupCommonProject(projectA, commonSource = SINGLE_INTERFACE_FIXTURE)
        configureLocalBuildCache(projectA, buildCacheDir)
        setupCommonProject(projectB, commonSource = SINGLE_INTERFACE_FIXTURE)
        configureLocalBuildCache(projectB, buildCacheDir)

        val first = runTask(projectA, "faktGenerate", "--build-cache")
        assertEquals(TaskOutcome.SUCCESS, first.task(":faktGenerate")?.outcome, first.output)

        val second = runTask(projectB, "faktGenerate", "--build-cache")
        assertEquals(
            TaskOutcome.FROM_CACHE,
            second.task(":faktGenerate")?.outcome,
            "Cross-directory cache hit failed — likely an absolute-path input slipped through (relocation canary).\nA:\n${first.output}\nB:\n${second.output}",
        )
    }

    @Test
    fun `GIVEN producer mode WHEN running twice in two dirs THEN firMetadataFile is byte-identical across runs`(
        @TempDir projectA: File,
        @TempDir projectB: File,
    ) {
        val cacheA = projectA.resolve("fir-metadata.json")
        setupCommonProject(
            projectA,
            commonSource = SINGLE_INTERFACE_FIXTURE,
            firMetadataFile = cacheA,
        )
        runTask(projectA, "faktGenerate").also {
            assertEquals(TaskOutcome.SUCCESS, it.task(":faktGenerate")?.outcome, it.output)
        }

        val cacheB = projectB.resolve("fir-metadata.json")
        setupCommonProject(
            projectB,
            commonSource = SINGLE_INTERFACE_FIXTURE,
            firMetadataFile = cacheB,
        )
        runTask(projectB, "faktGenerate").also {
            assertEquals(TaskOutcome.SUCCESS, it.task(":faktGenerate")?.outcome, it.output)
        }

        assertContentEquals(
            cacheA.readBytes(),
            cacheB.readBytes(),
            "firMetadataFile contents differ across runs — non-deterministic serializer leaks build-machine state",
        )
    }

    @Test
    fun `GIVEN cache populated WHEN running clean with --build-cache THEN second run reports FROM-CACHE and restores generated kt`(
        @TempDir projectDir: File,
        @TempDir buildCacheDir: File,
    ) {
        setupCommonProject(projectDir, commonSource = SINGLE_INTERFACE_FIXTURE)
        configureLocalBuildCache(projectDir, buildCacheDir)

        val first = runTask(projectDir, "faktGenerate", "--build-cache", "clean")
        assertEquals(TaskOutcome.SUCCESS, first.task(":faktGenerate")?.outcome, first.output)

        runTask(projectDir, "clean")

        val second = runTask(projectDir, "faktGenerate", "--build-cache")
        assertEquals(
            TaskOutcome.FROM_CACHE,
            second.task(":faktGenerate")?.outcome,
            "Expected FROM-CACHE; first:\n${first.output}\nsecond:\n${second.output}",
        )
        assertTrue(
            generatedFakes(projectDir).isNotEmpty(),
            "Cache restore must replace generated .kt files (issue #79 regression check, common path).",
        )
    }

    @Test
    fun `GIVEN commonMain fixture with no fake declarations WHEN running THEN task succeeds with empty output`(
        @TempDir projectDir: File
    ) {
        setupCommonProject(projectDir, commonSource = NO_FAKE_FIXTURE)

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        val generated =
            projectDir
                .resolve("build/generated/fakt")
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()
        assertEquals(emptyList(), generated, "Expected no generated files")
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

    private fun leaksJvmOnlyImports(file: File): Boolean =
        file.readText().lineSequence().any { line ->
            line.trimStart().startsWith("import java.") ||
                line.trimStart().startsWith("import javax.")
        }

    private fun setupCommonProject(
        projectDir: File,
        commonSource: String,
        jvmActualSource: String? = null,
        firMetadataFile: File? = null,
    ) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-common-producer-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        projectDir.resolve("src/commonMain/kotlin/fixture").mkdirs()
        projectDir.resolve("src/commonMain/kotlin/fixture/Fixture.kt").writeText(commonSource)
        if (jvmActualSource != null) {
            projectDir.resolve("src/jvmMain/kotlin/fixture").mkdirs()
            projectDir.resolve("src/jvmMain/kotlin/fixture/Actuals.kt").writeText(jvmActualSource)
        }
        projectDir
            .resolve("build.gradle.kts")
            .writeText(buildScriptForTask(projectDir, jvmActualSource != null, firMetadataFile))
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

    private fun buildScriptForTask(
        projectDir: File,
        includeJvmMain: Boolean,
        firMetadataFile: File?,
    ): String {
        val outputDir = projectDir.resolve("build/generated/fakt/metadata/commonMain/kotlin")
        val sourceSetContextJson =
            json.encodeToString(SourceSetContext.serializer(), COMMON_CONTEXT)
        val sourcesLiteral =
            if (includeJvmMain) {
                """sources.from(file("src/commonMain/kotlin"), file("src/jvmMain/kotlin"))"""
            } else {
                """sources.from(file("src/commonMain/kotlin"))"""
            }
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
                $sourcesLiteral
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
                scratchDir.set(layout.buildDirectory.dir("faktCaches/metadata/commonMain"))
                ${firMetadataFile?.let { "firMetadataFile.set(file(\"${it.absolutePath.replace('\\', '/')}\"))" } ?: ""}
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
        private val COMMON_CONTEXT =
            SourceSetContext(
                compilationName = "commonMain",
                targetName = "metadata",
                platformType = "common",
                isTest = false,
                defaultSourceSet = SourceSetInfo("commonMain", parents = emptyList()),
                allSourceSets = listOf(SourceSetInfo("commonMain", parents = emptyList())),
                outputDirectory = "/tmp/fakt-common-producer/commonTest",
                commonTestOutputDirectory = "/tmp/fakt-common-producer/commonTest",
            )

        private val SINGLE_INTERFACE_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            @Fake
            interface SessionService {
                suspend fun login(user: String): String
                fun activeUsers(): List<String>
                val isOnline: Boolean
            }
            """
                .trimIndent()

        private val EXPECT_PLUS_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            expect class PlatformClock {
                fun now(): Long
            }

            expect fun platformName(): String

            @Fake
            interface SessionService {
                suspend fun login(user: String): String
                fun activeUsers(): List<String>
                val isOnline: Boolean
            }
            """
                .trimIndent()

        private val JVM_ACTUALS =
            """
            package fixture

            actual class PlatformClock {
                actual fun now(): Long = 0L
            }

            actual fun platformName(): String = "jvm"
            """
                .trimIndent()

        private val NO_FAKE_FIXTURE =
            """
            package fixture

            object Marker
            """
                .trimIndent()
    }
}
