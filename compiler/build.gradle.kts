// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("fakt-kotlin-jvm")
    id("fakt-spotless")
    id("fakt-ktlint")
    id("fakt-detekt")
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.shadow)
}

// Kotlin compiler API opt-ins
// Following Metro compiler plugin pattern (https://github.com/slackhq/metro)
kotlin {
    compilerOptions {
        optIn.addAll(
            // UnsafeDuringIrConstructionAPI: Safe in our context because:
            // - We use IrClass.declarations and IrSymbol.owner in IrGenerationExtension.generate()
            // - This method is called AFTER IR construction is complete (post-linkage phase)
            // - All symbols are bound at this point, making the APIs safe to use
            // - Metro (production compiler plugin) uses the same approach
            "org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI",
            // ExperimentalCompilerApi: We're building a compiler plugin
            "org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
        )
    }
}

dependencies {
    // Compiler API data models
    implementation(projects.compilerApi)

    // Serialization for SourceSetContext deserialization
    implementation(libs.kotlinx.serialization.json)

    // compileOnly - provided by Gradle/Kotlin at runtime
    compileOnly(libs.kotlin.compilerEmbeddable)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.coroutines.test)
    // For tests, we need the actual compiler
    testImplementation(libs.kotlin.compilerEmbeddable)
}

// Disable regular jar task - Metro pattern
// The shadowJar will be the main artifact
tasks.jar.configure { enabled = false }

val shadowJar =
    tasks.shadowJar.apply {
        configure {
            // Include main source set output (Metro pattern)
            from(sourceSets.main.map { it.output })

            archiveClassifier.set("")

            // Include runtime dependencies
            configurations = listOf(project.configurations.runtimeClasspath.get())

            // CRITICAL: Merge service loader files from all JARs
            // Without this, service files from dependencies overwrite plugin's service files
            // This ensures ServiceLoader can discover both plugin classes and dependency classes
            mergeServiceFiles()

            manifest {
                attributes(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                )
            }
        }
    }

// Replace artifacts with shadowJar - Metro pattern
// CRITICAL: This ensures maven-publish uses shadowJar instead of regular jar
for (c in arrayOf("apiElements", "runtimeElements")) {
    configurations.named(c) {
        artifacts.removeIf { true }
    }
    artifacts.add(c, shadowJar)
}

tasks {
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
