// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    id("fakt-sample-kmp")
    alias(libs.plugins.fakt)
}

// Besides commonMain and the platform mains, this sample declares a fake in `webMain` (the
// intermediate source set js and wasmJs share). `faktGenerateMetadataWebMain` produces it (#162).
//
// Native (#152): linuxX64Main (platform.posix, a cinterop library with a forward-declared struct),
// linuxMain (commonized posix), appleMain (Foundation) and iosMain (UIKit) each hold a `@Fake` whose
// types only the Kotlin/Native compiler can resolve. Each is generated on the task path.
kotlin {
    linuxX64 {
        compilations.getByName("main").cinterops.create("sampleport") {
            definitionFile.set(file("src/nativeInterop/cinterop/sampleport.def"))
            includeDirs(file("src/nativeInterop/cinterop"))
        }
    }

    sourceSets {
        // Module-wide opt-in: an interface-level @OptIn loses its import in the generated fake
        // (#172). Remove once that is fixed. JVM/JS only warn about the unresolved marker.
        all { languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi") }

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

        // Platform-specific test dependencies (if needed)
        jvmTest {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
    }
}

fakt {
    logLevel.set(LogLevel.DEBUG)
}
