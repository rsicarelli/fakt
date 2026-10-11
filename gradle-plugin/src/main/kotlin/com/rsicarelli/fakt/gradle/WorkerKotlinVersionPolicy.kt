// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

/** Leading `major.minor.patch` of a Kotlin version string; a missing patch reads as 0. */
internal data class KotlinVersionParts(val major: Int, val minor: Int, val patch: Int)

/** How a user's Kotlin version compares with the compiler the Fakt worker runs on. */
internal enum class WorkerKotlinVerdict {
    /** Same, older, or the same `major.minor` with a higher patch. */
    SUPPORTED,

    /** One minor newer than the worker. */
    NEWER_MINOR,

    /** Two or more minors newer, or a newer major. */
    UNSUPPORTED_NEWER,

    /** A version string we cannot read; never blocks the build. */
    UNKNOWN,
}

/** What the version check does about a verdict. */
internal sealed interface WorkerKotlinOutcome {
    data object Silent : WorkerKotlinOutcome

    data class Warn(val message: String) : WorkerKotlinOutcome

    data class Fail(val message: String) : WorkerKotlinOutcome
}

private val VERSION_PREFIX = Regex("""^(\d+)\.(\d+)(?:\.(\d+))?""")

private const val ONE_MINOR_NEWER = 1

/**
 * Reads the leading `major.minor[.patch]` and ignores any pre-release or dev suffix (`2.5.0-Beta1`,
 * `2.5.20-dev-1234`, `2.4.0-titan-100`). Returns `null` when major and minor cannot be read.
 */
internal fun parseKotlinVersion(version: String): KotlinVersionParts? {
    val groups = VERSION_PREFIX.find(version)?.groupValues ?: return null
    val major = groups[1].toIntOrNull()
    val minor = groups[2].toIntOrNull()
    val patch = groups[3].ifEmpty { "0" }.toIntOrNull()
    return if (major == null || minor == null || patch == null) {
        null
    } else {
        KotlinVersionParts(major, minor, patch)
    }
}

/** Compares by `major.minor` only; the patch never matters. */
internal fun workerKotlinVersionVerdict(
    userKotlin: String,
    workerKotlin: String,
): WorkerKotlinVerdict {
    val user = parseKotlinVersion(userKotlin)
    val worker = parseKotlinVersion(workerKotlin)
    return when {
        user == null || worker == null -> WorkerKotlinVerdict.UNKNOWN
        user.major > worker.major -> WorkerKotlinVerdict.UNSUPPORTED_NEWER
        user.major < worker.major || user.minor <= worker.minor -> WorkerKotlinVerdict.SUPPORTED
        user.minor - worker.minor == ONE_MINOR_NEWER -> WorkerKotlinVerdict.NEWER_MINOR
        else -> WorkerKotlinVerdict.UNSUPPORTED_NEWER
    }
}

internal fun newerMinorWarning(user: String, worker: String): String =
    "Fakt: your Kotlin Gradle plugin is $user, one minor newer than the Kotlin $worker " +
        "that Fakt's worker compiles with. Fakt usually still works, but if generation " +
        "fails, update Fakt or see Compatibility > Worker compiler version in the docs."

internal fun unsupportedNewerError(user: String, worker: String): String =
    "Fakt: your Kotlin Gradle plugin is $user, but Fakt's worker compiles with Kotlin " +
        "$worker, two or more minor versions older. Update Fakt to a release built for " +
        "Kotlin $user, or point the `faktWorker` configuration at a matching " +
        "kotlin-compiler-embeddable. To build anyway, set `fakt.allowNewerKotlin=true` " +
        "in gradle.properties, which turns this error into a warning. " +
        "See Compatibility > Worker compiler version in the docs."

/** Combines the verdict with `fakt.allowNewerKotlin`: it turns a failure into a warning. */
internal fun workerKotlinOutcome(
    userKotlin: String,
    workerKotlin: String,
    allowNewer: Boolean,
): WorkerKotlinOutcome =
    when (workerKotlinVersionVerdict(userKotlin, workerKotlin)) {
        WorkerKotlinVerdict.SUPPORTED,
        WorkerKotlinVerdict.UNKNOWN -> WorkerKotlinOutcome.Silent
        WorkerKotlinVerdict.NEWER_MINOR ->
            WorkerKotlinOutcome.Warn(newerMinorWarning(userKotlin, workerKotlin))
        WorkerKotlinVerdict.UNSUPPORTED_NEWER -> {
            val message = unsupportedNewerError(userKotlin, workerKotlin)
            if (allowNewer) WorkerKotlinOutcome.Warn(message) else WorkerKotlinOutcome.Fail(message)
        }
    }
