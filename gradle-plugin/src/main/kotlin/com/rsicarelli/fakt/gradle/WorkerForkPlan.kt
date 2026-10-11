// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.io.File

/**
 * Classpath and system properties of the forked codegen worker. Pooled forks are keyed by both, so
 * a Native worker never shares a daemon with a JVM one.
 */
internal data class WorkerForkPlan(
    val classpath: List<File>,
    val systemProperties: Map<String, String>,
)

/**
 * Plans the worker fork for a task.
 *
 * With a [konanHome] (a Kotlin/Native task) the classpath is only the distribution's
 * `kotlin-native-compiler-embeddable.jar`, and `konan.home` points the compiler at its own
 * distribution. The jar is self-contained, so [faktWorkerClasspath] stays out: mixing the two
 * copies of `org.jetbrains.kotlin.cli.common.*` risks class clashes. Without one, the task keeps
 * the Fakt worker classpath and sets no properties.
 */
internal fun workerForkPlan(
    konanHome: File?,
    faktWorkerClasspath: Collection<File>,
): WorkerForkPlan =
    if (konanHome == null) {
        WorkerForkPlan(faktWorkerClasspath.toList(), emptyMap())
    } else {
        WorkerForkPlan(
            classpath = listOf(embeddableCompilerJar(konanHome)),
            systemProperties = mapOf(KONAN_HOME_PROPERTY to konanHome.absolutePath),
        )
    }

private const val KONAN_HOME_PROPERTY: String = "konan.home"
