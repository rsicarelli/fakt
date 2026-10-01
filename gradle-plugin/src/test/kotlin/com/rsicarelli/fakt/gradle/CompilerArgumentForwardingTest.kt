// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Pure tests for the compiler-argument mapping that feeds the Fakt worker. No Gradle, no Project.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompilerArgumentForwardingTest {
    private val sep = File.pathSeparator

    private fun forward(snapshot: CompilerOptionsSnapshot) = forwardedCompilerArguments(snapshot)

    private fun drop(vararg args: String) = dropUnforwardableArguments(args.toList())

    private fun assertDropped(vararg args: String) = assertEquals(emptyList(), drop(*args))

    private fun assertKept(vararg args: String) = assertEquals(args.toList(), drop(*args))

    // ---- A1 ----

    @Test
    fun `GIVEN language and api version WHEN forwarding THEN both flags are emitted`() {
        val result = forward(CompilerOptionsSnapshot(languageVersion = "2.0", apiVersion = "2.1"))

        assertEquals(listOf("-language-version", "2.0", "-api-version", "2.1"), result)
    }

    @Test
    fun `GIVEN no versions WHEN forwarding THEN neither flag is emitted`() {
        val result = forward(CompilerOptionsSnapshot())

        assertEquals(emptyList(), result)
    }

    @Test
    fun `GIVEN only api version WHEN forwarding THEN language version flag is absent`() {
        val result = forward(CompilerOptionsSnapshot(apiVersion = "2.0"))

        assertEquals(listOf("-api-version", "2.0"), result)
    }

    // ---- A2 ----

    @Test
    fun `GIVEN duplicated unsorted optIn WHEN forwarding THEN opt-in flags are distinct and sorted`() {
        val result = forward(CompilerOptionsSnapshot(optIn = listOf("b.B", "a.A", "b.B")))

        assertEquals(listOf("-opt-in=a.A", "-opt-in=b.B"), result)
    }

    // ---- A3 ----

    @Test
    fun `GIVEN progressive mode WHEN forwarding THEN progressive flag is emitted`() {
        assertEquals(
            listOf("-progressive"),
            forward(CompilerOptionsSnapshot(progressiveMode = true)),
        )
        assertEquals(emptyList(), forward(CompilerOptionsSnapshot(progressiveMode = false)))
    }

    // ---- A4 ----

    @Test
    fun `GIVEN jvm fields WHEN forwarding THEN jvm flags are emitted`() {
        val result =
            forward(
                CompilerOptionsSnapshot(
                    jvmTarget = "17",
                    jvmDefault = "no-compatibility",
                    noJdk = true,
                )
            )

        assertEquals(
            listOf("-jvm-target", "17", "-jvm-default", "no-compatibility", "-no-jdk"),
            result,
        )
    }

    @Test
    fun `GIVEN jvm target only WHEN forwarding THEN jvm-default and no-jdk are absent`() {
        val result = forward(CompilerOptionsSnapshot(jvmTarget = "21"))

        assertEquals(listOf("-jvm-target", "21"), result)
    }

    @Test
    fun `GIVEN snapshot without jvm fields WHEN forwarding THEN no jvm flag is emitted`() {
        val result =
            forward(
                CompilerOptionsSnapshot(
                    languageVersion = "2.0",
                    optIn = listOf("a.A"),
                    freeCompilerArgs = listOf("-Xcontext-parameters"),
                )
            )

        assertFalse(result.any { it.startsWith("-jvm") || it == "-no-jdk" })
    }

    // ---- A5 + order ----

    @Test
    fun `GIVEN every field WHEN forwarding THEN output follows the documented order`() {
        val result =
            forward(
                CompilerOptionsSnapshot(
                    languageVersion = "2.1",
                    apiVersion = "2.1",
                    optIn = listOf("z.Z", "a.A"),
                    progressiveMode = true,
                    freeCompilerArgs = listOf("-Xcontext-parameters", "-Xjsr305=strict"),
                    languageFeatures = listOf("ContextReceivers"),
                    jvmTarget = "17",
                    jvmDefault = "enable",
                    noJdk = true,
                )
            )

        assertEquals(
            listOf(
                "-language-version",
                "2.1",
                "-api-version",
                "2.1",
                "-jvm-target",
                "17",
                "-jvm-default",
                "enable",
                "-opt-in=a.A",
                "-opt-in=z.Z",
                "-progressive",
                "-no-jdk",
                "-XXLanguage:+ContextReceivers",
                "-Xcontext-parameters",
                "-Xjsr305=strict",
            ),
            result,
        )
    }

    @Test
    fun `GIVEN free args with unforwardable entries WHEN forwarding THEN they are filtered`() {
        val result =
            forward(
                CompilerOptionsSnapshot(
                    freeCompilerArgs = listOf("-Werror", "-Xcontext-parameters", "-d", "/out")
                )
            )

        assertEquals(listOf("-Xcontext-parameters"), result)
    }

    // ---- KEEP ----

    @Test
    fun `GIVEN harmless flags WHEN dropping THEN all are kept in order`() {
        assertKept(
            "-Xcontext-parameters",
            "-Xjsr305=strict",
            "-Xskip-prerelease-check",
            "-Xallow-unstable-dependencies",
            "-progressive",
            "-Wextra",
        )
    }

    @Test
    fun `GIVEN value flag with relative value WHEN dropping THEN flag and value are kept`() {
        assertKept("-opt-in", "a.B", "-language-version", "2.0")
    }

    // ---- DROP: always-drop flags ----

    @Test
    fun `GIVEN two-token always-drop flags WHEN dropping THEN flag and value are consumed together`() {
        val flags =
            listOf(
                "-d",
                "-classpath",
                "-cp",
                "-kotlin-home",
                "-jdk-home",
                "-P",
                "-libraries",
                "-ir-output-dir",
                "-ir-output-name",
                "-output",
                "-module-name",
                "-script-templates",
            )

        flags.forEach { flag ->
            assertEquals(
                listOf("-Xkeep"),
                drop(flag, "relative-value", "-Xkeep"),
                "flag $flag must take its value with it",
            )
        }
    }

    @Test
    fun `GIVEN equals-style always-drop flags WHEN dropping THEN they are dropped`() {
        assertDropped("-d=out", "-classpath=a", "-module-name=m", "-ir-output-dir=x")
    }

    @Test
    fun `GIVEN single-token always-drop flags WHEN dropping THEN they are dropped`() {
        assertDropped(
            "-Werror",
            "-Xwarning-level=FOO:error",
            "-Xwarning-level=*:error",
            "-include-runtime",
            "-no-stdlib",
            "-no-reflect",
            "-Xmulti-platform",
        )
    }

    @Test
    fun `GIVEN warning-level not at error WHEN dropping THEN it is kept`() {
        assertKept("-Xwarning-level=FOO:disabled")
    }

    // ---- DROP: prefixes ----

    @Test
    fun `GIVEN prefix-dropped flags WHEN dropping THEN they are dropped`() {
        assertDropped(
            "-Xplugin=a.jar",
            "-Xcompiler-plugin=a.jar",
            "-Xcompiler-plugin-order=x",
            "-Xfriend-paths=a",
            "-Xbuild-file=b",
            "-Xjava-source-roots=j",
            "-Xmodule-path=m",
            "-Xklib=k",
            "-Xinclude=i",
            "-Xcommon-sources=c",
            "-Xfragments=f",
            "-Xfragment-sources=f",
            "-Xfragment-refines=f",
            "-Xdump-perf=p",
            "-Xdump-directory=d",
            "-Xprofile=p",
            "-Xintellij-plugin-root=r",
            "-Xir-produce-klib-dir",
            "-Xir-produce-js",
            "-Xir-per-module",
            "-Xcache-directory=c",
            "-Xwasm-target=wasm-js",
            "-Xjdk-release=11",
            "-Xexplicit-api=strict",
        )
    }

    @Test
    fun `GIVEN plugin option pairs WHEN dropping THEN the -P pair is dropped`() {
        assertEquals(listOf("-Xkeep"), drop("-P", "plugin:x:y=z", "-Xkeep"))
        assertDropped("-Pplugin:x:y=z")
    }

    // ---- DROP: safety net ----

    @Test
    fun `GIVEN argfile WHEN dropping THEN it is dropped`() {
        assertEquals(listOf("-Xkeep"), drop("@args.txt", "-Xkeep", "@/abs/args"))
    }

    @Test
    fun `GIVEN bare positional tokens WHEN dropping THEN they are dropped`() {
        assertEquals(listOf("-Xkeep=1"), drop("Main.kt", "-Xkeep=1", "src", "relative/dir"))
    }

    @Test
    fun `GIVEN value of a value flag WHEN dropping THEN it is not treated as bare`() {
        assertKept("-jvm-target", "17", "-api-version", "2.0", "-main", "call")
    }

    @Test
    fun `GIVEN unix absolute path in equals value WHEN dropping THEN the flag is dropped`() {
        assertDropped("-Xsome-flag=/abs/path", "-Xother=/")
    }

    @Test
    fun `GIVEN windows absolute paths in equals value WHEN dropping THEN the flag is dropped`() {
        assertDropped(
            "-Xsome-flag=C:\\abs\\path",
            "-Xsome-flag=d:/abs",
            "-Xsome-flag=\\\\server\\s",
        )
    }

    @Test
    fun `GIVEN absolute path as value of a kept value flag WHEN dropping THEN flag and value are dropped`() {
        assertEquals(listOf("-Xkeep"), drop("-main", "/abs/Main", "-Xkeep"))
        assertEquals(listOf("-Xkeep"), drop("-target", "C:\\t", "-Xkeep"))
    }

    @Test
    fun `GIVEN path-separator joined list with absolute entry WHEN dropping THEN flag is dropped`() {
        assertDropped("-Xsome-flag=rel${sep}/abs/two", "-Xsome-flag=/abs/one${sep}/abs/two")
    }

    @Test
    fun `GIVEN relative path list WHEN dropping THEN flag is kept`() {
        assertKept("-Xsome-flag=rel${sep}other")
    }

    @Test
    fun `GIVEN dropped two-token flag at end of args WHEN dropping THEN nothing is left`() {
        assertDropped("-d")
    }

    // ---- property: hostile input never leaks ----

    @Test
    fun `GIVEN hostile free args WHEN forwarding THEN no token is absolute or an argfile`() {
        val hostile =
            listOf(
                "@args.txt",
                "@/abs",
                "/abs/src",
                "\\\\server\\share",
                "C:\\win",
                "c:/win",
                "-d",
                "/out",
                "-Xfoo=/abs",
                "-Xfoo=C:\\x",
                "-Xfoo=rel${sep}/abs",
                "-main",
                "/abs/Main",
                "-P",
                "plugin:a:b=/abs",
                "-Xplugin=/abs/p.jar",
                "Main.kt",
                "-Xcontext-parameters",
            )

        val result = forward(CompilerOptionsSnapshot(freeCompilerArgs = hostile))

        val absolute = Regex("^(/|\\\\|[A-Za-z]:[\\\\/])")
        assertTrue(result.none { absolute.containsMatchIn(it) || it.startsWith("@") }, "$result")
        assertEquals(listOf("-Xcontext-parameters"), result)
    }

    // ---- audit fix 1: versions below the worker minimum ----

    @Test
    fun `GIVEN versions below 2_0 in the snapshot WHEN forwarding THEN both flags are dropped`() {
        listOf("1.8", "1.9").forEach { version ->
            val result =
                forward(CompilerOptionsSnapshot(languageVersion = version, apiVersion = version))

            assertEquals(emptyList(), result, "version $version")
        }
    }

    @Test
    fun `GIVEN versions 2_0 and above in the snapshot WHEN forwarding THEN both flags stay`() {
        listOf("2.0", "2.1").forEach { version ->
            val result =
                forward(CompilerOptionsSnapshot(languageVersion = version, apiVersion = version))

            assertEquals(
                listOf("-language-version", version, "-api-version", version),
                result,
                "version $version",
            )
        }
    }

    @Test
    fun `GIVEN old versions as free args in pair or equals form WHEN dropping THEN they go`() {
        assertDropped("-language-version", "1.9")
        assertDropped("-api-version", "1.8")
        assertDropped("-language-version=1.9")
        assertDropped("-api-version=1.8")
    }

    @Test
    fun `GIVEN current versions as free args in pair or equals form WHEN dropping THEN they stay`() {
        assertKept("-language-version", "2.0")
        assertKept("-api-version", "2.1")
        assertKept("-language-version=2.0")
        assertKept("-api-version=2.1")
    }

    // ---- audit fix 3: two-token flags the table does not know ----

    @Test
    fun `GIVEN source-map value flags WHEN dropping THEN flag and value stay together`() {
        assertKept("-source-map-embed-sources", "always", "-Xcontext-parameters")
        assertKept("-source-map-names-policy", "x", "-Xcontext-parameters")
    }

    @Test
    fun `GIVEN Xplugin with a separate absolute value WHEN dropping THEN both tokens go`() {
        assertEquals(
            listOf("-Xcontext-parameters"),
            drop("-Xplugin", "/abs/x.jar", "-Xcontext-parameters"),
        )
        assertDropped("-Xplugin", "/abs/x.jar")
    }

    @Test
    fun `GIVEN a flag without value followed by another flag WHEN dropping THEN it stays single`() {
        assertKept("-Xcontext-parameters", "-Xjsr305=strict")
    }

    @Test
    fun `GIVEN an unknown X flag followed by a plain value WHEN dropping THEN they stay a pair`() {
        assertKept("-Xfoo", "value")
        assertEquals(listOf("-Xfoo", "value", "-Xbar"), drop("-Xfoo", "value", "-Xbar"))
    }

    @Test
    fun `GIVEN an X flag pair whose value is an absolute path WHEN dropping THEN both go`() {
        assertEquals(listOf("-Xbar"), drop("-Xfoo", "/abs/dir", "-Xbar"))
    }

    @Test
    fun `GIVEN a truly bare token WHEN dropping THEN it is still dropped`() {
        assertEquals(listOf("-Xbar"), drop("Main.kt", "-Xbar"))
    }

    // ---- audit fix 5a ----

    @Test
    fun `GIVEN a comma list with an absolute entry WHEN dropping THEN the flag is dropped`() {
        assertDropped("-Xfoo=rel,/abs")
        assertKept("-Xfoo=rel,other")
    }
}
