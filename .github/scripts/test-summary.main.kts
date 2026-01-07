#!/usr/bin/env kotlin

/*
 * Copyright (C) 2025 Rodrigo Sicarelli
 * SPDX-License-Identifier: Apache-2.0
 *
 * Test Summary Generator for GitHub Actions
 *
 * Parses JUnit XML test results and generates a clean Markdown summary
 * for GitHub Actions Job Summary (GITHUB_STEP_SUMMARY).
 *
 * Usage:
 *   kotlin test-summary.main.kts <base-path> <section-title>
 *
 * Examples:
 *   kotlin test-summary.main.kts "test-results" "Test Results"
 *   kotlin test-summary.main.kts "." "All Tests"
 *
 * Output:
 *   - When all tests pass: minimal "X tests passed across Y modules"
 *   - When tests fail: table with per-module stats + failure details
 */

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.NodeList

// region Data Classes

data class TestFailure(
    val testName: String,
    val className: String,
    val message: String,
    val stacktrace: String? = null
)

data class ModuleResult(
    val name: String,
    val tests: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val failures: List<TestFailure>
)

// endregion

// region XML Parsing

fun findTestResultXmls(basePath: File): List<File> =
    basePath.walkTopDown()
        .filter { it.isFile && it.name.endsWith(".xml") }
        .filter { it.absolutePath.contains("/build/test-results/") || it.name.startsWith("TEST-") }
        .toList()

fun extractModuleName(file: File): String {
    val path = file.absolutePath

    // Pattern: .../samples/<module>/build/test-results/...
    val samplesMatch = Regex(""".*/samples/([^/]+)/build/.*""").find(path)
    if (samplesMatch != null) return samplesMatch.groupValues[1]

    // Pattern: .../<module>/build/test-results/...
    val moduleMatch = Regex(""".*/([^/]+)/build/test-results/.*""").find(path)
    if (moduleMatch != null) return moduleMatch.groupValues[1]

    return "unknown"
}

fun parseJUnitXml(file: File): ModuleResult? {
    return try {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)

        val testsuites = doc.getElementsByTagName("testsuite")
        if (testsuites.length == 0) return null

        val testsuite = testsuites.item(0) as Element

        val tests = testsuite.getAttribute("tests").toIntOrNull() ?: 0
        val failures = testsuite.getAttribute("failures").toIntOrNull() ?: 0
        val errors = testsuite.getAttribute("errors").toIntOrNull() ?: 0
        val skipped = testsuite.getAttribute("skipped").toIntOrNull() ?: 0

        val totalFailed = failures + errors
        val passed = tests - totalFailed - skipped

        val failureDetails = extractFailures(doc)

        ModuleResult(
            name = extractModuleName(file),
            tests = tests,
            passed = passed,
            failed = totalFailed,
            skipped = skipped,
            failures = failureDetails
        )
    } catch (e: Exception) {
        System.err.println("Warning: Failed to parse ${file.name}: ${e.message}")
        null
    }
}

fun extractFailures(doc: org.w3c.dom.Document): List<TestFailure> {
    val failures = mutableListOf<TestFailure>()

    val testcases = doc.getElementsByTagName("testcase")
    for (i in 0 until testcases.length) {
        val testcase = testcases.item(i) as Element
        val testName = testcase.getAttribute("name") ?: "unknown"
        val className = testcase.getAttribute("classname")?.substringAfterLast('.') ?: ""

        // Check for failure element
        val failureNodes = testcase.getElementsByTagName("failure")
        if (failureNodes.length > 0) {
            val failure = failureNodes.item(0) as Element
            val message = failure.getAttribute("message")
                .takeIf { it.isNotBlank() }
                ?: "Test failed"
            val stacktrace = failure.textContent.takeIf { it.isNotBlank() }

            failures.add(TestFailure(testName, className, cleanMessage(message), stacktrace))
        }

        // Check for error element
        val errorNodes = testcase.getElementsByTagName("error")
        if (errorNodes.length > 0) {
            val error = errorNodes.item(0) as Element
            val message = error.getAttribute("message")
                .takeIf { it.isNotBlank() }
                ?: "Test error"
            val stacktrace = error.textContent.takeIf { it.isNotBlank() }

            failures.add(TestFailure(testName, className, cleanMessage(message), stacktrace))
        }
    }

    return failures
}

