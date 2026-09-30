// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.io.File

/**
 * A plain copy of the compiler options of one Kotlin compilation.
 *
 * It has no Gradle types on purpose, so the mapping below is easy to test. The JVM fields stay at
 * their defaults for JS, Wasm and metadata compilations, which must never receive JVM flags.
 */
internal data class CompilerOptionsSnapshot(
    val languageVersion: String? = null,
    val apiVersion: String? = null,
    val optIn: List<String> = emptyList(),
    val progressiveMode: Boolean = false,
    val freeCompilerArgs: List<String> = emptyList(),
    val languageFeatures: List<String> = emptyList(),
    val jvmTarget: String? = null,
    val jvmDefault: String? = null,
    val noJdk: Boolean = false,
)

/**
 * Turns the options of a compilation into the argument list that the Fakt worker parses before its
 * own setters.
 *
 * Order: `-language-version`, `-api-version`, `-jvm-target`, `-jvm-default` (only the ones that are
 * set), then `-opt-in=<fqn>` (distinct, sorted, so the task input is stable), `-progressive`,
 * `-no-jdk`, `-XXLanguage:+<feature>`, and last the free args after [dropUnforwardableArguments].
 *
 * The result is a task `@Input`, so it never contains an absolute path (the cache key would not be
 * relocatable).
 */
internal fun forwardedCompilerArguments(s: CompilerOptionsSnapshot): List<String> = buildList {
    addValueFlag("-language-version", s.languageVersion)
    addValueFlag("-api-version", s.apiVersion)
    addValueFlag("-jvm-target", s.jvmTarget)
    addValueFlag("-jvm-default", s.jvmDefault)
    s.optIn.distinct().sorted().forEach { add("-opt-in=$it") }
    if (s.progressiveMode) add("-progressive")
    if (s.noJdk) add("-no-jdk")
    s.languageFeatures.forEach { add("-XXLanguage:+$it") }
    addAll(dropUnforwardableArguments(s.freeCompilerArgs))
}

private fun MutableList<String>.addValueFlag(flag: String, value: String?) {
    if (value != null) {
        add(flag)
        add(value)
    }
}

/**
 * Removes the free compiler args that would be wrong or unsafe inside the Fakt worker.
 *
 * Dropped, and why:
 * - Flags that Fakt owns (sources, classpath, destination, module name, JDK home, plugins,
 *   `-no-stdlib`, `-no-reflect`, `-Xmulti-platform`, fragments, klib and IR output settings): the
 *   worker sets its own values, and the compilation's values point into other tasks' files.
 * - `-Werror` and `-Xwarning-level=*:error`: Fakt only needs the code to analyse, a warning must
 *   not fail generation.
 * - Flags for other outputs (`-include-runtime`, dumps, profiling, caches, wasm target and so on).
 * - Any `@argfile`, any bare token that is not the value of a two-token flag, and any flag whose
 *   value (also a `File.pathSeparator` list, also a two-token value) is an absolute path: an
 *   absolute path is not relocatable and breaks the build cache.
 *
 * A two-token flag (for example `-d out`) is dropped together with its value. Everything else is
 * kept, for example `-Xcontext-parameters`, `-Xjsr305=strict` and `-Xskip-prerelease-check`.
 */
internal fun dropUnforwardableArguments(args: List<String>): List<String> {
    val kept = mutableListOf<String>()
    var index = 0
    while (index < args.size) {
        val token = args[index]
        val value = if (token in ForwardingRules.VALUE_FLAGS) args.getOrNull(index + 1) else null
        if (isForwardable(token, value)) {
            kept.add(token)
            value?.let(kept::add)
        }
        index += if (token in ForwardingRules.VALUE_FLAGS) 2 else 1
    }
    return kept
}

private fun isForwardable(token: String, value: String?): Boolean =
    token.startsWith("-") &&
        !isDroppedFlag(token) &&
        !hasAbsolutePath(token.substringAfter('=', "")) &&
        (value == null || !hasAbsolutePath(value))

private fun isDroppedFlag(token: String): Boolean =
    token.substringBefore('=') in ForwardingRules.ALWAYS_DROPPED_FLAGS ||
        ForwardingRules.DROPPED_PREFIXES.any(token::startsWith) ||
        (token.startsWith("-Xwarning-level=") && token.endsWith(":error"))

private fun hasAbsolutePath(value: String): Boolean =
    ForwardingRules.ABSOLUTE_PATH.containsMatchIn(value) ||
        value.split(File.pathSeparator).any(ForwardingRules.ABSOLUTE_PATH::containsMatchIn)

/** The rule tables behind [dropUnforwardableArguments]. */
internal object ForwardingRules {
    val ABSOLUTE_PATH = Regex("^(/|\\\\|[A-Za-z]:[\\\\/])")

    /** Flags whose next token is their value. The value is consumed with the flag. */
    val VALUE_FLAGS =
        setOf(
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
            "-language-version",
            "-api-version",
            "-jvm-target",
            "-jvm-default",
            "-opt-in",
            "-main",
            "-target",
            "-module-kind",
            "-source-map-prefix",
            "-source-map-base-dirs",
        )

    /** Flags dropped by exact name (the part before any `=`). */
    val ALWAYS_DROPPED_FLAGS =
        setOf(
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
            "-Werror",
            "-include-runtime",
            "-no-stdlib",
            "-no-reflect",
            "-Xmulti-platform",
        )

    val DROPPED_PREFIXES =
        listOf(
            "-Pplugin:",
            "-Xplugin=",
            "-Xcompiler-plugin",
            "-Xfriend-paths=",
            "-Xbuild-file=",
            "-Xjava-source-roots=",
            "-Xmodule-path=",
            "-Xklib=",
            "-Xinclude=",
            "-Xcommon-sources=",
            "-Xfragment",
            "-Xdump-",
            "-Xprofile",
            "-Xintellij-plugin-root=",
            "-Xir-produce-",
            "-Xir-per-module",
            "-Xcache-directory",
            "-Xwasm-target=",
            "-Xjdk-release=",
            "-Xexplicit-api=",
        )
}
