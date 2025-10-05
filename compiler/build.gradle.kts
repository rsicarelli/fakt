// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("fakt-kotlin-jvm")
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.shadow)
}

dependencies {
    // compileOnly - provided by Gradle/Kotlin at runtime
    compileOnly(libs.kotlin.compilerEmbeddable)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.coroutines.test)
    // For tests, we need the actual compiler
    testImplementation(libs.kotlin.compilerEmbeddable)
}

tasks {
    jar {
        // Include service loader files from resources
        from(sourceSets.main.map { it.output })

        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        }
    }

    shadowJar {
        archiveClassifier.set("")

        // Don't relocate Kotlin compiler classes - they're provided by the compiler
        // Just include all dependencies as-is
        configurations = listOf(project.configurations.runtimeClasspath.get())

        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        }
    }

    // Make the shadow jar the main artifact
    named("build") {
        dependsOn(shadowJar)
    }

    test {
        jvmArgs("-Xmx2g", "-XX:MaxMetaspaceSize=512m")
        systemProperty("junit.jupiter.execution.timeout.default", "60s")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "2")
    }
}
