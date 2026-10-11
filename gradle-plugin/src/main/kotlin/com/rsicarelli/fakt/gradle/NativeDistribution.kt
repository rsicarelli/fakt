// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

private const val EMBEDDABLE_COMPILER_JAR: String = "kotlin-native-compiler-embeddable.jar"

/** `<konanHome>/konan/lib/kotlin-native-compiler-embeddable.jar`, the Native compiler driver. */
internal fun embeddableCompilerJar(konanHome: File): File =
    konanHome.resolve("konan").resolve("lib").resolve(EMBEDDABLE_COMPILER_JAR)

/**
 * Every input of the distribution lookup, as plain values so [resolveKonanHome] stays pure.
 *
 * @property nativeHome `kotlin.native.home`, or its `org.jetbrains.kotlin.native.home` alias.
 * @property konanDataDirProperty the `konan.data.dir` Gradle property.
 * @property konanDataDirEnv the `KONAN_DATA_DIR` environment variable.
 * @property userHome the `user.home` system property.
 * @property version `kotlin.native.version` (or its alias), else the applied KGP version.
 */
internal data class NativeDistributionQuery(
    val nativeHome: String?,
    val konanDataDirProperty: String?,
    val konanDataDirEnv: String?,
    val userHome: String,
    val version: String,
    val osName: String,
    val osArch: String,
)

/**
 * Where the Kotlin/Native distribution KGP provisioned lives, resolved the way KGP 2.4.10's
 * `NativePropertiesLoader.actualNativeHomeDirectory` does (verified with `javap`):
 * 1. the `konan.data.dir` property: `<dir>/kotlin-native-prebuilt-<host>-<version>`;
 * 2. `kotlin.native.home` (or `org.jetbrains.kotlin.native.home`): used as is;
 * 3. `<data dir>/kotlin-native-prebuilt-<host>-<version>`, where the data dir is the
 *    `KONAN_DATA_DIR` environment variable, else `<user.home>/.konan` (`DependencyDirectories`).
 *
 * So when both `konan.data.dir` and `kotlin.native.home` are set, KGP uses the former. This follows
 * KGP rather than the most intuitive reading, so Fakt always reads the directory KGP compiles with.
 * A `kotlin.native.distribution.type` other than the default `prebuilt` renames the directory in
 * KGP; Fakt does not model it.
 */
internal fun resolveKonanHome(query: NativeDistributionQuery): File {
    val dirName =
        "kotlin-native-prebuilt-${hostDistributionSuffix(query.osName, query.osArch)}-" +
            query.version
    val dataDir = query.konanDataDirEnv ?: File(query.userHome).resolve(".konan").path
    return query.konanDataDirProperty?.let { File(it).resolve(dirName) }
        ?: query.nativeHome?.let(::File)
        ?: File(dataDir).resolve(dirName)
}

/**
 * The `<os>-<arch>` part of a distribution directory name, as in `linux-x86_64` or `macos-aarch64`,
 * from the `os.name` and `os.arch` system properties.
 */
internal fun hostDistributionSuffix(osName: String, osArch: String): String {
    val os =
        when {
            osName.startsWith("Linux", ignoreCase = true) -> "linux"
            osName.startsWith("Mac", ignoreCase = true) -> "macos"
            osName.startsWith("Windows", ignoreCase = true) -> "windows"
            else -> error("Kotlin/Native has no distribution for the host OS '$osName'")
        }
    val arch =
        when (osArch.lowercase()) {
            "amd64",
            "x86_64" -> "x86_64"
            "aarch64",
            "arm64" -> "aarch64"
            else -> error("Kotlin/Native has no distribution for the host architecture '$osArch'")
        }
    return "$os-$arch"
}

/**
 * Why [konanHome] cannot run the Native compiler, or null when it can. Only the embeddable compiler
 * jar counts: `kotlin-native.jar` alone is the in-process KGP flavour and cannot be forked.
 */
internal fun nativeDistributionProblem(konanHome: File): String? =
    if (embeddableCompilerJar(konanHome).isFile) {
        null
    } else {
        "No Kotlin/Native distribution at '${konanHome.path}' (missing " +
            "konan/lib/$EMBEDDABLE_COMPILER_JAR). Fakt reads the distribution Kotlin Gradle " +
            "plugin provisions. Set kotlin.native.home to an existing distribution, or " +
            "konan.data.dir to the directory that holds it."
    }

/**
 * Why a task with [konanTargets] cannot run, or null when it can (or is not a Native task). A
 * Native task needs a [konanHome], and that directory must hold a runnable distribution.
 */
internal fun nativeTaskProblem(konanTargets: List<String>, konanHome: File?): String? =
    when {
        konanTargets.isEmpty() -> null
        konanHome == null ->
            "A Kotlin/Native task needs konanHome, but none is set (konanTargets=$konanTargets)."
        else -> nativeDistributionProblem(konanHome)
    }

/** The lazily resolved distribution of one project. */
internal class NativeDistributionProviders(
    val konanHome: Provider<Directory>,
    val version: Provider<String>,
)

/**
 * Configuration-cache safe lookup of the Kotlin/Native distribution. Every read goes through
 * [ProviderFactory], so the configuration cache tracks the properties as inputs, nothing on the
 * file system is touched, no task dependency is added and nothing is downloaded: KGP has already
 * provisioned the directory. [nativeDistributionProblem] checks it at execution time.
 */
internal fun nativeDistribution(project: Project): NativeDistributionProviders {
    val providers = project.providers
    // A constant of the applied plugin, read eagerly so no lambda captures the project.
    val kgpVersion = project.getKotlinPluginVersion()
    val version =
        providers
            .firstProperty("kotlin.native.version", "org.jetbrains.kotlin.native.version")
            .orElse(kgpVersion)
    val nativeHome =
        providers.firstProperty("kotlin.native.home", "org.jetbrains.kotlin.native.home")
    val dataDirProperty = providers.gradleProperty("konan.data.dir")
    val dataDirEnv = providers.environmentVariable("KONAN_DATA_DIR")
    val userHome = providers.systemProperty("user.home")
    val osName = providers.systemProperty("os.name")
    val osArch = providers.systemProperty("os.arch")
    val konanHome =
        providers.provider {
            resolveKonanHome(
                NativeDistributionQuery(
                    nativeHome = nativeHome.orNull,
                    konanDataDirProperty = dataDirProperty.orNull,
                    konanDataDirEnv = dataDirEnv.orNull,
                    userHome = userHome.get(),
                    version = version.get(),
                    osName = osName.get(),
                    osArch = osArch.get(),
                )
            )
        }
    return NativeDistributionProviders(konanHome = project.layout.dir(konanHome), version = version)
}

/** The first of [names] that is set as a Gradle property, in order. */
private fun ProviderFactory.firstProperty(first: String, second: String): Provider<String> =
    gradleProperty(first).orElse(gradleProperty(second))
