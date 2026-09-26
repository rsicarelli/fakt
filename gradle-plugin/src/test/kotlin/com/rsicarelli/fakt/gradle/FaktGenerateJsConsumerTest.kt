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
 * Gradle TestKit suite for the Kotlin/JS and Kotlin/Wasm **consumer** shape of [FaktGenerateTask]
 * (issue #151): a `jsMain` / `wasmJsMain` platform main driven by `K2JSCompiler` instead of the
 * in-process compiler plugin.
 *
 * Mirrors [FaktGenerateConsumerTest]: the platform main's own sources go in `sources`, `commonMain`
 * rides along in `analysisOnlySources` (so `actual`s pair with their `expect`s), and only the
 * platform `@Fake` is emitted. Dependencies are platform **klibs** fed through the klib input — the
 * stdlib JS / Wasm klibs are resolved by the build (`stdlibWebKlibsForTests` in
 * `gradle-plugin/build.gradle.kts`) and their paths passed as system properties. The `@Fake`
 * annotation is declared in-fixture because the `:annotations` JS/Wasm klibs aren't on the test
 * classpath; the real klibs are exercised by the `kmp-multi-target` / `kmp-no-jvm` sample CI cells.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateJsConsumerTest {

    @Test
    fun `GIVEN jsMain with actual declarations WHEN running consumer THEN emits only the platform fake`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, JS)

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertPlatformFakeOnly(projectDir, result)
    }

    @Test
    fun `GIVEN wasmJsMain with actual declarations WHEN running consumer THEN emits only the platform fake`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, WASM_JS)

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertPlatformFakeOnly(projectDir, result)
    }

    @Test
    fun `GIVEN unchanged inputs WHEN rerunning the js consumer THEN task is UP-TO-DATE`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, JS)
        runTask(projectDir, "faktGenerate")

        val second = runTask(projectDir, "faktGenerate")

        assertEquals(
            TaskOutcome.UP_TO_DATE,
            second.task(":faktGenerate")?.outcome,
            "The klib-driven consumer must be incremental like the JVM one:\n${second.output}",
        )
    }

    private fun assertPlatformFakeOnly(projectDir: File, result: BuildResult) {
        val fakes = generatedFakes(projectDir)
        val names = fakes.map { it.name }
        assertTrue(
            "FakeWebDeviceServiceImpl.kt" in names,
            "The consumer must emit its own platform fake; got $names\n${result.output}",
        )
        assertTrue(
            names.none { "CommonAuditService" in it },
            "commonMain @Fake declarations are analysis-only for consumers; got $names",
        )
        val platformFake = fakes.single { it.name == "FakeWebDeviceServiceImpl.kt" }
        assertTrue(
            "DeviceRecord" in platformFake.readText(),
            "The platform fake must render the commonMain type:\n${platformFake.readText()}",
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

    private fun setupProject(projectDir: File, platform: WebPlatform) {
        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "fakt-js-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        projectDir.resolve("src/commonMain/kotlin/fixture").mkdirs()
        projectDir.resolve("src/commonMain/kotlin/fixture/Common.kt").writeText(COMMON_FIXTURE)
        projectDir.resolve("src/commonMain/kotlin/fixture/Fake.kt").writeText(ANNOTATION_FIXTURE)
        val platformDir = projectDir.resolve("src/${platform.sourceSet}/kotlin/fixture")
        platformDir.mkdirs()
        platformDir.resolve("Web.kt").writeText(WEB_FIXTURE)
        projectDir.resolve("build.gradle.kts").writeText(buildScript(projectDir, platform))
    }

    private fun buildScript(projectDir: File, platform: WebPlatform): String {
        val outputDir =
            projectDir.resolve("build/generated/fakt/${platform.targetName}/main/kotlin")
        val context = contextFor(platform)
        val sourceSetContextJson = json.encodeToString(SourceSetContext.serializer(), context)
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
        val stdlibKlib = File(requireSystemProperty(platform.stdlibProperty))
        val wasmTargetLine =
            platform.wasmTarget?.let { """wasmTarget.set("$it")""" }
                ?: "// Kotlin/JS: no wasmTarget"
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
                sources.from(file("src/${platform.sourceSet}/kotlin"))
                analysisOnlySources.from(file("src/commonMain/kotlin"))
                commonKlibClasspath.from(${fileLiteral(stdlibKlib)})
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
                $wasmTargetLine
                generatedKotlinDir.set(file("${outputDir.absolutePath.replace('\\', '/')}"))
                scratchDir.set(layout.buildDirectory.dir("faktCaches/${platform.targetName}/main"))
            }
            """
            .trimIndent()
    }

    private fun contextFor(platform: WebPlatform): SourceSetContext {
        val platformSourceSet = SourceSetInfo(platform.sourceSet, parents = listOf("commonMain"))
        return SourceSetContext(
            compilationName = "main",
            targetName = platform.targetName,
            platformType = platform.platformType,
            isTest = false,
            defaultSourceSet = platformSourceSet,
            allSourceSets =
                listOf(platformSourceSet, SourceSetInfo("commonMain", parents = emptyList())),
            outputDirectory = "fakt://generated",
            commonTestOutputDirectory = "fakt://generated",
        )
    }

    private fun fileLiteral(file: File): String =
        """file("${file.absolutePath.replace('\\', '/')}")"""

    private fun requireSystemProperty(name: String): String =
        requireNotNull(System.getProperty(name)) {
            "$name system property not set — see the test task in gradle-plugin/build.gradle.kts."
        }

    private fun workerClasspath(): List<File> =
        System.getProperty("java.class.path").split(File.pathSeparator).map(::File).filter { entry
            ->
            entry.exists() &&
                "kctfork" !in entry.absolutePath &&
                !entry.name.startsWith("kotlin-gradle-plugin")
        }

    private val json = Json { prettyPrint = false }

    /** A Kotlin/JS or Kotlin/Wasm platform main as KGP would describe it. */
    private data class WebPlatform(
        val targetName: String,
        val platformType: String,
        val sourceSet: String,
        val stdlibProperty: String,
        val wasmTarget: String?,
    )

    companion object {
        private val JS =
            WebPlatform(
                targetName = "js",
                platformType = "js",
                sourceSet = "jsMain",
                stdlibProperty = "fakt.test.stdlibJsKlib",
                wasmTarget = null,
            )

        private val WASM_JS =
            WebPlatform(
                targetName = "wasmJs",
                platformType = "wasm",
                sourceSet = "wasmJsMain",
                stdlibProperty = "fakt.test.stdlibWasmJsKlib",
                wasmTarget = "wasm-js",
            )

        // The FIR checker matches `@Fake` by ClassId, so an in-fixture declaration stands in for
        // the `:annotations` JS/Wasm klib.
        private val ANNOTATION_FIXTURE =
            """
            package com.rsicarelli.fakt

            annotation class Fake
            """
                .trimIndent()

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

        private val WEB_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            actual fun platformName(): String = "Web"

            @Fake
            interface WebDeviceService {
                fun current(): DeviceRecord
                fun label(record: DeviceRecord): String
            }
            """
                .trimIndent()
    }
}
