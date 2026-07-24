#!/usr/bin/env kotlin

/*
 * Copyright (C) 2025 Rodrigo Sicarelli
 * SPDX-License-Identifier: Apache-2.0
 *
 * Runtime Benchmark Summary Generator for GitHub Actions.
 *
 * Aggregates the per-fork results produced by samples/runtime-benchmark into readable comparison
 * tables written to the GitHub Actions Job Summary (GITHUB_STEP_SUMMARY). Mirrors the sink/fallback
 * behaviour of test-summary.main.kts (dependency-free, appends Markdown, prints to stdout locally).
 *
 * Input: <module>/build/benchmark-results/<library>.fork<N>.json — each holds one fresh-JVM fork's
 * raw per-scenario samples. All forks of a library are pooled; the headline statistic is the MEDIAN
 * of the pooled samples (robust to JIT/GC outliers), with the fork-to-fork spread reported too.
 *
 * Usage: kotlin benchmark-summary.main.kts <base-path> <section-title>
 */

import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

// region Model & config

val LIBRARY_ORDER = listOf("fakt", "fakt-nohistory", "handwritten", "mokkery", "mockative", "mockk", "mockito")
val LIBRARY_LABELS = mapOf(
    "fakt" to "Fakt",
    "fakt-nohistory" to "Fakt (no hist.)",
    "handwritten" to "Hand-written",
    "mokkery" to "Mokkery",
    "mockative" to "Mockative",
    "mockk" to "MockK",
    "mockito" to "Mockito",
)
val SCENARIO_ORDER = listOf(
    "verify-heavy",
    "relaxed-unstubbed",
    "instantiation-churn",
    "property-heavy",
    "suspend-heavy",
)
val REFLECTION_LIBS = setOf("mockk", "mockito")

/** Pooled samples for one (library, scenario) across every fork, plus the per-fork means. */
class Cell {
    val samples = ArrayList<Long>()
    val forkMeans = ArrayList<Double>()
    fun addFork(forkSamples: List<Long>) {
        if (forkSamples.isEmpty()) return
        samples.addAll(forkSamples)
        forkMeans.add(forkSamples.average())
    }
    fun medianUs(): Double = median(samples) / 1000.0
    fun minUs(): Double = (samples.minOrNull() ?: 0L) / 1000.0
    /** Coefficient of variation of the per-fork means (%), i.e. run-to-run reproducibility. */
    fun spreadPct(): Double {
        if (forkMeans.size < 2) return 0.0
        val m = forkMeans.average()
        if (m == 0.0) return 0.0
        val v = forkMeans.sumOf { (it - m) * (it - m) } / forkMeans.size
        return sqrt(v) / m * 100.0
    }
}

// endregion

// region Parsing

fun median(xs: List<Long>): Double {
    if (xs.isEmpty()) return 0.0
    val s = xs.sorted()
    val n = s.size
    return if (n % 2 == 1) s[n / 2].toDouble() else (s[n / 2 - 1] + s[n / 2]) / 2.0
}

val scenarioRegex = Regex(""""name"\s*:\s*"([^"]+)"\s*,\s*"samplesNanos"\s*:\s*\[([^\]]*)]""")

