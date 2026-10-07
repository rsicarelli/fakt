// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Gradle TestKit suite for [FaktGenerateTask] over an **intermediate** source set (issue #162):
 * `commonMain` -> `webMain` -> platform main, where `webMain` holds the `actual` of a `commonMain`
 * `expect`.
 *
 * Lumping `commonMain` and `webMain` into one `-Xcommon-sources` module makes that `actual` a hard
 * error ("declared in the same module"); the worker therefore passes one `-Xfragments` entry per
 * source set when the task knows the source roots ([FaktGenerateTask.sourceSetRoots]). The metadata
 * driver rejects fragments outright, so there the same relation travels as `-Xrefines-paths` from
 * [FaktGenerateTask.refinesKlibs].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateIntermediateFragmentsTest {

    @Test
    fun `GIVEN webMain actual for a commonMain expect WHEN running the js consumer THEN succeeds without tolerated errors`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, Flavor.JS)

        val result = runTask(projectDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse("Fakt: tolerated" in result.output, result.output)
        assertPlatformFakeOnly(projectDir, "FakeJsDeviceServiceImpl.kt", result)
    }

    @Test
    fun `GIVEN webMain actual for a commonMain expect WHEN running the jvm consumer THEN succeeds without tolerated errors`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, Flavor.JVM)

        val result = runTask(projectDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse("Fakt: tolerated" in result.output, result.output)
        assertPlatformFakeOnly(projectDir, "FakeJvmDeviceServiceImpl.kt", result)
    }

    @Test
    fun `GIVEN refines klibs and source roots WHEN running the metadata producer THEN no fragments are passed and the fake is emitted`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir, Flavor.METADATA)

        val result = runTask(projectDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse("Fakt: tolerated" in result.output, result.output)
        val names = generatedFakes(projectDir).map { it.name }
        assertEquals(listOf("FakeWebThingImpl.kt"), names, result.output)
    }

    private fun assertPlatformFakeOnly(projectDir: File, expected: String, result: BuildResult) {
        val names = generatedFakes(projectDir).map { it.name }
        assertEquals(listOf(expected), names, "Only the platform fake is emitted\n${result.output}")
    }

    private fun runTask(projectDir: File): BuildResult =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments("faktGenerate", "--stacktrace")
            .build()

    private fun generatedFakes(projectDir: File): List<File> =
        projectDir
            .resolve("build/generated/fakt")
            .walkTopDown()
            .filter { it.isFile && it.name.startsWith("Fake") && it.extension == "kt" }
            .toList()

    private fun setupProject(projectDir: File, flavor: Flavor) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-intermediate-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        write(projectDir, "commonMain", "Common.kt", commonFixture(flavor))
        write(projectDir, "commonMain", "Fake.kt", ANNOTATION_FIXTURE)
        write(projectDir, "webMain", "Web.kt", webFixture(flavor))
        if (flavor != Flavor.METADATA) {
            write(projectDir, flavor.defaultSet, "Platform.kt", platformFixture(flavor))
        }
        projectDir.resolve("build.gradle.kts").writeText(buildScript(projectDir, flavor))
    }

    private fun write(projectDir: File, sourceSet: String, name: String, content: String) {
        val dir = projectDir.resolve("src/$sourceSet/kotlin/fixture").also { it.mkdirs() }
        dir.resolve(name).writeText(content)
    }

    private fun buildScript(projectDir: File, flavor: Flavor): String {
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
        val outputDir = projectDir.resolve("build/generated/fakt/${flavor.targetName}/main/kotlin")
        val json = Json { prettyPrint = false }
        val contextJson = json.encodeToString(SourceSetContext.serializer(), contextFor(flavor))
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
                ${flavorConfig(projectDir, flavor, classpathLiteral)}
                sourceSetRoots.put("commonMain", listOf(${quoted(projectDir, "commonMain")}))
                sourceSetRoots.put("webMain", listOf(${quoted(projectDir, "webMain")}))
                sourceSetRoots.put("${flavor.defaultSet}", listOf(${quoted(projectDir, flavor.defaultSet)}))
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
                scratchDir.set(layout.buildDirectory.dir("faktCaches/${flavor.targetName}/main"))
            }
            """
            .trimIndent()
    }

    private fun quoted(projectDir: File, sourceSet: String): String {
        val dir = projectDir.resolve("src/$sourceSet/kotlin")
        return "\"${dir.absolutePath.replace('\\', '/')}\""
    }

    private fun flavorConfig(projectDir: File, flavor: Flavor, classpathLiteral: String): String {
        val platformDir = "file(${quoted(projectDir, flavor.defaultSet)})"
        val commonDir = "file(${quoted(projectDir, "commonMain")})"
        val webDir = "file(${quoted(projectDir, "webMain")})"
        return when (flavor) {
            Flavor.JS ->
                """
                sources.from($platformDir)
                analysisOnlySources.from($commonDir, $webDir)
                commonKlibClasspath.from(${fileLiteral(File(requireProperty("fakt.test.stdlibJsKlib")))})
                """
            Flavor.JVM ->
                """
                sources.from($platformDir)
                analysisOnlySources.from($commonDir, $webDir)
                compileClasspath.from(
                    $classpathLiteral
                )
                """
            Flavor.METADATA ->
                """
                sources.from($webDir)
                analysisOnlySources.from($commonDir)
                commonKlibClasspath.from(${fileLiteral(stdlibCommonKlibDir)})
                refinesKlibs.from(${fileLiteral(stdlibCommonKlibDir)})
                """
        }
    }

    private fun contextFor(flavor: Flavor): SourceSetContext {
        val common = SourceSetInfo("commonMain", parents = emptyList())
        val web = SourceSetInfo("webMain", parents = listOf("commonMain"))
        val default =
            if (flavor == Flavor.METADATA) web
            else SourceSetInfo(flavor.defaultSet, parents = listOf("webMain"))
        return SourceSetContext(
            compilationName = "main",
            targetName = flavor.targetName,
            platformType = flavor.platformType,
            isTest = false,
            defaultSourceSet = default,
            allSourceSets = listOf(default, web, common).distinct(),
            outputDirectory = "fakt://generated",
            commonTestOutputDirectory = "fakt://generated",
            outputDirectories = mapOf(default.name to "fakt://generated"),
        )
    }

    private fun fileLiteral(file: File): String =
        """file("${file.absolutePath.replace('\\', '/')}")"""

    private fun requireProperty(name: String): String =
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

    private enum class Flavor(
        val targetName: String,
        val platformType: String,
        val defaultSet: String,
    ) {
        JS("js", "js", "jsMain"),
        JVM("jvm", "jvm", "jvmMain"),
        METADATA("metadata", "common", "webMain"),
    }

    companion object {
        /** The stdlib `commonMain` metadata klib, unpacked once from the `-all` archive. */
        private val stdlibCommonKlibDir: File by lazy {
            val jarPath = requireNotNull(System.getProperty("fakt.test.stdlibMetadataJar"))
            val targetRoot = Files.createTempDirectory("fakt-stdlib-metadata").toFile()
            ZipFile(jarPath).use { zip ->
                zip.entries()
                    .asSequence()
                    .filter { it.name.startsWith("commonMain/") }
                    .forEach { entry ->
                        val target = targetRoot.resolve(entry.name)
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                target.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                    }
            }
            targetRoot.resolve("commonMain")
        }

        private fun platformFixture(flavor: Flavor): String {
            val name = flavor.defaultSet.removeSuffix("Main").replaceFirstChar { it.uppercase() }
            return """
                package fixture

                import com.rsicarelli.fakt.Fake

                @Fake
                interface ${name}DeviceService {
                    fun current(): DeviceRecord
                    fun source(): String
                }
                """
                .trimIndent()
        }

        // The FIR checker matches `@Fake` by ClassId, so an in-fixture declaration stands in for
        // the `:annotations` klib on the klib-driven flavours.
        private val ANNOTATION_FIXTURE =
            """
            package com.rsicarelli.fakt

            annotation class Fake
            """
                .trimIndent()

        // The metadata driver has no IR actualizer and compiles the intermediate set alone, so its
        // fixture carries no expect/actual pair (an `actual` would lack its `expect`).
        private fun commonFixture(flavor: Flavor): String {
            val expectLine =
                if (flavor == Flavor.METADATA) "" else "expect fun platformName(): String"
            return """
                package fixture

                $expectLine

                data class DeviceRecord(val id: String)
                """
                .trimIndent()
        }

        private fun webFixture(flavor: Flavor): String {
            val actualLine =
                if (flavor == Flavor.METADATA) ""
                else """actual fun platformName(): String = "Web""""
            return """
                package fixture

                import com.rsicarelli.fakt.Fake

                $actualLine

                @Fake
                interface WebThing {
                    fun current(): DeviceRecord
                }
                """
                .trimIndent()
        }
    }
}
