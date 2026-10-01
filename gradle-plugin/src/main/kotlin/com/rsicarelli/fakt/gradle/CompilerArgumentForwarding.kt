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
    addVersionFlag("-language-version", s.languageVersion)
    addVersionFlag("-api-version", s.apiVersion)
    addValueFlag("-jvm-target", s.jvmTarget)
    addValueFlag("-jvm-default", s.jvmDefault)
    s.optIn.distinct().sorted().forEach { add("-opt-in=$it") }
    if (s.progressiveMode) add("-progressive")
    if (s.noJdk) add("-no-jdk")
    s.languageFeatures.forEach { add("-XXLanguage:+$it") }
    addAll(dropUnforwardableArguments(s.freeCompilerArgs))
}

private fun MutableList<String>.addVersionFlag(flag: String, value: String?) {
    if (value != null && !isBelowWorkerMinimum(value)) addValueFlag(flag, value)
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
        val next = args.getOrNull(index + 1)
        val isPair = isFlagValuePair(token, next)
        val value = if (isPair) next else null
        if (isForwardable(token, value)) {
            kept.add(token)
            value?.let(kept::add)
        }
        index += if (isPair || token in ForwardingRules.VALUE_FLAGS) 2 else 1
    }
    return kept
}

/**
 * A two-token flag: a known value flag, or an unknown `-X` flag without `=` that is followed by a
 * token that is not a flag (old `-Xfoo value` forms that the worker compiler still parses).
 */
private fun isFlagValuePair(token: String, next: String?): Boolean =
    token in ForwardingRules.VALUE_FLAGS ||
        (token.startsWith("-X") &&
            '=' !in token &&
            next != null &&
            !next.startsWith("-") &&
            !next.startsWith("@"))

private fun isForwardable(token: String, value: String?): Boolean =
    token.startsWith("-") &&
        !isDroppedFlag(token) &&
        !hasAbsolutePath(token.substringAfter('=', "")) &&
        (value == null || !hasAbsolutePath(value)) &&
        !isOldVersionFlag(token, value)

/** True for a `-language-version` / `-api-version` free arg below [MIN_WORKER_VERSION]. */
private fun isOldVersionFlag(token: String, value: String?): Boolean {
    val flag = token.substringBefore('=')
    val version = if ('=' in token) token.substringAfter('=') else value
    return flag in VERSION_FLAGS && version != null && isBelowWorkerMinimum(version)
}

private fun isDroppedFlag(token: String): Boolean =
    token.substringBefore('=') in ForwardingRules.ALWAYS_DROPPED_FLAGS ||
        ForwardingRules.DROPPED_PREFIXES.any(token::startsWith) ||
        (token.startsWith("-Xwarning-level=") && token.endsWith(":error"))

private fun hasAbsolutePath(value: String): Boolean =
    ForwardingRules.ABSOLUTE_PATH.containsMatchIn(value) ||
        value.split(File.pathSeparator, ",").any(ForwardingRules.ABSOLUTE_PATH::containsMatchIn)

/**
 * The lowest language and API version the Fakt worker accepts: 2.0.
 *
 * The worker is always `kotlin-compiler-embeddable` 2.4.10, whose `LanguageVersion.FIRST_SUPPORTED`
 * and `FIRST_API_SUPPORTED` are 2.0, and it rejects anything lower. A project on an older KGP can
 * still compile with `apiVersion = 1.9`, so such a value is dropped instead of forwarded. Dropping
 * is safe: the analysis becomes more lenient, never stricter.
 */
internal const val MIN_WORKER_VERSION = "2.0"

private val VERSION_FLAGS = setOf("-language-version", "-api-version")

/** A version that parses as `major.minor` and is lower than [MIN_WORKER_VERSION]. */
private fun isBelowWorkerMinimum(version: String): Boolean {
    val parts = version.split('.').map { it.toIntOrNull() }
    val major = parts.getOrNull(0)
    val minor = parts.getOrNull(1)
    val (minMajor, minMinor) = MIN_WORKER_VERSION.split('.').map(String::toInt)
    return major != null && (major < minMajor || (major == minMajor && (minor ?: 0) < minMinor))
}

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
            "-source-map-embed-sources",
            "-source-map-names-policy",
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
            "-Xplugin",
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
