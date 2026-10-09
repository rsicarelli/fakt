// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.util.Locale

/** The kind of test-like Kotlin compile task a name denotes. */
internal enum class TestCompileKind {
    UNIT,
    ANDROID_TEST,
    FIXTURES,
    TEST,
}

/** A parsed test-like compile task: [variant] (`main` when the task has none) and its [kind]. */
internal data class TestCompileTask(val variant: String, val kind: TestCompileKind)

private val TEST_COMPILE_TASK =
    Regex("^compile(.*?)(UnitTest|AndroidTest|TestFixtures|Test)Kotlin$")

private const val MAIN_VARIANT = "main"

/**
 * Parses a Kotlin compile task name into its build variant and test kind, or `null` when the task
 * is not a test-like compile (`compileKotlin`, `compileDebugKotlin`). The variant is matched as a
 * whole, so `debugMinified` is never confused with `debug`.
 *
 * `compileDebugMinifiedUnitTestKotlin` -> (`debugMinified`, UNIT); `compileTestKotlin` -> (`main`,
 * TEST).
 */
internal fun testCompileVariant(taskName: String): TestCompileTask? =
    TEST_COMPILE_TASK.matchEntire(taskName)?.let { match ->
        val (rawVariant, rawKind) = match.destructured
        TestCompileTask(
            variant =
                rawVariant.replaceFirstChar { it.lowercase(Locale.ROOT) }.ifEmpty { MAIN_VARIANT },
            kind =
                when (rawKind) {
                    "UnitTest" -> TestCompileKind.UNIT
                    "AndroidTest" -> TestCompileKind.ANDROID_TEST
                    "TestFixtures" -> TestCompileKind.FIXTURES
                    else -> TestCompileKind.TEST
                },
        )
    }

/**
 * Decides whether a non-KMP producer's generated-fakes directory is sourced by a Kotlin compile
 * task.
 * - Fixtures on: only the `testFixtures` compile of the producer's own variant (a `main` producer
 *   feeds every fixtures compile).
 * - Fixtures off, [associatedWith] known: the task's compilation is associated with the producer's
 *   compilation, whatever the task is called.
 * - Fixtures off, no association data: exact-name fallback, the producer's own variant for a
 *   variant producer, any non-fixtures test compile for `main`.
 *
 * @param associatedWith names of the compilations the task's compilation is associated with, or
 *   `null` when it has no association data.
 */
internal fun wiresTestCompile(
    taskName: String,
    producer: String,
    associatedWith: Set<String>?,
    useTestFixtures: Boolean,
): Boolean {
    val parsed = testCompileVariant(taskName)
    val producesMain = producer.equals(MAIN_VARIANT, ignoreCase = true)
    val sameVariant = parsed?.variant.equals(producer, ignoreCase = true)
    return when {
        useTestFixtures -> parsed?.kind == TestCompileKind.FIXTURES && (producesMain || sameVariant)
        parsed == null -> producesMain && associatedWith == null && isPlainTestTask(taskName)
        parsed.kind == TestCompileKind.FIXTURES -> false
        associatedWith != null -> producer in associatedWith
        else -> producesMain || sameVariant
    }
}

/** Today's `main` rule for task names the parser does not know: a non-fixtures `*test*` task. */
private fun isPlainTestTask(taskName: String): Boolean =
    taskName.contains("test", ignoreCase = true) && !isTestFixturesCompileTask(taskName)

/** Whether [taskName] is a `testFixtures` Kotlin compile task (legacy fixtures route). */
internal fun isTestFixturesCompileTask(taskName: String): Boolean =
    taskName.contains("testfixtures", ignoreCase = true)
