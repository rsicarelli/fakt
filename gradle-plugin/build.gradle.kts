// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-kotlin-jvm")
    id("fakt-publishing")
    id("fakt-spotless")
    id("fakt-detekt")
    `java-gradle-plugin`
}

description = "Fakt Gradle plugin for seamless build integration"

// Classpath served to the Fakt code-generation Worker via classloader isolation. Held off the
// Gradle daemon's own classpath because kotlin-compiler-embeddable + kotlin-compile-testing are
// heavy and would clash with KGP's bundled compiler if exposed there (research artifact 1, R2).
val faktWorker: Configuration by
    configurations.creating {
        isCanBeResolved = true
        isCanBeConsumed = false
        description = "Runtime classpath for the Fakt code-generation worker"
    }

dependencies {
    // Compiler API — exposed as `api` so consumers can use LogLevel, etc.
    api(projects.compilerApi)

    // Kotlin Gradle Plugin APIs (compileOnly like Metro)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin.api)

    // Compile-only refs used by FaktCodegenWorkAction at the file-import level. The compiler
    // driver itself is invoked reflectively from execute() so daemon-side decoration of the action
    // class never has to resolve kotlin-compiler-embeddable types.
    compileOnly(projects.codegenRuntime)

    // Serialization for SourceSetContext
    implementation(libs.kotlinx.serialization.json)

    // Worker classpath: drives K2 in an isolated classloader without polluting the Gradle daemon.
    // Just kotlin-compiler-embeddable — the Fakt :compiler shadowJar arrives via a separate
    // -Xplugin path so the daemon never sees compiler internals.
    faktWorker(libs.kotlin.compilerEmbeddable)

    // Test dependencies
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.gradlePlugin)
    testImplementation(libs.kotlin.gradlePlugin.api)
    testImplementation(libs.coroutines.test)
    testImplementation(gradleTestKit())
    testImplementation(projects.compilerRunner)
    testImplementation(projects.codegenRuntime)
    // Tests pass the test process's classpath to the Worker; needs kotlin-compiler-embeddable so
    // K2JVMCompiler is resolvable at runtime.
    testImplementation(libs.kotlin.compilerEmbeddable)
    // Brings the Fakt :compiler shadowJar onto the test classpath so the worker can resolve it
    // at -Xplugin time. :compiler's apiElements/runtimeElements are rewired to publish shadowJar.
    testImplementation(projects.compiler)
    // Test fixtures use the real `@Fake` annotation, so :annotations must be on the worker's
    // compile classpath. The test piggybacks on its own JVM classpath when constructing the
    // worker classpath, so this dep must be testImplementation here too.
    testImplementation(projects.annotations)
}

gradlePlugin {
    website.set("https://github.com/rsicarelli/fakt")
    vcsUrl.set("https://github.com/rsicarelli/fakt.git")

    plugins {
        create("faktPlugin") {
            id = "com.rsicarelli.fakt"
            implementationClass = "com.rsicarelli.fakt.gradle.FaktGradleSubplugin"
            displayName = "Fakt Plugin"
            description =
                "High-performance fake generator for Kotlin test environments using FIR + IR compiler plugin architecture"
            tags.set(listOf("kotlin", "compiler-plugin", "testing", "fake", "mock"))
            // Version inherited from PublishingConvention (gradle.properties:VERSION_NAME)
        }
    }
}

tasks {
    // Configure test task
    test {
        // TestKit suites (FaktGenerateTaskTest) spawn a Gradle daemon + worker that hosts
        // K2JVMCompiler — heavy on cold-cache CI runs, so bump both heap and timeout above
        // the 1g/30s defaults that worked when this module had only ProjectBuilder tests.
        jvmArgs("-Xmx2g")
        systemProperty("junit.jupiter.execution.timeout.default", "5m")
    }
}
