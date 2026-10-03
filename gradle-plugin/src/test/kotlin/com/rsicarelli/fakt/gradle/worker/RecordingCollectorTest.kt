// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/** Runs the real K2 JVM compiler through the recording collector (no Gradle involved). */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordingCollectorTest {

    private class Run(
        val exitName: String,
        val printed: () -> String,
        val recorder: AnalysisRecorder,
    )

    /** Same recipe as the worker classpath: the Gradle plugin jars must not shadow the compiler. */
    private fun isolatedCompilerLoader(): URLClassLoader =
        URLClassLoader(
            classpathEntries().map { it.toURI().toURL() }.toTypedArray(),
            ClassLoader.getPlatformClassLoader(),
        )

    private fun classpathEntries(): List<File> =
        System.getProperty("java.class.path").split(File.pathSeparator).map(::File).filter {
            it.exists() &&
                "kctfork" !in it.absolutePath &&
                !it.name.startsWith("kotlin-gradle-plugin")
        }

    private fun run(dir: File, source: String, check: (Run) -> Unit) {
        isolatedCompilerLoader().use { check(runWith(it, dir, source)) }
    }

    private fun runWith(loader: ClassLoader, dir: File, source: String): Run {
        val file = dir.resolve("Sample.kt").apply { writeText(source) }
        val bridge = K2CompilerBridge(loader, CompilerDriver.JVM)
        val args = bridge.newArgs()
        bridge.setOnArgs(args, "setFreeArgs", List::class.java, listOf(file.absolutePath))
        bridge.setOnArgs(
            args,
            "setClasspath",
            String::class.java,
            classpathEntries().joinToString(File.pathSeparator),
        )
        bridge.setOnArgs(args, "setNoStdlib", Boolean::class.javaPrimitiveType!!, true)
        bridge.setOnArgs(args, "setNoReflect", Boolean::class.javaPrimitiveType!!, true)
        bridge.setOnArgs(
            args,
            "setDestination",
            String::class.java,
            dir.resolve("out").absolutePath,
        )
        val buffer = ByteArrayOutputStream()
        val recorder = AnalysisRecorder()
        val collector =
            bridge.newRecordingCollector(
                bridge.newPrintingMessageCollector(PrintStream(buffer, true)),
                recorder,
            )
        val exit =
            bridge
                .execMethod()
                .invoke(bridge.newCompiler(), collector, bridge.servicesEmpty(), args)
        return Run((exit as Enum<*>).name, { buffer.toString() }, recorder)
    }

    @Test
    fun `GIVEN only a tolerated error WHEN K2 runs THEN it still exits COMPILATION_ERROR and prints nothing`(
        @TempDir dir: File
    ) {
        run(dir, "package p\nfun codec() = Unresolved.serializer()\n") { run ->
            assertEquals("COMPILATION_ERROR", run.exitName)
            assertTrue(run.recorder.hasHeldBack())
            assertEquals(AnalysisOutcome.SUCCESS, run.recorder.decide(run.exitName))
            assertFalse(run.printed().contains("nresolved reference"), run.printed())
        }
    }

    @Test
    fun `GIVEN a tolerated error WHEN recorded THEN it carries path line and column`(
        @TempDir dir: File
    ) {
        run(dir, "package p\nfun codec() = Unresolved.serializer()\n") { run ->
            val location = run.recorder.tolerated().single().location
            assertTrue(location != null && location.endsWith("Sample.kt:2:15"), location)
        }
    }

    @Test
    fun `GIVEN a held back error WHEN the run is replayed THEN the error is printed`(
        @TempDir dir: File
    ) {
        run(dir, "package p\nfun codec() = Unresolved.serializer()\n") { run ->
            run.recorder.replayHeldBack()

            assertTrue(run.printed().contains("nresolved reference"), run.printed())
        }
    }

    @Test
    fun `GIVEN clean code WHEN K2 runs THEN it exits OK with nothing held back`(
        @TempDir dir: File
    ) {
        run(dir, "package p\nfun ok() = 1\n") { run ->
            assertEquals("OK", run.exitName)
            assertFalse(run.recorder.hasHeldBack())
        }
    }
}
