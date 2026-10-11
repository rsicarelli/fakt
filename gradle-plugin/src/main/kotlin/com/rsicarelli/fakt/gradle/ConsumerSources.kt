// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Feeds a consumer task. `sources` (the task's only `@SkipWhenEmpty` input) holds the sets the
 * consumer emits: the compilation's own member sets plus the platform-owned ancestors (`nativeMain`
 * when one native target compiles it, `sharedJvmMain` in jvm + js), whose fakes only this consumer
 * can write. Without them an empty leaf made the task `NO-SOURCE` and those fakes were never
 * generated. `analysisOnlySources` keeps the rest (`commonMain`, shared intermediates), so no file
 * is listed in both.
 *
 * Lazy like [lazySources]: ownership reads the source set graph, so it is evaluated when Gradle
 * resolves the files, never at registration (KGP has not finished the `dependsOn` edges then).
 */
internal fun configureConsumerSources(
    task: FaktGenerateTask,
    compilation: KotlinCompilation<*>,
    enabled: Provider<Boolean>,
) {
    task.sources.from(compilation.lazySources(enabled) { consumerSplit(it).emitted })
    task.analysisOnlySources.from(
        compilation.lazySources(enabled) { consumerSplit(it).analysisOnly }
    )
}

private class ConsumerSplit(
    val emitted: List<KotlinSourceSet>,
    val analysisOnly: List<KotlinSourceSet>,
)

private fun consumerSplit(compilation: KotlinCompilation<*>): ConsumerSplit {
    val split = memberSplit(compilation)
    val owned = platformOwnedAncestorsOf(compilation, TaskShape.CONSUMER)
    val (platformOwned, rest) = split.analysisOnly.partition { it.name in owned }
    return ConsumerSplit(emitted = split.own + platformOwned, analysisOnly = rest)
}
