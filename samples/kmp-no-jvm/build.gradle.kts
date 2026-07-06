// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

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
