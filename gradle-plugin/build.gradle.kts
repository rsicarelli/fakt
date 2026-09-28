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

// The stdlib's composite metadata archive (per-source-set klibs under commonMain/ etc.). TestKit
// producer suites unpack its commonMain klib to feed KotlinMetadataCompiler a real common
// classpath — the metadata driver cannot read JVM jars.
val stdlibMetadataForTests: Configuration by
    configurations.creating {
        isCanBeResolved = true
        isCanBeConsumed = false
        isTransitive = false
        description = "kotlin-stdlib 'all' metadata archive for producer TestKit suites"
    }

// Kotlin/JS and Kotlin/Wasm stdlib klibs. The JS/Wasm consumer TestKit suite feeds them to the
// K2JSCompiler driver through the klib input — that driver cannot read JVM jars either.
val stdlibWebKlibsForTests: Configuration by
    configurations.creating {
        isCanBeResolved = true
        isCanBeConsumed = false
        isTransitive = false
        description = "kotlin-stdlib JS and Wasm klibs for the JS/Wasm consumer TestKit suite"
    }

dependencies {
    // Compiler API — exposed as `api` so consumers can use LogLevel, etc.
    api(projects.compilerApi)

    // Kotlin Gradle Plugin APIs (compileOnly like Metro)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin.api)

    // Android Gradle Plugin API (compileOnly), pinned to the oldest supported AGP. Only touched
    // when an Android compilation is wired — see android/AndroidIntegration.
    compileOnly(libs.agp.api.floor)

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
    // LauncherSessionListener API for ProjectBuilderWarmUp (test-suite infrastructure).
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.platform.launcher)
    // AGP's API types for AndroidIntegrationTest's hand-written androidComponents fake.
    testImplementation(libs.agp.api.floor)
    testImplementation(libs.kotlin.gradlePlugin)
    testImplementation(libs.kotlin.gradlePlugin.api)
    testImplementation(libs.coroutines.test)
    testImplementation(gradleTestKit())
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

    stdlibMetadataForTests(
        "org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.asProvider().get()}:all@jar"
    )
    stdlibWebKlibsForTests(
        "org.jetbrains.kotlin:kotlin-stdlib-js:${libs.versions.kotlin.asProvider().get()}@klib"
    )
    stdlibWebKlibsForTests(
        "org.jetbrains.kotlin:kotlin-stdlib-wasm-js:${libs.versions.kotlin.asProvider().get()}@klib"
    )
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
        // Resolved lazily at execution time (jvmArgumentProviders, not systemProperty) so the
        // configuration isn't resolved during configuration; a dedicated provider class keeps the
        // configuration cache free of script-object references.
        jvmArgumentProviders.add(
            objects.newInstance(StdlibMetadataArgProvider::class.java).apply {
                jar.from(stdlibMetadataForTests)
                webKlibs.from(stdlibWebKlibsForTests)
            }
        )
    }
}

/**
 * Passes the resolved stdlib `-all` metadata archive path to producer TestKit suites, and the JS /
 * Wasm stdlib klib paths to the JS/Wasm consumer suite.
 */
abstract class StdlibMetadataArgProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jar: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val webKlibs: ConfigurableFileCollection

    override fun asArguments(): List<String> =
        listOf(
            "-Dfakt.test.stdlibMetadataJar=${jar.singleFile.absolutePath}",
            "-Dfakt.test.stdlibJsKlib=${webKlib("kotlin-stdlib-js-")}",
            "-Dfakt.test.stdlibWasmJsKlib=${webKlib("kotlin-stdlib-wasm-js-")}",
        )

    private fun webKlib(prefix: String): String =
        webKlibs.files.single { it.name.startsWith(prefix) }.absolutePath
}
