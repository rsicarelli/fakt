// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.zip.ZipFile
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
 * Gradle TestKit suite for the producer of an intermediate metadata source set (#162), the task
 * shape `faktGenerateMetadata<Set>` registers for `webMain`.
 *
 * It drives the real worker over `webMain` alone, the way the plugin wires it: `commonMain` is a
 * compiled metadata klib that arrives through both `commonKlibClasspath` and `refinesKlibs`
 * (`-Xrefines-paths`). The fixture's `webMain` uses a `commonMain` type and a `commonMain` `expect
 * class` in a `@Fake` signature and holds the `actual` of a `commonMain` `expect fun`, so any
 * redeclaration or refinement mismatch shows up as a compiler error.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateIntermediateProducerTest {

    @Test
    fun `GIVEN webMain using commonMain declarations WHEN running the producer THEN it succeeds and writes only the webMain fake`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir)

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertFalse("Fakt: tolerated" in result.output, result.output)
        assertEquals(listOf("FakeWebStorageImpl.kt"), generatedFakes(projectDir).map { it.name })
        val fake = generatedFakes(projectDir).single().readText()
        assertTrue("StorageRecord" in fake && "DeviceInfo" in fake, fake)
    }

    @Test
    fun `GIVEN the producer ran once WHEN rerunning with unchanged inputs THEN the second invocation is UP-TO-DATE`(
        @TempDir projectDir: File
    ) {
        setupProject(projectDir)
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
    fun `GIVEN identical inputs in two project dirs sharing a build cache WHEN both run THEN the second reports FROM-CACHE`(
        @TempDir projectA: File,
        @TempDir projectB: File,
        @TempDir buildCacheDir: File,
    ) {
        setupProject(projectA)
        configureLocalBuildCache(projectA, buildCacheDir)
        setupProject(projectB)
        configureLocalBuildCache(projectB, buildCacheDir)
        val first = runTask(projectA, "faktGenerate", "--build-cache")
        assertEquals(TaskOutcome.SUCCESS, first.task(":faktGenerate")?.outcome, first.output)

        val second = runTask(projectB, "faktGenerate", "--build-cache")

        assertEquals(
            TaskOutcome.FROM_CACHE,
            second.task(":faktGenerate")?.outcome,
            "Cross-directory cache hit failed.\nA:\n${first.output}\nB:\n${second.output}",
        )
        assertEquals(listOf("FakeWebStorageImpl.kt"), generatedFakes(projectB).map { it.name })
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

    private fun setupProject(projectDir: File) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-intermediate-producer-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        val web = projectDir.resolve("src/webMain/kotlin/fixture").also { it.mkdirs() }
        web.resolve("WebStorage.kt").writeText(WEB_STORAGE_FIXTURE)
        web.resolve("Runtime.web.kt").writeText(WEB_ACTUAL_FIXTURE)
        projectDir.resolve("build.gradle.kts").writeText(buildScript(projectDir))
    }

    private fun buildScript(projectDir: File): String {
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
        val outputDir = projectDir.resolve("build/generated/fakt/metadata/webMain/kotlin")
        val contextJson = Json.encodeToString(SourceSetContext.serializer(), webContext())
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
                sources.from(file("src/webMain/kotlin"))
                commonKlibClasspath.from(
                    ${fileLiteral(stdlibCommonKlibDir)},
                    ${fileLiteral(commonMainKlibDir)}
                )
                refinesKlibs.from(${fileLiteral(commonMainKlibDir)})
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
                scratchDir.set(layout.buildDirectory.dir("faktCaches/metadata/webMain"))
            }
            """
            .trimIndent()
    }

    private fun webContext(): SourceSetContext {
        val common = SourceSetInfo("commonMain", parents = emptyList())
        val web = SourceSetInfo("webMain", parents = listOf("commonMain"))
        return SourceSetContext(
            compilationName = "webMain",
            targetName = "metadata",
            platformType = "common",
            isTest = false,
            defaultSourceSet = web,
            allSourceSets = listOf(web, common),
            outputDirectory = "fakt://generated",
            commonTestOutputDirectory = "fakt://generated",
            outputDirectories = mapOf("webMain" to "fakt://generated"),
        )
    }

    private fun fileLiteral(file: File): String =
        """file("${file.absolutePath.replace('\\', '/')}")"""

    private fun workerClasspath(): List<File> =
        System.getProperty("java.class.path").split(File.pathSeparator).map(::File).filter { entry
            ->
            entry.exists() &&
                "kctfork" !in entry.absolutePath &&
                !entry.name.startsWith("kotlin-gradle-plugin")
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

        /**
         * The fixture's `commonMain`, compiled once to the metadata klib KGP would hand a `webMain`
         * compilation. Shared read-only by every test, so two project dirs see byte-identical
         * klibs.
         */
        private val commonMainKlibDir: File by lazy {
            val root = Files.createTempDirectory("fakt-common-main").toFile()
            val sources = root.resolve("src").also { it.mkdirs() }
            sources.resolve("Common.kt").writeText(COMMON_FIXTURE)
            sources.resolve("Fake.kt").writeText(ANNOTATION_FIXTURE)
            val output = root.resolve("klib").also { it.mkdirs() }
            val errors = ByteArrayOutputStream()
            val exit =
                compileMetadata(
                    errors,
                    "-d",
                    output.absolutePath,
                    "-classpath",
                    stdlibCommonKlibDir.absolutePath,
                    "-module-name",
                    "commonMain",
                    "-Xmulti-platform",
                    "-Xexpect-actual-classes",
                    sources.resolve("Common.kt").absolutePath,
                    sources.resolve("Fake.kt").absolutePath,
                )
            check(exit == "OK") { "commonMain fixture did not compile ($exit): $errors" }
            output
        }

        /**
         * Runs `KotlinMetadataCompiler` from an isolated classloader (the test JVM also carries
         * KGP, whose bundled compiler references clash with an in-process compiler) and returns the
         * exit code name.
         */
        private fun compileMetadata(errors: ByteArrayOutputStream, vararg args: String): String {
            val compilerClasspath =
                System.getProperty("java.class.path")
                    .split(File.pathSeparator)
                    .map(::File)
                    .filter { entry ->
                        entry.exists() &&
                            "kctfork" !in entry.absolutePath &&
                            !entry.name.startsWith("kotlin-gradle-plugin")
                    }
            URLClassLoader(
                    compilerClasspath.map { it.toURI().toURL() }.toTypedArray(),
                    ClassLoader.getPlatformClassLoader(),
                )
                .use { loader ->
                    val compiler =
                        loader.loadClass("org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler")
                    val exec =
                        compiler.getMethod(
                            "exec",
                            PrintStream::class.java,
                            Array<String>::class.java,
                        )
                    val exit =
                        exec.invoke(
                            compiler.getConstructor().newInstance(),
                            PrintStream(errors),
                            args,
                        )
                    return (exit as Enum<*>).name
                }
        }

        // The FIR checker matches `@Fake` by ClassId, so an in-fixture declaration stands in for
        // the `:annotations` klib.
        private val ANNOTATION_FIXTURE =
            """
            package com.rsicarelli.fakt

            annotation class Fake
            """
                .trimIndent()

        private val COMMON_FIXTURE =
            """
            package fixture

            data class StorageRecord(val key: String, val size: Int)

            expect class DeviceInfo {
                fun model(): String
            }

            expect fun runtimeFamily(): String
            """
                .trimIndent()

        private val WEB_STORAGE_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            @Fake
            interface WebStorage {
                fun read(key: String): StorageRecord
                fun device(): DeviceInfo
                fun family(): String
            }
            """
                .trimIndent()

        private val WEB_ACTUAL_FIXTURE =
            """
            package fixture

            actual fun runtimeFamily(): String = "web"
            """
                .trimIndent()
    }
}
