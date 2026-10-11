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
 * Gradle TestKit suite (#163) for the **Android variant consumer** shape of [FaktGenerateTask],
 * driven through the real worker without an Android SDK.
 *
 * KGP adds `androidMain` to every variant compilation as a **sibling** of the variant's default
 * source set (`androidDebug`), not as a `dependsOn` parent. The variant consumer therefore feeds
 * both as platform `sources` (they are compiled by the `androidJvm` compiler alone), while
 * `commonMain` is analysis-only. `androidMain` holds the `actual`s of the `commonMain` `expect`s,
 * so the run only succeeds when it sits in the platform fragment.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateAndroidVariantConsumerTest {

    @Test
    fun `GIVEN androidDebug and androidMain as platform sources WHEN running the variant consumer THEN it emits both platform fakes and no common fake`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, Fixture.PLATFORM_SOURCES)

        val result = runTask(projectDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse(TOLERATED in result.output, result.output)
        assertEquals(
            listOf("FakeAndroidOnlyImpl.kt", "FakeDebugFlagsImpl.kt"),
            generatedFakes(projectDir).map { it.name }.sorted(),
            result.output,
        )
    }

    @Test
    fun `GIVEN the variant consumer ran once WHEN rerunning with unchanged inputs THEN the second invocation is UP-TO-DATE`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, Fixture.PLATFORM_SOURCES)
        val first = runTask(projectDir)
        assertEquals(TaskOutcome.SUCCESS, first.task(":faktGenerate")?.outcome, first.output)

        val second = runTask(projectDir)

        assertEquals(
            TaskOutcome.UP_TO_DATE,
            second.task(":faktGenerate")?.outcome,
            "Expected UP-TO-DATE on rerun; got:\n${second.output}",
        )
    }

    @Test
    fun `GIVEN an empty androidDebug WHEN running the variant consumer THEN the task still runs and emits the androidMain fake`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, Fixture.EMPTY_VARIANT_SET)

        val result = runTask(projectDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse(TOLERATED in result.output, result.output)
        assertEquals(
            listOf("FakeAndroidOnlyImpl.kt"),
            generatedFakes(projectDir).map { it.name },
            result.output,
        )
    }

    @Test
    fun `GIVEN androidMain passed as analysis-only WHEN running the variant consumer THEN its actuals are tolerated errors in the common fragment`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, Fixture.ANDROID_MAIN_AS_COMMON)

        val result = runTask(projectDir)

        // Characterises the pre-#163 split (androidMain landed in the common fragment): the worker
        // tolerates the unpaired actuals and still emits the routed fakes, so the signal is the
        // tolerated errors rather than a missing fake.
        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertTrue(TOLERATED in result.output, result.output)
        assertEquals(
            listOf("FakeAndroidOnlyImpl.kt", "FakeDebugFlagsImpl.kt"),
            generatedFakes(projectDir).map { it.name }.sorted(),
            result.output,
        )
    }

    private fun runTask(projectDir: File, vararg extra: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments("faktGenerate", *extra, "--stacktrace")
            .build()

    private fun generatedFakes(projectDir: File): List<File> =
        projectDir
            .resolve("build/generated/fakt")
            .walkTopDown()
            .filter { it.isFile && it.name.startsWith("Fake") && it.extension == "kt" }
            .toList()

    private fun setupProject(projectDir: File, fixture: Fixture) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-android-variant-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        // Every source set directory exists, even when empty: the wiring resolves them all.
        SOURCE_SETS.forEach { projectDir.resolve("src/$it/kotlin/fixture").mkdirs() }
        fixture.files.forEach { (set, name, content) ->
            projectDir.resolve("src/$set/kotlin/fixture/$name").writeText(content)
        }
        projectDir.resolve("build.gradle.kts").writeText(buildScript(projectDir, fixture))
    }

    private fun buildScript(projectDir: File, fixture: Fixture): String {
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
        val outputDir = projectDir.resolve("build/generated/fakt/android/debug/kotlin")
        val contextJson = Json.encodeToString(SourceSetContext.serializer(), CONTEXT)
        val roots =
            SOURCE_SETS.joinToString("\n                ") { set ->
                """sourceSetRoots.put("$set", listOf(${quoted(projectDir, set)}))"""
            }
        val sources = fixture.platformSets.joinToString { "file(${quoted(projectDir, it)})" }
        val analysis = fixture.commonSets.joinToString { "file(${quoted(projectDir, it)})" }
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
                sources.from($sources)
                analysisOnlySources.from($analysis)
                $roots
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
                logLevel.set(LogLevel.INFO)
                imports.set(listOf<String>())
                generatedKotlinDir.set(file("${outputDir.absolutePath.replace('\\', '/')}"))
                scratchDir.set(layout.buildDirectory.dir("faktCaches/android/debug"))
            }
            """
            .trimIndent()
    }

    private fun quoted(projectDir: File, sourceSet: String): String =
        "\"${projectDir.resolve("src/$sourceSet/kotlin").absolutePath.replace('\\', '/')}\""

    private fun fileLiteral(file: File): String =
        """file("${file.absolutePath.replace('\\', '/')}")"""

    private fun workerClasspath(): List<File> =
        System.getProperty("java.class.path").split(File.pathSeparator).map(::File).filter { entry
            ->
            entry.exists() &&
                "kctfork" !in entry.absolutePath &&
                !entry.name.startsWith("kotlin-gradle-plugin")
        }

    /** One fixture: which source sets are platform `sources` vs analysis-only, and their files. */
    private enum class Fixture(
        val platformSets: List<String>,
        val commonSets: List<String>,
        val files: List<Triple<String, String, String>>,
    ) {
        PLATFORM_SOURCES(
            listOf("androidDebug", "androidMain"),
            listOf("commonMain"),
            listOf(
                Triple("commonMain", "Common.kt", COMMON),
                Triple("commonMain", "Fake.kt", ANNOTATION),
                Triple("androidMain", "Platform.android.kt", ANDROID_MAIN),
                Triple("androidDebug", "DebugFlags.kt", DEBUG_FLAGS),
            ),
        ),
        EMPTY_VARIANT_SET(
            listOf("androidDebug", "androidMain"),
            listOf("commonMain"),
            listOf(
                Triple("commonMain", "Common.kt", COMMON),
                Triple("commonMain", "Fake.kt", ANNOTATION),
                Triple("androidMain", "Platform.android.kt", ANDROID_MAIN),
            ),
        ),
        ANDROID_MAIN_AS_COMMON(
            listOf("androidDebug"),
            listOf("commonMain", "androidMain"),
            listOf(
                Triple("commonMain", "Common.kt", COMMON),
                Triple("commonMain", "Fake.kt", ANNOTATION),
                Triple("androidMain", "Platform.android.kt", ANDROID_MAIN),
                Triple("androidDebug", "DebugFlags.kt", DEBUG_FLAGS),
            ),
        ),
    }

    companion object {
        private const val GENERATED = "fakt://generated"
        private const val TOLERATED = "Fakt: tolerated compiler error"
        private val SOURCE_SETS = listOf("commonMain", "androidMain", "androidDebug")

        // androidDebug and androidMain are siblings: both refine commonMain, neither the other.
        private val CONTEXT =
            SourceSetContext(
                compilationName = "debug",
                targetName = "android",
                platformType = "androidJvm",
                isTest = false,
                defaultSourceSet = SourceSetInfo("androidDebug", parents = listOf("commonMain")),
                allSourceSets =
                    listOf(
                        SourceSetInfo("androidDebug", parents = listOf("commonMain")),
                        SourceSetInfo("androidMain", parents = listOf("commonMain")),
                        SourceSetInfo("commonMain", parents = emptyList()),
                    ),
                outputDirectory = GENERATED,
                commonTestOutputDirectory = GENERATED,
                outputDirectories = mapOf("androidDebug" to GENERATED, "androidMain" to GENERATED),
            )

        // The FIR checker matches `@Fake` by ClassId, so an in-fixture declaration stands in for
        // the `:annotations` artifact.
        private val ANNOTATION =
            """
            package com.rsicarelli.fakt

            annotation class Fake
            """

        private val COMMON =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            expect fun platformName(): String

            @Fake
            interface CommonRepo {
                fun find(id: String): String
            }
            """

        private val ANDROID_MAIN =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            actual fun platformName(): String = "android"

            @Fake
            interface AndroidOnly {
                fun label(): String
            }
            """

        private val DEBUG_FLAGS =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            @Fake
            interface DebugFlags {
                fun enabled(): Boolean
            }
            """
    }
}
