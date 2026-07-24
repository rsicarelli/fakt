// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyJvmCompilation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

/**
 * Convention plugin for `samples/runtime-benchmark` modules.
 *
 * Every competing technology (Fakt, MockK, Mockito, Mokkery, Mockative, hand-written) is measured
 * under identical conditions. This plugin is the single source of truth for those conditions so no
 * individual module can quietly tune its own — the strongest anti-bias lever in the benchmark:
 *
 * - Kotlin/JVM plugin + explicit Java 11 target (same bytecode target for all).
 * - JUnit Platform with **parallelism disabled** and a **single fork** — benchmarks must not contend
 *   for CPU with each other or the noise dwarfs the signal (the default sample test convention runs
 *   tests concurrently, which is correct for correctness tests but wrong for timing).
 * - Fixed heap, fixed warmup/measurement/op-scale passed to the harness via system properties.
 * - Test task never considered up-to-date, so `benchmark` always re-measures.
 *
 * The `:fakt` module applies `id("com.rsicarelli.fakt")` on top of this; the mock modules do not.
 */
class FaktBenchmarkJvmPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            applyJvmCompilation()

            tasks.withType<Test>().configureEach {
                useJUnitPlatform()

                // Fairness: no cross-test contention, single JVM, deterministic heap.
                maxParallelForks = 1
                jvmArgs("-Xmx2g", "-XX:MaxMetaspaceSize=512m", "-XX:+HeapDumpOnOutOfMemoryError")
                systemProperty("junit.jupiter.execution.parallel.enabled", "false")
                systemProperty("junit.jupiter.execution.timeout.default", "30m")

                // Identical measurement config for every module (override with -Pfakt.benchmark.*).
                systemProperty("fakt.benchmark.library", project.name)
                systemProperty("fakt.benchmark.fork", benchProp("fakt.benchmark.fork", "1"))
                systemProperty(
                    "fakt.benchmark.outputDir",
                    layout.buildDirectory.dir("benchmark-results").get().asFile.absolutePath,
                )
                systemProperty("fakt.benchmark.warmup", benchProp("fakt.benchmark.warmup", "2"))
                systemProperty(
                    "fakt.benchmark.iterations",
                    benchProp("fakt.benchmark.iterations", "8"),
                )
                systemProperty("fakt.benchmark.invocations", benchProp("fakt.benchmark.invocations", "500"))
                systemProperty("fakt.benchmark.churn", benchProp("fakt.benchmark.churn", "50"))
                systemProperty("fakt.benchmark.featureInputs", benchProp("fakt.benchmark.featureInputs", "100"))

                // Always re-run: a benchmark is never "up to date".
                outputs.upToDateWhen { false }
            }
        }
    }

    private fun Project.benchProp(name: String, default: String): String =
        (findProperty(name) as? String) ?: default
}
