// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

/*
 * JVM module whose MAIN code calls `Receipt.serializer()`. That function only exists because the
 * Kotlin serialization compiler plugin is applied to `compileKotlin`. Fakt's worker does not load
 * that plugin, so it sees an unresolved reference outside any @Fake and must tolerate it (#165).
 * The @Fake interfaces here never use the serializable type, so generation still succeeds.
 * No KSP and no JDK toolchain: this module runs on the JDK that runs Gradle.
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.fakt)
}

dependencies {
    implementation(libs.fakt.annotations)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coroutines)

    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

fakt {
    // INFO or higher prints "Fakt: tolerated compiler error: ..." so the CI guard can see it.
    logLevel.set(LogLevel.INFO)
}
