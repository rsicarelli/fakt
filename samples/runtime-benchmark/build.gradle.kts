// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Root of the runtime-benchmark composite build. Each competing technology lives in its own
// isolated subproject (see settings.gradle.kts). This root only wires an aggregate lifecycle task.
// Plugins are applied per-subproject via versioned catalog aliases, so nothing is declared here.

// `./gradlew benchmark` runs every competitor's benchmark suite. Run with --continue in CI so one
// technology failing to resolve on a bleeding-edge Kotlin does not hide the others' results.
// :mockative is intentionally excluded (see settings.gradle.kts) — kept out of the aggregate run.
val competitors = listOf(":fakt", ":fakt-nohistory", ":handwritten", ":mockk", ":mockito", ":mokkery")

tasks.register("benchmark") {
    group = "verification"
    description = "Run the mock-tax benchmark suite for every competing technology."
    dependsOn(competitors.map { "$it:test" })
}
