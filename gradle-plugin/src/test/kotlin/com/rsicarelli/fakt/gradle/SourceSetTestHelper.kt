// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo

/**
 * Test helper for creating SourceSetContext instances for testing.
 * Provides factory methods that simulate real Gradle/KMP scenarios.
 */
object SourceSetTestHelper {
    /**
     * Create a simple JVM test context (jvmTest → commonMain).
     */
    fun createSimpleJvmTestContext(
        compilationName: String,
        targetName: String,
        outputDirectory: String,
    ): SourceSetContext =
        SourceSetContext(
            compilationName = compilationName,
            targetName = targetName,
            platformType = "jvm",
            isTest = true,
            defaultSourceSet = SourceSetInfo("jvmTest", listOf("commonMain")),
            allSourceSets =
                listOf(
                    SourceSetInfo("jvmTest", listOf("commonMain")),
                    SourceSetInfo("commonMain", emptyList()),
                ),
            outputDirectory = outputDirectory,
        )

    /**
     * Create a simple JVM main context (jvmMain → commonMain).
     */
    fun createSimpleJvmMainContext(
        compilationName: String,
        targetName: String,
        outputDirectory: String,
    ): SourceSetContext =
        SourceSetContext(
            compilationName = compilationName,
            targetName = targetName,
            platformType = "jvm",
            isTest = false,
            defaultSourceSet = SourceSetInfo("jvmMain", listOf("commonMain")),
            allSourceSets =
                listOf(
                    SourceSetInfo("jvmMain", listOf("commonMain")),
                    SourceSetInfo("commonMain", emptyList()),
                ),
            outputDirectory = outputDirectory,
        )

    /**
     * Create an iOS KMP context with full hierarchy:
     * iosX64Main → iosMain → appleMain → nativeMain → commonMain
     */
    fun createIosKmpContext(
        compilationName: String,
        targetName: String,
        outputDirectory: String,
    ): SourceSetContext =
        SourceSetContext(
            compilationName = compilationName,
            targetName = targetName,
            platformType = "native",
            isTest = false,
            defaultSourceSet = SourceSetInfo("iosX64Main", listOf("iosMain")),
            allSourceSets =
                listOf(
                    SourceSetInfo("iosX64Main", listOf("iosMain")),
                    SourceSetInfo("iosMain", listOf("appleMain")),
                    SourceSetInfo("appleMain", listOf("nativeMain")),
                    SourceSetInfo("nativeMain", listOf("commonMain")),
                    SourceSetInfo("commonMain", emptyList()),
                ),
            outputDirectory = outputDirectory,
        )

    /**
     * Create a context with multi-parent source set:
     * appleTest → iosTest, macosTest → commonTest
     */
    fun createMultiParentContext(
        compilationName: String,
        targetName: String,
        outputDirectory: String,
    ): SourceSetContext =
        SourceSetContext(
            compilationName = compilationName,
            targetName = targetName,
            platformType = "native",
            isTest = true,
            defaultSourceSet = SourceSetInfo("appleTest", listOf("iosTest", "macosTest")),
            allSourceSets =
                listOf(
                    SourceSetInfo("appleTest", listOf("iosTest", "macosTest")),
                    SourceSetInfo("iosTest", listOf("commonTest")),
                    SourceSetInfo("macosTest", listOf("commonTest")),
                    SourceSetInfo("commonTest", emptyList()),
                ),
            outputDirectory = outputDirectory,
        )
}
