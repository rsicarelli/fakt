// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

/*
 * JVM module whose MAIN code only compiles with the options the build gives `compileKotlin`:
 * - a module-wide optIn of an ERROR-level @RequiresOptIn marker (InteropMarker),
 * - the experimental flag -Xcontext-sensitive-resolution (an unqualified enum entry in a `when`),
 * - a KSP-generated type (GeneratedInvoice) used by main code and by a @Fake interface,
 * - a Gradle JDK toolchain (see interopToolchain in gradle.properties). On 17, main code also
 *   references java.lang.Compiler, which JDK 21 no longer has.
 * Fakt's worker must see the same options and JDK, or generating the fakes fails.
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.fakt)
}

val interopToolchain: Int = providers.gradleProperty("interopToolchain").map(String::toInt).get()

kotlin {
    jvmToolchain(interopToolchain)

    compilerOptions {
        optIn.add("com.rsicarelli.fakt.samples.compilerInterop.InteropMarker")
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
    }

    // Only JDK 17 still has java.lang.Compiler, so this source dir exists for that toolchain only.
    if (interopToolchain == 17) {
        sourceSets.named("main") { kotlin.srcDir("src/jdk17/kotlin") }
    }
}

dependencies {
    implementation(project(":processor"))
    ksp(project(":processor"))
    implementation(libs.fakt.annotations)

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}

tasks.test { useJUnitPlatform() }

fakt {
    logLevel.set(LogLevel.DEBUG)
}