fun cleanMessage(message: String): String =
    message
        .replace("\n", " ")
        .replace("\r", "")
        .trim()

// endregion

// region Aggregation

fun aggregateResults(results: List<ModuleResult>): List<ModuleResult> =
    results.groupBy { it.name }
        .map { (name, moduleResults) ->
            ModuleResult(
                name = name,
                tests = moduleResults.sumOf { it.tests },
                passed = moduleResults.sumOf { it.passed },
                failed = moduleResults.sumOf { it.failed },
                skipped = moduleResults.sumOf { it.skipped },
                failures = moduleResults.flatMap { it.failures }
            )
        }
        .sortedBy { it.name }

// endregion

// region Markdown Generation

fun generateMarkdown(title: String, results: List<ModuleResult>): String {
    val totalTests = results.sumOf { it.tests }
    val totalFailed = results.sumOf { it.failed }
    val moduleCount = results.size

    return buildString {
        appendLine("### $title")
        appendLine()

        if (totalFailed == 0) {
            // Success: minimal output
            appendLine("**$totalTests tests passed** across $moduleCount modules")
        } else {
            // Failures: detailed output with table
            appendLine("| Module | Passed | Failed | Skipped |")
            appendLine("|--------|--------|--------|---------|")

            results.forEach { result ->
                val failedCell = if (result.failed > 0) "**${result.failed}**" else "0"
                appendLine("| ${result.name} | ${result.passed} | $failedCell | ${result.skipped} |")
            }

            appendLine()
            appendLine("---")
            appendLine()

            // Failure details per module (not collapsed)
            results.filter { it.failed > 0 && it.failures.isNotEmpty() }.forEach { result ->
                appendLine("#### ${result.name}")
                appendLine()

                result.failures.forEach { failure ->
                    val testName = if (failure.className.isNotBlank()) {
                        "${failure.className}.${failure.testName}"
                    } else {
                        failure.testName
                    }

                    appendLine("**$testName**")
                    appendLine()
                    appendLine("> ${failure.message}")
                    appendLine()

                    // Stacktrace in collapsible section
                    failure.stacktrace?.let { trace ->
                        appendLine("<details>")
                        appendLine("<summary>Stacktrace</summary>")
                        appendLine()
                        appendLine("```")
                        appendLine(trace.trim())
                        appendLine("```")
                        appendLine()
                        appendLine("</details>")
                        appendLine()
                    }
                }
            }
        }
    }
}

// endregion

// region Output

fun writeToSummary(markdown: String) {
    val summaryFile = System.getenv("GITHUB_STEP_SUMMARY")

    if (summaryFile != null) {
        File(summaryFile).appendText(markdown)
        println("Summary written to GITHUB_STEP_SUMMARY")
    } else {
        // Local execution: print to stdout
        println()
        println("=== Generated Summary ===")
        println(markdown)
    }
}

// endregion

// region Main

fun main(args: Array<String>) {
    val basePath = args.getOrNull(0) ?: "."
    val title = args.getOrNull(1) ?: "Test Results"

    val baseDir = File(basePath)
    if (!baseDir.exists()) {
        System.err.println("Error: Base path does not exist: $basePath")
        return
    }

    println("Searching for test results in: ${baseDir.absolutePath}")

    val xmlFiles = findTestResultXmls(baseDir)
    if (xmlFiles.isEmpty()) {
        println("No test result files found in: $basePath")
        return
    }

    println("Found ${xmlFiles.size} test result files")

    val results = xmlFiles.mapNotNull { parseJUnitXml(it) }
    val aggregated = aggregateResults(results)

    val totalTests = aggregated.sumOf { it.tests }
    val totalPassed = aggregated.sumOf { it.passed }
    val totalFailed = aggregated.sumOf { it.failed }
    val totalSkipped = aggregated.sumOf { it.skipped }

    println("Results: $totalTests tests, $totalPassed passed, $totalFailed failed, $totalSkipped skipped")

    val markdown = generateMarkdown(title, aggregated)
    writeToSummary(markdown)
}

main(args)

// endregion
