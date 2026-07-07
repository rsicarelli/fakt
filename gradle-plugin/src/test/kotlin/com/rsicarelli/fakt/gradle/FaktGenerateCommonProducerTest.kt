// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import com.rsicarelli.fakt.compiler.fir.cache.MetadataCacheSerializer
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile
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
 * task with `platformType = "common"` so the worker drives **`KotlinMetadataCompiler`** over
 * `commonMain` sources — a frontend-only pipeline with no IR actualizer, which is why unpaired
 * `expect` declarations can no longer fail the producer (the issue #79 blocker). Fakes are written
 * by the plugin's FIR-phase emitter (`EmitPhase.FIR`) into the task's `@OutputDirectory`; every
 * target's test compilation later consumes that one declared directory as ordinary source.
 *
 * The metadata driver reads **metadata klibs**, not JVM jars: the harness unpacks the stdlib's
 * `commonMain` klib from the `-all` archive (resolved by the build, path passed via the
 * `fakt.test.stdlibMetadataJar` system property) and feeds it through `commonKlibClasspath`. The
 * `@Fake` annotation is declared in-fixture for the same reason; the real `:annotations` metadata
 * klib is exercised end-to-end by the sample-based CI cache-correctness contract.
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
    fun `GIVEN fake interface referencing an expect type in signatures WHEN running THEN the fake renders the expect type`(
        @TempDir projectDir: File
    ) {
        setupCommonProject(projectDir, commonSource = EXPECT_TYPE_IN_SIGNATURE_FIXTURE)

        val result = runTask(projectDir, "faktGenerate")

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":faktGenerate")?.outcome,
            "Expect types used in @Fake signatures must render like any other type; output:\n${result.output}",
        )
        val fake = generatedFakes(projectDir).single { it.name.contains("SessionService") }
        assertTrue(
            "PlatformClock" in fake.readText(),
            "Generated fake must reference the expect type:\n${fake.readText()}",
        )
    }

    @Test
    fun `GIVEN commonMain with an expect declaration but no actual WHEN running THEN task succeeds and the fake is generated`(
        @TempDir projectDir: File
    ) {
        setupCommonProject(projectDir, commonSource = EXPECT_PLUS_FIXTURE)

        val result = runTask(projectDir, "faktGenerate")

        // THE issue #79 blocker, locked in the fixed direction: NO_ACTUAL_FOR_EXPECT is an IR
        // actualizer diagnostic, and KotlinMetadataCompiler's frontend-only pipeline has no
        // actualizer — unpaired expects are structurally incapable of failing the producer.
        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":faktGenerate")?.outcome,
            "Unpaired expect in commonMain must not fail the metadata-driven producer; output:\n${result.output}",
        )
        assertTrue(
            generatedFakes(projectDir).any { it.name.contains("SessionService") },
            "Expected FakeSessionService*.kt despite the unpaired expect; output:\n${result.output}",
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

    @Test
    fun `GIVEN generated fake from a renamed interface WHEN rerunning THEN the stale fake is removed`(
        @TempDir projectDir: File
    ) {
        setupCommonProject(projectDir, commonSource = SINGLE_INTERFACE_FIXTURE)
        val first = runTask(projectDir, "faktGenerate")
        assertEquals(TaskOutcome.SUCCESS, first.task(":faktGenerate")?.outcome, first.output)
        assertTrue(generatedFakes(projectDir).any { "SessionService" in it.name })

        // Rename the interface in place — the old fake must not survive the rerun
        projectDir
            .resolve("src/commonMain/kotlin/fixture/Fixture.kt")
            .writeText(SINGLE_INTERFACE_FIXTURE.replace("SessionService", "RenamedService"))

        val second = runTask(projectDir, "faktGenerate")
        assertEquals(TaskOutcome.SUCCESS, second.task(":faktGenerate")?.outcome, second.output)
        val names = generatedFakes(projectDir).map { it.name }
        assertTrue("FakeRenamedServiceImpl.kt" in names, "Renamed fake missing; got $names")
        assertTrue(
            names.none { "SessionService" in it },
            "Stale fake for the old interface name survived: $names",
        )
    }

    @Test
    fun `GIVEN changed klib classpath content WHEN rerunning THEN producer re-executes instead of UP-TO-DATE`(
        @TempDir projectDir: File,
        @TempDir extraKlibDir: File,
    ) {
        // A non-.class resource is invisible to @CompileClasspath normalization but must be
        // content-hashed by the @Classpath commonKlibClasspath input — this locks the annotation
        // choice that closes the klib missed-invalidation hole.
        val marker = extraKlibDir.resolve("marker.bin").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        setupCommonProject(
            projectDir,
            commonSource = SINGLE_INTERFACE_FIXTURE,
            extraKlibClasspathDir = extraKlibDir,
        )

        val first = runTask(projectDir, "faktGenerate")
        assertEquals(TaskOutcome.SUCCESS, first.task(":faktGenerate")?.outcome, first.output)

        marker.writeBytes(byteArrayOf(4, 5, 6))

        val second = runTask(projectDir, "faktGenerate")
        assertEquals(
            TaskOutcome.SUCCESS,
            second.task(":faktGenerate")?.outcome,
            "Changed klib-classpath content must re-execute the producer (missed-invalidation hole); got:\n${second.output}",
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

    private fun leaksJvmOnlyImports(file: File): Boolean =
        file.readText().lineSequence().any { line ->
            line.trimStart().startsWith("import java.") ||
                line.trimStart().startsWith("import javax.")
        }

    private fun setupCommonProject(
        projectDir: File,
        commonSource: String,
        firMetadataFile: File? = null,
        extraKlibClasspathDir: File? = null,
    ) {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-common-producer-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        projectDir.resolve("src/commonMain/kotlin/fixture").mkdirs()
        projectDir.resolve("src/commonMain/kotlin/fixture/Fixture.kt").writeText(commonSource)
        // The metadata driver cannot read the JVM :annotations jar; declare the annotation
        // in-fixture (the FIR checker matches by ClassId). The real metadata klib is covered by
        // the sample-based CI contract.
        projectDir.resolve("src/commonMain/kotlin/fakt").mkdirs()
        projectDir
            .resolve("src/commonMain/kotlin/fakt/FakeAnnotation.kt")
            .writeText(IN_FIXTURE_FAKE_ANNOTATION)
        projectDir
            .resolve("build.gradle.kts")
            .writeText(buildScriptForTask(projectDir, firMetadataFile, extraKlibClasspathDir))
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
        firMetadataFile: File?,
        extraKlibClasspathDir: File?,
    ): String {
        val outputDir = projectDir.resolve("build/generated/fakt/metadata/commonMain/kotlin")
        val sourceSetContextJson =
            json.encodeToString(SourceSetContext.serializer(), COMMON_CONTEXT)
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
        val klibEntries =
            listOfNotNull(stdlibCommonKlibDir, extraKlibClasspathDir).joinToString(",\n        ") {
                dir ->
                """file("${dir.absolutePath.replace('\\', '/')}")"""
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
                sources.from(file("src/commonMain/kotlin"))
                commonKlibClasspath.from(
                    $klibEntries
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
        /**
         * The stdlib `commonMain` metadata klib, unpacked once per suite from the `-all` archive
         * the build resolves (see `stdlibMetadataForTests` in `gradle-plugin/build.gradle.kts`).
         * Read-only fixture data shared across tests.
         */
        private val stdlibCommonKlibDir: File by lazy {
            val jarPath =
                requireNotNull(System.getProperty("fakt.test.stdlibMetadataJar")) {
                    "fakt.test.stdlibMetadataJar system property not set — see the test task " +
                        "configuration in gradle-plugin/build.gradle.kts"
                }
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
            targetRoot.resolve("commonMain").also {
                require(it.isDirectory) { "stdlib commonMain klib missing in $jarPath" }
            }
        }

        private val IN_FIXTURE_FAKE_ANNOTATION =
            """
            package com.rsicarelli.fakt

            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.SOURCE)
            annotation class Fake
            """
                .trimIndent()

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

        private val EXPECT_TYPE_IN_SIGNATURE_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            expect class PlatformClock {
                fun now(): Long
            }

            @Fake
            interface SessionService {
                fun clock(): PlatformClock
                suspend fun login(user: String): String
                val isOnline: Boolean
            }
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
