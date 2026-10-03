// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Tiny KSP processor: for every class marked @Generate it writes a Generated<Name> class. The :jvm
// module's main code and one of its @Fake interfaces use that generated type.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Class files stay on Java 17 so the processor loads on any JDK the build may run on.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.ksp.api)
}
