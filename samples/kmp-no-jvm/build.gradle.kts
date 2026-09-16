// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

/*
 * KMP sample WITHOUT any JVM/Android target. Locks the cache-correct routing for projects whose
 * targets are all non-drivable: the commonMain producer (`faktGenerateMetadataCommonMain`) drives
 * `KotlinMetadataCompiler` — no JVM classpath required — while the platform mains (js, linuxX64)
 * stay on the in-process plugin (LEGACY_HYBRID). The unpaired `expect` in commonMain is the
 * issue #79 blocker shape: it must not fail the producer.
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

// This sample doesn't apply the fakt-sample-kmp convention plugin (see the FaktSampleKmpPlugin
// relocation), so it needs its own committed yarn.lock relocated to vendor/kotlin-js-store -
// otherwise GitHub's Dependency Graph indexes it as a manifest with no sibling package.json,
// spawning permanently-failing Dependabot Security Update jobs.
rootProject.plugins.withType(YarnPlugin::class.java) {
    rootProject.the<YarnRootExtension>().lockFileDirectory =
        rootProject.rootDir.resolve("vendor/kotlin-js-store")
}

kotlin {
    applyDefaultHierarchyTemplate()

    js {
        nodejs()
    }
    linuxX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.fakt.annotations)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
            }
        }
    }
}

fakt {
    logLevel.set(LogLevel.DEBUG)
}
