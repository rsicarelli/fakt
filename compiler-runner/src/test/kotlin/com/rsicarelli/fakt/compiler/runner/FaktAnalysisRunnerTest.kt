// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.runner

import com.rsicarelli.fakt.codegen.analysis.FakeDeclaration
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * BDD suite for the spike runner. Each test constructs a fresh session, drives K2 in-process via
 * `kotlin-compile-testing` (kctfork), and asserts on the captured [FakeDeclaration]s.
 *
 * Tests are isolated — no `@BeforeEach`/`@AfterEach`, no shared mutable state. Each test builds its
 * own `RunnerSession` and `KotlinCompilation`. Reuse is exercised explicitly by the leak-canary
 * test, which runs two compilations against the same module-level fixture set and asserts both
 * produce fresh declarations.
 */
@OptIn(ExperimentalCompilerApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktAnalysisRunnerTest {

    @Test
    fun `GIVEN single fake interface in fixture WHEN runner executes THEN returns one FakeDeclaration matching package and name`() {
        val session = FaktAnalysisRunner.newSession(stubSourceSetContext("UserService"))
        val source =
            SourceFile.kotlin(
                "UserService.kt",
                """
                    package com.rsicarelli.fakt.runner.fixture

                    import com.rsicarelli.fakt.Fake

                    @Fake
                    interface UserService {
                        fun getName(): String
                        val isActive: Boolean
                    }
                """,
            )

        val result = compile(session, source)

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val captured = session.captured()
        assertEquals(1, captured.size, "Expected one FakeDeclaration; got: $captured")
        val decl = captured.single()
        assertTrue(decl is FakeDeclaration.Interface, "Expected Interface, got ${decl::class}")
        assertEquals("UserService", decl.simpleName)
        assertEquals("com.rsicarelli.fakt.runner.fixture", decl.packageName)
        assertEquals(1, decl.functions.size, "Expected 1 function (getName)")
        assertEquals(1, decl.properties.size, "Expected 1 property (isActive)")
    }

    @Test
    fun `GIVEN classpath with stdlib only WHEN runner executes on minimal source THEN succeeds without spurious analysis errors`() {
        val session = FaktAnalysisRunner.newSession(stubSourceSetContext("Empty"))
        val source =
            SourceFile.kotlin(
                "Empty.kt",
                """
                    package com.rsicarelli.fakt.runner.fixture

                    object Marker
                """,
            )

        val result = compile(session, source)

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertEquals(emptyList<FakeDeclaration>(), session.captured())
    }

    @Test
    fun `GIVEN runner reused across two invocations WHEN inputs differ THEN second session captures the second fixture independently`() {
        val firstSession = FaktAnalysisRunner.newSession(stubSourceSetContext("First"))
        val firstSource =
            SourceFile.kotlin(
                "First.kt",
                """
                    package com.rsicarelli.fakt.runner.fixture

                    import com.rsicarelli.fakt.Fake

                    @Fake interface First { fun a(): Int }
                """,
            )
        val firstResult = compile(firstSession, firstSource)
        assertEquals(KotlinCompilation.ExitCode.OK, firstResult.exitCode, firstResult.messages)
        assertEquals(listOf("First"), firstSession.captured().map { it.simpleName })

        val secondSession = FaktAnalysisRunner.newSession(stubSourceSetContext("Second"))
        val secondSource =
            SourceFile.kotlin(
                "Second.kt",
                """
                    package com.rsicarelli.fakt.runner.fixture

                    import com.rsicarelli.fakt.Fake

                    @Fake interface Second { fun b(): String; fun c(): Boolean }
                """,
            )
        val secondResult = compile(secondSession, secondSource)
        assertEquals(KotlinCompilation.ExitCode.OK, secondResult.exitCode, secondResult.messages)

        val captured = secondSession.captured()
        assertEquals(1, captured.size)
        assertEquals("Second", captured.single().simpleName)
        assertEquals(2, (captured.single() as FakeDeclaration.Interface).functions.size)
    }

    @Test
    fun `GIVEN abstract class with @Fake WHEN runner executes THEN returns FakeDeclaration_Class with constructor params populated`() {
        val session = FaktAnalysisRunner.newSession(stubSourceSetContext("Repository"))
        val source =
            SourceFile.kotlin(
                "Repository.kt",
                """
                    package com.rsicarelli.fakt.runner.fixture

                    import com.rsicarelli.fakt.Fake

                    @Fake
                    abstract class Repository(val baseUrl: String, val timeout: Long) {
                        abstract fun fetch(id: String): String
                        open fun cacheKey(id: String): String = id
                    }
                """,
            )

        val result = compile(session, source)

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val captured = session.captured()
        assertEquals(1, captured.size, "Expected one FakeDeclaration; got: $captured")
        val decl = captured.single()
        assertTrue(decl is FakeDeclaration.Class, "Expected Class, got ${decl::class}")
        assertEquals("Repository", decl.simpleName)
        assertEquals(2, decl.constructorParameters.size, "Expected baseUrl + timeout ctor params")
        assertEquals(listOf("baseUrl", "timeout"), decl.constructorParameters.map { it.name })
        assertEquals(1, decl.abstractMethods.size, "Expected 1 abstract method (fetch)")
        assertEquals(1, decl.openMethods.size, "Expected 1 open method (cacheKey)")
    }

    @Test
    fun `GIVEN runner invoked 25 times in the same JVM WHEN sessions are independent THEN every invocation captures its declaration without leak`() {
        val source =
            SourceFile.kotlin(
                "Repeated.kt",
                """
                    package com.rsicarelli.fakt.runner.fixture

                    import com.rsicarelli.fakt.Fake

                    @Fake interface Repeated { fun ping(): Int }
                """,
            )

        repeat(25) { iteration ->
            val session = FaktAnalysisRunner.newSession(stubSourceSetContext("Repeat-$iteration"))
            val result = compile(session, source)
            assertEquals(
                KotlinCompilation.ExitCode.OK,
                result.exitCode,
                "Iteration $iteration: ${result.messages}",
            )
            assertEquals(1, session.captured().size, "Iteration $iteration: expected one capture")
        }
    }
}

@OptIn(ExperimentalCompilerApi::class)
private fun compile(session: RunnerSession, vararg sources: SourceFile): JvmCompilationResult =
    KotlinCompilation()
        .apply {
            this.sources = sources.toList()
            inheritClassPath = true
            messageOutputStream = System.out
            compilerPluginRegistrars = listOf(session.pluginRegistrar)
        }
        .compile()

private fun stubSourceSetContext(label: String): SourceSetContext =
    SourceSetContext(
        compilationName = "test-$label",
        targetName = "jvm",
        platformType = "jvm",
        isTest = true,
        defaultSourceSet = SourceSetInfo("jvmTest", parents = listOf("commonTest")),
        allSourceSets =
            listOf(
                SourceSetInfo("jvmTest", parents = listOf("commonTest")),
                SourceSetInfo("commonTest", parents = emptyList()),
            ),
        outputDirectory = "/tmp/fakt-spike/jvmTest",
        commonTestOutputDirectory = "/tmp/fakt-spike/commonTest",
    )
