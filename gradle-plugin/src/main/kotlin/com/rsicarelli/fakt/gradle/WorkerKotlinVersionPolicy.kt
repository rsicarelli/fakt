// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

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
    val (majorText, minorText, patchText) =
        VERSION_PREFIX.find(version)?.destructured ?: return null
    val major = majorText.toIntOrNull()
    val minor = minorText.toIntOrNull()
    val patch = patchText.ifEmpty { "0" }.toIntOrNull()
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
        "$worker, and your Kotlin may be at most one minor version newer than that. Update " +
        "Fakt to a release built for Kotlin $user, or point the `faktWorker` configuration at a " +
        "matching kotlin-compiler-embeddable. To build anyway, set `fakt.allowNewerKotlin=true` " +
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

/** Gradle property that lets a build go ahead when the user's Kotlin is too new for the worker. */
internal const val ALLOW_NEWER_KOTLIN_PROPERTY: String = "fakt.allowNewerKotlin"

/**
 * Testable seam of [checkWorkerKotlinVersion]: a `null` [userKotlin] (no Kotlin plugin applied) is
 * skipped, warnings go to [warn], and a failure throws [GradleException].
 */
internal fun checkWorkerKotlinVersion(
    userKotlin: String?,
    workerKotlin: String,
    allowNewer: Boolean,
    warn: (String) -> Unit,
) {
    if (userKotlin == null) return
    when (val outcome = workerKotlinOutcome(userKotlin, workerKotlin, allowNewer)) {
        WorkerKotlinOutcome.Silent -> Unit
        is WorkerKotlinOutcome.Warn -> warn(outcome.message)
        is WorkerKotlinOutcome.Fail -> throw GradleException(outcome.message)
    }
}

/**
 * Compares the project's Kotlin Gradle plugin version with the worker's pinned compiler. Projects
 * without the Kotlin plugin (for example AGP built-in Kotlin) are skipped.
 */
internal fun checkWorkerKotlinVersion(
    project: Project,
    workerKotlin: String = FaktGradleSubplugin.FAKT_KOTLIN_VERSION,
    warn: (String) -> Unit = project.logger::warn,
) {
    checkWorkerKotlinVersion(
        userKotlin = runCatching { project.getKotlinPluginVersion() }.getOrNull(),
        workerKotlin = workerKotlin,
        allowNewer =
            project.providers.gradleProperty(ALLOW_NEWER_KOTLIN_PROPERTY).orNull.toBoolean(),
        warn = warn,
    )
}
