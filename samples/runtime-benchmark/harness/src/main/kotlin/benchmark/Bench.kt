// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import java.io.File
import kotlin.math.sqrt

/**
 * The measurement engine shared, byte-for-byte, by every competing technology.
 *
 * Using one harness for all libraries is the core fairness guarantee: warmup count, measured
 * iteration count, the anti-dead-code-elimination sink and the timing method are identical whether
 * we are measuring Fakt or Mockito. Configuration comes entirely from system properties injected by
 * the `fakt-benchmark-jvm` convention plugin, so no module can tune its own knobs.
 *
 * This is a transparent nanoTime harness rather than JMH: Fakt generates its fakes only into the
 * *test* source set, so a separate JMH source set could not see them, and JMH's Gradle plugin lags
 * bleeding-edge Kotlin. Rigor comes from the orchestration: the whole suite is run in F fresh JVM
 * **forks** (each a separate `./gradlew` test invocation, tagged via `fakt.benchmark.fork`), each
 * doing warmup + N measured iterations. The summary pools every raw sample across all forks and
 * reports the median (robust to JIT/GC outliers) plus the fork-to-fork spread. Raw samples are
 * emitted to JSON and published as a CI artifact so anyone can recompute the numbers.
 */
object Bench {
    private val warmup = intProp("fakt.benchmark.warmup", 2)
    private val iterations = intProp("fakt.benchmark.iterations", 8)
    private val fork = System.getProperty("fakt.benchmark.fork") ?: "1"

    /** How many stubbed invocations each invocation-style scenario performs per measured iteration. */
    val invocations: Int = intProp("fakt.benchmark.invocations", 500)

    /** How many fresh fakes/mocks the instantiation-churn scenario creates per measured iteration. */
    val churn: Int = intProp("fakt.benchmark.churn", 50)

    @Volatile
    private var sink: Long = 0L

    private val results = mutableListOf<Result>()

    data class Result(val scenario: String, val samplesNanos: LongArray)

    /**
     * Measure a single scenario. [block] performs the whole create → configure → invoke → verify
     * lifecycle and returns a checksum that is folded into a volatile sink so the JIT cannot elide
     * the work as dead code.
     */
    fun measure(scenario: String, block: () -> Long) {
        try {
            repeat(warmup) { sink += block() }

            val samples = LongArray(iterations)
            for (i in 0 until iterations) {
                val start = System.nanoTime()
                val checksum = block()
                samples[i] = System.nanoTime() - start
                sink += checksum
            }

            results += Result(scenario, samples)
            val mean = samples.average()
            println("[bench] fork=$fork $scenario: mean=${(mean / 1000.0).round2()}us min=${samples.min() / 1000}us")
        } catch (e: Throwable) {
            // A single scenario failing must not lose the whole module's data — record and move on.
            println("[bench] fork=$fork $scenario: SKIPPED (${e::class.simpleName}: ${e.message})")
        }
    }

    /**
     * Persist this fork's raw samples as JSON for the summary script. One file per (library, fork)
     * so parallel/looped forks never overwrite each other; the summary globs and pools them all.
     */
    fun flush() {
        val library = System.getProperty("fakt.benchmark.library") ?: "unknown"
        val dir = File(System.getProperty("fakt.benchmark.outputDir") ?: "build/benchmark-results")
        dir.mkdirs()

        val json = buildString {
            append("{\n")
            append("  \"library\": \"").append(library).append("\",\n")
            append("  \"fork\": \"").append(fork).append("\",\n")
            append("  \"warmup\": ").append(warmup).append(",\n")
            append("  \"iterations\": ").append(iterations).append(",\n")
            append("  \"sink\": ").append(sink).append(",\n")
            append("  \"scenarios\": [\n")
            results.forEachIndexed { idx, r ->
                append("    {\"name\": \"").append(r.scenario).append("\", ")
                append("\"samplesNanos\": [").append(r.samplesNanos.joinToString(",")).append("]}")
                if (idx != results.lastIndex) append(",")
                append("\n")
            }
            append("  ]\n")
            append("}\n")
        }

        val file = File(dir, "$library.fork$fork.json")
        file.writeText(json)
        println("[bench] wrote ${results.size} scenarios for '$library' fork $fork -> ${file.absolutePath}")
    }

    private fun Double.round2(): Double = ((this * 100).toLong() / 100.0)

    private fun intProp(name: String, default: Int): Int =
        System.getProperty(name)?.toIntOrNull() ?: default
}
