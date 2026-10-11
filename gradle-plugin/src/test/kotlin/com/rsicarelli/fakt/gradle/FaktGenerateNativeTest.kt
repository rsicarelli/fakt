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
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Gradle TestKit suite for the Kotlin/Native shape of [FaktGenerateTask] (issue #152): a leaf
 * (`linuxX64Main`) and a shared-native source set driven by `K2Native` from a real Kotlin/Native
 * distribution instead of the in-process compiler plugin.
 *
 * Needs a distribution on disk, so every test is skipped unless the Gradle property
 * `fakt.test.konanHome` (forwarded as the system property of the same name) points at one, e.g.
 * `-Pfakt.test.konanHome=$HOME/.konan/kotlin-native-prebuilt-linux-x86_64-2.4.10`. The `@Fake`
 * annotation is declared in-fixture because the `:annotations` Native klibs are not on the test
 * classpath.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateNativeTest {

    @Test
    fun `GIVEN a linuxX64 leaf over posix types WHEN generating THEN emits only the leaf fake`(
        @TempDir projectDir: File
    ) {
        assumeDistribution()
        setupLeaf(projectDir, konanHome())

        val result = runTask(projectDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        val fakes = generatedFakes(projectDir)
        assertEquals(
            listOf("FakePosixClockImpl.kt"),
            fakes.map { it.name },
            "Only the leaf's own fake is emitted; commonMain is analysis-only\n${result.output}",
        )
        assertTrue(
            "timespec" in fakes.single().readText(),
            "The fake must render the platform.posix type:\n${fakes.single().readText()}",
        )
        assertAnalysedByNative(projectDir)
    }

    @Test
    fun `GIVEN a shared native set WHEN generating THEN emits its fake`(@TempDir projectDir: File) {
        assumeDistribution()
        setupShared(projectDir, konanHome())

        val result = runTask(projectDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertEquals(
            listOf("FakeNativeClockImpl.kt"),
            generatedFakes(projectDir).map { it.name },
            result.output,
        )
        assertAnalysedByNative(projectDir)
    }

    @Test
    fun `GIVEN unchanged inputs WHEN rerunning the native leaf THEN task is UP-TO-DATE`(
        @TempDir projectDir: File
    ) {
        assumeDistribution()
        setupLeaf(projectDir, konanHome())
        runTask(projectDir)

        val second = runTask(projectDir)

        assertEquals(
            TaskOutcome.UP_TO_DATE,
            second.task(":faktGenerate")?.outcome,
            "The native task must be incremental like the JVM one:\n${second.output}",
        )
    }

    @Test
    fun `GIVEN an empty KONAN_DATA_DIR WHEN generating with an explicit home THEN succeeds`(
        @TempDir projectDir: File,
        @TempDir emptyDataDir: File,
    ) {
        assumeDistribution()
        setupLeaf(projectDir, konanHome())

        val result =
            runTask(projectDir, environment = mapOf("KONAN_DATA_DIR" to emptyDataDir.absolutePath))

        assertEquals(TaskOutcome.SUCCESS, result.task(":faktGenerate")?.outcome, result.output)
        assertTrue(
            emptyDataDir.list().orEmpty().isEmpty(),
            "Nothing may be provisioned into the data dir: ${emptyDataDir.list()?.toList()}",
        )
    }

    @Test
    fun `GIVEN a konan home without a distribution WHEN generating THEN fails with the problem`(
        @TempDir projectDir: File,
        @TempDir emptyHome: File,
    ) {
        assumeDistribution()
        setupLeaf(projectDir, emptyHome)

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .forwardOutput()
                .withArguments("faktGenerate", "--stacktrace")
                .buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":faktGenerate")?.outcome, result.output)
        assertTrue(
            "No Kotlin/Native distribution at '${emptyHome.path}'" in result.output,
            "The failure must be the distribution problem:\n${result.output}",
        )
    }

    /** The Native driver leaves its throwaway klib in the task's scratch directory. */
    private fun assertAnalysedByNative(projectDir: File) {
        val klib = projectDir.resolve("build/faktCaches/native/main/native-klib/fakt-analysis.klib")
        assertTrue(
            klib.exists(),
            "K2Native must have produced ${klib.path}; scratch holds " +
                projectDir.resolve("build/faktCaches").walkTopDown().map { it.path }.toList(),
        )
    }

    private fun assumeDistribution() =
        Assumptions.assumeTrue(
            System.getProperty(KONAN_HOME_PROPERTY) != null,
            "Set -Pfakt.test.konanHome to a Kotlin/Native distribution to run the Native suite.",
        )

    private fun konanHome(): File = File(System.getProperty(KONAN_HOME_PROPERTY))

    private fun runTask(projectDir: File, environment: Map<String, String>? = null): BuildResult =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments("faktGenerate", "--stacktrace")
            .apply { environment?.let { withEnvironment(System.getenv() + it) } }
            .build()

    private fun generatedFakes(projectDir: File): List<File> =
        projectDir
            .resolve("build/generated/fakt")
            .walkTopDown()
            .filter { it.isFile && it.name.startsWith("Fake") && it.extension == "kt" }
            .toList()

    private fun setupLeaf(projectDir: File, konanHome: File) {
        writeSettings(projectDir)
        write(projectDir, "src/commonMain/kotlin/fixture/Common.kt", COMMON_FIXTURE)
        write(projectDir, "src/commonMain/kotlin/fixture/Fake.kt", ANNOTATION_FIXTURE)
        write(projectDir, "src/linuxX64Main/kotlin/fixture/Posix.kt", POSIX_FIXTURE)
        val main = SourceSetInfo("linuxX64Main", parents = listOf("commonMain"))
        val context =
            SourceSetContext(
                compilationName = "main",
                targetName = "linuxX64",
                platformType = "native",
                isTest = false,
                defaultSourceSet = main,
                allSourceSets = listOf(main, SourceSetInfo("commonMain", parents = emptyList())),
                outputDirectory = "fakt://generated",
                commonTestOutputDirectory = "fakt://generated",
                outputDirectories = mapOf(main.name to "fakt://generated"),
            )
        write(
            projectDir,
            "build.gradle.kts",
            buildScript(
                projectDir,
                context,
                konanHome,
                extra =
                    """
                    sources.from(file("src/linuxX64Main/kotlin"))
                    analysisOnlySources.from(file("src/commonMain/kotlin"))
                    sourceSetRoots.put("linuxX64Main", listOf(file("src/linuxX64Main/kotlin").path))
                    sourceSetRoots.put("commonMain", listOf(file("src/commonMain/kotlin").path))
                    konanTargets.set(listOf("linux_x64"))
                    compilerArguments.set(listOf("-opt-in=kotlinx.cinterop.ExperimentalForeignApi"))
                    """,
            ),
        )
    }

    private fun setupShared(projectDir: File, konanHome: File) {
        writeSettings(projectDir)
        write(projectDir, "src/nativeMain/kotlin/fixture/Fake.kt", ANNOTATION_FIXTURE)
        write(projectDir, "src/nativeMain/kotlin/fixture/Clock.kt", SHARED_FIXTURE)
        val main = SourceSetInfo("nativeMain", parents = emptyList())
        val context =
            SourceSetContext(
                compilationName = "nativeMain",
                targetName = "metadata",
                platformType = "common",
                isTest = false,
                defaultSourceSet = main,
                allSourceSets = listOf(main),
                outputDirectory = "fakt://generated",
                commonTestOutputDirectory = "fakt://generated",
                outputDirectories = mapOf(main.name to "fakt://generated"),
            )
        write(
            projectDir,
            "build.gradle.kts",
            buildScript(
                projectDir,
                context,
                konanHome,
                extra =
                    """
                    sources.from(file("src/nativeMain/kotlin"))
                    konanTargets.set(listOf("linux_arm64", "linux_x64"))
                    sharedNative.set(true)
                    """,
            ),
        )
    }

    private fun writeSettings(projectDir: File) {
        write(projectDir, "settings.gradle.kts", """rootProject.name = "fakt-native-test"""")
        write(
            projectDir,
            "gradle.properties",
            "org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n",
        )
    }

    private fun write(projectDir: File, path: String, text: String) {
        projectDir.resolve(path).also { it.parentFile.mkdirs() }.writeText(text)
    }

    private fun buildScript(
        projectDir: File,
        context: SourceSetContext,
        konanHome: File,
        extra: String,
    ): String {
        val outputDir = projectDir.resolve("build/generated/fakt/native/main/kotlin")
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
                $extra
                konanHome.set(file("${konanHome.absolutePath.replace('\\', '/')}"))
                kotlinNativeVersion.set("test-native")
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
                scratchDir.set(layout.buildDirectory.dir("faktCaches/native/main"))
            }
            """
            .trimIndent()
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

    private val json = Json { prettyPrint = false }

    companion object {
        private const val KONAN_HOME_PROPERTY = "fakt.test.konanHome"

        // The FIR checker matches `@Fake` by ClassId, so an in-fixture declaration stands in for
        // the `:annotations` Native klib.
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

            @Fake
            interface CommonAuditService {
                fun record(event: String): Boolean
            }
            """
                .trimIndent()

        private val POSIX_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake
            import kotlinx.cinterop.CPointer
            import platform.posix.FILE
            import platform.posix.timespec

            actual fun platformName(): String = "linux"

            @Fake
            interface PosixClock {
                fun now(): timespec?
                fun open(path: String): CPointer<FILE>?
                val clockId: Int
            }
            """
                .trimIndent()

        private val SHARED_FIXTURE =
            """
            package fixture

            import com.rsicarelli.fakt.Fake

            @Fake
            interface NativeClock {
                fun tick(label: String): Long
            }
            """
                .trimIndent()
    }
}
