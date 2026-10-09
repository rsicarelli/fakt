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
 * Gradle TestKit suite for the two JVM task shapes that own an intermediate source set without a
 * metadata compilation (#162), driven through the real worker the way the wiring configures them.
 * - **Synthetic intermediate** (`faktGenerateDesktopAndServerMain`): `desktopAndServerMain` is the
 *   emitted input, `commonMain` is analysis-only, and `desktopMain` (the representative) holds the
 *   `actual`s of `commonMain` `expect`s, so the run only succeeds when the worker passes one
 *   fragment per source set.
 * - **Consumer with a platform-owned ancestor** (`faktGenerateJvmMain`): `sharedJvmMain` is
 *   compiled by `jvm` alone, so the jvm consumer routes and emits it next to `jvmMain`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateIntermediateOwnersTest {

    @Test
    fun `GIVEN a jvm-only intermediate with an actual in the leaf WHEN running the synthetic task THEN it succeeds and writes only the intermediate fake`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, Scenario.SYNTHETIC)

        val result = runTask(projectDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse("Fakt: tolerated" in result.output, result.output)
        assertEquals(listOf("FakeSessionStoreImpl.kt"), generatedFakes(projectDir).map { it.name })
        val fake = generatedFakes(projectDir).single().readText()
        assertTrue("SessionRecord" in fake && "DeviceInfo" in fake, fake)
    }

    @Test
    fun `GIVEN the synthetic task ran once WHEN rerunning with unchanged inputs THEN the second invocation is UP-TO-DATE`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, Scenario.SYNTHETIC)
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
    fun `GIVEN identical inputs in two project dirs sharing a build cache WHEN both run THEN the second reports FROM-CACHE`(
        @TempDir projectA: File,
        @TempDir projectB: File,
        @TempDir buildCacheDir: File,
    ) {
        setupProject(projectA, Scenario.SYNTHETIC)
        configureLocalBuildCache(projectA, buildCacheDir)
        setupProject(projectB, Scenario.SYNTHETIC)
        configureLocalBuildCache(projectB, buildCacheDir)
        val first = runTask(projectA, "--build-cache")
        assertEquals(TaskOutcome.SUCCESS, first.task(":faktGenerate")?.outcome, first.output)

        val second = runTask(projectB, "--build-cache")

        assertEquals(
            TaskOutcome.FROM_CACHE,
            second.task(":faktGenerate")?.outcome,
            "Cross-directory cache hit failed.\nA:\n${first.output}\nB:\n${second.output}",
        )
        assertEquals(listOf("FakeSessionStoreImpl.kt"), generatedFakes(projectB).map { it.name })
    }

    @Test
    fun `GIVEN a platform-owned ancestor WHEN running the jvm consumer THEN it emits the ancestor and its own fake without tolerated errors`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, Scenario.CONSUMER)

        val result = runTask(projectDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse("Fakt: tolerated" in result.output, result.output)
        assertEquals(
            listOf("FakeJvmToolImpl.kt", "FakeSharedJvmThingImpl.kt"),
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

    private fun setupProject(projectDir: File, scenario: Scenario) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-intermediate-owners-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        scenario.sources.forEach { (set, files) ->
            val dir = projectDir.resolve("src/$set/kotlin/fixture").also { it.mkdirs() }
            files.forEach { (name, content) -> dir.resolve(name).writeText(content) }
        }
        projectDir.resolve("build.gradle.kts").writeText(buildScript(projectDir, scenario))
    }

    private fun buildScript(projectDir: File, scenario: Scenario): String {
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
        val outputDir = projectDir.resolve("build/generated/fakt/${scenario.outputName}/kotlin")
        val contextJson = Json.encodeToString(SourceSetContext.serializer(), scenario.context())
        val roots =
            scenario.sources.keys.joinToString("\n                ") { set ->
                """sourceSetRoots.put("$set", listOf(${quoted(projectDir, set)}))"""
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
                ${scenario.inputs(projectDir)}
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
                scratchDir.set(layout.buildDirectory.dir("faktCaches/${scenario.outputName}"))
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

    /** One fixture project: its sources per source set, task inputs and `SourceSetContext`. */
    private enum class Scenario(val outputName: String) {
        SYNTHETIC("synthetic/desktopAndServerMain") {
            override val sources =
                mapOf(
                    "commonMain" to listOf("Common.kt" to COMMON_FIXTURE, "Fake.kt" to ANNOTATION),
                    "desktopAndServerMain" to listOf("SessionStore.kt" to SESSION_STORE),
                    "desktopMain" to
                        listOf("Desktop.kt" to DESKTOP_ACTUALS, "Tool.kt" to DESKTOP_TOOL),
                )

            override fun inputs(projectDir: File) =
                """
                sources.from(file(${quoted(projectDir, "desktopAndServerMain")}))
                analysisOnlySources.from(file(${quoted(projectDir, "commonMain")}))
                platformAnalysisOnlySources.from(file(${quoted(projectDir, "desktopMain")}))
                """

            override fun context(): SourceSetContext {
                val common = SourceSetInfo("commonMain", parents = emptyList())
                val shared = SourceSetInfo("desktopAndServerMain", parents = listOf("commonMain"))
                val desktop = SourceSetInfo("desktopMain", parents = listOf("desktopAndServerMain"))
                return jvmContext(
                    "desktop",
                    desktop,
                    listOf(desktop, shared, common),
                    mapOf("desktopAndServerMain" to GENERATED),
                )
            }
        },
        CONSUMER("jvm/main") {
            override val sources =
                mapOf(
                    "commonMain" to listOf("Common.kt" to COMMON_FIXTURE, "Fake.kt" to ANNOTATION),
                    "sharedJvmMain" to
                        listOf("Shared.kt" to SHARED_JVM, "Actuals.kt" to SHARED_ACTUALS),
                    "jvmMain" to listOf("JvmTool.kt" to JVM_TOOL),
                )

            override fun inputs(projectDir: File) =
                """
                sources.from(file(${quoted(projectDir, "jvmMain")}))
                analysisOnlySources.from(
                    file(${quoted(projectDir, "commonMain")}),
                    file(${quoted(projectDir, "sharedJvmMain")})
                )
                """

            override fun context(): SourceSetContext {
                val common = SourceSetInfo("commonMain", parents = emptyList())
                val shared = SourceSetInfo("sharedJvmMain", parents = listOf("commonMain"))
                val jvm = SourceSetInfo("jvmMain", parents = listOf("sharedJvmMain"))
                return jvmContext(
                    "jvm",
                    jvm,
                    listOf(jvm, shared, common),
                    mapOf("jvmMain" to GENERATED, "sharedJvmMain" to GENERATED),
                )
            }
        };

        abstract val sources: Map<String, List<Pair<String, String>>>

        abstract fun inputs(projectDir: File): String

        abstract fun context(): SourceSetContext

        protected fun quoted(projectDir: File, sourceSet: String): String =
            "\"${projectDir.resolve("src/$sourceSet/kotlin").absolutePath.replace('\\', '/')}\""

        protected fun jvmContext(
            target: String,
            default: SourceSetInfo,
            all: List<SourceSetInfo>,
            routes: Map<String, String>,
        ) =
            SourceSetContext(
                compilationName = "main",
                targetName = target,
                platformType = "jvm",
                isTest = false,
                defaultSourceSet = default,
                allSourceSets = all,
                outputDirectory = GENERATED,
                commonTestOutputDirectory = GENERATED,
                outputDirectories = routes,
            )
    }

    companion object {
        private const val GENERATED = "fakt://generated"

        // The FIR checker matches `@Fake` by ClassId, so an in-fixture declaration stands in for
        // the `:annotations` artifact.
        private val ANNOTATION =
            """
            package com.rsicarelli.fakt

            annotation class Fake
            """

        private val COMMON_FIXTURE =
            """
            package fixture

            data class SessionRecord(val id: String)

            expect class DeviceInfo {
                fun model(): String
            }

            expect fun transport(): String
            """

        private val SESSION_STORE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            @Fake
            interface SessionStore {
                fun load(id: String): SessionRecord
                fun device(): DeviceInfo
                fun channel(): String
            }
            """

        private val DESKTOP_ACTUALS =
            """
            package fixture

            actual class DeviceInfo {
                actual fun model(): String = "desktop"
            }

            actual fun transport(): String = "desktop"
            """

        private val DESKTOP_TOOL =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            @Fake
            interface DesktopTool {
                fun run(): String
            }
            """

        private val SHARED_JVM =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            @Fake
            interface SharedJvmThing {
                fun record(): SessionRecord
                fun device(): DeviceInfo
            }
            """

        private val SHARED_ACTUALS =
            """
            package fixture

            actual class DeviceInfo {
                actual fun model(): String = "jvm"
            }

            actual fun transport(): String = "jvm"
            """

        private val JVM_TOOL =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            @Fake
            interface JvmTool {
                fun channel(): String
            }
            """
    }
}