/** Returns library -> (scenario -> Cell), pooling every fork file found under [base]. */
fun collect(base: File): Pair<Map<String, Map<String, Cell>>, Int> {
    val byLib = HashMap<String, HashMap<String, Cell>>()
    var iterations = 0
    val files = base.walkTopDown()
        .filter { it.isFile && it.parentFile.name == "benchmark-results" && it.extension == "json" }
        .toList()

    for (file in files) {
        try {
            val text = file.readText()
            val library = Regex(""""library"\s*:\s*"([^"]+)"""").find(text)?.groupValues?.get(1) ?: continue
            iterations = max(iterations, Regex(""""iterations"\s*:\s*(\d+)""").find(text)?.groupValues?.get(1)?.toInt() ?: 0)
            val libMap = byLib.getOrPut(library) { HashMap() }
            for (m in scenarioRegex.findAll(text)) {
                val scenario = m.groupValues[1]
                val samples = m.groupValues[2].split(',').mapNotNull { it.trim().toLongOrNull() }
                libMap.getOrPut(scenario) { Cell() }.addFork(samples)
            }
        } catch (e: Exception) {
            System.err.println("Warning: failed to parse ${file.name}: ${e.message}")
        }
    }
    return byLib to iterations
}

fun forkCount(base: File): Int = base.walkTopDown()
    .filter { it.isFile && it.parentFile.name == "benchmark-results" && it.extension == "json" }
    .groupBy { it.name.substringBefore(".fork") }
    .values.maxOfOrNull { it.size } ?: 0

// endregion

// region Formatting

fun num(us: Double): String = when {
    us >= 100 -> "%,d".format(us.roundToInt())
    us >= 10 -> "%.0f".format(us)
    else -> "%.1f".format(us)
}

fun ratio(v: Double, base: Double): String = when {
    base <= 0.0 -> "-"
    v / base >= 10 -> "%.0f×".format(v / base)
    else -> "%.1f×".format(v / base)
}

fun bar(v: Double, maxv: Double, width: Int = 28): String {
    if (maxv <= 0) return ""
    return "█".repeat(max(1, (v / maxv * width).roundToInt()))
}

// endregion

fun generateMarkdown(title: String, byLib: Map<String, Map<String, Cell>>, forks: Int, iters: Int): String {
    val present = LIBRARY_ORDER.filter { byLib.containsKey(it) }
    return buildString {
        appendLine("## 🏁 $title")
        appendLine()
        if (present.isEmpty()) {
            appendLine("_No benchmark results found._")
            return@buildString
        }

        // Per-library total = sum of scenario medians.
        val totals = present.associateWith { lib ->
            SCENARIO_ORDER.sumOf { byLib[lib]?.get(it)?.medianUs() ?: 0.0 }
        }
        val faktTotal = totals["fakt"]
        val maxTotal = totals.values.maxOrNull() ?: 1.0

        // Headline.
        if (faktTotal != null && faktTotal > 0) {
            val slower = REFLECTION_LIBS.filter { totals.containsKey(it) }
                .map { totals.getValue(it) / faktTotal }
            if (slower.isNotEmpty()) {
                val lo = "%.1f".format(slower.min())
                val hi = "%.1f".format(slower.max())
                appendLine("**Fakt runs the same suite ${lo}–${hi}× faster than reflection mocks** (MockK, Mockito) — lower is better.")
                appendLine()
            }
        }
        appendLine("_$forks fresh-JVM forks × $iters iterations each · pooled median · one runner._")
        appendLine()

        // (1) Total leaderboard, fastest first, with bars.
        appendLine("### Total across all scenarios (median µs, lower is better)")
        appendLine()
        appendLine("| Technology | µs | × vs Fakt | |")
        appendLine("|:-----------|---:|----------:|:--|")
        present.sortedBy { totals[it] }.forEach { lib ->
            val t = totals.getValue(lib)
            val label = if (lib == "fakt") "**${LIBRARY_LABELS[lib]}**" else LIBRARY_LABELS[lib] ?: lib
            appendLine("| $label | ${num(t)} | ${faktTotal?.let { ratio(t, it) } ?: "-"} | ${bar(t, maxTotal)} |")
        }
        appendLine()

        // (2) Per-scenario median with × vs Fakt, winner (lowest) in bold.
        appendLine("### Per scenario — median µs (× vs Fakt)")
        appendLine()
        appendLine("| Scenario | " + present.joinToString(" | ") { LIBRARY_LABELS[it] ?: it } + " |")
        appendLine("|:---------|" + present.joinToString("") { "---:|" })
        for (scenario in SCENARIO_ORDER) {
            val cells = present.map { it to (byLib[it]?.get(scenario)) }
            val best = cells.filter { it.second != null }.minByOrNull { it.second!!.medianUs() }?.first
            val faktUs = byLib["fakt"]?.get(scenario)?.medianUs()
            val row = present.joinToString(" | ") { lib ->
                val cell = byLib[lib]?.get(scenario) ?: return@joinToString "-"
                val v = cell.medianUs()
                val txt = "${num(v)}" + (faktUs?.let { " (${ratio(v, it)})" } ?: "")
                if (lib == best) "**$txt**" else txt
            }
            appendLine("| $scenario | $row |")
        }
        appendLine()

        // (3) Reproducibility — fork-to-fork spread (max CV% across scenarios).
        appendLine("<details><summary>Reproducibility & best-case (min) figures</summary>")
        appendLine()
        appendLine("Fork-to-fork spread = coefficient of variation of the per-fork means (lower = more stable).")
        appendLine()
        appendLine("| Technology | max spread | best-case total µs (min) |")
        appendLine("|:-----------|-----------:|-------------------------:|")
        present.sortedBy { totals[it] }.forEach { lib ->
            val maxSpread = SCENARIO_ORDER.mapNotNull { byLib[lib]?.get(it)?.spreadPct() }.maxOrNull() ?: 0.0
            val minTotal = SCENARIO_ORDER.sumOf { byLib[lib]?.get(it)?.minUs() ?: 0.0 }
            appendLine("| ${LIBRARY_LABELS[lib]} | ${"%.0f".format(maxSpread)}% | ${num(minTotal)} |")
        }
        appendLine()
        appendLine("</details>")
        appendLine()
        appendLine("---")
        appendLine()
        appendLine("Scenarios target documented \"Mock Tax\" hot-spots; identical work per technology; raw per-fork JSON published as a CI artifact. See `samples/runtime-benchmark/README.md`.")
    }
}

fun writeToSummary(markdown: String) {
    val summaryFile = System.getenv("GITHUB_STEP_SUMMARY")
    if (summaryFile != null) {
        File(summaryFile).appendText(markdown)
        println("Summary written to GITHUB_STEP_SUMMARY")
    } else {
        println()
        println(markdown)
    }
}

fun main(args: Array<String>) {
    val basePath = args.getOrNull(0) ?: "samples/runtime-benchmark"
    val title = args.getOrNull(1) ?: "Runtime Benchmark"
    val base = File(basePath)
    if (!base.exists()) {
        System.err.println("Error: base path does not exist: $basePath")
        return
    }
    println("Scanning for benchmark results in: ${base.absolutePath}")
    val (byLib, iters) = collect(base)
    val forks = forkCount(base)
    println("Found results for: ${byLib.keys.sorted()} across $forks fork(s)")
    writeToSummary(generateMarkdown(title, byLib, forks, iters))
}

main(args)
